package com.onair.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.onair.studio.model.SceneManager

@Composable
fun StudioScreen(
    sceneManager: SceneManager,
    isStreaming: Boolean,
    isRecording: Boolean,
    onToggleStream: () -> Unit,
    onToggleRecord: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioDarkBg)
    ) {
        // Top Broadcast Telemetry Bar
        TelemetryBar(
            isStreaming = isStreaming,
            isRecording = isRecording,
            fps = 60,
            bitrateKbps = 4500,
            droppedPercent = 0.0f,
            onOpenSettings = { showSettings = true }
        )

        // Main Studio Canvas (Dual Viewports: Preview vs Program with Transition Controller)
        Row(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preview Viewport (Left)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                VideoViewport(
                    title = "PREVIEW (Staging)",
                    badgeColor = StudioCyan,
                    isLive = false
                )
            }

            // Studio Mode Transition Central Pillar
            Column(
                modifier = Modifier
                    .width(110.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        val prev = sceneManager.previewSceneId.value
                        sceneManager.transitionToProgram(prev)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CUT", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        val prev = sceneManager.previewSceneId.value
                        sceneManager.transitionToProgram(prev)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("FADE", color = StudioText)
                }
            }

            // Program Viewport (Right - LIVE Output)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                VideoViewport(
                    title = "PROGRAM (Live Feed)",
                    badgeColor = LiveCrimson,
                    isLive = isStreaming
                )
            }
        }

        // Bottom Docks (Scenes, Sources, Multichannel Audio Mixer, Controls)
        Row(
            modifier = Modifier
                .height(260.dp)
                .fillMaxWidth()
                .background(StudioSurface)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SceneDock(
                sceneManager = sceneManager,
                modifier = Modifier.weight(0.9f)
            )

            SourceDock(
                sceneManager = sceneManager,
                modifier = Modifier.weight(1.1f)
            )

            AudioMixerDock(
                modifier = Modifier.weight(1.3f)
            )

            ControlsDock(
                isStreaming = isStreaming,
                isRecording = isRecording,
                onToggleStream = onToggleStream,
                onToggleRecord = onToggleRecord,
                modifier = Modifier.width(160.dp)
            )
        }
    }

    if (showSettings) {
        SettingsModal(onDismiss = { showSettings = false })
    }
}

@Composable
fun TelemetryBar(
    isStreaming: Boolean,
    isRecording: Boolean,
    fps: Int,
    bitrateKbps: Int,
    droppedPercent: Float,
    onOpenSettings: () -> Unit
) {
    Surface(
        color = StudioCard,
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ONAIR STUDIO",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(16.dp))
                Badge(containerColor = if (isStreaming) LiveCrimson else Color.DarkGray) {
                    Text(if (isStreaming) "LIVE" else "OFFLINE", color = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Badge(containerColor = if (isRecording) RecordOrange else Color.DarkGray) {
                    Text(if (isRecording) "REC" else "STANDBY", color = Color.Black)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("FPS: $fps.0", color = StudioMuted)
                Text("Bitrate: $bitrateKbps kbps", color = StudioMuted)
                Text("Dropped: $droppedPercent%", color = StudioMuted)
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = StudioText)
                }
            }
        }
    }
}
