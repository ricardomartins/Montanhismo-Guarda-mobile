package pt.rikmartins.clubemg.mobile.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.permission.NotificationPermissionHandler
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

    LaunchedEffect(eventId) { viewModel.setEventId(eventId) }

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
                    ),
                )
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
        val eventVal = event
        if (eventVal != null) EventDetails(
            event = eventVal,
            onVisitPageClick = { uriHandler.openUri(eventVal.url) },
            onBookmarkClick = { viewModel.setBookmarkOfEventTo(it) },
            modifier = Modifier.padding(paddingValues),
        ) else {
            // Loading state could be here
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
    NotificationPermissionHandler { requestPermission ->
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        var showPinchHint by remember { mutableStateOf(false) }
        var size by remember { mutableStateOf(IntSize.Zero) }

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                        if (scale > 1f) {
                            val maxX = (size.width * (scale - 1)) / 2
                            val maxY = (size.height * (scale - 1)) / 2
                            offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                            offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }

                        // Dismiss hint immediately when user starts interacting
                        if (zoom != 1f || pan.x != 0f || pan.y != 0f) {
                            showPinchHint = false
                        }
                    }
                }
        ) {
            val fallback = painterResource(id = R.drawable.fallback)
            val selectedImage = remember(event) { event.calendarEvent.images.firstOrNull { it.id == null } }


            var hintPlayed by remember { mutableStateOf(false) }

            LaunchedEffect(showPinchHint) {
                if (showPinchHint) {
                    delay(2500) // Show for a couple of seconds
                    showPinchHint = false
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .onSizeChanged { size = it }
                    .zIndex(if (scale > 1f) 1f else 0f)
            ) {
                AsyncImage(
                    model = selectedImage?.url,
                    contentDescription = "${event.title} cover image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .animateContentSize(),
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.Center,
                    placeholder = fallback,
                    error = fallback,
                    fallback = fallback,
                    onSuccess = {
                            if (!hintPlayed) {
                                showPinchHint = true
                                hintPlayed = true
                            }
                    }
                )

                // The Pinch Overlay Hint
                androidx.compose.animation.AnimatedVisibility(
                    visible = showPinchHint,
                    enter = fadeIn(tween(1000)),
                    exit = fadeOut(tween(1000)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                shape = CircleShape
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.pinch),
                            contentDescription = "Pinch to zoom hint",
                            modifier = Modifier.size(96.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = scale <= ZOOM_FOCUS_THRESHOLD,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                Column {
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
                        ) { Text(stringResource(R.string.visit_page_action)) }

                        BookmarkToggleButton(
                            isBookmarked = event.isBookmarked,
                            setBookmark = {
                                if (it) requestPermission()
                                onBookmarkClick(it)
                            }
                        )
                    }
                }
            }
        }
    }
}

private const val MAX_ZOOM = 4.0f
private const val ZOOM_FOCUS_THRESHOLD = 1.1f
