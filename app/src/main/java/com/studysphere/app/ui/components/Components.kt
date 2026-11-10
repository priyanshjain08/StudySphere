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

object SubjectIcons {
    private val map: Map<String, ImageVector> = mapOf(
        "book" to Icons.Filled.Book, "science" to Icons.Filled.Science, "math" to Icons.Filled.Calculate,
        "language" to Icons.Filled.Translate, "code" to Icons.Filled.Code, "art" to Icons.Filled.Palette,
        "history" to Icons.Filled.HistoryEdu, "music" to Icons.Filled.MusicNote,
        "sport" to Icons.Filled.SportsSoccer, "world" to Icons.Filled.Public
    )
    fun get(name: String): ImageVector = map[name] ?: Icons.Filled.Book
}

@Composable
fun SCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            Modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp), content = content
        )
    }
}

@Composable
fun GradientCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF3F8CFF)))).padding(20.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) { Column(content = content) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Screen(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    fab: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = actions,
                windowInsets = WindowInsets(0, 0, 0, 0),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
            Box(Modifier.weight(1f).fillMaxWidth(), content = content)
        }
        if (fab != null) Box(Modifier.align(Alignment.BottomEnd).padding(16.dp)) { fab() }
    }
}

@Composable
fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.semantics { contentDescription = "Loading" })
    }
}

@Composable
fun EmptyState(
    icon: ImageVector, title: String, message: String,
    actionLabel: String? = null, modifier: Modifier = Modifier, onAction: (() -> Unit)? = null
) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(
                Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = .3f), MaterialTheme.colorScheme.secondary.copy(alpha = .2f)))
            ),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary) }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) Button(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
fun Field(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    singleLine: Boolean = true, error: String? = null, keyboardType: KeyboardType = KeyboardType.Text, minLines: Int = 1
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = modifier.fillMaxWidth(), singleLine = singleLine, minLines = minLines,
        isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun SubjectPicker(subjects: List<Subject>, selectedId: Long?, onSelect: (Long?) -> Unit, modifier: Modifier = Modifier, label: String = "Subject") {
    var open by remember { mutableStateOf(false) }
    val sel = subjects.firstOrNull { it.id == selectedId }
    Box(modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) {
            if (sel != null) { SubjectDot(sel.color); Spacer(Modifier.width(8.dp)) }
            Text(sel?.name ?: "$label: none", maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.ArrowDropDown, null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onSelect(null); open = false })
            subjects.forEach { s ->
                DropdownMenuItem(text = { Text(s.name) }, leadingIcon = { SubjectDot(s.color) }, onClick = { onSelect(s.id); open = false })
            }
        }
    }
}

@Composable
fun SubjectDot(color: Int, size: Dp = 10.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(color)))
}

@Composable
fun SubjectChip(subject: Subject?) {
    if (subject == null) return
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Color(subject.color).copy(alpha = 0.18f)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        SubjectDot(subject.color, 7.dp)
        Text(subject.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
fun DateField(label: String, day: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    OutlinedButton(
        onClick = {
            val d = Dates.date(day)
            android.app.DatePickerDialog(ctx, { _, y, m, dd -> onChange(java.time.LocalDate.of(y, m + 1, dd).toEpochDay()) }, d.year, d.monthValue - 1, d.dayOfMonth).show()
        },
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
        Text("$label: ${Dates.fmt(day, "d MMM yyyy")}", maxLines = 1)
    }
}

@Composable
fun TimeField(label: String, minutes: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    OutlinedButton(
        onClick = {
            android.app.TimePickerDialog(ctx, { _, h, m -> onChange(h * 60 + m) }, minutes / 60, minutes % 60, true).show()
        },
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.Filled.Schedule, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
        Text("$label: ${Dates.time(minutes)}", maxLines = 1)
    }
}

@Composable
fun ChoiceChips(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, o -> FilterChip(selected = i == selected, onClick = { onSelect(i) }, label = { Text(o) }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)
                .navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ProgressRing(
    progress: Float, ringSize: Dp, modifier: Modifier = Modifier, stroke: Dp = 14.dp,
    track: Color = Color.White.copy(alpha = 0.22f),
    colors: List<Color> = listOf(Color(0xFF8B7CF6), Color(0xFF4F9DFF)),
    content: @Composable BoxScope.() -> Unit = {}
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(600), label = "ring")
    Box(modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val arc = Size(this.size.width - s, this.size.height - s)
            drawArc(track, 0f, 360f, false, Offset(s / 2, s / 2), arc, style = Stroke(s, cap = StrokeCap.Round))
            if (p > 0f) drawArc(Brush.linearGradient(colors), -90f, 360f * p, false, Offset(s / 2, s / 2), arc, style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun StatTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    SCard(modifier) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionTitle(text: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun Pill(text: String, color: Color) {
    Text(
        text, style = MaterialTheme.typography.labelSmall, color = color,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
