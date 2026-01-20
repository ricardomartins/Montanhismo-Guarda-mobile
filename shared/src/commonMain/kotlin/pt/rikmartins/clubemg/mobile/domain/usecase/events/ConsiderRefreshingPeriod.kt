package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.datetime.LocalDateRange
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class ConsiderRefreshingPeriod(private val eventProvider: EventProvider) : UseCase.Consumer<LocalDateRange>() {

    override suspend fun execute(param: LocalDateRange) = eventProvider.considerRefreshingPeriod(param)

    interface EventProvider {
        suspend fun considerRefreshingPeriod(period: LocalDateRange)
    }
}