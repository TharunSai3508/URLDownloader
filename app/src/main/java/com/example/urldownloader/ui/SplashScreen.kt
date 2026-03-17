package com.example.urldownloader.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand colors used in the splash (match the icon gradient)
private val SplashPurple = Color(0xFF7C4DFF)
private val SplashBlue   = Color(0xFF3D5AFE)
private val SplashCyan   = Color(0xFF00BCD4)
private val SplashTeal   = Color(0xFF1DE9B6)

@Composable
fun SplashScreen() {
    val inf = rememberInfiniteTransition(label = "splash_inf")

    // ── Animated background gradient colours ────────────────────────────────
    val bg1 by inf.animateColor(
        initialValue = SplashPurple,
        targetValue  = Color(0xFF1A237E),
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Reverse),
        label = "bg1"
    )
    val bg2 by inf.animateColor(
        initialValue = SplashCyan,
        targetValue  = Color(0xFF9C27B0),
        animationSpec = infiniteRepeatable(tween(4100, 600, LinearEasing), RepeatMode.Reverse),
        label = "bg2"
    )

    // ── Pulsing ripple rings (3 rings with offset phases) ───────────────────
    val ring1 by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "r1")
    val ring2 by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1800, 600, LinearEasing), RepeatMode.Restart), label = "r2")
    val ring3 by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(1800, 1200, LinearEasing), RepeatMode.Restart), label = "r3")

    // ── Floating ambient orbs ────────────────────────────────────────────────
    val orb1 by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Reverse), label = "o1")
    val orb2 by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(5200, 900, LinearEasing), RepeatMode.Reverse), label = "o2")
    val orb3 by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(3700, 1800, LinearEasing), RepeatMode.Reverse), label = "o3")
    val orb4 by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(4600, 2500, LinearEasing), RepeatMode.Reverse), label = "o4")

    // ── Icon glow pulse ──────────────────────────────────────────────────────
    val glowAlpha by inf.animateFloat(0.25f, 0.65f,
        infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "glow")
    val glowScale by inf.animateFloat(0.92f, 1.08f,
        infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "glowS")

    // ── Shimmer highlight on icon ring ───────────────────────────────────────
    val shimmer by inf.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "sh")

    // ── Entrance animations (one-shot) ───────────────────────────────────────
    val logoScale  = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleSlide = remember { Animatable(40f) }
    val sub1Alpha  = remember { Animatable(0f) }
    val sub2Alpha  = remember { Animatable(0f) }
    val dotsAlpha  = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 180f))
        titleSlide.animateTo(0f,  tween(380, easing = FastOutSlowInEasing))
        titleAlpha.animateTo(1f,  tween(380))
        sub1Alpha.animateTo(1f,   tween(300))
        sub2Alpha.animateTo(1f,   tween(300))
        dotsAlpha.animateTo(1f,   tween(500))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = listOf(bg1, bg2),
                start = Offset.Zero, end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY))),
        contentAlignment = Alignment.Center
    ) {

        // ── Background canvas: orbs + ripple rings ───────────────────────────
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Ambient orbs
            drawCircle(Color.White.copy(alpha = 0.055f), 230f,
                Offset(size.width * 0.12f, size.height * 0.18f + orb1 * 50f))
            drawCircle(Color.White.copy(alpha = 0.045f), 170f,
                Offset(size.width * 0.88f, size.height * 0.25f - orb2 * 35f))
            drawCircle(Color.White.copy(alpha = 0.07f),  120f,
                Offset(size.width * 0.78f, size.height * 0.78f + orb3 * 28f))
            drawCircle(Color.White.copy(alpha = 0.038f), 260f,
                Offset(size.width * 0.08f, size.height * 0.82f - orb4 * 22f))
            drawCircle(SplashTeal.copy(alpha = 0.12f),   90f,
                Offset(size.width * 0.92f, size.height * 0.62f + orb1 * 18f))

            // Ripple rings from icon centre
            fun ripple(p: Float) {
                drawCircle(
                    color  = Color.White.copy(alpha = (1f - p) * 0.22f),
                    radius = 90f + p * 280f,
                    center = Offset(cx, cy),
                    style  = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.8f)
                )
            }
            ripple(ring1); ripple(ring2); ripple(ring3)
        }

        // ── Main content column ──────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {

            // Icon cluster
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(148.dp)
            ) {
                // Outer glow disc
                Box(
                    modifier = Modifier
                        .size(148.dp)
                        .scale(glowScale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    SplashCyan.copy(alpha = glowAlpha),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                // Mid glow disc
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                // Icon circle with gradient border
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(logoScale.value)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.20f),
                                    Color.White.copy(alpha = 0.08f)
                                )
                            ),
                            CircleShape
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.9f),
                                    SplashCyan.copy(alpha = 0.6f),
                                    SplashPurple.copy(alpha = 0.4f),
                                    Color.White.copy(alpha = 0.9f)
                                ),
                                center = Offset(shimmer, shimmer)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(50.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // App title
            Text(
                text = "URL Downloader",
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .offset(y = titleSlide.value.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Subtitle 1
            Text(
                text = "Download anything from any URL",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(sub1Alpha.value)
            )

            Spacer(Modifier.height(6.dp))

            // Subtitle 2 – feature pill row
            Row(
                modifier = Modifier.alpha(sub2Alpha.value),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Images", "Videos", "Audio", "Files").forEach { label ->
                    Box(
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.18f),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            // Loading dots
            LoadingDots(modifier = Modifier.alpha(dotsAlpha.value))
        }
    }
}

@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "dots")

    @Composable
    fun dotAlpha(delay: Int) = inf.animateFloat(
        initialValue = 0.25f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delay)
        ),
        label = "dot$delay"
    ).value

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(0, 200, 400).forEach { delay ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(dotAlpha(delay))
                    .background(Color.White, CircleShape)
            )
        }
    }
}
