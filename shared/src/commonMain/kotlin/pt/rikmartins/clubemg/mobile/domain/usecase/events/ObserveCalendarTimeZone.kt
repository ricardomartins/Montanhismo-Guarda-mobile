package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.flow.Flow
import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import kotlinx.datetime.TimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase

class ObserveCalendarTimeZone(private val repository: EventRepository) : WatchCase.Supplier<TimeZone>() {

    override fun execute(): Flow<TimeZone> = repository.eventsTimezone
}