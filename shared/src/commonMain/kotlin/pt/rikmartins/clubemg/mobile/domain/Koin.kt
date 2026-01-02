package pt.rikmartins.clubemg.mobile.domain

import org.koin.dsl.module
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RequestEventsForDate
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventsSupplier
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarCurrentDay
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveEventCalendar

internal val domainModule = module {
    single { RequestEventsForDate(get()) }
    single { EventsSupplier(get()) }
    single { ObserveEventCalendar(get(), get()) }
    single { ObserveCalendarCurrentDay(get()) }
}
