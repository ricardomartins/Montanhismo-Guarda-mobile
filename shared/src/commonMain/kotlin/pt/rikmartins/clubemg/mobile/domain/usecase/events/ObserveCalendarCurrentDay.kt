package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import pt.rikmartins.clubemg.mobile.domain.usecase.base.WatchCase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ObserveCalendarCurrentDay(
    private val gateway: Gateway,
    private val clock: Clock = Clock.System,
) : WatchCase.Supplier<LocalDate>() {

    override fun execute(): Flow<LocalDate> = flow {
        val timeZone = gateway.getTimeZone()

        while (true) {
            emit(clock.todayIn(timeZone))
            delay(5.minutes)
        }
    }
        .distinctUntilChanged()

    interface Gateway {

        suspend fun getTimeZone(): TimeZone
    }
}
