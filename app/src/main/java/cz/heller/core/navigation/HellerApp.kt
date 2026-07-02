package cz.heller.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cz.heller.feature.records.RecordDetailScreen
import cz.heller.R
import cz.heller.feature.accounts.AccountsScreen
import cz.heller.feature.accounts.AddAccountScreen
import cz.heller.feature.addrecord.AddRecordScreen
import cz.heller.feature.backup.BackupScreen
import cz.heller.feature.budgets.AddBudgetScreen
import cz.heller.feature.budgets.BudgetsScreen
import cz.heller.feature.categories.AddCategoryScreen
import cz.heller.feature.categories.CategoriesScreen
import cz.heller.feature.categories.CategoryPickerScreen
import cz.heller.feature.dashboard.DashboardScreen
import cz.heller.feature.more.MoreScreen
import cz.heller.feature.onboarding.OnboardingScreen
import cz.heller.feature.planned.AddPlannedPaymentScreen
import cz.heller.feature.planned.MatchPaymentScreen
import cz.heller.feature.planned.PlannedPaymentDetailScreen
import cz.heller.feature.planned.PlannedPaymentsScreen
import cz.heller.feature.records.RecordsScreen
import cz.heller.feature.recurring.RecurringScreen
import cz.heller.feature.statistics.StatisticsExpensesScreen
import cz.heller.feature.statistics.StatisticsForecastScreen
import cz.heller.feature.statistics.StatisticsIncomeScreen
import cz.heller.feature.statistics.StatisticsScreen

/** Kořen: rozhodne mezi onboardingem a hlavní aplikací podle existence účtu. */
@Composable
fun HellerApp(rootViewModel: RootViewModel = hiltViewModel()) {
    val rootState by rootViewModel.state.collectAsStateWithLifecycle()
    // Při každém otevření / návratu do aplikace stáhni čerstvá data z Fia.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { rootViewModel.onAppForegrounded() }
    when (rootState) {
        RootUiState.Loading -> Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {}
        RootUiState.Onboarding -> OnboardingScreen()
        RootUiState.Ready -> MainScaffold()
    }
}

@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = currentRoute in TopLevelDestination.entries.map { it.route }.toSet()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (isTopLevel) CalmBottomBar(navController) },
        floatingActionButton = {
            // FAB jen na Přehledu (nový záznam) a Platbách (nová plánovaná platba).
            val fabRoute = when (currentRoute) {
                Routes.DASHBOARD -> Routes.ADD_RECORD
                Routes.PLANNED -> Routes.ADD_PLANNED
                else -> null
            }
            if (fabRoute != null) {
                FloatingActionButton(
                    onClick = { navController.navigate(fabRoute) },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_record))
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onOpenRecord = { navController.navigate(Routes.recordDetail(it)) },
                    onOpenPlanned = { navController.navigate(Routes.PLANNED) },
                    onMatchPayment = { navController.navigate(Routes.matchPayment(it)) },
                )
            }
            composable(Routes.RECORDS) {
                RecordsScreen(
                    onOpenRecord = { navController.navigate(Routes.recordDetail(it)) },
                )
            }
            composable(Routes.STATISTICS) {
                StatisticsScreen(
                    onOpenExpenses = { ym -> navController.navigate(Routes.statisticsExpenses(ym)) },
                    onOpenIncome = { ym -> navController.navigate(Routes.statisticsIncome(ym)) },
                    onOpenForecast = { ym -> navController.navigate(Routes.statisticsForecast(ym)) },
                )
            }
            composable(
                route = Routes.STATISTICS_EXPENSES_ROUTE,
                arguments = listOf(navArgument("ym") { type = NavType.StringType }),
            ) {
                StatisticsExpensesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRecord = { navController.navigate(Routes.recordDetail(it)) },
                )
            }
            composable(
                route = Routes.STATISTICS_INCOME_ROUTE,
                arguments = listOf(navArgument("ym") { type = NavType.StringType }),
            ) {
                StatisticsIncomeScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRecord = { navController.navigate(Routes.recordDetail(it)) },
                )
            }
            composable(
                route = Routes.STATISTICS_FORECAST_ROUTE,
                arguments = listOf(navArgument("ym") { type = NavType.StringType }),
            ) {
                StatisticsForecastScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.BUDGETS) {
                BudgetsScreen(
                    onAddBudget = { navController.navigate(Routes.ADD_BUDGET) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ADD_BUDGET) {
                AddBudgetScreen(onClose = { navController.popBackStack() })
            }
            composable(Routes.MORE) {
                MoreScreen(
                    onOpenAccounts = { navController.navigate(Routes.ACCOUNTS) },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenBudgets = { navController.navigate(Routes.BUDGETS) },
                    onOpenBackup = { navController.navigate(Routes.BACKUP) },
                )
            }
            composable(Routes.RECURRING) {
                RecurringScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.BACKUP) {
                BackupScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CATEGORIES) {
                CategoriesScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { type -> navController.navigate(Routes.addCategory(type.name)) },
                    onEdit = { id -> navController.navigate(Routes.editCategory(id)) },
                )
            }
            composable(
                route = Routes.ADD_CATEGORY_ROUTE,
                arguments = listOf(
                    navArgument("categoryId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("type") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                AddCategoryScreen(onClose = { navController.popBackStack() })
            }
            composable(Routes.PLANNED) {
                PlannedPaymentsScreen(
                    onOpenDetail = { id -> navController.navigate(Routes.plannedDetail(id)) },
                )
            }
            composable(
                route = Routes.PLANNED_DETAIL_ROUTE,
                arguments = listOf(navArgument("plannedId") { type = NavType.StringType }),
            ) {
                PlannedPaymentDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.editPlanned(id)) },
                )
            }
            composable(
                route = Routes.MATCH_PAYMENT_ROUTE,
                arguments = listOf(navArgument("plannedId") { type = NavType.StringType }),
            ) {
                MatchPaymentScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.ADD_PLANNED_ROUTE,
                arguments = listOf(navArgument("plannedId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }),
            ) { entry ->
                val picked by entry.savedStateHandle
                    .getStateFlow<String?>("picked_category", null)
                    .collectAsStateWithLifecycle()
                AddPlannedPaymentScreen(
                    onClose = { navController.popBackStack() },
                    onPickCategory = { type -> navController.navigate(Routes.categoryPicker(type.name)) },
                    pickedCategoryId = picked,
                    onPickedConsumed = { entry.savedStateHandle["picked_category"] = null },
                )
            }
            composable(
                route = Routes.ADD_RECORD_ROUTE,
                arguments = listOf(navArgument("recordId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }),
            ) { entry ->
                val picked by entry.savedStateHandle
                    .getStateFlow<String?>("picked_category", null)
                    .collectAsStateWithLifecycle()
                AddRecordScreen(
                    onClose = { navController.popBackStack() },
                    onPickCategory = { type -> navController.navigate(Routes.categoryPicker(type.name)) },
                    pickedCategoryId = picked,
                    onPickedConsumed = { entry.savedStateHandle["picked_category"] = null },
                )
            }
            composable(
                route = Routes.CATEGORY_PICKER_ROUTE,
                arguments = listOf(navArgument("type") { type = NavType.StringType }),
            ) {
                CategoryPickerScreen(
                    onBack = { navController.popBackStack() },
                    onPicked = { id ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("picked_category", id)
                        navController.popBackStack()
                    },
                )
            }
            composable(
                route = Routes.RECORD_DETAIL_ROUTE,
                arguments = listOf(navArgument("recordId") { type = NavType.StringType }),
            ) {
                RecordDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(Routes.addRecordEdit(id)) },
                )
            }
            composable(Routes.ACCOUNTS) {
                AccountsScreen(
                    onBack = { navController.popBackStack() },
                    onAdd = { navController.navigate(Routes.ADD_ACCOUNT) },
                    onEdit = { id -> navController.navigate(Routes.editAccount(id)) },
                )
            }
            composable(
                route = Routes.ADD_ACCOUNT_ROUTE,
                arguments = listOf(navArgument("accountId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }),
            ) {
                AddAccountScreen(
                    onClose = { navController.popBackStack() },
                    onOpenRecurring = { navController.navigate(Routes.RECURRING) },
                )
            }
        }
    }
}
