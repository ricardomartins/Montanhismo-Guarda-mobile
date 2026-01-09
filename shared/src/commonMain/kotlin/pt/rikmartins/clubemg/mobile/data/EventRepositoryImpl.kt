package pt.rikmartins.clubemg.mobile.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pt.rikmartins.clubemg.mobile.domain.usecase.events.CalendarEvent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventDiff
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarCurrentDay
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarTimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveRefreshingRanges
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RefreshCache
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetRelevantDatePeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.toLocalDate
import pt.rikmartins.clubemg.mobile.nextDay
import pt.rikmartins.clubemg.mobile.previousDay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, FlowPreview::class, ExperimentalCoroutinesApi::class)
class EventRepositoryImpl(
    externalScope: CoroutineScope,
    private val eventSource: EventSource,
    private val eventStorage: EventStorage,
    private val bootTime: Instant = Clock.System.now()
) : ObserveAllEvents.Gateway, ObserveCalendarCurrentDay.Gateway, ObserveCalendarTimeZone.Gateway,
    ObserveRefreshingRanges.Gateway, RefreshCache.Gateway, SetRelevantDatePeriod.Gateway,
    SynchronizeFavouriteEvents.EventsProvider {

    private val expirationDate: MutableStateFlow<Instant> = MutableStateFlow(bootTime.minus(DEFAULT_CACHE_DURATION))

    private val bootDate: LocalDate
        get() = bootTime.toLocalDate(timeZone = TimeZone.UTC)

    private val relevantDates = MutableStateFlow(
        bootDate.minus(BEFORE_TODAY_PERIOD)..bootDate.plus(AFTER_TODAY_PERIOD)
    )

    init {
        externalScope.launch {
            combine(
                expirationDate.debounce(EXPIRATION_DATE_DEBOUNCE_DURATION),
                relevantDates.sample(DATE_CHANGE_SAMPLING_DURATION)
            ) { expiration, dates -> expiration to dates }
                .collect { (expiration, dates) ->
                    refreshStaleEvents(dates, expiration)
                }
        }
    }

    override suspend fun setRelevantDatePeriod(period: LocalDateRange) {
        relevantDates.value = period
    }

    override suspend fun refreshCache() {
        this.expirationDate.update { Clock.System.now() }
    }

    override val eventsTimezone: Flow<TimeZone>
        get() = eventStorage.timezone

    override val events: Flow<List<CalendarEvent>>
        get() = eventStorage.events

    override val refreshingRanges = MutableStateFlow(emptySet<LocalDateRange>())

    private suspend fun refreshStaleEvents(relevantDates: LocalDateRange, expirationDate: Instant) {
        eventStorage.getDateRangeTimestampsOf(relevantDates)
            .fillGaps(relevantDates)
            .filter { rangeTimestamp -> rangeTimestamp.timestamp < expirationDate }
            .map { it.dateRange.intersectWith(relevantDates) }
            .mergeCloseEnoughRanges()
            .filter { it != LocalDateRange.EMPTY }
            .asFlow()
            .flatMapMerge(concurrency = 3) { range ->
                refreshingRanges.update { it.plusElement(range) }
                flow {
                    try {
                        emit(refreshRange(range))
                    } finally {
                        refreshingRanges.update { it.minusElement(range) }
                    }
                }
            }.collect()
    }

    private suspend fun refreshRange(range: LocalDateRange) {
        val events = eventSource.getEventsInRange(range)
        eventStorage.saveEventsAndRanges(events, range)
    }

    private fun Collection<DateRangeTimestamp>.fillGaps(relevantDates: LocalDateRange): Collection<DateRangeTimestamp> {
        val sortedRanges = sortedBy { it.dateRange.start }

        val filledList = mutableSetOf<DateRangeTimestamp>()

        var nextExpectedStart = relevantDates.start

        for (item in sortedRanges) {
            val range = item.dateRange

            if (range.start > nextExpectedStart) {
                val gapEnd = minOf(range.start.previousDay(), relevantDates.endInclusive)

                if (nextExpectedStart <= gapEnd) {
                    filledList.add(
                        DateRangeTimestamp(nextExpectedStart..gapEnd, Instant.DISTANT_PAST)
                    )
                }
            }

            filledList.add(item)

            val nextDay = range.endInclusive.nextDay()
            if (nextDay > nextExpectedStart) {
                nextExpectedStart = nextDay
            }
        }

        if (nextExpectedStart <= relevantDates.endInclusive) {
            filledList.add(
                DateRangeTimestamp(nextExpectedStart..relevantDates.endInclusive, Instant.DISTANT_PAST)
            )
        }

        return filledList
    }

    private fun List<LocalDateRange>.mergeCloseEnoughRanges(): List<LocalDateRange> = sortedBy { it.start }
        .fold(mutableListOf()) { acc, dateRange ->
            acc.apply {
                if (lastOrNull()?.endInclusive?.plus(PROXIMITY_THRESHOLD)?.let { it >= dateRange.start } ?: false) {
                    val last = removeLast()
                    add(last.start..dateRange.endInclusive)
                } else add(dateRange)
            }
        }

    private fun LocalDateRange.intersectWith(other: LocalDateRange): LocalDateRange {
        val start = maxOf(this.start, other.start)
        val endInclusive = minOf(this.endInclusive, other.endInclusive)
        return if (start <= endInclusive) start..endInclusive else LocalDateRange.EMPTY
    }

    override suspend fun refreshEventsForDiff(eventIds: Collection<String>): Collection<EventDiff> {
        val freshCalendarEvents = eventSource.getEventsById(eventIds)
        return eventStorage.saveEventsForDiff(freshCalendarEvents)
    }

    interface EventSource {
        suspend fun getTimeZone(): TimeZone

        suspend fun getEventsInRange(dateRange: LocalDateRange): List<CalendarEvent>

        suspend fun getEventsById(eventsIds: Collection<String>): List<CalendarEvent>
    }

    interface EventStorage {
        val events: Flow<List<CalendarEvent>>

        val timezone: Flow<TimeZone>

        suspend fun setTimeZone(timezone: TimeZone)

        suspend fun getDateRangeTimestampsOf(dateRange: LocalDateRange): Collection<DateRangeTimestamp>

        suspend fun saveEventsAndRanges(
            events: List<CalendarEvent>,
            dateRange: LocalDateRange,
            timestamp: Instant = Clock.System.now()
        )

        fun saveEventsForDiff(calendarEvents: List<CalendarEvent>): Collection<EventDiff>
    }

    data class DateRangeTimestamp(
        val dateRange: LocalDateRange,
        val timestamp: Instant,
    )

    private companion object {

        val BEFORE_TODAY_PERIOD = DatePeriod(days = 30)
        val AFTER_TODAY_PERIOD = DatePeriod(days = 90)

        val DATE_CHANGE_SAMPLING_DURATION = 1.seconds
        val EXPIRATION_DATE_DEBOUNCE_DURATION = 1.seconds

        val PROXIMITY_THRESHOLD = DatePeriod(days = 7)

        val DEFAULT_CACHE_DURATION = 5.days
    }
}
