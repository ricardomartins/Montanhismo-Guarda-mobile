package pt.rikmartins.clubemg.mobile.domain.usecase.events

import pt.rikmartins.clubemg.mobile.domain.entity.CalendarEvent
import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import pt.rikmartins.clubemg.mobile.domain.usecase.WatchCase
import kotlinx.coroutines.flow.Flow

class EventsSupplier(private val eventRepository: EventRepository): WatchCase.Supplier<List<CalendarEvent>>() {
    override fun get(): Flow<List<CalendarEvent>> = eventRepository.events
}