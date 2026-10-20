package com.onair.studio.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.onair.studio.MainActivity
import com.onair.studio.R
import com.onair.studio.media.AudioMixer
import com.onair.studio.media.HardwareEncoder
import com.onair.studio.media.LocalRecorder
import com.onair.studio.media.RtmpStreamer
import com.onair.studio.model.EncoderConfig
import com.onair.studio.model.StreamConfig
import java.io.File

/**
 * Persistent Android Foreground Service holding the hardware encoder,
 * RTMP streaming socket, local MP4 recorder, and audio mixer pipelines.
 */
class OnAirStreamingService : LifecycleService() {

    private val binder = LocalBinder()

    var isLiveStreaming = false
        private set
    var isRecording = false
        private set

    private var encoder: HardwareEncoder? = null
    private var rtmpStreamer: RtmpStreamer? = null
    private var localRecorder: LocalRecorder? = null
    private var audioMixer: AudioMixer? = null

    inner class LocalBinder : Binder() {
        fun getService(): OnAirStreamingService = this@OnAirStreamingService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_STREAM -> startStreamingPipeline()
            ACTION_STOP_STREAM -> stopStreamingPipeline()
            ACTION_START_RECORD -> startRecordingPipeline()
            ACTION_STOP_RECORD -> stopRecordingPipeline()
        }

        return START_STICKY
    }

    fun startStreamingPipeline(
        streamConfig: StreamConfig = StreamConfig(),
        encoderConfig: EncoderConfig = EncoderConfig()
    ) {
        if (isLiveStreaming) return
        isLiveStreaming = true

        startForegroundNotification("ON AIR • Streaming 1080p60 Live")

        rtmpStreamer = RtmpStreamer(streamConfig) { targetBitrate ->
            encoder?.updateBitrate(targetBitrate)
        }.apply { start() }

        encoder = HardwareEncoder(
            encoderConfig,
            onVideoEncoded = { buf, info ->
                rtmpStreamer?.submitVideoFrame(buf, info)
                localRecorder?.writeVideoSample(buf, info)
            },
            onAudioEncoded = { buf, info ->
                rtmpStreamer?.submitAudioFrame(buf, info)
                localRecorder?.writeAudioSample(buf, info)
            }
        ).apply { start() }

        audioMixer = AudioMixer { pcmBytes, ptsUs ->
            encoder?.feedPcmAudio(pcmBytes, ptsUs)
        }.apply { start() }
    }

    fun stopStreamingPipeline() {
        isLiveStreaming = false
        audioMixer?.stop()
        encoder?.stop()
        rtmpStreamer?.stop()

        if (!isRecording) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            startForegroundNotification("REC • Recording Local MP4")
        }
    }

    fun startRecordingPipeline() {
        if (isRecording) return
        isRecording = true

        val recFile = File(getExternalFilesDir(null), "OnAir_REC_${System.currentTimeMillis()}.mp4")
        localRecorder = LocalRecorder(recFile).apply { start() }
        startForegroundNotification("REC • Recording Local MP4")
    }

    fun stopRecordingPipeline() {
        isRecording = false
        localRecorder?.stop()
        localRecorder = null

        if (!isLiveStreaming) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startForegroundNotification(statusText: String) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OnAir Studio Broadcast Engine")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OnAir Live Streaming Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live broadcast status & controls"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "onair_studio_streaming_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_STREAM = "com.onair.studio.START_STREAM"
        const val ACTION_STOP_STREAM = "com.onair.studio.STOP_STREAM"
        const val ACTION_START_RECORD = "com.onair.studio.START_RECORD"
        const val ACTION_STOP_RECORD = "com.onair.studio.STOP_RECORD"
    }
}
