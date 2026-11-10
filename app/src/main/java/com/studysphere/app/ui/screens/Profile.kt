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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun ProfileScreen(vm: StudyViewModel, nav: NavController) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val subjectsS by vm.subjects.collectAsStateWithLifecycle()
    val analytics by vm.analytics.collectAsStateWithLifecycle()
    val subjects = subjectsS; val a = analytics

    Screen("Profile", actions = { IconButton(onClick = { nav.navigate("settings") }) { Icon(Icons.Filled.Settings, "Settings") } }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                GradientCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = .25f)), contentAlignment = Alignment.Center) {
                            Text(prefs.name.take(1).uppercase().ifBlank { "S" }, style = MaterialTheme.typography.headlineMedium)
                        }
                        Column {
                            Text(prefs.name.ifBlank { "Student" }, style = MaterialTheme.typography.titleLarge)
                            Text(prefs.level)
                            Text("Daily goal: ${Dates.duration(prefs.goalMin)}")
                        }
                    }
                }
            }
            item {
                if (a == null) LoadingBox() else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Study time", Dates.duration(a.totalMin), Icons.Filled.Schedule, Modifier.weight(1f))
                    StatTile("Tasks done", "${a.completedTasks}", Icons.Filled.TaskAlt, Modifier.weight(1f))
                    StatTile("Streak", "${a.streak}", Icons.Filled.LocalFireDepartment, Modifier.weight(1f))
                }
            }
            item {
                SectionTitle("My subjects", "Manage") { nav.navigate("subjects") }
                if (subjects.isNullOrEmpty()) Text("No subjects yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjects.forEach { s -> Box(Modifier.clickable { nav.navigate("subject/${s.id}") }) { SubjectChip(s) } }
                }
            }
            item { SectionTitle("More") }
            val links = listOf(
                Triple("Tasks & assignments", Icons.Filled.TaskAlt, "tasks"), Triple("Calendar", Icons.Filled.CalendarMonth, "calendar"),
                Triple("Flashcards", Icons.Filled.Style, "flashcards"), Triple("Exam tracker", Icons.Filled.School, "exams"),
                Triple("Analytics", Icons.Filled.Insights, "analytics"), Triple("Subjects", Icons.Filled.Book, "subjects"),
                Triple("Settings", Icons.Filled.Settings, "settings")
            )
            items(links) { (label, icon, route) ->
                SCard(onClick = { nav.navigate(route) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    }
                }
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, range: IntRange, step: Int, format: (Int) -> String, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        IconButton(onClick = { onChange((value - step).coerceIn(range)) }) { Icon(Icons.Filled.Remove, "Decrease $label") }
        Text(format(value), Modifier.widthIn(min = 56.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = { onChange((value + step).coerceIn(range)) }) { Icon(Icons.Filled.Add, "Increase $label") }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsScreen(vm: StudyViewModel, nav: NavController) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val pending by vm.pendingImport.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(false) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) vm.export(uri) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) vm.prepareImport(uri) }

    Screen("Settings", onBack = { nav.popBackStack() }) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SCard {
                Text("Profile", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Name: ${prefs.name}", Modifier.weight(1f)); TextButton(onClick = { editName = true }) { Text("Change") }
                }
                Text("Academic level", style = MaterialTheme.typography.labelMedium)
                ChoiceChips(Constants.LEVELS, Constants.LEVELS.indexOf(prefs.level), { vm.pref(Keys.LEVEL, Constants.LEVELS[it]) })
            }
            SCard {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                ChoiceChips(listOf("Dark theme", "Light theme", "System theme"), listOf(2, 1, 0).indexOf(prefs.theme), { vm.pref(Keys.THEME, listOf(2, 1, 0)[it]) })
            }
            SCard {
                Text("Study preferences", style = MaterialTheme.typography.titleMedium)
                Stepper("Daily study goal", prefs.goalMin, 15..720, 15, { Dates.duration(it) }) { vm.pref(Keys.GOAL, it) }
                Stepper("Focus duration", prefs.focusMin, 5..120, 5, { "$it min" }) { vm.pref(Keys.FOCUS_MIN, it); vm.timer.select(0, it * 60) }
                Stepper("Short break", prefs.shortMin, 1..30, 1, { "$it min" }) { vm.pref(Keys.SHORT_MIN, it) }
                Stepper("Long break", prefs.longMin, 5..60, 5, { "$it min" }) { vm.pref(Keys.LONG_MIN, it) }
            }
            SCard {
                Text("Notifications", style = MaterialTheme.typography.titleMedium)
                ToggleRow("Upcoming study sessions", prefs.nSession) { vm.pref(Keys.N_SESSION, it) }
                ToggleRow("Assignment deadlines", prefs.nTask) { vm.pref(Keys.N_TASK, it) }
                ToggleRow("Exam reminders", prefs.nExam) { vm.pref(Keys.N_EXAM, it) }
                ToggleRow("Daily study reminder (evening)", prefs.nDaily) { vm.pref(Keys.N_DAILY, it) }
                Text("Reminders are checked roughly every 15 minutes. Android may delay them when battery saver is on, so they are not exact. Focus timer alerts are always shown when a session ends.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SCard {
                Text("Data", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { exporter.launch("studysphere-backup-${Dates.fmt(Dates.today(), "yyyy-MM-dd")}.json") }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.Upload, null); Spacer(Modifier.width(8.dp)); Text("Export data")
                }
                OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.Download, null); Spacer(Modifier.width(8.dp)); Text("Import backup")
                }
                OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Filled.DeleteForever, null); Spacer(Modifier.width(8.dp)); Text("Clear application data")
                }
            }
            SCard {
                Text("About StudySphere", style = MaterialTheme.typography.titleMedium)
                Text("Version 1.0.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Your entire academic life, organized in one place.")
                Text("Privacy", style = MaterialTheme.typography.labelMedium)
                Text("All your data is stored only on this device. StudySphere has no account, no analytics and never sends your information to any server. Backup files you export are not encrypted, so keep them somewhere safe.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (editName) {
        var n by remember { mutableStateOf(prefs.name) }
        AlertDialog(
            onDismissRequest = { editName = false }, title = { Text("Your name") },
            text = { Field(n, { n = it }, "Name") },
            confirmButton = { TextButton(onClick = { if (n.isNotBlank()) { vm.pref(Keys.NAME, n.trim()); editName = false } }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("Cancel") } }
        )
    }
    pending?.let { b ->
        ConfirmDialog(
            "Replace all data?",
            "This backup contains ${b.subjects.size} subjects, ${b.tasks.size} tasks, ${b.sessions.size} study sessions, ${b.notes.size} notes, ${b.decks.size} decks with ${b.cards.size} cards and ${b.exams.size} exams. Importing will permanently replace everything currently in the app.",
            "Replace", { vm.confirmImport() }, { vm.cancelImport() }
        )
    }
    if (confirmReset) ConfirmDialog(
        "Clear all data?", "This permanently deletes every subject, task, note, flashcard, exam and setting, and returns you to onboarding. Consider exporting a backup first.",
        "Clear everything", { confirmReset = false; vm.resetApp() }, { confirmReset = false }
    )
}
