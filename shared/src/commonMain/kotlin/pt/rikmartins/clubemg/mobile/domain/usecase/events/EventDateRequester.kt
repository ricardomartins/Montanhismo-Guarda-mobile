package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.datetime.LocalDate
import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import pt.rikmartins.clubemg.mobile.domain.usecase.UseCase
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class EventDateRequester(private val eventRepository: EventRepository): UseCase.Consumer<LocalDate>() {
    override suspend fun accept(request: LocalDate) = eventRepository.requestDate(date = request)
}