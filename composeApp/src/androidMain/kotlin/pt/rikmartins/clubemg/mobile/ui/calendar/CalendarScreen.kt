package pt.rikmartins.clubemg.mobile.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.ui.ScaffoldViewModel
import pt.rikmartins.clubemg.mobile.thisWeeksMonday
import pt.rikmartins.clubemg.mobile.ui.CalendarViewModel
import pt.rikmartins.clubemg.mobile.ui.UiEventWithBookmark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    scaffoldViewModel: ScaffoldViewModel,
    navigateToDetails: (event: UiEventWithBookmark) -> Unit,
) {
    val viewModel: CalendarViewModel = koinViewModel()
    val model by viewModel.model.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()

    val today = model.today
    val todayMonday = today?.thisWeeksMonday()
    val weeks = model.weeksOfEvents

    var showFab by remember { mutableStateOf(false) }

    LaunchedEffect(listState, todayMonday) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }.collect { visibleInfo ->
            val firstVisibleDate = (visibleInfo.firstOrNull()?.key as? Long)
                ?.let { LocalDate.fromEpochDays(it) }
            val lastVisibleDate = (visibleInfo.lastOrNull()?.key as? Long)
                ?.let { LocalDate.fromEpochDays(it) }
            if (firstVisibleDate != null && lastVisibleDate != null)
                viewModel.notifyViewedDates(firstVisibleDate..lastVisibleDate)

            val todayMondayKey = todayMonday?.toEpochDays()
            if (todayMondayKey != null) {
                val isTodayVisible = visibleInfo.any { it.key == todayMondayKey }
                showFab = !isTodayVisible
            } else {
                showFab = false
            }
        }
    }

    scaffoldViewModel.updateScaffold(
        title = { Text(stringResource(R.string.title_calendar)) },
        actions = {
            IconButton(onClick = { viewModel.forceSync() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_sync),
                    contentDescription = stringResource(R.string.force_sync_action),
                )
            }
        },
        fab = {
            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                FloatingActionButton(
                    onClick = {
                        todayMonday?.let { todaysMonday ->
                            scope.launch {
                                val index = weeks.indexOfFirst { it.monday == todaysMonday }
                                if (index >= 0) listState.animateScrollToItem(index)
                            }
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_today),
                        contentDescription = stringResource(R.string.jump_to_today_action),
                    )
                }
            }
        },
    )

    LazyColumn(
        state = listState,
    ) {
        items(weeks, key = { it.monday.toEpochDays() }) { weekOfEvents ->
            Week(
                weekOfEvents = weekOfEvents,
                today = today,
                onEventClick = { navigateToDetails(it) },
                setImageSize = { ofEvent, withSize ->
                    viewModel.updateImageSize(
                        ofEvent = ofEvent,
                        withWidth = withSize.width,
                        andHeight = withSize.height,
                    )
                },
            )
            HorizontalDivider()
        }
    }
}
