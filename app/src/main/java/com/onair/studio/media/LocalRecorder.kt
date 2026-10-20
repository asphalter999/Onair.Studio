package com.onair.studio.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Synchronous MP4 container writer using Android MediaMuxer.
 */
class LocalRecorder(private val outputFile: File) {

    private var muxer: MediaMuxer? = null
    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private val isMuxerStarted = AtomicBoolean(false)

    fun start() {
        muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    @Synchronized
    fun addVideoTrack(format: MediaFormat) {
        val m = muxer ?: return
        videoTrackIndex = m.addTrack(format)
        checkStartMuxer()
    }

    @Synchronized
    fun addAudioTrack(format: MediaFormat) {
        val m = muxer ?: return
        audioTrackIndex = m.addTrack(format)
        checkStartMuxer()
    }

    private fun checkStartMuxer() {
        if (videoTrackIndex >= 0 && audioTrackIndex >= 0 && !isMuxerStarted.get()) {
            muxer?.start()
            isMuxerStarted.set(true)
        }
    }

    @Synchronized
    fun writeVideoSample(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (!isMuxerStarted.get() || videoTrackIndex < 0) return
        muxer?.writeSampleData(videoTrackIndex, buffer, info)
    }

    @Synchronized
    fun writeAudioSample(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (!isMuxerStarted.get() || audioTrackIndex < 0) return
        muxer?.writeSampleData(audioTrackIndex, buffer, info)
    }

    fun stop() {
        if (isMuxerStarted.getAndSet(false)) {
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {}
        }
        muxer = null
        videoTrackIndex = -1
        audioTrackIndex = -1
    }
}
