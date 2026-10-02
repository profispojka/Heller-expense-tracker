package cz.heller.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cz.heller.R
import cz.heller.core.money.AmountFormat
import cz.heller.core.money.Money
import cz.heller.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    val amountFormat: StateFlow<AmountFormat> = settings.amountFormat
        .map { AmountFormat.fromCode(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Money.amountFormat)

    fun setAmountFormat(format: AmountFormat) {
        // Hned překreslit (Money je snapshot state), uložení doběhne na pozadí.
        Money.applyAmountFormat(format.code)
        viewModelScope.launch { settings.setAmountFormat(format.code) }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val selected by vm.amountFormat.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.more_settings), style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        Column(Modifier.verticalScroll(rememberScrollState())) {
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stringResource(R.string.settings_amount_format), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(R.string.settings_amount_format_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AmountFormat.entries.forEach { format ->
                AmountFormatRow(
                    label = stringResource(format.labelRes()),
                    example = Money.format(EXAMPLE_MINOR, false, format, Money.currencySymbol),
                    selected = format == selected,
                    onClick = { vm.setAmountFormat(format) },
                )
            }
        }
    }
}

private const val EXAMPLE_MINOR = 913_358L // 9 133,58

private fun AmountFormat.labelRes(): Int = when (this) {
    AmountFormat.CZECH -> R.string.amount_format_cs
    AmountFormat.US -> R.string.amount_format_us
    AmountFormat.EUROPEAN -> R.string.amount_format_eu
}

@Composable
private fun AmountFormatRow(label: String, example: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.padding(horizontal = 8.dp))
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(example, style = MaterialTheme.typography.bodyLarge)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
}
