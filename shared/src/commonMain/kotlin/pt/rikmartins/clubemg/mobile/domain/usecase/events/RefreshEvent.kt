package pt.rikmartins.clubemg.mobile.domain.usecase.events

import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class RefreshEvent(private val eventProvider: EventProvider): UseCase.Consumer<String>() {

    override suspend fun execute(param: String) = eventProvider.refreshEvent(param)

    interface EventProvider {
        suspend fun refreshEvent(eventId: String)
    }
}