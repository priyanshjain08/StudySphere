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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

private class Intro(val icon: ImageVector, val title: String, val text: String)

private val intro = listOf(
    Intro(Icons.Filled.School, "Welcome to StudySphere", "Your entire academic life, organized in one place."),
    Intro(Icons.Filled.CalendarMonth, "Organize your academic schedule", "Plan study sessions, track assignments and never miss an exam date."),
    Intro(Icons.Filled.Timer, "Stay focused and productive", "Use the Pomodoro timer, notes and flashcards to study smarter."),
    Intro(Icons.Filled.Insights, "Track your academic progress", "See study hours, streaks and subject insights built from your own activity.")
)

@Composable
fun OnboardingScreen(vm: StudyViewModel) {
    val total = intro.size + 1
    val pager = rememberPagerState(pageCount = { total })
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (pager.currentPage < total - 1) TextButton(onClick = { scope.launch { pager.animateScrollToPage(total - 1) } }) { Text("Skip") }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            if (page < intro.size) IntroPage(intro[page]) else SetupPage(vm)
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(total) { i ->
                    Box(
                        Modifier.size(if (i == pager.currentPage) 22.dp else 8.dp, 8.dp).clip(CircleShape)
                            .background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    )
                }
            }
            if (pager.currentPage < total - 1) {
                Button(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Next") }
            }
        }
    }
}

@Composable
private fun IntroPage(p: Intro) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(primary.copy(alpha = .10f), size.minDimension / 2)
                drawCircle(secondary.copy(alpha = .35f), size.minDimension / 2.2f, style = Stroke(3f))
                drawCircle(primary.copy(alpha = .45f), size.minDimension / 3.2f, style = Stroke(3f))
                drawCircle(Color(0xFFFFC857), 12f, Offset(size.width * 0.86f, size.height * 0.28f))
            }
            Box(
                Modifier.size(112.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF6C5CE7), Color(0xFF3F8CFF)))),
                contentAlignment = Alignment.Center
            ) { Icon(p.icon, null, Modifier.size(56.dp), tint = Color.White) }
        }
        Spacer(Modifier.height(32.dp))
        Text(p.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(p.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

private val suggested = listOf("Mathematics", "Physics", "Chemistry", "Biology", "English", "History", "Computer Science", "Economics")

@Composable
private fun SetupPage(vm: StudyViewModel) {
    var name by remember { mutableStateOf("") }
    var level by remember { mutableIntStateOf(1) }
    var goal by remember { mutableFloatStateOf(120f) }
    var themeIdx by remember { mutableIntStateOf(0) }
    var custom by remember { mutableStateOf("") }
    val chosen = remember { mutableStateListOf<String>() }
    var tried by remember { mutableStateOf(false) }
    val themeCodes = listOf(2, 1, 0)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Set up your space", style = MaterialTheme.typography.headlineMedium)
        Text("Everything stays on your device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Field(name, { name = it }, "Your name", error = if (tried && name.isBlank()) "Please enter your name" else null)
        Text("Academic level", style = MaterialTheme.typography.labelMedium)
        ChoiceChips(Constants.LEVELS, level, { level = it })
        Text("Subjects", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (suggested + chosen.filter { it !in suggested }).forEach { s ->
                FilterChip(
                    selected = s in chosen,
                    onClick = { if (s in chosen) chosen.remove(s) else chosen.add(s) },
                    label = { Text(s) }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(custom, { custom = it }, "Add your own subject", Modifier.weight(1f))
            FilledTonalButton(
                onClick = { val c = custom.trim(); if (c.isNotEmpty() && c !in chosen) chosen.add(c); custom = "" },
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Add") }
        }
        Text("Daily study goal: ${Dates.duration(goal.toInt())}", style = MaterialTheme.typography.labelMedium)
        Slider(goal, { goal = it }, valueRange = 30f..480f, steps = 29)
        Text("Preferred theme", style = MaterialTheme.typography.labelMedium)
        ChoiceChips(listOf("Dark", "Light", "System"), themeIdx, { themeIdx = it })
        Button(
            onClick = {
                tried = true
                if (name.isNotBlank()) vm.finishOnboarding(name, Constants.LEVELS[level], goal.toInt(), themeCodes[themeIdx], chosen.toList())
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) { Text("Get Started") }
        Spacer(Modifier.height(16.dp))
    }
}
