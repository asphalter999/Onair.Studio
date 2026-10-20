package com.onair.studio.media

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.onair.studio.model.AudioChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.log10
import kotlin.math.max

/**
 * Multichannel Audio Mixer handling low-latency microphone capture,
 * volume normalization, mute toggles, real-time VU decibel metering, and PTS timestamps.
 */
class AudioMixer(
    val sampleRate: Int = 48000,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_STEREO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    private val onPcmFrameAvailable: (data: ByteArray, ptsUs: Long) -> Unit
) {
    private val isRecording = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null

    private val channels = ConcurrentHashMap<String, AudioChannel>()

    // Observable VU meter levels in dBFS (-60 dB to 0 dB)
    private val _micVuDb = MutableStateFlow(-60f)
    val micVuDb: StateFlow<Float> = _micVuDb

    private val _systemVuDb = MutableStateFlow(-60f)
    val systemVuDb: StateFlow<Float> = _systemVuDb

    fun registerChannel(channel: AudioChannel) {
        channels[channel.id] = channel
    }

    fun updateChannelVolume(id: String, volume: Float, isMuted: Boolean) {
        channels[id]?.let { ch ->
            channels[id] = ch.copy(volume = volume, isMuted = isMuted)
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRecording.getAndSet(true)) return

        val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = max(minBufSize * 2, 4096)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()

        captureThread = Thread({
            val audioBuffer = ShortArray(bufferSize / 2)
            val byteBuffer = ByteArray(bufferSize)

            while (isRecording.get()) {
                val readShorts = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: -1
                if (readShorts > 0) {
                    val ptsUs = System.nanoTime() / 1000L

                    // Calculate RMS amplitude for real-time VU meter
                    var sumSquares = 0.0
                    val micChannel = channels["mic"]
                    val micVol = if (micChannel?.isMuted == true) 0f else (micChannel?.volume ?: 1f)

                    for (i in 0 until readShorts) {
                        // Apply channel volume scaling & clamping
                        var sample = (audioBuffer[i] * micVol).toInt()
                        sample = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        audioBuffer[i] = sample.toShort()

                        sumSquares += sample * sample

                        // Convert short back to PCM 16-bit little endian bytes
                        byteBuffer[i * 2] = (sample and 0xFF).toByte()
                        byteBuffer[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
                    }

                    val rms = kotlin.math.sqrt(sumSquares / readShorts)
                    val db = if (rms > 1.0) {
                        20.0f * log10(rms.toFloat() / 32767.0f)
                    } else {
                        -60.0f
                    }
                    _micVuDb.value = max(db, -60.0f)

                    // Forward processed PCM frame to AAC encoder
                    val frameBytes = ByteArray(readShorts * 2)
                    System.arraycopy(byteBuffer, 0, frameBytes, 0, frameBytes.size)
                    onPcmFrameAvailable(frameBytes, ptsUs)
                }
            }
        }, "OnAir-AudioMixer-Thread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        isRecording.set(false)
        captureThread?.interrupt()
        captureThread = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        _micVuDb.value = -60f
        _systemVuDb.value = -60f
    }
}
