package pt.rikmartins.clubemg.mobile.domain.entity

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
data class WeekOfEvents(
    val range: OpenEndRange<Instant>,
    val events: List<Event>,
) {

    interface Event
}