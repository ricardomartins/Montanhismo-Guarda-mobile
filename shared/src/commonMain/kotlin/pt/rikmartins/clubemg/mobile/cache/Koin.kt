package pt.rikmartins.clubemg.mobile.cache

import org.koin.dsl.module

internal val cacheModule = module {
    single { createDatabase(get()) }
}
