package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class RefreshEvent(
    private val eventProvider: EventProvider,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): UseCase.Consumer<String>(dispatcher) {

    override suspend fun execute(param: String) = eventProvider.refreshEvent(param)

    interface EventProvider {
        suspend fun refreshEvent(eventId: String)
    }
}