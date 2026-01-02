package pt.rikmartins.clubemg.mobile.ui

import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.take
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RequestEventsForDate
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarCurrentDay
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarTimeZone
import pt.rikmartins.clubemg.mobile.ui.WeekUtils.getMondaysInRange
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, FlowPreview::class)
class CalendarViewModel(
    private val requestEventsForDate: RequestEventsForDate,
    observeCalendarCurrentDay: ObserveCalendarCurrentDay,
    observeAllEvents: ObserveAllEvents,
    observeCalendarTimeZone: ObserveCalendarTimeZone,
) : ViewModel() {

    private val relevantDates = MutableStateFlow<LocalDateRange?>(null)

    init {
        viewModelScope.launch {
            relevantDates.collectLatest { relevantDates ->
                relevantDates?.let {
                    requestEventsForDate(it.start.minus(1, DateTimeUnit.MONTH))
                    requestEventsForDate(it.endInclusive.plus(3, DateTimeUnit.MONTH))
                }
            }
        }
    }

    private val calendarCurrentDay = observeCalendarCurrentDay()

    val model = combine(
        combine(relevantDates.filterNotNull().sample(500), calendarCurrentDay) { weekLimits, currentDay ->
            val newStart = maxOf(weekLimits.start.minus(6, DateTimeUnit.MONTH), currentDay.minus(2, DateTimeUnit.YEAR))
            val newEnd = minOf(weekLimits.endInclusive.plus(6, DateTimeUnit.MONTH), currentDay.plus(2, DateTimeUnit.YEAR))

            newStart..newEnd to currentDay
        },
        observeAllEvents(),
        observeCalendarTimeZone(),
    ) { (weekLimits, currentDay), events, calendarTimeZone  ->
        val mondays = getMondaysInRange(weekLimits)
        val simplifiedEvents = events.map { SimplifiedEvent(it, calendarTimeZone) }

        buildMap {
            simplifiedEvents.forEach { simplifiedEvent ->
                val mondayIter = mondays.iterator()
                while (mondayIter.hasNext()) {
                    val currentMonday = mondayIter.next()
                    if (simplifiedEvent.range.endInclusive < currentMonday) break

                    val currentSunday = currentMonday.plus(6, DateTimeUnit.DAY)
                    if (simplifiedEvent.range.start > currentSunday) continue

                    getOrPut(currentMonday) { mutableListOf() }.add(simplifiedEvent)
                }
            }
            mondays.forEach { getOrPut(it) { mutableListOf() } }
        }.map { (monday, events) -> WeekOfEvents(monday, events) }
            .sortedBy { it.monday }
            .let { Model(it, currentDay) }
    }.onStart {
        emitAll(
            calendarCurrentDay
                .map { currentDay ->
                    Model(getMondaysInRange(currentDay..currentDay.plus(30, DateTimeUnit.DAY))
                        .map { WeekOfEvents(it, emptyList()) }, currentDay)
                }
                .take(1)
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Model(emptyList(), null))

    data class Model(
        val weeksOfEvents: List<WeekOfEvents>,
        val today: LocalDate?,
    )

    fun notifyViewedDates(dateRange: LocalDateRange) {
        val relevantDatesValue = relevantDates.value
        if (relevantDatesValue == null) {
            relevantDates.value = dateRange
        } else {
            val newStart = dateRange.start.takeIf { dateRange.start < relevantDatesValue.start }
            val newEnd = dateRange.endInclusive.takeIf { dateRange.endInclusive > relevantDatesValue.endInclusive }

            if (newStart != null || newEnd != null) {
                relevantDates.value =
                    (newStart ?: relevantDatesValue.start)..(newEnd ?: relevantDatesValue.endInclusive)
            }
        }
    }
}
