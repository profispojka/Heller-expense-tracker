package cz.heller.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.ui.graphics.vector.ImageVector
import cz.heller.R

/** Trasy v aplikaci. */
object Routes {
    const val DASHBOARD = "dashboard"
    const val RECORDS = "records"
    const val BUDGETS = "budgets"
    const val MORE = "more"
    const val ADD_RECORD = "add_record"
    const val ACCOUNTS = "accounts"
    const val ADD_ACCOUNT = "add_account"
    const val ADD_BUDGET = "add_budget"
    const val PLANNED = "planned"
    const val ADD_PLANNED = "add_planned"
    const val STATISTICS = "statistics"

    const val STATISTICS_EXPENSES_ROUTE = "statistics_expenses/{ym}"
    fun statisticsExpenses(ym: String) = "statistics_expenses/$ym"

    const val STATISTICS_INCOME_ROUTE = "statistics_income/{ym}"
    fun statisticsIncome(ym: String) = "statistics_income/$ym"

    const val STATISTICS_FORECAST_ROUTE = "statistics_forecast/{ym}"
    fun statisticsForecast(ym: String) = "statistics_forecast/$ym"

    // add_record slouží i pro editaci (volitelný arg recordId).
    const val ADD_RECORD_ROUTE = "add_record?recordId={recordId}"
    fun addRecordEdit(id: String) = "add_record?recordId=$id"

    const val RECORD_DETAIL_ROUTE = "record_detail/{recordId}"
    fun recordDetail(id: String) = "record_detail/$id"

    // add_account slouží i pro editaci (volitelný arg accountId).
    const val ADD_ACCOUNT_ROUTE = "add_account?accountId={accountId}"
    fun editAccount(id: String) = "add_account?accountId=$id"

    const val PLANNED_DETAIL_ROUTE = "planned_detail/{plannedId}"
    fun plannedDetail(id: String) = "planned_detail/$id"

    // Ruční napojení nadcházející platby na konkrétní transakci.
    const val MATCH_PAYMENT_ROUTE = "match_payment/{plannedId}"
    fun matchPayment(id: String) = "match_payment/$id"

    const val CATEGORIES = "categories"
    const val BACKUP = "backup"
    const val RECURRING = "recurring"
    const val CATEGORY_PICKER_ROUTE = "category_picker/{type}"
    fun categoryPicker(type: String) = "category_picker/$type"
    // add_category slouží i pro editaci (volitelné argy categoryId, type).
    const val ADD_CATEGORY_ROUTE = "add_category?categoryId={categoryId}&type={type}"
    fun addCategory(type: String) = "add_category?type=$type"
    fun editCategory(id: String) = "add_category?categoryId=$id"

    // add_planned slouží i pro editaci (volitelný arg plannedId).
    const val ADD_PLANNED_ROUTE = "add_planned?plannedId={plannedId}"
    fun editPlanned(id: String) = "add_planned?plannedId=$id"
}

/** Položky spodní navigace. */
enum class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    DASHBOARD(Routes.DASHBOARD, R.string.nav_dashboard, Icons.Filled.Home),
    PLANNED(Routes.PLANNED, R.string.nav_planned, Icons.Filled.EventRepeat),
    STATISTICS(Routes.STATISTICS, R.string.nav_statistics, Icons.Filled.BarChart),
    RECORDS(Routes.RECORDS, R.string.nav_records, Icons.AutoMirrored.Filled.ReceiptLong),
    MORE(Routes.MORE, R.string.nav_more, Icons.Filled.MoreHoriz),
}
