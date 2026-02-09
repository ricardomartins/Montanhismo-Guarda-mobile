package pt.rikmartins.clubemg.mobile

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

fun LocalDate.previousDay(): LocalDate = minus(1, DateTimeUnit.DAY)

fun LocalDate.nextDay(): LocalDate = plus(1, DateTimeUnit.DAY)

private fun LocalDate.thisWeeksDayOfWeek(dayOfWeek: DayOfWeek): LocalDate =
    plus(dayOfWeek.ordinal - this.dayOfWeek.ordinal, DateTimeUnit.DAY)

fun LocalDate.thisWeeksMonday(): LocalDate = thisWeeksDayOfWeek(DayOfWeek.MONDAY)
//fun LocalDate.thisWeeksTuesday(): LocalDate = thisWeeksDayOfWeek(DayOfWeek.TUESDAY)
//fun LocalDate.thisWeeksWednesday(): LocalDate = thisWeeksDayOfWeek(DayOfWeek.WEDNESDAY)
//fun LocalDate.thisWeeksThursday(): LocalDate = thisWeeksDayOfWeek(DayOfWeek.THURSDAY)
fun LocalDate.thisWeeksFriday(): LocalDate = thisWeeksDayOfWeek(DayOfWeek.FRIDAY)
fun LocalDate.thisWeeksSaturday(): LocalDate = thisWeeksDayOfWeek(DayOfWeek.SATURDAY)
fun LocalDate.thisWeeksSunday(): LocalDate = thisWeeksDayOfWeek(DayOfWeek.SUNDAY)
