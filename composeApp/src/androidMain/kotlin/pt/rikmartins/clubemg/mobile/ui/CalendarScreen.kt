package pt.rikmartins.clubemg.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.domain.usecase.events.CalendarEvent
import pt.rikmartins.clubemg.mobile.thisWeeksMonday

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navigateToDetails: (event: CalendarEvent) -> Unit) {
    val viewModel: CalendarViewModel = koinViewModel()
    val model by viewModel.model.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val scope = rememberCoroutineScope()

    val today = model.today
    val todayMonday = today?.thisWeeksMonday()
    val weeks = model.weeksOfEvents

    var showFab by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var selectedEvent by remember { mutableStateOf<SimplifiedEvent?>(null) }

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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.title_calendar)) },
                    actions = {
                        IconButton(onClick = { viewModel.forceSync() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.sync_24),
                                contentDescription = stringResource(R.string.force_sync),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    scrollBehavior = scrollBehavior,
                )
                AnimatedVisibility(
                    visible = model.isRefreshing,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        floatingActionButton = {
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
                        painter = painterResource(id = R.drawable.today_24),
                        contentDescription = stringResource(R.string.jump_to_today),
                    )
                }
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = listState,
        ) {
            items(weeks, key = { it.monday.toEpochDays() }) { weekOfEvents ->
                Week(weekOfEvents, today) { selectedEvent = it }
                HorizontalDivider(thickness = Dp.Hairline)
            }
        }
        if (selectedEvent != null) EventActionsDialog(
            event = selectedEvent!!,
            navigateToDetails = {
                navigateToDetails(it)
                selectedEvent = null
            },
        ) { selectedEvent = null }
    }
}
