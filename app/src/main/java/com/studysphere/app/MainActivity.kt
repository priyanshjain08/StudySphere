package com.studysphere.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studysphere.app.focus.TimerService
import com.studysphere.app.ui.components.LoadingBox
import com.studysphere.app.ui.navigation.MainApp
import com.studysphere.app.ui.screens.OnboardingScreen
import com.studysphere.app.ui.theme.StudySphereTheme
import com.studysphere.app.viewmodel.StudyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: StudyViewModel = viewModel()
            val prefs by vm.prefs.collectAsStateWithLifecycle()
            StudySphereTheme(prefs.theme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when {
                        !prefs.loaded -> LoadingBox()
                        !prefs.onboarded -> OnboardingScreen(vm)
                        else -> MainApp(vm)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Restore a timer that was running when the app process was recreated.
        val timer = (application as StudyApp).timer
        timer.restore()
        if (timer.state.value.running) {
            try { TimerService.start(this) } catch (e: Exception) { /* service start not allowed right now */ }
        }
    }
}
