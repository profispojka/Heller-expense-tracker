package cz.heller.feature.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.heller.R
import cz.heller.core.designsystem.component.CalmChip
import cz.heller.core.designsystem.component.CalmConfirmSheet
import cz.heller.core.money.AppCurrency
import cz.heller.core.money.Money
import cz.heller.core.security.WrongBackupPasswordException
import cz.heller.data.backup.BackupManager
import cz.heller.data.backup.BackupType
import cz.heller.feature.backup.EnterBackupPasswordDialog
import cz.heller.data.db.AccountType
import cz.heller.data.repo.AccountRepository
import cz.heller.data.settings.SettingsRepository
import cz.heller.feature.accounts.AccountForm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val accounts: AccountRepository,
    private val settings: SettingsRepository,
    private val backup: BackupManager,
) : ViewModel() {
    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    fun createFirstAccount(name: String, type: AccountType, initialCents: Long, currencyCode: String) {
        viewModelScope.launch {
            settings.setCurrency(currencyCode)
            Money.applyCurrency(currencyCode)
            accounts.create(name.ifBlank { "Hotovost" }, type, initialCents, "wallet")
        }
    }

    suspend fun peekType(uri: Uri): BackupType = backup.peekType(uri)

    /** Obnoví ze zálohy. Vrací true, pokud šlo o **špatné heslo** (dialog má zůstat otevřený). */
    suspend fun restore(uri: Uri, password: CharArray?): Boolean {
        _status.value = null
        return runCatching { backup.restoreFrom(uri, password) }
            .fold(
                onSuccess = { backup.restartApp(); false },
                onFailure = { e ->
                    if (e is WrongBackupPasswordException) {
                        true
                    } else {
                        _status.value = e.message ?: ""
                        false
                    }
                },
            )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(vm: OnboardingViewModel = hiltViewModel()) {
    var currency by remember { mutableStateOf(AppCurrency.CZK) }
    val status by vm.status.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var restoreType by remember { mutableStateOf<BackupType?>(null) }
    var wrongPassword by remember { mutableStateOf(false) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> restoreUri = uri }

    LaunchedEffect(restoreUri) {
        wrongPassword = false
        restoreType = restoreUri?.let { vm.peekType(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_welcome),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(top = 32.dp),
        )
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        )

        Text(stringResource(R.string.onboarding_currency), style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
        ) {
            AppCurrency.entries.forEach { c ->
                CalmChip(
                    label = "${c.symbol} (${c.code})",
                    selected = currency == c,
                    onClick = { currency = c },
                )
            }
        }

        AccountForm(
            submitLabel = stringResource(R.string.onboarding_start),
            onSubmit = { name, type, cents, _ -> vm.createFirstAccount(name, type, cents, currency.code) },
            showBusinessToggle = false,
        )

        Text(
            text = stringResource(R.string.onboarding_restore_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )

        OutlinedButton(
            onClick = { restoreLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(stringResource(R.string.restore_action), style = MaterialTheme.typography.titleMedium)
        }

        if (status != null) {
            Text(
                text = status!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
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
        uri != null && restoreType == BackupType.INVALID -> {
            CalmConfirmSheet(
                title = stringResource(R.string.backup_error_invalid),
                confirmLabel = stringResource(R.string.action_back),
                onConfirm = { restoreUri = null },
                onDismiss = { restoreUri = null },
            )
        }
    }
}
