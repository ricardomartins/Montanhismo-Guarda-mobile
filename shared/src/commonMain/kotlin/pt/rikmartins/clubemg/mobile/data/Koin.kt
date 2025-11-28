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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import pt.rikmartins.clubemg.mobile.data.service.event.EventCalendarApi
import pt.rikmartins.clubemg.mobile.domain.entity.CalendarEvent
import pt.rikmartins.clubemg.mobile.domain.gateway.EventRepository

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

    single<EventRepositoryImpl.EventSource> { EventCalendarApi(get()) }
    single<EventRepositoryImpl.EventStorage> { // FIXME
        object : EventRepositoryImpl.EventStorage {
            override fun getAllEvents(): Flow<List<CalendarEvent>> = flowOf(emptyList())

            override suspend fun saveEvents(events: List<CalendarEvent>) {

            }

            override suspend fun deleteEvents(events: List<CalendarEvent>) {

            }

            override val isAccessing: Flow<Boolean>
                get() = flowOf(false)
        }
    }
    single<EventRepository> { EventRepositoryImpl(get(), get()) }
}
