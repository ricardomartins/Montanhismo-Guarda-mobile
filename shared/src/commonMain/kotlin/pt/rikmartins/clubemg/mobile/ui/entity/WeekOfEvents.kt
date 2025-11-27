package pt.rikmartins.clubemg.mobile.ui.entity

import kotlinx.datetime.LocalDateRange
import pt.rikmartins.clubemg.mobile.domain.entity.CalendarEvent
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
data class WeekOfEvents(
    val range: LocalDateRange,
    val events: List<CalendarEvent>,
)