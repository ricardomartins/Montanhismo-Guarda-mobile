package pt.rikmartins.clubemg.mobile.ui

import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import pt.rikmartins.clubemg.mobile.domain.entity.SimplifiedEvent

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