package pt.rikmartins.clubemg.mobile.ui

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val uiModule = module {
    factoryOf(::CalendarViewModel)
//    factory { DetailViewModel(get()) }
}