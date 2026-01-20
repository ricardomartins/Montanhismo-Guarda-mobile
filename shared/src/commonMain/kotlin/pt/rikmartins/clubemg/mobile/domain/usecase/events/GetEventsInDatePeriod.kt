package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.datetime.LocalDateRange
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class GetEventsInDatePeriod(private val eventProvider: EventProvider) : UseCase.Consumer<LocalDateRange>() {

    override suspend fun execute(param: LocalDateRange) = eventProvider.getEventsBookmarkAwareIn(param)

    interface EventProvider {
        suspend fun getEventsBookmarkAwareIn(period: LocalDateRange)
    }
}