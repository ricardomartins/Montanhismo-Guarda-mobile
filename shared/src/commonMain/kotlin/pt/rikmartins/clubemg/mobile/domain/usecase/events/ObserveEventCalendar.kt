package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import pt.rikmartins.clubemg.mobile.domain.entity.SimplifiedEvent
import pt.rikmartins.clubemg.mobile.domain.entity.WeekOfEvents
import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase

class ObserveEventCalendar(
    private val eventRepository: EventRepository,
    private val observeCalendarCurrentDay: ObserveCalendarCurrentDay,
) : WatchCase.Supplier<List<WeekOfEvents>>() {

    override fun execute(): Flow<List<WeekOfEvents>> = combine(
        eventRepository.events,
        observeCalendarCurrentDay(),
        eventRepository.eventsTimezone
    ) { calendarEvents, currentDay, timeZone ->
        calendarEvents.map { calendarEvent -> SimplifiedEvent(calendarEvent, timeZone) }
            .fold(mutableMapOf<LocalDate, MutableList<SimplifiedEvent>>()) { acc, simplifiedEvent ->
                acc.apply {
                    simplifiedEvent.range.toWeeks().forEach { monday ->
                        getOrPut(monday) { mutableListOf() }.add(simplifiedEvent)
                    }
                }
            }
            .map { (monday, events) -> WeekOfEvents(monday, events) }
            .sortAndPad(currentDay)
    }

    fun LocalDateRange.toWeeks(): List<LocalDate> = buildList {
        val daysFromMonday = start.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal
        var currentMonday = start.minus(daysFromMonday, DateTimeUnit.DAY)

        while (currentMonday <= endInclusive) {
            add(currentMonday)
            currentMonday = currentMonday.plus(1, DateTimeUnit.WEEK)
        }
    }

    private fun List<WeekOfEvents>.sortAndPad(
        currentDay: LocalDate,
        padSize: DatePeriod = DatePeriod(months = 6)
    ): List<WeekOfEvents> = sortedBy { it.monday }.run {
        val firstEventDate = firstOrNull()?.monday
        val lastEventDate = lastOrNull()?.monday?.plus(6, DateTimeUnit.DAY)

        val startDate = (firstEventDate?.let { minOf(currentDay, it) } ?: currentDay).minus(padSize)
        val endDate = (lastEventDate?.let { maxOf(currentDay, it) } ?: currentDay).plus(padSize)

        var currentMonday = startDate.minus(startDate.dayOfWeek.ordinal, DateTimeUnit.DAY)

        val iterator = iterator()
        var nextExistingWeek = iterator.takeIf { it.hasNext() }?.next()

        buildList {
            while (currentMonday <= endDate) {
                val nextExistingWeekValue = nextExistingWeek
                if (currentMonday == nextExistingWeekValue?.monday) {
                    add(nextExistingWeekValue)
                    nextExistingWeek = iterator.takeIf { it.hasNext() }?.next()
                } else {
                    add(WeekOfEvents(currentMonday, emptyList()))
                }
                currentMonday = currentMonday.plus(1, DateTimeUnit.WEEK)
            }
        }
    }
}
