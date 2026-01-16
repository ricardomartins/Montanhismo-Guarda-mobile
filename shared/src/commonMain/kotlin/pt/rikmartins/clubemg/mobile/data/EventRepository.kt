package pt.rikmartins.clubemg.mobile.data

import co.touchlab.kermit.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pt.rikmartins.clubemg.mobile.domain.usecase.events.CalendarEvent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventDiff
import pt.rikmartins.clubemg.mobile.domain.usecase.events.GetCalendarTimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarCurrentDay
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveRefreshingRanges
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RefreshPeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetRelevantDatePeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents
import pt.rikmartins.clubemg.mobile.nextDay
import pt.rikmartins.clubemg.mobile.previousDay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, FlowPreview::class, ExperimentalCoroutinesApi::class)
class EventRepository(
    private val eventSource: EventSource,
    private val eventStorage: EventStorage,
    private val logger: Logger = Logger.withTag(SynchronizeFavouriteEvents::class.simpleName!!)
) : ObserveAllEvents.EventsProvider, ObserveCalendarCurrentDay.Gateway, GetCalendarTimeZone.Gateway,
    ObserveRefreshingRanges.Gateway, RefreshPeriod.Gateway, SetRelevantDatePeriod.Gateway,
    SynchronizeFavouriteEvents.EventsProvider {

    override suspend fun setRelevantDatePeriod(period: LocalDateRange) = refreshStaleEvents(period)

    override suspend fun refreshPeriod(period: LocalDateRange) = refreshRange(period)

    override suspend fun getTimeZone(): TimeZone = eventStorage.getTimeZone()

    override val events: Flow<List<CalendarEvent>>
        get() = eventStorage.events

    override val refreshingRanges = MutableStateFlow(emptySet<LocalDateRange>())

    private val refreshMutex = Mutex()

    private suspend fun refreshStaleEvents(period: LocalDateRange) {
        logger.v { "Refreshing stale events for $period entered the queue" }
        refreshMutex.withLock {
            logger.d { "Proceeding with refreshing stale events for $period" }
            eventStorage.getDateRangeTimestampsOf(period)
                .fillGaps(period)
                .filterAccordingToRules(eventStorage.getTimeZone())
                .map { it.dateRange.intersectWith(period) }
                .mergeCloseEnoughRanges()
                .filter { it != LocalDateRange.EMPTY }
                .asFlow()
                .flatMapMerge(concurrency = 3) { range -> flowOf(refreshRange(range, false)) }
                    .collect()
        }
    }

    private suspend fun refreshRange(range: LocalDateRange, withLock: Boolean = true) {
        suspend fun execute() {
            refreshingRanges.update { it.plusElement(range) }
            try {
                logger.d { "Refreshing range $range" }
                val events = eventSource.getEventsInRange(range)
                logger.v { "Fetched ${events.size} events for range $range. Saving..." }
                eventStorage.saveEventsAndRanges(events, range)
                logger.v { "Successfully finished refreshing range $range" }
            } catch (e: Exception) {
                logger.e(e) { "Failed to refresh range $range" }
                throw e
            } finally {
                refreshingRanges.update { it.minusElement(range) }
            }
        }

        if (withLock) refreshMutex.withLock { execute() } else execute()
    }

    private fun Collection<DateRangeTimestamp>.filterAccordingToRules(
        timeZone: TimeZone,
    ): Collection<DateRangeTimestamp> {
        val now = Clock.System.now()
        val aMonthFromNow = now.plus(30.days)

        return filter { dateRangeTimestamp ->
            dateRangeTimestamp.isStale(now, aMonthFromNow, timeZone)
                .also { if (it) logger.v { "$dateRangeTimestamp was found to be stale" } }
        }
    }

    private fun DateRangeTimestamp.isStale(now: Instant, aMonthFromNow: Instant, timeZone: TimeZone): Boolean {
        val dateRangeStart = this.dateRange.start.atStartOfDayIn(timeZone)
        val dateRangeEnd = this.dateRange.endInclusive.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone)

        // Calculate how long this item is allowed to be cached.
        val cacheExpiryDuration = when {
            dateRangeStart >= aMonthFromNow -> DEFAULT_CACHE_DURATION + (dateRangeStart - aMonthFromNow)
            dateRangeEnd < now -> DEFAULT_CACHE_DURATION + (now - dateRangeEnd)
            else -> DEFAULT_CACHE_DURATION
        }

        val itemAge = now - this.timestamp
        return itemAge > cacheExpiryDuration
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

        suspend fun getTimeZone(): TimeZone

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

        val PROXIMITY_THRESHOLD = DatePeriod(days = 7)

        val DEFAULT_CACHE_DURATION = 3.days
    }
}
