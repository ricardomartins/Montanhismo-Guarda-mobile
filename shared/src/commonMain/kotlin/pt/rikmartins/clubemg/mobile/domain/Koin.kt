package pt.rikmartins.clubemg.mobile.domain

import org.koin.dsl.module
import pt.rikmartins.clubemg.mobile.domain.usecase.events.AccessSupplier
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventDateRequester
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventsSupplier
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarCurrentDay
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveEventCalendar
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal val domainModule = module {
    single { AccessSupplier(get()) }
    single { EventDateRequester(get()) }
    single { EventsSupplier(get()) }
    single { ObserveEventCalendar(get(), get()) }
    single { ObserveCalendarCurrentDay(get()) }
    single { ObserveCalendarCurrentDay(get()) }
}
