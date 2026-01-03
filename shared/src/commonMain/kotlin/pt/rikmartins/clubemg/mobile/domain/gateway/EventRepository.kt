package pt.rikmartins.clubemg.mobile.domain.gateway

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
interface EventRepository {
    suspend fun requestDate(date: LocalDate)
    val events: Flow<List<CalendarEvent>>
    val eventsTimezone: Flow<TimeZone>
    suspend fun setCacheExpirationDate(expirationDate: Instant)
    val refreshingRanges: Flow<Set<LocalDateRange>>
}

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