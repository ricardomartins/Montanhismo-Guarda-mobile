package pt.rikmartins.clubemg.mobile.data.event

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import pt.rikmartins.clubemg.mobile.cache.AppDatabase
import pt.rikmartins.clubemg.mobile.cache.CalendarEvent as CacheCalendarEvent
import pt.rikmartins.clubemg.mobile.cache.EventImage as CacheEventImage
import pt.rikmartins.clubemg.mobile.cache.SelectAllWithImages
import pt.rikmartins.clubemg.mobile.domain.usecase.events.CalendarEvent
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventAttendanceMode
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventDiff
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventImage
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventStatusType
import pt.rikmartins.clubemg.mobile.nextDay
import pt.rikmartins.clubemg.mobile.previousDay
import kotlin.collections.map
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class DataBaseEventStorage(
    database: AppDatabase,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val logger: Logger = Logger.withTag(DataBaseEventStorage::class.simpleName!!)
) : EventRepository.EventStorage {

    private val queries = database.appDatabaseQueries

    override val events: Flow<List<CalendarEvent>> = queries.selectAllWithImages().asFlow()
        .mapToList(defaultDispatcher).map { rows -> rows.toCalendarEvent() }

    override suspend fun getTimeZone(): TimeZone = withContext(defaultDispatcher) {
        TimeZone.of(queries.getTextValue(TIMEZONE_KEY).executeAsOneOrNull()?.value_ ?: DEFAULT_API_TIMEZONE)
    }

    override suspend fun setTimeZone(timezone: TimeZone): Unit = withContext(defaultDispatcher) {
        TODO("Not yet implemented")
    }

    override suspend fun getDateRangeTimestampsOf(
        dateRange: LocalDateRange,
    ): List<EventRepository.DateRangeTimestamp> = withContext(defaultDispatcher) {
        queries.getDateRangeTimestampsThatIntersectRange(
            rangeStart = dateRange.start,
            rangeEnd = dateRange.endInclusive
        ).executeAsList()
            .also { logger.d { "Found ${it.size} date range timestamps between ${dateRange.start} and ${dateRange.endInclusive}" } }
            .map { dateRangeTimestamp ->
                EventRepository.DateRangeTimestamp(
                    dateRange = dateRangeTimestamp.dateRangeStart..dateRangeTimestamp.dateRangeEnd,
                    timestamp = dateRangeTimestamp.timestamp,
                )
            }
    }

    override suspend fun saveEventsAndRanges(
        events: List<CalendarEvent>,
        dateRange: LocalDateRange,
        timestamp: Instant,
    ) = withContext(defaultDispatcher) {
        val timeZone = TimeZone.of(
            queries.getTextValue(TIMEZONE_KEY).executeAsOneOrNull()?.value_
                ?: DEFAULT_API_TIMEZONE
        )

        queries.transaction {
            saveEvents(events, dateRange, timeZone)
            saveDateRangeWithTimestamp(dateRange, timestamp)
        }
    }

    private fun saveEvents(events: List<CalendarEvent>, dateRange: LocalDateRange, timeZone: TimeZone) {
        val existingEvents = queries.selectEventsInRange(
            rangeStart = dateRange.start.atStartOfDayIn(timeZone),
            rangeEnd = dateRange.endInclusive.atEndOfDayIn(timeZone),
        ).executeAsList()

        val eventsToDelete = existingEvents.toMutableList()
        val eventsToUpsert = events.toMutableList()

        existingEvents.forEach { existingEvent ->
            val correspondingNewEvent = events.firstOrNull { it.id == existingEvent.id }

            if (correspondingNewEvent != null) {
                eventsToDelete.remove(existingEvent)

                if (correspondingNewEvent.modifiedDate <= existingEvent.modifiedDate) {
                    eventsToUpsert.remove(correspondingNewEvent)
                }
            }
        }

        queries.deleteEvents(eventsToDelete.map { calendarEvent -> calendarEvent.id })

        queries.deleteImagesOfEvents(eventsToUpsert.map { calendarEvent -> calendarEvent.id })
        eventsToUpsert.forEach { calendarEvent -> replaceSingleEvent(calendarEvent) }
    }

    private fun replaceSingleEvent(event: CalendarEvent) {
        queries.replaceEvent(event.asCacheCalendarEvent())
        event.images.forEach { queries.replaceEventImage(it.asCacheEventImage(event.id)) }
    }

    private fun saveDateRangeWithTimestamp(dateRange: LocalDateRange, timestamp: Instant) {
        queries.getDateRangeTimestampsThatIntersectRange(
            rangeStart = dateRange.start,
            rangeEnd = dateRange.endInclusive,
        )
            .executeAsList().forEach { existing ->
                val startsBeforeNew = existing.dateRangeStart < dateRange.start
                val endsAfterNew = existing.dateRangeEnd > dateRange.endInclusive

                when {
                    startsBeforeNew && endsAfterNew -> {
                        // Existing range contains the new one
                        queries.updateDateRangeTimestampWithId(
                            id = existing.id,
                            rangeStart = existing.dateRangeStart,
                            rangeEnd = dateRange.start.previousDay(),
                        )
                        queries.insertDateRangeTimestamp(
                            rangeStart = dateRange.endInclusive.nextDay(),
                            rangeEnd = existing.dateRangeEnd,
                            timestamp = existing.timestamp,
                        )
                    }

                    startsBeforeNew ->
                        // Existing range starts before the new one
                        queries.updateDateRangeTimestampWithId(
                            id = existing.id,
                            rangeStart = existing.dateRangeStart,
                            rangeEnd = dateRange.start.previousDay(),
                        )

                    endsAfterNew ->
                        // Existing range ends after the new one
                        queries.updateDateRangeTimestampWithId(
                            id = existing.id,
                            rangeStart = dateRange.endInclusive.nextDay(),
                            rangeEnd = existing.dateRangeEnd,
                        )

                    else -> queries.deleteDateRangeTimestampWithId(existing.id)
                }
            }
        queries.insertDateRangeTimestamp(
            rangeStart = dateRange.start,
            rangeEnd = dateRange.endInclusive,
            timestamp = timestamp
        )
    }

    override suspend fun saveEventsForDiff(calendarEvents: List<CalendarEvent>): Collection<EventDiff> =
        withContext(defaultDispatcher) {
            buildSet {
                queries.transaction {
                    val existingEvents = queries.selectEventsById(calendarEvents.map { it.id }).executeAsList()

                    val eventsToUpsert = calendarEvents.toMutableList()

                    existingEvents.forEach { existingEvent ->
                        val correspondingNewEvent = calendarEvents.firstOrNull { it.id == existingEvent.id }

                        if (correspondingNewEvent != null) {
                            add(existingEvent diffWith correspondingNewEvent)

                            if (correspondingNewEvent.modifiedDate <= existingEvent.modifiedDate)
                                eventsToUpsert.remove(correspondingNewEvent)
                        }
                    }

                    queries.deleteImagesOfEvents(eventsToUpsert.map { calendarEvent -> calendarEvent.id })
                    eventsToUpsert.forEach { calendarEvent -> replaceSingleEvent(calendarEvent) }
                }
            }
        }

    private infix fun CacheCalendarEvent.diffWith(other: CalendarEvent) = EventDiff(
        StorageCalendarEvent(
            id = id,
            creationDate = creationDate,
            modifiedDate = modifiedDate,
            title = title,
            url = url,
            startDate = startDate,
            endDate = endDate,
            enrollmentUrl = enrollmentUrl,
            images = emptyList(),
            eventStatusType = eventStatusType,
            eventAttendanceMode = eventAttendanceMode,
        ),
        newEvent = other
    )

    private data class StorageEventImage(
        val calendarEventId: String,
        override val id: String?,
        override val url: String,
        override val width: Int,
        override val height: Int,
        override val fileSize: Int,
    ) : EventImage

    private data class StorageCalendarEvent(
        override val id: String,
        override val creationDate: Instant,
        override val modifiedDate: Instant,
        override val title: String,
        override val url: String,
        override val startDate: Instant,
        override val endDate: Instant,
        override val enrollmentUrl: String,
        override val images: List<StorageEventImage>,
        override val eventStatusType: EventStatusType?,
        override val eventAttendanceMode: EventAttendanceMode?,
    ) : CalendarEvent

    private fun CalendarEvent.asCacheCalendarEvent() = CacheCalendarEvent(
        id = id,
        creationDate = creationDate,
        modifiedDate = modifiedDate,
        title = title,
        url = url,
        startDate = startDate,
        endDate = endDate,
        enrollmentUrl = enrollmentUrl,
        eventStatusType = eventStatusType,
        eventAttendanceMode = eventAttendanceMode,
    )

    private fun EventImage.asCacheEventImage(eventId: String) = CacheEventImage(
        calendarEventId = eventId,
        id = id,
        url = url,
        width = width.toLong(),
        height = height.toLong(),
        fileSize = fileSize.toLong(),
    )

    private fun List<SelectAllWithImages>.toCalendarEvent(): List<StorageCalendarEvent> = this.groupBy { it.id }
        .map { (_, rowList) ->
            val calendarEvent = rowList.first()

            StorageCalendarEvent(
                id = calendarEvent.id,
                creationDate = calendarEvent.creationDate,
                modifiedDate = calendarEvent.modifiedDate,
                title = calendarEvent.title,
                url = calendarEvent.url,
                startDate = calendarEvent.startDate,
                endDate = calendarEvent.endDate,
                enrollmentUrl = calendarEvent.enrollmentUrl,
                images = rowList.mapNotNull { eventImage ->
                    if (eventImage.url_ != null &&
                        eventImage.width != null &&
                        eventImage.height != null &&
                        eventImage.fileSize != null
                    ) {
                        StorageEventImage(
                            calendarEventId = calendarEvent.id,
                            id = eventImage.id_,
                            url = eventImage.url_,
                            width = eventImage.width.toInt(),
                            height = eventImage.height.toInt(),
                            fileSize = eventImage.fileSize.toInt(),
                        )
                    } else null
                },
                eventStatusType = calendarEvent.eventStatusType,
                eventAttendanceMode = calendarEvent.eventAttendanceMode,
            )
        }

    // Remember: database precision is in milliseconds
    private fun LocalDate.atEndOfDayIn(timeZone: TimeZone): Instant =
        plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone) - 1.milliseconds

    private companion object {
        const val TIMEZONE_KEY = "timezone"

        const val DEFAULT_API_TIMEZONE = "Europe/Lisbon"
    }
}