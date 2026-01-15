package pt.rikmartins.clubemg.mobile.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import org.koin.androidx.compose.koinViewModel
import pt.rikmartins.clubemg.mobile.R

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
                            painter = painterResource(id = R.drawable.ic_today),
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