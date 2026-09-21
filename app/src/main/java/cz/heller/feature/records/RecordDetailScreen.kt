package cz.heller.feature.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import cz.heller.core.designsystem.component.CalmDialogDismissButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import cz.heller.R
import cz.heller.core.designsystem.component.CalmChip
import cz.heller.core.designsystem.component.CalmPrimaryButton
import cz.heller.core.designsystem.component.MoneyAmount
import cz.heller.data.db.RecordEntity
import cz.heller.data.db.RecordSource
import cz.heller.data.db.RecordType
import cz.heller.data.repo.AccountRepository
import cz.heller.data.repo.CategorizationRepository
import cz.heller.data.repo.CategoryRepository
import cz.heller.data.repo.RecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class RecordDetailUiState(
    val record: RecordEntity? = null,
    val typeLabel: String = "",
    val signedAmountMinor: Long = 0,
    val categoryName: String? = null,
    val accountName: String? = null,
    val transferAccountName: String? = null,
    val dateText: String = "",
    /** Kategorii přiřadila automatika a uživatel ji ještě nepotvrdil. */
    val categoryAuto: Boolean = false,
    /** Tipy kategorií pro nezařazený příjem/výdaj. */
    val suggestions: List<CategorySuggestion> = emptyList(),
) {
    val isTransfer: Boolean get() = record?.type == RecordType.TRANSFER
}

@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val records: RecordRepository,
    categories: CategoryRepository,
    accounts: AccountRepository,
    private val categorization: CategorizationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val recordId: String = checkNotNull(savedStateHandle["recordId"])
    private val formatter = DateTimeFormatter.ofPattern("d. M. yyyy H:mm", Locale.getDefault())
    private val zone = ZoneId.systemDefault()

    val state: StateFlow<RecordDetailUiState> = combine(
        records.observeById(recordId), categories.observeAll(), accounts.observeActive(),
    ) { r, cats, accs ->
        if (r == null) return@combine RecordDetailUiState()
        val byId = cats.associateBy { it.id }
        val accById = accs.associateBy { it.id }
        val signed = when (r.type) {
            RecordType.INCOME -> r.amountMinor
            RecordType.EXPENSE -> -r.amountMinor
            RecordType.TRANSFER -> if (r.transferOut == true) -r.amountMinor else r.amountMinor
        }
        RecordDetailUiState(
            record = r,
            typeLabel = context.getString(when (r.type) {
                RecordType.EXPENSE -> R.string.type_expense
                RecordType.INCOME -> R.string.type_income
                RecordType.TRANSFER -> R.string.record_transfer
            }),
            signedAmountMinor = signed,
            categoryName = r.categoryId?.let { byId[it]?.name },
            accountName = accById[r.accountId]?.name,
            transferAccountName = r.transferAccountId?.let { accById[it]?.name },
            dateText = formatter.format(Instant.ofEpochMilli(r.dateTime).atZone(zone)),
            categoryAuto = r.categoryId != null && r.categoryAuto,
            suggestions = if (r.categoryId == null && r.type != RecordType.TRANSFER) {
                categorization.suggestionsFor(r).mapNotNull { id -> byId[id]?.let { CategorySuggestion(id, it.name) } }
            } else {
                emptyList()
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordDetailUiState())

    fun delete(onDone: () -> Unit) {
        val r = state.value.record ?: return
        viewModelScope.launch {
            records.deleteWithPartner(r)
            onDone()
        }
    }

    fun assignCategory(categoryId: String) {
        viewModelScope.launch { records.assignCategory(recordId, categoryId) }
    }

    fun confirmCategory() {
        viewModelScope.launch { records.confirmCategory(recordId) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    vm: RecordDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val record = state.record

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.record_detail_title), style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(thickness = 3.dp, color = MaterialTheme.colorScheme.onBackground)

        if (record == null) {
            Text(stringResource(R.string.record_not_found), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
            return
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MoneyAmount(
                state.signedAmountMinor,
                withSign = true,
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.fillMaxWidth(),
            )

            DetailRow(stringResource(R.string.detail_type), state.typeLabel)
            if (state.isTransfer && record.transferAccountId != null) {
                DetailRow(stringResource(R.string.detail_from_account), state.accountName ?: "—")
                DetailRow(stringResource(R.string.detail_to_account), state.transferAccountName ?: "—")
            } else {
                DetailRow(stringResource(R.string.detail_account), state.accountName ?: "—")
                if (!state.isTransfer) {
                    val categoryText = state.categoryName?.let { name ->
                        if (state.categoryAuto) "$name · ${stringResource(R.string.category_auto)}" else name
                    } ?: stringResource(R.string.no_category)
                    DetailRow(stringResource(R.string.detail_category), categoryText)
                }
            }
            DetailRow(stringResource(R.string.detail_date), state.dateText)
            record.payee?.let { DetailRow(stringResource(R.string.detail_payee), it) }
            record.note?.let { DetailRow(stringResource(R.string.detail_note), it) }

            // Automaticky přiřazená kategorie: vysvětli a nabídni potvrzení na jeden tap.
            if (state.categoryAuto) {
                Text(
                    stringResource(R.string.category_auto_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { vm.confirmCategory() },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(stringResource(R.string.action_confirm_category), style = MaterialTheme.typography.titleMedium)
                }
            }
            // Nezařazený záznam: tipy kategorií jako chipy.
            if (state.suggestions.isNotEmpty()) {
                Text(
                    stringResource(R.string.suggestions_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.suggestions.forEach { s ->
                        CalmChip(label = s.name, selected = false, onClick = { vm.assignCategory(s.id) })
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Oboustranný převod (2 propojené záznamy) se jen maže. Jednostranný (z Fia nebo ručně
            // označený) jde upravit — třeba zrušit označení převodu.
            if (!state.isTransfer || record.transferRecordId == null) {
                CalmPrimaryButton(stringResource(R.string.action_edit), onClick = { onEdit(record.id) })
            }
            // Záznamy z Fio jsou napojené na banku (smazaný se při synchronizaci vrátí) —
            // mazat jdou jen ruční záznamy.
            if (record.source != RecordSource.FIO) {
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(stringResource(R.string.action_delete), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.record_delete_title)) },
            text = { Text(stringResource(if (state.isTransfer) R.string.record_delete_transfer_msg else R.string.record_delete_msg)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(onBack) }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                CalmDialogDismissButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
    }
}
