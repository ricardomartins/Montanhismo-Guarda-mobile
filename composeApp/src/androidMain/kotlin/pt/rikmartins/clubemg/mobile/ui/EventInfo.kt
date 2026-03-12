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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.toJavaLocalDate
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventStatusType
import pt.rikmartins.clubemg.mobile.domain.usecase.events.EventTaxonomy
import pt.rikmartins.clubemg.mobile.domain.usecase.events.TaxonomyType
import pt.rikmartins.clubemg.mobile.ui.theme.LocalCustomColorsPalette
import java.time.format.DateTimeFormatter



@Composable
fun SmallEventInfo(
    title: String?,
    range: LocalDateRange?,
    isBookmarked: Boolean,
    eventStatus: EventStatusType?,
    modifier: Modifier = Modifier,
) {
    EventInfo(title, range, isBookmarked, emptySet(), eventStatus, EventInfoSize.SMALL, modifier)
}
@Composable
fun LargeEventInfo(
    title: String?,
    range: LocalDateRange?,
    isBookmarked: Boolean,
    categories: Collection<EventTaxonomy>,
    eventStatus: EventStatusType?,
    modifier: Modifier = Modifier,
) {
    EventInfo(title, range, isBookmarked, categories, eventStatus, EventInfoSize.LARGE, modifier)
}

private enum class EventInfoSize {
    SMALL, LARGE;
}

@Composable
private fun EventInfo(
    title: String?,
    range: LocalDateRange?,
    isBookmarked: Boolean,
    categories: Collection<EventTaxonomy>,
    eventStatus: EventStatusType?,
    eventInfoSize: EventInfoSize,
    modifier: Modifier = Modifier,
) {
    @StringRes var explanationTextRes = 0
    @DrawableRes var dateIconRes = R.drawable.ic_empty_calendar
    var dateColor: Color = MaterialTheme.colorScheme.onSurface

    when (eventStatus) {
        null -> {
            dateIconRes = R.drawable.ic_event_unknown
        }

        EventStatusType.Scheduled -> {
            /* use defaults */
        }

        EventStatusType.Rescheduled, EventStatusType.Postponed -> {
            dateIconRes = R.drawable.ic_event_postponed
            explanationTextRes = R.string.postponed_event_explanation
            dateColor = LocalCustomColorsPalette.current.eventPostponed
        }

        EventStatusType.Cancelled -> {
            dateIconRes = R.drawable.ic_event_busy
            explanationTextRes = R.string.canceled_event_explanation
            dateColor = LocalCustomColorsPalette.current.eventCanceled
        }

        EventStatusType.MovedOnline -> {
            /* use defaults, and in the future it should be reported for analytics */
        }
    }
    val dateIcon = painterResource(dateIconRes)

    val titleStyle: TextStyle
    val dateStyle: TextStyle
    val dateIconSize: Dp
    val spacing: Dp
    when (eventInfoSize) {
        EventInfoSize.SMALL -> {
            titleStyle = MaterialTheme.typography.bodyMedium
            dateStyle = MaterialTheme.typography.bodySmall
            dateIconSize = 12.dp
            spacing = 8.dp
        }
        EventInfoSize.LARGE -> {
            titleStyle = MaterialTheme.typography.bodyLarge
            dateStyle = MaterialTheme.typography.bodyMedium
            dateIconSize = 16.dp
            spacing = 16.dp
        }
    }

    Row(modifier = modifier) {
        Column(modifier = Modifier.weight(1f)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                if (title != null) {
                    val titleText =
                        if (explanationTextRes != 0) "$title (${stringResource(explanationTextRes)})"
                        else title

                    Text(
                        text = titleText,
                        modifier = Modifier.alignByBaseline(),
                        style = titleStyle,
                    )
                }

                if (eventInfoSize == EventInfoSize.LARGE) categories.toEventCategories().forEach {
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
                        modifier = Modifier.height(dateIconSize),
                        tint = dateColor
                    )
                    Text(
                        text = dateText,
                        modifier = Modifier.padding(start = 4.dp),
                        style = dateStyle,
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

private sealed interface EventCategory : Comparable<EventCategory> {
    val order: Int

    sealed class Match(
        @param:StringRes val designationRes: Int,
        val slugMatch: String,
        override val order: Int,
    ) : EventCategory {

        data object Assembly : Match(designationRes = R.string.category_assembly, slugMatch = "assembleia", order = 10)
        data object Cycling : Match(designationRes = R.string.category_cycling, slugMatch = "ciclismo", order = 20)
        data object MountainBike : Match(
            designationRes = R.string.category_mountain_bike,
            slugMatch = "btt",
            order = 21
        )

        data object RoadBike : Match(designationRes = R.string.category_road, slugMatch = "estrada", order = 22)
        data object Nautical : Match(designationRes = R.string.category_sailing, slugMatch = "nautica", order = 30)
        data object Canoeing : Match(designationRes = R.string.category_canoe, slugMatch = "canoagem", order = 31)
        data object Competition : Match(
            designationRes = R.string.category_competition,
            slugMatch = "competicao",
            order = 40
        )

        data object MountainRunning : Match(
            designationRes = R.string.category_mountain_run,
            slugMatch = "corrida-de-montanha",
            order = 50
        )

        data object Climbing : Match(designationRes = R.string.category_climbing, slugMatch = "escalada", order = 60)
        data object Training : Match(designationRes = R.string.category_training, slugMatch = "formacao", order = 70)
        data object Mountain : Match(designationRes = R.string.category_mountain, slugMatch = "montanha", order = 80)
        data object Wintry : Match(designationRes = R.string.category_invernal, slugMatch = "invernal", order = 81)
        data object Multiactivity : Match(
            designationRes = R.string.category_multiactivity,
            slugMatch = "multiatividade",
            order = 90
        )

        data object Orienteering : Match(
            designationRes = R.string.category_orienteering,
            slugMatch = "orientacao",
            order = 100
        )

        data object Hiking : Match(
            designationRes = R.string.category_pedestrianism,
            slugMatch = "pedestrianismo",
            order = 110
        )

        data object Social : Match(designationRes = R.string.category_social, slugMatch = "social", order = 120)

        override fun compareTo(other: EventCategory): Int = order.compareTo(other.order)

        companion object {
            val entries = setOf(
                Assembly, Cycling, MountainBike, RoadBike, Nautical, Canoeing, Competition, MountainRunning, Climbing,
                Training, Mountain, Wintry, Multiactivity, Orienteering, Hiking, Social,
            )
        }
    }

    data class NoMatch(val designation: String) : EventCategory {
        override val order: Int = Int.MAX_VALUE

        override fun compareTo(other: EventCategory): Int = order.compareTo(other.order)
    }
}

private fun EventTaxonomy.toEventCategory(): EventCategory =
    EventCategory.Match.entries.find { slug == it.slugMatch } ?: EventCategory.NoMatch(designation = name)

private fun Collection<EventTaxonomy>.toEventCategories(): List<EventCategory> =
    filter { it.taxonomyType == TaxonomyType.CATEGORY }.map { it.toEventCategory() }.sorted()
