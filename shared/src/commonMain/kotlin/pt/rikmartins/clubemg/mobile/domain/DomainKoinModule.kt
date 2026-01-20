package pt.rikmartins.clubemg.mobile.domain

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import pt.rikmartins.clubemg.mobile.domain.usecase.events.GetCalendarTimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ConsiderRefreshingPeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarCurrentDay
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveRefreshing
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RefreshEvent
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RefreshPeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetBookmarkOfEventId
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents

internal val domainModule = module {
    singleOf(::ConsiderRefreshingPeriod)
    singleOf(::ObserveAllEvents)
    single { ObserveCalendarCurrentDay(get())}
    singleOf(::GetCalendarTimeZone)
    singleOf(::ObserveRefreshing)
    singleOf(::RefreshPeriod)
    single { SynchronizeFavouriteEvents(get(), get(), get()) }
    singleOf(::SetBookmarkOfEventId)
    singleOf(::RefreshEvent)
}
