package com.makan.roadpulse.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * RoadPulse brand logo: a map-pin silhouette with an EKG heartbeat line
 * in place of the centre dot. Dark navy fill, teal stroke and pulse line.
 */
@Composable
fun RoadPulseLogo(modifier: Modifier = Modifier, size: Dp = 80.dp) {
    // Canvas is sized width=size, height=size*1.25 to give the pin its natural
    // teardrop proportions without clipping the bottom point.
    Canvas(modifier = modifier.size(size, size * 1.25f)) {
        val w  = size.toPx()
        val h  = w * 1.25f
        val cx = w / 2f

        // ── Location-pin silhouette ───────────────────────────────────────────
        val pinPath = Path().apply {
            moveTo(cx, h) // bottom tip
            cubicTo(
                cx - w * 0.05f, h * 0.65f,
                cx - w * 0.5f,  h * 0.45f,
                cx - w * 0.5f,  h * 0.35f
            )
            cubicTo(
                cx - w * 0.5f,  h * 0.1f,
                cx - w * 0.25f, 0f,
                cx,             0f
            )
            cubicTo(
                cx + w * 0.25f, 0f,
                cx + w * 0.5f,  h * 0.1f,
                cx + w * 0.5f,  h * 0.35f
            )
            cubicTo(
                cx + w * 0.5f,  h * 0.45f,
                cx + w * 0.05f, h * 0.65f,
                cx,             h
            )
            close()
        }
        drawPath(pinPath, color = Color(0xFF1A1A2E))
        drawPath(pinPath, color = Color(0xFF5DCAA5), style = Stroke(width = 2.dp.toPx()))

        // ── Heartbeat / EKG pulse line ────────────────────────────────────────
        val pulseY     = h * 0.35f
        val pulseWidth = w * 0.5f
        val startX     = cx - pulseWidth / 2f
        val pulsePath  = Path().apply {
            moveTo(startX, pulseY)
            lineTo(startX + pulseWidth * 0.25f, pulseY)
            lineTo(startX + pulseWidth * 0.35f, pulseY - h * 0.15f) // spike up
            lineTo(startX + pulseWidth * 0.5f,  pulseY + h * 0.15f) // spike down
            lineTo(startX + pulseWidth * 0.6f,  pulseY - h * 0.08f) // small echo
            lineTo(startX + pulseWidth * 0.7f,  pulseY)
            lineTo(startX + pulseWidth,          pulseY)
        }
        drawPath(
            pulsePath,
            color = Color(0xFF5DCAA5),
            style = Stroke(
                width = 2.5.dp.toPx(),
                cap   = StrokeCap.Round,
                join  = StrokeJoin.Round
            )
        )
    }
}
