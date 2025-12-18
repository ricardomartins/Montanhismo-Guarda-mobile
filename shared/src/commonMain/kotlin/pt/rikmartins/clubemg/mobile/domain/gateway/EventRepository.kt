package pt.rikmartins.clubemg.mobile.domain.gateway

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
interface EventRepository {
    val localAccess: Flow<Boolean>
    val remoteAccess: Flow<Boolean>
    suspend fun requestDate(date: LocalDate)
    val providedStartDate: Flow<Instant?>
    val providedEndDate: Flow<Instant?>
    val events: Flow<List<CalendarEvent>>
    val eventsTimezone: Flow<TimeZone>
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