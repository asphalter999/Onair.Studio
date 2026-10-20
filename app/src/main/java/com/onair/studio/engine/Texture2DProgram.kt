package com.onair.studio.engine

import android.opengl.GLES11Ext
import android.opengl.GLES20
import java.nio.FloatBuffer

/**
 * GL program handling texture drawing for both standard TEXTURE_2D and
 * Android Camera/Screen Capture TEXTURE_EXTERNAL_OES.
 */
class Texture2DProgram(val programType: ProgramType) {

    enum class ProgramType {
        TEXTURE_2D,
        TEXTURE_EXT_OES
    }

    private var programHandle: Int = 0
    private var uMVPMatrixLoc: Int = 0
    private var uTexMatrixLoc: Int = 0
    private var uAlphaLoc: Int = 0
    private var aPositionLoc: Int = 0
    private var aTextureCoordLoc: Int = 0

    init {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(
            GLES20.GL_FRAGMENT_SHADER,
            if (programType == ProgramType.TEXTURE_EXT_OES) FRAGMENT_SHADER_EXT else FRAGMENT_SHADER_2D
        )

        programHandle = GLES20.glCreateProgram()
        GLES20.glAttachShader(programHandle, vertexShader)
        GLES20.glAttachShader(programHandle, fragmentShader)
        GLES20.glLinkProgram(programHandle)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            val log = GLES20.glGetProgramInfoLog(programHandle)
            GLES20.glDeleteProgram(programHandle)
            throw RuntimeException("Could not link GL program: $log")
        }

        uMVPMatrixLoc = GLES20.glGetUniformLocation(programHandle, "uMVPMatrix")
        uTexMatrixLoc = GLES20.glGetUniformLocation(programHandle, "uTexMatrix")
        uAlphaLoc = GLES20.glGetUniformLocation(programHandle, "uAlpha")
        aPositionLoc = GLES20.glGetAttribLocation(programHandle, "aPosition")
        aTextureCoordLoc = GLES20.glGetAttribLocation(programHandle, "aTextureCoord")
    }

    fun draw(
        mvpMatrix: FloatArray,
        vertexBuffer: FloatBuffer,
        firstVertex: Int,
        vertexCount: Int,
        coordsPerVertex: Int,
        vertexStride: Int,
        texMatrix: FloatArray,
        texBuffer: FloatBuffer,
        textureId: Int,
        texStride: Int,
        alpha: Float = 1.0f
    ) {
        GLES20.glUseProgram(programHandle)

        val target = if (programType == ProgramType.TEXTURE_EXT_OES) {
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        } else {
            GLES20.GL_TEXTURE_2D
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(target, textureId)

        GLES20.glUniformMatrix4fv(uMVPMatrixLoc, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uTexMatrixLoc, 1, false, texMatrix, 0)
        GLES20.glUniform1f(uAlphaLoc, alpha)

        GLES20.glEnableVertexAttribArray(aPositionLoc)
        GLES20.glVertexAttribPointer(aPositionLoc, coordsPerVertex, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)

        GLES20.glEnableVertexAttribArray(aTextureCoordLoc)
        GLES20.glVertexAttribPointer(aTextureCoordLoc, 2, GLES20.GL_FLOAT, false, texStride, texBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount)

        GLES20.glDisableVertexAttribArray(aPositionLoc)
        GLES20.glDisableVertexAttribArray(aTextureCoordLoc)
        GLES20.glBindTexture(target, 0)
        GLES20.glUseProgram(0)
    }

    fun release() {
        if (programHandle != 0) {
            GLES20.glDeleteProgram(programHandle)
            programHandle = 0
        }
    }

    private fun compileShader(shaderType: Int, source: String): Int {
        val shader = GLES20.glCreateShader(shaderType)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            val info = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw RuntimeException("Could not compile shader $shaderType: $info")
        }
        return shader
    }

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uMVPMatrix;
            uniform mat4 uTexMatrix;
            attribute vec4 aPosition;
            attribute vec4 aTextureCoord;
            varying vec2 vTextureCoord;
            void main() {
                gl_Position = uMVPMatrix * aPosition;
                vTextureCoord = (uTexMatrix * aTextureCoord).xy;
            }
        """

        private const val FRAGMENT_SHADER_2D = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D sTexture;
            uniform float uAlpha;
            void main() {
                vec4 color = texture2D(sTexture, vTextureCoord);
                gl_FragColor = vec4(color.rgb, color.a * uAlpha);
            }
        """

        private const val FRAGMENT_SHADER_EXT = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform samplerExternalOES sTexture;
            uniform float uAlpha;
            void main() {
                vec4 color = texture2D(sTexture, vTextureCoord);
                gl_FragColor = vec4(color.rgb, color.a * uAlpha);
            }
        """
    }
}
