package pt.rikmartins.clubemg.mobile.domain.entity

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
interface CalendarEvent {
    val id: String
    val creationDate: Instant
    val modifiedDate: Instant
    val title: String
    val url: String
    val description: String
    val allDay: Boolean
    val startDate: Instant
    val endDate: Instant
    val images: List<EventImage>
}

interface EventImage {
    val id: String?
    val url: String
    val width: Int
    val height: Int
    val fileSize: Int
}