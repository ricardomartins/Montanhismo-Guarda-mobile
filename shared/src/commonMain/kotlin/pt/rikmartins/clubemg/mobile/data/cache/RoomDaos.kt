package pt.rikmartins.clubemg.mobile.data.cache

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventAttendanceMode
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventStatusType

@Dao
interface EventsDao {
    @Query("""
        SELECT e.id, e.creationDate, e.modifiedDate, e.title, e.url, e.startDate, e.endDate, e.enrollmentUrl, 
               e.eventStatusType, e.eventAttendanceMode,
               i.id AS imageId, i.url AS imageUrl, i.width AS imageWidth, i.height AS imageHeight, i.fileSize AS imageFileSize,
               t.slug AS taxonomySlug, t.name AS taxonomyName, t.taxonomyType AS taxonomyType
        FROM CalendarEvent e
        LEFT JOIN EventImage i ON e.id = i.calendarEventId
        LEFT JOIN CalendarEvent_EventTaxonomy et ON e.id = et.calendarEventId
        LEFT JOIN EventTaxonomy t ON et.eventTaxonomySlug = t.slug AND et.eventTaxonomyType = t.taxonomyType
    """)
    fun selectAllWithDetails(): Flow<List<RoomEventWithDetails>>

    @Query("""
        SELECT e.id, e.creationDate, e.modifiedDate, e.title, e.url, e.startDate, e.endDate, e.enrollmentUrl, 
               e.eventStatusType, e.eventAttendanceMode,
               i.id AS imageId, i.url AS imageUrl, i.width AS imageWidth, i.height AS imageHeight, i.fileSize AS imageFileSize,
               t.slug AS taxonomySlug, t.name AS taxonomyName, t.taxonomyType AS taxonomyType
        FROM CalendarEvent e
        LEFT JOIN EventImage i ON e.id = i.calendarEventId
        LEFT JOIN CalendarEvent_EventTaxonomy et ON e.id = et.calendarEventId
        LEFT JOIN EventTaxonomy t ON et.eventTaxonomySlug = t.slug AND et.eventTaxonomyType = t.taxonomyType
        WHERE e.startDate >= :rangeStart AND e.endDate <= :rangeEnd
    """)
    suspend fun selectEventsInRangeWithDetails(rangeStart: Long, rangeEnd: Long): List<RoomEventWithDetails>

    @Query("""
        SELECT e.id, e.creationDate, e.modifiedDate, e.title, e.url, e.startDate, e.endDate, e.enrollmentUrl, 
               e.eventStatusType, e.eventAttendanceMode,
               i.id AS imageId, i.url AS imageUrl, i.width AS imageWidth, i.height AS imageHeight, i.fileSize AS imageFileSize,
               t.slug AS taxonomySlug, t.name AS taxonomyName, t.taxonomyType AS taxonomyType
        FROM CalendarEvent e
        LEFT JOIN EventImage i ON e.id = i.calendarEventId
        LEFT JOIN CalendarEvent_EventTaxonomy et ON e.id = et.calendarEventId
        LEFT JOIN EventTaxonomy t ON et.eventTaxonomySlug = t.slug AND et.eventTaxonomyType = t.taxonomyType
        WHERE e.id IN (:ids)
    """)
    fun observeEventsByIdWithDetails(ids: List<String>): Flow<List<RoomEventWithDetails>>

    @Query("""
        SELECT e.id, e.creationDate, e.modifiedDate, e.title, e.url, e.startDate, e.endDate, e.enrollmentUrl, 
               e.eventStatusType, e.eventAttendanceMode,
               i.id AS imageId, i.url AS imageUrl, i.width AS imageWidth, i.height AS imageHeight, i.fileSize AS imageFileSize,
               t.slug AS taxonomySlug, t.name AS taxonomyName, t.taxonomyType AS taxonomyType
        FROM CalendarEvent e
        LEFT JOIN EventImage i ON e.id = i.calendarEventId
        LEFT JOIN CalendarEvent_EventTaxonomy et ON e.id = et.calendarEventId
        LEFT JOIN EventTaxonomy t ON et.eventTaxonomySlug = t.slug AND et.eventTaxonomyType = t.taxonomyType
        WHERE e.id IN (:ids)
    """)
    suspend fun selectEventsByIdWithDetails(ids: List<String>): List<RoomEventWithDetails>

    @Query("DELETE FROM CalendarEvent WHERE id IN (:ids)")
    suspend fun deleteEvents(ids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceEvent(event: RoomCalendarEvent)

    @Query("DELETE FROM EventImage WHERE calendarEventId = :eventId")
    suspend fun deleteImagesOfEvent(eventId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceEventImage(image: RoomEventImage)

    @Query("DELETE FROM CalendarEvent_EventTaxonomy WHERE calendarEventId = :eventId")
    suspend fun removeAllTaxonomiesFromCalendarEvent(eventId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addEventTaxonomy(taxonomy: RoomEventTaxonomy)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTaxonomyToCalendarEvent(eventTaxonomy: RoomCalendarEventEventTaxonomy)

    @Query("UPDATE CalendarEvent SET eventStatusType = :statusType WHERE id = :id")
    suspend fun updateEventStatusType(statusType: EventStatusType?, id: String)

    @Query("UPDATE CalendarEvent SET eventAttendanceMode = :attendanceMode WHERE id = :id")
    suspend fun updateEventAttendanceMode(attendanceMode: EventAttendanceMode?, id: String)
}

@Dao
interface RangesDao {
    @Query("SELECT * FROM DateRangeTimestamp WHERE dateRangeStart <= :rangeEnd AND dateRangeEnd >= :rangeStart")
    suspend fun getDateRangeTimestampsThatIntersectRange(rangeStart: Long, rangeEnd: Long): List<RoomDateRangeTimestamp>

    @Query("UPDATE DateRangeTimestamp SET dateRangeStart = :rangeStart, dateRangeEnd = :rangeEnd WHERE id = :id")
    suspend fun updateDateRangeTimestampWithId(id: Long, rangeStart: Long, rangeEnd: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDateRangeTimestamp(range: RoomDateRangeTimestamp)

    @Query("DELETE FROM DateRangeTimestamp WHERE id = :id")
    suspend fun deleteDateRangeTimestampWithId(id: Long)
}

@Dao
interface SingleValuesDao {
    @Query("SELECT * FROM SingleValue WHERE `key` = :key")
    suspend fun getTextValue(key: String): RoomSingleValue?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setTextValue(singleValue: RoomSingleValue)
}