package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.model.PetalStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class PetalParticle(
    val xFraction: Float,
    val startOffset: Float,
    val speedMultiplier: Float,
    val driftPx: Float,
    val sizeScale: Float,
    val initialRotation: Float,
    val spinDir: Float,
    val opacityScale: Float
)

@Composable
fun FloatingPetalsBackground(
    enabled: Boolean,
    style: PetalStyle,
    sizeDp: Int,
    opacityPct: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    val particles = remember {
        val rng = Random(2026)
        List(18) {
            PetalParticle(
                xFraction = rng.nextFloat() * 0.94f + 0.03f,
                startOffset = rng.nextFloat(),
                speedMultiplier = 0.75f + rng.nextFloat() * 0.65f,
                driftPx = (if (rng.nextBoolean()) 1f else -1f) * (20f + rng.nextFloat() * 38f),
                sizeScale = 0.78f + rng.nextFloat() * 0.44f,
                initialRotation = rng.nextFloat() * 360f,
                spinDir = if (rng.nextBoolean()) 1f else -1f,
                opacityScale = 0.8f + rng.nextFloat() * 0.35f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "petals")
    val masterProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "petal_fall"
    )

    val baseAlpha = (opacityPct.coerceIn(8, 80) / 100f) * if (isDark) 0.92f else 1f

    Canvas(modifier = modifier.fillMaxSize()) {
        val baseSizePx = sizeDp.coerceIn(12, 42) * density
        val w = size.width
        val h = size.height

        particles.forEach { p ->
            val t = (masterProgress * p.speedMultiplier + p.startOffset) % 1f
            val y = -baseSizePx + t * (h + baseSizePx * 2f)
            val sway = sin(t * 2.0 * PI).toFloat() * p.driftPx
            val x = p.xFraction * w + sway
            val rot = p.initialRotation + p.spinDir * t * 360f
            val alpha = (baseAlpha * p.opacityScale).coerceIn(0.04f, 0.85f)
            val flowerSize = baseSizePx * p.sizeScale

            // Fade smoothly near top and bottom edges
            val edgeFade = when {
                t < 0.1f -> t / 0.1f
                t > 0.88f -> (1f - t) / 0.12f
                else -> 1f
            }.coerceIn(0f, 1f)

            val finalAlpha = alpha * edgeFade
            if (finalAlpha > 0.01f) {
                rotate(degrees = rot, pivot = Offset(x, y)) {
                    drawFlower(style = style, center = Offset(x, y), radius = flowerSize * 0.5f, alpha = finalAlpha)
                }
            }
        }
    }
}

private fun DrawScope.drawFlower(
    style: PetalStyle,
    center: Offset,
    radius: Float,
    alpha: Float
) {
    when (style) {
        PetalStyle.SAKURA -> {
            val petalColor = Color(0xFFFB7185).copy(alpha = alpha * 0.8f)
            val innerPetalColor = Color(0xFFF472B6).copy(alpha = alpha * 0.72f)
            val coreColor = Color(0xFFFBBF24).copy(alpha = alpha * 0.9f)
            for (i in 0 until 5) {
                val angle = (i * 72f)
                rotate(degrees = angle, pivot = center) {
                    val p = Path().apply {
                        moveTo(center.x, center.y)
                        cubicTo(
                            center.x - radius * 0.45f, center.y - radius * 0.45f,
                            center.x - radius * 0.35f, center.y - radius * 1.05f,
                            center.x, center.y - radius * 0.92f
                        )
                        cubicTo(
                            center.x + radius * 0.35f, center.y - radius * 1.05f,
                            center.x + radius * 0.45f, center.y - radius * 0.45f,
                            center.x, center.y
                        )
                    }
                    drawPath(p, color = if (i % 2 == 0) petalColor else innerPetalColor)
                }
            }
            drawCircle(color = coreColor, radius = radius * 0.18f, center = center)
        }

        PetalStyle.ROSE -> {
            val outer = Color(0xFFE11D48).copy(alpha = alpha * 0.68f)
            val inner = Color(0xFFF43F5E).copy(alpha = alpha * 0.85f)
            drawCircle(color = outer, radius = radius * 0.88f, center = center)
            drawCircle(color = inner, radius = radius * 0.56f, center = Offset(center.x - radius * 0.08f, center.y - radius * 0.06f))
            drawCircle(color = Color(0xFFFDA4AF).copy(alpha = alpha * 0.75f), radius = radius * 0.25f, center = center)
        }

        PetalStyle.LOTUS -> {
            val outerColor = Color(0xFFEC4899).copy(alpha = alpha * 0.68f)
            val midColor = Color(0xFFF9A8D4).copy(alpha = alpha * 0.82f)
            val angles = listOf(-48f, -24f, 0f, 24f, 48f)
            angles.forEachIndexed { idx, deg ->
                rotate(degrees = deg, pivot = Offset(center.x, center.y + radius * 0.35f)) {
                    val p = Path().apply {
                        moveTo(center.x, center.y + radius * 0.35f)
                        quadraticTo(center.x - radius * 0.38f, center.y - radius * 0.25f, center.x, center.y - radius * 0.9f)
                        quadraticTo(center.x + radius * 0.38f, center.y - radius * 0.25f, center.x, center.y + radius * 0.35f)
                    }
                    drawPath(p, color = if (idx == 2) midColor else outerColor)
                }
            }
        }

        PetalStyle.BLOSSOM -> {
            val petalColor = Color(0xFFFDE047).copy(alpha = alpha * 0.82f)
            val coreColor = Color(0xFFF59E0B).copy(alpha = alpha * 0.95f)
            for (i in 0 until 5) {
                val rad = (i * 72.0 * PI / 180.0)
                val px = center.x + cos(rad).toFloat() * radius * 0.48f
                val py = center.y + sin(rad).toFloat() * radius * 0.48f
                drawCircle(color = petalColor, radius = radius * 0.36f, center = Offset(px, py))
            }
            drawCircle(color = coreColor, radius = radius * 0.26f, center = center)
        }
    }
}
