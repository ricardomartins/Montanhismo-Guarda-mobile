package pt.rikmartins.clubemg.mobile.ui

import pt.rikmartins.clubemg.mobile.domain.entity.CalendarEvent
import pt.rikmartins.clubemg.mobile.domain.usecase.events.AccessSupplier
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventsSupplier
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventDateRequester

class CalendarViewModel(
    private val eventsSupplier: EventsSupplier,
    private val accessSupplier: AccessSupplier,
    private val eventDateRequester: EventDateRequester,
): ViewModel() {
    val events: StateFlow<List<CalendarEvent>> = eventsSupplier()
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
