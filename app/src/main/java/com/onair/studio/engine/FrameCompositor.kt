package com.onair.studio.engine

import android.opengl.GLES20
import android.opengl.Matrix
import com.onair.studio.model.LayerConfig
import com.onair.studio.model.SourceType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * High-performance offscreen Framebuffer Object (FBO) compositor
 * that blends N active visual sources (Camera, Screen, Overlays, Titles)
 * based on position, scale, rotation, and alpha.
 */
class FrameCompositor(val outputWidth: Int = 1920, val outputHeight: Int = 1080) {

    private val program2D = Texture2DProgram(Texture2DProgram.ProgramType.TEXTURE_2D)
    private val programOes = Texture2DProgram(Texture2DProgram.ProgramType.TEXTURE_EXT_OES)

    private var fboHandle: Int = 0
    var fboTextureId: Int = 0
        private set

    private val vertexBuffer: FloatBuffer
    private val texCoordBuffer: FloatBuffer
    private val identityMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    init {
        val quadCoords = floatArrayOf(
            -1.0f, -1.0f,
             1.0f, -1.0f,
            -1.0f,  1.0f,
             1.0f,  1.0f
        )
        val texCoords = floatArrayOf(
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
        )

        vertexBuffer = ByteBuffer.allocateDirect(quadCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(quadCoords)
        vertexBuffer.position(0)

        texCoordBuffer = ByteBuffer.allocateDirect(texCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(texCoords)
        texCoordBuffer.position(0)

        Matrix.setIdentityM(identityMatrix, 0)
        Matrix.orthoM(projectionMatrix, 0, 0f, outputWidth.toFloat(), outputHeight.toFloat(), 0f, -1f, 1f)

        setupFbo()
    }

    private fun setupFbo() {
        val fbos = IntArray(1)
        val textures = IntArray(1)

        GLES20.glGenFramebuffers(1, fbos, 0)
        GLES20.glGenTextures(1, textures, 0)

        fboHandle = fbos[0]
        fboTextureId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
            outputWidth, outputHeight, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboHandle)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER,
            GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D,
            fboTextureId,
            0
        )

        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("FBO creation failed with status: $status")
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun beginFboRender() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboHandle)
        GLES20.glViewport(0, 0, outputWidth, outputHeight)
        GLES20.glClearColor(0.05f, 0.05f, 0.07f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    fun renderLayer(layer: LayerConfig, textureId: Int, texMatrix: FloatArray? = null) {
        if (!layer.isVisible || textureId <= 0) return

        Matrix.setIdentityM(modelMatrix, 0)
        // Transform normalized viewport coordinates into output pixel space
        val x = layer.x * outputWidth
        val y = layer.y * outputHeight
        val w = layer.width * outputWidth
        val h = layer.height * outputHeight

        Matrix.translateM(modelMatrix, 0, x + w / 2f, y + h / 2f, 0f)
        Matrix.rotateM(modelMatrix, 0, layer.rotation, 0f, 0f, 1f)
        Matrix.scaleM(modelMatrix, 0, w / 2f, h / 2f, 1f)

        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0)

        val activeTexMatrix = texMatrix ?: identityMatrix
        if (layer.sourceType == SourceType.CAMERA || layer.sourceType == SourceType.SCREEN_CAPTURE) {
            programOes.draw(
                mvpMatrix = mvpMatrix,
                vertexBuffer = vertexBuffer,
                firstVertex = 0,
                vertexCount = 4,
                coordsPerVertex = 2,
                vertexStride = 2 * 4,
                texMatrix = activeTexMatrix,
                texBuffer = texCoordBuffer,
                textureId = textureId,
                texStride = 2 * 4,
                alpha = layer.opacity
            )
        } else {
            program2D.draw(
                mvpMatrix = mvpMatrix,
                vertexBuffer = vertexBuffer,
                firstVertex = 0,
                vertexCount = 4,
                coordsPerVertex = 2,
                vertexStride = 2 * 4,
                texMatrix = activeTexMatrix,
                texBuffer = texCoordBuffer,
                textureId = textureId,
                texStride = 2 * 4,
                alpha = layer.opacity
            )
        }
    }

    fun endFboRender() {
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    fun release() {
        val fbos = intArrayOf(fboHandle)
        val textures = intArrayOf(fboTextureId)
        GLES20.glDeleteFramebuffers(1, fbos, 0)
        GLES20.glDeleteTextures(1, textures, 0)
        program2D.release()
        programOes.release()
    }
}
