package pt.rikmartins.clubemg.mobile.domain.gateway

import pt.rikmartins.clubemg.mobile.domain.entity.CalendarEvent
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
