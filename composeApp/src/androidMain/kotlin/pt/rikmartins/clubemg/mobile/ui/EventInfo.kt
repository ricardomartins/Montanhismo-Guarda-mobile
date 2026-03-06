package pt.rikmartins.clubemg.mobile.ui

import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.toJavaLocalDate
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventStatusType
import pt.rikmartins.clubemg.mobile.ui.theme.LocalCustomColorsPalette
import java.time.format.DateTimeFormatter

@Composable
fun EventInfo(
    title: String?,
    range: LocalDateRange?,
    isBookmarked: Boolean,
    categories: List<EventCategory>,
    eventStatus: EventStatusType?,
    modifier: Modifier = Modifier,
) {
    @StringRes var explanationTextRes = 0
    @DrawableRes var dateIconRes = R.drawable.ic_empty_calendar
    var dateColor: Color = MaterialTheme.colorScheme.onSurface

    when (eventStatus) {
        null, EventStatusType.Scheduled -> {
            /* use defaults */
        }

        EventStatusType.Rescheduled, EventStatusType.Postponed -> {
            dateIconRes = R.drawable.ic_event_busy
            explanationTextRes = R.string.postponed_event_explanation
            dateColor = LocalCustomColorsPalette.current.eventPostponed
        }

        EventStatusType.Cancelled -> {
            dateIconRes = R.drawable.ic_event_repeat
            explanationTextRes = R.string.canceled_event_explanation
            dateColor = LocalCustomColorsPalette.current.eventCanceled
        }

        EventStatusType.MovedOnline -> {
            /* use defaults, and in the future it should be reported for analytics */
        }
    }
    val dateIcon = painterResource(dateIconRes)

    Row(modifier = modifier) {
        Column(modifier = Modifier.weight(1f)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (title != null) {
                    val titleText =
                        if (explanationTextRes != 0) "$title (${stringResource(explanationTextRes)})"
                        else title

                    Text(
                        text = titleText,
                        modifier = Modifier.alignByBaseline(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                categories.forEach {
                    val categoryName = when (it) {
                        is EventCategory.Match -> stringResource(it.designationRes)
                        is EventCategory.NoMatch -> it.designation
                    }

                    Text(
                        text = categoryName,
                        modifier = Modifier.alignByBaseline(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (range != null) {
                val locale = LocalConfiguration.current.locales[0]
                val formatter =
                    remember { DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "LLL-d")) }

                val dateText = with(range) {
                    if (size > 1) {
                        val startDate = start.toJavaLocalDate().format(formatter)
                        val endDate = endInclusive.toJavaLocalDate().format(formatter)

                        "$startDate - $endDate"
                    } else {
                        start.toJavaLocalDate().format(formatter)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = dateIcon,
                        contentDescription = null, // TODO: Localize
                        modifier = Modifier.height(16.dp),
                        tint = dateColor
                    )
                    Text(
                        text = dateText,
                        modifier = Modifier.padding(start = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = dateColor,
                    )
                }
            }
        }

        if (isBookmarked) Icon(
            painter = painterResource(R.drawable.ic_bookmark),
            contentDescription = null, // TODO: Localize
            tint = MaterialTheme.colorScheme.primary
        )
    }
}