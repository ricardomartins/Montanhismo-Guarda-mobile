package pt.rikmartins.clubemg.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.minus
import pt.rikmartins.clubemg.mobile.data.MuseumObject
import org.koin.androidx.compose.koinViewModel
import pt.rikmartins.clubemg.mobile.R

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
                Week(weekOfEvents, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                HorizontalDivider(thickness = Dp.Hairline)
            }
        }
    }
}

@Composable
private fun Week(weekOfEvents: CalendarViewModel.WeekOfEvents, modifier: Modifier = Modifier) {
    ConstraintLayout(
        modifier = modifier
            .heightIn(min = 128.dp)
            .fillMaxWidth()
    ) {
        val weekdaysGuideline = createGuidelineFromStart(0.2f)
        val sundayGuideline = createGuidelineFromEnd(0.4f)

        val (weekdayCluster, saturdayText, sundayText, weekdaysDivider, sundayDivider) = createRefs()

        DayCluster(
            LocalDateRange(
                weekOfEvents.range.start,
                weekOfEvents.range.endInclusive.minus(2, DateTimeUnit.DAY)
            ),
            modifier = Modifier
                .constrainAs(weekdayCluster) {
                    linkTo(parent.start, parent.top, weekdaysGuideline, parent.bottom)
                    width = Dimension.fillToConstraints
                    height = Dimension.fillToConstraints
                }
        )

        VerticalDivider(
            thickness = Dp.Hairline,
            modifier = Modifier.constrainAs(weekdaysDivider) {
                linkTo(weekdaysGuideline, parent.top, weekdaysGuideline, parent.bottom)
            },
        )

        val rangeIterator = weekOfEvents.range.iterator()
        while (rangeIterator.hasNext() && rangeIterator.next().dayOfWeek != DayOfWeek.FRIDAY);

        Text(
            text = rangeIterator.next().day.toString(),
            modifier = Modifier.constrainAs(saturdayText) {
                linkTo(weekdaysGuideline, parent.top, sundayGuideline, parent.bottom)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            },
            textAlign = TextAlign.Center,
        )

        VerticalDivider(
            thickness = Dp.Hairline,
            modifier = Modifier.constrainAs(sundayDivider) {
                linkTo(sundayGuideline, parent.top, sundayGuideline, parent.bottom)
            },
        )

        Text(
            text = rangeIterator.next().day.toString(),
            modifier = Modifier.constrainAs(sundayText) {
                linkTo(sundayGuideline, parent.top, parent.end, parent.bottom)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            },
            textAlign = TextAlign.Center,

            )
    }
}

@Composable
private fun DayCluster(dates: LocalDateRange, modifier: Modifier = Modifier, columnCount: Int = 2) {
    Column(modifier) {
        val iterator = dates.iterator()

        if (iterator.hasNext()) {
            Text(
                text = iterator.next().day.toString(),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
        }
        while (iterator.hasNext()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                var rowCount = 0

                while (iterator.hasNext() && rowCount < columnCount) {
                    Text(
                        text = iterator.next().day.toString(),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
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
    CalendarViewModel.WeekOfEvents(
        LocalDateRange(LocalDate(2025, 9, 1), LocalDate(2025, 9, 7)),
        emptyList(),
    )
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
