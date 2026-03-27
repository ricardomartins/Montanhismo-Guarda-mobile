package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase

class ObserveEvent(
    private val eventsProvider: EventsProvider,
    private val bookmarkProvider: BookmarkProvider,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : WatchCase.Function<String, EventWithBookmark?>(dispatcher) {

    override fun execute(param: String): Flow<EventWithBookmark?> =
        combine(
            eventsProvider.observeEventsById(listOf(param)).map { it.firstOrNull() },
            bookmarkProvider.favouriteEventsIds
        ) { event, favouriteEventsIds ->
            event?.let {
                EventWithBookmarkImpl(
                    calendarEvent = it,
                    isBookmarked = favouriteEventsIds.contains(it.id)
                )
            }
        }

    interface EventsProvider {
        fun observeEventsById(ids: Collection<String>): Flow<Collection<CalendarEvent>>
    }

    interface BookmarkProvider {
        val favouriteEventsIds: Flow<Collection<String>>
    }
}
