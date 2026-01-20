package pt.rikmartins.clubemg.mobile.ui

import org.koin.dsl.module

internal val uiModule = module {
    factory { CalendarViewModel(
        considerRefreshingPeriod = get(),
        observeCalendarCurrentDay = get(),
        observeAllEvents = get(),
        getCalendarTimeZone = get(),
        observeRefreshing = get(),
        refreshPeriod = get(),
        setBookmarkOfEventId = get(),
        refreshEvent = get(),
    ) }
//    factory { DetailViewModel(get()) }
}