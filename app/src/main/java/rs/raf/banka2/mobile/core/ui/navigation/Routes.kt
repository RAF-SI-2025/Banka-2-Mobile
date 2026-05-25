package rs.raf.banka2.mobile.core.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Compose Navigation rute. Svaki ekran je serializable data class
 * (sa eventualnim argumentima) ili data object (kada nema argumenata).
 *
 * NavHost koristi `composable<Route>(...)` overload pa nema parsiranja stringova.
 * Argumenti se cuvaju u savedStateHandle automatski.
 */
object Routes {

    // ─── Auth ─────────────────────────────────────────────
    @Serializable data object Splash
    @Serializable data object Login
    @Serializable data object ForgotPassword
    @Serializable data class ResetPassword(val token: String? = null)
    @Serializable data class ActivateAccount(val token: String? = null)

    // ─── Klijent / opste ──────────────────────────────────
    @Serializable data object Home

    // ─── Celina 2 ─────────────────────────────────────────
    @Serializable data object AccountsList
    @Serializable data class AccountDetails(val accountId: Long)
    @Serializable data class BusinessAccountDetails(val accountId: Long)
    @Serializable data object AccountRequestNew
    @Serializable data object AccountRequestsMy

    @Serializable data object PaymentsNew
    @Serializable data object PaymentsHistory
    @Serializable data class PaymentDetails(val paymentId: Long)
    @Serializable data object Recipients

    @Serializable data object Transfers
    @Serializable data object TransfersHistory

    @Serializable data object Exchange

    @Serializable data object Cards
    @Serializable data object CardRequestNew

    @Serializable data object Loans
    @Serializable data object LoanApply

    @Serializable data object MarginAccounts

    // ─── Celina 3 ─────────────────────────────────────────
    @Serializable data object SecuritiesList
    @Serializable data class SecuritiesDetails(val listingId: Long)
    @Serializable data class CreateOrder(val listingId: Long, val direction: String)
    @Serializable data object MyOrders

    @Serializable data object Portfolio

    // ─── Celina 4 ─────────────────────────────────────────
    @Serializable data object OtcDiscovery
    @Serializable data object OtcOffersAndContracts
    @Serializable data object FundsList
    @Serializable data class FundDetails(val fundId: Long)
    @Serializable data object FundCreate
    @Serializable data object MyFunds

    // ─── Employee/Admin ───────────────────────────────────
    @Serializable data object SupervisorDashboard
    @Serializable data object EmployeesList
    @Serializable data object EmployeeNew
    @Serializable data class EmployeeEdit(val employeeId: Long)
    @Serializable data object EmployeeAccountsPortal
    @Serializable data object EmployeeCardRequests
    @Serializable data object EmployeeAccountRequests
    @Serializable data object EmployeeClientsPortal
    @Serializable data object EmployeeLoanRequests
    @Serializable data object EmployeeAllLoans
    @Serializable data object EmployeeOrders
    @Serializable data object Actuaries
    @Serializable data object TaxPortal
    @Serializable data object ProfitBank
    @Serializable data object ExchangesManagement

    // ─── Mobile-specific ──────────────────────────────────
    @Serializable data object Otp

    // ─── TODO_final C2 #4 — Notifikacije ───────────────────
    @Serializable data object Notifications

    // ─── TODO_final Mobile bonus #7 — Quick Approve ────────
    @Serializable data class QuickApprovePayment(
        val paymentId: Long,
        val notificationCreatedAt: String = ""
    )

    // ─── Stednja (Celina 2 extra) ─────────────────────────
    @Serializable data object SavingsList
    @Serializable data object SavingsNewDeposit
    @Serializable data class SavingsDetails(val depositId: Long)

    // ─── Klijentski extra ekrani ──────────────────────────
    @Serializable data class LoanDetailsRoute(val loanId: Long)

    // ─── Supervizor extra ─────────────────────────────────
    @Serializable data object MarginAccountCreate
    @Serializable data class EmployeeAccountCardsList(val accountId: Long)
    @Serializable data object EmployeeAccountCreate
    @Serializable data class ClientEditRoute(val clientId: Long)
    @Serializable data class MarginTransactionsRoute(val accountId: Long)

    // ─── TODO_final 25.05.2026 ────────────────────────────
    /** B7 / C3 §69 — Audit log portal (supervisor/admin only). */
    @Serializable data object AuditLog
    /** B10 / C4 §13 — OTC istorija pregovora (supervisor/admin only). */
    @Serializable data object OtcNegotiationHistory

    // ─── TODO_final FE2/FE3 26.05.2026 ────────────────────
    /** FE2 / C3 #8 — Watchlist (liste pracenja hartija). */
    @Serializable data object Watchlist
    /** FE2 / C3 #6 — Cenovni alarmi (Price Alert). */
    @Serializable data object PriceAlerts
    /** FE3 / C3 #10 — Trajni nalozi (DCA / RecurringOrder). */
    @Serializable data object RecurringOrders
}
