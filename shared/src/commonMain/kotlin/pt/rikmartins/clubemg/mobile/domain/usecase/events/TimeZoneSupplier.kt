package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.flow.Flow
import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import kotlinx.datetime.TimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase

class TimeZoneSupplier(private val repository: EventRepository) : WatchCase.Supplier<TimeZone>() {
    override fun get(): Flow<TimeZone> = repository.eventsTimezone
}