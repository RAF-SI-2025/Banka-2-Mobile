package rs.raf.banka2.mobile.feature.pricealerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.pricealert.PriceAlertCondition
import rs.raf.banka2.mobile.data.dto.pricealert.PriceAlertDto
import rs.raf.banka2.mobile.data.repository.PriceAlertRepository
import java.math.BigDecimal
import javax.inject.Inject

/**
 * [FE2 Mobile port — Price Alert Dialog] VM koji handluje formu za kreiranje
 * cenovnog alarma. Zove se iz SecuritiesDetailsScreen ("Postavi cenovni alarm").
 *
 * Validacija (pure, testabilno):
 *  - threshold mora biti pozitivan broj
 *  - threshold mora biti razlicit od trenutne cene (inace alarm bi se
 *    okidnuo odmah na sledecem scheduler tick-u)
 *
 * Predicted % od trenutne cene se racuna realtime — UI prikazuje "+5.2%" ili
 * "-3.1%" da korisnik vidi koliko je daleko prag od trenutka kreiranja.
 */
@HiltViewModel
class PriceAlertDialogViewModel @Inject constructor(
    private val repository: PriceAlertRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PriceAlertDialogState())
    val state: StateFlow<PriceAlertDialogState> = _state.asStateFlow()

    fun reset(listingId: Long, ticker: String, currentPrice: Double?) {
        _state.update {
            PriceAlertDialogState(
                listingId = listingId,
                ticker = ticker,
                currentPrice = currentPrice,
            )
        }
    }

    fun setCondition(condition: PriceAlertCondition) =
        _state.update { it.copy(condition = condition) }

    fun setThresholdText(text: String) {
        // Filtriraj sve sto nije cifra ili tacka/zarez
        val normalized = text.replace(',', '.').trim()
        val parsed = normalized.toBigDecimalOrNull()
        _state.update { it.copy(thresholdText = text, threshold = parsed) }
    }

    fun submit() {
        val s = _state.value
        val listingId = s.listingId ?: return
        val threshold = s.threshold
        val validation = validate(threshold, s.currentPrice, s.condition)
        if (validation != null) {
            _state.update { it.copy(error = validation) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            when (val result = repository.create(listingId, s.condition, threshold!!)) {
                is ApiResult.Success -> _state.update {
                    it.copy(submitting = false, createdAlert = result.data)
                }
                is ApiResult.Failure -> _state.update {
                    it.copy(submitting = false, error = result.error.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, createdAlert = null) }

    companion object {
        /**
         * Pure validacija — vraca user-facing poruku ako threshold nije validan,
         * inace null. Testirano u `PriceAlertDialogValidationTest`.
         *
         * R1-598: validira i SMER. Ranije se proveravalo samo da je prag != trenutne
         * cene, pa je ABOVE-alarm sa pragom ISPOD trenutne cene (ili BELOW iznad)
         * bio prihvacen — takav alarm se okida ODMAH na sledecem scheduler tick-u
         * (besmislen). Sada:
         *   - ABOVE  → prag MORA biti > trenutne cene
         *   - BELOW  → prag MORA biti < trenutne cene
         */
        fun validate(
            threshold: BigDecimal?,
            currentPrice: Double?,
            condition: PriceAlertCondition,
        ): String? {
            if (threshold == null || threshold <= BigDecimal.ZERO) {
                return "Prag mora biti pozitivan broj."
            }
            if (currentPrice != null && currentPrice > 0.0) {
                val t = threshold.toDouble()
                val diff = t - currentPrice
                if (kotlin.math.abs(diff) < 0.0001) {
                    return "Prag mora biti razlicit od trenutne cene (alarm bi se okinuo odmah)."
                }
                when (condition) {
                    PriceAlertCondition.ABOVE -> if (t <= currentPrice) {
                        return "Za alarm \"iznad\" prag mora biti veci od trenutne cene."
                    }
                    PriceAlertCondition.BELOW -> if (t >= currentPrice) {
                        return "Za alarm \"ispod\" prag mora biti manji od trenutne cene."
                    }
                }
            }
            return null
        }

        /**
         * Procenat razlike threshold-a od trenutne cene. Vraca null ako
         * currentPrice nije > 0. Testirano.
         */
        fun percentDifference(threshold: BigDecimal?, currentPrice: Double?): Double? {
            if (threshold == null || currentPrice == null || currentPrice <= 0.0) return null
            return ((threshold.toDouble() - currentPrice) / currentPrice) * 100.0
        }
    }
}

data class PriceAlertDialogState(
    val listingId: Long? = null,
    val ticker: String = "",
    val currentPrice: Double? = null,
    val condition: PriceAlertCondition = PriceAlertCondition.ABOVE,
    val thresholdText: String = "",
    val threshold: BigDecimal? = null,
    val submitting: Boolean = false,
    val error: String? = null,
    val createdAlert: PriceAlertDto? = null,
)
