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
fun AnalyticsScreen(vm: StudyViewModel, nav: NavController) {
    val analytics by vm.analytics.collectAsStateWithLifecycle()
    val a = analytics
    Screen("Analytics", onBack = { nav.popBackStack() }) {
        if (a == null) LoadingBox() else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (a.totalMin == 0 && a.completedTasks == 0 && a.pendingTasks == 0) item {
                EmptyState(Icons.Filled.Insights, "No activity yet", "Finish a focus session or complete a planned study session and your insights will appear here.")
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Total study time", Dates.duration(a.totalMin), Icons.Filled.Schedule, Modifier.weight(1f))
                    StatTile("Today", Dates.duration(a.todayMin), Icons.Filled.Today, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Last 7 days", Dates.duration(a.weekMin), Icons.Filled.DateRange, Modifier.weight(1f))
                    StatTile("This month", Dates.duration(a.monthMin), Icons.Filled.CalendarMonth, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Current streak", "${a.streak} day${if (a.streak == 1) "" else "s"}", Icons.Filled.LocalFireDepartment, Modifier.weight(1f))
                    StatTile("Longest streak", "${a.longest} day${if (a.longest == 1) "" else "s"}", Icons.Filled.EmojiEvents, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Tasks completed", "${a.completedTasks}", Icons.Filled.TaskAlt, Modifier.weight(1f))
                    StatTile("Focus sessions", "${a.focusCount}", Icons.Filled.Timer, Modifier.weight(1f))
                }
            }
            item { SCard {
                Text("Weekly study activity", style = MaterialTheme.typography.titleMedium)
                if (a.last7.all { it.second == 0 }) Text("No study time recorded in the last 7 days.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else BarChart(a.last7)
            } }
            item { SCard {
                Text("Monthly study trend", style = MaterialTheme.typography.titleMedium)
                if (a.monthly.all { it.second == 0 }) Text("Not enough data yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else LineChart(a.monthly)
            } }
            item { SCard {
                Text("Time by subject", style = MaterialTheme.typography.titleMedium)
                if (a.bySubject.isEmpty()) Text("Study time per subject will show here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else DonutChart(a.bySubject)
            } }
            item { SCard {
                Text("Completed vs pending tasks", style = MaterialTheme.typography.titleMedium)
                SplitBar(a.completedTasks, a.pendingTasks)
            } }
            item { SCard {
                Text("Focus statistics", style = MaterialTheme.typography.titleMedium)
                Text("${a.focusCount} sessions · ${Dates.duration(a.focusMin)} total", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Average session: ${if (a.focusCount == 0) "n/a" else Dates.duration(a.avgFocusMin)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
        }
    }
}
