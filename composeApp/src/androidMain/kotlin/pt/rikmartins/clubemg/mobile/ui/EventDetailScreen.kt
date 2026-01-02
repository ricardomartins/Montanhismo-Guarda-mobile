package pt.rikmartins.clubemg.mobile.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel
import pt.rikmartins.clubemg.mobile.R
import pt.rikmartins.clubemg.mobile.domain.entity.SimplifiedEvent

@Composable
fun EventDetailScreen(eventId: String, navigateBack: () -> Unit) {

    val viewModel: DetailViewModel = koinViewModel()

//    val event by viewModel.event.collectAsStateWithLifecycle()

    LaunchedEffect(eventId) {
        viewModel.setEventId(eventId)
    }

//    AnimatedContent(event != null) { objectAvailable ->
//        if (objectAvailable) {
//            EventDetails(event!!, onBackClick = navigateBack)
//        } else {
////            EmptyScreenContent(Modifier.fillMaxSize())
//        }
//    }
}

@Composable
private fun EventDetails(
    obj: SimplifiedEvent,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Merdas")},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.today_24),
                            contentDescription = stringResource(R.string.jump_to_today),
                        )
                    }
                }
            )
        },
        modifier = modifier.windowInsetsPadding(WindowInsets.systemBars),
    ) { paddingValues ->
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
        ) {
            HtmlContent(obj.url)
        }
    }
}

@Composable
fun HtmlContent(htmlSnippet: String) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // Settings to ensure the view behaves correctly
                settings.javaScriptEnabled = false

                // Ensures links open within the WebView, not an external browser
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            // loadDataWithBaseURL is better than loadData for handling special characters
            webView.loadUrl(htmlSnippet)
        }
    )
}