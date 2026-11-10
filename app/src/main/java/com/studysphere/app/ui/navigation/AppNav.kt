package com.studysphere.app.ui.navigation

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
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.studysphere.app.ui.screens.*

fun NavController.goTab(route: String) = navigate(route) {
    popUpTo("home") { saveState = true }
    launchSingleTop = true
    restoreState = true
}

private class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("planner", "Planner", Icons.Filled.CalendarMonth),
    Tab("focus", "Focus", Icons.Filled.Timer),
    Tab("notes", "Notes", Icons.Filled.EditNote),
    Tab("profile", "Profile", Icons.Filled.Person)
)

@Composable
fun MainApp(vm: StudyViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val snack = remember { SnackbarHostState() }
    val ctx = LocalContext.current

    LaunchedEffect(Unit) { vm.messages.collect { snack.showSnackbar(it) } }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (route in tabs.map { it.route }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route, onClick = { nav.goTab(t.route) },
                            icon = { Icon(t.icon, null) }, label = { Text(t.label) }
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") { HomeScreen(vm, nav) }
            composable("planner") { PlannerScreen(vm, nav) }
            composable("focus") { FocusScreen(vm) }
            composable("notes") { NotesScreen(vm, nav) }
            composable("profile") { ProfileScreen(vm, nav) }
            composable("note/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                NoteEditorScreen(vm, nav, it.arguments?.getLong("id") ?: 0L)
            }
            composable("tasks") { TasksScreen(vm, nav) }
            composable("calendar") { CalendarScreen(vm, nav) }
            composable("exams") { ExamsScreen(vm, nav) }
            composable("analytics") { AnalyticsScreen(vm, nav) }
            composable("flashcards") { FlashcardsScreen(vm, nav) }
            composable("deck/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                DeckScreen(vm, nav, it.arguments?.getLong("id") ?: 0L)
            }
            composable(
                "study/{id}/{mode}",
                listOf(navArgument("id") { type = NavType.LongType }, navArgument("mode") { type = NavType.IntType })
            ) { StudyScreen(vm, nav, it.arguments?.getLong("id") ?: 0L, it.arguments?.getInt("mode") ?: 0) }
            composable("subjects") { SubjectsScreen(vm, nav) }
            composable("subject/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                SubjectDetailScreen(vm, nav, it.arguments?.getLong("id") ?: 0L)
            }
            composable("settings") { SettingsScreen(vm, nav) }
        }
    }
}
