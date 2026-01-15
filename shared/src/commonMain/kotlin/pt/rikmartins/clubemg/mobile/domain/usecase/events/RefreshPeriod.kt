package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.datetime.LocalDateRange
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class RefreshPeriod(private val gateway: Gateway): UseCase.Consumer<LocalDateRange>() {

    override suspend fun execute(param: LocalDateRange) = gateway.refreshPeriod(param)

    interface Gateway {
        suspend fun refreshPeriod(period: LocalDateRange)
    }
}