package cz.heller.feature.records

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cz.heller.R
import cz.heller.core.designsystem.CategoryIcons
import cz.heller.core.designsystem.component.CalmChip
import cz.heller.core.designsystem.component.MoneyAmount

/**
 * Jeden řádek záznamu (ikona, název, podtitul, částka se znaménkem). Nezařazený záznam má pod
 * sebou chipy s tipy kategorií — tap zařadí bez otevírání detailu ([onSuggestion]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecordRowItem(
    row: RecordRowUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onSuggestion: ((String) -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(CategoryIcons.forKey(row.iconKey), contentDescription = null, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(row.title, style = MaterialTheme.typography.bodyLarge)
                if (row.subtitle != null) {
                    Text(
                        row.subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            MoneyAmount(row.amountMinor, withSign = true, style = MaterialTheme.typography.bodyLarge)
        }
        if (onSuggestion != null && row.suggestions.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.suggestions_label) + ":",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                row.suggestions.forEach { s ->
                    CalmChip(label = s.name, selected = false, onClick = { onSuggestion(s.id) })
                }
            }
        }
    }
}
