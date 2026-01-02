package pt.rikmartins.clubemg.mobile.data

import kotlinx.coroutines.CoroutineScope
import pt.rikmartins.clubemg.mobile.domain.gateway.CalendarEvent
import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import pt.rikmartins.clubemg.mobile.domain.usecase.events.toLocalDate
import pt.rikmartins.clubemg.mobile.nextDay
import pt.rikmartins.clubemg.mobile.previousDay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, FlowPreview::class)
class EventRepositoryImpl(
    private val externalScope: CoroutineScope,
    private val eventSource: EventSource,
    private val eventStorage: EventStorage,
    private val bootTime: Instant = Clock.System.now()
) : EventRepository {

    private val expirationDate: MutableStateFlow<Instant> = MutableStateFlow(bootTime.minus(DEFAULT_CACHE_DURATION))

    private val bootDate: LocalDate
        get() = bootTime.toLocalDate(timeZone = TimeZone.UTC)

    private val relevantDates = MutableStateFlow(
        bootDate.minus(BEFORE_TODAY_PERIOD)..bootDate.plus(AFTER_TODAY_PERIOD)
    )

    init {
        externalScope.launch {
            combine(
                expirationDate,
                relevantDates.debounce(DATE_CHANGE_DEBOUNCE_DURATION)
            ) { expiration, dates -> expiration to dates }
                .collectLatest { (expiration, dates) ->
                    refreshStaleEvents(dates, expiration)
                }
        }
    }

    override suspend fun requestDate(date: LocalDate) {
        when {
            date < relevantDates.value.start -> relevantDates.value = date..relevantDates.value.endInclusive
            date > relevantDates.value.endInclusive -> relevantDates.value = relevantDates.value.start..date
        }
    }

    override suspend fun setCacheExpirationDate(expirationDate: Instant) {
        this.expirationDate.value = expirationDate
    }

    override val eventsTimezone: Flow<TimeZone>
        get() = eventStorage.timezone

    override val events: Flow<List<CalendarEvent>>
        get() = eventStorage.events

    private suspend fun refreshStaleEvents(relevantDates: LocalDateRange, expirationDate: Instant) = coroutineScope {
        eventStorage.getDateRangeTimestampsOf(relevantDates)
            .fillGaps(relevantDates)
            .filter { rangeTimestamp -> rangeTimestamp.timestamp < expirationDate }
            .map { it.dateRange.intersectWith(relevantDates) }
            .mergeCloseEnoughRanges()
            .map {
                async {
                    val events = eventSource.getEvents(it)
                    eventStorage.saveEventsAndRanges(events, it)
                }
            }.awaitAll()
    }

    private fun List<DateRangeTimestamp>.fillGaps(relevantDates: LocalDateRange): List<DateRangeTimestamp> {
       val sortedRanges = sortedBy { it.dateRange.start }

        val filledList = mutableListOf<DateRangeTimestamp>()

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

        // 3. Check for a trailing gap after the last existing range
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

    interface EventSource {
        suspend fun getTimeZone(): TimeZone

        suspend fun getEvents(dateRange: LocalDateRange): List<CalendarEvent>
    }

    interface EventStorage {
        val events: Flow<List<CalendarEvent>>

        val timezone: Flow<TimeZone>

        suspend fun setTimeZone(timezone: TimeZone)

        suspend fun getDateRangeTimestampsOf(dateRange: LocalDateRange): List<DateRangeTimestamp>

        suspend fun saveEventsAndRanges(
            events: List<CalendarEvent>,
            dateRange: LocalDateRange,
            timestamp: Instant = Clock.System.now()
        )
    }

    data class DateRangeTimestamp(
        val dateRange: LocalDateRange,
        val timestamp: Instant,
    )

    private companion object {

        val BEFORE_TODAY_PERIOD = DatePeriod(days = 30)
        val AFTER_TODAY_PERIOD = DatePeriod(days = 90)

        val DATE_CHANGE_DEBOUNCE_DURATION = 500.milliseconds

        val PROXIMITY_THRESHOLD = DatePeriod(days = 7)

        val DEFAULT_CACHE_DURATION = 5.days
    }
}
