package pt.rikmartins.clubemg.mobile.domain.usecase.events

import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase
import kotlinx.coroutines.flow.Flow

class ObserveRefreshing(private val gateway: Gateway): WatchCase.Supplier<Boolean>() {

    override fun execute(): Flow<Boolean> = gateway.refreshingDetail

    interface Gateway {
        val refreshingDetail: Flow<Boolean>
    }
}