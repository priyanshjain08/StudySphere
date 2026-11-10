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

@Composable
fun HomeScreen(vm: StudyViewModel, nav: NavController) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val analytics by vm.analytics.collectAsStateWithLifecycle()
    val sessionsS by vm.sessions.collectAsStateWithLifecycle()
    val tasksS by vm.tasks.collectAsStateWithLifecycle()
    val examsS by vm.exams.collectAsStateWithLifecycle()
    val subjectsS by vm.subjects.collectAsStateWithLifecycle()
    val an = analytics; val sessions = sessionsS; val tasks = tasksS; val exams = examsS; val subjects = subjectsS
    if (an == null || sessions == null || tasks == null || exams == null || subjects == null) { LoadingBox(); return }

    val today = Dates.today()
    val subMap = subjects.associateBy { it.id }
    var showTask by remember { mutableStateOf(false) }
    var showSession by remember { mutableStateOf(false) }
    val todays = sessions.filter { it.date == today }.sortedBy { it.startMin }
    val pending = tasks.filter { it.status != Constants.DONE }.sortedWith(compareBy<Task> { it.deadline }.thenByDescending { it.priority })
    val upcoming = exams.filter { it.date >= today }.sortedBy { it.date }.take(3)
    val goal = prefs.goalMin.coerceAtLeast(1)
    val remaining = (goal - an.todayMin).coerceAtLeast(0)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${Dates.greeting()}, ${prefs.name.ifBlank { "Student" }}", style = MaterialTheme.typography.titleLarge)
                    Text(Dates.fmt(today, "EEEE, d MMMM yyyy"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { nav.navigate("settings") }) { Icon(Icons.Filled.Notifications, "Notification settings") }
                Box(
                    Modifier.size(48.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF3F8CFF))))
                        .clickable { nav.goTab("profile") }.semantics { contentDescription = "Open profile" },
                    contentAlignment = Alignment.Center
                ) { Text(prefs.name.take(1).uppercase().ifBlank { "S" }, color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
        item {
            GradientCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ProgressRing(an.todayMin.toFloat() / goal, 110.dp, stroke = 12.dp, colors = listOf(Color.White, Color(0xFFFFE08A))) {
                        Text("${(an.todayMin * 100 / goal).coerceAtMost(999)}%", style = MaterialTheme.typography.titleLarge)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Today's progress", style = MaterialTheme.typography.titleMedium)
                        Text("Goal: ${Dates.duration(goal)}")
                        Text("Studied: ${Dates.duration(an.todayMin)}")
                        Text(if (remaining == 0) "Goal reached. Great job!" else "Remaining: ${Dates.duration(remaining)}")
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(Icons.Filled.Timer, "Focus", Modifier.weight(1f)) { nav.goTab("focus") }
                QuickAction(Icons.Filled.TaskAlt, "Add task", Modifier.weight(1f)) { showTask = true }
                QuickAction(Icons.Filled.EditNote, "New note", Modifier.weight(1f)) { nav.navigate("note/0") }
                QuickAction(Icons.Filled.Schedule, "Add session", Modifier.weight(1f)) { showSession = true }
            }
        }
        item {
            SectionTitle("Today's schedule", "Planner") { nav.goTab("planner") }
            if (todays.isEmpty()) {
                SCard { Text("No study sessions scheduled today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { showSession = true }) { Text("Add a study session") } }
            } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                todays.forEach { s ->
                    SCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Checkbox(checked = s.completed, onCheckedChange = { vm.toggleSession(s) })
                            Column(Modifier.weight(1f)) {
                                Text(s.title, style = MaterialTheme.typography.titleMedium, textDecoration = if (s.completed) TextDecoration.LineThrough else null)
                                Text("${Dates.time(s.startMin)} – ${Dates.time(s.endMin)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            SubjectChip(subMap[s.subjectId])
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("Pending tasks (${pending.size})", "All tasks") { nav.navigate("tasks") }
            if (pending.isEmpty()) {
                SCard { Text("You're all caught up. No pending tasks.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pending.take(3).forEach { t ->
                    SCard(onClick = { nav.navigate("tasks") }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(Constants.PRIORITY_COLORS[t.priority])))
                            Column(Modifier.weight(1f)) {
                                Text(t.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val over = TaskLogic.isOverdue(t, today)
                                Text(
                                    if (over) "Overdue · ${Dates.fmt(t.deadline)}" else "Due ${Dates.fmt(t.deadline)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(Constants.PRIORITIES[t.priority], style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        item {
            SectionTitle("Study statistics", "Analytics") { nav.navigate("analytics") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("This week", Dates.duration(an.weekMin), Icons.Filled.Schedule, Modifier.weight(1f))
                StatTile("Tasks done", "${an.completedTasks}", Icons.Filled.TaskAlt, Modifier.weight(1f))
                StatTile("Day streak", "${an.streak}", Icons.Filled.LocalFireDepartment, Modifier.weight(1f))
            }
        }
        item {
            SectionTitle("Upcoming exams", "All exams") { nav.navigate("exams") }
            if (upcoming.isEmpty()) {
                SCard { Text("No upcoming exams.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { nav.navigate("exams") }) { Text("Add an exam") } }
            } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                upcoming.forEach { e ->
                    SCard(onClick = { nav.navigate("exams") }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(e.name, style = MaterialTheme.typography.titleMedium)
                                SubjectChip(subMap[e.subjectId])
                            }
                            Text(Dates.countdown(e.date, today), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
    if (showTask) TaskSheet(vm, subjects, null) { showTask = false }
    if (showSession) SessionSheet(vm, subjects, sessions, null, today) { showSession = false }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick).heightIn(min = 72.dp).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1)
    }
}
