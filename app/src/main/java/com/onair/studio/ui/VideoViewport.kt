package com.onair.studio.ui

import android.opengl.GLSurfaceView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoViewport(
    title: String,
    badgeColor: Color,
    isLive: Boolean
) {
    Surface(
        color = Color.Black,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    GLSurfaceView(context).apply {
                        setEGLContextClientVersion(3)
                        // Dedicated OpenGL Renderer hooked up to FrameCompositor FBO
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Header Overlay Tag
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge(containerColor = badgeColor) {
                    Text(title, color = Color.White)
                }
            }
        }
    }
}
