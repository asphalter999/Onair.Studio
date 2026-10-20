package com.onair.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.onair.studio.model.SceneManager
import com.onair.studio.ui.OnAirStudioTheme
import com.onair.studio.ui.RequestStudioPermissions
import com.onair.studio.ui.StudioScreen

class MainActivity : ComponentActivity() {

    private val sceneManager = SceneManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OnAirStudioTheme {
                var permissionsGranted by remember { mutableStateOf(false) }
                var isStreaming by remember { mutableStateOf(false) }
                var isRecording by remember { mutableStateOf(false) }

                if (!permissionsGranted) {
                    RequestStudioPermissions {
                        permissionsGranted = true
                    }
                }

                StudioScreen(
                    sceneManager = sceneManager,
                    isStreaming = isStreaming,
                    isRecording = isRecording,
                    onToggleStream = { isStreaming = !isStreaming },
                    onToggleRecord = { isRecording = !isRecording },
                    onOpenSettings = {}
                )
            }
        }
    }
}
