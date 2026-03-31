package pt.rikmartins.clubemg.mobile

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import pt.rikmartins.clubemg.mobile.cache.getDatabaseBuilder
import pt.rikmartins.clubemg.mobile.datastore.createDataStore
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents
import pt.rikmartins.clubemg.mobile.notification.AndroidNotifier
import pt.rikmartins.clubemg.mobile.notification.EventSyncWorker
import pt.rikmartins.clubemg.mobile.ui.ScaffoldViewModel
import java.io.File

internal val androidModule = module {
    workerOf(::EventSyncWorker)
    single { getDatabaseBuilder(androidContext()) }
    singleOf(::AndroidNotifier) bind SynchronizeFavouriteEvents.Notifier::class
    single { createDataStore { fileName -> androidContext().filesDir.resolve(fileName).absolutePath } }
    viewModelOf(::ScaffoldViewModel)

    // Remove the old database file at startup
    single<Unit>(createdAtStart = true) {
        val context = androidContext()

        val dbFile = context.getDatabasePath("clubemg.db")
        if (dbFile.exists()) dbFile.delete()

        val shmFile = File(dbFile.absolutePath + "-shm")
        if (shmFile.exists()) shmFile.delete()

        val walFile = File(dbFile.absolutePath + "-wal")
        if (walFile.exists()) walFile.delete()
    }
}