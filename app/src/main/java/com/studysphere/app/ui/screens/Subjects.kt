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
fun SubjectsScreen(vm: StudyViewModel, nav: NavController) {
    val subjectsS by vm.subjects.collectAsStateWithLifecycle()
    val subjects = subjectsS
    var query by remember { mutableStateOf("") }
    var showNew by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<Subject?>(null) }
    var del by remember { mutableStateOf<Subject?>(null) }

    Screen(
        "Subjects", onBack = { nav.popBackStack() },
        fab = { ExtendedFloatingActionButton(onClick = { showNew = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Add subject") }) }
    ) {
        if (subjects == null) LoadingBox() else {
            val shown = subjects.filter { query.isBlank() || it.name.contains(query.trim(), true) }
            Column(Modifier.fillMaxSize()) {
                Field(query, { query = it }, "Search subjects", Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp))
                if (shown.isEmpty()) {
                    if (subjects.isEmpty()) EmptyState(Icons.Filled.Book, "No subjects yet", "Add the subjects you study to organise everything by course.", "Add a subject") { showNew = true }
                    else EmptyState(Icons.Filled.Search, "No matching subjects", "Try another search term.")
                } else LazyColumn(contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(shown, key = { it.id }) { s ->
                        SCard(onClick = { nav.navigate("subject/${s.id}") }) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(s.color).copy(alpha = .22f)), contentAlignment = Alignment.Center) {
                                    Icon(SubjectIcons.get(s.icon), null, tint = Color(s.color))
                                }
                                Text(s.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                IconButton(onClick = { edit = s }) { Icon(Icons.Filled.Edit, "Edit ${s.name}") }
                                IconButton(onClick = { del = s }) { Icon(Icons.Filled.Delete, "Delete ${s.name}") }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showNew || edit != null) SubjectSheet(vm, edit) { showNew = false; edit = null }
    del?.let { s ->
        ConfirmDialog("Delete subject?", "\"${s.name}\" will be removed. Its sessions, tasks, notes, decks and exams are kept, just without a subject.", "Delete",
            { vm.deleteSubject(s.id); del = null }, { del = null })
    }
}

@Composable
fun SubjectDetailScreen(vm: StudyViewModel, nav: NavController, id: Long) {
    val subjectsS by vm.subjects.collectAsStateWithLifecycle()
    val sessionsS by vm.sessions.collectAsStateWithLifecycle()
    val tasksS by vm.tasks.collectAsStateWithLifecycle()
    val notesS by vm.notes.collectAsStateWithLifecycle()
    val decksS by vm.decks.collectAsStateWithLifecycle()
    val examsS by vm.exams.collectAsStateWithLifecycle()
    val focusS by vm.focus.collectAsStateWithLifecycle()
    val subjects = subjectsS; val sessions = sessionsS; val tasks = tasksS; val notes = notesS; val decks = decksS; val exams = examsS; val focus = focusS
    if (subjects == null || sessions == null || tasks == null || notes == null || decks == null || exams == null || focus == null) {
        Screen("Subject", onBack = { nav.popBackStack() }) { LoadingBox() }; return
    }
    val s = subjects.firstOrNull { it.id == id }
    if (s == null) { Screen("Subject", onBack = { nav.popBackStack() }) { EmptyState(Icons.Filled.Book, "Subject not found", "It may have been deleted.", "Back") { nav.popBackStack() } }; return }
    val ss = sessions.filter { it.subjectId == id }
    val ts = tasks.filter { it.subjectId == id }
    val ns = notes.filter { it.subjectId == id }
    val ds = decks.filter { it.subjectId == id }
    val es = exams.filter { it.subjectId == id }.sortedBy { it.date }
    val studyMin = ss.filter { it.completed }.sumOf { it.endMin - it.startMin } +
        focus.filter { it.subjectId == id && (it.mode == 0 || it.mode == 3) }.sumOf { it.durationSec } / 60

    Screen(s.name, onBack = { nav.popBackStack() }) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                GradientCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(SubjectIcons.get(s.icon), null, Modifier.size(36.dp))
                        Column {
                            Text("Total time spent", style = MaterialTheme.typography.bodySmall)
                            Text(Dates.duration(studyMin), style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
            item { SectionTitle("Study sessions (${ss.size})") }
            if (ss.isEmpty()) item { Text("No study sessions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(ss.sortedByDescending { it.date }.take(5), key = { "ss${it.id}" }) { x ->
                Text("${if (x.completed) "✓ " else ""}${x.title} · ${Dates.fmt(x.date)} ${Dates.time(x.startMin)}")
            }
            item { SectionTitle("Tasks (${ts.size})", "Open") { nav.navigate("tasks") } }
            if (ts.isEmpty()) item { Text("No tasks yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(ts.take(5), key = { "t${it.id}" }) { x -> Text("${Constants.STATUSES[x.status]} · ${x.title} · due ${Dates.fmt(x.deadline)}") }
            item { SectionTitle("Notes (${ns.size})") }
            if (ns.isEmpty()) item { Text("No notes yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(ns.take(5), key = { "n${it.id}" }) { x ->
                TextButton(onClick = { nav.navigate("note/${x.id}") }) { Text(x.title) }
            }
            item { SectionTitle("Flashcard decks (${ds.size})") }
            if (ds.isEmpty()) item { Text("No decks yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(ds, key = { "d${it.id}" }) { x -> TextButton(onClick = { nav.navigate("deck/${x.id}") }) { Text(x.name) } }
            item { SectionTitle("Related examinations (${es.size})") }
            if (es.isEmpty()) item { Text("No exams yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(es, key = { "e${it.id}" }) { x -> Text("${x.name} · ${Dates.fmt(x.date)} · ${Dates.countdown(x.date)}") }
        }
    }
}
