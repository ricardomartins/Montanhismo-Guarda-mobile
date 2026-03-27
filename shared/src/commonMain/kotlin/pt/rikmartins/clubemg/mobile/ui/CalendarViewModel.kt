package pt.rikmartins.clubemg.mobile.ui

import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import pt.rikmartins.clubemg.mobile.domain.usecase.events.GetCalendarTimeZone
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveAllEvents
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ConsiderRefreshingPeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventWithBookmark
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveCalendarCurrentDay
import pt.rikmartins.clubemg.mobile.domain.usecase.events.RefreshPeriod
import pt.rikmartins.clubemg.mobile.domain.usecase.events.toLocalDate
import pt.rikmartins.clubemg.mobile.ui.WeekUtils.getMondaysInRange
import kotlin.math.abs
import kotlin.math.ln
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, FlowPreview::class)
class CalendarViewModel(
    private val considerRefreshingPeriod: ConsiderRefreshingPeriod,
    observeCalendarCurrentDay: ObserveCalendarCurrentDay,
    observeAllEvents: ObserveAllEvents,
    private val getCalendarTimeZone: GetCalendarTimeZone,
    private val refreshPeriod: RefreshPeriod,
) : ViewModel() {

    private val visibleDates = MutableStateFlow<LocalDateRange?>(null)
    private val filteredVisibleDates = visibleDates.filterNotNull().sample(500)
    private val eventImageUrlMapFlow = MutableStateFlow<Map<String, String?>>(emptyMap())

    init {
        viewModelScope.launch {
            filteredVisibleDates.collect {
                considerRefreshingPeriod(
                    it.start.minus(1, DateTimeUnit.MONTH)..it.endInclusive.plus(3, DateTimeUnit.MONTH)
                )
            }
        }
    }

    private val calendarCurrentDay = observeCalendarCurrentDay()

    private val datesThatWereEverVisible = filteredVisibleDates.scan(LocalDateRange.EMPTY) { acc, value ->
        if (acc == LocalDateRange.EMPTY) value
        else minOf(acc.start, value.start)..maxOf(acc.endInclusive, value.endInclusive)
    }
        .filter { it != LocalDateRange.EMPTY }
        .distinctUntilChanged { old, new -> old.start == new.start && old.endInclusive == new.endInclusive }

    private val calendarTimeZoneFlow = flow { emit(getCalendarTimeZone()) }

    val model = combine(
        combine(datesThatWereEverVisible, calendarCurrentDay) { weekLimits, currentDay ->
            val newStart = maxOf(
                weekLimits.start.minus(6, DateTimeUnit.MONTH),
                currentDay.minus(2, DateTimeUnit.YEAR)
            )
            val newEnd = minOf(
                weekLimits.endInclusive.plus(6, DateTimeUnit.MONTH),
                currentDay.plus(2, DateTimeUnit.YEAR)
            )

            newStart..newEnd to currentDay
        },
        observeAllEvents(),
        eventImageUrlMapFlow,
        calendarTimeZoneFlow,
    ) { (weekLimits, currentDay), events, eventImageUrlMap, timeZone ->
        val mondays = getMondaysInRange(weekLimits)
        val uiEventWithBookmarks = events.map {
            val imageUrl =
                if (eventImageUrlMap.contains(it.id)) eventImageUrlMap[it.id]
                else IMAGE_URL_SIGNAL_WAITING

            CalendarUiEventWithBookmark(it, timeZone, imageUrl)
        }

        selectedEvent.value?.id?.let { selectedEventId ->
            uiEventWithBookmarks.find { it.id == selectedEventId }?.let { selectedEvent.value = it }
        }

        buildMap {
            uiEventWithBookmarks.forEach { simplifiedEvent ->
                val mondayIter = mondays.iterator()
                while (mondayIter.hasNext()) {
                    val currentMonday = mondayIter.next()
                    if (simplifiedEvent.range.endInclusive < currentMonday) break

                    val currentSunday = currentMonday.plus(6, DateTimeUnit.DAY)
                    if (simplifiedEvent.range.start > currentSunday) continue

                    getOrPut(currentMonday) { mutableListOf() }.add(simplifiedEvent)
                }
            }
            mondays.forEach { getOrPut(it) { mutableListOf() } }
        }
            .map { (monday, events) -> WeekOfEvents(monday, events) }
            .sortedBy { it.monday }
            .let { Model(it, currentDay) }
    }.onStart {
        emitAll(
            calendarCurrentDay
                .map { currentDay ->
                    Model(
                        weeksOfEvents = getMondaysInRange(currentDay..currentDay.plus(30, DateTimeUnit.DAY))
                            .map { WeekOfEvents(it, emptyList()) },
                        today = currentDay,
                    )
                }
                .take(1)
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, Model(emptyList(), null))

    val selectedEvent = MutableStateFlow<UiEventWithBookmark?>(null)

    fun notifyViewedDates(dateRange: LocalDateRange) {
        visibleDates.value = dateRange
    }

    fun forceSync() {
        viewModelScope.launch {
            visibleDates.value?.let {
                refreshPeriod(
                    it.start.minus(1, DateTimeUnit.MONTH)..it.endInclusive.plus(1, DateTimeUnit.MONTH)
                )
            }
        }
    }

    fun updateImageSize(ofEvent: UiEventWithBookmark, withWidth: Int, andHeight: Int) =
        eventImageUrlMapFlow.update { mappings ->
            if (mappings[ofEvent.id] != null) return@update mappings

            val images = ofEvent.calendarEvent.images.takeIf { it.isNotEmpty() }
                ?: return@update mappings + (ofEvent.id to null)

            val sortedImages = images.sortedBy { it.fileSize }

            val originalImgAspectRatio = with(sortedImages.last()) { width.toFloat() / height.toFloat() }
            val targetAspectRatio = withWidth.toFloat() / andHeight.toFloat()

            val lnOfOriginal = ln(originalImgAspectRatio)
            val lnOfTarget = ln(targetAspectRatio)
            val absoluteDifferenceOfLns = abs(lnOfOriginal - lnOfTarget)
            if (absoluteDifferenceOfLns > ORIGINAL_TO_TARGET_ASPECT_RATIO_ACCEPTANCE)
                return@update mappings + (ofEvent.id to null)

            val selectedImage = sortedImages.firstOrNull { it.width > withWidth && it.height > andHeight }?.url
            return@update mappings + (ofEvent.id to selectedImage)
        }

    private data class CalendarUiEventWithBookmark(
        override val calendarEvent: EventWithBookmark,
        override val range: LocalDateRange,
        override val isBookmarked: Boolean,
        override val preferredImageUrl: String?,
    ) : UiEventWithBookmark {

        constructor(
            calendarEvent: EventWithBookmark,
            timeZone: TimeZone,
            preferredImageUrl: String?,
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

    companion object {
        /**
         * To signal that measures from the view are still missing, before an actual image url is provided.
         */
        const val IMAGE_URL_SIGNAL_WAITING = "Waiting"

        private const val ORIGINAL_TO_TARGET_ASPECT_RATIO_ACCEPTANCE = 1.75f
    }
}
