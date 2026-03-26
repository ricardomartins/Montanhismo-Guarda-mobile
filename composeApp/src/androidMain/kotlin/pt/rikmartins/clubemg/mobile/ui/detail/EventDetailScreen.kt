package pt.rikmartins.clubemg.mobile.ui.detail

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.ui.BookmarkToggleButton
import pt.rikmartins.clubemg.mobile.ui.DetailViewModel
import pt.rikmartins.clubemg.mobile.ui.LargeEventInfo
import pt.rikmartins.clubemg.mobile.ui.UiEventWithBookmark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    navigateBack: () -> Unit
) {
    val viewModel: DetailViewModel = koinViewModel()
    val event by viewModel.event.collectAsStateWithLifecycle()
    val refreshingEventIds by viewModel.refreshingEventIds.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(eventId) {
        viewModel.setEventId(eventId)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(event?.title ?: "") },
                    navigationIcon = {
                        IconButton(onClick = navigateBack) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.back_action),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                )
                // We keep the refreshing indicator here under the top bar
                AnimatedVisibility(
                    visible = event != null && refreshingEventIds.contains(event?.id),
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = event,
            label = "EventDetailContent",
            modifier = Modifier.padding(paddingValues)
        ) { eventVal ->
            if (eventVal != null) {
                EventDetails(
                    event = eventVal,
                    onVisitPageClick = { uriHandler.openUri(eventVal.url) },
                    onBookmarkClick = { viewModel.setBookmarkOfEventTo(it) }
                )
            } else {
                // Loading state could be here
            }
        }
    }
}

@Composable
private fun EventDetails(
    event: UiEventWithBookmark,
    onVisitPageClick: () -> Unit,
    onBookmarkClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
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
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    fun requestNotificationPermission() = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> {}
        hasNotificationPermission -> {}
        (context as? Activity)?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
        } == true -> showRationale = true
        else -> permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        val fallback = painterResource(id = R.drawable.fallback)
        val selectedImage = remember(event) { event.calendarEvent.images.firstOrNull { it.id == null } }

        AsyncImage(
            model = selectedImage?.url,
            contentDescription = "${event.title} cover image",
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

        LargeEventInfo(
            title = event.title,
            range = event.range,
            isBookmarked = false,
            categories = event.calendarEvent.taxonomies,
            eventStatus = event.calendarEvent.eventStatusType,
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
                onClick = onVisitPageClick
            ) {
                Text(stringResource(R.string.visit_page_action))
            }

            BookmarkToggleButton(
                isBookmarked = event.isBookmarked,
                setBookmark = {
                    if (it) requestNotificationPermission()
                    onBookmarkClick(it)
                }
            )
        }
    }

    if (showRationale) AlertDialog(
        onDismissRequest = { showRationale = false },
        icon = { Icon(painterResource(R.drawable.ic_notifications_off), null) },
        title = { Text(stringResource(R.string.notification_permission_rationale_title)) },
        text = { Text(stringResource(R.string.notification_permission_rationale_text)) },
        confirmButton = {
            TextButton(onClick = {
                showRationale = false
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
