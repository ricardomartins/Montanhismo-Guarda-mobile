package pt.rikmartins.clubemg.mobile.ui

import org.koin.dsl.module

internal val uiModule = module {
    factory { CalendarViewModel(get(), get(), get(), get()) }
//    factory { DetailViewModel(get()) }
}