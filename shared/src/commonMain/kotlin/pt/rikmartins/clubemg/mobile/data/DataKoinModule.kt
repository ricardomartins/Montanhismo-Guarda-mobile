package pt.rikmartins.clubemg.mobile.data

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger as KtorLogger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.http.ContentType
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.binds
import org.koin.dsl.module
import pt.rikmartins.clubemg.mobile.data.service.event.EventCalendarApi
import pt.rikmartins.clubemg.mobile.data.storage.DataBaseEventStorage
import pt.rikmartins.clubemg.mobile.domain.usecase.events.GetCalendarTimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarCurrentDay
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveRefreshingRanges
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RefreshPeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetBookmarkOfEventId
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetRelevantDatePeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SynchronizeFavouriteEvents

internal val dataModule = module {
    single {
        val json = Json { ignoreUnknownKeys = true }
        HttpClient {
            install(ContentNegotiation) { json(json, contentType = ContentType.Any) }
            install(Resources)

            // Add the Logging plugin
            install(Logging) {
                logger = object : KtorLogger {
                    override fun log(message: String) = Logger.v("HttpClient") { message }
                }
                level = LogLevel.ALL
            }

            defaultRequest {
                url { takeFrom("https://www.montanhismo-guarda.pt/portal/wp-json/tribe/events/v1/") }
            }
        }
    }

    single<EventRepository.EventSource> { EventCalendarApi(get()) }
    single<EventRepository.EventStorage> { DataBaseEventStorage(get()) }
    single { EventRepository(get(), get()) } binds arrayOf(
        ObserveAllEvents.EventsProvider::class,
        ObserveCalendarCurrentDay.Gateway::class,
        GetCalendarTimeZone.Gateway::class,
        ObserveRefreshingRanges.Gateway::class,
        RefreshPeriod.Gateway::class,
        SetRelevantDatePeriod.Gateway::class,
        SynchronizeFavouriteEvents.EventsProvider::class,
    )

    singleOf(::BookmarkRepository) binds arrayOf(
        SynchronizeFavouriteEvents.BookmarkProvider::class,
        SetBookmarkOfEventId.BookmarkProvider::class,
        ObserveAllEvents.BookmarkProvider::class,
    )
}
