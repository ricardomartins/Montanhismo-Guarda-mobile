package pt.rikmartins.clubemg.mobile.domain

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import pt.rikmartins.clubemg.mobile.domain.usecase.events.GetCalendarTimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetRelevantDatePeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarCurrentDay
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveRefreshingRanges
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RefreshPeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetBookmarkOfEventId
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents

internal val domainModule = module {
    singleOf(::SetRelevantDatePeriod)
    singleOf(::ObserveAllEvents)
    single { ObserveCalendarCurrentDay(get())}
    singleOf(::GetCalendarTimeZone)
    singleOf(::ObserveRefreshingRanges)
    singleOf(::RefreshPeriod)
    single { SynchronizeFavouriteEvents(get(), get(), get()) }
    singleOf(::SetBookmarkOfEventId)
}
