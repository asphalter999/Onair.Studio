package com.onair.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun SceneDock(sceneManager: SceneManager, modifier: Modifier = Modifier) {
    val scenes by sceneManager.scenes.collectAsState()
    val previewId by sceneManager.previewSceneId.collectAsState()

    Surface(
        color = StudioCard,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxHeight()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("SCENES", style = MaterialTheme.typography.labelMedium, color = StudioMuted)
            Spacer(modifier = Modifier.height(4.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(scenes) { scene ->
                    val isSelected = scene.id == previewId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) StudioBorder else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { sceneManager.selectPreviewScene(scene.id) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(scene.name, color = if (isSelected) StudioCyan else StudioText)
                    }
                }
            }
        }
    }
}

@Composable
fun SourceDock(sceneManager: SceneManager, modifier: Modifier = Modifier) {
    val scenes by sceneManager.scenes.collectAsState()
    val previewId by sceneManager.previewSceneId.collectAsState()
    val currentScene = scenes.find { it.id == previewId }

    Surface(
        color = StudioCard,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxHeight()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("SOURCES", style = MaterialTheme.typography.labelMedium, color = StudioMuted)
            Spacer(modifier = Modifier.height(4.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(currentScene?.layers ?: emptyList()) { layer ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StudioBorder, RoundedCornerShape(4.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(layer.name, color = StudioText, style = MaterialTheme.typography.bodySmall)
                        IconButton(
                            onClick = { currentScene?.let { sceneManager.toggleLayerVisibility(it.id, layer.id) } },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle",
                                tint = if (layer.isVisible) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ControlsDock(
    isStreaming: Boolean,
    isRecording: Boolean,
    onToggleStream: () -> Unit,
    onToggleRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioCard,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onToggleStream,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isStreaming) LiveCrimson else Color(0xFF238636)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isStreaming) "STOP STREAM" else "START STREAM")
            }

            Button(
                onClick = onToggleRecord,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) RecordOrange else Color(0xFF388BFD)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isRecording) "STOP RECORD" else "START RECORD")
            }
        }
    }
}
