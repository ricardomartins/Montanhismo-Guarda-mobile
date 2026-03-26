package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateRange
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class RefreshPeriod(
    private val gateway: Gateway,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
): UseCase.Consumer<LocalDateRange>(dispatcher) {

    override suspend fun execute(param: LocalDateRange) = gateway.refreshPeriod(param)

    interface Gateway {
        suspend fun refreshPeriod(period: LocalDateRange)
    }
}