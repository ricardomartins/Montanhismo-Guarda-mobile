package pt.rikmartins.clubemg.mobile

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
        Surface {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = CalendarDestination) {
                composable<CalendarDestination> {
                    CalendarScreen(navigateToDetails = { objectId ->
//                        navController.navigate(EventDetailDestination(objectId))
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
