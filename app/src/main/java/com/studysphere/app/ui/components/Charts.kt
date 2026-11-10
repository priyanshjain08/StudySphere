package com.studysphere.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.studysphere.app.data.*
import com.studysphere.app.domain.*
import com.studysphere.app.util.*
import com.studysphere.app.ui.components.*
import com.studysphere.app.ui.navigation.goTab
import com.studysphere.app.viewmodel.StudyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BarChart(data: List<Pair<String, Int>>, modifier: Modifier = Modifier, height: Dp = 140.dp) {
    val max = (data.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        data.forEach { (label, v) ->
            val frac by animateFloatAsState(v.toFloat() / max, animationSpec = tween(600), label = "bar")
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (v > 0) Dates.duration(v) else "", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Box(
                    Modifier.fillMaxWidth().height((height * frac).coerceAtLeast(if (v > 0) 4.dp else 2.dp))
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (v > 0) Brush.verticalGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)) else SolidColor(track))
                )
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
fun LineChart(data: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    val max = (data.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    val line = MaterialTheme.colorScheme.primary
    val fillTop = line.copy(alpha = 0.25f)
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val w = size.width; val h = size.height
            drawLine(grid, Offset(0f, h), Offset(w, h), 2f)
            if (data.size >= 2) {
                val step = w / (data.size - 1)
                val pts = data.mapIndexed { i, d -> Offset(i * step, h - (d.second.toFloat() / max) * h * 0.85f - h * 0.05f) }
                val area = Path().apply {
                    moveTo(0f, h); pts.forEach { lineTo(it.x, it.y) }; lineTo(w, h); close()
                }
                drawPath(area, Brush.verticalGradient(listOf(fillTop, Color.Transparent)))
                val path = Path().apply { moveTo(pts[0].x, pts[0].y); pts.drop(1).forEach { lineTo(it.x, it.y) } }
                drawPath(path, line, style = Stroke(6f, cap = StrokeCap.Round))
                pts.forEach { drawCircle(line, 8f, it) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            data.forEach { Text(it.first, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun DonutChart(slices: List<Triple<String, Int, Int>>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.second }.coerceAtLeast(1)
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Canvas(Modifier.size(130.dp)) {
            var start = -90f
            val s = 26.dp.toPx()
            val arc = Size(size.width - s, size.height - s)
            slices.forEach { sl ->
                val sweep = 360f * sl.second / total
                drawArc(Color(sl.third), start, (sweep - 1.5f).coerceAtLeast(0.5f), false, Offset(s / 2, s / 2), arc, style = Stroke(s))
                start += sweep
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            slices.take(6).forEach { sl ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubjectDot(sl.third)
                    Text("${sl.first} · ${Dates.duration(sl.second)}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun SplitBar(done: Int, pending: Int) {
    val total = done + pending
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (total == 0) {
            Text("No tasks yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))) {
                if (done > 0) Box(Modifier.weight(done.toFloat()).fillMaxHeight().background(Color(0xFF34D399)))
                if (pending > 0) Box(Modifier.weight(pending.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Completed: $done", style = MaterialTheme.typography.bodySmall)
                Text("Pending: $pending", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
