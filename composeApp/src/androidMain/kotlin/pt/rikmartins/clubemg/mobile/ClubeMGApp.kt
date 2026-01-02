package pt.rikmartins.clubemg.mobile

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pt.rikmartins.clubemg.mobile.cache.DatabaseDriverFactory

class ClubeMGApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val appModule = module {
        single<CoroutineScope> { applicationScope }
        single { DatabaseDriverFactory(this@ClubeMGApp) }
    }

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@ClubeMGApp)
            modules(appModule)
        }
    }
}
