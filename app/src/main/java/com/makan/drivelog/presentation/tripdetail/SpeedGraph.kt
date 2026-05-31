package com.makan.drivelog.presentation.tripdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makan.drivelog.domain.model.LocationPoint

private val TEAL  = Color(0xFF5DCAA5)
private val CORAL = Color(0xFFD85A30)
private val MUTED = Color(0xFF888888)

@Composable
fun SpeedGraph(
    points: List<LocationPoint>,
    scrubIndex: Int,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    val speeds = remember(points) { points.map { (it.speed * 3.6f).toFloat() } }
    val maxSpeed = remember(speeds) { (speeds.maxOrNull() ?: 1f) * 1.1f }
    val bgColor = MaterialTheme.colorScheme.surfaceVariant

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
    ) {
        val hPad = 36.dp.toPx()
        val topPad = 24.dp.toPx()
        val botPad = 22.dp.toPx()
        val gw = size.width - 2 * hPad
        val gh = size.height - topPad - botPad

        if (gw <= 0 || gh <= 0) return@Canvas
        drawSpeedFill(speeds, maxSpeed, hPad, topPad, gw, gh)
        drawSpeedLine(speeds, maxSpeed, hPad, topPad, gw, gh)
        drawScrubLine(scrubIndex, speeds.size, hPad, topPad, gw, gh)
        drawLabels(speeds, maxSpeed)
    }
}

private fun DrawScope.drawSpeedFill(
    speeds: List<Float>, maxSpeed: Float,
    hPad: Float, topPad: Float, gw: Float, gh: Float
) {
    val path = Path()
    speeds.forEachIndexed { i, spd ->
        val x = hPad + (i.toFloat() / (speeds.size - 1)) * gw
        val y = topPad + gh - (spd / maxSpeed).coerceIn(0f, 1f) * gh
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.lineTo(hPad + gw, topPad + gh)
    path.lineTo(hPad, topPad + gh)
    path.close()
    drawPath(path, color = TEAL.copy(alpha = 0.12f))
}

private fun DrawScope.drawSpeedLine(
    speeds: List<Float>, maxSpeed: Float,
    hPad: Float, topPad: Float, gw: Float, gh: Float
) {
    val path = Path()
    speeds.forEachIndexed { i, spd ->
        val x = hPad + (i.toFloat() / (speeds.size - 1)) * gw
        val y = topPad + gh - (spd / maxSpeed).coerceIn(0f, 1f) * gh
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color = TEAL, style = Stroke(
        width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round
    ))
}

private fun DrawScope.drawScrubLine(
    scrubIndex: Int, count: Int,
    hPad: Float, topPad: Float, gw: Float, gh: Float
) {
    val x = hPad + (scrubIndex.toFloat() / (count - 1).coerceAtLeast(1)) * gw
    val interval = floatArrayOf(8f, 5f)
    var y = topPad
    var draw = true
    while (y < topPad + gh) {
        val segEnd = (y + interval[if (draw) 0 else 1]).coerceAtMost(topPad + gh)
        if (draw) {
            drawLine(CORAL, Offset(x, y), Offset(x, segEnd), strokeWidth = 1.5.dp.toPx())
        }
        y = segEnd
        draw = !draw
    }
}

private fun DrawScope.drawLabels(speeds: List<Float>, maxSpeed: Float) {
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        color = MUTED.toArgb()
        textSize = 8.sp.toPx()
    }
    val maxLabel = "${maxSpeed.toInt()} km/h"
    drawContext.canvas.nativeCanvas.drawText(maxLabel, 4f, 16.dp.toPx(), paint)
    drawContext.canvas.nativeCanvas.drawText("0", 4f, size.height - 6.dp.toPx(), paint)
}
