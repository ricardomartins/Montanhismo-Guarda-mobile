package pt.rikmartins.clubemg.mobile.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.ui.detail.EventDetailScreen
import pt.rikmartins.clubemg.mobile.ui.theme.AppTheme

sealed interface AppDestination {

    @Serializable
    sealed class Main(
        @field:StringRes val labelResId: Int,
        @field:DrawableRes val iconResId: Int,
        @field:DrawableRes val iconSelectedResId: Int
    ) : AppDestination {

        @Serializable
        object Root : Main(labelResId = 0, iconResId = 0, iconSelectedResId = 0)

        @Serializable
        object Calendar : Main(
            labelResId = R.string.calendar_navigation,
            iconResId = R.drawable.ic_bottom_nav_calendar,
            iconSelectedResId = R.drawable.ic_bottom_nav_calendar_selected
        )

        @Serializable
        object Bookmarks : Main(
            labelResId = R.string.bookmarks_navigation,
            iconResId = R.drawable.ic_bottom_nav_bookmarks,
            iconSelectedResId = R.drawable.ic_bottom_nav_bookmarks_selected
        )

        @Serializable
        object Settings : Main(
            labelResId = R.string.settings_navigation,
            iconResId = R.drawable.ic_bottom_nav_settings,
            iconSelectedResId = R.drawable.ic_bottom_nav_settings_selected
        )

        companion object {
            val destinations = listOf(
                Calendar,
                Bookmarks,
                Settings,
            )
        }
    }

    @Serializable
    data class EventDetail(val eventId: String) : AppDestination
}

@Composable
fun App() {
    AppTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = AppDestination.Main.Root,
            modifier = Modifier.fillMaxSize()
        ) {
            val navigateToDetails: (event: UiEventWithBookmark) -> Unit = { event ->
                navController.navigate(AppDestination.EventDetail(event.id))
            }

            composable<AppDestination.Main.Root> { MainScreen(navigateToDetails) }

            composable<AppDestination.EventDetail> { backStackEntry ->
                EventDetailScreen(
                    eventId = backStackEntry.toRoute<AppDestination.EventDetail>().eventId,
                    navigateBack = navController::popBackStack
                )
            }
        }
    }
}