package cz.heller.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import cz.heller.core.designsystem.component.CalmConfirmSheet
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import cz.heller.core.designsystem.component.CalmPrimaryButton
import cz.heller.core.security.WrongBackupPasswordException
import cz.heller.data.backup.BackupManager
import cz.heller.data.backup.BackupType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backup: BackupManager,
) : ViewModel() {
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    fun backup(uri: Uri, password: CharArray) {
        viewModelScope.launch {
            _status.value = context.getString(R.string.backup_in_progress)
            runCatching { backup.backupTo(uri, password) }
                .onSuccess { _status.value = context.getString(R.string.backup_done) }
                .onFailure { _status.value = context.getString(R.string.backup_failed, it.message ?: "") }
        }
    }

    suspend fun peekType(uri: Uri): BackupType = backup.peekType(uri)

    /** Obnoví ze zálohy. Vrací true, pokud šlo o **špatné heslo** (dialog má zůstat otevřený). */
    suspend fun restore(uri: Uri, password: CharArray?): Boolean {
        _status.value = context.getString(R.string.restore_in_progress)
        return runCatching { backup.restoreFrom(uri, password) }
            .fold(
                onSuccess = { backup.restartApp(); false },
                onFailure = { e ->
                    if (e is WrongBackupPasswordException) {
                        _status.value = null
                        true
                    } else {
                        _status.value = context.getString(R.string.restore_failed, e.message ?: "")
                        false
                    }
                },
            )
    }

    fun invalidBackup() {
        _status.value = context.getString(R.string.backup_error_invalid)
    }
}

@Composable
fun BackupScreen(onBack: () -> Unit, vm: BackupViewModel = hiltViewModel()) {
    val status by vm.status.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Záloha: nejdřív heslo, pak výběr souboru.
    var showSetPassword by remember { mutableStateOf(false) }
    var pendingBackupPassword by remember { mutableStateOf<CharArray?>(null) }

    // Obnova: vybraný soubor + jeho typ + stav špatného hesla.
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var restoreType by remember { mutableStateOf<BackupType?>(null) }
    var wrongPassword by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val pwd = pendingBackupPassword
        if (uri != null && pwd != null) vm.backup(uri, pwd)
        pendingBackupPassword = null
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> restoreUri = uri }

    // Po výběru souboru zjisti typ (šifrovaný vs starý plaintext vs neplatný).
    LaunchedEffect(restoreUri) {
        val u = restoreUri
        wrongPassword = false
        restoreType = u?.let { vm.peekType(it) }
        if (restoreType == BackupType.INVALID) {
            vm.invalidBackup()
            restoreUri = null
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Text(stringResource(R.string.more_backup), style = MaterialTheme.typography.titleLarge)
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(R.string.backup_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            CalmPrimaryButton(stringResource(R.string.backup_action), onClick = { showSetPassword = true })

            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(stringResource(R.string.restore_action), style = MaterialTheme.typography.titleMedium)
            }

            if (status != null) {
                Text(status!!, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showSetPassword) {
        SetBackupPasswordDialog(
            onConfirm = { pwd ->
                showSetPassword = false
                pendingBackupPassword = pwd
                backupLauncher.launch("heller-zaloha.hbk")
            },
            onDismiss = { showSetPassword = false },
        )
    }

    val uri = restoreUri
    when {
        uri != null && restoreType == BackupType.ENCRYPTED -> {
            EnterBackupPasswordDialog(
                error = wrongPassword,
                onConfirm = { pwd ->
                    scope.launch {
                        val badPassword = vm.restore(uri, pwd)
                        if (badPassword) wrongPassword = true else restoreUri = null
                    }
                },
                onDismiss = { restoreUri = null },
            )
        }
        uri != null && restoreType == BackupType.LEGACY_PLAINTEXT -> {
            CalmConfirmSheet(
                title = stringResource(R.string.restore_confirm_title),
                confirmLabel = stringResource(R.string.restore_confirm_yes),
                onConfirm = { scope.launch { vm.restore(uri, null) }; restoreUri = null },
                onDismiss = { restoreUri = null },
            )
        }
    }
}
