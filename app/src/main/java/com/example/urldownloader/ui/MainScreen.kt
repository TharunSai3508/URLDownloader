package com.example.urldownloader.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.urldownloader.download.Downloader
import com.example.urldownloader.model.MediaType
import com.example.urldownloader.ui.theme.AppColors
import com.example.urldownloader.viewmodel.MainViewModel
import com.example.urldownloader.viewmodel.UiState

// Gradient used across the app as the brand strip
private val BrandGradient = Brush.horizontalGradient(
    listOf(AppColors.Purple, AppColors.Blue, AppColors.Cyan)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {

    val context   = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val uiState   by vm.uiState.collectAsStateWithLifecycle()
    var url       by remember { mutableStateOf("") }

    // Handle shared-text intents
    val activity = context as? android.app.Activity
    LaunchedEffect(Unit) {
        val shared = activity?.intent
            ?.takeIf { it.action == android.content.Intent.ACTION_SEND }
            ?.getStringExtra(android.content.Intent.EXTRA_TEXT)
        if (!shared.isNullOrBlank()) { url = shared; vm.analyze(shared) }
    }

    Scaffold(
        topBar = {
            // Custom gradient top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandGradient)
                    .statusBarsPadding()
                    .height(56.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "URL Downloader",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxSize()
        ) {

            // ── URL input card ──────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Paste or enter URL") },
                        placeholder = { Text("https://example.com/video.mp4") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimatedVisibility(url.isNotEmpty()) {
                                    IconButton(onClick = { url = ""; vm.clear() }) {
                                        Icon(Icons.Default.Close, "Clear")
                                    }
                                }
                                IconButton(onClick = {
                                    clipboard.getText()?.text
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { url = it }
                                }) {
                                    Icon(Icons.Default.ContentPaste, "Paste")
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    // Gradient analyse button
                    val isLoading = uiState is UiState.Loading
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (url.isBlank() || isLoading)
                                    Brush.horizontalGradient(
                                        listOf(Color.Gray.copy(.4f), Color.Gray.copy(.3f))
                                    )
                                else BrandGradient
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { vm.analyze(url) },
                            enabled = url.isNotBlank() && !isLoading,
                            modifier = Modifier.fillMaxSize(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor  = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.ManageSearch, null, tint = Color.White)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isLoading) "Analyzing…" else "Analyze URL",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── State panel ─────────────────────────────────────────────────
            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "state_anim"
            ) { state ->
                when (state) {
                    is UiState.Idle    -> IdleState()
                    is UiState.Loading -> LoadingState()
                    is UiState.Error   -> ErrorState(state.message) { vm.analyze(url) }

                    is UiState.Success -> {
                        val availableTypes = vm.getAvailableTypes()
                        val activeFilter   = vm.getActiveFilter()

                        Column {
                            // Header row
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Count badge
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "${state.items.size} found",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(Modifier.weight(1f))

                                // Download all gradient button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BrandGradient),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FilledTonalButton(
                                        onClick = { Downloader.downloadAll(context, state.items) },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color.Transparent
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DownloadForOffline,
                                            null,
                                            tint = Color.White,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text(
                                            "Download All",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            // Filter chips
                            if (availableTypes.size > 1) {
                                Spacer(Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    item {
                                        GradientFilterChip(
                                            selected = activeFilter == null,
                                            label = "All",
                                            gradient = BrandGradient,
                                            onClick = { vm.setFilter(null) }
                                        )
                                    }
                                    items(availableTypes) { type ->
                                        GradientFilterChip(
                                            selected = activeFilter == type,
                                            label = type.name.lowercase().replaceFirstChar { it.uppercaseChar() },
                                            gradient = typeGradient(type),
                                            onClick = { vm.setFilter(type) }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(state.items, key = { it.url }) { media ->
                                    MediaItemView(media)
                                }
                                item { Spacer(Modifier.height(24.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Filter chip with active gradient fill ────────────────────────────────────
@Composable
private fun GradientFilterChip(
    selected: Boolean,
    label: String,
    gradient: Brush,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) gradient else Brush.horizontalGradient(
                listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
            ))
    ) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = {
                Text(
                    label,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor          = Color.Transparent,
                selectedContainerColor  = Color.Transparent,
                labelColor              = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedLabelColor      = Color.White
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor         = MaterialTheme.colorScheme.outlineVariant,
                selectedBorderColor = Color.Transparent
            )
        )
    }
}

// Gradient per media type (for chips)
private fun typeGradient(type: MediaType): Brush = Brush.horizontalGradient(
    when (type) {
        MediaType.IMAGE    -> listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))
        MediaType.GIF      -> listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))
        MediaType.VIDEO    -> listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
        MediaType.AUDIO    -> listOf(Color(0xFF00695C), Color(0xFF26A69A))
        MediaType.PDF      -> listOf(Color(0xFFC62828), Color(0xFFEF5350))
        MediaType.DOCUMENT -> listOf(Color(0xFF1976D2), Color(0xFF64B5F6))
        MediaType.ARCHIVE  -> listOf(Color(0xFFE65100), Color(0xFFFF8A65))
        MediaType.UNKNOWN  -> listOf(Color(0xFF546E7A), Color(0xFF90A4AE))
    }
)

// ── Idle / Loading / Error composables ───────────────────────────────────────

@Composable
private fun IdleState() {
    val inf = rememberInfiniteTransition(label = "idle")
    val pulse by inf.animateFloat(
        0.92f, 1.08f,
        infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(AppColors.Purple.copy(.18f), Color.Transparent)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier
                        .size(54.dp)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse },
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Paste any URL above",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "We'll scan it for images, videos,\naudio, PDFs, archives and more.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // Type pill row
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("🖼 Image", "🎬 Video", "🎵 Audio", "📄 PDF").forEach { t ->
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            t,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    val inf = rememberInfiniteTransition(label = "loading")
    val rot by inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "rot"
    )

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 56.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.sweepGradient(
                            listOf(AppColors.Purple, AppColors.Cyan, AppColors.Purple)
                        ),
                        CircleShape
                    )
                    .graphicsLayer { rotationZ = rot }
                    .padding(4.dp)
                    .background(MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(38.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    trackColor = Color.Transparent
                )
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Scanning for content…",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Detecting images, videos, audio & files",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ErrorOutline,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    message,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandGradient),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxSize(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
            ) {
                Icon(Icons.Default.Refresh, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Retry", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
