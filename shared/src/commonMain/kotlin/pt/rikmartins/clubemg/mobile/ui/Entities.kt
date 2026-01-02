package pt.rikmartins.clubemg.mobile.ui

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import pt.rikmartins.clubemg.mobile.domain.gateway.CalendarEvent
import pt.rikmartins.clubemg.mobile.domain.gateway.EventImage
import pt.rikmartins.clubemg.mobile.domain.usecase.events.toLocalDate
import kotlin.time.ExperimentalTime

data class WeekOfEvents(
    val monday: LocalDate,
    val events: List<SimplifiedEvent> = emptyList(),
)

@OptIn(ExperimentalTime::class)
class SimplifiedEvent(
    private val calendarEvent: CalendarEvent,
    timeZone: TimeZone,
) {
    val id: String
        get() = calendarEvent.id
    val title: String
        get() = calendarEvent.title
    val url: String
        get() = calendarEvent.url
    val description: String
        get() = calendarEvent.description
    val range: LocalDateRange = with(calendarEvent) {
        startDate.toLocalDate(timeZone)..endDate.toLocalDate(timeZone)
    }

    /**
     * A list of [EventImage]s associated with the event, sorted by file size in ascending order.
     *
     * This property is lazily initialized.
     */
    val sortedImages: List<EventImage> by lazy { calendarEvent.images.sortedBy { it.fileSize } }
}
