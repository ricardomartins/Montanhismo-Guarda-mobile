package pt.rikmartins.clubemg.mobile

import android.app.Application
import pt.rikmartins.clubemg.mobile.di.initKoin
import pt.rikmartins.clubemg.mobile.screens.DetailViewModel
import pt.rikmartins.clubemg.mobile.screens.ListViewModel
import org.koin.dsl.module

class MuseumApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(
            listOf(
                module {
                    factory { ListViewModel(get()) }
                    factory { DetailViewModel(get()) }
                }
            )
        )
    }
}
