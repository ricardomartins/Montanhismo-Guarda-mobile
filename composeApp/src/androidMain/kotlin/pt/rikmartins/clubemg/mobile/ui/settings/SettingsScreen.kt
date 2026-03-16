package pt.rikmartins.clubemg.mobile.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.oss.licenses.v2.OssLicensesMenuActivity
import pt.rikmartins.clubemg.mobile.BuildConfig
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.ui.ScaffoldViewModel
import kotlin.jvm.java

@Composable
fun SettingsScreen(scaffoldViewModel: ScaffoldViewModel) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        scaffoldViewModel.updateScaffold(
            title = { Text(stringResource(R.string.settings_title)) },
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.settings_about_section_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                // App Version Item
                SettingsItem(
                    title = stringResource(R.string.settings_about_section_version_title),
                    subtitle = BuildConfig.VERSION_NAME
                )

                // Open Source Licenses Item
                SettingsItem(
                    title = stringResource(R.string.settings_about_section_open_source_licenses_title),
                    subtitle = stringResource(R.string.settings_about_section_open_source_licenses_subtitle),
                    onClick = {
                        // Start the activity that shows the licenses
                        context.startActivity(
                            Intent(
                                context,
                                OssLicensesMenuActivity::class.java
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, onClick: (() -> Unit)? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = title, fontWeight = FontWeight.SemiBold)
        Text(text = subtitle, fontSize = 14.sp)
    }
}