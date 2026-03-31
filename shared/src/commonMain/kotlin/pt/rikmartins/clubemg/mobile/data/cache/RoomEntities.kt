package pt.rikmartins.clubemg.mobile.data.cache

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventAttendanceMode
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventStatusType
import pt.rikmartins.clubemg.mobile.domain.usecase.events.TaxonomyType

@Entity(tableName = "CalendarEvent")
data class RoomCalendarEvent(
    @PrimaryKey val id: String,
    val creationDate: Long,
    val modifiedDate: Long,
    val title: String,
    val url: String,
    val startDate: Long,
    val endDate: Long,
    val enrollmentUrl: String,
    val eventStatusType: EventStatusType?,
    val eventAttendanceMode: EventAttendanceMode?
)

@Entity(
    tableName = "EventImage",
    foreignKeys = [
        ForeignKey(
            entity = RoomCalendarEvent::class,
            parentColumns = ["id"],
            childColumns = ["calendarEventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("calendarEventId")]
)
data class RoomEventImage(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val calendarEventId: String,
    val id: String?,
    val url: String,
    val width: Long,
    val height: Long,
    val fileSize: Long
)

@Entity(
    tableName = "EventTaxonomy",
    primaryKeys = ["slug", "taxonomyType"]
)
data class RoomEventTaxonomy(
    val slug: String,
    val taxonomyType: TaxonomyType,
    val name: String
)

@Entity(
    tableName = "CalendarEvent_EventTaxonomy",
    primaryKeys = ["calendarEventId", "eventTaxonomySlug", "eventTaxonomyType"],
    foreignKeys = [
        ForeignKey(
            entity = RoomCalendarEvent::class,
            parentColumns = ["id"],
            childColumns = ["calendarEventId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoomEventTaxonomy::class,
            parentColumns = ["slug", "taxonomyType"],
            childColumns = ["eventTaxonomySlug", "eventTaxonomyType"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("calendarEventId"), Index("eventTaxonomySlug", "eventTaxonomyType")]
)
data class RoomCalendarEventEventTaxonomy(
    val calendarEventId: String,
    val eventTaxonomySlug: String,
    val eventTaxonomyType: TaxonomyType
)

@Entity(tableName = "DateRangeTimestamp")
data class RoomDateRangeTimestamp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateRangeStart: Long,
    val dateRangeEnd: Long,
    val timestamp: Long
)

@Entity(tableName = "SingleValue")
data class RoomSingleValue(
    @PrimaryKey val key: String,
    val value: String
)

data class RoomEventWithDetails(
    val id: String,
    val creationDate: Long,
    val modifiedDate: Long,
    val title: String,
    val url: String,
    val startDate: Long,
    val endDate: Long,
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
    val taxonomyType: TaxonomyType?
)
