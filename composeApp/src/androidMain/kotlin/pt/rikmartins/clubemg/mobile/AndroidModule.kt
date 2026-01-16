package pt.rikmartins.clubemg.mobile

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import pt.rikmartins.clubemg.mobile.cache.DatabaseDriverFactory
import pt.rikmartins.clubemg.mobile.datastore.createDataStore
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents
import pt.rikmartins.clubemg.mobile.ui.notification.AndroidNotifier
import pt.rikmartins.clubemg.mobile.ui.notification.EventSyncWorker

internal val androidModule = module {
    workerOf(::EventSyncWorker)
    singleOf(::DatabaseDriverFactory)
    singleOf(::AndroidNotifier) bind SynchronizeFavouriteEvents.Notifier::class
    single { createDataStore { fileName -> androidContext().filesDir.resolve(fileName).absolutePath } }
}