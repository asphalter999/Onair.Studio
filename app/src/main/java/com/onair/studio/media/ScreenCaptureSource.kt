package com.onair.studio.media

import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.view.Surface

/**
 * Captures Android display content via MediaProjection into an OpenGL OES texture.
 */
class ScreenCaptureSource(
    private val mediaProjection: MediaProjection,
    private val width: Int = 1920,
    private val height: Int = 1080,
    private val densityDpi: Int = 320,
    private val onFrameAvailable: (texMatrix: FloatArray) -> Unit
) {
    var textureId: Int = 0
        private set
    private var surfaceTexture: SurfaceTexture? = null
    private var surface: Surface? = null
    private var virtualDisplay: VirtualDisplay? = null
    private val texMatrix = FloatArray(16)

    fun setupGlTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        surfaceTexture = SurfaceTexture(textureId).apply {
            setDefaultBufferSize(width, height)
            setOnFrameAvailableListener { st ->
                st.updateTexImage()
                st.getTransformMatrix(texMatrix)
                onFrameAvailable(texMatrix)
            }
        }

        surface = Surface(surfaceTexture)
        return textureId
    }

    fun startCapture() {
        val surf = surface ?: return
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "OnAirScreenCapture",
            width,
            height,
            densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            surf,
            null,
            null
        )
    }

    fun release() {
        virtualDisplay?.release()
        surface?.release()
        surfaceTexture?.release()
        if (textureId != 0) {
            val textures = intArrayOf(textureId)
            GLES20.glDeleteTextures(1, textures, 0)
            textureId = 0
        }
    }
}
