package pt.rikmartins.clubemg.mobile.ui

import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventWithBookmark
import pt.rikmartins.clubemg.mobile.domain.usecase.events.GetCalendarTimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveEvent
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveRefreshing
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RefreshEvent
import pt.rikmartins.clubemg.mobile.domain.usecase.events.SetBookmarkOfEventId
import pt.rikmartins.clubemg.mobile.domain.usecase.events.toLocalDate

class DetailViewModel(
    private val observeEvent: ObserveEvent,
    private val setBookmarkOfEventId: SetBookmarkOfEventId,
    private val refreshEvent: RefreshEvent,
    private val getCalendarTimeZone: GetCalendarTimeZone,
    observeRefreshing: ObserveRefreshing,
) : ViewModel() {

    private val eventId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val eventFlow = eventId.flatMapLatest {
        it?.let { observeEvent(it) } ?: flowOf(null)
    }

    private val timeZoneFlow = flowOf(Unit).map { getCalendarTimeZone() }

    @NativeCoroutinesState
    val event: StateFlow<UiEventWithBookmark?> = combine(
        eventFlow,
        timeZoneFlow
    ) { event, timeZone ->
        event?.let { DetailUiEventWithBookmark(it, timeZone) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val refreshingEventIds = observeRefreshing().map { it.singularEventIds }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setEventId(eventId: String) {
        this.eventId.value = eventId
        viewModelScope.launch { refreshEvent(eventId) }
    }

    fun setBookmarkOfEventTo(isBookmarked: Boolean) {
        val id = eventId.value ?: return
        viewModelScope.launch {
            setBookmarkOfEventId(id, isBookmarked)
        }
    }

    private data class DetailUiEventWithBookmark(
        override val calendarEvent: EventWithBookmark,
        override val range: LocalDateRange,
        override val isBookmarked: Boolean,
        override val preferredImageUrl: String? = null,
    ) : UiEventWithBookmark {

        constructor(
            calendarEvent: EventWithBookmark,
            timeZone: TimeZone,
        ) : this(
            calendarEvent = calendarEvent,
            range = with(calendarEvent) {
                startDate.toLocalDate(timeZone)..endDate.toLocalDate(timeZone)
            },
            isBookmarked = calendarEvent.isBookmarked,
        )

        override val id: String
            get() = calendarEvent.id
        override val title: String
            get() = calendarEvent.title
        override val url: String
            get() = calendarEvent.url
    }
}
