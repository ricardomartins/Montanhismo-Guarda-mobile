package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateRange
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class ConsiderRefreshingPeriod(
    private val eventProvider: EventProvider,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : UseCase.Consumer<LocalDateRange>(dispatcher) {

    override suspend fun execute(param: LocalDateRange) = eventProvider.considerRefreshingPeriod(param)

    interface EventProvider {
        suspend fun considerRefreshingPeriod(period: LocalDateRange)
    }
}