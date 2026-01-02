package pt.rikmartins.clubemg.mobile.ui

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateProgression
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.step
import pt.rikmartins.clubemg.mobile.thisWeeksMonday

internal object WeekUtils {

    fun getMondaysInRange(range: LocalDateRange): LocalDateProgression =
        (range.start.thisWeeksMonday()..range.endInclusive).step(1, DateTimeUnit.WEEK)
}