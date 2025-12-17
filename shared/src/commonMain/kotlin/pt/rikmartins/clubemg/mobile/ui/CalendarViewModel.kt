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
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.minus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import pt.rikmartins.clubemg.mobile.domain.entity.CalendarEvent
import pt.rikmartins.clubemg.mobile.domain.entity.EventImage
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventDateRequester
import pt.rikmartins.clubemg.mobile.domain.usecase.events.TimeZoneSupplier
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class CalendarViewModel(
    private val eventsSupplier: EventsSupplier,
    private val accessSupplier: AccessSupplier,
    private val eventDateRequester: EventDateRequester,
    private val timeZoneSupplier: TimeZoneSupplier,
) : ViewModel() {

    private val today = flow {
        emit(Clock.System.now())
        delay(5.minutes)
    }
        .combine(timeZoneSupplier()) { today, timeZone -> today.toLocalDateTime(timeZone).date }

    private val weeksOfEvents = eventsSupplier()
        .combine(timeZoneSupplier()) { calendarEvents, timeZone ->
            calendarEvents.map { calendarEvent -> SimplifiedEvent(calendarEvent, timeZone) }
                .fold(mutableMapOf<LocalDate, MutableList<SimplifiedEvent>>()) { acc, simplifiedEvent ->
                    acc.apply {
                        simplifiedEvent.toWeeks().forEach { monday ->
                            getOrPut(monday) { mutableListOf() }.add(simplifiedEvent)
                        }
                    }
                }
                .map { (monday, events) -> WeekOfEvents(monday, events) }
                .sortAndPad(timeZone)
        }

    private fun SimplifiedEvent.toWeeks(): List<LocalDate> = buildList {
        val startDate = range.start
        val daysFromMonday = startDate.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal
        var currentMonday = startDate.minus(daysFromMonday, DateTimeUnit.DAY)

        while (currentMonday <= range.endInclusive) {
            add(currentMonday)
            currentMonday = currentMonday.plus(1, DateTimeUnit.WEEK)
        }
    }

    private fun List<WeekOfEvents>.sortAndPad(
        timeZone: TimeZone,
        padSize: DatePeriod = DatePeriod(months = 6)
    ): List<WeekOfEvents> = sortedBy { it.monday }.run {
        val today = Clock.System.todayIn(timeZone)
        val firstEventDate = firstOrNull()?.monday
        val lastEventDate = lastOrNull()?.monday?.plus(6, DateTimeUnit.DAY)

        val startDate = (firstEventDate?.let { minOf(today, it) } ?: today).minus(padSize)
        val endDate = (lastEventDate?.let { maxOf(today, it) } ?: today).plus(padSize)

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
        val monday: LocalDate,
        val events: List<SimplifiedEvent>,
    )

    class SimplifiedEvent(
        private val calendarEvent: CalendarEvent,
        private val timeZone: TimeZone,
    ) {
        val id: String
            get() = calendarEvent.id
        val title: String
            get() = calendarEvent.title
        val url: String
            get() = calendarEvent.url
        val description: String
            get() = calendarEvent.description
        val range: LocalDateRange by lazy {
            with(calendarEvent) {
                startDate.toLocalDate(timeZone)..endDate.toLocalDate(timeZone)
            }
        }
        val sortedImages: List<EventImage> by lazy { calendarEvent.images.sortedBy { it.fileSize } }

        private fun Instant.toLocalDate(timeZone: TimeZone): LocalDate =
            toLocalDateTime(timeZone).date
    }

    data class Model(
        val weeksOfEvents: List<WeekOfEvents>,
        val today: LocalDate?,
    )
}
