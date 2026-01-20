package pt.rikmartins.clubemg.mobile.domain.usecase.events

import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase
import kotlinx.coroutines.flow.Flow

class ObserveRefreshing(private val gateway: Gateway): WatchCase.Supplier<RefreshState>() {

    override fun execute(): Flow<RefreshState> = gateway.refreshingDetail

    interface Gateway {
        val refreshingDetail: Flow<RefreshState>
    }
}