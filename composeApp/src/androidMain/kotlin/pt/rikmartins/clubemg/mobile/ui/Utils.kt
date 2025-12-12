package pt.rikmartins.clubemg.mobile.ui

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

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

fun LocalDate.nextFriday(): LocalDate = (DayOfWeek.FRIDAY.ordinal - dayOfWeek.ordinal)
    .let { if (it < 0) it + 7 else it }
    .let { plus(it, DateTimeUnit.DAY) }
