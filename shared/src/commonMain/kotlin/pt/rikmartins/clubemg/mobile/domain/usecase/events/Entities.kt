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
    val images: List<EventImage> // TODO: Change to Collection
    val eventStatusType: EventStatusType?
    val eventAttendanceMode: EventAttendanceMode?
    val categories: Collection<String>
    val tags: Collection<String>
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

// TODO: Turn to an interface
data class EventDiff(
    val oldEvent: CalendarEvent,
    val newEvent: CalendarEvent,
)

interface RefreshState {
    val singularEventIds: Collection<String>
    val dateRanges: Collection<LocalDateRange>
}

internal data class EventWithBookmarkImpl(
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
    override val categories: Collection<String>,
    override val tags: Collection<String>,
) : EventWithBookmark {

    constructor(calendarEvent: CalendarEvent, isBookmarked: Boolean) : this(
        id = calendarEvent.id,
        creationDate = calendarEvent.creationDate,
        modifiedDate = calendarEvent.modifiedDate,
        title = calendarEvent.title,
        url = calendarEvent.url,
        startDate = calendarEvent.startDate,
        endDate = calendarEvent.endDate,
        enrollmentUrl = calendarEvent.enrollmentUrl,
        images = calendarEvent.images,
        isBookmarked = isBookmarked,
        eventStatusType = calendarEvent.eventStatusType,
        eventAttendanceMode = calendarEvent.eventAttendanceMode,
        categories = calendarEvent.categories,
        tags = calendarEvent.tags,
    )
}

