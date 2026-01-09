package pt.rikmartins.clubemg.mobile.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import kotlinx.datetime.toJavaLocalDate
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.domain.usecase.events.CalendarEvent
import java.time.format.DateTimeFormatter

@Composable
fun EventActionsDialog(
    event: SimplifiedEvent,
    navigateToDetails: (event: CalendarEvent) -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp)
        ) {
            val fallback = painterResource(id = R.drawable.fallback)

            val selectedImage = remember(event) { event.sortedImages.firstOrNull { it.id == null } }

            AsyncImage(
                model = selectedImage?.url,
                contentDescription = "${event.title} cover image", // TODO: Localize
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.FillWidth,
                alignment = Alignment.Center,
                placeholder = fallback,
                error = fallback,
                fallback = fallback,
            )

            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp),
            ) {
                Text(
                    text = event.title,
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(end = 8.dp, top = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )

                val locale = LocalConfiguration.current.locales[0]
                val formatter = DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "LLL-d"))

                val dateText = with(event.range) {
                    if (size > 1) {
                        val startDate = start.toJavaLocalDate().format(formatter)
                        val endDate = endInclusive.toJavaLocalDate().format(formatter)

                        "$startDate - $endDate"
                    } else {
                        start.toJavaLocalDate().format(formatter)
                    }
                }

                Text(
                    text = dateText,
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
            ) {
                Button(onClick = { navigateToDetails(event.calendarEvent) }) {
                    Text(stringResource(R.string.visit_page))
                }
            }
        }
    }
}