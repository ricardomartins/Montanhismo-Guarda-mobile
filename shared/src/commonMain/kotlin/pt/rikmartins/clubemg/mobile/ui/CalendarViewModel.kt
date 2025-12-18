package pt.rikmartins.clubemg.mobile.ui

import pt.rikmartins.clubemg.mobile.domain.usecase.events.AccessSupplier
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventsSupplier
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import pt.rikmartins.clubemg.mobile.domain.entity.WeekOfEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveEventCalendar
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventDateRequester
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarCurrentDay
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CalendarViewModel(
    private val eventsSupplier: EventsSupplier,
    private val accessSupplier: AccessSupplier,
    private val eventDateRequester: EventDateRequester,
    private val observeCalendarCurrentDay: ObserveCalendarCurrentDay,
    private val observeEventCalendar: ObserveEventCalendar,
) : ViewModel() {

    private fun LocalDateRange.toWeeks(): List<LocalDate> = buildList {
        val daysFromMonday = start.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal
        var currentMonday = start.minus(daysFromMonday, DateTimeUnit.DAY)

        while (currentMonday <= endInclusive) {
            add(currentMonday)
            currentMonday = currentMonday.plus(1, DateTimeUnit.WEEK)
        }
    }

    val model = combine(observeEventCalendar(), observeCalendarCurrentDay()) { eventCalendar, currentDay ->
        Model(
            eventCalendar,
            currentDay
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Model(emptyList(), null))

    data class Model(
        val weeksOfEvents: List<WeekOfEvents>,
        val today: LocalDate?,
    )

    fun requestDateRange(dateRange: ClosedRange<LocalDate>) {
        viewModelScope.launch {
            eventDateRequester(dateRange.start)
            eventDateRequester(dateRange.endInclusive)
        }
    }
}
