package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.datetime.LocalDateRange
import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class SetRelevantDatePeriod(private val eventRepository: EventRepository) : UseCase<LocalDateRange, Unit>() {

    override suspend fun execute(params: LocalDateRange): Unit = eventRepository.setRelevantDatePeriod(params)
}