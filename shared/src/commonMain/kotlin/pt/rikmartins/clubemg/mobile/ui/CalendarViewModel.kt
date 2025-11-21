package pt.rikmartins.clubemg.mobile.ui

import pt.rikmartins.clubemg.mobile.domain.usecase.events.AccessSupplier
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventsSupplier
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventDateRequester
import pt.rikmartins.clubemg.mobile.domain.usecase.events.TimeZoneSupplier
import pt.rikmartins.clubemg.mobile.ui.entity.WeekOfEvents
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CalendarViewModel(
    private val eventsSupplier: EventsSupplier,
    private val accessSupplier: AccessSupplier,
    private val eventDateRequester: EventDateRequester,
    private val timeZoneSupplier: TimeZoneSupplier,
) : ViewModel() {
    private val weekRangesFlow = timeZoneSupplier().map { timeZone ->
        buildList {
            val today = Clock.System.todayIn(timeZone)
            val twoYearsAgo = today.minus(2, kotlinx.datetime.DateTimeUnit.YEAR)
            val twoYearsHence = today.plus(2, kotlinx.datetime.DateTimeUnit.YEAR)
            val daysFromMonday = twoYearsAgo.dayOfWeek.ordinal - DayOfWeek.MONDAY.ordinal
            var currentStartOfWeek =
                twoYearsAgo.minus(daysFromMonday, kotlinx.datetime.DateTimeUnit.DAY)

            while (currentStartOfWeek <= twoYearsHence) {
                val currentEndOfWeek =
                    currentStartOfWeek.plus(6, kotlinx.datetime.DateTimeUnit.DAY)
                add(LocalDateRange(currentStartOfWeek, currentEndOfWeek))
                currentStartOfWeek =
                    currentStartOfWeek.plus(1, kotlinx.datetime.DateTimeUnit.WEEK)
            }
        }
    }

    val weeksOfEvents: Flow<List<WeekOfEvents>> = eventsSupplier().combine(weekRangesFlow) { events, weekRanges ->



        emptyList()
    }

    fun requestDateRange(dateRange: ClosedRange<LocalDate>) {
        viewModelScope.launch {
            eventDateRequester(dateRange.start)
            eventDateRequester(dateRange.endInclusive)
        }
    }
}
