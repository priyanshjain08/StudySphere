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

private class CalEvent(
    val type: Int, val id: Long, val title: String, val day: Long,
    val start: Int?, val color: Int, val subtitle: String, val done: Boolean
)

private fun typeColor(t: Int) = when (t) { 0 -> Color(0xFF8B7CF6); 1 -> Color(0xFFFBBF24); else -> Color(0xFFFB7185) }

private fun buildEvents(ss: List<StudySession>, ts: List<Task>, ex: List<Examination>, sub: Map<Long, Subject>, sessionsOnly: Boolean): List<CalEvent> {
    val out = ArrayList<CalEvent>()
    ss.forEach {
        val s = sub[it.subjectId]
        out.add(CalEvent(0, it.id, it.title, it.date, it.startMin, s?.color ?: 0xFF8B7CF6.toInt(),
            "${Dates.time(it.startMin)}–${Dates.time(it.endMin)}" + (s?.let { x -> " · ${x.name}" } ?: ""), it.completed))
    }
    if (!sessionsOnly) {
        ts.forEach {
            out.add(CalEvent(1, it.id, it.title, it.deadline, null, Constants.PRIORITY_COLORS[it.priority],
                "Deadline · ${Constants.PRIORITIES[it.priority]} priority", it.status == Constants.DONE))
        }
        ex.forEach {
            out.add(CalEvent(2, it.id, it.name, it.date, it.timeMin, 0xFFFB7185.toInt(),
                "Exam · ${Dates.time(it.timeMin)}" + if (it.location.isNotBlank()) " · ${it.location}" else "", false))
        }
    }
    return out
}

private fun shift(day: Long, mode: Int, dir: Int): Long {
    val d = Dates.date(day)
    return when (mode) { 0 -> d.plusDays(dir.toLong()); 1 -> d.plusWeeks(dir.toLong()); else -> d.plusMonths(dir.toLong()) }.toEpochDay()
}

@Composable
fun PlannerScreen(vm: StudyViewModel, nav: NavController) {
    Screen("Study planner") { CalendarBody(vm, nav, sessionsOnly = true, initialMode = 0) }
}

@Composable
fun CalendarScreen(vm: StudyViewModel, nav: NavController) {
    Screen("Calendar", onBack = { nav.popBackStack() }) { CalendarBody(vm, nav, sessionsOnly = false, initialMode = 2) }
}

@Composable
private fun CalendarBody(vm: StudyViewModel, nav: NavController, sessionsOnly: Boolean, initialMode: Int) {
    val sessionsS by vm.sessions.collectAsStateWithLifecycle()
    val tasksS by vm.tasks.collectAsStateWithLifecycle()
    val examsS by vm.exams.collectAsStateWithLifecycle()
    val subjectsS by vm.subjects.collectAsStateWithLifecycle()
    val ss = sessionsS; val ts = tasksS; val ex = examsS; val sub = subjectsS
    if (ss == null || ts == null || ex == null || sub == null) { LoadingBox(); return }

    var mode by remember { mutableIntStateOf(initialMode) }
    var selected by remember { mutableLongStateOf(Dates.today()) }
    var detail by remember { mutableStateOf<CalEvent?>(null) }
    var editing by remember { mutableStateOf<StudySession?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<StudySession?>(null) }
    val subMap = sub.associateBy { it.id }
    val events = remember(ss, ts, ex, sub, sessionsOnly) { buildEvents(ss, ts, ex, subMap, sessionsOnly) }
    fun on(day: Long) = events.filter { it.day == day }.sortedBy { it.start ?: -1 }
    val monday = Dates.date(selected).let { it.minusDays((it.dayOfWeek.value - 1).toLong()) }.toEpochDay()
    val title = when (mode) {
        0 -> Dates.fmt(selected, "EEEE, d MMM")
        1 -> "Week of " + Dates.fmt(monday, "d MMM")
        else -> Dates.fmt(selected, "MMMM yyyy")
    }
    val emptyText = if (sessionsOnly) {
        if (selected == Dates.today()) "No study sessions scheduled today." else "No study sessions on this day."
    } else "Nothing scheduled on this day."

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                TabRow(selectedTabIndex = mode, containerColor = Color.Transparent) {
                    listOf("Day", "Week", "Month").forEachIndexed { i, t -> Tab(selected = mode == i, onClick = { mode = i }, text = { Text(t) }) }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { selected = shift(selected, mode, -1) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous") }
                    TextButton(onClick = { selected = Dates.today() }) { Text(title, style = MaterialTheme.typography.titleMedium) }
                    IconButton(onClick = { selected = shift(selected, mode, 1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next") }
                }
            }
            when (mode) {
                0 -> eventItems(on(selected), emptyText) { detail = it }
                1 -> for (i in 0..6) {
                    val d = monday + i
                    item(key = "wd$d") {
                        Text(Dates.fmt(d, "EEEE, d MMM"), style = MaterialTheme.typography.titleMedium,
                            color = if (d == Dates.today()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                    eventItems(on(d), "Nothing planned.") { detail = it }
                }
                else -> {
                    item { MonthGrid(selected, events) { selected = it } }
                    if (!sessionsOnly) item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf("Study" to 0, "Deadline" to 1, "Exam" to 2).forEach { (n, t) ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    SubjectDot(typeColor(t).toArgb(), 8.dp); Text(n, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    item { Text(Dates.fmt(selected, "EEEE, d MMM"), style = MaterialTheme.typography.titleMedium) }
                    eventItems(on(selected), emptyText) { detail = it }
                }
            }
        }
        if (sessionsOnly) {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Add session") }
            )
        }
    }

    detail?.let { e ->
        AlertDialog(
            onDismissRequest = { detail = null },
            title = { Text(e.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(Dates.fmt(e.day, "EEEE, d MMM yyyy"))
                    Text(e.subtitle)
                    Text(when (e.type) { 0 -> if (e.done) "Completed" else "Planned"; 1 -> if (e.done) "Task completed" else "Task deadline"; else -> "Examination" },
                        color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                Row {
                    if (e.type == 0) {
                        val s = ss.firstOrNull { it.id == e.id }
                        TextButton(onClick = { detail = null; if (s != null) vm.toggleSession(s) }) { Text(if (e.done) "Undo" else "Done") }
                        TextButton(onClick = { detail = null; editing = s }) { Text("Edit") }
                        TextButton(onClick = { detail = null; deleting = s }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    } else TextButton(onClick = { detail = null; nav.navigate(if (e.type == 1) "tasks" else "exams") }) { Text("Open") }
                }
            },
            dismissButton = { TextButton(onClick = { detail = null }) { Text("Close") } }
        )
    }
    if (adding) SessionSheet(vm, sub, ss, null, selected) { adding = false }
    editing?.let { s -> SessionSheet(vm, sub, ss, s, s.date) { editing = null } }
    deleting?.let { s ->
        ConfirmDialog("Delete session?", "\"${s.title}\" will be removed from your planner.", "Delete", { vm.deleteSession(s.id); deleting = null }, { deleting = null })
    }
}

private fun LazyListScope.eventItems(list: List<CalEvent>, empty: String, onClick: (CalEvent) -> Unit) {
    if (list.isEmpty()) {
        item { Text(empty, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp)) }
    } else items(list, key = { "${it.type}-${it.id}" }) { EventRow(it) { onClick(it) } }
}

@Composable
private fun EventRow(e: CalEvent, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.width(5.dp).height(40.dp).clip(RoundedCornerShape(3.dp)).background(Color(e.color)))
        Icon(when (e.type) { 0 -> Icons.Filled.Schedule; 1 -> Icons.Filled.TaskAlt; else -> Icons.Filled.School }, null, tint = Color(e.color))
        Column(Modifier.weight(1f)) {
            Text(e.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                textDecoration = if (e.done) TextDecoration.LineThrough else null)
            Text(e.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        if (e.done) Icon(Icons.Filled.CheckCircle, "Completed", tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun MonthGrid(selected: Long, events: List<CalEvent>, onSelect: (Long) -> Unit) {
    val d = Dates.date(selected)
    val first = d.withDayOfMonth(1)
    val offset = first.dayOfWeek.value - 1
    val len = d.lengthOfMonth()
    val rows = (offset + len + 6) / 7
    val today = Dates.today()
    Column {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
        }
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0..6) {
                    val n = r * 7 + c - offset + 1
                    if (n < 1 || n > len) {
                        Spacer(Modifier.weight(1f).height(52.dp))
                    } else {
                        val day = first.plusDays((n - 1).toLong()).toEpochDay()
                        val isSel = day == selected
                        val types = events.filter { it.day == day }.map { it.type }.toSet()
                        Column(
                            Modifier.weight(1f).height(52.dp).padding(2.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onSelect(day) }
                                .semantics { contentDescription = Dates.fmt(day, "d MMMM") + if (types.isNotEmpty()) ", has events" else "" },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                        ) {
                            Text("$n", color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (day == today) FontWeight.ExtraBold else FontWeight.Normal)
                            Row {
                                types.sorted().forEach { Box(Modifier.padding(1.dp).size(5.dp).clip(CircleShape).background(typeColor(it))) }
                            }
                        }
                    }
                }
            }
        }
    }
}
