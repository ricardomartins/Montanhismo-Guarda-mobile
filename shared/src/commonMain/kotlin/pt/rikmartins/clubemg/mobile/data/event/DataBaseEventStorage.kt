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
import pt.rikmartins.clubemg.mobile.cache.CalendarEvent as CacheCalendarEvent
import pt.rikmartins.clubemg.mobile.cache.EventImage as CacheEventImage
import pt.rikmartins.clubemg.mobile.cache.SelectAllWithDetails
import pt.rikmartins.clubemg.mobile.cache.SelectEventsByIdWithDetails
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

    override val events: Flow<List<CalendarEvent>> = eventsQueries.selectAllWithDetails().asFlow()
        .mapToList(defaultDispatcher).map { rows -> rows.toCalendarEventAll() }

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
        val existingEvents = selectEventsInRange(
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

        deleteEvents(eventsToDelete.map { calendarEvent -> calendarEvent.id })

        deleteImagesOfEvents(eventsToUpsert.map { calendarEvent -> calendarEvent.id })
        eventsToUpsert.forEach { calendarEvent -> replaceSingleEvent(calendarEvent) }
    }

    private fun EventsQueries.replaceSingleEvent(event: CalendarEvent) {
        replaceEvent(
            id = event.id,
            creationDate = event.creationDate,
            modifiedDate = event.modifiedDate,
            title = event.title,
            url = event.url,
            startDate = event.startDate,
            endDate = event.endDate,
            enrollmentUrl = event.enrollmentUrl,
            eventStatusType = event.eventStatusType,
            eventAttendanceMode = event.eventAttendanceMode,
        )
        if (event.eventStatusType != null) updateEventStatusType(event.eventStatusType, event.id)
        if (event.eventAttendanceMode != null) updateEventAttendanceMode(event.eventAttendanceMode, event.id)
        event.images.forEach { replaceEventImage(it.asCacheEventImage(event.id)) }
        eventsQueries.removeAllTaxonomiesFromCalendarEvent(event.id)
        event.taxonomies.forEach {
            eventsQueries.addEventTaxonomy(
                CacheEventTaxonomy(
                    slug = it.slug,
                    taxonomyType = it.taxonomyType,
                    name = it.name,
                )
            )
            eventsQueries.addTaxonomyToCalendarEvent(
                CalendarEvent_EventTaxonomy(
                    calendarEventId = event.id,
                    eventTaxonomySlug = it.slug,
                    eventTaxonomyType = it.taxonomyType,
                )
            )
        }
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
                    val existingEvents = eventsQueries.selectEventsById(calendarEvents.map { it.id }).executeAsList()

                    val eventsToUpsert = calendarEvents.toMutableList()

                    existingEvents.forEach { existingEvent ->
                        val correspondingNewEvent = calendarEvents.firstOrNull { it.id == existingEvent.id }

                        if (correspondingNewEvent != null) {
                            add(existingEvent diffWith correspondingNewEvent)

                            if (correspondingNewEvent.modifiedDate <= existingEvent.modifiedDate)
                                eventsToUpsert.remove(correspondingNewEvent)
                        }
                    }

                    eventsQueries.deleteImagesOfEvents(eventsToUpsert.map { calendarEvent -> calendarEvent.id })
                    eventsToUpsert.forEach { calendarEvent -> eventsQueries.replaceSingleEvent(calendarEvent) }
                }
            }
        }

    override fun observeEventsById(ids: Collection<String>): Flow<Collection<CalendarEvent>> =
        eventsQueries.selectEventsByIdWithDetails(ids).asFlow().mapToList(defaultDispatcher)
            .map { rows -> rows.toCalendarEventById() }

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
            images = emptySet(),
            eventStatusType = eventStatusType,
            eventAttendanceMode = eventAttendanceMode,
            taxonomies = emptySet(),
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
        override val images: Set<StorageEventImage>,
        override val eventStatusType: EventStatusType?,
        override val eventAttendanceMode: EventAttendanceMode?,
        override val taxonomies: Collection<StorageEventTaxonomy>,
    ) : CalendarEvent

    private fun EventImage.asCacheEventImage(eventId: String) = CacheEventImage(
        calendarEventId = eventId,
        id = id,
        url = url,
        width = width.toLong(),
        height = height.toLong(),
        fileSize = fileSize.toLong(),
    )

    private fun List<SelectAllWithDetails>.toCalendarEventAll(): List<StorageCalendarEvent> = groupBy { it.id }
        .map { (_, rowList) ->
            val calendarEvent = rowList.first()

            val eventImages = rowList.distinctBy { it.image_id }.mapNotNullTo(mutableSetOf()) { eventImage ->
                if (eventImage.image_url != null &&
                    eventImage.image_width != null &&
                    eventImage.image_height != null &&
                    eventImage.image_fileSize != null
                ) {
                    StorageEventImage(
                        calendarEventId = calendarEvent.id,
                        id = eventImage.image_id,
                        url = eventImage.image_url,
                        width = eventImage.image_width.toInt(),
                        height = eventImage.image_height.toInt(),
                        fileSize = eventImage.image_fileSize.toInt(),
                    )
                } else null
            }

            val eventTaxonomies = rowList.distinctBy { it.taxonomy_slug }
                .mapNotNullTo(mutableSetOf()) {
                    with(it) {
                        if (taxonomy_name != null && taxonomy_slug != null && taxonomy_type != null)
                            StorageEventTaxonomy(
                                name = taxonomy_name,
                                slug = taxonomy_slug,
                                taxonomyType = taxonomy_type,
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

    private fun List<SelectEventsByIdWithDetails>.toCalendarEventById(): List<StorageCalendarEvent> = groupBy { it.id }
        .map { (_, rowList) ->
            val calendarEvent = rowList.first()

            val eventImages = rowList.distinctBy { it.image_id }.mapNotNullTo(mutableSetOf()) { eventImage ->
                if (eventImage.image_url != null &&
                    eventImage.image_width != null &&
                    eventImage.image_height != null &&
                    eventImage.image_fileSize != null
                ) {
                    StorageEventImage(
                        calendarEventId = calendarEvent.id,
                        id = eventImage.image_id,
                        url = eventImage.image_url,
                        width = eventImage.image_width.toInt(),
                        height = eventImage.image_height.toInt(),
                        fileSize = eventImage.image_fileSize.toInt(),
                    )
                } else null
            }

            val eventTaxonomies = rowList.distinctBy { it.taxonomy_slug }
                .mapNotNullTo(mutableSetOf()) {
                    with(it) {
                        if (taxonomy_name != null && taxonomy_slug != null && taxonomy_type != null)
                            StorageEventTaxonomy(
                                name = taxonomy_name,
                                slug = taxonomy_slug,
                                taxonomyType = taxonomy_type,
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