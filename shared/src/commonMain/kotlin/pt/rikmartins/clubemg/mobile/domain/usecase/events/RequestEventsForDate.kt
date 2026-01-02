package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.datetime.LocalDate
import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class RequestEventsForDate(private val eventRepository: EventRepository): UseCase<LocalDate, Unit>() {

    override suspend fun execute(params: LocalDate): Unit = eventRepository.requestDate(date = params)
}