package pt.rikmartins.clubemg.mobile.di

import pt.rikmartins.clubemg.mobile.data.dataModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun initKoin() = initKoin(emptyList())

fun initKoin(extraModules: List<Module>) {
    startKoin {
        modules(
            dataModule,
            *extraModules.toTypedArray(),
        )
    }
}
