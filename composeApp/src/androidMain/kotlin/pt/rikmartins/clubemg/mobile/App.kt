package pt.rikmartins.clubemg.mobile

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import pt.rikmartins.clubemg.mobile.ui.EventDetailScreen
import pt.rikmartins.clubemg.mobile.ui.CalendarScreen
import pt.rikmartins.clubemg.mobile.ui.theme.AppTheme

sealed interface AppDestination {

    @Serializable
    sealed class Main(
        @StringRes val labelResId: Int,
        @DrawableRes val iconResId: Int,
        @DrawableRes val iconSelectedResId: Int
    ) {

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    AppTheme {
        val navController = rememberNavController()
        val uriHandler = LocalUriHandler.current

        val scaffoldViewModel: ScaffoldViewModel = koinViewModel()
        val scaffoldState by scaffoldViewModel.scaffoldState.collectAsStateWithLifecycle()
        val showProgressIndicator by scaffoldViewModel.showProgressIndicator.collectAsStateWithLifecycle()

        val topAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        val bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()

        Scaffold(
            modifier = Modifier
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                .nestedScroll(bottomAppBarScrollBehavior.nestedScrollConnection),
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = scaffoldState.topBarTitle,
                        actions = scaffoldState.topBarActions,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        scrollBehavior = topAppBarScrollBehavior,
                    )
                    AnimatedVisibility(
                        visible = showProgressIndicator,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            floatingActionButton = scaffoldState.floatingActionButton,
            bottomBar = {
                BottomAppBar(scrollBehavior = bottomAppBarScrollBehavior) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    val selectedMain = AppDestination.Main.destinations.find { screen ->
                        currentDestination?.hierarchy?.any { it.route == screen::class.qualifiedName } == true
                    }
                    AppDestination.Main.destinations.forEach { screen ->
                        val selected = selectedMain == screen
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    painter = painterResource(
                                        id = if (selected) screen.iconSelectedResId else screen.iconResId
                                    ),
                                    contentDescription = stringResource(screen.labelResId)
                                )
                            },
                            label = { Text(stringResource(screen.labelResId)) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = AppDestination.Main.Calendar,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                composable<AppDestination.Main.Calendar> {
                    CalendarScreen(
                        scaffoldViewModel,
                        navigateToDetails = { event -> uriHandler.openUri(event.url) }
                    )
                }
                composable<AppDestination.Main.Bookmarks> {
                    // Placeholder
                }
                composable<AppDestination.Main.Settings> {
                    // Placeholder
                }
                composable<AppDestination.EventDetail> { backStackEntry ->
                    EventDetailScreen(
                        eventId = backStackEntry.toRoute<AppDestination.EventDetail>().eventId,
                        navigateBack = navController::popBackStack
                    )
                }
            }
        }
    }
}
