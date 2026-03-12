package pt.rikmartins.clubemg.mobile.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import com.rickclephas.kmp.observableviewmodel.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScaffoldConfig(
    val topBarTitle: @Composable () -> Unit = {},
    val topBarActions: @Composable RowScope.() -> Unit = {},
    val floatingActionButton: @Composable () -> Unit = {},
)

class ScaffoldViewModel : ViewModel() {
    private val _scaffoldConfig = MutableStateFlow(ScaffoldConfig())
    val scaffoldState = _scaffoldConfig.asStateFlow()

    private val _showProgressIndicator = MutableStateFlow(false)
    val showProgressIndicator = _showProgressIndicator.asStateFlow()

    fun updateScaffold(
        title: (@Composable () -> Unit),
        actions: (@Composable RowScope.() -> Unit) = {},
        fab: (@Composable () -> Unit) = {},
    ) {
        _scaffoldConfig.value = ScaffoldConfig(
            topBarTitle = title,
            topBarActions = actions,
            floatingActionButton = fab,
        )
    }

    fun showProgress(show: Boolean) {
        _showProgressIndicator.value = show
    }
}