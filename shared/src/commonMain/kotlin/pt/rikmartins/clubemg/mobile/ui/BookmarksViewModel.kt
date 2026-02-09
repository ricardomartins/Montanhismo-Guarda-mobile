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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import pt.rikmartins.clubemg.mobile.addAllWhen
import pt.rikmartins.clubemg.mobile.domain.usecase.events.GetCalendarTimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllFavouriteEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetBookmarkOfEventId

class BookmarksViewModel(
    observeAllFavouriteEvents: ObserveAllFavouriteEvents,
    private val setBookmarkOfEventId: SetBookmarkOfEventId,
    private val getCalendarTimeZone: GetCalendarTimeZone,
) : ViewModel() {

    private val recentlyUnbookmarkedEvents = MutableStateFlow<Collection<UiEventBookmarkWithEvent>>(emptySet())

    private val eventImageOutputSize = MutableStateFlow<ImageSize?>(null)

    private val calendarTimeZoneFlow = flow { emit(getCalendarTimeZone()) }

    @OptIn(FlowPreview::class)
    val model: StateFlow<List<UiEventBookmarkWithEvent>> = combine(
        combine(
            observeAllFavouriteEvents(),
            recentlyUnbookmarkedEvents
        ) { bookmarked, unbookmarked ->
            buildSet {
                addAll(bookmarked)
                addAllWhen(unbookmarked) { event -> bookmarked.none { it.id == event.id } }
            }
        },
        eventImageOutputSize,
        calendarTimeZoneFlow
    ) { events, imageSize, timeZone ->
        events.map { event ->
            UiEventBookmarkWithEvent(
                id = event.id,
                isBookmarked = event.isBookmarked,
                event = event.event,
                imageUrl = imageSize?.let {
                    event.event?.images?.sortedBy { it.fileSize }
                        ?.firstOrNull { it.width > imageSize.width && it.height > imageSize.height }
                        ?.url
                },
                timeZone = timeZone,
            )
        }
    }
        .debounce(200)
        .map { events -> events.sortedBy { it.event?.startDate } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unbookmarkEvent(event: UiEventBookmarkWithEvent) {
        recentlyUnbookmarkedEvents.update { unbookmarked ->
            if (unbookmarked.none { it.id == event.id }) unbookmarked + event.copy(isBookmarked = false)
            else unbookmarked
        }
        viewModelScope.launch { setBookmarkOfEventId(event.id, false) }
    }

    fun bookmarkEvent(event: UiEventBookmarkWithEvent) {
        recentlyUnbookmarkedEvents.update { unbookmarked ->
            unbookmarked.find { it.id == event.id }?.let { unbookmarked - it } ?: unbookmarked
        }
        viewModelScope.launch { setBookmarkOfEventId(event.id, true) }
    }

    fun updateImageSize(width: Float, height: Float) = eventImageOutputSize.update { ImageSize(width, height) }

    private data class ImageSize(val width: Float, val height: Float)
}