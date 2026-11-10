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
fun TasksScreen(vm: StudyViewModel, nav: NavController) {
    val tasksS by vm.tasks.collectAsStateWithLifecycle()
    val subjectsS by vm.subjects.collectAsStateWithLifecycle()
    val tasks = tasksS; val subjects = subjectsS
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var sort by remember { mutableIntStateOf(0) }
    var showNew by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<Task?>(null) }
    var del by remember { mutableStateOf<Task?>(null) }
    var menu by remember { mutableStateOf(false) }

    Screen(
        "Tasks & assignments", onBack = { nav.popBackStack() },
        actions = {
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.Sort, "Sort tasks") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                listOf("Deadline", "Priority", "Recently created", "Completion status").forEachIndexed { i, n ->
                    DropdownMenuItem(text = { Text(n) }, onClick = { sort = i; menu = false },
                        trailingIcon = { if (sort == i) Icon(Icons.Filled.Check, null) })
                }
            }
        },
        fab = { ExtendedFloatingActionButton(onClick = { showNew = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Add task") }) }
    ) {
        if (tasks == null || subjects == null) LoadingBox() else {
            val today = Dates.today()
            val subMap = subjects.associateBy { it.id }
            val shown = TaskLogic.sort(TaskLogic.filter(tasks, query, filter, today), sort)
            Column(Modifier.fillMaxSize()) {
                Field(query, { query = it }, "Search tasks", Modifier.padding(horizontal = 16.dp))
                ChoiceChips(listOf("All", "Pending", "In progress", "Completed", "Overdue"), filter, { filter = it }, Modifier.padding(16.dp, 8.dp))
                if (shown.isEmpty()) {
                    if (tasks.isEmpty()) EmptyState(Icons.Filled.TaskAlt, "No tasks yet", "Add assignments and deadlines to stay on top of your work.", "Add your first task") { showNew = true }
                    else EmptyState(Icons.Filled.Search, "Nothing matches", "Try a different search or filter.")
                } else LazyColumn(contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(shown, key = { it.id }) { t ->
                        val over = TaskLogic.isOverdue(t, today)
                        SCard(onClick = { edit = t }) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Checkbox(checked = t.status == Constants.DONE, onCheckedChange = { vm.toggleTask(t) })
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(t.title, style = MaterialTheme.typography.titleMedium,
                                        textDecoration = if (t.status == Constants.DONE) TextDecoration.LineThrough else null)
                                    if (t.description.isNotBlank()) Text(t.description, style = MaterialTheme.typography.bodySmall, maxLines = 2,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                        Pill(Constants.PRIORITIES[t.priority], Color(Constants.PRIORITY_COLORS[t.priority]))
                                        Pill(Constants.STATUSES[t.status], MaterialTheme.colorScheme.secondary)
                                        Text(if (over) "Overdue · ${Dates.fmt(t.deadline)}" else "Due ${Dates.fmt(t.deadline)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                        SubjectChip(subMap[t.subjectId])
                                    }
                                }
                                IconButton(onClick = { del = t }) { Icon(Icons.Filled.Delete, "Delete task ${t.title}") }
                            }
                        }
                    }
                }
            }
        }
    }
    if ((showNew || edit != null) && subjects != null) {
        TaskSheet(vm, subjects, edit) { showNew = false; edit = null }
    }
    del?.let { t ->
        ConfirmDialog("Delete task?", "\"${t.title}\" will be permanently deleted.", "Delete", { vm.deleteTask(t.id); del = null }, { del = null })
    }
}
