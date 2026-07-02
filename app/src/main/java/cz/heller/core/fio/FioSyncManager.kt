package cz.heller.core.fio

import cz.heller.data.repo.FioRepository
import cz.heller.data.repo.FioSyncResult
import cz.heller.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rychlá synchronizace s Fio na popředí — volá se při každém otevření / návratu do aplikace.
 * Doplňuje denní [FioSyncScheduler]; ten běží i když je appka zavřená, tohle zajistí čerstvá
 * data hned po otevření. Fio dovolí jen 1 dotaz za 30 s (jinak 409), proto se připojení, které
 * se synchronizovalo nedávno, přeskočí ([minIntervalMillis]).
 */
@Singleton
class FioSyncManager @Inject constructor(
    private val settings: SettingsRepository,
    private val fio: FioRepository,
    private val scheduler: FioSyncScheduler,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = AtomicBoolean(false)

    /** Sesynchronizuje všechna Fio připojení, u kterých od poslední synchronizace uběhlo [minIntervalMillis]. */
    fun syncNow(minIntervalMillis: Long = DEFAULT_MIN_INTERVAL_MILLIS) {
        // Nespouštěj další běh, dokud předchozí neskončil (zamezí souběhu a zbytečným 409).
        if (!running.compareAndSet(false, true)) return
        scope.launch {
            try {
                val connections = settings.fioConnections.first()
                if (connections.isEmpty()) return@launch

                // Re-arm denní worker při každém otevření appky. Jinak se plánuje jen z obrazovky
                // účtu, takže po restartu/force-stopu (časté u OEM) se automatika už nikdy neobnovila
                // — hlavní důvod, proč se data „na začátku jela a pak nikdy".
                scheduler.scheduleDaily()

                val now = System.currentTimeMillis()
                for (c in connections) {
                    if (c.token.isBlank()) continue
                    if (now - c.lastSyncMillis < minIntervalMillis) continue
                    if (fio.sync(c.token, c.accountId, 90) is FioSyncResult.Success) {
                        settings.setFioLastSync(c.accountId, System.currentTimeMillis())
                    }
                }
            } finally {
                running.set(false)
            }
        }
    }

    private companion object {
        // Fio limit je 1 dotaz / 30 s na token — s malou rezervou.
        const val DEFAULT_MIN_INTERVAL_MILLIS = 30_000L
    }
}
