package pt.rikmartins.clubemg.mobile.ui

import org.koin.dsl.module

internal val uiModule = module {
    factory { CalendarViewModel(
        setRelevantDatePeriod = get(),
        observeCalendarCurrentDay = get(),
        observeAllEvents = get(),
        observeCalendarTimeZone = get(),
        observeRefreshingRanges = get(),
        refreshCache = get(),
    ) }
//    factory { DetailViewModel(get()) }
}