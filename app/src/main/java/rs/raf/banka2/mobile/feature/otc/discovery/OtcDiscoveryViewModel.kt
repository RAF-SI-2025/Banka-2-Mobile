package rs.raf.banka2.mobile.feature.otc.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.otc.CreateOtcOfferDto
import rs.raf.banka2.mobile.data.dto.otc.OtcListingDto
import rs.raf.banka2.mobile.data.repository.OtcRepository
import rs.raf.banka2.mobile.feature.otc.OtcScope
import javax.inject.Inject

@HiltViewModel
class OtcDiscoveryViewModel @Inject constructor(
    private val repository: OtcRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OtcDiscoveryState())
    val state: StateFlow<OtcDiscoveryState> = _state.asStateFlow()

    private val _events = Channel<OtcDiscoveryEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init { refresh() }

    fun setScope(scope: OtcScope) {
        _state.update { it.copy(scope = scope) }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = repository.discover(_state.value.scope.inter)) {
            is ApiResult.Success -> _state.update { it.copy(loading = false, listings = result.data) }
            is ApiResult.Failure -> _state.update {
                it.copy(loading = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }

    fun submitOffer(
        listing: OtcListingDto,
        quantity: Int,
        pricePerStock: Double,
        premium: Double,
        settlementDate: String
    ) = viewModelScope.launch {
        val request = CreateOtcOfferDto(
            listingId = listing.listingId,
            sellerUserId = listing.sellerUserId,
            sellerRole = listing.sellerRole,
            quantity = quantity,
            pricePerStock = pricePerStock,
            premium = premium,
            settlementDate = settlementDate,
            foreign = listing.foreign,
            bankRoutingNumber = listing.bankRoutingNumber,
            foreignSellerPublicId = listing.foreignSellerPublicId,
            foreignListingTicker = if (listing.foreign) listing.ticker else null
        )
        _state.update { it.copy(submitting = true) }
        when (val result = repository.createOffer(_state.value.scope.inter, request)) {
            is ApiResult.Success -> {
                _state.update { it.copy(submitting = false) }
                _events.send(OtcDiscoveryEvent.OfferSent)
            }
            is ApiResult.Failure -> _state.update {
                it.copy(submitting = false, error = result.error.message)
            }
            ApiResult.Loading -> Unit
        }
    }
}

data class OtcDiscoveryState(
    val scope: OtcScope = OtcScope.Domestic,
    val loading: Boolean = false,
    val listings: List<OtcListingDto> = emptyList(),
    val error: String? = null,
    val submitting: Boolean = false
)

sealed interface OtcDiscoveryEvent {
    data object OfferSent : OtcDiscoveryEvent
}
