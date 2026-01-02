package pt.rikmartins.clubemg.mobile.cache

import app.cash.sqldelight.ColumnAdapter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

val localDateAdapter = object : ColumnAdapter<LocalDate, Long> {

    override fun decode(databaseValue: Long): LocalDate = LocalDate.fromEpochDays(databaseValue)

    override fun encode(value: LocalDate): Long = value.toEpochDays()
}

val instantAdapter = object : ColumnAdapter<Instant, Long> {

    override fun decode(databaseValue: Long): Instant = Instant.fromEpochMilliseconds(databaseValue)

    override fun encode(value: Instant): Long = value.toEpochMilliseconds()
}
