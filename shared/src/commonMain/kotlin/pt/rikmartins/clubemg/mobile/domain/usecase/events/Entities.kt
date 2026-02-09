package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.datetime.LocalDateRange
import kotlin.time.Instant

interface CalendarEvent {
    val id: String
    val creationDate: Instant
    val modifiedDate: Instant
    val title: String
    val url: String
    val startDate: Instant
    val endDate: Instant
    val enrollmentUrl: String
    val images: List<EventImage>
    val eventStatusType: EventStatusType?
    val eventAttendanceMode: EventAttendanceMode?
}

interface EventImage {
    val id: String?
    val url: String
    val width: Int
    val height: Int
    val fileSize: Int
}

enum class EventStatusType {
    Cancelled, MovedOnline, Postponed, Rescheduled, Scheduled;
}

enum class EventAttendanceMode {
    Mixed, Offline, Online;
}

interface EventWithBookmark : CalendarEvent {
    val isBookmarked: Boolean
}

interface EventBookmarkWithEvent {
    val id: String
    val isBookmarked: Boolean
    val event: CalendarEvent?
}

// TODO: Turn to an interface
data class EventDiff(
    val oldEvent: CalendarEvent,
    val newEvent: CalendarEvent,
)

interface RefreshState {
    val singularEventIds: Collection<String>
    val dateRanges: Collection<LocalDateRange>
}
