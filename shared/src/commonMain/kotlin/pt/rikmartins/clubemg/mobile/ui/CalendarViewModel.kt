package pt.rikmartins.clubemg.mobile.ui

import pt.rikmartins.clubemg.mobile.domain.usecase.events.AccessSupplier
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventsSupplier
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.todayIn
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventDateRequester
import pt.rikmartins.clubemg.mobile.domain.usecase.events.TimeZoneSupplier
import pt.rikmartins.clubemg.mobile.ui.entity.WeekOfEvents
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CalendarViewModel(
    private val eventsSupplier: EventsSupplier,
    private val accessSupplier: AccessSupplier,
    private val eventDateRequester: EventDateRequester,
    private val timeZoneSupplier: TimeZoneSupplier,
): ViewModel() {
    @OptIn(ExperimentalTime::class)
    private val weekRangesFlow = MutableStateFlow(
        buildList<LocalDateRange> {
            Clock.System.now()
            LocalDateRange(Clock.System.todayIn(timeZoneSupplier()))
        }
    )




    val weeksOfEvents: StateFlow<List<WeekOfEvents>> = eventsSupplier()
        .stateIn(
            viewModelScope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun requestDateRange(dateRange: ClosedRange<LocalDate>) {
        viewModelScope.launch {
            eventDateRequester(dateRange.start)
            eventDateRequester(dateRange.endInclusive)
        }
    }
}
