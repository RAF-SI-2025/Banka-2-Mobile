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
import rs.raf.banka2.mobile.core.format.DateFormatter
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.core.storage.OtcStateStore
import rs.raf.banka2.mobile.data.dto.account.AccountDto
import rs.raf.banka2.mobile.data.dto.otc.CounterOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.OtcContractDto
import rs.raf.banka2.mobile.data.dto.otc.OtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.SagaStatusDto
import rs.raf.banka2.mobile.data.repository.AccountRepository
import rs.raf.banka2.mobile.data.repository.OtcRepository
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class OtcOffersAndContractsViewModel @Inject constructor(
    private val repository: OtcRepository,
    private val accountRepository: AccountRepository,
    private val otcStateStore: OtcStateStore,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(OtcOffersAndContractsState())
    val state: StateFlow<OtcOffersAndContractsState> = _state.asStateFlow()

    private val _events = Channel<OtcOffersAndContractsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** Cache poslednje vidjenog timestamp-a per scope da unread brojac ne padne na 0 odmah po setTab. */
    private val cachedLastEntrance = mutableMapOf<String, Long>()

    init {
        refresh()
        // R1-593: ucitaj racune da bi accept/exercise mogli da posalju realni
        // buyerAccountId (BE ga koristi za podmirenje premije/strike-a). Ranije
        // se SLEPO slao null → BE bi uzimao default/grcio se na podmirenje.
        loadAccounts()
    }

    private fun loadAccounts() = viewModelScope.launch {
        when (val result = accountRepository.getMyAccounts()) {
            is ApiResult.Success -> _state.update { st ->
                st.copy(
                    accounts = result.data,
                    selectedAccountId = st.selectedAccountId ?: defaultAccountId(result.data)
                )
            }
            else -> Unit // tihi fail — accept/exercise i dalje rade sa null (BE default)
        }
    }

    /** Prvi aktivan racun (preferira RSD) — razuman default za podmirenje OTC-a. */
    private fun defaultAccountId(accounts: List<AccountDto>): Long? {
        val active = accounts.filter { it.status == null || it.status.equals("ACTIVE", true) }
        return (active.firstOrNull { it.currency.equals("RSD", true) } ?: active.firstOrNull())?.id
    }

    fun selectAccount(accountId: Long?) = _state.update { it.copy(selectedAccountId = accountId) }

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

    /**
     * BE intra OtcOfferDto/OtcContractDto NE salje `myRole` (samo buyerId/sellerId).
     * Derivacija na klijentu: poredjenje sa ulogovanim userId-em. Bez ovoga
     * "Iskoristi" dugme (gejtovano na myRole==BUYER) se nikad ne prikaze. Inter-bank
     * DTO vec ne nosi buyerId u Long obliku (string id-evi) pa ga ostavljamo netaknut.
     */
    private fun OtcOfferDto.withDerivedRole(userId: Long?): OtcOfferDto =
        if (foreign || myRole != null || userId == null) this
        else copy(
            myRole = when (userId) {
                buyerId -> "BUYER"
                sellerId -> "SELLER"
                else -> myRole
            }
        )

    private fun OtcContractDto.withDerivedRole(userId: Long?): OtcContractDto =
        if (foreign || myRole != null || userId == null) this
        else copy(
            myRole = when (userId) {
                buyerId -> "BUYER"
                sellerId -> "SELLER"
                else -> myRole
            }
        )

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
        // R3-1608: BE `lastModifiedAt` je bare LocalDateTime ("2026-06-01T10:30:00")
        // bez offseta — `Instant.parse` zahteva zonu/offset pa je UVEK bacao →
        // unread brojac trajno 0. `DateFormatter.parseDateTime` defenzivno parsira
        // LocalDateTime/OffsetDateTime/LocalDate.
        val dateTime = DateFormatter.parseDateTime(iso) ?: return 0L
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (_state.value.tab) {
            OtcTab.OffersDomestic, OtcTab.OffersForeign -> {
                val inter = _state.value.tab == OtcTab.OffersForeign
                val scope = if (inter) "inter" else "intra"
                when (val result = repository.listOffers(inter)) {
                    is ApiResult.Success -> {
                        val uid = currentUserId()
                        val offers = result.data.map { it.withDerivedRole(uid) }
                        _state.update { it.copy(loading = false, offers = offers) }
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
                    is ApiResult.Success -> {
                        val uid = currentUserId()
                        val contracts = result.data.map { it.withDerivedRole(uid) }
                        _state.update { it.copy(loading = false, contracts = contracts) }
                    }
                    is ApiResult.Failure -> _state.update {
                        it.copy(loading = false, error = result.error.message)
                    }
                    ApiResult.Loading -> Unit
                }
            }
        }
    }

    fun acceptOffer(offer: OtcOfferDto, buyerAccountId: Long? = null) = viewModelScope.launch {
        // R1-593: koristi prosledjeni racun, inace selektovani/default (ne null).
        val accountId = buyerAccountId ?: _state.value.selectedAccountId
        when (val result = repository.accept(offer.foreign, offer, accountId)) {
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
     * Za intra-bank koristi Model-B SAGA orkestrator: exercise vraca
     * `OtcExerciseResultDto` sa `sagaId`-em, koji se polluje preko
     * `GET /otc/saga/{sagaId}` dok SAGA ne dodje do terminala.
     */
    fun startExercise(contract: OtcContractDto, buyerAccountId: Long? = null) {
        // R1-593: exercise mora poslati realni buyerAccountId (BE podmiruje strike
        // sa njega). Ranije je Screen UVEK prosledjivao null → podmirenje bez
        // izabranog racuna. Koristi prosledjeni, inace selektovani/default racun.
        val accountId = buyerAccountId ?: _state.value.selectedAccountId
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
            if (contract.foreign) {
                when (val result = repository.exerciseInter(contract, accountId)) {
                    is ApiResult.Success ->
                        // Inter-bank: poll listMyInterContracts po foreignId dok status ne pređe u terminal.
                        contract.foreignId?.let { pollInterContract(it) }
                            ?: markAborted("Inter-bank ugovor nema foreignId — ne mogu pratiti SAGA status.")
                    is ApiResult.Failure -> markAborted(result.error.message)
                    ApiResult.Loading -> Unit
                }
            } else {
                when (val result = repository.exerciseIntra(contract, accountId)) {
                    is ApiResult.Success -> {
                        val exResult = result.data
                        // BE izvrsava SAGA-u sinhrono — odgovor vec nosi terminalni status.
                        // Odmah reflektujemo poznato stanje, pa pollujemo sagaId za detaljniji progress.
                        applySagaStatus(exResult.sagaStatus, exResult.currentStep)
                        val sagaId = exResult.sagaId
                        if (sagaId.isNullOrBlank()) {
                            // Bez sagaId-a nema sta da se polluje — oslonimo se na vec primenjeni terminal.
                            if (_state.value.exerciseInProgress?.phase !in TERMINAL_PHASES) {
                                markCommitted()
                            }
                        } else if (_state.value.exerciseInProgress?.phase !in TERMINAL_PHASES) {
                            pollIntraSaga(sagaId)
                        }
                    }
                    is ApiResult.Failure -> markAborted(result.error.message)
                    ApiResult.Loading -> Unit
                }
            }
        }
    }

    /**
     * R1-479: rucno odustajanje od (intra) OTC ugovora. Premija se NE vraca (BE rule).
     * Posle uspeha refresh-ujemo da ugovor predje u ABANDONED i izgubi "Iskoristi".
     */
    fun abandonContract(contract: OtcContractDto) = viewModelScope.launch {
        when (val result = repository.abandonContract(contract)) {
            is ApiResult.Success -> {
                _events.send(OtcOffersAndContractsEvent.Toast("Odustali ste od ugovora."))
                refresh()
            }
            is ApiResult.Failure -> _state.update { it.copy(error = result.error.message) }
            ApiResult.Loading -> Unit
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

    private suspend fun pollIntraSaga(sagaId: String) {
        repeat(40) { _ ->
            when (val result = repository.sagaStatusIntra(sagaId)) {
                is ApiResult.Success -> {
                    val saga: SagaStatusDto = result.data
                    applySagaStatus(saga.status, saga.currentStep, saga.log.lastOrNull()?.message)
                    if (_state.value.exerciseInProgress?.phase in TERMINAL_PHASES) return
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

    /**
     * Mapira BE `SagaStatus` (RUNNING / COMPENSATING / COMPENSATED / COMPLETED /
     * FAILED) na UI progress fazu. `currentStep` (1..5) odredjuje koju forward
     * fazu prikazujemo dok je SAGA jos u toku.
     */
    private fun applySagaStatus(sagaStatus: String?, currentStep: Int, message: String? = null) {
        val phase: String
        val phaseMessage: String?
        when (sagaStatus?.uppercase()) {
            "COMPLETED" -> {
                phase = "COMMITTED"
                phaseMessage = "Ugovor je izvrsen."
            }
            "COMPENSATED", "FAILED" -> {
                phase = "ABORTED"
                phaseMessage = message ?: "Transakcija je opozvana — sredstva su vracena."
            }
            "COMPENSATING" -> {
                // R1-271: COMPENSATING NIJE terminal u BE SagaStatus-u (prelazi u
                // COMPENSATED/FAILED). Ranije se mapirao na ABORTED (terminal) →
                // polling je stao DOK je kompenzacija jos trajala → korisnik je video
                // "opozvano" pre nego sto su sredstva stvarno vracena. Drzimo
                // ne-terminalnu fazu da polling nastavi do COMPENSATED/FAILED.
                phase = "COMPENSATING"
                phaseMessage = message ?: "Opozivanje transakcije u toku..."
            }
            else -> {
                // RUNNING ili nepoznato — prikazi forward fazu po currentStep-u.
                phase = forwardPhaseForStep(currentStep)
                phaseMessage = message ?: "Izvrsenje u toku (korak $currentStep/5)..."
            }
        }
        _state.update {
            it.copy(
                exerciseInProgress = it.exerciseInProgress?.copy(
                    phase = phase,
                    message = phaseMessage
                )
            )
        }
    }

    /**
     * Forward faza za prikaz dok je SAGA jos u toku (RUNNING). Nikad ne vraca
     * terminalnu fazu (COMMITTED) — terminal odlucuje SagaStatus, ne korak.
     */
    private fun forwardPhaseForStep(step: Int): String = when (step) {
        in Int.MIN_VALUE..1 -> "RESERVE_FUNDS"
        2 -> "RESERVE_SHARES"
        3 -> "TRANSFER_FUNDS"
        else -> "TRANSFER_OWNERSHIP"
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
    val exerciseInProgress: ExerciseProgress? = null,
    /** R1-593: racuni za podmirenje OTC accept/exercise + trenutno izabrani. */
    val accounts: List<AccountDto> = emptyList(),
    val selectedAccountId: Long? = null
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
