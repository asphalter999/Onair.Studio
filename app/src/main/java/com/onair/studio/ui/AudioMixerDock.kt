package com.onair.studio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AudioMixerDock(modifier: Modifier = Modifier) {
    var micVol by remember { mutableFloatStateOf(0.85f) }
    var micMuted by remember { mutableStateOf(false) }

    Surface(
        color = StudioCard,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxHeight()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("AUDIO MIXER", style = MaterialTheme.typography.labelMedium, color = StudioMuted)
            Spacer(modifier = Modifier.height(8.dp))

            // Microphone Channel
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { micMuted = !micMuted }) {
                    Icon(
                        if (micMuted) Icons.Default.VolumeOff else Icons.Default.Mic,
                        contentDescription = "Mute",
                        tint = if (micMuted) LiveCrimson else Color.White
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Mic / Aux (Internal / BT)", style = MaterialTheme.typography.bodySmall, color = StudioText)

                    // Dynamic VU Meter Bar (-60 dB to 0 dB)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color.Black, RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(if (micMuted) 0f else 0.72f)
                                .fillMaxHeight()
                                .background(VuGreen, RoundedCornerShape(3.dp))
                        )
                    }

                    Slider(
                        value = micVol,
                        onValueChange = { micVol = it },
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }
    }
}
