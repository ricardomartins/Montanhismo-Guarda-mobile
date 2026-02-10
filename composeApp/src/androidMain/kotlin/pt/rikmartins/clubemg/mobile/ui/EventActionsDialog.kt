package pt.rikmartins.clubemg.mobile.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import pt.rikmartins.clubemg.mobile.R

@Composable
fun EventActionsDialog(
    event: UiEventWithBookmark,
    refreshingEventIds: Collection<String>,
    navigateToDetails: (event: UiEventWithBookmark) -> Unit,
    setBookmarkTo: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var showRationale by remember { mutableStateOf(false) }

    val context = LocalContext.current

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true // For older APIs
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (!isGranted) {
            // Maybe warn the user that they may need to enable it manually
        }
    }

    fun requestNotificationPermission() = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> {} // No notification permission needed
        hasNotificationPermission -> {} // Already have permission
        (context as? Activity)?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
        } == true -> showRationale = true // Trigger rationale
        else -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) // TODO: Explain why we need this permission
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp)
        ) {
            val fallback = painterResource(id = R.drawable.fallback)
            val selectedImage = remember(event) { event.calendarEvent.images.firstOrNull { it.id == null } }

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

            EventInfo(
                title = event.title,
                range = event.range,
                isBookmarked = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp)
            )

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
            ) {
                Button(
                    colors = ButtonDefaults.textButtonColors(),
                    shape = ButtonDefaults.textShape,
                    onClick = { navigateToDetails(event) }
                ) {
                    Text(stringResource(R.string.visit_page_action))
                }

                BookmarkToggleButton(
                    isBookmarked = event.isBookmarked,
                    setBookmark = {
                        if (it) requestNotificationPermission()
                        setBookmarkTo(it)
                    }
                )
            }
            AnimatedVisibility(
                visible = refreshingEventIds.any { it == event.id },
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showRationale) AlertDialog(
        onDismissRequest = { showRationale = false },
        icon = { Icon(painterResource(R.drawable.ic_notifications_off), null) }, // FIXME: Localize
        title = { Text(stringResource(R.string.notification_permission_rationale_title)) },
        text = { Text(stringResource(R.string.notification_permission_rationale_text)) },
        confirmButton = {
            TextButton(onClick = {
                showRationale = false
                // Launch the permission request
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text(stringResource(R.string.notification_permission_rationale_positive_action)) }
        },
        dismissButton = {
            TextButton(onClick = { showRationale = false }) {
                Text(stringResource(R.string.notification_permission_rationale_negative_action))
            }
        }
    )
}
