package pt.rikmartins.clubemg.mobile.ui

import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import pt.rikmartins.clubemg.mobile.domain.entity.WeekOfEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveEventCalendar
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RequestEventsForDate
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarCurrentDay
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CalendarViewModel(
    private val requestEventsForDate: RequestEventsForDate,
    private val observeCalendarCurrentDay: ObserveCalendarCurrentDay,
    private val observeEventCalendar: ObserveEventCalendar,
) : ViewModel() {

    val model = combine(observeEventCalendar(), observeCalendarCurrentDay()) { eventCalendar, currentDay ->
        Model(
            eventCalendar,
            currentDay
        )
    }
        .onStart {
            emitAll(
                observeCalendarCurrentDay().map { currentDay ->
                    Model(
                        weeksOfEvents = (currentDay..currentDay.plus(30, DateTimeUnit.DAY)).toWeeks()
                            .map { monday -> WeekOfEvents(monday, emptyList()) },
                        today = currentDay
                    )
                }.take(1)
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Model(emptyList(), null))

    data class Model(
        val weeksOfEvents: List<WeekOfEvents>,
        val today: LocalDate?,
    )

    fun requestDateRange(dateRange: LocalDateRange) {
        viewModelScope.launch {
            requestEventsForDate(dateRange.start)
            requestEventsForDate(dateRange.endInclusive)
        }
    }

    fun LocalDateRange.toWeeks(): List<LocalDate> = buildList {
        val daysFromMonday = start.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal
        var currentMonday = start.minus(daysFromMonday, DateTimeUnit.DAY)

        while (currentMonday <= endInclusive) {
            add(currentMonday)
            currentMonday = currentMonday.plus(1, DateTimeUnit.WEEK)
        }
    }
}
