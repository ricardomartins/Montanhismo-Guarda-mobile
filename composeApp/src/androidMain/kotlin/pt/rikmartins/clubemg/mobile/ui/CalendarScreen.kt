package pt.rikmartins.clubemg.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.toJavaLocalDate
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
            items(weeks, key = { it.monday.toEpochDays() }) { weekOfEvents ->
                Week(weekOfEvents, today)
                HorizontalDivider(thickness = Dp.Hairline)
            }
        }
    }
}

private const val WEEKDAYS_GUIDELINE = 1f / 6f
private const val SUNDAY_GUIDELINE = (1f - WEEKDAYS_GUIDELINE) / 2f

private const val WEEKDAYS_WEIGHT = 2f
private const val WEEKEND_DAY_WEIGHT = 5f
private const val WEEK_TOTAL_WEIGHT = WEEKDAYS_WEIGHT + WEEKEND_DAY_WEIGHT + WEEKEND_DAY_WEIGHT

@Composable
private fun Week(
    weekOfEvents: CalendarViewModel.WeekOfEvents,
    today: LocalDate?,
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .wrapContentHeight()
) {
    val monday = weekOfEvents.monday
    val saturday = monday.thisWeeksSaturday()
    val sunday = monday.thisWeeksSunday()

    if (sunday.day <= 7) MonthLabelRow(monday, sunday)

    Row(modifier = Modifier.height(160.dp)) {
        DayCluster(monday, today = today, modifier = Modifier.weight(WEEKDAYS_WEIGHT))
        DayBox(
            localDate = saturday,
            today = today,
            modifier = Modifier
                .weight(WEEKEND_DAY_WEIGHT)
                .fillMaxHeight()
        )
        DayBox(
            localDate = sunday,
            today = today,
            modifier = Modifier
                .weight(WEEKEND_DAY_WEIGHT)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun MonthLabelRow(monday: LocalDate, sunday: LocalDate) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        val (previousMonthSurface, _) =
            LocalCustomColorsPalette.current.getSurfaceAndOnSurfaceOfDate(monday)
        val (currentMonthSurface, _) =
            LocalCustomColorsPalette.current.getSurfaceAndOnSurfaceOfDate(sunday)

        val monthLabelWeight = when (sunday.day) {
            1 -> WEEKEND_DAY_WEIGHT
            2, 3, 4, 5, 6 -> WEEKEND_DAY_WEIGHT * 2f
            else -> WEEK_TOTAL_WEIGHT
        }

        if (monthLabelWeight != WEEK_TOTAL_WEIGHT) Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(WEEK_TOTAL_WEIGHT - monthLabelWeight)
                .background(previousMonthSurface)
        )

        Text(
            text = sunday.toJavaLocalDate().format(
                DateTimeFormatter.ofPattern(
                    if (sunday.month == Month.JANUARY) "MMMM y" else "MMMM"
                )
            ),
            modifier = Modifier
                .weight(monthLabelWeight)
                .background(currentMonthSurface),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
    }
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
    monday: LocalDate,
    today: LocalDate?,
    modifier: Modifier = Modifier,
    columnCount: Int = 2, // TODO: Remove
) {
    Column(modifier) {
        val iterator = (monday..monday.thisWeeksFriday()).iterator()

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
        monday = LocalDate(2025, 9, 1),
        events = emptyList(),
    ),
    today = LocalDate(2025, 9, 6),
)
