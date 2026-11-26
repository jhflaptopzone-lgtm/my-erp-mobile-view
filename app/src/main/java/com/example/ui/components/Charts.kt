package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MonthlySalePoint
import com.example.data.model.PaymentMethodCount
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningYellow
import com.example.util.Formatters

@Composable
fun MonthlySalesLineChart(
    points: List<MonthlySalePoint>,
    modifier: Modifier = Modifier
) {
    val ordered = points.reversed()
    if (ordered.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(170.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No monthly sales recorded yet", color = TextMuted, fontSize = 13.sp)
        }
        return
    }

    val maxVal = (ordered.maxOfOrNull { it.total } ?: 1.0).coerceAtLeast(1.0)
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
        ) {
            val padH = 24f
            val padV = 20f
            val chartW = (size.width - padH * 2).coerceAtLeast(10f)
            val chartH = (size.height - padV * 2).coerceAtLeast(10f)

            // Horizontal gridlines
            for (i in 0..3) {
                val y = padV + chartH * (i / 3f)
                drawLine(
                    color = DarkBorder,
                    start = Offset(padH, y),
                    end = Offset(size.width - padH, y),
                    strokeWidth = 2f
                )
            }

            val coords = ordered.mapIndexed { idx, pt ->
                val x = if (ordered.size == 1) size.width / 2f
                else padH + chartW * (idx.toFloat() / (ordered.size - 1))
                val ratio = (pt.total / maxVal).toFloat().coerceIn(0f, 1f)
                val y = padV + chartH * (1f - ratio)
                Offset(x, y)
            }

            if (coords.size >= 2) {
                val fillPath = Path().apply {
                    moveTo(coords.first().x, padV + chartH)
                    coords.forEach { lineTo(it.x, it.y) }
                    lineTo(coords.last().x, padV + chartH)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(NeonGreen.copy(alpha = 0.25f), Color.Transparent),
                        startY = padV,
                        endY = padV + chartH
                    )
                )

                val linePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        lineTo(coords[i].x, coords[i].y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = NeonGreen,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )
            }

            coords.forEach { pt ->
                drawCircle(color = Color.Black, radius = 9f, center = pt)
                drawCircle(color = NeonGreen, radius = 6f, center = pt)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ordered.forEach { pt ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(pt.month, color = TextMuted, fontSize = 10.sp)
                    Text(
                        Formatters.formatNumber(pt.total),
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentDistributionChart(
    methods: List<PaymentMethodCount>,
    modifier: Modifier = Modifier
) {
    if (methods.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No payment distribution data yet", color = TextMuted, fontSize = 13.sp)
        }
        return
    }

    val palette = listOf(NeonGreen, InfoBlue, WarningYellow, PurpleAccent)
    val total = methods.sumOf { it.count }.coerceAtLeast(1)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Canvas(modifier = Modifier.size(135.dp)) {
            val diameter = size.minDimension - 16f
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            var startAngle = -90f
            methods.forEachIndexed { idx, item ->
                val sweep = (item.count.toFloat() / total.toFloat()) * 360f
                drawArc(
                    color = palette[idx % palette.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = 28f)
                )
                startAngle += sweep
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            methods.forEachIndexed { idx, item ->
                val color = palette[idx % palette.size]
                val pct = ((item.count * 100.0) / total).toInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${item.paymentType}: ${item.count} ($pct%)",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
