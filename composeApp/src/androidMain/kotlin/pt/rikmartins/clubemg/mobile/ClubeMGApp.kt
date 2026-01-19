package pt.rikmartins.clubemg.mobile

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import pt.rikmartins.clubemg.mobile.ui.notification.setupNotificationChannels
import pt.rikmartins.clubemg.mobile.ui.notification.scheduleEventSync

class ClubeMGApp : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG

        initKoin {
            androidContext(this@ClubeMGApp)
            workManagerFactory()
            modules(androidModule)
        }

        setupNotificationChannels()
        scheduleEventSync()
    }
}
