package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.BatchSessionScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            
            MyApplicationTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
                    
                    NavHost(navController = navController, startDestination = "history") {
                        composable("history") {
                            val history by viewModel.history.collectAsState()
                            HistoryScreen(
                                history = history,
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { viewModel.toggleTheme() },
                                onNavigateToCamera = { 
                                    viewModel.endBatch()
                                    navController.navigate("camera") 
                                },
                                onStartBatch = {
                                    viewModel.startBatch()
                                    navController.navigate("batch_session")
                                }
                            )
                        }
                        composable("batch_session") {
                            val batch by viewModel.currentBatch.collectAsState()
                            BatchSessionScreen(
                                batch = batch,
                                onAddScan = {
                                    navController.navigate("camera")
                                },
                                onEndBatch = {
                                    viewModel.endBatch()
                                    navController.popBackStack("history", inclusive = false)
                                }
                            )
                        }
                        composable("camera") {
                            CameraScreen(
                                onImageCaptured = { uri ->
                                    capturedImageUri = uri
                                    viewModel.gradeLeather(this@MainActivity, uri)
                                    navController.navigate("result") {
                                        popUpTo("camera") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("result") {
                            val uri = capturedImageUri
                            if (uri != null) {
                                val result by viewModel.currentResult.collectAsState()
                                val isLoading by viewModel.isLoading.collectAsState()
                                val error by viewModel.error.collectAsState()

                                ResultScreen(
                                    imageUri = uri,
                                    result = result,
                                    isLoading = isLoading,
                                    error = error,
                                    onSaveOverride = { grade, overrideReason ->
                                        val finalReasoning = if (!overrideReason.isNullOrBlank()) {
                                            "Manual override: $overrideReason"
                                        } else {
                                            result?.reasoning ?: "Manual verification"
                                        }
                                        viewModel.saveResult(
                                            imageUri = uri,
                                            grade = grade,
                                            confidence = result?.confidence ?: 1.0f,
                                            reasoning = finalReasoning
                                        )
                                        if (viewModel.isBatchMode.value) {
                                            navController.popBackStack("batch_session", inclusive = false)
                                        } else {
                                            navController.popBackStack("history", inclusive = false)
                                        }
                                    },
                                    onCancel = {
                                        if (viewModel.isBatchMode.value) {
                                            navController.popBackStack("batch_session", inclusive = false)
                                        } else {
                                            navController.popBackStack("history", inclusive = false)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
