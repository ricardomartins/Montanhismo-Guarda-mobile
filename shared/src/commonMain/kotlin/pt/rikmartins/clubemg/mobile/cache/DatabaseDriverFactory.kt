package pt.rikmartins.clubemg.mobile.cache

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import co.touchlab.kermit.Logger
import kotlinx.datetime.LocalDate
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventAttendanceMode
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventStatusType
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
            eventStatusTypeAdapter = eventStatusTypeAdapter,
            eventAttendanceModeAdapter = eventAttendanceModeAdapter,
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

    private val eventStatusTypeAdapter = object : ColumnAdapter<EventStatusType, String> {

        private val logger: Logger = Logger.withTag("eventStatusTypeAdapter")

        override fun decode(databaseValue: String): EventStatusType = try {
            EventStatusType.valueOf(databaseValue)
        } catch (_: IllegalArgumentException) {
            logger.w { "Invalid event status type: $databaseValue, defaulting to ${EventStatusType.Scheduled}" }
            EventStatusType.Scheduled
        }

        override fun encode(value: EventStatusType): String = value.name
    }

    private val eventAttendanceModeAdapter = object : ColumnAdapter<EventAttendanceMode, String> {

        private val logger: Logger = Logger.withTag("eventAttendanceModeAdapter")

        override fun decode(databaseValue: String): EventAttendanceMode = try {
            EventAttendanceMode.valueOf(databaseValue)
        } catch (_: IllegalArgumentException) {
            logger.w { "Invalid event status type: $databaseValue, defaulting to ${EventAttendanceMode.Offline}" }
            EventAttendanceMode.Offline
        }

        override fun encode(value: EventAttendanceMode): String = value.name
    }
}