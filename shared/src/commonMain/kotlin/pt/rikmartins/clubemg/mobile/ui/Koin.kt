package pt.rikmartins.clubemg.mobile.ui

import org.koin.dsl.module

internal val uiModule = module {
    factory { CalendarViewModel(
        setRelevantDatePeriod = get(),
        observeCalendarCurrentDay = get(),
        observeAllEvents = get(),
        getCalendarTimeZone = get(),
        observeRefreshingRanges = get(),
        refreshPeriod = get(),
        setBookmarkOfEventId = get(),
    ) }
//    factory { DetailViewModel(get()) }
}