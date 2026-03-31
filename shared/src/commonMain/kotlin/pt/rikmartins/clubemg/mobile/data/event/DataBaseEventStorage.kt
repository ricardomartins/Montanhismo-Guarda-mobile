package pt.rikmartins.clubemg.mobile.data.event

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
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
import pt.rikmartins.clubemg.mobile.data.cache.AppDatabase
import pt.rikmartins.clubemg.mobile.data.cache.RoomCalendarEventEventTaxonomy
import pt.rikmartins.clubemg.mobile.data.cache.RoomEventTaxonomy
import pt.rikmartins.clubemg.mobile.data.cache.RoomEventImage
import pt.rikmartins.clubemg.mobile.data.cache.RoomCalendarEvent
import pt.rikmartins.clubemg.mobile.data.cache.RoomDateRangeTimestamp
import pt.rikmartins.clubemg.mobile.data.cache.RoomEventWithDetails
import pt.rikmartins.clubemg.mobile.domain.usecase.events.CalendarEvent
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventAttendanceMode
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventDiff
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventImage
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventStatusType
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventTaxonomy
import pt.rikmartins.clubemg.mobile.domain.usecase.events.TaxonomyType
import pt.rikmartins.clubemg.mobile.nextDay
import pt.rikmartins.clubemg.mobile.previousDay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class DataBaseEventStorage(
    private val database: AppDatabase,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val logger: Logger = Logger.withTag(DataBaseEventStorage::class.simpleName!!)
) : EventRepository.EventStorage {

    private val eventsDao = database.eventsDao()
    private val rangesDao = database.rangesDao()
    private val singleValuesDao = database.singleValuesDao()

    override val events: Flow<List<CalendarEvent>> = eventsDao
        .selectAllWithDetails().map { rows -> rows.toCalendarEvent() }

    override suspend fun getTimeZone(): TimeZone = withContext(defaultDispatcher) {
        TimeZone.of(singleValuesDao.getTextValue(TIMEZONE_KEY)?.value ?: DEFAULT_API_TIMEZONE)
    }

    override suspend fun setTimeZone(timezone: TimeZone): Unit = withContext(defaultDispatcher) {
        TODO("Not yet implemented")
    }

    override suspend fun getDateRangeTimestampsOf(
        dateRange: LocalDateRange,
    ): List<EventRepository.DateRangeTimestamp> = withContext(defaultDispatcher) {
        rangesDao.getDateRangeTimestampsThatIntersectRange(
            rangeStart = dateRange.start.toEpochDays(),
            rangeEnd = dateRange.endInclusive.toEpochDays()
        )
            .also { logger.d { "Found ${it.size} date range timestamps between ${dateRange.start} and ${dateRange.endInclusive}" } }
            .map { dateRangeTimestamp ->
                EventRepository.DateRangeTimestamp(
                    dateRange = LocalDate.fromEpochDays(dateRangeTimestamp.dateRangeStart.toInt())..LocalDate.fromEpochDays(
                        dateRangeTimestamp.dateRangeEnd.toInt()
                    ),
                    timestamp = Instant.fromEpochMilliseconds(dateRangeTimestamp.timestamp),
                )
            }
    }

    override suspend fun saveEventsAndRanges(
        events: List<CalendarEvent>,
        dateRange: LocalDateRange,
        timestamp: Instant,
    ) = withContext(defaultDispatcher) {
        val timeZone = TimeZone.of(
            singleValuesDao.getTextValue(TIMEZONE_KEY)?.value ?: DEFAULT_API_TIMEZONE
        )

        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                saveEvents(events, dateRange, timeZone)
                saveDateRangeWithTimestamp(dateRange, timestamp)
            }
        }
    }

    private suspend fun saveEvents(
        events: List<CalendarEvent>,
        dateRange: LocalDateRange,
        timeZone: TimeZone,
    ) {
        val existingEvents = eventsDao.selectEventsInRangeWithDetails(
            rangeStart = dateRange.start.atStartOfDayIn(timeZone).toEpochMilliseconds(),
            rangeEnd = dateRange.endInclusive.atEndOfDayIn(timeZone).toEpochMilliseconds()
        ).toCalendarEvent()

        val eventsToDelete = existingEvents.toMutableList()
        val eventsToUpsert = events.mapTo(mutableListOf<Pair<CalendarEvent, CalendarEvent?>>()) { it to null }

        existingEvents.forEach { existingEvent ->
            val correspondingNewEvent = events.firstOrNull { it.id == existingEvent.id }

            if (correspondingNewEvent != null) {
                eventsToDelete.remove(existingEvent)
                eventsToUpsert.removeAll { it.first.id == correspondingNewEvent.id }
                eventsToUpsert.add(correspondingNewEvent to existingEvent)
            }
        }
        eventsDao.deleteEvents(eventsToDelete.map { calendarEvent -> calendarEvent.id })
        eventsToUpsert.forEach { (calendarEvent, existingEvent) -> replaceSingleEvent(calendarEvent, existingEvent) }
    }

    private suspend fun replaceSingleEvent(newEvent: CalendarEvent, existingEvent: CalendarEvent?) {
        val doUpdateEvent: Boolean
        val doUpdateTaxonomies: Boolean // Necessary to populate DB after migration
        val doUpdateStatusType: Boolean // Necessary because lists of events don't have this data
        val doUpdateAttendanceMode: Boolean // Necessary because lists of events don't have this data
        if (existingEvent == null || newEvent.modifiedDate > existingEvent.modifiedDate) {
            doUpdateEvent = true
            doUpdateTaxonomies = true
            doUpdateStatusType = false
            doUpdateAttendanceMode = false
        } else {
            doUpdateEvent = false
            doUpdateTaxonomies = newEvent.taxonomies.size != existingEvent.taxonomies.size
            doUpdateStatusType = newEvent.eventStatusType != null && existingEvent.eventStatusType == null
            doUpdateAttendanceMode = newEvent.eventAttendanceMode != null && existingEvent.eventAttendanceMode == null
        }

        if (doUpdateEvent) {
            eventsDao.replaceEvent(
                RoomCalendarEvent(
                    id = newEvent.id,
                    creationDate = newEvent.creationDate.toEpochMilliseconds(),
                    modifiedDate = newEvent.modifiedDate.toEpochMilliseconds(),
                    title = newEvent.title,
                    url = newEvent.url,
                    startDate = newEvent.startDate.toEpochMilliseconds(),
                    endDate = newEvent.endDate.toEpochMilliseconds(),
                    enrollmentUrl = newEvent.enrollmentUrl,
                    eventStatusType = newEvent.eventStatusType,
                    eventAttendanceMode = newEvent.eventAttendanceMode,
                )
            )

            if (existingEvent != null) eventsDao.deleteImagesOfEvent(existingEvent.id)
            newEvent.images.forEach { eventsDao.replaceEventImage(it.asCacheEventImage(newEvent.id)) }
        }

        if (doUpdateTaxonomies) {
            if (existingEvent != null) eventsDao.removeAllTaxonomiesFromCalendarEvent(existingEvent.id)
            newEvent.taxonomies.forEach {
                eventsDao.addEventTaxonomy(
                    RoomEventTaxonomy(
                        slug = it.slug,
                        taxonomyType = it.taxonomyType,
                        name = it.name,
                    )
                )
                eventsDao.addTaxonomyToCalendarEvent(
                    RoomCalendarEventEventTaxonomy(
                        calendarEventId = newEvent.id,
                        eventTaxonomySlug = it.slug,
                        eventTaxonomyType = it.taxonomyType,
                    )
                )
            }
        }

        if (doUpdateStatusType) eventsDao.updateEventStatusType(newEvent.eventStatusType, newEvent.id)
        if (doUpdateAttendanceMode) eventsDao.updateEventAttendanceMode(newEvent.eventAttendanceMode, newEvent.id)
    }

    private suspend fun saveDateRangeWithTimestamp(dateRange: LocalDateRange, timestamp: Instant) {
        rangesDao.getDateRangeTimestampsThatIntersectRange(
            rangeStart = dateRange.start.toEpochDays(),
            rangeEnd = dateRange.endInclusive.toEpochDays(),
        ).forEach { existing ->
            val existingStart = LocalDate.fromEpochDays(existing.dateRangeStart.toInt())
            val existingEnd = LocalDate.fromEpochDays(existing.dateRangeEnd.toInt())
            val startsBeforeNew = existingStart < dateRange.start
            val endsAfterNew = existingEnd > dateRange.endInclusive

            when {
                startsBeforeNew && endsAfterNew -> {
                    rangesDao.updateDateRangeTimestampWithId(
                        id = existing.id,
                        rangeStart = existingStart.toEpochDays(),
                        rangeEnd = dateRange.start.previousDay().toEpochDays(),
                    )
                    rangesDao.insertDateRangeTimestamp(
                        RoomDateRangeTimestamp(
                            dateRangeStart = dateRange.endInclusive.nextDay().toEpochDays(),
                            dateRangeEnd = existingEnd.toEpochDays(),
                            timestamp = existing.timestamp,
                        )
                    )
                }

                startsBeforeNew ->
                    rangesDao.updateDateRangeTimestampWithId(
                        id = existing.id,
                        rangeStart = existingStart.toEpochDays(),
                        rangeEnd = dateRange.start.previousDay().toEpochDays(),
                    )

                endsAfterNew ->
                    rangesDao.updateDateRangeTimestampWithId(
                        id = existing.id,
                        rangeStart = dateRange.endInclusive.nextDay().toEpochDays(),
                        rangeEnd = existingEnd.toEpochDays(),
                    )

                else -> rangesDao.deleteDateRangeTimestampWithId(existing.id)
            }
        }
        rangesDao.insertDateRangeTimestamp(
            RoomDateRangeTimestamp(
                dateRangeStart = dateRange.start.toEpochDays(),
                dateRangeEnd = dateRange.endInclusive.toEpochDays(),
                timestamp = timestamp.toEpochMilliseconds()
            )
        )
    }

    override suspend fun saveEventsForDiff(calendarEvents: List<CalendarEvent>): Collection<EventDiff> =
        withContext(defaultDispatcher) {
            buildSet {
                val existingEvents = eventsDao.selectEventsByIdWithDetails(
                    ids = calendarEvents.map { it.id }
                ).toCalendarEvent()

                val eventsToUpsert = calendarEvents
                    .mapTo(mutableListOf<Pair<CalendarEvent, CalendarEvent?>>()) { it to null }

                existingEvents.forEach { existingEvent ->
                    val correspondingNewEvent = calendarEvents.firstOrNull { it.id == existingEvent.id }

                    if (correspondingNewEvent != null) {
                        add(existingEvent diffWith correspondingNewEvent)

                        eventsToUpsert.removeAll { it.first.id == correspondingNewEvent.id }
                        eventsToUpsert.add(correspondingNewEvent to existingEvent)
                    }
                }

                eventsToUpsert.forEach { (calendarEvent, existingEvent) ->
                    replaceSingleEvent(calendarEvent, existingEvent)
                }
            }
        }

    override fun observeEventsById(ids: Collection<String>): Flow<Collection<CalendarEvent>> =
        eventsDao.observeEventsByIdWithDetails(ids.toList()).map { rows -> rows.toCalendarEvent() }

    private infix fun CalendarEvent.diffWith(other: CalendarEvent) = EventDiff(oldEvent = this, newEvent = other)

    private data class StorageEventImage(
        val calendarEventId: String,
        override val id: String?,
        override val url: String,
        override val width: Int,
        override val height: Int,
        override val fileSize: Int,
    ) : EventImage

    private data class StorageEventTaxonomy(
        override val name: String,
        override val slug: String,
        override val taxonomyType: TaxonomyType,
    ) : EventTaxonomy

    private data class StorageCalendarEvent(
        override val id: String,
        override val creationDate: Instant,
        override val modifiedDate: Instant,
        override val title: String,
        override val url: String,
        override val startDate: Instant,
        override val endDate: Instant,
        override val enrollmentUrl: String,
        override val images: Collection<EventImage>,
        override val eventStatusType: EventStatusType?,
        override val eventAttendanceMode: EventAttendanceMode?,
        override val taxonomies: Collection<EventTaxonomy>,
    ) : CalendarEvent

    private fun EventImage.asCacheEventImage(eventId: String) = RoomEventImage(
        calendarEventId = eventId,
        id = id,
        url = url,
        width = width.toLong(),
        height = height.toLong(),
        fileSize = fileSize.toLong(),
    )

    private fun List<RoomEventWithDetails>.toCalendarEvent(): List<StorageCalendarEvent> = groupBy { it.id }
        .map { (_, rowList) ->
            val calendarEvent = rowList.first()

            val eventImages = rowList.distinctBy { it.imageId }.mapNotNullTo(mutableSetOf()) { eventImage ->
                if (eventImage.imageUrl != null &&
                    eventImage.imageWidth != null &&
                    eventImage.imageHeight != null &&
                    eventImage.imageFileSize != null
                ) {
                    StorageEventImage(
                        calendarEventId = calendarEvent.id,
                        id = eventImage.imageId,
                        url = eventImage.imageUrl,
                        width = eventImage.imageWidth.toInt(),
                        height = eventImage.imageHeight.toInt(),
                        fileSize = eventImage.imageFileSize.toInt(),
                    )
                } else null
            }

            val eventTaxonomies = rowList.distinctBy { it.taxonomySlug }
                .mapNotNullTo(mutableSetOf()) {
                    with(it) {
                        if (taxonomyName != null && taxonomySlug != null && taxonomyType != null)
                            StorageEventTaxonomy(
                                name = taxonomyName,
                                slug = taxonomySlug,
                                taxonomyType = taxonomyType,
                            )
                        else null
                    }
                }

            StorageCalendarEvent(
                id = calendarEvent.id,
                creationDate = Instant.fromEpochMilliseconds(calendarEvent.creationDate),
                modifiedDate = Instant.fromEpochMilliseconds(calendarEvent.modifiedDate),
                title = calendarEvent.title,
                url = calendarEvent.url,
                startDate = Instant.fromEpochMilliseconds(calendarEvent.startDate),
                endDate = Instant.fromEpochMilliseconds(calendarEvent.endDate),
                enrollmentUrl = calendarEvent.enrollmentUrl,
                images = eventImages,
                eventStatusType = calendarEvent.eventStatusType,
                eventAttendanceMode = calendarEvent.eventAttendanceMode,
                taxonomies = eventTaxonomies,
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
