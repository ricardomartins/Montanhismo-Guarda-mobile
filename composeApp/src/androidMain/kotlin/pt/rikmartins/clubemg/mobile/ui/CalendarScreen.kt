package pt.rikmartins.clubemg.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.toJavaLocalDate
import org.koin.androidx.compose.koinViewModel
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.domain.entity.SimplifiedEvent
import pt.rikmartins.clubemg.mobile.domain.entity.WeekOfEvents
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

private const val COMPACT_DAY_WEIGHT = 2f
private const val FULL_DAY_WEIGHT = 5f
private const val WEEK_TOTAL_WEIGHT = COMPACT_DAY_WEIGHT + FULL_DAY_WEIGHT + FULL_DAY_WEIGHT

private const val WEEK_HEIGHT = 256
private val WEEK_HEIGHT_IN_DP = WEEK_HEIGHT.dp

@Composable
private fun Week(
    weekOfEvents: WeekOfEvents,
    today: LocalDate?,
) = Box(contentAlignment = Alignment.BottomCenter) {
    var dayLabelSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val eventsHeight = remember(dayLabelSize) { WEEK_HEIGHT_IN_DP - (dayLabelSize.height / density.density).dp }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        val monday = weekOfEvents.monday
        val saturday = monday.thisWeeksSaturday()
        val sunday = monday.thisWeeksSunday()

        if (sunday.day <= 7) MonthLabelRow(monday, sunday)

        WeekRow(
            monday = monday,
            today = today,
            saturday = saturday,
            sunday = sunday,
            onDayLabelSizeChanged = { dayLabelSize = it },
            modifier = Modifier.height(WEEK_HEIGHT_IN_DP)
        )
    }
    if (weekOfEvents.events.isNotEmpty()) EventsOnWeek(weekOfEvents, modifier = Modifier.height(eventsHeight))
}

@Composable
private fun WeekRow(
    monday: LocalDate,
    today: LocalDate?,
    saturday: LocalDate,
    sunday: LocalDate,
    onDayLabelSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        DayCluster(monday, today = today, modifier = Modifier.weight(COMPACT_DAY_WEIGHT))
        DayBox(
            localDate = saturday,
            today = today,
            modifier = Modifier
                .weight(FULL_DAY_WEIGHT)
                .fillMaxHeight()
        )
        DayBox(
            localDate = sunday,
            today = today,
            modifier = Modifier
                .weight(FULL_DAY_WEIGHT)
                .fillMaxHeight(),
            onLabelSizeChanged = onDayLabelSizeChanged,
        )
    }
}

@Composable
private fun MonthLabelRow(monday: LocalDate, sunday: LocalDate) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        val (previousMonthSurface, _) = LocalCustomColorsPalette.current.getSurfaceAndOnSurfaceOfDate(monday)
        val (currentMonthSurface, _) = LocalCustomColorsPalette.current.getSurfaceAndOnSurfaceOfDate(sunday)

        val monthLabelWeight = when (sunday.day) {
            1 -> FULL_DAY_WEIGHT
            2, 3, 4, 5, 6 -> FULL_DAY_WEIGHT * 2f
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
private fun WeekEndDayLabel(
    text: String,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) = Text(
    text = text,
    modifier = modifier
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
private fun WeekDayLabel(
    text: String,
    isToday: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) = Text(
    text = text,
    modifier = modifier
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
private fun DayLabel(
    localDate: LocalDate,
    today: LocalDate?,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) = when (localDate.dayOfWeek) {
    DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> WeekEndDayLabel(
        text = localDate.day.toString(),
        isToday = localDate == today,
        modifier = modifier,
        color = color,
    )

    else -> WeekDayLabel(
        text = localDate.day.toString(),
        isToday = localDate == today,
        color = color
    )
}

@Composable
private fun DayBox(
    localDate: LocalDate,
    today: LocalDate?,
    modifier: Modifier = Modifier,
    onLabelSizeChanged: ((IntSize) -> Unit)? = null,
) {
    val (surfaceColor, onSurfaceColor) =
        LocalCustomColorsPalette.current.getSurfaceAndOnSurfaceOfDate(localDate)

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.background(surfaceColor),
    ) {
        DayLabel(
            localDate,
            today,
            modifier = Modifier.run {
                if (onLabelSizeChanged != null) onSizeChanged(onLabelSizeChanged)
                else this
            },
            color = onSurfaceColor
        )
    }
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

interface EventRow {

    sealed interface ThatAccepts {
        interface StartSpan<TARGET : Span.Double> : ThatAccepts {
            fun addEventThatSpansWeekdays(
                event: SimplifiedEvent,
                fromPreviousWeek: Boolean,
            ): TARGET

            fun addEventThatSpansWeekdaysAndSaturday(
                event: SimplifiedEvent,
                fromPreviousWeek: Boolean,
            ): TARGET

            fun addEventThatSpansSaturday(event: SimplifiedEvent): TARGET
        }

        interface EndSpan<TARGET : Span.Double> : ThatAccepts {
            fun addEventThatSpansSunday(
                event: SimplifiedEvent,
                toNextWeek: Boolean
            ): TARGET
        }
    }

    sealed interface Span : EventRow {
        sealed interface Single : Span {
            val event: SimplifiedEvent

            data class Weekdays(
                override val event: SimplifiedEvent,
                val fromPreviousWeek: Boolean,
            ) : Single, ThatAccepts.EndSpan<Double> {

                override fun addEventThatSpansSunday(
                    event: SimplifiedEvent,
                    toNextWeek: Boolean
                ) = Double.WeekdaysPlusSunday(
                    startEvent = this.event,
                    fromPreviousWeek = fromPreviousWeek,
                    endEvent = event,
                    toNextWeek = toNextWeek
                )
            }

            data class WeekdaysAndSaturday(
                override val event: SimplifiedEvent,
                val fromPreviousWeek: Boolean,
            ) : Single, ThatAccepts.EndSpan<Double> {

                override fun addEventThatSpansSunday(
                    event: SimplifiedEvent,
                    toNextWeek: Boolean
                ) = Double.WeekdaysAndSaturdayPlusSunday(
                    startEvent = this.event,
                    fromPreviousWeek = fromPreviousWeek,
                    endEvent = event,
                    toNextWeek = toNextWeek
                )
            }

            data class WholeWeek(
                override val event: SimplifiedEvent,
                val fromPreviousWeek: Boolean,
                val toNextWeek: Boolean,
            ) : Single

            data class Saturday(
                override val event: SimplifiedEvent,
            ) : Single, ThatAccepts.EndSpan<Double> {

                override fun addEventThatSpansSunday(
                    event: SimplifiedEvent,
                    toNextWeek: Boolean
                ) = Double.SaturdayPlusSunday(
                    startEvent = this.event,
                    endEvent = event,
                    toNextWeek = toNextWeek
                )
            }

            data class Weekend(
                override val event: SimplifiedEvent,
                val toNextWeek: Boolean,
            ) : Single

            data class Sunday(
                override val event: SimplifiedEvent,
                val toNextWeek: Boolean,
            ) : Single, ThatAccepts.StartSpan<Double> {

                override fun addEventThatSpansWeekdays(
                    event: SimplifiedEvent,
                    fromPreviousWeek: Boolean
                ) = Double.WeekdaysPlusSunday(
                    startEvent = event,
                    fromPreviousWeek = fromPreviousWeek,
                    endEvent = this.event,
                    toNextWeek = toNextWeek
                )

                override fun addEventThatSpansWeekdaysAndSaturday(
                    event: SimplifiedEvent,
                    fromPreviousWeek: Boolean
                ) = Double.WeekdaysAndSaturdayPlusSunday(
                    startEvent = event,
                    fromPreviousWeek = fromPreviousWeek,
                    endEvent = this.event,
                    toNextWeek = toNextWeek
                )

                override fun addEventThatSpansSaturday(event: SimplifiedEvent) =
                    Double.SaturdayPlusSunday(
                        startEvent = event,
                        endEvent = this.event,
                        toNextWeek = toNextWeek
                    )
            }
        }

        sealed interface Double : Span {
            val startEvent: SimplifiedEvent
            val endEvent: SimplifiedEvent

            data class WeekdaysPlusSunday(
                override val startEvent: SimplifiedEvent,
                val fromPreviousWeek: Boolean,
                override val endEvent: SimplifiedEvent,
                val toNextWeek: Boolean,
            ) : Double

            data class WeekdaysAndSaturdayPlusSunday(
                override val startEvent: SimplifiedEvent,
                val fromPreviousWeek: Boolean,
                override val endEvent: SimplifiedEvent,
                val toNextWeek: Boolean,
            ) : Double

            data class SaturdayPlusSunday(
                override val startEvent: SimplifiedEvent,
                override val endEvent: SimplifiedEvent,
                val toNextWeek: Boolean,
            ) : Double
        }
    }
}

@Composable
private fun EventsOnWeek(
    weekOfEvents: WeekOfEvents,
    modifier: Modifier = Modifier,
) {
    val monday = weekOfEvents.monday
    val saturday = monday.thisWeeksSaturday()
    val sunday = monday.thisWeeksSunday()

    val rows: List<EventRow.Span> = buildList {
        weekOfEvents.events.forEach { event ->
            val range = event.range

            val spansWeekdays = range.start < saturday
            val spansSaturday = saturday in range
            val spansSunday = sunday in range

            val fromPreviousWeek = range.start < monday
            val toNextWeek = range.endInclusive > sunday

            when {
                spansWeekdays && spansSaturday && spansSunday ->
                    add(EventRow.Span.Single.WholeWeek(event, fromPreviousWeek, toNextWeek))

                spansSaturday && spansSunday -> add(EventRow.Span.Single.Weekend(event, toNextWeek))

                spansWeekdays && spansSaturday -> filterIsInstance<EventRow.ThatAccepts.StartSpan<*>>().firstOrNull()
                    ?.let { acceptingEventRow ->
                        replaceAll {
                            if (it == acceptingEventRow)
                                acceptingEventRow.addEventThatSpansWeekdaysAndSaturday(event, fromPreviousWeek)
                            else it
                        }
                    }
                    ?: add(EventRow.Span.Single.WeekdaysAndSaturday(event, fromPreviousWeek))

                spansWeekdays -> filterIsInstance<EventRow.ThatAccepts.StartSpan<*>>().firstOrNull()
                    ?.let { acceptingEventRow ->
                        replaceAll {
                            if (it == acceptingEventRow)
                                acceptingEventRow.addEventThatSpansWeekdays(event, fromPreviousWeek)
                            else it
                        }
                    }
                    ?: add(EventRow.Span.Single.Weekdays(event, fromPreviousWeek))

                spansSaturday -> filterIsInstance<EventRow.ThatAccepts.StartSpan<*>>().firstOrNull()
                    ?.let { acceptingEventRow ->
                        replaceAll {
                            if (it == acceptingEventRow) acceptingEventRow.addEventThatSpansSaturday(event)
                            else it
                        }
                    }
                    ?: add(EventRow.Span.Single.Saturday(event))

                spansSunday -> filterIsInstance<EventRow.ThatAccepts.EndSpan<*>>().firstOrNull()
                    ?.let { acceptingEventRow ->
                        replaceAll {
                            if (it == acceptingEventRow)
                                acceptingEventRow.addEventThatSpansSunday(event, toNextWeek)
                            else it
                        }
                    }
                    ?: add(EventRow.Span.Single.Sunday(event, toNextWeek))

                else -> throw IllegalStateException("Event $this is not inside the week that starts on: $monday")
            }
        }
    }

    Column(modifier = modifier) {
        rows.forEach { eventRow ->
            Row(Modifier.weight(1f)) {
                when (eventRow) {
                    is EventRow.Span.Single.Weekdays -> {
                        EventCard(eventRow.event, modifier = Modifier.weight(FULL_DAY_WEIGHT))
                        Spacer(modifier.weight(COMPACT_DAY_WEIGHT + FULL_DAY_WEIGHT))
                    }

                    is EventRow.Span.Single.WeekdaysAndSaturday -> {
                        EventCard(eventRow.event, modifier = Modifier.weight(COMPACT_DAY_WEIGHT + FULL_DAY_WEIGHT))
                        Spacer(modifier.weight(FULL_DAY_WEIGHT))
                    }

                    is EventRow.Span.Single.WholeWeek -> EventCard(eventRow.event)
                    is EventRow.Span.Single.Saturday -> {
                        Spacer(modifier.weight(COMPACT_DAY_WEIGHT))
                        EventCard(eventRow.event, modifier = Modifier.weight(FULL_DAY_WEIGHT))
                        Spacer(modifier.weight(FULL_DAY_WEIGHT))
                    }

                    is EventRow.Span.Single.Weekend -> {
                        Spacer(modifier.weight(COMPACT_DAY_WEIGHT))
                        EventCard(eventRow.event, modifier = Modifier.weight(FULL_DAY_WEIGHT + FULL_DAY_WEIGHT))
                    }

                    is EventRow.Span.Single.Sunday -> {
                        Spacer(modifier.weight(COMPACT_DAY_WEIGHT + FULL_DAY_WEIGHT))
                        EventCard(eventRow.event, modifier = Modifier.weight(FULL_DAY_WEIGHT))
                    }

                    is EventRow.Span.Double.WeekdaysPlusSunday -> {
                        EventCard(eventRow.startEvent, modifier = Modifier.weight(FULL_DAY_WEIGHT))
                        Spacer(modifier.weight(COMPACT_DAY_WEIGHT))
                        EventCard(eventRow.endEvent, modifier = Modifier.weight(FULL_DAY_WEIGHT))
                    }

                    is EventRow.Span.Double.WeekdaysAndSaturdayPlusSunday -> {
                        EventCard(eventRow.startEvent, modifier = Modifier.weight(COMPACT_DAY_WEIGHT + FULL_DAY_WEIGHT))
                        EventCard(eventRow.endEvent, modifier = Modifier.weight(FULL_DAY_WEIGHT))
                    }

                    is EventRow.Span.Double.SaturdayPlusSunday -> {
                        Spacer(modifier.weight(COMPACT_DAY_WEIGHT))
                        EventCard(eventRow.startEvent, modifier = Modifier.weight(FULL_DAY_WEIGHT))
                        EventCard(eventRow.endEvent, modifier = Modifier.weight(FULL_DAY_WEIGHT))
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: SimplifiedEvent, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        var imageSize by remember { mutableStateOf(IntSize.Zero) }
        val selectedImage = remember(imageSize) {
            event.sortedImages.firstOrNull {
                it.width > imageSize.width && it.height > imageSize.height
            }?.url
        }

        AsyncImage(
            model = selectedImage,
            contentDescription = "${event.title} cover image", // TODO: Localize
            modifier = Modifier
                .fillMaxWidth()
                .weight(3f)
                .onSizeChanged { imageSize = it },
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.fallback),
            error = painterResource(id = R.drawable.fallback),
            fallback = painterResource(id = R.drawable.fallback),
        )
        Text(
            text = event.title,
            modifier = Modifier
                .padding(8.dp)
                .weight(2f),
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
