package pt.rikmartins.clubemg.mobile.ui.notification

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.io.IOException
import pt.rikmartins.clubemg.mobile.BuildConfig
import pt.rikmartins.clubemg.mobile.ClubeMGApp
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents
import java.util.concurrent.TimeUnit

internal class EventSyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val synchronizeFavouriteEvents: SynchronizeFavouriteEvents,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        Log.v("EventSyncWorker", "Synchronization starting.")
        synchronizeFavouriteEvents()
        Log.d("EventSyncWorker", "Synchronization successful.")
        Result.success()
    } catch (e: IOException) {
        Log.w("EventSyncWorker", "Failed to synchronize, will retry.", e)
        Result.retry()
    } catch (e: Exception) {
        Log.e("EventSyncWorker", "An unexpected error occurred during synchronization.", e)
        Result.failure()
    }
}

/**
 * Notice the use of [BuildConfig]
 */
internal fun ClubeMGApp.scheduleEventSync() {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(!BuildConfig.DEBUG)
        .build()

    val syncRequest = (if (BuildConfig.DEBUG) PeriodicWorkRequestBuilder<EventSyncWorker>(
        repeatInterval = 15, repeatIntervalTimeUnit = TimeUnit.MINUTES,
    ) else PeriodicWorkRequestBuilder<EventSyncWorker>(
        repeatInterval = 1, repeatIntervalTimeUnit = TimeUnit.DAYS,
        flexTimeInterval = 6, flexTimeIntervalUnit = TimeUnit.HOURS
    ))
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
        WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        syncRequest
    )
}

private const val WORK_NAME = "EventSync"