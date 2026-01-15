package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.datetime.LocalDateRange
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class SetRelevantDatePeriod(private val gateway: Gateway) : UseCase.Consumer<LocalDateRange>() {

    override suspend fun execute(param: LocalDateRange) = gateway.setRelevantDatePeriod(param)

    interface Gateway {
        suspend fun setRelevantDatePeriod(period: LocalDateRange)
    }
}