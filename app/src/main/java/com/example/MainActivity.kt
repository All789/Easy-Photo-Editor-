package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.EditorScreen
import com.example.ui.HomeScreen
import com.example.ui.IntroScreen
import com.example.ui.PrivacyPolicyScreen
import com.example.ui.theme.EasyPhotoEditorTheme
import com.example.viewmodel.EditorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EasyPhotoEditorTheme {
                val editorViewModel: EditorViewModel = viewModel()
                var currentScreen by remember { mutableStateOf(CurrentScreen.INTRO) }
                var launchedWithCreatedPage by remember { mutableStateOf(false) }

                when (currentScreen) {
                    CurrentScreen.INTRO -> {
                        IntroScreen(
                            onContinue = { currentScreen = CurrentScreen.HOME }
                        )
                    }
                    CurrentScreen.HOME -> {
                        HomeScreen(
                            viewModel = editorViewModel,
                            onOpenEditor = { hasCreatedPage ->
                                launchedWithCreatedPage = hasCreatedPage
                                currentScreen = CurrentScreen.EDITOR
                            },
                            onOpenPrivacyPolicy = {
                                currentScreen = CurrentScreen.PRIVACY_POLICY
                            }
                        )
                    }
                    CurrentScreen.PRIVACY_POLICY -> {
                        PrivacyPolicyScreen(
                            onBack = { currentScreen = CurrentScreen.HOME }
                        )
                    }
                    CurrentScreen.EDITOR -> {
                        EditorScreen(
                            viewModel = editorViewModel,
                            initialHasCreatedPage = launchedWithCreatedPage,
                            onNavigateHome = { currentScreen = CurrentScreen.HOME }
                        )
                    }
                }
            }
        }
    }
}
