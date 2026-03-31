package pt.rikmartins.clubemg.mobile.data.cache

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventAttendanceMode
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventStatusType
import pt.rikmartins.clubemg.mobile.domain.usecase.events.TaxonomyType

class RoomConverters {
    @TypeConverter
    fun fromEventStatusType(value: EventStatusType?): String? = value?.name

    @TypeConverter
    fun toEventStatusType(value: String?): EventStatusType? = value?.let { enumValueOf<EventStatusType>(it) }

    @TypeConverter
    fun fromEventAttendanceMode(value: EventAttendanceMode?): String? = value?.name

    @TypeConverter
    fun toEventAttendanceMode(value: String?): EventAttendanceMode? = value?.let { enumValueOf<EventAttendanceMode>(it) }

    @TypeConverter
    fun fromTaxonomyType(value: TaxonomyType): String = value.name

    @TypeConverter
    fun toTaxonomyType(value: String): TaxonomyType = enumValueOf<TaxonomyType>(value)
}

@Database(
    entities = [
        RoomCalendarEvent::class,
        RoomEventImage::class,
        RoomEventTaxonomy::class,
        RoomCalendarEventEventTaxonomy::class,
        RoomDateRangeTimestamp::class,
        RoomSingleValue::class
    ],
    version = 1
)
@ConstructedBy(AppDatabaseConstructor::class)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventsDao(): EventsDao
    abstract fun rangesDao(): RangesDao
    abstract fun singleValuesDao(): SingleValuesDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

fun getRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase = builder
    .setDriver(BundledSQLiteDriver())
    .build()