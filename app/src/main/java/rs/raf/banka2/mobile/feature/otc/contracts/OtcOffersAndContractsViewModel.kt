package rs.raf.banka2.mobile.feature.otc.contracts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.auth.SessionManager
import rs.raf.banka2.mobile.core.auth.SessionState
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.storage.OtcStateStore
import rs.raf.banka2.mobile.data.dto.otc.CounterOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.OtcContractDto
import rs.raf.banka2.mobile.data.dto.otc.OtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.SagaStatusDto
import rs.raf.banka2.mobile.data.repository.OtcRepository
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class OtcOffersAndContractsViewModel @Inject constructor(
    private val repository: OtcRepository,
    private val otcStateStore: OtcStateStore,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(OtcOffersAndContractsState())
    val state: StateFlow<OtcOffersAndContractsState> = _state.asStateFlow()

    private val _events = Channel<OtcOffersAndContractsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Cache poslednje vidjenog timestamp-a per scope da unread brojac ne padne na 0 odmah po setTab. */
    private val cachedLastEntrance = mutableMapOf<String, Long>()

    init { refresh() }

    fun setTab(tab: OtcTab) {
        // Spec Celina 4 (Nova) §2030-2090: kad korisnik prebaci na "Ponude" tab,
        // markiramo trenutak kao lastEntrance da nove izmene (od drugih korisnika)
        // budu unread. Cache-iramo prethodnu vrednost za prikaz badge-a tokom
        // trenutne sesije — store updateujemo samo pri ulasku.
        val isOffersTab = tab == OtcTab.OffersDomestic || tab == OtcTab.OffersForeign
        if (isOffersTab) markCurrentEntrance(tab.scopeKey())
        _state.update { it.copy(tab = tab) }
        refresh()
    }

    private fun OtcTab.scopeKey(): String = when (this) {
        OtcTab.OffersDomestic, OtcTab.ContractsDomestic -> "intra"
        OtcTab.OffersForeign, OtcTab.ContractsForeign -> "inter"
    }

    private fun currentUserId(): Long? =
        (sessionManager.state.value as? SessionState.LoggedIn)?.profile?.id

    private fun markCurrentEntrance(scope: String) {
        val userId = currentUserId() ?: return
        val previousEntrance = cachedLastEntrance.getOrPut(scope) { otcStateStore.lastEntrance(userId, scope) }
        // Cuvamo prethodnu vrednost u cache-u dok korisnik gleda tab; store upisujemo
        // sadasnji timestamp da naredna sesija pocne sa cistom listom unread.
        otcStateStore.markEntrance(userId, scope)
        // Sracunaj unread za trenutni tab pomocu prethodne vrednosti
        recomputeUnread(scope, previousEntrance)
    }

    private fun recomputeUnread(scope: String, previousEntrance: Long) {
        val userId = currentUserId() ?: return
        val offers = _state.value.offers
        val unread = offers.count { offer ->
            val modifiedAtMs = parseIsoMillis(offer.lastModified)
            val modifiedByOther = offer.modifiedBy != null && offer.modifiedBy != userId.toString()
            modifiedAtMs > previousEntrance && modifiedByOther
        }
        if (scope == "intra") _state.update { it.copy(unreadIntra = unread) }
        else _state.update { it.copy(unreadInter = unread) }
    }

    private fun parseIsoMillis(iso: String?): Long {
        if (iso.isNullOrBlank()) return 0L
        return runCatching { Instant.parse(iso).toEpochMilli() }.getOrElse { 0L }
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (_state.value.tab) {
            OtcTab.OffersDomestic, OtcTab.OffersForeign -> {
                val inter = _state.value.tab == OtcTab.OffersForeign
                val scope = if (inter) "inter" else "intra"
                when (val result = repository.listOffers(inter)) {
                    is ApiResult.Success -> {
                        _state.update { it.copy(loading = false, offers = result.data) }
                        // Recompute unread za scope koristeci cached prethodnu vrednost,
                        // odnosno store ako jos nema cache-a (prvi load)
                        val userId = currentUserId()
                        if (userId != null) {
                            val previous = cachedLastEntrance.getOrPut(scope) {
                                otcStateStore.lastEntrance(userId, scope)
                            }
                            recomputeUnread(scope, previous)
                        }
                    }
                    is ApiResult.Failure -> _state.update {
                        it.copy(loading = false, error = result.error.message)
                    }
                    ApiResult.Loading -> Unit
                }
            }
            OtcTab.ContractsDomestic, OtcTab.ContractsForeign -> {
                val inter = _state.value.tab == OtcTab.ContractsForeign
                when (val result = repository.listContracts(inter)) {
                    is ApiResult.Success -> _state.update { it.copy(loading = false, contracts = result.data) }
                    is ApiResult.Failure -> _state.update {
                        it.copy(loading = false, error = result.error.message)
                    }
                    ApiResult.Loading -> Unit
                }
            }
        }
    }

    fun acceptOffer(offer: OtcOfferDto, buyerAccountId: Long?) = viewModelScope.launch {
        when (val result = repository.accept(offer.foreign, offer, buyerAccountId)) {
            is ApiResult.Success -> {
                _events.send(OtcOffersAndContractsEvent.Toast("Ponuda prihvacena."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    fun declineOffer(offer: OtcOfferDto) = viewModelScope.launch {
        when (val result = repository.decline(offer.foreign, offer)) {
            is ApiResult.Success -> {
                _events.send(OtcOffersAndContractsEvent.Toast("Ponuda odbijena."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    fun counterOffer(
        offer: OtcOfferDto,
        quantity: Int,
        pricePerStock: Double,
        premium: Double,
        settlementDate: String
    ) = viewModelScope.launch {
        val body = CounterOtcOfferDto(quantity, pricePerStock, premium, settlementDate)
        when (val result = repository.counter(offer.foreign, offer, body)) {
            is ApiResult.Success -> {
                _events.send(OtcOffersAndContractsEvent.Toast("Kontraponuda poslata."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
        }
    }

    /**
     * Pokrece exercise. Za inter-bank ugovore prati ishod kroz polling
     * `listMyInterContracts` (BE wrapper ne ekspozira pojedinacne SAGA faze).
     * Za intra-bank koristi `/otc/contracts/{id}/saga-status` endpoint.
     */
    fun startExercise(contract: OtcContractDto, buyerAccountId: Long?) {
        _state.update {
            it.copy(
                exerciseInProgress = ExerciseProgress(
                    contractId = contract.id,
                    foreign = contract.foreign,
                    phase = "INITIATED",
                    message = "Pokrecem izvrsenje..."
                )
            )
        }
        viewModelScope.launch {
            when (val result = repository.exercise(contract.foreign, contract, buyerAccountId)) {
                is ApiResult.Success -> {
                    if (contract.foreign) {
                        // Inter-bank: poll listMyInterContracts po foreignId dok status ne pređe u terminal.
                        contract.foreignId?.let { pollInterContract(it) }
                            ?: markAborted("Inter-bank ugovor nema foreignId — ne mogu pratiti SAGA status.")
                    } else {
                        // Intra-bank: ako BE vec vraca EXERCISED, prikazi to; inace polluj saga-status.
                        val returnedStatus = result.data.status
                        if (returnedStatus.equals("EXERCISED", true) || returnedStatus.equals("COMMITTED", true)) {
                            markCommitted()
                        } else {
                            pollIntraSaga(contract.id)
                        }
                    }
                }
                is ApiResult.Failure -> markAborted(result.error.message)
                ApiResult.Loading -> Unit
            }
        }
    }

    fun closeExercise() = viewModelScope.launch {
        _state.update { it.copy(exerciseInProgress = null) }
        refresh()
    }

    private fun markCommitted() {
        _state.update {
            it.copy(
                exerciseInProgress = it.exerciseInProgress?.copy(
                    phase = "COMMITTED",
                    message = "Ugovor je izvrsen."
                )
            )
        }
    }

    private fun markAborted(message: String?) {
        _state.update {
            it.copy(
                exerciseInProgress = it.exerciseInProgress?.copy(
                    phase = "ABORTED",
                    message = message
                )
            )
        }
    }

    private suspend fun pollIntraSaga(contractId: Long) {
        repeat(40) { _ ->
            when (val result = repository.sagaStatusIntra(contractId)) {
                is ApiResult.Success -> {
                    val status: SagaStatusDto = result.data
                    _state.update {
                        it.copy(
                            exerciseInProgress = it.exerciseInProgress?.copy(
                                phase = status.phase,
                                message = status.message
                            )
                        )
                    }
                    if (status.phase in TERMINAL_PHASES) return
                }
                else -> Unit
            }
            delay(2000L)
        }
        _state.update {
            it.copy(
                exerciseInProgress = it.exerciseInProgress?.copy(
                    phase = "STUCK",
                    message = "Status nije potvrdjen u predvidjenom vremenu — banka ce dovrsiti naknadno."
                )
            )
        }
    }

    private suspend fun pollInterContract(foreignId: String) {
        // BE wrapper ne ekspozira pojedinacne SAGA faze, pa imitiramo progress
        // kroz indeks polling-a (faza se aproksimira) dok status contract-a
        // ne predje u EXERCISED (uspeh) ili ABORTED/EXPIRED (neuspeh).
        repeat(40) { tick ->
            when (val result = repository.pollInterContractStatus(foreignId)) {
                is ApiResult.Success -> {
                    val status = result.data
                    val phase = mapInterContractStatusToPhase(status, tick)
                    _state.update {
                        it.copy(
                            exerciseInProgress = it.exerciseInProgress?.copy(
                                phase = phase,
                                message = interStatusMessage(status, tick)
                            )
                        )
                    }
                    when (status.uppercase()) {
                        "EXERCISED" -> { markCommitted(); return }
                        "ABORTED", "EXPIRED" -> { markAborted("Transakcija nije uspela ($status)."); return }
                    }
                }
                else -> Unit
            }
            delay(2000L)
        }
        _state.update {
            it.copy(
                exerciseInProgress = it.exerciseInProgress?.copy(
                    phase = "STUCK",
                    message = "SAGA nije potvrdjena u predvidjenom vremenu — banka ce dovrsiti naknadno."
                )
            )
        }
    }

    /**
     * Aproksimacija faze za inter-bank exercise (5 koraka iz spec-a). BE wrapper
     * ne salje stvarnu fazu pa progress prikazujemo kroz tick brojac (svaki
     * tick je 2 sekunde, faze se menjaju otprilike svakih 4 sekunde).
     */
    private fun mapInterContractStatusToPhase(status: String, tick: Int): String {
        return when (status.uppercase()) {
            "EXERCISED" -> "COMMITTED"
            "ABORTED", "EXPIRED" -> "ABORTED"
            else -> when {
                tick < 2 -> "RESERVE_FUNDS"
                tick < 4 -> "RESERVE_SHARES"
                tick < 6 -> "TRANSFER_FUNDS"
                tick < 8 -> "TRANSFER_OWNERSHIP"
                else -> "INITIATED"
            }
        }
    }

    private fun interStatusMessage(status: String, tick: Int): String? = when (status.uppercase()) {
        "EXERCISED" -> "Ugovor je uspesno izvrsen."
        "ABORTED" -> "Transakcija je opozvana."
        "EXPIRED" -> "Ugovor je istekao pre nego sto je SAGA zavrsena."
        "ACTIVE" -> "Cekam da partner banka dovrsi SAGA korak ${tick + 1}/40..."
        else -> null
    }

    private companion object {
        val TERMINAL_PHASES = setOf("COMMITTED", "ABORTED", "STUCK")
    }
}

enum class OtcTab(val label: String) {
    OffersDomestic("Ponude (domace)"),
    ContractsDomestic("Ugovori (domaci)"),
    OffersForeign("Ponude (inostrane)"),
    ContractsForeign("Ugovori (inostrani)")
}

data class OtcOffersAndContractsState(
    val tab: OtcTab = OtcTab.OffersDomestic,
    val loading: Boolean = false,
    val offers: List<OtcOfferDto> = emptyList(),
    val contracts: List<OtcContractDto> = emptyList(),
    val unreadIntra: Int = 0,
    val unreadInter: Int = 0,
    val error: String? = null,
    val exerciseInProgress: ExerciseProgress? = null
)

data class ExerciseProgress(
    val contractId: Long,
    val foreign: Boolean,
    val phase: String,
    val message: String? = null
)

sealed interface OtcOffersAndContractsEvent {
    data class Toast(val message: String) : OtcOffersAndContractsEvent
}
