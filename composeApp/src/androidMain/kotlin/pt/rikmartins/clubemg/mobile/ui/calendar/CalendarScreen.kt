package pt.rikmartins.clubemg.mobile.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.toJavaLocalDate
import org.koin.androidx.compose.koinViewModel
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.thisWeeksMonday
import pt.rikmartins.clubemg.mobile.thisWeeksSunday
import pt.rikmartins.clubemg.mobile.ui.CalendarViewModel
import pt.rikmartins.clubemg.mobile.ui.ScaffoldViewModel
import pt.rikmartins.clubemg.mobile.ui.UiEventWithBookmark
import pt.rikmartins.clubemg.mobile.ui.WeekOfEvents
import pt.rikmartins.clubemg.mobile.ui.theme.LocalCustomColorsPalette
import java.time.format.DateTimeFormatter

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
    val currentWeekMonday = today?.thisWeeksMonday()
    val weeks = model.weeksOfEvents

    var isTodayFabVisible by remember { mutableStateOf(false) }

    LaunchedEffect(listState, currentWeekMonday) {
        snapshotFlow {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            val startVisibleDate = (visibleItems.firstOrNull()?.key as? Long)?.let { LocalDate.fromEpochDays(it) }
            val endVisibleDate = (visibleItems.lastOrNull()?.key as? Long)?.let { LocalDate.fromEpochDays(it) }

            startVisibleDate to endVisibleDate
        }
            .distinctUntilChanged()
            .collect { (startVisibleDate, endVisibleDate) ->
                if (startVisibleDate != null && endVisibleDate != null) {
                    val visibleDateRange = startVisibleDate..endVisibleDate
                    viewModel.notifyViewedDates(visibleDateRange)

                    isTodayFabVisible =
                        if (currentWeekMonday != null) currentWeekMonday !in visibleDateRange
                        else false
                }
            }
    }

    LaunchedEffect(Unit) {
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
                    visible = isTodayFabVisible,
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    FloatingActionButton(
                        onClick = {
                            currentWeekMonday?.let { todaysMonday ->
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
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
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

        StickyMonthHeadersOverlay(weeks, listState)
    }
}

private data class PillData(
    val date: LocalDate,
    val includeYear: Boolean,
    val yOffset: Int
) {
    val dateText: String = date.toJavaLocalDate().format(
        if (date.month == Month.JANUARY || includeYear) formatterWithYear else formatterMonthOnly
    )

    companion object {
        private val formatterWithYear = DateTimeFormatter.ofPattern("MMMM y")
        private val formatterMonthOnly = DateTimeFormatter.ofPattern("MMMM")
    }
}

@Composable
private fun StickyMonthHeadersOverlay(weeks: List<WeekOfEvents>, listState: LazyListState) {
    var showYearOfPills by remember { mutableStateOf(false) }

    LaunchedEffect(showYearOfPills) {
        if (showYearOfPills) {
            delay(1000)
            showYearOfPills = false
        }
    }

    val density = LocalDensity.current
    val stickyHeaderTopPadding = remember(density) { with(density) { 8.dp.roundToPx() } }
    val headerSpacing = remember(density) { with(density) { 8.dp.roundToPx() } }
    var headerHeight by remember { mutableIntStateOf(with(density) { 32.dp.roundToPx() }) }

    val pillsToDraw by remember(weeks) {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || weeks.isEmpty()) return@derivedStateOf emptyList()

            val monthTransitions = mutableListOf<PillData>()

            // 1. Check boundaries between currently visible items
            for (visibleItem in visibleItems) if (visibleItem.index > 0) {
                val previousWeekSunday = weeks.getOrNull(visibleItem.index - 1)?.monday?.thisWeeksSunday()
                val currentWeekSunday = weeks.getOrNull(visibleItem.index)?.monday?.thisWeeksSunday()

                if (previousWeekSunday != null && currentWeekSunday != null &&
                    (previousWeekSunday.month != currentWeekSunday.month || previousWeekSunday.year != currentWeekSunday.year)
                ) monthTransitions.add(
                    PillData(
                        date = currentWeekSunday,
                        includeYear = showYearOfPills,
                        yOffset = visibleItem.offset - headerHeight / 2
                    )
                )
            }

            // 2. Check boundary between the last visible item and the *first invisible* item
            val lastItem = visibleItems.last()
            if (lastItem.index < weeks.lastIndex) {
                val lastSunday = weeks.getOrNull(lastItem.index)?.monday?.thisWeeksSunday()
                val nextSunday = weeks.getOrNull(lastItem.index + 1)?.monday?.thisWeeksSunday()

                if (lastSunday != null && nextSunday != null &&
                    (lastSunday.month != nextSunday.month || lastSunday.year != nextSunday.year)
                ) {
                    val boundaryOffset = lastItem.offset + lastItem.size
                    monthTransitions.add(
                        PillData(
                            date = nextSunday,
                            includeYear = showYearOfPills,
                            yOffset = boundaryOffset - headerHeight / 2
                        )
                    )
                }
            }

            // Calculate Sticky Header
            val firstVisibleItem = visibleItems.first()
            val stickyHeaderDate = weeks.getOrNull(firstVisibleItem.index)?.monday?.thisWeeksSunday()

            val result = mutableListOf<PillData>()
            if (stickyHeaderDate != null) {
                // Find the first boundary that is transitioning to a DIFFERENT month
                val nextMonthHeader = monthTransitions.firstOrNull {
                    it.date.month != stickyHeaderDate.month || it.date.year != stickyHeaderDate.year
                }
                val nextHeaderOffset = nextMonthHeader?.yOffset ?: Int.MAX_VALUE
                val stickyHeaderOffset = minOf(stickyHeaderTopPadding, nextHeaderOffset - headerHeight - headerSpacing)

                // Only draw the sticky header if it's on screen
                if (stickyHeaderOffset > -headerHeight) result.add(
                    PillData(
                        date = stickyHeaderDate,
                        includeYear = showYearOfPills,
                        yOffset = stickyHeaderOffset
                    )
                )
            }

            // Add the boundaries, filtering out any that are strictly off-screen
            val viewportHeight = listState.layoutInfo.viewportSize.height
            monthTransitions.filterTo(result) { it.yOffset > -headerHeight && it.yOffset < viewportHeight }

            result
        }
    }

    for (monthHeader in pillsToDraw) key(monthHeader.date.toEpochDays()) {
        val (surfaceColor, onSurfaceColor) =
            LocalCustomColorsPalette.current.getSurfaceAndOnSurfaceOfDate(monthHeader.date)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, monthHeader.yOffset) }
                .onSizeChanged { size ->
                    headerHeight = size.height
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = monthHeader.dateText,
                color = onSurfaceColor,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
                    .background(surfaceColor, RoundedCornerShape(16.dp))
                    .clickable {
                        showYearOfPills = true
                    }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}
