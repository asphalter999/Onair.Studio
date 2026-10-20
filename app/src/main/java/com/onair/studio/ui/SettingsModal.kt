package com.onair.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun SettingsModal(onDismiss: () -> Unit) {
    var rtmpUrl by remember { mutableStateOf("rtmp://a.rtmp.youtube.com/live2") }
    var streamKey by remember { mutableStateOf("••••••••••••••••") }
    var bitrate by remember { mutableFloatStateOf(4500f) }
    var selectedResolution by remember { mutableStateOf("1080p (1920x1080)") }
    var selectedFps by remember { mutableStateOf("60 FPS") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = StudioCard,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Broadcast & Encoder Settings", style = MaterialTheme.typography.titleLarge, color = StudioText)

                OutlinedTextField(
                    value = rtmpUrl,
                    onValueChange = { rtmpUrl = it },
                    label = { Text("RTMP / RTMPS Ingest Server") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = streamKey,
                    onValueChange = { streamKey = it },
                    label = { Text("Stream Key") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Target Video Bitrate: ${bitrate.toInt()} kbps", color = StudioText)
                Slider(
                    value = bitrate,
                    onValueChange = { bitrate = it },
                    valueRange = 1000f..10000f,
                    steps = 18
                )

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onDismiss) { Text("Save & Apply") }
                }
            }
        }
    }
}
