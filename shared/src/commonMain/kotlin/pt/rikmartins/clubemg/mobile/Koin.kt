package pt.rikmartins.clubemg.mobile

import pt.rikmartins.clubemg.mobile.data.dataModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.includes
import pt.rikmartins.clubemg.mobile.cache.cacheModule
import pt.rikmartins.clubemg.mobile.domain.domainModule
import pt.rikmartins.clubemg.mobile.ui.uiModule

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        includes(config)
        modules(cacheModule, dataModule, domainModule, uiModule)
    }
}
