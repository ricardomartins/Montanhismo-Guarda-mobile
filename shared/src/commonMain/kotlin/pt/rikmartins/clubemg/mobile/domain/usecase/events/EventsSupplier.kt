package pt.rikmartins.clubemg.mobile.domain.usecase.events

import pt.rikmartins.clubemg.mobile.domain.gateway.CalendarEvent
import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase
import kotlinx.coroutines.flow.Flow

class EventsSupplier(private val eventRepository: EventRepository): WatchCase.Supplier<List<CalendarEvent>>() {

    override fun execute(): Flow<List<CalendarEvent>> = eventRepository.events
}