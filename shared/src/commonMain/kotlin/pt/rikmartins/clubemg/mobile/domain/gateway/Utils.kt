package pt.rikmartins.clubemg.mobile.domain.gateway

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

val LocalDate.weekOfYear: Int
    get() {
        fun getFirstDayOfWeekYear(year: Int): LocalDate {
            val fourthDayOfYear = LocalDate(year, 1, 4)
            return fourthDayOfYear.minus(fourthDayOfYear.dayOfWeek.ordinal, DateTimeUnit.DAY)
        }

        val firstDayOfThisWeekYear = getFirstDayOfWeekYear(year)
        val firstDayOfRelevantWeekYear =
            if (this >= firstDayOfThisWeekYear) {
                val firstDayOfNextWeekYear = getFirstDayOfWeekYear(year + 1)
                if (this >= firstDayOfNextWeekYear) {
                    firstDayOfNextWeekYear
                } else {
                    firstDayOfThisWeekYear
                }
            } else {
                getFirstDayOfWeekYear(year - 1)
            }
        return (this.dayOfYear - firstDayOfRelevantWeekYear.dayOfYear) / 7 + 1
    }