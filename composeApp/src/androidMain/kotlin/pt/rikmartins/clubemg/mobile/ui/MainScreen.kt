package pt.rikmartins.clubemg.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.koin.androidx.compose.koinViewModel
import pt.rikmartins.clubemg.mobile.ui.bookmarks.BookmarksScreen
import pt.rikmartins.clubemg.mobile.ui.calendar.CalendarScreen
import pt.rikmartins.clubemg.mobile.ui.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navigateToDetails: (event: UiEventWithBookmark) -> Unit) {

    val navController = rememberNavController()

    val scaffoldViewModel: ScaffoldViewModel = koinViewModel()
    val scaffoldState by scaffoldViewModel.scaffoldState.collectAsStateWithLifecycle()
    val showProgressIndicator by scaffoldViewModel.showProgressIndicator.collectAsStateWithLifecycle()

    val topAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val bottomAppBarScrollBehavior = BottomAppBarDefaults.exitAlwaysScrollBehavior()

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier
            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
            .nestedScroll(bottomAppBarScrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopAppBar(
                    title = scaffoldState.topBarTitle,
                    actions = scaffoldState.topBarActions,
                    navigationIcon = scaffoldState.navigationIcon,
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = scaffoldState.floatingActionButton,
        bottomBar = {
            BottomAppBar(scrollBehavior = bottomAppBarScrollBehavior) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val selectedMain = currentDestination?.run {
                    when {
                        hasRoute<AppDestination.Main.Calendar>() -> AppDestination.Main.Calendar
                        hasRoute<AppDestination.Main.Bookmarks>() -> AppDestination.Main.Bookmarks
                        hasRoute<AppDestination.Main.Settings>() -> AppDestination.Main.Settings
                        else -> null
                    }
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
                    scaffoldViewModel = scaffoldViewModel,
                    navigateToDetails = navigateToDetails,
                )
            }
            composable<AppDestination.Main.Bookmarks> {
                BookmarksScreen(
                    scaffoldViewModel = scaffoldViewModel,
                    navigateToDetails = navigateToDetails,
                    snackbarHostState = snackbarHostState
                )
            }
            composable<AppDestination.Main.Settings> {
                SettingsScreen(
                    scaffoldViewModel = scaffoldViewModel,
                )
            }
        }
    }
}