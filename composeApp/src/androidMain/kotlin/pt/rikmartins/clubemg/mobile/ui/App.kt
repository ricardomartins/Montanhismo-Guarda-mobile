package pt.rikmartins.clubemg.mobile.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import pt.rikmartins.clubemg.mobile.ui.detail.EventDetailScreen
import pt.rikmartins.clubemg.mobile.ui.theme.AppTheme

private sealed interface AppDestination {

    @Serializable
    object Main : AppDestination

    @Serializable
    data class EventDetail(val eventId: String) : AppDestination
}

@Composable
fun App() {
    AppTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = AppDestination.Main,
            modifier = Modifier.fillMaxSize()
        ) {
            val navigateToDetails: (event: UiEventWithBookmark) -> Unit = { event ->
                navController.navigate(AppDestination.EventDetail(event.id))
            }

            composable<AppDestination.Main> { MainScreen(navigateToDetails) }

            composable<AppDestination.EventDetail> { backStackEntry ->
                EventDetailScreen(
                    eventId = backStackEntry.toRoute<AppDestination.EventDetail>().eventId,
                    navigateBack = navController::popBackStack
                )
            }
        }
    }
}