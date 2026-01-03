package pt.rikmartins.clubemg.mobile.domain.usecase.events

import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateRange

class ObserveRefreshingRanges(
    private val eventRepository: EventRepository,
): WatchCase.Supplier<Set<LocalDateRange>>() {

    override fun execute(): Flow<Set<LocalDateRange>> = eventRepository.refreshingRanges
}