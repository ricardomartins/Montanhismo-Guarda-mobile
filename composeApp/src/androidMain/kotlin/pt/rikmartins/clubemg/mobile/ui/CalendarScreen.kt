package pt.rikmartins.clubemg.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.minus
import kotlinx.datetime.toJavaLocalDate
import pt.rikmartins.clubemg.mobile.data.MuseumObject
import org.koin.androidx.compose.koinViewModel
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.ui.theme.CustomColorsPalette
import pt.rikmartins.clubemg.mobile.ui.theme.LocalCustomColorsPalette
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navigateToDetails: (objectId: Int) -> Unit) {
    val viewModel: CalendarViewModel = koinViewModel()
    val model by viewModel.model.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val today = model.today
    val weeks = model.weeksOfEvents

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    LaunchedEffect(weeks) {
        if (weeks.isNotEmpty() && today != null) {
            val todayWeekIndex = weeks.indexOfFirst { today in it.range }
            if (todayWeekIndex >= 0) listState.scrollToItem(todayWeekIndex)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.title_calendar)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = listState,
        ) {
            items(weeks, key = { it.range.start.toEpochDays() }) { weekOfEvents ->
                Week(weekOfEvents, today)
                HorizontalDivider(thickness = Dp.Hairline)
            }
        }
    }
}

private const val WEEKDAYS_GUIDELINE = 1f / 6f
private const val SUNDAY_GUIDELINE = (1f - WEEKDAYS_GUIDELINE) / 2f

@Composable
private fun Week(
    weekOfEvents: CalendarViewModel.WeekOfEvents,
    today: LocalDate?,
    modifier: Modifier = Modifier
) = ConstraintLayout(
    modifier = modifier
        .fillMaxWidth()
        .wrapContentHeight()
) {
    val dateRange = weekOfEvents.range
    val monday = dateRange.start
    val sunday = dateRange.endInclusive
    val saturday = dateRange.first { it.dayOfWeek == DayOfWeek.SATURDAY }

    val weekdaysGuideline = createGuidelineFromStart(WEEKDAYS_GUIDELINE)
    val sundayGuideline = createGuidelineFromEnd(SUNDAY_GUIDELINE)

    val (previousMonthSpace, monthText, weekdayCluster, saturdayText, sundayText) = createRefs()

    val (previousMonthSurface, _) =
        LocalCustomColorsPalette.current.getSurfaceAndOnSurfaceOfDate(monday)
    val (currentMonthSurface, _) =
        LocalCustomColorsPalette.current.getSurfaceAndOnSurfaceOfDate(sunday)

    Text(
        text = sunday.toJavaLocalDate()
            .format(DateTimeFormatter.ofPattern("MMMM")),
        modifier = Modifier
            .constrainAs(monthText) {
                start.linkTo(
                    when (sunday.day) {
                        1 -> sundayGuideline
                        2, 3, 4, 5, 6 -> weekdaysGuideline
                        7 -> parent.start
                        else -> parent.end
                    }
                )
                top.linkTo(parent.top)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
            .run { if (dateRange.any { it.day == 1 }) this else height(0.dp) }
            .background(currentMonthSurface),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall,
    )

    Box(
        Modifier
            .constrainAs(previousMonthSpace) {
                linkTo(parent.start, parent.top, monthText.start, monthText.bottom)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            }
            .background(previousMonthSurface)
    )

    DayCluster(
        LocalDateRange(
            monday,
            sunday.minus(2, DateTimeUnit.DAY)
        ),
        today = today,
        modifier = Modifier
            .constrainAs(weekdayCluster) {
                linkTo(
                    start = parent.start,
                    top = monthText.bottom,
                    end = weekdaysGuideline,
                    bottom = parent.bottom
                )
                width = Dimension.fillToConstraints
            }
            .height(160.dp)
    )

    DayBox(
        localDate = saturday,
        today = today,
        modifier = Modifier
            .constrainAs(saturdayText) {
                linkTo(
                    start = weekdaysGuideline,
                    top = monthText.bottom,
                    end = sundayGuideline,
                    bottom = parent.bottom
                )
                width = Dimension.fillToConstraints
            }
            .height(160.dp)
    )

    DayBox(
        localDate = sunday,
        today = today,
        modifier = Modifier
            .constrainAs(sundayText) {
                linkTo(
                    start = sundayGuideline,
                    top = monthText.bottom,
                    end = parent.end,
                    bottom = parent.bottom
                )
                width = Dimension.fillToConstraints
            }
            .height(160.dp)
    )
}

@Composable
private fun WeekEndDayLabel(text: String, isToday: Boolean, color: Color = Color.Unspecified) =
    Text(
        text = text,
        modifier = Modifier
            .run {
                if (isToday) background(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shape = MaterialTheme.shapes.medium,
                ) else this
            }
            .padding(4.dp),
        color = if (isToday) MaterialTheme.colorScheme.inverseOnSurface else color,
    )

@Composable
private fun WeekDayLabel(text: String, isToday: Boolean, color: Color = Color.Unspecified) = Text(
    text = text,
    modifier = Modifier
        .run {
            if (isToday) background(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = MaterialTheme.shapes.medium
            ) else this
        }
        .padding(4.dp),
    color = if (isToday) MaterialTheme.colorScheme.inverseOnSurface else color,
    style = MaterialTheme.typography.bodySmall,
)

@Composable
private fun DayLabel(localDate: LocalDate, today: LocalDate?, color: Color = Color.Unspecified) =
    when (localDate.dayOfWeek) {
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> WeekEndDayLabel(
            text = localDate.day.toString(),
            isToday = localDate == today,
            color = color,
        )

        else -> WeekDayLabel(
            text = localDate.day.toString(),
            isToday = localDate == today,
            color = color
        )
    }

@Composable
private fun DayBox(localDate: LocalDate, today: LocalDate?, modifier: Modifier = Modifier) {
    val (surfaceColor, onSurfaceColor) =
        LocalCustomColorsPalette.current.getSurfaceAndOnSurfaceOfDate(localDate)

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.background(surfaceColor),
    ) { DayLabel(localDate, today, color = onSurfaceColor) }
}

private fun CustomColorsPalette.getSurfaceAndOnSurfaceOfDate(
    localDate: LocalDate,
): Pair<Color, Color> {
    val monthColorSet = localDate.month.ordinal % 3

    return when (monthColorSet) {
        0 -> monthSurface1 to monthOnSurface1
        1 -> monthSurface2 to monthOnSurface2
        else -> monthSurface3 to monthOnSurface3
    }
}

@Composable
private fun DayCluster(
    dates: LocalDateRange,
    today: LocalDate?,
    modifier: Modifier = Modifier,
    columnCount: Int = 2, // TODO: Remove
) {
    Column(modifier) {
        val iterator = dates.iterator()

        if (iterator.hasNext()) DayBox(
            localDate = iterator.next(),
            today = today,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        while (iterator.hasNext()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                var rowCount = 0

                while (iterator.hasNext() && rowCount < columnCount) {
                    DayBox(
                        localDate = iterator.next(),
                        today = today,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    rowCount++
                }
            }
        }
    }
}

@Preview
@Composable
private fun WeekPreview() = Week(
    weekOfEvents = CalendarViewModel.WeekOfEvents(
        range = LocalDateRange(
            start = LocalDate(2025, 9, 1),
            endInclusive = LocalDate(2025, 9, 7)
        ),
        events = emptyList(),
    ),
    today = LocalDate(2025, 9, 6),
)

@Composable
private fun ObjectFrame(
    obj: MuseumObject,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = obj.primaryImageSmall,
            contentDescription = obj.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.LightGray),
        )

        Spacer(Modifier.height(2.dp))

        Text(obj.title, style = MaterialTheme.typography.titleMedium)
        Text(obj.artistDisplayName, style = MaterialTheme.typography.bodyMedium)
        Text(obj.objectDate, style = MaterialTheme.typography.bodySmall)
    }
}
