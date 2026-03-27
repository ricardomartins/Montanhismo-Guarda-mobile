package pt.rikmartins.clubemg.mobile.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import pt.rikmartins.clubemg.mobile.R

/**
 * A composable that handles notification permission request flow.
 * It provides a `requestPermission` lambda to its content.
 * When this lambda is called, it either requests permission directly
 * or shows a rationale dialog if needed.
 *
 * @param content The content that can trigger the permission request.
 * It receives a lambda that should be called to start the permission flow.
 */
@Composable
fun NotificationPermissionHandler(content: @Composable (requestPermission: () -> Unit) -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasPermission = isGranted }

    val requestPermission: () -> Unit = {
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> { /* Do nothing, permission is granted by default */
            }

            hasPermission -> { /* Already granted */
            }

            (context as? Activity)?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true -> {
                showRationale = true
            }

            else -> launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    content(requestPermission)

    if (showRationale) AlertDialog(
        onDismissRequest = { showRationale = false },
        icon = { Icon(painterResource(R.drawable.ic_notifications_off), null) },
        title = { Text(stringResource(R.string.notification_permission_rationale_title)) },
        text = { Text(stringResource(R.string.notification_permission_rationale_text)) },
        confirmButton = {
            TextButton(onClick = {
                showRationale = false
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text(stringResource(R.string.notification_permission_rationale_positive_action)) }
        },
        dismissButton = {
            TextButton(onClick = { showRationale = false }) {
                Text(stringResource(R.string.notification_permission_rationale_negative_action))
            }
        }
    )
}
