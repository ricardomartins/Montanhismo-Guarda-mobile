package pt.rikmartins.clubemg.mobile

import pt.rikmartins.clubemg.mobile.data.dataModule
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import pt.rikmartins.clubemg.mobile.domain.domainModule
import pt.rikmartins.clubemg.mobile.ui.uiModule

fun initKoin(extraModules: List<Module> = emptyList()) {
    startKoin {
        val modules = modules(
            dataModule,
            domainModule,
            uiModule,
            *extraModules.toTypedArray(),
        )
    }
}
