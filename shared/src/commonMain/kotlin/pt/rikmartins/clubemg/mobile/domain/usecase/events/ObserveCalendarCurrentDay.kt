package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.todayIn
import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ObserveCalendarCurrentDay(
    private val repository: EventRepository,
    private val clock: Clock = Clock.System,
) : WatchCase.Supplier<LocalDate>() {

    override fun execute(): Flow<LocalDate> = flow {
        while (true) {
            emit(Unit)
            delay(5.minutes)
        }
    }
        .combine(repository.eventsTimezone) { _, timeZone -> clock.todayIn(timeZone) }
        .distinctUntilChanged()
}
