package cz.heller.feature.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import cz.heller.R
import cz.heller.core.designsystem.CategoryIcons
import cz.heller.core.designsystem.component.CalmTopBar
import cz.heller.core.designsystem.component.MoneyAmount
import cz.heller.core.time.PlannedPayments
import cz.heller.core.time.Periods
import cz.heller.data.db.RecordType
import cz.heller.data.repo.CategoryRepository
import cz.heller.data.repo.PlannedPaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

data class ForecastItem(
    val id: String,
    val name: String,
    val icon: String,
    val dateText: String,
    val amountMinor: Long,
)

data class ForecastUiState(
    val monthName: String = "",
    val totalMinor: Long = 0,
    val items: List<ForecastItem> = emptyList(),
)

@HiltViewModel
class StatisticsForecastViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    planned: PlannedPaymentRepository,
    categories: CategoryRepository,
) : ViewModel() {

    // Přijímáme vybraný měsíc ze statistik; výhled je pro měsíc následující (stejně jako číslo ve statistikách).
    private val selectedYm: YearMonth =
        runCatching { YearMonth.parse(savedStateHandle.get<String>("ym")) }.getOrDefault(YearMonth.now())
    private val forecastYm: YearMonth = selectedYm.plusMonths(1)

    val state: StateFlow<ForecastUiState> = combine(
        planned.observeAll(), categories.observeAll(),
    ) { pays, cats ->
        val byId = cats.associateBy { it.id }

        val items = pays
            .filter { it.type == RecordType.EXPENSE }
            .flatMap { p ->
                val icon = p.categoryId?.let { byId[it]?.icon } ?: "more_horiz"
                PlannedPayments.occurrenceDatesInMonth(
                    p.startEpochDay, p.frequencyUnit, p.frequencyCount, p.endEpochDay, forecastYm,
                ).map { date -> Triple(date, p, icon) }
            }
            .sortedBy { it.first }
            .map { (date, p, icon) ->
                ForecastItem(
                    id = "${p.id}_${date.toEpochDay()}",
                    name = p.name,
                    icon = icon,
                    dateText = PlannedPayments.formatDate(date),
                    amountMinor = p.amountMinor,
                )
            }

        ForecastUiState(Periods.monthName(forecastYm), items.sumOf { it.amountMinor }, items)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForecastUiState())
}

@Composable
fun StatisticsForecastScreen(
    onBack: () -> Unit,
    vm: StatisticsForecastViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        CalmTopBar(stringResource(R.string.stats_forecast), onBack = onBack)

        if (state.items.isEmpty()) {
            Text(
                stringResource(R.string.stats_forecast_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                textAlign = TextAlign.Center,
            )
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize()) {
            item(key = "total") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        state.monthName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(R.string.stats_forecast),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MoneyAmount(-state.totalMinor, withSign = false, style = MaterialTheme.typography.headlineMedium)
                }
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
            }

            items(state.items, key = { it.id }) { item ->
                ForecastRow(item)
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun ForecastRow(item: ForecastItem) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(CategoryIcons.forKey(item.icon), contentDescription = null, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(
                item.dateText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MoneyAmount(-item.amountMinor, withSign = false, style = MaterialTheme.typography.bodyLarge)
    }
}
