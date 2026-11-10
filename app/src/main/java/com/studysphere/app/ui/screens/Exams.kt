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
fun ExamsScreen(vm: StudyViewModel, nav: NavController) {
    val examsS by vm.exams.collectAsStateWithLifecycle()
    val subjectsS by vm.subjects.collectAsStateWithLifecycle()
    val exams = examsS; val subjects = subjectsS
    var showNew by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<Examination?>(null) }
    var del by remember { mutableStateOf<Examination?>(null) }

    Screen(
        "Exam tracker", onBack = { nav.popBackStack() },
        fab = { ExtendedFloatingActionButton(onClick = { showNew = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Add exam") }) }
    ) {
        if (exams == null || subjects == null) LoadingBox() else {
            val today = Dates.today()
            val subMap = subjects.associateBy { it.id }
            val upcoming = exams.filter { it.date >= today }.sortedBy { it.date }
            val past = exams.filter { it.date < today }.sortedByDescending { it.date }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { SectionTitle("Upcoming examinations") }
                if (upcoming.isEmpty()) item {
                    EmptyState(Icons.Filled.School, "No upcoming exams", "Add an exam to see a countdown and track your preparation.", "Add an exam") { showNew = true }
                } else items(upcoming, key = { "u${it.id}" }) { e -> ExamCard(vm, e, subMap[e.subjectId], today, { edit = e }, { del = e }) }
                if (past.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)); SectionTitle("Past examinations") }
                    items(past, key = { "p${it.id}" }) { e -> ExamCard(vm, e, subMap[e.subjectId], today, { edit = e }, { del = e }) }
                }
            }
        }
    }
    if ((showNew || edit != null) && subjects != null) ExamSheet(vm, subjects, edit) { showNew = false; edit = null }
    del?.let { e ->
        ConfirmDialog("Delete exam?", "\"${e.name}\" will be permanently deleted.", "Delete", { vm.deleteExam(e.id); del = null }, { del = null })
    }
}

@Composable
private fun ExamCard(vm: StudyViewModel, e: Examination, subject: Subject?, today: Long, onEdit: () -> Unit, onDelete: () -> Unit) {
    val past = e.date < today
    SCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(e.name, style = MaterialTheme.typography.titleMedium)
                SubjectChip(subject)
                Text("${Dates.fmt(e.date, "EEE, d MMM yyyy")} · ${Dates.time(e.timeMin)}" + if (e.location.isNotBlank()) " · ${e.location}" else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                if (past) "Completed" else Dates.countdown(e.date, today), style = MaterialTheme.typography.labelMedium,
                color = if (past) MaterialTheme.colorScheme.onSurfaceVariant else if (e.date - today <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete exam ${e.name}") }
        }
        if (!past) {
            Text("Preparation ${e.progress}%", style = MaterialTheme.typography.labelMedium)
            var value by remember(e.id, e.progress) { mutableFloatStateOf(e.progress.toFloat()) }
            Slider(value, { value = it }, onValueChangeFinished = { vm.setExamProgress(e, value.toInt()) }, valueRange = 0f..100f, steps = 19)
        }
        if (e.notes.isNotBlank()) Text(e.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
