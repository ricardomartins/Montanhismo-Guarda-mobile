package pt.rikmartins.clubemg.mobile.ui

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.events.CalendarEvent
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventBookmarkWithEvent
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventImage
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventStatusType
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventWithBookmark
import pt.rikmartins.clubemg.mobile.domain.usecase.events.toLocalDate

data class Model(
    val weeksOfEvents: List<WeekOfEvents>,
    val today: LocalDate?,
)

data class WeekOfEvents(
    val monday: LocalDate,
    val events: List<SimplifiedEvent> = emptyList(),
)

class SimplifiedEvent(
    val calendarEvent: EventWithBookmark,
    timeZone: TimeZone,
) {
    val id: String
        get() = calendarEvent.id
    val title: String
        get() = calendarEvent.title
    val url: String
        get() = calendarEvent.url
    val range: LocalDateRange = with(calendarEvent) {
        startDate.toLocalDate(timeZone)..endDate.toLocalDate(timeZone)
    }
    val isBookmarked: Boolean
        get() = calendarEvent.isBookmarked
    val isCancelled: Boolean
        get() = calendarEvent.eventStatusType == EventStatusType.Cancelled

    val isPostponed: Boolean
        get() = calendarEvent.eventStatusType == EventStatusType.Postponed

    /**
     * A list of [EventImage]s associated with the event, sorted by file size in ascending order.
     *
     * This property is lazily initialized.
     */
    val sortedImages: List<EventImage> by lazy { calendarEvent.images.sortedBy { it.fileSize } }
}

data class UiEventBookmarkWithEvent(
    override val id: String,
    override val isBookmarked: Boolean,
    override val event: CalendarEvent?,
    val imageUrl: String?,
    val timeZone: TimeZone,
) : EventBookmarkWithEvent {

    val range: LocalDateRange? = event?.run {
        startDate.toLocalDate(timeZone)..endDate.toLocalDate(timeZone)
    }
}
