package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.onStart
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase

class ObserveAllFavouriteEvents(
    private val bookmarkProvider: BookmarkProvider,
    private val eventsProvider: EventsProvider,
) : WatchCase.Supplier<Collection<EventBookmarkWithEvent>>() {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun execute(): Flow<Collection<EventBookmarkWithEvent>> {
        val favouriteEventsIds = bookmarkProvider.favouriteEventsIds
        val favouriteEvents = favouriteEventsIds.flatMapMerge { eventsProvider.observeEventsById(it) }
            .onStart { emit(emptyList()) }

        return combine(favouriteEvents, favouriteEventsIds) { events, favouriteEventsIds ->
            favouriteEventsIds.map { eventId ->
                EventBookmarkWithEventImpl(
                    id = eventId,
                    isBookmarked = true,
                    event = events.find { it.id == eventId }
                )
            }
        }
    }

    interface EventsProvider {
        fun observeEventsById(ids: Collection<String>): Flow<Collection<CalendarEvent>>
    }

    interface BookmarkProvider {
        val favouriteEventsIds: Flow<Collection<String>>
    }

    private data class EventBookmarkWithEventImpl(
        override val id: String,
        override val isBookmarked: Boolean,
        override val event: CalendarEvent?
    ) : EventBookmarkWithEvent
}