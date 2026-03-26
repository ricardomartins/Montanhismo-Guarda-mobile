package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class GetCalendarTimeZone(
    private val gateway: Gateway,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) : UseCase.Supplier<TimeZone>(dispatcher) {

    override suspend fun execute(): TimeZone = gateway.getTimeZone()

    interface Gateway {
        suspend fun getTimeZone(): TimeZone
    }
}