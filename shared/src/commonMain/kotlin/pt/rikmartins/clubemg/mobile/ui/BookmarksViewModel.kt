package pt.rikmartins.clubemg.mobile.ui

import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventWithBookmark
import pt.rikmartins.clubemg.mobile.domain.usecase.events.GetCalendarTimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllFavouriteEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveRefreshing
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RefreshEvent
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetBookmarkOfEventId
import pt.rikmartins.clubemg.mobile.domain.usecase.events.toLocalDate

class BookmarksViewModel(
    observeAllFavouriteEvents: ObserveAllFavouriteEvents,
    private val setBookmarkOfEventId: SetBookmarkOfEventId,
    private val getCalendarTimeZone: GetCalendarTimeZone,
    private val refreshEvent: RefreshEvent,
    observeRefreshing: ObserveRefreshing,
) : ViewModel() {

    private val eventImageOutputSize = MutableStateFlow<ImageSize?>(null)

    private val calendarTimeZoneFlow = flow { emit(getCalendarTimeZone()) }

    val selectedEvent = MutableStateFlow<UiEventWithBookmark?>(null)

    @OptIn(FlowPreview::class)
    val model: StateFlow<List<UiEventWithBookmark>> = combine(
        combine(
            observeAllFavouriteEvents(),
            calendarTimeZoneFlow,
        ) { bookmarked, timeZone -> bookmarked.map { BookmarksUiEventWithBookmark(it, timeZone) } },
        eventImageOutputSize.debounce(500),
    ) { events, imageSize ->
        events.map { event ->
            event.copy(
                preferredImageUrl = imageSize?.let {
                    event.calendarEvent.images
                        .sortedBy { it.fileSize }
                        .firstOrNull { it.width > imageSize.width && it.height > imageSize.height }
                        ?.url
                }
            ).also { event ->
                selectedEvent.update { selectedEvent ->
                    if (event.id == selectedEvent?.id) event else selectedEvent
                }
            }
        }
    }
        .map { events -> events.sortedBy { it.calendarEvent.startDate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val refreshing = observeRefreshing()

    val refreshingEventIds = refreshing.map { it.singularEventIds }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedEvent(event: UiEventWithBookmark) {
        viewModelScope.launch { refreshEvent(event.id) }
        selectedEvent.value = event
    }

    fun unsetSelectedEvent() {
        selectedEvent.value = null
    }

    fun unbookmarkEvent(eventId: String) {
        viewModelScope.launch { setBookmarkOfEventId(eventId, false) }
        selectedEvent.update { if (it?.id == eventId) it.asBookmarksEntity(false) else it }
    }

    fun bookmarkEvent(eventId: String) {
        viewModelScope.launch { setBookmarkOfEventId(eventId, true) }
        selectedEvent.update { if (it?.id == eventId) it.asBookmarksEntity(true) else it }
    }

    fun updateImageSize(width: Int, height: Int) = eventImageOutputSize.update { ImageSize(width, height) }

    private data class BookmarksUiEventWithBookmark(
        override val calendarEvent: EventWithBookmark,
        override val range: LocalDateRange,
        override val isBookmarked: Boolean,
        override val preferredImageUrl: String? = null,
    ) : UiEventWithBookmark {

        constructor(
            calendarEvent: EventWithBookmark,
            timeZone: TimeZone,
            preferredImageUrl: String? = null
        ) : this(
            calendarEvent = calendarEvent,
            range = with(calendarEvent) {
                startDate.toLocalDate(timeZone)..endDate.toLocalDate(timeZone)
            },
            isBookmarked = calendarEvent.isBookmarked,
            preferredImageUrl = preferredImageUrl,
        )

        override val id: String
            get() = calendarEvent.id
        override val title: String
            get() = calendarEvent.title
        override val url: String
            get() = calendarEvent.url
    }

    private fun UiEventWithBookmark.asBookmarksEntity(isBookmarked: Boolean? = null) = BookmarksUiEventWithBookmark(
        calendarEvent = calendarEvent,
        range = range,
        isBookmarked = isBookmarked ?: this.isBookmarked,
        preferredImageUrl = preferredImageUrl
    )

    private data class ImageSize(val width: Int, val height: Int)
}