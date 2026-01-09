package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.flow.Flow
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase

class ObserveAllEvents(private val gateway: Gateway) : WatchCase.Supplier<List<CalendarEvent>>() {

    override fun execute(): Flow<List<CalendarEvent>> = gateway.events

    interface Gateway {
        val events: Flow<List<CalendarEvent>>
    }
}