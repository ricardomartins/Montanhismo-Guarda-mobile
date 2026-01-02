package pt.rikmartins.clubemg.mobile.domain

import org.koin.dsl.module
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RequestEventsForDate
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventsSupplier
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarCurrentDay
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarTimeZone

internal val domainModule = module {
    single { RequestEventsForDate(get()) }
    single { EventsSupplier(get()) }
    single { ObserveAllEvents(get()) }
    single { ObserveCalendarCurrentDay(get()) }
    single { ObserveCalendarTimeZone(get()) }
}
