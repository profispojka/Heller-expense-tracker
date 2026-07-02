package cz.heller.core.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.heller.core.fio.FioSyncManager
import cz.heller.core.money.Money
import cz.heller.data.repo.AccountRepository
import cz.heller.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface RootUiState {
    data object Loading : RootUiState
    data object Onboarding : RootUiState
    data object Ready : RootUiState
}

@HiltViewModel
class RootViewModel @Inject constructor(
    accountRepository: AccountRepository,
    settings: SettingsRepository,
    private val fioSyncManager: FioSyncManager,
) : ViewModel() {

    /** Zavolá se při každém otevření / návratu do aplikace — stáhne čerstvá data z Fia. */
    fun onAppForegrounded() = fioSyncManager.syncNow()

    val state: StateFlow<RootUiState> = combine(
        accountRepository.observeActive(),
        settings.currency,
    ) { accounts, currency ->
        // Zvolenou měnu nastav dřív, než se vykreslí jakákoliv částka.
        Money.applyCurrency(currency)
        if (accounts.isEmpty()) RootUiState.Onboarding else RootUiState.Ready
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootUiState.Loading)
}
