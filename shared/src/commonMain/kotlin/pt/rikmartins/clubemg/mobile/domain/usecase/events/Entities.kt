package pt.rikmartins.clubemg.mobile.domain.usecase.events

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
}

interface EventImage {
    val id: String?
    val url: String
    val width: Int
    val height: Int
    val fileSize: Int
}

interface MergedEvent : CalendarEvent {
    val isBookmarked: Boolean
}

data class EventDiff(
    val oldEvent: CalendarEvent,
    val newEvent: CalendarEvent,
)
