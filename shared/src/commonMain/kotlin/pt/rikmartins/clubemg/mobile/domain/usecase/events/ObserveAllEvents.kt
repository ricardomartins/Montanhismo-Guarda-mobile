package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase
import kotlin.time.Instant

class ObserveAllEvents(private val eventsProvider: EventsProvider, private val bookmarkProvider: BookmarkProvider) : WatchCase.Supplier<List<MergedEvent>>() {

    override fun execute(): Flow<List<MergedEvent>> =
        combine(eventsProvider.events, bookmarkProvider.favouriteEventsIds) { events, favouriteEventsIds ->
            val favouriteEventsIds = favouriteEventsIds.toMutableSet()

            events.map { event ->
                MergedEventImpl(
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

    private data class MergedEventImpl(
        override val id: String,
        override val creationDate: Instant,
        override val modifiedDate: Instant,
        override val title: String,
        override val url: String,
        override val startDate: Instant,
        override val endDate: Instant,
        override val enrollmentUrl: String,
        override val images: List<EventImage>,
        override val isBookmarked: Boolean,
        override val eventStatusType: EventStatusType?,
        override val eventAttendanceMode: EventAttendanceMode?,
    ) : MergedEvent
}