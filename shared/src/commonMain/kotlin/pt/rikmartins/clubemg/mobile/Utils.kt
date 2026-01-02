package pt.rikmartins.clubemg.mobile

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

fun LocalDate.previousDay(): LocalDate = minus(1, DateTimeUnit.DAY)

fun LocalDate.nextDay(): LocalDate = plus(1, DateTimeUnit.DAY)
