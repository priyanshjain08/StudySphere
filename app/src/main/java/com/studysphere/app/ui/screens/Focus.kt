package com.studysphere.app.ui.screens

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
import com.studysphere.app.focus.TimerService

@Composable
fun FocusScreen(vm: StudyViewModel) {
    val ts by vm.timer.state.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val subjectsS by vm.subjects.collectAsStateWithLifecycle()
    val focusS by vm.focus.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    var subject by remember { mutableStateOf<Long?>(null) }
    var label by remember { mutableStateOf("") }
    var custom by remember { mutableStateOf("45") }

    // Keeps the UI in sync while visible; the foreground service keeps the timer alive in the background.
    LaunchedEffect(ts.running) { while (ts.running) { vm.timer.tick(); delay(500) } }

    fun modeSeconds(mode: Int): Int {
        val minutes = when (mode) {
            0 -> prefs.focusMin
            1 -> prefs.shortMin
            2 -> prefs.longMin
            else -> (custom.toIntOrNull() ?: 45).coerceIn(1, 300)
        }
        return minutes * 60
    }

    val today = Dates.today()
    val todays = (focusS ?: emptyList()).filter { it.day == today && (it.mode == 0 || it.mode == 3) }
    val subjects = subjectsS ?: emptyList()
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        1f, 1.03f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "scale"
    )

    Screen("Focus") {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ChoiceChips(Constants.MODE_NAMES, ts.mode, { if (!ts.active) vm.timer.select(it, modeSeconds(it)) })
            if (ts.mode == 3 && !ts.active) {
                Field(custom, { custom = it.filter { c -> c.isDigit() }.take(3); vm.timer.select(3, modeSeconds(3)) },
                    "Custom duration (minutes)", keyboardType = KeyboardType.Number)
            }
            ProgressRing(
                TimerMath.progress(ts.totalSec, ts.remainingSec), 280.dp, Modifier.scale(if (ts.running) pulse else 1f), stroke = 18.dp,
                track = MaterialTheme.colorScheme.surfaceVariant,
                colors = if (ts.mode == 1 || ts.mode == 2) listOf(Color(0xFF34D399), Color(0xFF2DD4BF)) else listOf(Color(0xFF8B7CF6), Color(0xFF4F9DFF))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Dates.clock(ts.remainingSec), fontSize = 56.sp, fontWeight = FontWeight.Bold)
                    Text(Constants.MODE_NAMES[ts.mode], color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${todays.size} session${if (todays.size == 1) "" else "s"} today", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (!ts.active) {
                SubjectPicker(subjects, subject, { subject = it })
                Field(label, { label = it }, "Session label (optional)")
            } else {
                val name = subjects.firstOrNull { it.id == ts.subjectId }?.name
                Text(listOfNotNull(name, ts.label.ifBlank { null }).joinToString(" · ").ifBlank { "Stay focused" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!ts.active) {
                    Button(onClick = { vm.timer.start(subject, label.trim()); TimerService.start(ctx) }, modifier = Modifier.heightIn(min = 52.dp)) {
                        Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Start")
                    }
                } else {
                    if (ts.running) FilledTonalButton(onClick = { vm.timer.pause() }, modifier = Modifier.heightIn(min = 52.dp)) {
                        Icon(Icons.Filled.Pause, null); Spacer(Modifier.width(6.dp)); Text("Pause")
                    } else Button(onClick = { vm.timer.resume(); TimerService.start(ctx) }, modifier = Modifier.heightIn(min = 52.dp)) {
                        Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Resume")
                    }
                    OutlinedButton(onClick = { vm.timer.reset() }, modifier = Modifier.heightIn(min = 52.dp)) { Text("Reset") }
                    OutlinedButton(onClick = { vm.timer.skip() }, modifier = Modifier.heightIn(min = 52.dp)) {
                        Icon(Icons.Filled.SkipNext, null); Spacer(Modifier.width(4.dp)); Text("Skip")
                    }
                }
            }
            SCard {
                Text("Today's focus", style = MaterialTheme.typography.titleMedium)
                val min = todays.sumOf { it.durationSec } / 60
                Text(if (todays.isEmpty()) "No focus sessions yet. Start your first one above." else "${todays.size} completed · ${Dates.duration(min)} of focused study",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
