package pt.rikmartins.clubemg.mobile.domain.usecase.events

import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class RefreshCache(private val eventRepository: EventRepository): UseCase<Unit, Unit>() {

    override suspend fun execute(params: Unit): Unit = eventRepository.refreshCache()
}