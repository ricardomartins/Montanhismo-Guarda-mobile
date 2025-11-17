package pt.rikmartins.clubemg.mobile

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import pt.rikmartins.clubemg.mobile.ui.EventDetailScreen
import pt.rikmartins.clubemg.mobile.ui.CalendarScreen

@Serializable
object CalendarDestination

@Serializable
data class EventDetailDestination(val objectId: Int)

@Composable
fun App() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        Surface {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = CalendarDestination) {
                composable<CalendarDestination> {
                    CalendarScreen(navigateToDetails = { objectId ->
                        navController.navigate(EventDetailDestination(objectId))
                    })
                }
                composable<EventDetailDestination> { backStackEntry ->
                    EventDetailScreen(
                        objectId = backStackEntry.toRoute<EventDetailDestination>().objectId,
                        navigateBack = navController::popBackStack
                    )
                }
            }
        }
    }
}
