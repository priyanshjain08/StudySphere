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
fun SessionSheet(
    vm: StudyViewModel, subjects: List<Subject>, existing: List<StudySession>,
    initial: StudySession?, defaultDay: Long, onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var subject by remember { mutableStateOf(initial?.subjectId) }
    var day by remember { mutableLongStateOf(initial?.date ?: defaultDay) }
    val defStart = ((Dates.nowMin() / 60 + 1) * 60).coerceAtMost(22 * 60)
    var start by remember { mutableIntStateOf(initial?.startMin ?: defStart) }
    var end by remember { mutableIntStateOf(initial?.endMin ?: (defStart + 60)) }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var tried by remember { mutableStateOf(false) }
    val titleErr = if (tried && title.isBlank()) "Title is required" else null
    val timeErr = if (!SessionLogic.validTimes(start, end)) "End time must be after start time" else null
    val overlaps = SessionLogic.overlaps(existing, day, start, end, initial?.id ?: 0)

    Sheet(if (initial == null) "Add study session" else "Edit study session", onDismiss) {
        Field(title, { title = it }, "Title", error = titleErr)
        SubjectPicker(subjects, subject, { subject = it })
        DateField("Date", day, { day = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeField("Start", start, { start = it }, Modifier.weight(1f))
            TimeField("End", end, { end = it }, Modifier.weight(1f))
        }
        if (timeErr != null) Text(timeErr, color = MaterialTheme.colorScheme.error)
        if (overlaps.isNotEmpty()) {
            Text("Warning: overlaps with " + overlaps.joinToString { it.title }, color = MaterialTheme.colorScheme.tertiary)
        }
        Field(desc, { desc = it }, "Description (optional)", singleLine = false, minLines = 2)
        Button(
            onClick = {
                tried = true
                if (title.isNotBlank() && timeErr == null) {
                    vm.saveSession(StudySession(initial?.id ?: 0, title.trim(), subject, day, start, end, desc.trim(), initial?.completed ?: false))
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) { Text("Save") }
    }
}

@Composable
fun TaskSheet(vm: StudyViewModel, subjects: List<Subject>, initial: Task?, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var subject by remember { mutableStateOf(initial?.subjectId) }
    var deadline by remember { mutableLongStateOf(initial?.deadline ?: (Dates.today() + 1)) }
    var priority by remember { mutableIntStateOf(initial?.priority ?: 1) }
    var status by remember { mutableIntStateOf(initial?.status ?: 0) }
    var tried by remember { mutableStateOf(false) }

    Sheet(if (initial == null) "New task" else "Edit task", onDismiss) {
        Field(title, { title = it }, "Title", error = if (tried && title.isBlank()) "Title is required" else null)
        Field(desc, { desc = it }, "Description (optional)", singleLine = false, minLines = 2)
        SubjectPicker(subjects, subject, { subject = it })
        DateField("Deadline", deadline, { deadline = it })
        Text("Priority", style = MaterialTheme.typography.labelMedium)
        ChoiceChips(Constants.PRIORITIES, priority, { priority = it })
        Text("Status", style = MaterialTheme.typography.labelMedium)
        ChoiceChips(Constants.STATUSES, status, { status = it })
        Button(
            onClick = {
                tried = true
                if (title.isNotBlank()) {
                    vm.saveTask(
                        (initial ?: Task(title = "", deadline = deadline)).copy(
                            title = title.trim(), description = desc.trim(), subjectId = subject,
                            deadline = deadline, priority = priority, status = status
                        )
                    )
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) { Text("Save") }
    }
}

@Composable
fun ExamSheet(vm: StudyViewModel, subjects: List<Subject>, initial: Examination?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var subject by remember { mutableStateOf(initial?.subjectId) }
    var date by remember { mutableLongStateOf(initial?.date ?: (Dates.today() + 14)) }
    var time by remember { mutableIntStateOf(initial?.timeMin ?: 9 * 60) }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var progress by remember { mutableFloatStateOf((initial?.progress ?: 0).toFloat()) }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var tried by remember { mutableStateOf(false) }

    Sheet(if (initial == null) "Add examination" else "Edit examination", onDismiss) {
        Field(name, { name = it }, "Exam name", error = if (tried && name.isBlank()) "Name is required" else null)
        SubjectPicker(subjects, subject, { subject = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateField("Date", date, { date = it }, Modifier.weight(1f))
            TimeField("Time", time, { time = it }, Modifier.weight(1f))
        }
        Field(location, { location = it }, "Location (optional)")
        Text("Preparation: ${progress.toInt()}%", style = MaterialTheme.typography.labelMedium)
        Slider(progress, { progress = it }, valueRange = 0f..100f, steps = 19)
        Field(notes, { notes = it }, "Notes (optional)", singleLine = false, minLines = 2)
        Button(
            onClick = {
                tried = true
                if (name.isNotBlank()) {
                    vm.saveExam(Examination(initial?.id ?: 0, name.trim(), subject, date, time, location.trim(), progress.toInt(), notes.trim()))
                    onDismiss()
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) { Text("Save") }
    }
}

@Composable
fun SubjectSheet(vm: StudyViewModel, initial: Subject?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var color by remember { mutableIntStateOf(initial?.color ?: Constants.SUBJECT_COLORS[0]) }
    var icon by remember { mutableStateOf(initial?.icon ?: "book") }
    var tried by remember { mutableStateOf(false) }

    Sheet(if (initial == null) "Add subject" else "Edit subject", onDismiss) {
        Field(name, { name = it }, "Subject name", error = if (tried && name.isBlank()) "Name is required" else null)
        Text("Colour", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Constants.SUBJECT_COLORS.forEach { c ->
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color(c))
                        .border(if (c == color) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        .clickable { color = c }.semantics { contentDescription = "Colour option" }
                )
            }
        }
        Text("Icon", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Constants.SUBJECT_ICONS.forEach { key ->
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (key == icon) Color(color).copy(alpha = .3f) else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { icon = key }.semantics { contentDescription = "Icon $key" },
                    contentAlignment = Alignment.Center
                ) { Icon(SubjectIcons.get(key), null, tint = if (key == icon) Color(color) else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        Button(
            onClick = {
                tried = true
                if (name.isNotBlank()) { vm.saveSubject(Subject(initial?.id ?: 0, name.trim(), color, icon)); onDismiss() }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) { Text("Save") }
    }
}
