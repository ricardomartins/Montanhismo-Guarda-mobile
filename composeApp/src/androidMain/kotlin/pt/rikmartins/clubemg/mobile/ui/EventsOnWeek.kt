package pt.rikmartins.clubemg.mobile.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.toJavaLocalDate
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.thisWeeksSaturday
import pt.rikmartins.clubemg.mobile.thisWeeksSunday
import java.time.format.DateTimeFormatter
import java.util.Locale

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
internal fun EventsOnWeek(
    weekOfEvents: WeekOfEvents,
    onEventClick: (event: SimplifiedEvent) -> Unit,
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
        val showImage = rows.size < 2

        rows.forEach { eventRow ->
            Row(Modifier.weight(1f)) {
                when (eventRow) {
                    is EventRow.Span.Single.Weekdays -> {
                        EventCard(eventRow.event, showImage, onEventClick, modifier = Modifier.weight(FULL_DAY_WEIGHT))
                        Spacer(modifier.weight(COMPACT_DAY_WEIGHT + FULL_DAY_WEIGHT))
                    }

                    is EventRow.Span.Single.WeekdaysAndSaturday -> {
                        EventCard(
                            eventRow.event,
                            showImage,
                            onEventClick,
                            modifier = Modifier.weight(COMPACT_DAY_WEIGHT + FULL_DAY_WEIGHT)
                        )
                        Spacer(modifier.weight(FULL_DAY_WEIGHT))
                    }

                    is EventRow.Span.Single.WholeWeek -> EventCard(eventRow.event, showImage, onEventClick)
                    is EventRow.Span.Single.Saturday -> {
                        Spacer(modifier.weight(COMPACT_DAY_WEIGHT))
                        EventCard(eventRow.event, showImage, onEventClick, modifier = Modifier.weight(FULL_DAY_WEIGHT))
                        Spacer(modifier.weight(FULL_DAY_WEIGHT))
                    }

                    is EventRow.Span.Single.Weekend -> {
                        Spacer(modifier.weight(COMPACT_DAY_WEIGHT))
                        EventCard(
                            eventRow.event,
                            showImage,
                            onEventClick,
                            modifier = Modifier.weight(FULL_DAY_WEIGHT + FULL_DAY_WEIGHT),
                        )
                    }

                    is EventRow.Span.Single.Sunday -> {
                        Spacer(modifier.weight(COMPACT_DAY_WEIGHT + FULL_DAY_WEIGHT))
                        EventCard(eventRow.event, showImage, onEventClick, modifier = Modifier.weight(FULL_DAY_WEIGHT))
                    }

                    is EventRow.Span.Double.WeekdaysPlusSunday -> {
                        EventCard(
                            eventRow.startEvent,
                            showImage,
                            onEventClick,
                            modifier = Modifier.weight(FULL_DAY_WEIGHT)
                        )
                        Spacer(modifier.weight(COMPACT_DAY_WEIGHT))
                        EventCard(
                            eventRow.endEvent,
                            showImage,
                            onEventClick,
                            modifier = Modifier.weight(FULL_DAY_WEIGHT)
                        )
                    }

                    is EventRow.Span.Double.WeekdaysAndSaturdayPlusSunday -> {
                        EventCard(
                            eventRow.startEvent,
                            showImage,
                            onEventClick,
                            modifier = Modifier.weight(COMPACT_DAY_WEIGHT + FULL_DAY_WEIGHT),
                        )
                        EventCard(
                            eventRow.endEvent,
                            showImage,
                            onEventClick,
                            modifier = Modifier.weight(FULL_DAY_WEIGHT)
                        )
                    }

                    is EventRow.Span.Double.SaturdayPlusSunday -> {
                        Spacer(modifier.weight(COMPACT_DAY_WEIGHT))
                        EventCard(
                            eventRow.startEvent,
                            showImage,
                            onEventClick,
                            modifier = Modifier.weight(FULL_DAY_WEIGHT)
                        )
                        EventCard(
                            eventRow.endEvent,
                            showImage,
                            onEventClick,
                            modifier = Modifier.weight(FULL_DAY_WEIGHT)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: SimplifiedEvent,
    showImage: Boolean,
    onEventClick: (event: SimplifiedEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxHeight()
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        onClick = { onEventClick(event) },
    ) {
        if (showImage) {
            var imageSize by remember { mutableStateOf(IntSize.Zero) }
            val selectedImage = remember(imageSize) {
                event.sortedImages.firstOrNull {
                    it.width > imageSize.width && it.height > imageSize.height
                }?.url
            }

            if (selectedImage != null) AsyncImage(
                model = selectedImage,
                contentDescription = "${event.title} cover image", // TODO: Localize
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { imageSize = it }
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                placeholder = painterResource(id = R.drawable.fallback),
                error = painterResource(id = R.drawable.fallback),
                fallback = painterResource(id = R.drawable.fallback),
            )
        }
        Row {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Text(
                    text = event.range.toLocalizedString(LocalConfiguration.current.locales[0]),
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (event.isBookmarked) Icon(
                painter = painterResource(R.drawable.ic_bookmark),
                contentDescription = null, // TODO: Localize
                modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun LocalDateRange.toLocalizedString(locale: Locale): String {
    val formatter = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "LLL-d"))

    return if (size > 1) {
        val startDate = start.toJavaLocalDate().format(formatter)
        val endDate = endInclusive.toJavaLocalDate().format(formatter)

        "$startDate - $endDate"
    } else {
        start.toJavaLocalDate().format(formatter)
    }
}