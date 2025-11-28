package pt.rikmartins.clubemg.mobile

import android.app.Application

class ClubeMGApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
