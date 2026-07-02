package cz.heller.feature.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cz.heller.R

/**
 * Minimální délka hesla zálohy. Záloha nese živý (byť read-only) bankovní token a její sůl/IV jsou
 * ve veřejné hlavičce, takže jediná ochrana je heslo — proto rozumná délka a ne jen číslice.
 */
private const val MIN_PASSWORD = 8

/** Heslo je dost silné: aspoň [MIN_PASSWORD] znaků a ne pouze číslice. */
private fun passwordStrongEnough(pwd: String): Boolean =
    pwd.length >= MIN_PASSWORD && !pwd.all { it.isDigit() }

/** Dialog pro nastavení hesla při vytváření zálohy (heslo + potvrzení). */
@Composable
fun SetBackupPasswordDialog(
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var pwd by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val tooShort = pwd.isNotEmpty() && !passwordStrongEnough(pwd)
    val mismatch = confirm.isNotEmpty() && confirm != pwd
    val valid = passwordStrongEnough(pwd) && confirm == pwd

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.backup_password_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.backup_password_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = { Text(stringResource(R.string.backup_password_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = tooShort,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text(stringResource(R.string.backup_password_confirm_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = mismatch,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (tooShort) {
                    Text(
                        stringResource(R.string.backup_password_too_short, MIN_PASSWORD),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else if (mismatch) {
                    Text(
                        stringResource(R.string.backup_password_mismatch),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pwd.toCharArray()) }, enabled = valid) {
                Text(stringResource(R.string.backup_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Dialog pro zadání hesla při obnově šifrované zálohy. [error] = předchozí pokus měl špatné heslo. */
@Composable
fun EnterBackupPasswordDialog(
    error: Boolean,
    onConfirm: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var pwd by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restore_password_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.restore_password_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = { Text(stringResource(R.string.backup_password_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                if (error) {
                    Text(
                        stringResource(R.string.restore_wrong_password),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pwd.toCharArray()) }, enabled = pwd.isNotEmpty()) {
                Text(stringResource(R.string.restore_confirm_yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
