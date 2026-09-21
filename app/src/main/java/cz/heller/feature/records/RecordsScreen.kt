package cz.heller.feature.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import cz.heller.R
import cz.heller.core.designsystem.component.CalmChip
import cz.heller.core.designsystem.component.CalmTopBar
import cz.heller.core.designsystem.component.EmptyState
import cz.heller.core.designsystem.component.MoneyAmount
import cz.heller.data.db.AccountEntity
import cz.heller.data.db.RecordType
import cz.heller.data.repo.AccountRepository
import cz.heller.data.repo.CategorizationRepository
import cz.heller.data.repo.CategoryRepository
import cz.heller.data.repo.RecordRepository
import cz.heller.feature.budgets.CategoryGroup
import cz.heller.feature.budgets.expenseGroups
import cz.heller.feature.budgets.groupIdOf
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class DayGroup(val label: String, val items: List<RecordRowUi>, val dayTotalMinor: Long)

data class RecordFilter(
    val type: RecordType? = null,
    val accountId: String? = null,
    val groupId: String? = null,
    /** Jen nezařazené příjmy/výdaje — „inbox" k roztřídění. */
    val uncategorized: Boolean = false,
) {
    val isActive: Boolean get() = type != null || accountId != null || groupId != null || uncategorized
}

data class RecordsUiState(
    val groups: List<DayGroup> = emptyList(),
    val incomeMinor: Long = 0,
    val expenseMinor: Long = 0,
    val filter: RecordFilter = RecordFilter(),
    val accounts: List<AccountEntity> = emptyList(),
    val categoryGroups: List<CategoryGroup> = emptyList(),
)

@HiltViewModel
class RecordsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val records: RecordRepository,
    categories: CategoryRepository,
    accounts: AccountRepository,
    private val categorization: CategorizationRepository,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d. M. yyyy", Locale.getDefault())
    private val filter = MutableStateFlow(RecordFilter())

    val state: StateFlow<RecordsUiState> = combine(
        records.observeAll(),
        categories.observeAll(),
        accounts.observeActive(),
        filter,
    ) { recs, cats, accs, f ->
        val byId = cats.associateBy { it.id }
        val filtered = recs.filter { r ->
            (f.type == null || r.type == f.type) &&
                (f.accountId == null || r.accountId == f.accountId) &&
                (f.groupId == null || groupIdOf(r.categoryId, byId) == f.groupId) &&
                (!f.uncategorized || (r.categoryId == null && r.type != RecordType.TRANSFER))
        }
        val accMap = accs.associateBy { it.id }
        val income = filtered.filter { it.type == RecordType.INCOME }.sumOf { it.amountMinor }
        val expense = filtered.filter { it.type == RecordType.EXPENSE }.sumOf { it.amountMinor }

        val groups = filtered
            .groupBy { Instant.ofEpochMilli(it.dateTime).atZone(zone).toLocalDate() }
            .entries
            .sortedByDescending { it.key }
            .map { (date, list) ->
                val rows = list.sortedByDescending { it.dateTime }.map { r ->
                    // Nezařazený příjem/výdaj dostane tipy kategorií (chipy na jeden tap).
                    val tips = if (r.categoryId == null && r.type != RecordType.TRANSFER) {
                        categorization.suggestionsFor(r).mapNotNull { id -> byId[id]?.let { CategorySuggestion(id, it.name) } }
                    } else {
                        emptyList()
                    }
                    r.toRowUi(byId, accMap, context, tips)
                }
                // Převody mezi vlastními účty nejsou výdaj ani příjem — do denního součtu se nepočítají.
                val dayTotal = list.sumOf { r ->
                    when (r.type) {
                        RecordType.INCOME -> r.amountMinor
                        RecordType.EXPENSE -> -r.amountMinor
                        RecordType.TRANSFER -> 0L
                    }
                }
                DayGroup(labelFor(date), rows, dayTotal)
            }
        RecordsUiState(groups, income, expense, f, accs, expenseGroups(cats))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordsUiState())

    fun setTypeFilter(type: RecordType?) = filter.update { it.copy(type = type) }
    fun setAccountFilter(id: String?) = filter.update { it.copy(accountId = id) }
    fun setGroupFilter(id: String?) = filter.update { it.copy(groupId = id, uncategorized = false) }
    fun setUncategorizedFilter(on: Boolean) = filter.update { it.copy(uncategorized = on, groupId = null) }
    fun clearFilter() = filter.update { RecordFilter() }

    /** Tap na tip kategorie: zařadí záznam jako potvrzený a dožene stejné obchodníky. */
    fun assignCategory(recordId: String, categoryId: String) {
        viewModelScope.launch { records.assignCategory(recordId, categoryId) }
    }

    private fun labelFor(date: LocalDate): String {
        val today = LocalDate.now()
        return when (date) {
            today -> context.getString(R.string.day_today)
            today.minusDays(1) -> context.getString(R.string.day_yesterday)
            else -> date.format(formatter)
        }
    }
}

@Composable
fun RecordsScreen(
    onOpenRecord: (String) -> Unit,
    vm: RecordsViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showFilter by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    // Po otevření filtru roluj nahoru, ať je panel vidět (LazyColumn jinak drží pozici).
    LaunchedEffect(showFilter) { if (showFilter) listState.scrollToItem(0) }

    Column(Modifier.fillMaxSize()) {
        CalmTopBar(stringResource(R.string.nav_records))

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.records_income), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyAmount(state.incomeMinor, withSign = true)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.records_expenses), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                MoneyAmount(-state.expenseMinor, withSign = true)
            }
            CalmChip(
                label = stringResource(R.string.filter_label) + if (state.filter.isActive) " •" else "",
                selected = showFilter || state.filter.isActive,
                onClick = { showFilter = !showFilter },
            )
        }

        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        // Filtr je hlavička LazyColumnu, takže scrolluje spolu se seznamem (i když má hodně kategorií).
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            if (showFilter) {
                item(key = "filter") {
                    FilterPanel(state, vm)
                    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                }
            }

            if (state.groups.isEmpty()) {
                item(key = "empty") {
                    Box(
                        Modifier.fillParentMaxSize().padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = stringResource(if (state.filter.isActive) R.string.records_empty_filter_title else R.string.records_empty_title),
                            subtitle = stringResource(if (state.filter.isActive) R.string.records_empty_filter_sub else R.string.records_empty_sub),
                        )
                    }
                }
            } else {
                state.groups.forEach { group ->
                    item(key = "h_${group.label}") {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(group.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                MoneyAmount(group.dayTotalMinor, withSign = true, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    items(group.items.size, key = { group.items[it].id }) { i ->
                        val item = group.items[i]
                        RecordRowItem(
                            item,
                            onClick = { onOpenRecord(item.id) },
                            onSuggestion = { catId -> vm.assignCategory(item.id, catId) },
                        )
                        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterPanel(state: RecordsUiState, vm: RecordsViewModel) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.detail_type), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CalmChip(stringResource(R.string.filter_all), state.filter.type == null, onClick = { vm.setTypeFilter(null) })
            CalmChip(stringResource(R.string.type_expense), state.filter.type == RecordType.EXPENSE, onClick = { vm.setTypeFilter(RecordType.EXPENSE) })
            CalmChip(stringResource(R.string.type_income), state.filter.type == RecordType.INCOME, onClick = { vm.setTypeFilter(RecordType.INCOME) })
            CalmChip(stringResource(R.string.record_transfer), state.filter.type == RecordType.TRANSFER, onClick = { vm.setTypeFilter(RecordType.TRANSFER) })
        }

        Text(stringResource(R.string.detail_account), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CalmChip(stringResource(R.string.filter_all), state.filter.accountId == null, onClick = { vm.setAccountFilter(null) })
            state.accounts.forEach { a ->
                CalmChip(a.name, state.filter.accountId == a.id, onClick = { vm.setAccountFilter(a.id) })
            }
        }

        Text(stringResource(R.string.detail_category), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CalmChip(stringResource(R.string.filter_all), state.filter.groupId == null && !state.filter.uncategorized, onClick = { vm.setGroupFilter(null) })
            CalmChip(stringResource(R.string.filter_uncategorized), state.filter.uncategorized, onClick = { vm.setUncategorizedFilter(!state.filter.uncategorized) })
            state.categoryGroups.forEach { g ->
                CalmChip(g.name, state.filter.groupId == g.id, onClick = { vm.setGroupFilter(g.id) })
            }
        }

        if (state.filter.isActive) {
            CalmChip(stringResource(R.string.filter_clear), selected = false, onClick = { vm.clearFilter() }, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
