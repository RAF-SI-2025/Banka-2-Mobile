package rs.raf.banka2.mobile.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import rs.raf.banka2.mobile.feature.accounts.AccountDetailsScreen
import rs.raf.banka2.mobile.feature.accounts.AccountRequestNewScreen
import rs.raf.banka2.mobile.feature.accounts.AccountsListScreen
import rs.raf.banka2.mobile.feature.accounts.business.BusinessAccountDetailsScreen
import rs.raf.banka2.mobile.feature.actuaries.ActuariesScreen
import rs.raf.banka2.mobile.feature.audit.AuditLogScreen
import rs.raf.banka2.mobile.feature.clients.edit.ClientEditScreen
import rs.raf.banka2.mobile.feature.margin.transactions.MarginTransactionsScreen
import rs.raf.banka2.mobile.feature.otc.history.OtcNegotiationHistoryScreen
import rs.raf.banka2.mobile.feature.auth.activate.ActivateAccountScreen
import rs.raf.banka2.mobile.feature.auth.forgot.ForgotPasswordScreen
import rs.raf.banka2.mobile.feature.auth.login.LoginScreen
import rs.raf.banka2.mobile.feature.auth.reset.ResetPasswordScreen
import rs.raf.banka2.mobile.feature.cards.CardRequestNewScreen
import rs.raf.banka2.mobile.feature.cards.CardsScreen
import rs.raf.banka2.mobile.feature.exchange.ExchangeScreen
import rs.raf.banka2.mobile.feature.exchanges.ExchangesScreen
import rs.raf.banka2.mobile.feature.funds.create.CreateFundScreen
import rs.raf.banka2.mobile.feature.funds.details.FundDetailsScreen
import rs.raf.banka2.mobile.feature.funds.discovery.FundsDiscoveryScreen
import rs.raf.banka2.mobile.feature.funds.my.MyFundsScreen
import rs.raf.banka2.mobile.feature.home.HomeAction
import rs.raf.banka2.mobile.feature.home.HomeScreen
import rs.raf.banka2.mobile.feature.loans.LoanApplyScreen
import rs.raf.banka2.mobile.feature.loans.LoansScreen
import rs.raf.banka2.mobile.feature.margin.MarginScreen
import rs.raf.banka2.mobile.feature.notifications.NotificationTarget
import rs.raf.banka2.mobile.feature.notifications.NotificationsScreen
import rs.raf.banka2.mobile.feature.orders.create.CreateOrderScreen
import rs.raf.banka2.mobile.feature.orders.my.MyOrdersScreen
import rs.raf.banka2.mobile.feature.orders.supervisor.OrdersSupervisorScreen
import rs.raf.banka2.mobile.feature.otc.contracts.OtcOffersAndContractsScreen
import rs.raf.banka2.mobile.feature.otc.discovery.OtcDiscoveryScreen
import rs.raf.banka2.mobile.feature.otp.OtpScreen
import rs.raf.banka2.mobile.feature.profitbank.ProfitBankScreen
import rs.raf.banka2.mobile.feature.accounts.requestsmy.MyAccountRequestsScreen
import rs.raf.banka2.mobile.feature.clients.portal.ClientsPortalScreen
import rs.raf.banka2.mobile.feature.employees.create.EmployeeCreateScreen
import rs.raf.banka2.mobile.feature.employees.edit.EmployeeEditScreen
import rs.raf.banka2.mobile.feature.employees.list.EmployeeListScreen
import rs.raf.banka2.mobile.feature.loans.details.LoanDetailsScreen
import rs.raf.banka2.mobile.feature.payments.create.NewPaymentScreen
import rs.raf.banka2.mobile.feature.payments.details.PaymentDetailsScreen
import rs.raf.banka2.mobile.feature.pricealerts.PriceAlertsScreen
import rs.raf.banka2.mobile.feature.recurringorders.RecurringOrdersScreen
import rs.raf.banka2.mobile.feature.watchlist.WatchlistScreen
import rs.raf.banka2.mobile.feature.payments.quickapprove.QuickApproveScreen
import rs.raf.banka2.mobile.feature.payments.history.PaymentHistoryScreen
import rs.raf.banka2.mobile.feature.payments.recipients.RecipientsScreen
import rs.raf.banka2.mobile.feature.supervisor.accountcards.AccountCardsScreen
import rs.raf.banka2.mobile.feature.supervisor.accountcreate.CreateAccountForClientScreen
import rs.raf.banka2.mobile.feature.supervisor.accountrequests.AccountRequestsScreen
import rs.raf.banka2.mobile.feature.supervisor.accounts.AllAccountsScreen
import rs.raf.banka2.mobile.feature.supervisor.allloans.AllLoansScreen
import rs.raf.banka2.mobile.feature.supervisor.cardrequests.CardRequestsScreen
import rs.raf.banka2.mobile.feature.supervisor.dashboard.SupervisorDashboardScreen
import rs.raf.banka2.mobile.feature.supervisor.loanrequests.LoanRequestsScreen
import rs.raf.banka2.mobile.feature.supervisor.margincreate.CreateMarginScreen
import rs.raf.banka2.mobile.feature.portfolio.PortfolioScreen
import rs.raf.banka2.mobile.feature.securities.details.SecuritiesDetailsScreen
import rs.raf.banka2.mobile.feature.securities.list.SecuritiesListScreen
import rs.raf.banka2.mobile.feature.splash.SplashScreen
import rs.raf.banka2.mobile.feature.tax.TaxScreen
import rs.raf.banka2.mobile.feature.savings.details.SavingsDetailsScreen
import rs.raf.banka2.mobile.feature.savings.list.SavingsListScreen
import rs.raf.banka2.mobile.feature.savings.newdeposit.SavingsNewDepositScreen
import rs.raf.banka2.mobile.feature.transfers.create.NewTransferScreen
import rs.raf.banka2.mobile.feature.transfers.history.TransferHistoryScreen

/**
 * Glavni navigation graph. Faza 3 dodaje sve berzanske rute (Securities,
 * Orders, Portfolio) + supervisor portale (Orderi, Aktuari, Porez, Berze).
 */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backToLogin: () -> Unit = {
        navController.navigate(Routes.Login) {
            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.Splash
    ) {
        addAuthRoutes(navController, backToLogin)
        addClientRoutes(navController, backToLogin)
        addTradingRoutes(navController)
        addOtcAndFundsRoutes(navController)
        addEmployeeRoutes(navController)
    }
}

private fun androidx.navigation.NavGraphBuilder.addAuthRoutes(
    navController: NavHostController,
    backToLogin: () -> Unit
) {
    composable<Routes.Splash> {
        SplashScreen(
            onGoToLogin = backToLogin,
            onGoToHome = {
                navController.navigate(Routes.Home) {
                    popUpTo(Routes.Splash) { inclusive = true }
                }
            }
        )
    }
    composable<Routes.Login> {
        LoginScreen(
            onLoggedIn = {
                navController.navigate(Routes.Home) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                }
            },
            onForgotPassword = { navController.navigate(Routes.ForgotPassword) },
            onActivateAccount = { navController.navigate(Routes.ActivateAccount(token = null)) }
        )
    }
    composable<Routes.ForgotPassword> {
        ForgotPasswordScreen(onBack = { navController.popBackStack() })
    }
    composable<Routes.ResetPassword> {
        ResetPasswordScreen(onBack = { navController.popBackStack() }, onSuccess = backToLogin)
    }
    composable<Routes.ActivateAccount> {
        ActivateAccountScreen(onBack = { navController.popBackStack() }, onSuccess = backToLogin)
    }
}

private fun androidx.navigation.NavGraphBuilder.addClientRoutes(
    navController: NavHostController,
    backToLogin: () -> Unit
) {
    composable<Routes.Home> {
        HomeScreen(
            onLogout = backToLogin,
            onNavigate = { action -> navController.handleHomeAction(action) }
        )
    }

    composable<Routes.AccountsList> {
        AccountsListScreen(
            onBack = { navController.popBackStack() },
            onAccountClick = { id, isBusiness ->
                if (isBusiness) navController.navigate(Routes.BusinessAccountDetails(id))
                else navController.navigate(Routes.AccountDetails(id))
            },
            onNewRequest = { navController.navigate(Routes.AccountRequestNew) }
        )
    }
    composable<Routes.AccountDetails> {
        AccountDetailsScreen(onBack = { navController.popBackStack() })
    }
    composable<Routes.BusinessAccountDetails> {
        BusinessAccountDetailsScreen(onBack = { navController.popBackStack() })
    }
    composable<Routes.AccountRequestNew> {
        AccountRequestNewScreen(
            onBack = { navController.popBackStack() },
            onSubmitted = { navController.popBackStack() }
        )
    }
    composable<Routes.PaymentsNew> {
        NewPaymentScreen(
            onBack = { navController.popBackStack() },
            onSuccess = {
                navController.navigate(Routes.PaymentsHistory) {
                    popUpTo(Routes.PaymentsNew) { inclusive = true }
                }
            }
        )
    }
    composable<Routes.PaymentsHistory> {
        PaymentHistoryScreen(
            onBack = { navController.popBackStack() },
            onPaymentClick = { id -> navController.navigate(Routes.PaymentDetails(id)) }
        )
    }
    composable<Routes.PaymentDetails> {
        PaymentDetailsScreen(onBack = { navController.popBackStack() })
    }
    composable<Routes.Recipients> { RecipientsScreen(onBack = { navController.popBackStack() }) }
    composable<Routes.AccountRequestsMy> {
        MyAccountRequestsScreen(onBack = { navController.popBackStack() })
    }
    composable<Routes.Transfers> {
        NewTransferScreen(
            onBack = { navController.popBackStack() },
            onSuccess = {
                navController.navigate(Routes.TransfersHistory) {
                    popUpTo(Routes.Transfers) { inclusive = true }
                }
            }
        )
    }
    composable<Routes.TransfersHistory> { TransferHistoryScreen(onBack = { navController.popBackStack() }) }
    composable<Routes.Exchange> { ExchangeScreen(onBack = { navController.popBackStack() }) }
    composable<Routes.Cards> {
        CardsScreen(
            onBack = { navController.popBackStack() },
            onRequestNewCard = { navController.navigate(Routes.CardRequestNew) }
        )
    }
    composable<Routes.CardRequestNew> {
        CardRequestNewScreen(onBack = { navController.popBackStack() }, onSubmitted = { navController.popBackStack() })
    }
    composable<Routes.Loans> {
        LoansScreen(
            onBack = { navController.popBackStack() },
            onApply = { navController.navigate(Routes.LoanApply) },
            onLoanClick = { id -> navController.navigate(Routes.LoanDetailsRoute(id)) }
        )
    }
    composable<Routes.LoanApply> {
        LoanApplyScreen(onBack = { navController.popBackStack() }, onSubmitted = { navController.popBackStack() })
    }
    composable<Routes.LoanDetailsRoute> {
        LoanDetailsScreen(onBack = { navController.popBackStack() })
    }
    composable<Routes.SavingsList> {
        SavingsListScreen(
            onBack = { navController.popBackStack() },
            onNewDeposit = { navController.navigate(Routes.SavingsNewDeposit) },
            onDepositClick = { id -> navController.navigate(Routes.SavingsDetails(id)) }
        )
    }
    composable<Routes.SavingsNewDeposit> {
        SavingsNewDepositScreen(
            onBack = { navController.popBackStack() },
            onSuccess = {
                navController.navigate(Routes.SavingsList) {
                    popUpTo(Routes.SavingsNewDeposit) { inclusive = true }
                }
            }
        )
    }
    composable<Routes.SavingsDetails> {
        SavingsDetailsScreen(onBack = { navController.popBackStack() })
    }
    // P1-fe-mobile-authz-1 (1755/1388): marzni racuni su trgovinski feature — NO_AGENT.
    composable<Routes.MarginAccounts> {
        RouteGuard(RouteAccess.NO_AGENT) {
            MarginScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
    composable<Routes.MarginTransactionsRoute> {
        RouteGuard(RouteAccess.NO_AGENT) {
            MarginTransactionsScreen(onBack = { navController.popBackStack() })
        }
    }
    composable<Routes.Otp> { OtpScreen(onBack = { navController.popBackStack() }) }

    // TODO_final C2 #4 — Notifications inbox.
    composable<Routes.Notifications> {
        NotificationsScreen(
            onBack = { navController.popBackStack() },
            onTarget = { target -> navController.handleNotificationTarget(target) }
        )
    }

    // TODO_final Mobile bonus #7 — Quick Approve ekran (pun produkcioni UI).
    composable<Routes.QuickApprovePayment> {
        QuickApproveScreen(onBack = { navController.popBackStack() })
    }

    // TODO_final FE2 #8 — Watchlist (liste pracenja).
    composable<Routes.Watchlist> {
        WatchlistScreen(
            onBack = { navController.popBackStack() },
            onTradeListing = { listingId ->
                navController.navigate(Routes.SecuritiesDetails(listingId))
            }
        )
    }

    // TODO_final FE2 #6 — Cenovni alarmi.
    composable<Routes.PriceAlerts> {
        PriceAlertsScreen(
            onBack = { navController.popBackStack() },
            onOpenSecurities = { navController.navigate(Routes.SecuritiesList) }
        )
    }

    // TODO_final FE3 #10 — DCA / Trajni nalozi.
    composable<Routes.RecurringOrders> {
        RecurringOrdersScreen(onBack = { navController.popBackStack() })
    }
}

private fun NavHostController.handleNotificationTarget(target: NotificationTarget) {
    when (target) {
        NotificationTarget.Payments -> navigate(Routes.PaymentsHistory)
        NotificationTarget.Orders -> navigate(Routes.MyOrders)
        NotificationTarget.Otc -> navigate(Routes.OtcOffersAndContracts)
        NotificationTarget.Funds -> navigate(Routes.FundsList)
        is NotificationTarget.Fund -> navigate(Routes.FundDetails(target.fundId))
        NotificationTarget.Cards -> navigate(Routes.Cards)
        NotificationTarget.Loans -> navigate(Routes.Loans)
        NotificationTarget.Accounts -> navigate(Routes.AccountsList)
        NotificationTarget.RecurringOrders -> navigate(Routes.RecurringOrders)
        NotificationTarget.PriceAlerts -> navigate(Routes.PriceAlerts)
        is NotificationTarget.QuickApprovePayment -> navigate(
            Routes.QuickApprovePayment(
                paymentId = target.paymentId,
                notificationCreatedAt = target.notificationCreatedAt
            )
        )
    }
}

private fun androidx.navigation.NavGraphBuilder.addTradingRoutes(navController: NavHostController) {
    // P1-fe-mobile-authz-1 (1755): trgovinske rute su NO_AGENT (agent nema
    // berzanski pristup), a kreiranje naloga je TRADE (admin/supervizor ili
    // klijent sa TRADE_STOCKS). RouteGuard montira ekran samo ako sesija ima
    // dozvoljenu rolu — inace placeholder "nemate pristup" (fail-closed).
    composable<Routes.SecuritiesList> {
        RouteGuard(RouteAccess.NO_AGENT) {
            SecuritiesListScreen(
                onBack = { navController.popBackStack() },
                onListingClick = { listingId -> navController.navigate(Routes.SecuritiesDetails(listingId)) }
            )
        }
    }
    composable<Routes.SecuritiesDetails> {
        RouteGuard(RouteAccess.NO_AGENT) {
            SecuritiesDetailsScreen(
                onBack = { navController.popBackStack() },
                onOrder = { id, direction -> navController.navigate(Routes.CreateOrder(id, direction)) }
            )
        }
    }
    composable<Routes.CreateOrder> {
        RouteGuard(RouteAccess.TRADE) {
            CreateOrderScreen(
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(Routes.MyOrders) {
                        popUpTo(Routes.SecuritiesList) { inclusive = false }
                    }
                }
            )
        }
    }
    composable<Routes.MyOrders> {
        RouteGuard(RouteAccess.NO_AGENT) {
            MyOrdersScreen(onBack = { navController.popBackStack() })
        }
    }
    composable<Routes.Portfolio> {
        RouteGuard(RouteAccess.NO_AGENT) {
            PortfolioScreen(
                onBack = { navController.popBackStack() },
                onSell = { listingId -> navController.navigate(Routes.CreateOrder(listingId, "SELL")) }
            )
        }
    }
}

private fun androidx.navigation.NavGraphBuilder.addEmployeeRoutes(navController: NavHostController) {
    // P1-fe-mobile-authz-1 (1755): svaki employee/supervisor/admin ekran je
    // RouteGuard-ovan po roli (fail-closed). Ranije su sve rute bile globalno
    // registrovane bez provere → deep-link/notif-target je montirao tudji ekran.
    // SUPERVISOR-only: Orderi/Aktuari/Porez/ProfitBank/Dashboard/Audit/OTC-istorija.
    composable<Routes.EmployeeOrders> {
        RouteGuard(RouteAccess.SUPERVISOR) { OrdersSupervisorScreen(onBack = { navController.popBackStack() }) }
    }
    composable<Routes.Actuaries> {
        RouteGuard(RouteAccess.SUPERVISOR) { ActuariesScreen(onBack = { navController.popBackStack() }) }
    }
    composable<Routes.TaxPortal> {
        RouteGuard(RouteAccess.SUPERVISOR) { TaxScreen(onBack = { navController.popBackStack() }) }
    }
    composable<Routes.ExchangesManagement> {
        RouteGuard(RouteAccess.EMPLOYEE) { ExchangesScreen(onBack = { navController.popBackStack() }) }
    }
    composable<Routes.ProfitBank> {
        RouteGuard(RouteAccess.SUPERVISOR) { ProfitBankScreen(onBack = { navController.popBackStack() }) }
    }

    composable<Routes.SupervisorDashboard> {
        RouteGuard(RouteAccess.SUPERVISOR) {
            SupervisorDashboardScreen(onBack = { navController.popBackStack() })
        }
    }
    composable<Routes.EmployeesList> {
        RouteGuard(RouteAccess.ADMIN) {
            EmployeeListScreen(
                onBack = { navController.popBackStack() },
                onEmployeeClick = { id -> navController.navigate(Routes.EmployeeEdit(id)) },
                onCreateNew = { navController.navigate(Routes.EmployeeNew) }
            )
        }
    }
    composable<Routes.EmployeeNew> {
        RouteGuard(RouteAccess.ADMIN) {
            EmployeeCreateScreen(
                onBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack() }
            )
        }
    }
    composable<Routes.EmployeeEdit> {
        RouteGuard(RouteAccess.ADMIN) {
            EmployeeEditScreen(onBack = { navController.popBackStack() })
        }
    }
    composable<Routes.EmployeeClientsPortal> {
        RouteGuard(RouteAccess.EMPLOYEE) {
            ClientsPortalScreen(
                onBack = { navController.popBackStack() },
                onClientClick = { id -> navController.navigate(Routes.ClientEditRoute(id)) }
            )
        }
    }
    composable<Routes.ClientEditRoute> {
        RouteGuard(RouteAccess.EMPLOYEE) {
            ClientEditScreen(onBack = { navController.popBackStack() })
        }
    }
    composable<Routes.EmployeeAccountsPortal> {
        RouteGuard(RouteAccess.EMPLOYEE) {
            AllAccountsScreen(
                onBack = { navController.popBackStack() },
                onCreateNew = { navController.navigate(Routes.EmployeeAccountCreate) },
                onAccountClick = { id -> navController.navigate(Routes.EmployeeAccountCardsList(id)) }
            )
        }
    }
    composable<Routes.EmployeeAccountCreate> {
        RouteGuard(RouteAccess.EMPLOYEE) {
            CreateAccountForClientScreen(
                onBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack() }
            )
        }
    }
    composable<Routes.EmployeeAccountCardsList> {
        RouteGuard(RouteAccess.EMPLOYEE) {
            AccountCardsScreen(onBack = { navController.popBackStack() })
        }
    }
    composable<Routes.EmployeeCardRequests> {
        RouteGuard(RouteAccess.EMPLOYEE) {
            CardRequestsScreen(onBack = { navController.popBackStack() })
        }
    }
    composable<Routes.EmployeeAccountRequests> {
        RouteGuard(RouteAccess.EMPLOYEE) {
            AccountRequestsScreen(onBack = { navController.popBackStack() })
        }
    }
    composable<Routes.EmployeeLoanRequests> {
        RouteGuard(RouteAccess.EMPLOYEE) {
            LoanRequestsScreen(onBack = { navController.popBackStack() })
        }
    }
    composable<Routes.EmployeeAllLoans> {
        RouteGuard(RouteAccess.EMPLOYEE) {
            AllLoansScreen(onBack = { navController.popBackStack() })
        }
    }
    composable<Routes.MarginAccountCreate> {
        RouteGuard(RouteAccess.EMPLOYEE) {
            CreateMarginScreen(
                onBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack() }
            )
        }
    }
    // B7 / Spec C3 §69 — Audit log portal (supervisor/admin only).
    composable<Routes.AuditLog> {
        RouteGuard(RouteAccess.SUPERVISOR) {
            AuditLogScreen(onBack = { navController.popBackStack() })
        }
    }
    // B10 / Spec C4 §13 — OTC istorija pregovora (supervisor/admin only).
    composable<Routes.OtcNegotiationHistory> {
        RouteGuard(RouteAccess.SUPERVISOR) {
            OtcNegotiationHistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun androidx.navigation.NavGraphBuilder.addOtcAndFundsRoutes(navController: NavHostController) {
    // P1-fe-mobile-authz-1 (1755): OTC tok je NO_AGENT (§137-141). Fond
    // discovery/details/MyFunds su za SVE role (discovery & details), pa nisu
    // guard-ovani; kreiranje fonda je SUPERVISOR-only.
    composable<Routes.OtcDiscovery> {
        RouteGuard(RouteAccess.NO_AGENT) {
            OtcDiscoveryScreen(
                onBack = { navController.popBackStack() },
                onOpenOffersAndContracts = { navController.navigate(Routes.OtcOffersAndContracts) }
            )
        }
    }
    composable<Routes.OtcOffersAndContracts> {
        RouteGuard(RouteAccess.NO_AGENT) {
            OtcOffersAndContractsScreen(onBack = { navController.popBackStack() })
        }
    }
    composable<Routes.FundsList> {
        FundsDiscoveryScreen(
            onBack = { navController.popBackStack() },
            onFundClick = { id -> navController.navigate(Routes.FundDetails(id)) },
            onCreateFund = { navController.navigate(Routes.FundCreate) },
            onMyFunds = { navController.navigate(Routes.MyFunds) }
        )
    }
    composable<Routes.FundDetails> {
        FundDetailsScreen(onBack = { navController.popBackStack() })
    }
    composable<Routes.FundCreate> {
        RouteGuard(RouteAccess.SUPERVISOR) {
            CreateFundScreen(
                onBack = { navController.popBackStack() },
                onCreated = { id ->
                    navController.navigate(Routes.FundDetails(id)) {
                        popUpTo(Routes.FundCreate) { inclusive = true }
                    }
                }
            )
        }
    }
    composable<Routes.MyFunds> {
        MyFundsScreen(
            onBack = { navController.popBackStack() },
            onFundClick = { id -> navController.navigate(Routes.FundDetails(id)) }
        )
    }
}

private fun NavHostController.handleHomeAction(action: HomeAction) {
    when (action) {
        HomeAction.OpenAccountList -> navigate(Routes.AccountsList)
        is HomeAction.OpenAccount -> navigate(Routes.AccountDetails(action.accountId))
        HomeAction.NewPayment -> navigate(Routes.PaymentsNew)
        HomeAction.NewTransfer -> navigate(Routes.Transfers)
        HomeAction.OpenExchange -> navigate(Routes.Exchange)
        HomeAction.OpenOtp -> navigate(Routes.Otp)
        HomeAction.OpenCards -> navigate(Routes.Cards)
        HomeAction.OpenLoans -> navigate(Routes.Loans)
        HomeAction.OpenRecipients -> navigate(Routes.Recipients)
        HomeAction.OpenPaymentHistory -> navigate(Routes.PaymentsHistory)
        HomeAction.OpenSecurities -> navigate(Routes.SecuritiesList)
        HomeAction.OpenPortfolio -> navigate(Routes.Portfolio)
        HomeAction.OpenMargin -> navigate(Routes.MarginAccounts)
        HomeAction.OpenOtc -> navigate(Routes.OtcDiscovery)
        HomeAction.OpenFunds -> navigate(Routes.FundsList)
        HomeAction.OpenProfitBank -> navigate(Routes.ProfitBank)
        HomeAction.OpenSupervisorOrders -> navigate(Routes.EmployeeOrders)
        HomeAction.OpenActuaries -> navigate(Routes.Actuaries)
        HomeAction.OpenTax -> navigate(Routes.TaxPortal)
        HomeAction.OpenExchangesManagement -> navigate(Routes.ExchangesManagement)
        HomeAction.OpenEmployees -> navigate(Routes.EmployeesList)
        HomeAction.OpenAccountRequestsMy -> navigate(Routes.AccountRequestsMy)
        HomeAction.OpenSupervisorDashboard -> navigate(Routes.SupervisorDashboard)
        HomeAction.OpenEmployeeAccounts -> navigate(Routes.EmployeeAccountsPortal)
        HomeAction.OpenEmployeeClients -> navigate(Routes.EmployeeClientsPortal)
        HomeAction.OpenEmployeeCardRequests -> navigate(Routes.EmployeeCardRequests)
        HomeAction.OpenEmployeeAccountRequests -> navigate(Routes.EmployeeAccountRequests)
        HomeAction.OpenEmployeeLoanRequests -> navigate(Routes.EmployeeLoanRequests)
        HomeAction.OpenEmployeeAllLoans -> navigate(Routes.EmployeeAllLoans)
        HomeAction.OpenMarginCreate -> navigate(Routes.MarginAccountCreate)
        HomeAction.OpenSavings -> navigate(Routes.SavingsList)
        HomeAction.OpenNotifications -> navigate(Routes.Notifications)
        HomeAction.OpenAuditLog -> navigate(Routes.AuditLog)
        HomeAction.OpenOtcNegotiationHistory -> navigate(Routes.OtcNegotiationHistory)
        HomeAction.OpenWatchlist -> navigate(Routes.Watchlist)
        HomeAction.OpenPriceAlerts -> navigate(Routes.PriceAlerts)
        HomeAction.OpenRecurringOrders -> navigate(Routes.RecurringOrders)
    }
}
