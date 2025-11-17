package pt.rikmartins.clubemg.mobile.ui.entity

import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
data class WeekOfEvents(
    val range: ClosedRange<LocalDate>,
    val events: List<Event>,
) {

    interface Event
}