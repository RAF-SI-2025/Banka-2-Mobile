package rs.raf.banka2.mobile.feature.supervisor.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.raf.banka2.mobile.core.network.ApiResult
import rs.raf.banka2.mobile.data.dto.order.OrderDto
import rs.raf.banka2.mobile.data.repository.EmployeeAdminRepository
import rs.raf.banka2.mobile.data.repository.OrderRepository
import rs.raf.banka2.mobile.data.repository.PortfolioRepository
import javax.inject.Inject

@HiltViewModel
class SupervisorDashboardViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val employeeRepository: EmployeeAdminRepository,
    private val portfolioRepository: PortfolioRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SupervisorDashboardState())
    val state: StateFlow<SupervisorDashboardState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { loadOrders() }
        viewModelScope.launch { loadEmployeeStats() }
        viewModelScope.launch { loadPortfolioSummary() }
    }

    private suspend fun loadOrders() {
        when (val result = orderRepository.listAll(page = 0, size = 5)) {
            is ApiResult.Success -> {
                val all = result.data
                _state.update {
                    it.copy(
                        recentOrders = all,
                        pending = all.count { o -> o.status == "PENDING" },
                        approved = all.count { o -> o.status == "APPROVED" },
                        done = all.count { o -> o.status == "DONE" }
                    )
                }
            }
            else -> Unit
        }
    }

    private suspend fun loadEmployeeStats() {
        when (val result = employeeRepository.list()) {
            is ApiResult.Success -> _state.update {
                it.copy(
                    employeesActive = result.data.count { e -> e.active != false },
                    employeesTotal = result.data.size
                )
            }
            else -> Unit
        }
    }

    private suspend fun loadPortfolioSummary() {
        when (val result = portfolioRepository.summary()) {
            is ApiResult.Success -> _state.update {
                it.copy(
                    portfolioValue = result.data.totalValue,
                    portfolioProfit = result.data.totalProfit,
                    taxOwed = result.data.taxOwed
                )
            }
            else -> Unit
        }
    }
}

data class SupervisorDashboardState(
    val recentOrders: List<OrderDto> = emptyList(),
    val pending: Int = 0,
    val approved: Int = 0,
    val done: Int = 0,
    val employeesActive: Int = 0,
    val employeesTotal: Int = 0,
    val portfolioValue: Double = 0.0,
    val portfolioProfit: Double = 0.0,
    val taxOwed: Double? = null
)
