package pt.rikmartins.clubemg.mobile.domain.usecase.events

import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class AccessSupplier(private val eventRepository: EventRepository) : WatchCase.Supplier<Boolean>() {

    override fun execute(): Flow<Boolean> =
        combine(eventRepository.localAccess, eventRepository.remoteAccess) { local, remote -> local || remote }
}