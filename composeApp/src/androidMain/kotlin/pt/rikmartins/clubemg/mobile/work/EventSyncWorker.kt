package pt.rikmartins.clubemg.mobile.work

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
import pt.rikmartins.clubemg.mobile.ClubeMGApp
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents
import java.util.concurrent.TimeUnit

internal class EventSyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val synchronizeFavouriteEvents: SynchronizeFavouriteEvents
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        synchronizeFavouriteEvents(Unit)
        Log.d("FavouriteEventsWorker", "Synchronization successful.")
        Result.success()
    } catch (e: IOException) {
        Log.e("FavouriteEventsWorker", "Failed to synchronize, will retry.", e)
        Result.retry()
    } catch (e: Exception) {
        Log.e("FavouriteEventsWorker", "An unexpected error occurred during synchronization.", e)
        Result.failure()
    }
}

internal fun ClubeMGApp.scheduleEventSync() {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .setRequiresDeviceIdle(true)
        .build()

    val syncRequest = PeriodicWorkRequestBuilder<EventSyncWorker>(
        repeatInterval = 3, repeatIntervalTimeUnit = TimeUnit.DAYS,
        flexTimeInterval = 3, flexTimeIntervalUnit = TimeUnit.HOURS
    )
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
        WORK_NAME,
        ExistingPeriodicWorkPolicy.KEEP,
        syncRequest
    )
}

private const val WORK_NAME = "EventSync"