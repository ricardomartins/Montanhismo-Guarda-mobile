package pt.rikmartins.clubemg.mobile.domain.usecase.events

import kotlinx.datetime.TimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class GetCalendarTimeZone(private val gateway: Gateway) : UseCase.Supplier<TimeZone>() {

    override suspend fun execute(): TimeZone = gateway.getTimeZone()

    interface Gateway {
        suspend fun getTimeZone(): TimeZone
    }
}