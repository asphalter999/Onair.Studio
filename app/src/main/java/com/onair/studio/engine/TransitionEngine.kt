package com.onair.studio.engine

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Studio Mode Transition Engine blending Preview Scene Texture
 * into Program Scene Texture using Cut, Cross-Fade, or Wipe shaders.
 */
class TransitionEngine {

    enum class TransitionType {
        CUT,
        FADE,
        WIPE_LEFT,
        WIPE_UP
    }

    private var programHandle: Int = 0
    private var uProgressLoc: Int = 0
    private var uTypeLoc: Int = 0
    private var sTexPreviewLoc: Int = 0
    private var sTexProgramLoc: Int = 0

    private val vertexBuffer: FloatBuffer
    private val texBuffer: FloatBuffer

    init {
        val quad = floatArrayOf(
            -1f, -1f,
             1f, -1f,
            -1f,  1f,
             1f,  1f
        )
        val tex = floatArrayOf(
            0f, 0f,
            1f, 0f,
            0f, 1f,
            1f, 1f
        )

        vertexBuffer = ByteBuffer.allocateDirect(quad.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(quad)
        vertexBuffer.position(0)

        texBuffer = ByteBuffer.allocateDirect(tex.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().put(tex)
        texBuffer.position(0)

        val vShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SRC)
        val fShader = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SRC)

        programHandle = GLES20.glCreateProgram()
        GLES20.glAttachShader(programHandle, vShader)
        GLES20.glAttachShader(programHandle, fShader)
        GLES20.glLinkProgram(programHandle)

        uProgressLoc = GLES20.glGetUniformLocation(programHandle, "uProgress")
        uTypeLoc = GLES20.glGetUniformLocation(programHandle, "uType")
        sTexPreviewLoc = GLES20.glGetUniformLocation(programHandle, "sTexPreview")
        sTexProgramLoc = GLES20.glGetUniformLocation(programHandle, "sTexProgram")
    }

    fun renderTransition(
        previewTextureId: Int,
        programTextureId: Int,
        type: TransitionType,
        progress: Float
    ) {
        GLES20.glUseProgram(programHandle)

        val typeInt = when (type) {
            TransitionType.CUT -> 0
            TransitionType.FADE -> 1
            TransitionType.WIPE_LEFT -> 2
            TransitionType.WIPE_UP -> 3
        }

        GLES20.glUniform1f(uProgressLoc, progress)
        GLES20.glUniform1i(uTypeLoc, typeInt)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, previewTextureId)
        GLES20.glUniform1i(sTexPreviewLoc, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, programTextureId)
        GLES20.glUniform1i(sTexProgramLoc, 1)

        val posLoc = GLES20.glGetAttribLocation(programHandle, "aPosition")
        val texLoc = GLES20.glGetAttribLocation(programHandle, "aTexCoord")

        GLES20.glEnableVertexAttribArray(posLoc)
        GLES20.glVertexAttribPointer(posLoc, 2, GLES20.GL_FLOAT, false, 8, vertexBuffer)

        GLES20.glEnableVertexAttribArray(texLoc)
        GLES20.glVertexAttribPointer(texLoc, 2, GLES20.GL_FLOAT, false, 8, texBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(posLoc)
        GLES20.glDisableVertexAttribArray(texLoc)
        GLES20.glUseProgram(0)
    }

    private fun compileShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }

    companion object {
        private const val VERTEX_SRC = """
            attribute vec4 aPosition;
            attribute vec2 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = aTexCoord;
            }
        """

        private const val FRAGMENT_SRC = """
            precision mediump float;
            varying vec2 vTexCoord;
            uniform sampler2D sTexPreview;
            uniform sampler2D sTexProgram;
            uniform float uProgress;
            uniform int uType;

            void main() {
                vec4 cPrev = texture2D(sTexPreview, vTexCoord);
                vec4 cProg = texture2D(sTexProgram, vTexCoord);

                if (uType == 0) { // CUT
                    gl_FragColor = (uProgress >= 1.0) ? cPrev : cProg;
                } else if (uType == 1) { // FADE
                    gl_FragColor = mix(cProg, cPrev, uProgress);
                } else if (uType == 2) { // WIPE_LEFT
                    if (vTexCoord.x < uProgress) {
                        gl_FragColor = cPrev;
                    } else {
                        gl_FragColor = cProg;
                    }
                } else { // WIPE_UP
                    if (vTexCoord.y < uProgress) {
                        gl_FragColor = cPrev;
                    } else {
                        gl_FragColor = cProg;
                    }
                }
            }
        """
    }
}
