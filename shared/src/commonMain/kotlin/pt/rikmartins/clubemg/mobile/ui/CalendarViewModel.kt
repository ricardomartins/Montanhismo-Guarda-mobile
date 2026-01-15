package pt.rikmartins.clubemg.mobile.ui

import co.touchlab.kermit.Logger
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import pt.rikmartins.clubemg.mobile.domain.usecase.events.GetCalendarTimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetRelevantDatePeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarCurrentDay
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveRefreshingRanges
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RefreshPeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetBookmarkOfEventId
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents
import pt.rikmartins.clubemg.mobile.ui.WeekUtils.getMondaysInRange
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, FlowPreview::class)
class CalendarViewModel(
    private val setRelevantDatePeriod: SetRelevantDatePeriod,
    observeCalendarCurrentDay: ObserveCalendarCurrentDay,
    observeAllEvents: ObserveAllEvents,
    private val getCalendarTimeZone: GetCalendarTimeZone,
    observeRefreshingRanges: ObserveRefreshingRanges,
    private val refreshPeriod: RefreshPeriod,
    private val setBookmarkOfEventId: SetBookmarkOfEventId,
    private val logger: Logger = Logger.withTag(SynchronizeFavouriteEvents::class.simpleName!!)
) : ViewModel() {

    private val visibleDates = MutableStateFlow<LocalDateRange?>(null)
    private val filteredVisibleDates = visibleDates.filterNotNull().sample(500)

    init {
        viewModelScope.launch {
            filteredVisibleDates.collect {
                setRelevantDatePeriod(
                    it.start.minus(1, DateTimeUnit.MONTH)..it.endInclusive.plus(3, DateTimeUnit.MONTH)
                )
            }
        }
    }

    private val calendarCurrentDay = observeCalendarCurrentDay()

    private val datesThatWereEverVisible = filteredVisibleDates.scan(LocalDateRange.EMPTY) { acc, value ->
        if (acc == LocalDateRange.EMPTY) value
        else minOf(acc.start, value.start)..maxOf(acc.endInclusive, value.endInclusive)
    }
        .filter { it != LocalDateRange.EMPTY }
        .distinctUntilChanged { old, new -> old.start == new.start && old.endInclusive == new.endInclusive }

    val model = combine(
        combine(datesThatWereEverVisible.sample(500), calendarCurrentDay) { weekLimits, currentDay ->
            val newStart = maxOf(
                weekLimits.start.minus(6, DateTimeUnit.MONTH),
                currentDay.minus(2, DateTimeUnit.YEAR)
            )
            val newEnd = minOf(
                weekLimits.endInclusive.plus(6, DateTimeUnit.MONTH),
                currentDay.plus(2, DateTimeUnit.YEAR)
            )

            newStart..newEnd to currentDay
        },
        observeAllEvents(),
        observeRefreshingRanges().map { it.isNotEmpty() }.debounce(250.milliseconds),
    ) { (weekLimits, currentDay), events, isRefreshing ->
        val calendarTimeZone = getCalendarTimeZone()

        val mondays = getMondaysInRange(weekLimits)
        val simplifiedEvents = events.map { SimplifiedEvent(it, calendarTimeZone) }

        selectedEvent.value?.id?.let { selectedEventId ->
            simplifiedEvents.find { it.id == selectedEventId }?.let { selectedEvent.value = it }
        }

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
        }
            .map { (monday, events) -> WeekOfEvents(monday, events) }
            .sortedBy { it.monday }
            .let { Model(it, currentDay, isRefreshing) }
    }.onStart {
        emitAll(
            calendarCurrentDay
                .map { currentDay ->
                    Model(
                        weeksOfEvents = getMondaysInRange(currentDay..currentDay.plus(30, DateTimeUnit.DAY))
                            .map { WeekOfEvents(it, emptyList()) },
                        today = currentDay,
                        isRefreshing = true,
                    )
                }
                .take(1)
        )
    }.onEach { logger.v { "Model: $it" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Model(emptyList(), null, true))

    val selectedEvent = MutableStateFlow<SimplifiedEvent?>(null)

    fun setSelectedEvent(event: SimplifiedEvent) {
        selectedEvent.value = event
    }

    fun unsetSelectedEvent() {
        selectedEvent.value = null
    }


    fun notifyViewedDates(dateRange: LocalDateRange) {
        visibleDates.value = dateRange
    }

    fun forceSync() {
        viewModelScope.launch {
            visibleDates.value?.let {
                refreshPeriod(
                    it.start.minus(1, DateTimeUnit.MONTH)..it.endInclusive.plus(1, DateTimeUnit.MONTH)
                )
            }
        }
    }

    fun setBookmarkOfEventTo(event: SimplifiedEvent, isBookmarked: Boolean) {
        viewModelScope.launch {
            setBookmarkOfEventId(event.id, isBookmarked)
        }
    }
}
