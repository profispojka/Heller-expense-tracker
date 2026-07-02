package cz.heller.core.fio

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Plánuje / ruší denní automatickou synchronizaci s Fio (WorkManager). */
@Singleton
class FioSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun scheduleDaily() {
        val request = PeriodicWorkRequestBuilder<FioSyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
            )
            .build()
        // UPDATE (ne KEEP): pokud se dřívější naplánování „rozpadlo" (force-stop od OEM, decay),
        // re-arm ho čerstvým requestem místo tichého ponechání mrtvého záznamu.
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(FioSyncWorker.NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(FioSyncWorker.NAME)
    }
}
