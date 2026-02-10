package pt.rikmartins.clubemg.mobile.ui

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventWithBookmark

data class Model(
    val weeksOfEvents: List<WeekOfEvents>,
    val today: LocalDate?,
)

data class WeekOfEvents(
    val monday: LocalDate,
    val events: List<UiEventWithBookmark> = emptyList(),
)

interface UiEventWithBookmark {
    val calendarEvent: EventWithBookmark
    val id: String
    val title: String
    val range: LocalDateRange
    val url: String
    val isBookmarked: Boolean
    val preferredImageUrl: String?
}
