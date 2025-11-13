package pt.rikmartins.clubemg.mobile.domain.usecase.events

import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase
import kotlinx.datetime.TimeZone

class TimeZoneSupplier(private val repository: EventRepository) : UseCase.Supplier<TimeZone>() {
    override suspend fun get(): TimeZone = repository.eventsTimezone
}