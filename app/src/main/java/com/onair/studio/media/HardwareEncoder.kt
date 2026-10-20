package com.onair.studio.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import com.onair.studio.model.EncoderConfig
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Dual hardware-accelerated MediaCodec encoder managing H.264 video
 * input via Surface and AAC audio input via direct PCM ByteBuffer submission.
 */
class HardwareEncoder(
    private val config: EncoderConfig,
    private val onVideoEncoded: (buffer: ByteBuffer, info: MediaCodec.BufferInfo) -> Unit,
    private val onAudioEncoded: (buffer: ByteBuffer, info: MediaCodec.BufferInfo) -> Unit
) {
    private var videoEncoder: MediaCodec? = null
    private var audioEncoder: MediaCodec? = null
    var inputSurface: Surface? = null
        private set

    private val isRunning = AtomicBoolean(false)
    private var videoDrainThread: Thread? = null
    private var audioDrainThread: Thread? = null

    fun start() {
        if (isRunning.getAndSet(true)) return

        setupVideoEncoder()
        setupAudioEncoder()

        startVideoDrainLoop()
        startAudioDrainLoop()
    }

    private fun setupVideoEncoder() {
        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            config.videoWidth,
            config.videoHeight
        ).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, config.videoBitrateKbps * 1000)
            setInteger(MediaFormat.KEY_FRAME_RATE, config.videoFps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, config.keyFrameIntervalSec)
            setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
            setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh)
            setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel41)
        }

        videoEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = createInputSurface()
            start()
        }
    }

    private fun setupAudioEncoder() {
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            config.audioSampleRate,
            config.audioChannelCount
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, config.audioBitrateKbps * 1000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        audioEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC).apply {
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            start()
        }
    }

    fun feedPcmAudio(pcmData: ByteArray, ptsUs: Long) {
        val encoder = audioEncoder ?: return
        if (!isRunning.get()) return

        val inputIndex = encoder.dequeueInputBuffer(10000L)
        if (inputIndex >= 0) {
            val inputBuffer = encoder.getInputBuffer(inputIndex) ?: return
            inputBuffer.clear()
            inputBuffer.put(pcmData)
            encoder.queueInputBuffer(inputIndex, 0, pcmData.size, ptsUs, 0)
        }
    }

    private fun startVideoDrainLoop() {
        videoDrainThread = Thread({
            val bufferInfo = MediaCodec.BufferInfo()
            val encoder = videoEncoder ?: return@Thread

            while (isRunning.get()) {
                val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000L)
                when {
                    outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // Header NAL units (SPS / PPS) available
                    }
                    outputIndex >= 0 -> {
                        val outputBuffer = encoder.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            onVideoEncoded(outputBuffer, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
        }, "OnAir-VideoDrain-Thread").apply { start() }
    }

    private fun startAudioDrainLoop() {
        audioDrainThread = Thread({
            val bufferInfo = MediaCodec.BufferInfo()
            val encoder = audioEncoder ?: return@Thread

            while (isRunning.get()) {
                val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000L)
                when {
                    outputIndex >= 0 -> {
                        val outputBuffer = encoder.getOutputBuffer(outputIndex)
                        if (outputBuffer != null && bufferInfo.size > 0) {
                            outputBuffer.position(bufferInfo.offset)
                            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                            onAudioEncoded(outputBuffer, bufferInfo)
                        }
                        encoder.releaseOutputBuffer(outputIndex, false)
                    }
                }
            }
        }, "OnAir-AudioDrain-Thread").apply { start() }
    }

    fun updateBitrate(newBitrateKbps: Int) {
        val bundle = android.os.Bundle().apply {
            putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, newBitrateKbps * 1000)
        }
        videoEncoder?.setParameters(bundle)
    }

    fun stop() {
        isRunning.set(false)
        videoDrainThread?.interrupt()
        audioDrainThread?.interrupt()

        try {
            videoEncoder?.stop()
            videoEncoder?.release()
        } catch (_: Exception) {}

        try {
            audioEncoder?.stop()
            audioEncoder?.release()
        } catch (_: Exception) {}

        videoEncoder = null
        audioEncoder = null
        inputSurface?.release()
        inputSurface = null
    }
}
