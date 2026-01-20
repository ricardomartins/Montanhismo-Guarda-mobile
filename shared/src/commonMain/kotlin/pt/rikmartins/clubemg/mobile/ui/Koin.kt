package pt.rikmartins.clubemg.mobile.ui

import org.koin.dsl.module

internal val uiModule = module {
    factory { CalendarViewModel(
        getEventsInDatePeriod = get(),
        observeCalendarCurrentDay = get(),
        observeAllEvents = get(),
        getCalendarTimeZone = get(),
        observeRefreshing = get(),
        refreshPeriod = get(),
        setBookmarkOfEventId = get(),
    ) }
//    factory { DetailViewModel(get()) }
}