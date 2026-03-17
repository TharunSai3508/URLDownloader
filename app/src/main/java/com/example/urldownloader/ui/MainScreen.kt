package com.example.urldownloader.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.urldownloader.download.Downloader
import com.example.urldownloader.model.MediaType
import com.example.urldownloader.viewmodel.MainViewModel
import com.example.urldownloader.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    var url by remember { mutableStateOf("") }

    // Handle shared intents
    val activity = context as? android.app.Activity
    LaunchedEffect(Unit) {
        val sharedText = activity?.intent
            ?.takeIf { it.action == android.content.Intent.ACTION_SEND }
            ?.getStringExtra(android.content.Intent.EXTRA_TEXT)
        if (!sharedText.isNullOrBlank()) {
            url = sharedText
            vm.analyze(sharedText)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("URL Downloader") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxSize()
        ) {
            // URL Input
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Paste or enter URL") },
                placeholder = { Text("https://example.com/...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedVisibility(visible = url.isNotEmpty()) {
                            IconButton(onClick = { url = ""; vm.clear() }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear URL")
                            }
                        }
                        IconButton(onClick = {
                            val text = clipboard.getText()?.text
                            if (!text.isNullOrBlank()) url = text
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste from clipboard")
                        }
                    }
                }
            )

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = { vm.analyze(url) },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank() && uiState !is UiState.Loading
            ) {
                Icon(Icons.Default.ManageSearch, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Analyze URL")
            }

            Spacer(Modifier.height(16.dp))

            when (val state = uiState) {

                is UiState.Idle -> IdleState()

                is UiState.Loading -> LoadingState()

                is UiState.Error -> ErrorState(message = state.message, onRetry = { vm.analyze(url) })

                is UiState.Success -> {
                    val availableTypes = vm.getAvailableTypes()
                    val activeFilter = vm.getActiveFilter()

                    // Header row: count + Download All
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.items.size} item(s) found",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = { Downloader.downloadAll(context, state.items) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.DownloadForOffline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Download All", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Filter chips
                    if (availableTypes.size > 1) {
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = activeFilter == null,
                                    onClick = { vm.setFilter(null) },
                                    label = { Text("All") }
                                )
                            }
                            items(availableTypes) { type ->
                                FilterChip(
                                    selected = activeFilter == type,
                                    onClick = { vm.setFilter(type) },
                                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercaseChar() }) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.items, key = { it.url }) { media ->
                            MediaItemView(media = media, context = context)
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun IdleState() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 60.dp)
        ) {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Paste any URL to find\ndownloadable content",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Images • Videos • Audio • PDFs • Archives",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outlineVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 60.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Analyzing URL…", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Scanning for all media content",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Retry")
        }
    }
}
