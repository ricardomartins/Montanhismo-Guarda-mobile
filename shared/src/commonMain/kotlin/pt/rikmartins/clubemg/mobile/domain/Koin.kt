package pt.rikmartins.clubemg.mobile.domain

import org.koin.dsl.module
import pt.rikmartins.clubemg.mobile.domain.usecase.events.AccessSupplier
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventDateRequester
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventsSupplier
import pt.rikmartins.clubemg.mobile.domain.usecase.events.TimeZoneSupplier

internal val domainModule = module {
    single { AccessSupplier(get()) }
    single { EventDateRequester(get()) }
    single { EventsSupplier(get()) }
    single { TimeZoneSupplier(get()) }
}
