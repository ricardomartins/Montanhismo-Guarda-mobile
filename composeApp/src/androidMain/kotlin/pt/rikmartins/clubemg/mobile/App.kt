package pt.rikmartins.clubemg.mobile

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import pt.rikmartins.clubemg.mobile.ui.EventDetailScreen
import pt.rikmartins.clubemg.mobile.ui.CalendarScreen
import pt.rikmartins.clubemg.mobile.ui.theme.AppTheme

@Serializable
object CalendarDestination

@Serializable
data class EventDetailDestination(val eventId: String)

@Composable
fun App() {
    AppTheme {
        Surface { // TODO: Investigate possibility of removing this Surface (thus having everything directly inside AppTheme)
            val navController = rememberNavController()
            val uriHandler = LocalUriHandler.current

            NavHost(navController = navController, startDestination = CalendarDestination) {
                composable<CalendarDestination> {
                    CalendarScreen(navigateToDetails = { event ->
//                        navController.navigate(EventDetailDestination(objectId))
                        uriHandler.openUri(event.url)
                    })
                }
                composable<EventDetailDestination> { backStackEntry ->
                    EventDetailScreen(
                        eventId = backStackEntry.toRoute<EventDetailDestination>().eventId,
                        navigateBack = navController::popBackStack
                    )
                }
            }
        }
    }
}
