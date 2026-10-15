package com.example.bluetoothfinder.ui.screen.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.bluetoothfinder.domain.SignalCategory
import com.example.bluetoothfinder.ui.theme.color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Visualisasi radar kedekatan perangkat target.
 *
 * @param fraction posisi target dari pusat (0 = sangat dekat, 1 = batas jangkauan).
 * @param category kategori sinyal -> menentukan warna indikator.
 * @param lost bila true target berwarna redup (sinyal hilang / di luar jangkauan).
 */
@Composable
fun RadarView(
    fraction: Float,
    category: SignalCategory,
    lost: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = category.color
    val outline = MaterialTheme.colorScheme.outlineVariant
    val centerColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    val transition = rememberInfiniteTransition(label = "radar")

    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse",
    )

    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0.05f, 0.97f),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 60f),
        label = "targetRadius",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Latar lingkar luar
        drawCircle(color = accent.copy(alpha = if (lost) 0.05f else 0.10f), radius = radius, center = center)

        // Cincin jarak
        for (i in 1..4) {
            drawCircle(
                color = outline,
                radius = radius * i / 4f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
        }

        // Crosshair
        val lineStroke = 1.dp.toPx()
        drawLine(outline, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), lineStroke)
        drawLine(outline, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), lineStroke)

        // Gelombang pulse
        val pulseAlpha = if (lost) 0.15f else 0.45f
        listOf(0f, 0.5f).forEach { offset ->
            val p = (pulse + offset) % 1f
            drawCircle(
                color = accent.copy(alpha = (1f - p) * pulseAlpha),
                radius = radius * (0.15f + 0.85f * p),
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        // Garis pemindai (sweep)
        val sweepRad = Math.toRadians(sweepAngle.toDouble())
        val sweepEnd = Offset(
            center.x + cos(sweepRad).toFloat() * radius,
            center.y + sin(sweepRad).toFloat() * radius,
        )
        drawLine(
            color = accent.copy(alpha = if (lost) 0.2f else 0.6f),
            start = center,
            end = sweepEnd,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        // Ponsel pengguna di pusat
        drawCircle(color = centerColor, radius = 9.dp.toPx(), center = center)
        drawCircle(
            color = centerColor.copy(alpha = 0.35f),
            radius = 15.dp.toPx(),
            center = center,
            style = Stroke(width = 2.dp.toPx()),
        )

        // Perangkat target
        val targetAngle = Math.toRadians(-58.0)
        val targetRadius = radius * animatedFraction
        val targetCenter = Offset(
            center.x + cos(targetAngle).toFloat() * targetRadius,
            center.y + sin(targetAngle).toFloat() * targetRadius,
        )
        val targetAlpha = if (lost) 0.55f else 1f

        drawCircle(
            color = accent.copy(alpha = 0.28f * targetAlpha),
            radius = 22.dp.toPx(),
            center = targetCenter,
        )
        drawCircle(
            color = accent.copy(alpha = targetAlpha),
            radius = 7.dp.toPx(),
            center = targetCenter,
        )
        drawCircle(
            color = surfaceColor.copy(alpha = 0.9f * targetAlpha),
            radius = 3.dp.toPx(),
            center = targetCenter,
        )
    }
}
