package pt.rikmartins.clubemg.mobile.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.binds
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

    // FIXME
    single {
        object : SynchronizeFavouriteEvents.BookmarkProvider, SetBookmarkOfEventId.BookmarkProvider,
            ObserveAllEvents.BookmarkProvider {

            val dummyStorage = MutableStateFlow(setOf("5001", "5610", "5588"))

            override suspend fun getAllBookmarkedEventsIds(): Collection<String> = dummyStorage.value

            override suspend fun addBookmark(eventId: String) {
                dummyStorage.update { it + eventId }
            }

            override suspend fun removeBookmark(eventId: String) {
                dummyStorage.update { it - eventId }
            }

            override val favouriteEventsIds: Flow<Collection<String>>
                get() = dummyStorage
        }
    } binds arrayOf(
        SynchronizeFavouriteEvents.BookmarkProvider::class,
        SetBookmarkOfEventId.BookmarkProvider::class,
        ObserveAllEvents.BookmarkProvider::class,
    )
}
