package pt.rikmartins.clubemg.mobile.domain.usecase.events

import pt.rikmartins.clubemg.mobile.domain.entity.CalendarEvent
import pt.rikmartins.clubemg.mobile.domain.entity.WeekOfEvents
import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class GetWeeklyEventsFlow(private val eventRepository: EventRepository) : WatchCase.Supplier<List<WeekOfEvents>>() {

    override fun get(): Flow<List<WeekOfEvents>> = eventRepository.events.map { calendarEvents ->
        calendarEvents.fold(mutableListOf<WeekOfEvents>()) { acc, event ->
            acc

            event.startDate
        }







        val weeks = mutableMapOf<OpenEndRange<Instant>, MutableCollection<CalendarEvent>>()

        calendarEvents.forEach { event ->
            val startWeek = weeks.filter { (range, _) -> range.contains(event.startDate) }
                .firstNotNullOfOrNull { mapEntry -> mapEntry.value }
                ?: mutableListOf<CalendarEvent>().also { weekEvents ->
                    weeks[event.startDate.asLocalWeekRange(eventRepository.eventsTimezone)] = weekEvents
                }

            val endWeek = weeks.filter { (range, _) -> range.contains(event.endDate) }
                .firstNotNullOfOrNull { mapEntry -> mapEntry.value }
                ?: mutableListOf<CalendarEvent>().also { weekEvents ->
                    weeks[event.endDate.asLocalWeekRange(eventRepository.eventsTimezone)] = weekEvents
                }

            startWeek.add(event)
            if (startWeek !== endWeek) endWeek.add(event)
        }

        weeks
    }

    private fun Instant.asLocalWeekRange(timezone: TimeZone): OpenEndRange<Instant> {
        val localDate = toLocalDateTime(timezone).date
        val previousMonday = LocalDate.fromEpochDays(localDate.toEpochDays() - localDate.dayOfWeek.isoDayNumber + 1)
            .atStartOfDayIn(timezone)
        val nextMonday = LocalDate.fromEpochDays(localDate.toEpochDays() + (8 - localDate.dayOfWeek.isoDayNumber))
            .atStartOfDayIn(timezone)
        return previousMonday..<nextMonday
    }
}