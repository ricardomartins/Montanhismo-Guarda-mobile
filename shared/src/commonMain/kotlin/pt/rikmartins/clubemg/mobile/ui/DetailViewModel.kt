package pt.rikmartins.clubemg.mobile.ui

import com.rickclephas.kmp.observableviewmodel.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class DetailViewModel(
//    private val observeEvent: ObserveEvent,
) : ViewModel() {

    private val eventId = MutableStateFlow<String?>(null)

//    @OptIn(ExperimentalCoroutinesApi::class)
//    @NativeCoroutinesState
//    val event: StateFlow<SimplifiedEvent?> = eventId.flatMapLatest {
//        it?.let { observeEvent(it) } ?: flowOf(null)
//    }
//        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setEventId(eventId: String) {
        this.eventId.value = eventId
    }
}