package pt.rikmartins.clubemg.mobile.domain.usecase.events

import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateRange

class ObserveRefreshingRanges(private val gateway: Gateway): WatchCase.Supplier<Set<LocalDateRange>>() {

    override fun execute(): Flow<Set<LocalDateRange>> = gateway.refreshingRanges

    interface Gateway {
        val refreshingRanges: Flow<Set<LocalDateRange>>
    }
}