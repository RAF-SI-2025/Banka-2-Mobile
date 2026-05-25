package rs.raf.banka2.mobile.feature.home

/** Akcije koje Home moze da emituje — parent ih pretvara u navigation. */
sealed interface HomeAction {
    data object OpenAccountList : HomeAction
    data class OpenAccount(val accountId: Long) : HomeAction
    data object NewPayment : HomeAction
    data object NewTransfer : HomeAction
    data object OpenExchange : HomeAction
    data object OpenOtp : HomeAction
    data object OpenCards : HomeAction
    data object OpenLoans : HomeAction
    data object OpenRecipients : HomeAction
    data object OpenPaymentHistory : HomeAction
    data object OpenSecurities : HomeAction
    data object OpenPortfolio : HomeAction
    data object OpenMargin : HomeAction
    data object OpenOtc : HomeAction
    data object OpenFunds : HomeAction
    data object OpenProfitBank : HomeAction
    data object OpenSupervisorOrders : HomeAction
    data object OpenActuaries : HomeAction
    data object OpenTax : HomeAction
    data object OpenExchangesManagement : HomeAction
    data object OpenEmployees : HomeAction
    data object OpenAccountRequestsMy : HomeAction
    data object OpenSupervisorDashboard : HomeAction
    data object OpenEmployeeAccounts : HomeAction
    data object OpenEmployeeClients : HomeAction
    data object OpenEmployeeCardRequests : HomeAction
    data object OpenEmployeeAccountRequests : HomeAction
    data object OpenEmployeeLoanRequests : HomeAction
    data object OpenEmployeeAllLoans : HomeAction
    data object OpenMarginCreate : HomeAction
    data object OpenSavings : HomeAction
    data object OpenNotifications : HomeAction
    /** B7 / C3 §69 — Audit log portal (supervisor/admin only). */
    data object OpenAuditLog : HomeAction
    /** B10 / C4 §13 — OTC istorija pregovora (supervisor/admin only). */
    data object OpenOtcNegotiationHistory : HomeAction
    /** FE2 / C3 #8 — Watchlist. */
    data object OpenWatchlist : HomeAction
    /** FE2 / C3 #6 — Cenovni alarmi. */
    data object OpenPriceAlerts : HomeAction
    /** FE3 / C3 #10 — DCA / RecurringOrder. */
    data object OpenRecurringOrders : HomeAction
}
