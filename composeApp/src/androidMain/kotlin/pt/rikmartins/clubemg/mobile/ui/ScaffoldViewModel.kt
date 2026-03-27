package pt.rikmartins.clubemg.mobile.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.stateIn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import pt.rikmartins.clubemg.mobile.domain.usecase.events.ObserveRefreshing
import kotlin.time.Duration.Companion.milliseconds

data class ScaffoldConfig(
    val topBarTitle: @Composable () -> Unit = {},
    val topBarActions: @Composable RowScope.() -> Unit = {},
    val navigationIcon: @Composable () -> Unit = {},
    val floatingActionButton: @Composable () -> Unit = {},
)

class ScaffoldViewModel(observeRefreshing: ObserveRefreshing) : ViewModel() {
    private val _scaffoldConfig = MutableStateFlow(ScaffoldConfig())
    val scaffoldState = _scaffoldConfig.asStateFlow()

    @OptIn(FlowPreview::class)
    val showProgressIndicator = observeRefreshing().map { it.dateRanges.isNotEmpty() }.distinctUntilChanged()
        .debounce(200.milliseconds)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun updateScaffold(
        title: (@Composable () -> Unit),
        actions: (@Composable RowScope.() -> Unit) = {},
        navigationIcon: (@Composable () -> Unit) = {},
        fab: (@Composable () -> Unit) = {},
    ) {
        _scaffoldConfig.value = ScaffoldConfig(
            topBarTitle = title,
            topBarActions = actions,
            navigationIcon = navigationIcon,
            floatingActionButton = fab,
        )
    }
}
