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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ── Brand palette ─────────────────────────────────────────────────────────────
private val Pink   = Color(0xFFFF006E)
private val Violet = Color(0xFF9500FF)
private val Blue   = Color(0xFF3D5AFE)
private val Cyan   = Color(0xFF00E5FF)
private val Teal   = Color(0xFF1DE9B6)

// ── Pre-computed particle data (constant across recompositions) ───────────────
private data class Particle(
    val x0: Float,       // initial fractional x  0..1
    val y0: Float,       // initial fractional y  0..1
    val vx: Float,       // x velocity  units-per-ms (fractional)
    val vy: Float,       // y velocity  units-per-ms (fractional), mostly upward
    val radiusDp: Float,
    val baseAlpha: Float,
    val color: Color,
    val phase: Float     // phase offset for alpha pulsing
)

private val PARTICLE_COLORS = listOf(
    Color(0xFFFF80AB), Cyan, Teal, Color(0xFFB47CFF), Color(0xFFFFFFFF), Blue
)

private val PARTICLES: List<Particle> = run {
    val rng = Random(0xC0FFEE)
    List(22) { i ->
        Particle(
            x0        = rng.nextFloat(),
            y0        = rng.nextFloat(),
            vx        = (rng.nextFloat() - 0.5f) * 0.000025f,
            vy        = -(rng.nextFloat() * 0.000022f + 0.000008f),
            radiusDp  = rng.nextFloat() * 4f + 1.5f,
            baseAlpha = rng.nextFloat() * 0.28f + 0.06f,
            color     = PARTICLE_COLORS[i % PARTICLE_COLORS.size],
            phase     = rng.nextFloat() * (2f * PI.toFloat())
        )
    }
}

@Composable
fun SplashScreen() {
    val inf = rememberInfiniteTransition(label = "splash")

    // ── Animated background gradient ─────────────────────────────────────────
    val bg1 by inf.animateColor(Pink,  Color(0xFF1A0040),
        infiniteRepeatable(tween(3400, easing = LinearEasing), RepeatMode.Reverse), "bg1")
    val bg2 by inf.animateColor(Cyan,  Color(0xFF6A00AA),
        infiniteRepeatable(tween(4200, 700, LinearEasing), RepeatMode.Reverse), "bg2")

    // ── Ripple rings ──────────────────────────────────────────────────────────
    val r1 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), "r1")
    val r2 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, 667, LinearEasing), RepeatMode.Restart), "r2")
    val r3 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2000, 1334, LinearEasing), RepeatMode.Restart), "r3")

    // ── Icon glow pulse ───────────────────────────────────────────────────────
    val glowA by inf.animateFloat(0.2f, 0.7f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), "ga")
    val glowS by inf.animateFloat(0.88f, 1.12f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), "gs")

    // ── Outer rotating arc ────────────────────────────────────────────────────
    val arcAngle by inf.animateFloat(0f, 360f,
        infiniteRepeatable(tween(2800, easing = LinearEasing)), "arc")
    val arcAngle2 by inf.animateFloat(360f, 0f,
        infiniteRepeatable(tween(4200, easing = LinearEasing)), "arc2")

    // ── Frame-clock for particle movement ─────────────────────────────────────
    var frameMs by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        val t0 = withFrameMillis { it }
        while (true) { withFrameMillis { frameMs = it - t0 } }
    }

    // ── One-shot entrance animations ─────────────────────────────────────────
    val logoScale  = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleY     = remember { Animatable(48f) }
    val sub1Alpha  = remember { Animatable(0f) }
    val sub2Alpha  = remember { Animatable(0f) }
    val dotsAlpha  = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 160f))
        titleY.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
        titleAlpha.animateTo(1f, tween(420))
        sub1Alpha.animateTo(1f, tween(340))
        sub2Alpha.animateTo(1f, tween(320))
        dotsAlpha.animateTo(1f, tween(500))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(bg1, bg2),
                    Offset.Zero,
                    Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        // ── Canvas layer: particles + ripple rings ────────────────────────────
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val t  = frameMs.toFloat()

            // Moving particles
            PARTICLES.forEach { p ->
                var x = ((p.x0 + p.vx * t) % 1f + 1f) % 1f
                var y = ((p.y0 + p.vy * t) % 1f + 1f) % 1f
                val a = (p.baseAlpha * (0.65f + 0.35f * sin(t * 0.0018f + p.phase)))
                    .coerceIn(0f, 1f)
                drawCircle(
                    color  = p.color.copy(alpha = a),
                    radius = p.radiusDp * density,
                    center = Offset(x * size.width, y * size.height)
                )
            }

            // Ripple rings expanding from icon centre
            fun ripple(progress: Float) = drawCircle(
                color  = Color.White.copy(alpha = (1f - progress) * 0.18f),
                radius = 80f + progress * 300f,
                center = Offset(cx, cy),
                style  = Stroke(1.6f)
            )
            ripple(r1); ripple(r2); ripple(r3)
        }

        // ── Main content ──────────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {

            // Icon cluster
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(156.dp)
            ) {
                // Outer pulsing glow disc
                Box(
                    modifier = Modifier
                        .size(156.dp)
                        .scale(glowS)
                        .background(
                            Brush.radialGradient(listOf(Cyan.copy(glowA), Color.Transparent)),
                            CircleShape
                        )
                )

                // Rotating outer arc (fast)
                androidx.compose.foundation.Canvas(Modifier.size(148.dp)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, Cyan.copy(.9f), Color.Transparent),
                            center
                        ),
                        startAngle = arcAngle,
                        sweepAngle = 200f,
                        useCenter  = false,
                        style      = Stroke(3.5f, cap = StrokeCap.Round)
                    )
                }

                // Rotating inner arc (slow, opposite direction)
                androidx.compose.foundation.Canvas(Modifier.size(116.dp)) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, Pink.copy(.8f), Violet.copy(.5f), Color.Transparent),
                            center
                        ),
                        startAngle = arcAngle2,
                        sweepAngle = 160f,
                        useCenter  = false,
                        style      = Stroke(2.5f, cap = StrokeCap.Round)
                    )
                }

                // Mid glow
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color.White.copy(.15f), Color.Transparent)
                            ),
                            CircleShape
                        )
                )

                // Icon circle — scale-in on entrance
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(logoScale.value)
                        .background(
                            Brush.linearGradient(
                                listOf(Color.White.copy(.22f), Color.White.copy(.07f))
                            ),
                            CircleShape
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color.White.copy(.95f), Cyan.copy(.7f),
                                    Pink.copy(.6f), Violet.copy(.5f), Color.White.copy(.95f)
                                )
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CloudDownload,
                        null,
                        Modifier.size(50.dp),
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Title
            Text(
                "URL Downloader",
                fontSize   = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White,
                letterSpacing = 0.6.sp,
                textAlign  = TextAlign.Center,
                modifier   = Modifier
                    .alpha(titleAlpha.value)
                    .offset(y = titleY.value.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Download any content from any URL",
                style   = MaterialTheme.typography.bodyLarge,
                color   = Color.White.copy(.85f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.alpha(sub1Alpha.value)
            )

            Spacer(Modifier.height(10.dp))

            // Feature pills
            Row(
                modifier = Modifier.alpha(sub2Alpha.value),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("🎬 Video" to Blue, "🎵 Audio" to Cyan,
                       "🖼 Image" to Violet, "📦 Files" to Pink).forEach { (lbl, col) ->
                    Box(
                        modifier = Modifier
                            .background(col.copy(.28f), RoundedCornerShape(20.dp))
                            .border(1.dp, col.copy(.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                    ) {
                        Text(lbl, style = MaterialTheme.typography.labelSmall,
                            color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(52.dp))

            // Bouncing loading dots
            LoadingDots(modifier = Modifier.alpha(dotsAlpha.value))
        }
    }
}

@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "dots")

    @Composable
    fun dot(delay: Int, color: Color) {
        val a by inf.animateFloat(
            0.2f, 1f,
            infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse,
                StartOffset(delay)),
            "d$delay"
        )
        val s by inf.animateFloat(
            0.7f, 1.2f,
            infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse,
                StartOffset(delay)),
            "ds$delay"
        )
        Box(
            modifier = Modifier
                .scale(s)
                .size(9.dp)
                .alpha(a)
                .background(color, CircleShape)
        )
    }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        dot(0, Cyan); dot(180, Pink); dot(360, Violet)
    }
}
