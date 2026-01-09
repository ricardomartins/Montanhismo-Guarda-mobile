package pt.rikmartins.clubemg.mobile.domain.usecase.events

import pt.rikmartins.clubemg.mobile.domain.usecase.base.UseCase

class RefreshCache(private val gateway: Gateway): UseCase<Unit, Unit>() {

    override suspend fun execute(params: Unit): Unit = gateway.refreshCache()

    interface Gateway {
        suspend fun refreshCache()
    }
}