package pt.rikmartins.clubemg.mobile.domain.gateway

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

interface EventRepository {
    suspend fun setRelevantDatePeriod(period: LocalDateRange)
    val events: Flow<List<CalendarEvent>>
    val eventsTimezone: Flow<TimeZone>
    suspend fun refreshCache()
    val refreshingRanges: Flow<Set<LocalDateRange>>
}

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