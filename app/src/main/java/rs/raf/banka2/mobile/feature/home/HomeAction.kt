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
}
