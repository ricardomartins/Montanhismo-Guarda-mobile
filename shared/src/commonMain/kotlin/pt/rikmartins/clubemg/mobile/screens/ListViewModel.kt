package pt.rikmartins.clubemg.mobile.screens

import pt.rikmartins.clubemg.mobile.data.MuseumObject
import pt.rikmartins.clubemg.mobile.data.MuseumRepository
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow

class ListViewModel(museumRepository: MuseumRepository) : ViewModel() {
    @NativeCoroutinesState
    val objects: StateFlow<List<MuseumObject>> =
        museumRepository.getObjects()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
