package pt.rikmartins.clubemg.mobile.cache

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import kotlinx.datetime.LocalDate
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventAttendanceMode
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventStatusType
import pt.rikmartins.clubemg.mobile.domain.usecase.events.TaxonomyType
import kotlin.time.Instant

interface SqlDriverFactory {
    fun createDriver(): SqlDriver
}

internal class DatabaseDriverFactory(
    private val driverFactory: SqlDriverFactory,
) {

    fun createDatabase(): AppDatabase = AppDatabase(
        driver = driverFactory.createDriver(),
        CalendarEventAdapter = CalendarEvent.Adapter(
            creationDateAdapter = instantAdapter,
            modifiedDateAdapter = instantAdapter,
            startDateAdapter = instantAdapter,
            endDateAdapter = instantAdapter,
            eventStatusTypeAdapter = EnumColumnAdapter<EventStatusType>(),
            eventAttendanceModeAdapter = EnumColumnAdapter<EventAttendanceMode>(),
        ),
        CalendarEvent_EventTaxonomyAdapter = CalendarEvent_EventTaxonomy.Adapter(
            eventTaxonomyTypeAdapter = taxonomyTypeAdapter,
        ),
        EventTaxonomyAdapter = EventTaxonomy.Adapter(
            taxonomyTypeAdapter = taxonomyTypeAdapter,
        ),
        DateRangeTimestampAdapter = DateRangeTimestamp.Adapter(
            dateRangeStartAdapter = localDateAdapter,
            dateRangeEndAdapter = localDateAdapter,
            timestampAdapter = instantAdapter,
        )
    )

    internal val localDateAdapter = object : ColumnAdapter<LocalDate, Long> {

        override fun decode(databaseValue: Long): LocalDate = LocalDate.fromEpochDays(databaseValue)

        override fun encode(value: LocalDate): Long = value.toEpochDays()
    }

    internal val instantAdapter = object : ColumnAdapter<Instant, Long> {

        override fun decode(databaseValue: Long): Instant = Instant.fromEpochMilliseconds(databaseValue)

        override fun encode(value: Instant): Long = value.toEpochMilliseconds()
    }

    private val taxonomyTypeAdapter = EnumColumnAdapter<TaxonomyType>()
}