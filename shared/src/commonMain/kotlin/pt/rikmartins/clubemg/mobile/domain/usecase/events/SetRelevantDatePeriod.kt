package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.datetime.LocalDateRange
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class SetRelevantDatePeriod(private val gateway: Gateway) : UseCase<LocalDateRange, Unit>() {

    override suspend fun execute(params: LocalDateRange): Unit = gateway.setRelevantDatePeriod(params)

    interface Gateway {
        suspend fun setRelevantDatePeriod(period: LocalDateRange)
    }
}