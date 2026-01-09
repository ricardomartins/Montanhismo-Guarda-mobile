package pt.rikmartins.clubemg.mobile

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.dsl.module
import pt.rikmartins.clubemg.mobile.cache.DatabaseDriverFactory
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventDiff
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents
import pt.rikmartins.clubemg.mobile.work.EventSyncWorker
import pt.rikmartins.clubemg.mobile.work.scheduleEventSync

class ClubeMGApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val appModule = module {
        single<CoroutineScope> { applicationScope }
        single { DatabaseDriverFactory(this@ClubeMGApp) }
        workerOf(::EventSyncWorker)

        // FIXME
        single {
            object : SynchronizeFavouriteEvents.Notifier {
                override suspend fun notifyFavouriteEventsChanged(eventsDiffs: Collection<EventDiff>) {
                    Log.d("FavouriteEventsChanged", eventsDiffs.toString())
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@ClubeMGApp)
            workManagerFactory()
            modules(appModule)
        }

        scheduleEventSync()
    }
}
