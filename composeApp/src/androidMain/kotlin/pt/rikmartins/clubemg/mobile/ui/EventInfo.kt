package pt.rikmartins.clubemg.mobile.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateRange
import kotlinx.datetime.toJavaLocalDate
import pt.rikmartins.clubemg.mobile.R
import java.time.format.DateTimeFormatter

@Composable
fun EventInfo(title: String?, range: LocalDateRange?, isBookmarked: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (title != null) Text(
                text = title,
                modifier = Modifier.padding(end = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
            )

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
                Text(
                    text = dateText,
                    modifier = Modifier.padding(end = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (isBookmarked) Icon(
            painter = painterResource(R.drawable.ic_bookmark),
            contentDescription = null, // TODO: Localize
            tint = MaterialTheme.colorScheme.primary
        )
    }
}