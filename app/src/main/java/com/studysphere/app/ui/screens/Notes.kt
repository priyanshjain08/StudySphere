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
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.activity.compose.BackHandler

private fun preview(n: Note) = n.content.replace('\n', ' ').take(140)

@Composable
fun NotesScreen(vm: StudyViewModel, nav: NavController) {
    val notesS by vm.notes.collectAsStateWithLifecycle()
    val subjectsS by vm.subjects.collectAsStateWithLifecycle()
    val notes = notesS; val subjects = subjectsS
    var query by remember { mutableStateOf("") }
    var grid by remember { mutableStateOf(true) }
    var sort by remember { mutableIntStateOf(0) }
    var menu by remember { mutableStateOf(false) }

    Screen(
        "Notes",
        actions = {
            IconButton(onClick = { grid = !grid }) { Icon(if (grid) Icons.Filled.ViewList else Icons.Filled.GridView, if (grid) "Switch to list" else "Switch to grid") }
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.Sort, "Sort notes") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                listOf("Last updated", "Date created", "Title").forEachIndexed { i, n ->
                    DropdownMenuItem(text = { Text(n) }, onClick = { sort = i; menu = false }, trailingIcon = { if (sort == i) Icon(Icons.Filled.Check, null) })
                }
            }
        },
        fab = { ExtendedFloatingActionButton(onClick = { nav.navigate("note/0") }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("New note") }) }
    ) {
        if (notes == null || subjects == null) LoadingBox() else {
            val subMap = subjects.associateBy { it.id }
            val q = query.trim()
            val filtered = notes.filter {
                q.isEmpty() || it.title.contains(q, true) || it.content.contains(q, true) || it.tags.contains(q, true) ||
                    (subMap[it.subjectId]?.name?.contains(q, true) ?: false)
            }
            val sorted = when (sort) {
                1 -> filtered.sortedByDescending { it.createdAt }
                2 -> filtered.sortedBy { it.title.lowercase() }
                else -> filtered.sortedByDescending { it.updatedAt }
            }.sortedByDescending { it.pinned }

            Column(Modifier.fillMaxSize()) {
                Field(query, { query = it }, "Search notes", Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp))
                if (sorted.isEmpty()) {
                    if (notes.isEmpty()) EmptyState(Icons.Filled.EditNote, "Your notes will appear here", "Capture ideas, summaries and lecture notes. They are saved automatically.", "Create a note") { nav.navigate("note/0") }
                    else EmptyState(Icons.Filled.Search, "No matching notes", "Try another search term.")
                } else if (grid) {
                    LazyVerticalGrid(
                        GridCells.Adaptive(160.dp), contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) { items(sorted, key = { it.id }) { n -> NoteCard(n, subMap[n.subjectId], vm) { nav.navigate("note/${n.id}") } } }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(sorted, key = { it.id }) { n -> NoteCard(n, subMap[n.subjectId], vm) { nav.navigate("note/${n.id}") } }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(n: Note, subject: Subject?, vm: StudyViewModel, onClick: () -> Unit) {
    SCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Text(n.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = { vm.togglePin(n) }, modifier = Modifier.size(32.dp)) {
                Icon(if (n.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, if (n.pinned) "Unpin note" else "Pin note",
                    tint = if (n.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (n.content.isNotBlank()) Text(preview(n), style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SubjectChip(subject)
        if (n.tags.isNotBlank()) Text(n.tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }.joinToString("  ") { "#$it" },
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1)
        Text(Dates.fmt(Dates.dayOf(n.updatedAt), "d MMM yyyy"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NoteEditorScreen(vm: StudyViewModel, nav: NavController, noteId: Long) {
    val notesS by vm.notes.collectAsStateWithLifecycle()
    val subjectsS by vm.subjects.collectAsStateWithLifecycle()
    val notes = notesS; val subjects = subjectsS
    if (notes == null || subjects == null) { LoadingBox(); return }

    val existing = remember(noteId) { notes.firstOrNull { it.id == noteId } }
    var id by remember { mutableLongStateOf(noteId) }
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var content by remember { mutableStateOf(existing?.content ?: "") }
    var tags by remember { mutableStateOf(existing?.tags ?: "") }
    var subject by remember { mutableStateOf(existing?.subjectId) }
    var pinned by remember { mutableStateOf(existing?.pinned ?: false) }
    var created by remember { mutableLongStateOf(existing?.createdAt ?: System.currentTimeMillis()) }
    var touched by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(if (existing != null) "Saved" else "New note") }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun save() {
        if (title.isBlank() && content.isBlank()) return
        val r = vm.repo.saveNote(Note(id, title.trim().ifBlank { "Untitled" }, content, subject, tags.trim(), pinned, created, System.currentTimeMillis()))
        if (id == 0L && r > 0) id = r
        status = "Saved"
    }
    fun leave() { scope.launch { if (touched) save(); nav.popBackStack() } }

    LaunchedEffect(title, content, tags, subject, pinned) {
        if (touched) { status = "Saving..."; delay(800); save() }
    }
    BackHandler { leave() }

    Screen(
        "Note", onBack = { leave() },
        actions = {
            IconButton(onClick = { pinned = !pinned; touched = true }) {
                Icon(if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, if (pinned) "Unpin note" else "Pin note")
            }
            if (id != 0L) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Delete note") }
        }
    ) {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextField(
                value = title, onValueChange = { title = it; touched = true }, placeholder = { Text("Title", style = MaterialTheme.typography.headlineMedium) },
                textStyle = MaterialTheme.typography.headlineMedium, modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent)
            )
            SubjectPicker(subjects, subject, { subject = it; touched = true })
            Field(tags, { tags = it; touched = true }, "Tags (comma separated)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                AssistChip(onClick = { content += (if (content.isEmpty() || content.endsWith("\n")) "" else "\n") + "## "; touched = true }, label = { Text("Heading") })
                AssistChip(onClick = { content += (if (content.isEmpty() || content.endsWith("\n")) "" else "\n") + "• "; touched = true }, label = { Text("Bullet") })
                AssistChip(onClick = { content += (if (content.isEmpty() || content.endsWith("\n")) "" else "\n") + "☐ "; touched = true }, label = { Text("Checkbox") })
            }
            TextField(
                value = content, onValueChange = { content = it; touched = true },
                placeholder = { Text("Start writing...") },
                textStyle = MaterialTheme.typography.bodyLarge, modifier = Modifier.fillMaxWidth().heightIn(min = 320.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp)
            )
            Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${content.length} characters · ${content.split(Regex("\\s+")).count { it.isNotBlank() }} words", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
    if (confirmDelete) ConfirmDialog("Delete note?", "This note will be permanently deleted.", "Delete",
        { confirmDelete = false; vm.deleteNote(id); nav.popBackStack() }, { confirmDelete = false })
}
