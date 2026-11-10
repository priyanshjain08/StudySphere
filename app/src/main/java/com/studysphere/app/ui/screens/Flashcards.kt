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
fun FlashcardsScreen(vm: StudyViewModel, nav: NavController) {
    val decksS by vm.decks.collectAsStateWithLifecycle()
    val cardsS by vm.cards.collectAsStateWithLifecycle()
    val subjectsS by vm.subjects.collectAsStateWithLifecycle()
    val decks = decksS; val cards = cardsS; val subjects = subjectsS
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<FlashcardDeck?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<FlashcardDeck?>(null) }

    Screen(
        "Flashcards", onBack = { nav.popBackStack() },
        fab = { ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("New deck") }) }
    ) {
        if (decks == null || cards == null || subjects == null) LoadingBox() else {
            val today = Dates.today()
            val subMap = subjects.associateBy { it.id }
            val q = query.trim()
            val shown = decks.filter { d ->
                q.isEmpty() || d.name.contains(q, true) || cards.any { it.deckId == d.id && (it.question.contains(q, true) || it.answer.contains(q, true) || it.tags.contains(q, true)) }
            }
            Column(Modifier.fillMaxSize()) {
                Field(query, { query = it }, "Search decks and cards", Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp))
                if (shown.isEmpty()) {
                    if (decks.isEmpty()) EmptyState(Icons.Filled.Style, "No flashcard decks yet", "Create a deck and add questions to start learning with spaced repetition.", "Create a deck") { creating = true }
                    else EmptyState(Icons.Filled.Search, "No matching decks", "Try another search term.")
                } else LazyColumn(contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(shown, key = { it.id }) { d ->
                        val dc = cards.filter { it.deckId == d.id }
                        val due = dc.count { it.dueDay <= today }
                        var menu by remember { mutableStateOf(false) }
                        SCard(onClick = { nav.navigate("deck/${d.id}") }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(d.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${dc.size} card${if (dc.size == 1) "" else "s"} · $due due", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    SubjectChip(subMap[d.subjectId])
                                }
                                Box {
                                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Deck options") }
                                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                        DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; editing = d })
                                        DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; deleting = d })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if ((creating || editing != null) && subjects != null) {
        var name by remember(editing) { mutableStateOf(editing?.name ?: "") }
        var subject by remember(editing) { mutableStateOf(editing?.subjectId) }
        var tried by remember(editing) { mutableStateOf(false) }
        Sheet(if (editing == null) "New deck" else "Rename deck", { creating = false; editing = null }) {
            Field(name, { name = it }, "Deck name", error = if (tried && name.isBlank()) "Name is required" else null)
            SubjectPicker(subjects, subject, { subject = it })
            Button(onClick = {
                tried = true
                if (name.isNotBlank()) { vm.saveDeck((editing ?: FlashcardDeck(name = "")).copy(name = name.trim(), subjectId = subject)); creating = false; editing = null }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Save") }
        }
    }
    deleting?.let { d ->
        ConfirmDialog("Delete deck?", "\"${d.name}\" and all of its cards and study history will be deleted.", "Delete", { vm.deleteDeck(d.id); deleting = null }, { deleting = null })
    }
}

@Composable
fun DeckScreen(vm: StudyViewModel, nav: NavController, deckId: Long) {
    val decksS by vm.decks.collectAsStateWithLifecycle()
    val cardsS by vm.cards.collectAsStateWithLifecycle()
    val reviewsS by vm.reviews.collectAsStateWithLifecycle()
    val subjectsS by vm.subjects.collectAsStateWithLifecycle()
    val decks = decksS; val cards = cardsS; val reviews = reviewsS; val subjects = subjectsS
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Flashcard?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Flashcard?>(null) }

    if (decks == null || cards == null || reviews == null || subjects == null) { Screen("Deck", onBack = { nav.popBackStack() }) { LoadingBox() }; return }
    val deck = decks.firstOrNull { it.id == deckId }
    if (deck == null) {
        Screen("Deck", onBack = { nav.popBackStack() }) { EmptyState(Icons.Filled.Style, "Deck not found", "This deck may have been deleted.", "Back to decks") { nav.popBackStack() } }
        return
    }
    val today = Dates.today()
    val dc = cards.filter { it.deckId == deckId }
    val due = dc.count { it.dueDay <= today }
    val shown = dc.filter { query.isBlank() || it.question.contains(query.trim(), true) || it.answer.contains(query.trim(), true) || it.tags.contains(query.trim(), true) }
    val history = reviews.filter { it.deckId == deckId }.groupBy { it.day }.toList().sortedByDescending { it.first }.take(7)

    Screen(
        deck.name, onBack = { nav.popBackStack() },
        fab = { ExtendedFloatingActionButton(onClick = { adding = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Add card") }) }
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                GradientCard {
                    Text("${dc.size} cards · $due due today", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { nav.navigate("study/$deckId/0") }, enabled = due > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF3B2FB5))) { Text("Study due") }
                        OutlinedButton(onClick = { nav.navigate("study/$deckId/1") }, enabled = dc.isNotEmpty()) { Text("Study all") }
                        OutlinedButton(onClick = { nav.navigate("study/$deckId/2") }, enabled = dc.isNotEmpty()) {
                            Icon(Icons.Filled.Shuffle, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Shuffle")
                        }
                    }
                }
            }
            if (dc.isEmpty()) item { EmptyState(Icons.Filled.Style, "No cards in this deck", "Add your first question and answer.", "Add a card") { adding = true } }
            else {
                item { Field(query, { query = it }, "Search cards") }
                items(shown, key = { it.id }) { c ->
                    SCard(onClick = { editing = c }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(c.question, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(c.answer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                                Text(if (c.dueDay <= today) "Due now" else "Next review ${Dates.fmt(c.dueDay)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { deleting = c }) { Icon(Icons.Filled.Delete, "Remove card") }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp)); SectionTitle("Study history")
                if (history.isEmpty()) Text("No reviews yet. Study this deck to build your history.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else SCard {
                    history.forEach { (day, rs) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(Dates.fmt(day, "EEE, d MMM"))
                            Text("${rs.size} reviewed · ${rs.count { it.rating == 3 }} easy, ${rs.count { it.rating == 2 }} medium, ${rs.count { it.rating == 1 }} hard", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    if (adding || editing != null) {
        val init = editing
        var q by remember(init) { mutableStateOf(init?.question ?: "") }
        var a by remember(init) { mutableStateOf(init?.answer ?: "") }
        var subject by remember(init) { mutableStateOf(init?.subjectId) }
        var tags by remember(init) { mutableStateOf(init?.tags ?: "") }
        var tried by remember(init) { mutableStateOf(false) }
        Sheet(if (init == null) "Add flashcard" else "Edit flashcard", { adding = false; editing = null }) {
            Field(q, { q = it }, "Question", singleLine = false, minLines = 2, error = if (tried && q.isBlank()) "Question is required" else null)
            Field(a, { a = it }, "Answer", singleLine = false, minLines = 2, error = if (tried && a.isBlank()) "Answer is required" else null)
            SubjectPicker(subjects, subject, { subject = it })
            Field(tags, { tags = it }, "Tags (optional, comma separated)")
            Button(onClick = {
                tried = true
                if (q.isNotBlank() && a.isNotBlank()) {
                    vm.saveCard((init ?: Flashcard(deckId = deckId, question = "", answer = "")).copy(question = q.trim(), answer = a.trim(), subjectId = subject, tags = tags.trim()))
                    adding = false; editing = null
                }
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Save") }
        }
    }
    deleting?.let { c ->
        ConfirmDialog("Remove flashcard?", "This card and its review history will be deleted.", "Remove", { vm.deleteCard(c.id); deleting = null }, { deleting = null })
    }
}

@Composable
fun StudyScreen(vm: StudyViewModel, nav: NavController, deckId: Long, mode: Int) {
    val cardsS by vm.cards.collectAsStateWithLifecycle()
    val cards = cardsS
    if (cards == null) { Screen("Study", onBack = { nav.popBackStack() }) { LoadingBox() }; return }
    val today = Dates.today()
    // The queue is fixed when the session starts so cards don't jump around while rating.
    val queue = remember(deckId, mode) {
        val base = cards.filter { it.deckId == deckId && (mode > 0 || it.dueDay <= today) }
        if (mode == 2) base.shuffled() else base.sortedBy { it.dueDay }
    }
    var idx by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var easy by remember { mutableIntStateOf(0) }
    var medium by remember { mutableIntStateOf(0) }
    var hard by remember { mutableIntStateOf(0) }

    Screen("Study", onBack = { nav.popBackStack() }) {
        when {
            queue.isEmpty() -> EmptyState(Icons.Filled.CheckCircle, "All caught up", "No cards are due in this deck right now.", "Back to deck") { nav.popBackStack() }
            idx >= queue.size -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.EmojiEvents, null, Modifier.size(72.dp), tint = Color(0xFFFFC857))
                Text("Session complete!", style = MaterialTheme.typography.headlineMedium)
                Text("${queue.size} cards reviewed", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Easy", "$easy", Icons.Filled.SentimentSatisfied, Modifier.weight(1f))
                    StatTile("Medium", "$medium", Icons.Filled.SentimentNeutral, Modifier.weight(1f))
                    StatTile("Hard", "$hard", Icons.Filled.SentimentDissatisfied, Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { nav.popBackStack() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back to deck") }
            }
            else -> {
                val card = queue[idx]
                val rot by animateFloatAsState(if (revealed) 180f else 0f, animationSpec = tween(400), label = "flip")
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LinearProgressIndicator(progress = { idx.toFloat() / queue.size }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
                    Text("${queue.size - idx} card${if (queue.size - idx == 1) "" else "s"} remaining", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        Modifier.fillMaxWidth().weight(1f).heightIn(min = 240.dp)
                            .graphicsLayer { rotationY = rot; cameraDistance = 12f * density }
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(if (rot <= 90f) listOf(Color(0xFF6C5CE7), Color(0xFF3F8CFF)) else listOf(Color(0xFF0F766E), Color(0xFF2563EB))))
                            .clickable { revealed = !revealed }
                            .semantics { contentDescription = if (revealed) "Answer. Tap to show question" else "Question. Tap to reveal answer" }
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rot <= 90f) Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("QUESTION", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .7f))
                            Text(card.question, style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
                            Text("Tap to reveal answer", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .7f))
                        } else Column(Modifier.graphicsLayer { rotationY = 180f }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("ANSWER", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .7f))
                            Text(card.answer, style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        fun rate(r: Int) {
                            vm.review(card, r)
                            when (r) { 3 -> easy++; 2 -> medium++; else -> hard++ }
                            revealed = false; idx++
                        }
                        Button(onClick = { rate(1) }, enabled = revealed, modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))) { Text("Hard") }
                        Button(onClick = { rate(2) }, enabled = revealed, modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))) { Text("Medium") }
                        Button(onClick = { rate(3) }, enabled = revealed, modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) { Text("Easy") }
                    }
                }
            }
        }
    }
}
