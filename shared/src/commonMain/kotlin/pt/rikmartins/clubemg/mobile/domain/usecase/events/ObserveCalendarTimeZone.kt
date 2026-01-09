package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase

class ObserveCalendarTimeZone(private val gateway: Gateway) : WatchCase.Supplier<TimeZone>() {

    override fun execute(): Flow<TimeZone> = gateway.eventsTimezone

    interface Gateway {
        val eventsTimezone: Flow<TimeZone>
    }
}