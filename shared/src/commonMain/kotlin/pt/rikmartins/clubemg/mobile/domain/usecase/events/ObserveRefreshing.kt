package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase
import kotlinx.coroutines.flow.Flow

class ObserveRefreshing(
    private val gateway: Gateway,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): WatchCase.Supplier<RefreshState>(dispatcher) {

    override fun execute(): Flow<RefreshState> = gateway.refreshingDetail

    interface Gateway {
        val refreshingDetail: Flow<RefreshState>
    }
}