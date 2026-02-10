package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase

class ObserveAllEvents(private val eventsProvider: EventsProvider, private val bookmarkProvider: BookmarkProvider) : WatchCase.Supplier<List<EventWithBookmark>>() {

    override fun execute(): Flow<List<EventWithBookmark>> =
        combine(eventsProvider.events, bookmarkProvider.favouriteEventsIds) { events, favouriteEventsIds ->
            val favouriteEventsIds = favouriteEventsIds.toMutableSet()

            events.map { event ->
                EventWithBookmarkImpl(
                    id = event.id,
                    creationDate = event.creationDate,
                    modifiedDate = event.modifiedDate,
                    title = event.title,
                    url = event.url,
                    startDate = event.startDate,
                    endDate = event.endDate,
                    enrollmentUrl = event.enrollmentUrl,
                    images = event.images,
                    eventStatusType = event.eventStatusType,
                    eventAttendanceMode = event.eventAttendanceMode,
                    isBookmarked = favouriteEventsIds.remove(event.id),
                )
            }
        }

    interface EventsProvider {
        val events: Flow<List<CalendarEvent>>
    }

    interface BookmarkProvider {
        val favouriteEventsIds: Flow<Collection<String>>
    }
}