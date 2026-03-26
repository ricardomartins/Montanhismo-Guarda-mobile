package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase

class ObserveAllFavouriteEvents(
    private val bookmarkProvider: BookmarkProvider,
    private val eventsProvider: EventsProvider,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : WatchCase.Supplier<Collection<EventWithBookmark>>(dispatcher) {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun execute(): Flow<Collection<EventWithBookmark>> = bookmarkProvider.favouriteEventsIds
        .flatMapMerge { favouriteEventsIds -> eventsProvider.observeEventsById(favouriteEventsIds) }
        .map { events ->
            events.map { event -> EventWithBookmarkImpl(calendarEvent = event, isBookmarked = true) }
        }

    interface EventsProvider {
        fun observeEventsById(ids: Collection<String>): Flow<Collection<CalendarEvent>>
    }

    interface BookmarkProvider {
        val favouriteEventsIds: Flow<Collection<String>>
    }
}