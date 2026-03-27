package pt.rikmartins.clubemg.mobile.domain

import org.koin.dsl.module
import pt.rikmartins.clubemg.mobile.domain.usecase.events.GetCalendarTimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ConsiderRefreshingPeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllFavouriteEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarCurrentDay
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveEvent
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveRefreshing
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RefreshEvent
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RefreshPeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetBookmarkOfEventId
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents

internal val domainModule = module {
    single { ConsiderRefreshingPeriod(get()) }
    single { ObserveAllEvents(get(), get()) }
    single { ObserveCalendarCurrentDay(get()) }
    single { GetCalendarTimeZone(get()) }
    single { ObserveRefreshing(get()) }
    single { RefreshPeriod(get()) }
    single { SynchronizeFavouriteEvents(get(), get(), get()) }
    single { SetBookmarkOfEventId(get()) }
    single { RefreshEvent(get()) }
    single { ObserveAllFavouriteEvents(get(), get()) }
    single { ObserveEvent(get(), get()) }
}
