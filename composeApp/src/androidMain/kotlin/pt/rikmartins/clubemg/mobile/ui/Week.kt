package pt.rikmartins.clubemg.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.toJavaLocalDate
import pt.rikmartins.clubemg.mobile.domain.entity.SimplifiedEvent
import pt.rikmartins.clubemg.mobile.domain.entity.WeekOfEvents
import pt.rikmartins.clubemg.mobile.ui.theme.CustomColorsPalette
import pt.rikmartins.clubemg.mobile.ui.theme.LocalCustomColorsPalette
import java.time.format.DateTimeFormatter

internal const val COMPACT_DAY_WEIGHT = 2f
internal const val FULL_DAY_WEIGHT = 5f
internal const val WEEK_TOTAL_WEIGHT = COMPACT_DAY_WEIGHT + FULL_DAY_WEIGHT + FULL_DAY_WEIGHT

private const val WEEK_HEIGHT = 256
private val WEEK_HEIGHT_IN_DP = WEEK_HEIGHT.dp

@Composable
internal fun Week(
    weekOfEvents: WeekOfEvents,
    today: LocalDate?,
    onEventClick: (event: SimplifiedEvent) -> Unit,
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
    if (weekOfEvents.events.isNotEmpty())
        EventsOnWeek(weekOfEvents, onEventClick, modifier = Modifier.height(eventsHeight))
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
    val monthColorSet = localDate.month.ordinal % 2

    return when (monthColorSet) {
        0 -> monthSurface1 to monthOnSurface1
        else -> monthSurface2 to monthOnSurface2
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
