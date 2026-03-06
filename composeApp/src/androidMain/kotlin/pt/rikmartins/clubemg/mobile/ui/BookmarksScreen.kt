package pt.rikmartins.clubemg.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.ScaffoldViewModel

@Composable
fun BookmarksScreen(
    scaffoldViewModel: ScaffoldViewModel,
    navigateToDetails: (event: UiEventWithBookmark) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val viewModel: BookmarksViewModel = koinViewModel()
    val selectedEvent by viewModel.selectedEvent.collectAsStateWithLifecycle()
    val bookmarkedEvents by viewModel.model.collectAsStateWithLifecycle()
    val refreshingEventIds by viewModel.refreshingEventIds.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        scaffoldViewModel.updateScaffold(
            title = { Text(stringResource(R.string.title_bookmarks)) },
        )
    }

    if (bookmarkedEvents.isEmpty()) EmptyState(
        headline = stringResource(R.string.empty_state_headline_bookmarks),
        description = stringResource(R.string.empty_state_description_bookmarks),
        iconRes = R.drawable.ic_bookmark_border
    ) else LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(all = 8.dp),
    ) {
        items(bookmarkedEvents, key = { it.id }) { event ->
            val snackBarMessage = stringResource(R.string.bookmarks_snackbar_unbookmarked_text, event.title)
            val snackBarUndoActionLabel = stringResource(R.string.undo_action)

            Bookmark(
                event,
                unsetBookmark = {
                    viewModel.unbookmarkEvent(event.id)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = snackBarMessage,
                            actionLabel = snackBarUndoActionLabel,
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.bookmarkEvent(event.id)
                    }
                },
                onEventClick = { viewModel.setSelectedEvent(it) },
                onSizeChanged = { viewModel.updateImageSize(it.width, it.height) },
                modifier = Modifier.animateItem()
            )
        }
    }

    val selectedEventVal = selectedEvent
    if (selectedEventVal != null) EventActionsDialog(
        event = selectedEventVal,
        refreshingEventIds = refreshingEventIds,
        navigateToDetails = {
            navigateToDetails(it)
            viewModel.unsetSelectedEvent()
        },
        setBookmarkTo = {
            if (it) viewModel.bookmarkEvent(selectedEventVal.id)
            else viewModel.unbookmarkEvent(selectedEventVal.id)
        },
    ) { viewModel.unsetSelectedEvent() }
}

private const val IMAGE_ASPECT_RATIO = 4758f / 6892f // Usual size for a poster

@Composable
private fun Bookmark(
    event: UiEventWithBookmark,
    unsetBookmark: () -> Unit,
    onEventClick: (event: UiEventWithBookmark) -> Unit,
    modifier: Modifier = Modifier,
    onSizeChanged: ((IntSize) -> Unit)? = null,
) {

    val categories = remember(event) { event.calendarEvent.taxonomies.toEventCategories() }

    Card(
        modifier = modifier
            .wrapContentHeight()
            .fillMaxWidth(),
        onClick = { onEventClick(event) }
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            AsyncImage(
                model = event.preferredImageUrl,
                contentDescription = "Cover image of event $event.title", // TODO: Localize
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(IMAGE_ASPECT_RATIO)
                    .clip(MaterialTheme.shapes.medium)
                    .run { if (onSizeChanged != null) onSizeChanged(onSizeChanged) else this },
                contentScale = ContentScale.Fit,
                alignment = Alignment.Center,
                placeholder = ColorPainter(MaterialTheme.colorScheme.tertiary),
                error = ColorPainter(MaterialTheme.colorScheme.tertiary),
                fallback = ColorPainter(MaterialTheme.colorScheme.tertiary),
            )

            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .padding(start = 12.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            ) {
                EventInfo(
                    title = event.title,
                    range = event.range,
                    isBookmarked = false,
                    categories = categories,
                    modifier = Modifier.weight(1f),
                )
                BookmarkToggleButton(
                    isBookmarked = event.isBookmarked,
                    setBookmark = { if (!it) unsetBookmark() },
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

