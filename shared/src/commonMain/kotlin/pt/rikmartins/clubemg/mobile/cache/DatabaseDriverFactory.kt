package pt.rikmartins.clubemg.mobile.cache

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DatabaseDriverFactory): AppDatabase {
    val driver = driverFactory.createDriver()

    return AppDatabase(
        driver = driver,
        CalendarEventAdapter = CalendarEvent.Adapter(
            creationDateAdapter = instantAdapter,
            modifiedDateAdapter = instantAdapter,
            startDateAdapter = instantAdapter,
            endDateAdapter = instantAdapter
        ),
        DateRangeTimestampAdapter = DateRangeTimestamp.Adapter(
            dateRangeStartAdapter = localDateAdapter,
            dateRangeEndAdapter = localDateAdapter,
            timestampAdapter = instantAdapter
        )
    )
}
