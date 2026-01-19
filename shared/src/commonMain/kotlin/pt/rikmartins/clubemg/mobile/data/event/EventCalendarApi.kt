package pt.rikmartins.clubemg.mobile.data.event

import co.touchlab.kermit.Logger
import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.resources.Resource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import pt.rikmartins.clubemg.mobile.domain.usecase.events.CalendarEvent
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventImage
import kotlin.time.Instant

class EventCalendarApi(
    private val client: HttpClient,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EventRepository.EventSource {

    override suspend fun getEventsInRange(dateRange: LocalDateRange): List<CalendarEvent> = coroutineScope {
        val startDateAsParam = dateRange.start.atTime(0, 0).asEventDate()
        val endDateAsParam = dateRange.endInclusive.atTime(23, 59, 59, 999_999_999).asEventDate()

        val events = buildList {
            val headPage = client.get(
                EventsResource(startDate = startDateAsParam, endDate = endDateAsParam)
            ).body<EventResponse>()
            add(headPage)

            headPage.totalPages.takeIf { it > 1 }?.let { 2..it }
                ?.map {
                    async(defaultDispatcher) {
                        client.get(EventsResource(startDate = startDateAsParam, endDate = endDateAsParam, page = it))
                            .body<EventResponse>()
                    }
                }
                ?.awaitAll()
                ?.let { addAll(it) }
        }.flatMap { it.events }.distinctBy { it.id }

        events.map { event -> event.asApiCalendarEvent() }
    }

    override suspend fun getEventsById(eventsIds: Collection<String>): List<CalendarEvent> = supervisorScope {
        eventsIds.mapNotNull { it.toIntOrNull() }
            .map { eventId ->
                async {
                    try {
                        client.get(EventsResource.Id(id = eventId)).body<EventItem>()
                            .asApiCalendarEvent()
                    } catch (e: Exception) {
                        Logger.e("Failed to fetch or map event with ID: $eventId", e)
                        null
                    }
                }
            }
            .awaitAll()
            .filterNotNull()
    }

    override suspend fun getTimeZone(): TimeZone {
        TODO("Not yet implemented")
    }

    private fun EventItem.asApiCalendarEvent(): ApiCalendarEvent {
        val timeZone = TimeZone.of(timezone)

        return ApiCalendarEvent(
            id = id.toString(),
            creationDate = date.asLocalDateTime().toInstant(timeZone),
            modifiedDate = modified.asLocalDateTime().toInstant(timeZone),
            url = url,
            title = Ksoup.clean(title),
            startDate = startDate.asLocalDateTime().toInstant(timeZone),
            endDate = endDate.asLocalDateTime().toInstant(timeZone),
            enrollmentUrl = website,
            images = image?.run {
                buildList {
                    add(this@run.asEventImage(null))
                    sizes.forEach { (key, value) -> add(value.asEventImage(key)) }
                }
            }.orEmpty()
        )
    }

    private fun LocalDateTime.asEventDate(): String = format(EVENT_DATE_TIME_FORMAT)
    private fun String.asLocalDateTime(): LocalDateTime = LocalDateTime.parse(this, EVENT_DATE_TIME_FORMAT)

    private companion object {
        const val PAGE_SIZE = 10

        val EVENT_DATE_TIME_FORMAT = LocalDateTime.Format {
            date(LocalDate.Formats.ISO)
            char(' ')
            hour()
            char(':')
            minute()
            char(':')
            second()
        }
    }

    private data class ApiCalendarEvent(
        override val id: String,
        override val creationDate: Instant,
        override val modifiedDate: Instant,
        override val title: String,
        override val url: String,
        override val startDate: Instant,
        override val endDate: Instant,
        override val enrollmentUrl: String,
        override val images: List<ApiEventImage>,
    ) : CalendarEvent

    private data class ApiEventImage(
        override val id: String?,
        override val url: String,
        override val width: Int,
        override val height: Int,
        override val fileSize: Int,
    ) : EventImage

    @Serializable
    @Resource("/events")
    private data class EventsResource(
        @SerialName("start_date") val startDate: String? = null,
        @SerialName("end_date") val endDate: String? = null,
        @SerialName("per_page") val perPage: Int? = PAGE_SIZE,
        val page: Int? = null,
        @SerialName("include") val includeCsv: String? = null,
    ) {
        @Serializable
        @Resource("{id}")
        data class Id(
            val parent: EventsResource = EventsResource(perPage = null),
            val id: Int,
        )
    }

    @Serializable
    @Resource("/categories")
    private data class CategoriesResource(
        val page: Int? = null,
        @SerialName("per_page") val perPage: Int? = null,
        val search: String? = null,
        val exclude: List<Int>? = null,
        val include: List<Int>? = null,
        val order: String? = null,
        val orderby: String? = null,
        @SerialName("hide_empty") val hideEmpty: Boolean? = null,
        val parent: Int? = null,
        val post: Int? = null,
        val event: Int? = null,
        val slug: String? = null
    ) {
        @Serializable
        @Resource("{id}")
        data class Id(val parent: CategoriesResource = CategoriesResource(), val id: Int)
    }

    @Serializable
    @Resource("/tags")
    private data class TagsResource(
        val page: Int? = null,
        @SerialName("per_page") val perPage: Int? = null,
        val search: String? = null,
        val exclude: List<Int>? = null,
        val include: List<Int>? = null,
        val order: String? = null,
        val orderby: String? = null,
        @SerialName("hide_empty") val hideEmpty: Boolean? = null,
        val post: Int? = null,
        val event: Int? = null,
        val slug: String? = null
    ) {
        @Serializable
        @Resource("{id}")
        data class Id(val parent: TagsResource = TagsResource(), val id: Int)
    }

    @Serializable
    private data class EventItem(
        val id: Int, // The event ID
        val date: String, // The event creation date in the site's timezone
        val modified: String, // The event last modified date in the site's timezone
        val url: String, // The URL for the event page
        val title: String, // The event name
        @SerialName("start_date") val startDate: String, // The event start date in the site's timezone
        @SerialName("end_date") val endDate: String, // The event end date in the event or site time zone
        val timezone: String, // The event time zone string
        val website: String, // The cost details of the event
        @Serializable(with = ImageStructureItemBooleanSerializer::class)
        val image: ImageStructureItem?,
    )

    private interface ImageStructure {
        val width: Int // The image width
        val height: Int // The image height
        val fileSize: Int // The image size in bytes
        val url: String // The image URL
    }

    @Serializable
    private data class ImageStructureItem(
        val id: Int, // The image ID
        override val url: String, // The image URL
        override val width: Int, // The image width
        override val height: Int, // The image height
        @SerialName("filesize") override val fileSize: Int, // The image size in bytes
        val sizes: Map<String, ImageSizeItem>
    ) : ImageStructure

    @Serializable
    private data class ImageSizeItem(
        override val width: Int,
        override val height: Int,
        @SerialName("filesize") override val fileSize: Int,
        override val url: String
    ) : ImageStructure

    @Serializable
    private data class EventResponse(
        val events: List<EventItem>,
        val total: Int, // Total events in the response
        @SerialName("total_pages") val totalPages: Int,
        @SerialName("rest_url") val restUrl: String,
        @SerialName("next_rest_url") val nextRestUrl: String? = null,
        @SerialName("previous_rest_url") val previousRestUrl: String? = null,
    )

    private fun ImageStructure.asEventImage(id: String?): ApiEventImage = ApiEventImage(
        id = id,
        url = url,
        width = width,
        height = height,
        fileSize = fileSize,
    )

    private class ImageStructureItemBooleanSerializer : KSerializer<ImageStructureItem?> {
        private val delegateSerializer = ImageStructureItem.serializer()
        override val descriptor: SerialDescriptor = delegateSerializer.descriptor

        override fun serialize(encoder: Encoder, value: ImageStructureItem?) {
            value?.also { encoder.encodeSerializableValue(delegateSerializer, value) }
                ?: encoder.encodeBoolean(false)
        }

        override fun deserialize(decoder: Decoder): ImageStructureItem? {
            val jsonInput = (decoder as? JsonDecoder)?.decodeJsonElement()
                ?: throw IllegalStateException("This serializer can only be used with JSON")

            if (jsonInput is JsonNull || (jsonInput is JsonPrimitive && jsonInput.booleanOrNull == false)) {
                return null
            }

            return decoder.json.decodeFromJsonElement(delegateSerializer, jsonInput.jsonObject)
        }
    }
}