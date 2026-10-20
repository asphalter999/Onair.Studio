package com.onair.studio.model

enum class SourceType {
    CAMERA,
    SCREEN_CAPTURE,
    MEDIA_VIDEO,
    IMAGE_OVERLAY,
    TEXT_BANNER
}

data class LayerConfig(
    val id: String,
    val name: String,
    val sourceType: SourceType,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 1f,
    val height: Float = 1f,
    val rotation: Float = 0f,
    val opacity: Float = 1f,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val customText: String = "",
    val mediaUri: String = ""
)

data class Scene(
    val id: String,
    val name: String,
    val layers: List<LayerConfig> = emptyList(),
    val isProgramActive: Boolean = false,
    val isPreviewActive: Boolean = false
)

data class AudioChannel(
    val id: String,
    val name: String,
    val volume: Float = 0.8f,
    val isMuted: Boolean = false,
    val isBluetooth: Boolean = false
)

data class StreamConfig(
    val rtmpUrl: String = "rtmp://live.youtube.com/live2",
    val streamKey: String = "xxxx-xxxx-xxxx-xxxx",
    val targetBitrateKbps: Int = 4500,
    val srtEnabled: Boolean = false
)

data class EncoderConfig(
    val videoWidth: Int = 1920,
    val videoHeight: Int = 1080,
    val videoFps: Int = 60,
    val videoBitrateKbps: Int = 4500,
    val keyFrameIntervalSec: Int = 2,
    val audioSampleRate: Int = 48000,
    val audioBitrateKbps: Int = 160,
    val audioChannelCount: Int = 2
)
