@file:OptIn(ExperimentalTime::class)

package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun Instant.toLocalDate(timeZone: TimeZone): LocalDate =
    toLocalDateTime(timeZone).date
