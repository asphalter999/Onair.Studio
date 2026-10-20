package com.onair.studio.media

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

/**
 * CameraX source binding hardware camera frames directly to an OpenGL OES texture.
 */
class CameraSource(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onFrameAvailable: (texMatrix: FloatArray) -> Unit
) {
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    var textureId: Int = 0
        private set
    private var surfaceTexture: SurfaceTexture? = null
    private val texMatrix = FloatArray(16)

    fun setupGlTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        surfaceTexture = SurfaceTexture(textureId).apply {
            setDefaultBufferSize(1920, 1080)
            setOnFrameAvailableListener { st ->
                st.updateTexImage()
                st.getTransformMatrix(texMatrix)
                onFrameAvailable(texMatrix)
            }
        }
        return textureId
    }

    fun startCamera(width: Int = 1920, height: Int = 1080) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCamera(width, height)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindCamera(width: Int, height: Int) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        val preview = Preview.Builder()
            .setTargetResolution(Size(width, height))
            .build()

        val st = surfaceTexture ?: return
        preview.setSurfaceProvider { request ->
            val surface = android.view.Surface(st)
            request.provideSurface(surface, cameraExecutor) {
                surface.release()
            }
        }

        camera = provider.bindToLifecycle(lifecycleOwner, selector, preview)
    }

    fun toggleCamera(width: Int = 1920, height: Int = 1080) {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        bindCamera(width, height)
    }

    fun release() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
        surfaceTexture?.release()
        if (textureId != 0) {
            val textures = intArrayOf(textureId)
            GLES20.glDeleteTextures(1, textures, 0)
            textureId = 0
        }
    }
}
