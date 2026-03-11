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
import pt.rikmartins.clubemg.mobile.cache.CalendarEvent_EventTaxonomy
import pt.rikmartins.clubemg.mobile.cache.EventTaxonomy as CacheEventTaxonomy
import pt.rikmartins.clubemg.mobile.cache.EventsQueries
import pt.rikmartins.clubemg.mobile.cache.EventImage as CacheEventImage
import pt.rikmartins.clubemg.mobile.domain.usecase.events.CalendarEvent
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventAttendanceMode
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventDiff
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventImage
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventStatusType
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventTaxonomy
import pt.rikmartins.clubemg.mobile.domain.usecase.events.TaxonomyType
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

    private val eventsQueries = database.eventsQueries
    private val rangesQueries = database.rangesQueries
    private val singleValuesQueries = database.singleValuesQueries

    override val events: Flow<List<CalendarEvent>> = eventsQueries
        .selectAllWithDetails(mapper = ::GenericCacheEventWithDetails).asFlow().mapToList(defaultDispatcher)
        .map { rows -> rows.toCalendarEvent() }

    override suspend fun getTimeZone(): TimeZone = withContext(defaultDispatcher) {
        TimeZone.of(singleValuesQueries.getTextValue(TIMEZONE_KEY).executeAsOneOrNull()?.value_ ?: DEFAULT_API_TIMEZONE)
    }

    override suspend fun setTimeZone(timezone: TimeZone): Unit = withContext(defaultDispatcher) {
        TODO("Not yet implemented")
    }

    override suspend fun getDateRangeTimestampsOf(
        dateRange: LocalDateRange,
    ): List<EventRepository.DateRangeTimestamp> = withContext(defaultDispatcher) {
        rangesQueries.getDateRangeTimestampsThatIntersectRange(
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
        val timeZone =
            TimeZone.of(
                singleValuesQueries.getTextValue(TIMEZONE_KEY).executeAsOneOrNull()?.value_ ?: DEFAULT_API_TIMEZONE
            )

        eventsQueries.transaction {
            eventsQueries.saveEvents(events, dateRange, timeZone)
            saveDateRangeWithTimestamp(dateRange, timestamp)
        }
    }

    private fun EventsQueries.saveEvents(
        events: List<CalendarEvent>,
        dateRange: LocalDateRange,
        timeZone: TimeZone,
    ) {
        val existingEvents = selectEventsInRangeWithDetails(
            rangeStart = dateRange.start.atStartOfDayIn(timeZone),
            rangeEnd = dateRange.endInclusive.atEndOfDayIn(timeZone),
            mapper = ::GenericCacheEventWithDetails
        ).executeAsList().toCalendarEvent()

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
        deleteEvents(eventsToDelete.map { calendarEvent -> calendarEvent.id })
        eventsToUpsert.forEach { (calendarEvent, existingEvent) -> replaceSingleEvent(calendarEvent, existingEvent) }
    }

    private fun EventsQueries.replaceSingleEvent(newEvent: CalendarEvent, existingEvent: CalendarEvent?) {
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
            replaceEvent(
                id = newEvent.id,
                creationDate = newEvent.creationDate,
                modifiedDate = newEvent.modifiedDate,
                title = newEvent.title,
                url = newEvent.url,
                startDate = newEvent.startDate,
                endDate = newEvent.endDate,
                enrollmentUrl = newEvent.enrollmentUrl,
                eventStatusType = newEvent.eventStatusType,
                eventAttendanceMode = newEvent.eventAttendanceMode,
            )

            if (existingEvent != null) deleteImagesOfEvent(existingEvent.id)
            newEvent.images.forEach { replaceEventImage(it.asCacheEventImage(newEvent.id)) }
        }

        if (doUpdateTaxonomies) {
            if (existingEvent != null) removeAllTaxonomiesFromCalendarEvent(existingEvent.id)
            newEvent.taxonomies.forEach {
                addEventTaxonomy(
                    CacheEventTaxonomy(
                        slug = it.slug,
                        taxonomyType = it.taxonomyType,
                        name = it.name,
                    )
                )
                addTaxonomyToCalendarEvent(
                    CalendarEvent_EventTaxonomy(
                        calendarEventId = newEvent.id,
                        eventTaxonomySlug = it.slug,
                        eventTaxonomyType = it.taxonomyType,
                    )
                )
            }
        }

        if (doUpdateStatusType) updateEventStatusType(newEvent.eventStatusType, newEvent.id)
        if (doUpdateAttendanceMode) updateEventAttendanceMode(newEvent.eventAttendanceMode, newEvent.id)
    }

    private fun saveDateRangeWithTimestamp(dateRange: LocalDateRange, timestamp: Instant) {
        rangesQueries.getDateRangeTimestampsThatIntersectRange(
            rangeStart = dateRange.start,
            rangeEnd = dateRange.endInclusive,
        )
            .executeAsList().forEach { existing ->
                val startsBeforeNew = existing.dateRangeStart < dateRange.start
                val endsAfterNew = existing.dateRangeEnd > dateRange.endInclusive

                when {
                    startsBeforeNew && endsAfterNew -> {
                        // Existing range contains the new one
                        rangesQueries.updateDateRangeTimestampWithId(
                            id = existing.id,
                            rangeStart = existing.dateRangeStart,
                            rangeEnd = dateRange.start.previousDay(),
                        )
                        rangesQueries.insertDateRangeTimestamp(
                            rangeStart = dateRange.endInclusive.nextDay(),
                            rangeEnd = existing.dateRangeEnd,
                            timestamp = existing.timestamp,
                        )
                    }

                    startsBeforeNew ->
                        // Existing range starts before the new one
                        rangesQueries.updateDateRangeTimestampWithId(
                            id = existing.id,
                            rangeStart = existing.dateRangeStart,
                            rangeEnd = dateRange.start.previousDay(),
                        )

                    endsAfterNew ->
                        // Existing range ends after the new one
                        rangesQueries.updateDateRangeTimestampWithId(
                            id = existing.id,
                            rangeStart = dateRange.endInclusive.nextDay(),
                            rangeEnd = existing.dateRangeEnd,
                        )

                    else -> rangesQueries.deleteDateRangeTimestampWithId(existing.id)
                }
            }
        rangesQueries.insertDateRangeTimestamp(
            rangeStart = dateRange.start,
            rangeEnd = dateRange.endInclusive,
            timestamp = timestamp
        )
    }

    override suspend fun saveEventsForDiff(calendarEvents: List<CalendarEvent>): Collection<EventDiff> =
        withContext(defaultDispatcher) {
            buildSet {
                eventsQueries.transaction {
                    val existingEvents = eventsQueries.selectEventsByIdWithDetails(
                        id = calendarEvents.map { it.id },
                        mapper = ::GenericCacheEventWithDetails
                    ).executeAsList().toCalendarEvent()

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
                        eventsQueries.replaceSingleEvent(calendarEvent, existingEvent)
                    }
                }
            }
        }

    override fun observeEventsById(ids: Collection<String>): Flow<Collection<CalendarEvent>> =
        eventsQueries.selectEventsByIdWithDetails(id = ids, mapper = ::GenericCacheEventWithDetails).asFlow()
            .mapToList(defaultDispatcher).map { rows -> rows.toCalendarEvent() }

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

    private fun EventImage.asCacheEventImage(eventId: String) = CacheEventImage(
        calendarEventId = eventId,
        id = id,
        url = url,
        width = width.toLong(),
        height = height.toLong(),
        fileSize = fileSize.toLong(),
    )

    private data class GenericCacheEventWithDetails(
        val id: String,
        val creationDate: Instant,
        val modifiedDate: Instant,
        val title: String,
        val url: String,
        val startDate: Instant,
        val endDate: Instant,
        val enrollmentUrl: String,
        val eventStatusType: EventStatusType?,
        val eventAttendanceMode: EventAttendanceMode?,
        val imageId: String?,
        val imageUrl: String?,
        val imageWidth: Long?,
        val imageHeight: Long?,
        val imageFileSize: Long?,
        val taxonomySlug: String?,
        val taxonomyName: String?,
        val taxonomyType: TaxonomyType?,
    )

    private fun List<GenericCacheEventWithDetails>.toCalendarEvent(): List<StorageCalendarEvent> = groupBy { it.id }
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
                creationDate = calendarEvent.creationDate,
                modifiedDate = calendarEvent.modifiedDate,
                title = calendarEvent.title,
                url = calendarEvent.url,
                startDate = calendarEvent.startDate,
                endDate = calendarEvent.endDate,
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