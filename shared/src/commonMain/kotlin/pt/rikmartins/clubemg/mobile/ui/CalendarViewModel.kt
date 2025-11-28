package pt.rikmartins.clubemg.mobile.ui

import pt.rikmartins.clubemg.mobile.domain.usecase.events.AccessSupplier
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventsSupplier
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import pt.rikmartins.clubemg.mobile.domain.entity.CalendarEvent
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventDateRequester
import pt.rikmartins.clubemg.mobile.domain.usecase.events.TimeZoneSupplier
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CalendarViewModel(
    private val eventsSupplier: EventsSupplier,
    private val accessSupplier: AccessSupplier,
    private val eventDateRequester: EventDateRequester,
    private val timeZoneSupplier: TimeZoneSupplier,
) : ViewModel() {

    private val today = flow {
        emit(Clock.System.now())
        delay(15000) // 15 seconds
    }
        .combine(timeZoneSupplier()) { today, timeZone -> today.toLocalDateTime(timeZone).date }

    private val weeksOfEvents = eventsSupplier().combine(timeZoneSupplier()) { events, timeZone ->
        events.fold(mutableMapOf<LocalDateRange, MutableList<CalendarEvent>>()) { acc, calendarEvent ->
            acc.apply {
                calendarEvent.toWeeks(timeZone).forEach { week ->
                    getOrPut(week) { mutableListOf() }.add(calendarEvent)
                }
            }
        }
            .map { (range, events) -> WeekOfEvents(range, events) }
            .sortAndPad(timeZone)
    }

    private fun CalendarEvent.toWeeks(timeZone: TimeZone): List<LocalDateRange> = buildList {
        val startDate = startDate.toLocalDateTime(timeZone).date
        val endDate = endDate.toLocalDateTime(timeZone).date
        val daysFromMonday = startDate.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal
        var currentStartOfWeek = startDate.minus(daysFromMonday, DateTimeUnit.DAY)

        while (currentStartOfWeek <= endDate) {
            val currentEndOfWeek = currentStartOfWeek.plus(6, DateTimeUnit.DAY)
            add(LocalDateRange(currentStartOfWeek, currentEndOfWeek))
            currentStartOfWeek = currentStartOfWeek.plus(1, DateTimeUnit.WEEK)
        }
    }

    private fun List<WeekOfEvents>.sortAndPad(
        timeZone: TimeZone,
        padSize: DatePeriod = DatePeriod(months = 6)
    ): List<WeekOfEvents> = sortedBy { it.range.first }.run {
        val today = Clock.System.todayIn(timeZone)
        val firstEventDate = firstOrNull()?.range?.first
        val lastEventDate = lastOrNull()?.range?.last

        val startDate = (firstEventDate?.let { minOf(today, it) } ?: today).minus(padSize)
        val endDate = (lastEventDate?.let { maxOf(today, it) } ?: today).plus(padSize)

        var currentStartOfWeek = startDate.minus(startDate.dayOfWeek.ordinal, DateTimeUnit.DAY)

        val iterator = iterator()
        var nextExistingWeek = iterator.takeIf { it.hasNext() }?.next()

        buildList {
            while (currentStartOfWeek <= endDate) {
                val nextExistingWeekValue = nextExistingWeek
                if (currentStartOfWeek == nextExistingWeekValue?.range?.first) {
                    add(nextExistingWeekValue)
                    nextExistingWeek = iterator.takeIf { it.hasNext() }?.next()
                } else {
                    val currentEndOfWeek = currentStartOfWeek.plus(6, DateTimeUnit.DAY)
                    add(
                        WeekOfEvents(
                            LocalDateRange(currentStartOfWeek, currentEndOfWeek),
                            emptyList()
                        )
                    )
                }
                currentStartOfWeek = currentStartOfWeek.plus(1, DateTimeUnit.WEEK)
            }
        }
    }

    val model =
        combine(weeksOfEvents, today) { weeksOfEvents, today -> Model(weeksOfEvents, today) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Model(emptyList(), null))

    fun requestDateRange(dateRange: ClosedRange<LocalDate>) {
        viewModelScope.launch {
            eventDateRequester(dateRange.start)
            eventDateRequester(dateRange.endInclusive)
        }
    }

    data class WeekOfEvents(
        val range: LocalDateRange,
        val events: List<CalendarEvent>,
    )

    data class Model(
        val weeksOfEvents: List<WeekOfEvents>,
        val today: LocalDate?,
    )
}
