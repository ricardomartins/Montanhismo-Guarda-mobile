package pt.rikmartins.clubemg.mobile.domain

import org.koin.dsl.module
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetRelevantDatePeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarCurrentDay
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarTimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveRefreshingRanges
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RefreshCache
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents

internal val domainModule = module {
    single { SetRelevantDatePeriod(get()) }
    single { ObserveAllEvents(get()) }
    single { ObserveCalendarCurrentDay(get()) }
    single { ObserveCalendarTimeZone(get()) }
    single { ObserveRefreshingRanges(get()) }
    single { RefreshCache(get()) }

    // FIXME
    single {
        object : SynchronizeFavouriteEvents.FavouriteProvider {
            override suspend fun getAllFavouriteEventsIds(): Collection<String> = listOf("5001")
        }
    }
}
