package pt.rikmartins.clubemg.mobile.data

import pt.rikmartins.clubemg.mobile.domain.entity.CalendarEvent
import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningReduce
import kotlinx.coroutines.flow.scan
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class, FlowPreview::class)
class EventRepositoryImpl(
    private val eventSource: EventSource,
    private val eventStorage: EventStorage,
) : EventRepository {
    private val scope = CoroutineScope(SupervisorJob()) // FIXME

    override val localAccess by lazy { eventSource.isAccessing }

    override val remoteAccess by lazy { eventStorage.isAccessing }

    private val _startDate = MutableStateFlow(getEarliestDateToStore())

    private val _endDate = MutableStateFlow(getLatestDateToStore())

    override suspend fun requestDate(date: LocalDate) {
        val timezone = eventsTimezone.first()
        val startDate = date.atStartOfDayIn(timezone)
        val endDate = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timezone)

        when {
            startDate < _startDate.value -> _startDate.value = startDate
            endDate > _endDate.value -> _endDate.value = endDate
        }
    }

    private val storedEvents = eventStorage.getAllEvents().map { events ->
        var startDate: Instant? = null
        var endDate: Instant? = null
        events.forEach {
            if (startDate == null || it.startDate < startDate) {
                startDate = it.startDate
            }
            if (endDate == null || it.endDate > endDate) {
                endDate = it.endDate
            }
        }

        EventsAndDates(
            events = events,
            startDate = startDate,
            endDate = endDate,
        )
    }

    private val freshEvents = combine(
        _startDate.debounce(DATE_CHANGE_DEBOUNCE_DURATION).distinctUntilChanged(),
        _endDate.debounce(DATE_CHANGE_DEBOUNCE_DURATION).distinctUntilChanged(),
    ) { startDate, endDate -> startDate to endDate }
        .scan(initial = DateChanges(startDate = Change(), endDate = Change())) { acc, (startDate, endDate) ->
            DateChanges(
                startDate = Change(previous = acc.startDate.current, current = startDate),
                endDate = Change(previous = acc.endDate.current, current = endDate)
            )
        }
        .map { (startDate, endDate) ->
            EventsAndDates(
                events = startDate.current?.let { currentStartDate ->
                    endDate.current?.let { currentEndDate ->
                        buildList {
                            if (startDate.previous != null && endDate.previous != null) {
                                // We have previous dates, so fetch changes
                                if (endDate.hasChanged) {
                                    // Fetch events from the previous end date up to the new current end date
                                    add(endDate.previous..<currentEndDate)
                                }
                                if (startDate.hasChanged) {
                                    // Fetch events from the new current start date up to the previous start date
                                    add(currentStartDate..<startDate.previous)
                                }

                            } else {
                                // No complete previous range, so do an initial full fetch
                                add(currentStartDate..<currentEndDate)
                            }
                        }
                            .flatMap { eventSource.getEvents(it.start, it.endExclusive) }
                    }
                } ?: emptyList<CalendarEvent>(),
                startDate = startDate.current,
                endDate = endDate.current,
            )
        }
        .runningReduce { acc, value ->
            EventsAndDates(
                events = (value.events + acc.events).distinctBy { it.id },
                startDate = value.startDate,
                endDate = value.endDate
            )
        }

    override val events: Flow<List<CalendarEvent>> by lazy {
        combine(storedEvents, freshEvents) { stored, fresh ->
            val mergeStructure = mutableListOf<MergeStructure>()
            val toStore = mutableListOf<CalendarEvent>()
            val toDelete = mutableListOf<CalendarEvent>()

            var earlierFreshStartDate: Instant? = null
            var latestFreshStartDate: Instant? = null

            fresh.events.forEach { freshEvent ->
                mergeStructure.add(MergeStructure(freshEvent = freshEvent))
                if (earlierFreshStartDate == null || freshEvent.startDate < earlierFreshStartDate) {
                    earlierFreshStartDate = freshEvent.startDate
                }
                if (latestFreshStartDate == null || freshEvent.startDate > latestFreshStartDate) {
                    latestFreshStartDate = freshEvent.startDate
                }
            }
            stored.events.forEach { storedEvent ->
                mergeStructure.firstOrNull { (it.freshEvent ?: it.storedEvent)!!.id == storedEvent.id }?.let {
                    it.storedEvent = storedEvent
                } ?: mergeStructure.add(MergeStructure(storedEvent = storedEvent))
            }

            val merged = mutableListOf<CalendarEvent>()
            val earliestDateToStore = getEarliestDateToStore()
            val latestDateToStore = getLatestDateToStore()

            mergeStructure.forEach {
                val storedEvent = it.storedEvent
                val freshEvent = it.freshEvent

                when {
                    storedEvent != null && freshEvent != null -> {
                        when {
                            storedEvent.startDate > latestDateToStore || storedEvent.endDate < earliestDateToStore ->
                                toDelete.add(storedEvent)

                            freshEvent.modifiedDate > storedEvent.modifiedDate -> toStore.add(freshEvent)
                        }
                        merged.add(freshEvent)
                    }

                    freshEvent != null -> {
                        if (freshEvent.startDate <= latestDateToStore && freshEvent.endDate >= earliestDateToStore) {
                            toStore.add(freshEvent)
                        }
                        merged.add(freshEvent)
                    }

                    storedEvent != null -> {
                        if (earlierFreshStartDate != null && storedEvent.startDate >= earlierFreshStartDate && storedEvent.startDate <= latestFreshStartDate!!) {
                            toDelete.add(storedEvent)
                        } else {
                            if (storedEvent.startDate > latestDateToStore || storedEvent.endDate < earliestDateToStore) {
                                toDelete.add(storedEvent)
                            }
                            merged.add(storedEvent)
                        }
                    }
                }
            }
            eventStorage.deleteEvents(toDelete)
            eventStorage.saveEvents(toStore)

            merged.sortedBy { it.startDate }
        }
    }
    override val eventsTimezone: Flow<TimeZone>
        get() = eventSource.timezone

    override val providedStartDate by lazy {
        combine(storedEvents, freshEvents) { (_, stored, _), (_, fresh, _) -> fresh ?: stored }
    }

    override val providedEndDate by lazy {
        combine(storedEvents, freshEvents) { (_, _, stored), (_, _, fresh) -> fresh ?: stored }
    }

    private data class EventsAndDates(
        val events: List<CalendarEvent>,
        val startDate: Instant?,
        val endDate: Instant?,
    )

    private data class MergeStructure(
        var storedEvent: CalendarEvent? = null,
        var freshEvent: CalendarEvent? = null,
    )

    private fun getEarliestDateToStore(): Instant =
        Clock.System.now().minus(BEFORE_TODAY_PERIOD)

    private fun getLatestDateToStore(): Instant =
        Clock.System.now().plus(AFTER_TODAY_PERIOD)

    interface EventSource {
        suspend fun getEvents(startDate: Instant, endDate: Instant): List<CalendarEvent>
        val isAccessing: Flow<Boolean>
        val timezone: Flow<TimeZone>
    }

    interface EventStorage {
        fun getAllEvents(): Flow<List<CalendarEvent>> // Should not emit empty, unless there are no events
        suspend fun saveEvents(events: List<CalendarEvent>)
        suspend fun deleteEvents(events: List<CalendarEvent>)
        val isAccessing: Flow<Boolean>
    }

    private data class Change<T>(val previous: T? = null, val current: T? = null) {
        val hasChanged: Boolean = previous != current
    }

    private data class DateChanges(val startDate: Change<Instant>, val endDate: Change<Instant>)

    private companion object {
        val BEFORE_TODAY_PERIOD = 30.days
        val AFTER_TODAY_PERIOD = 90.days
        val DATE_CHANGE_DEBOUNCE_DURATION = 500.milliseconds
    }
}
