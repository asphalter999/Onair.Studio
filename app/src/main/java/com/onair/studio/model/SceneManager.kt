package com.onair.studio.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Manages studio scene graphs, active Preview scene, active Program scene,
 * and layer composition hierarchies.
 */
class SceneManager {

    private val defaultScenes = listOf(
        Scene(
            id = "scene-talk",
            name = "Main Broadcast (Cam + Lower Third)",
            layers = listOf(
                LayerConfig(
                    id = "layer-cam",
                    name = "Primary 1080p Camera",
                    sourceType = SourceType.CAMERA,
                    x = 0f, y = 0f, width = 1f, height = 1f
                ),
                LayerConfig(
                    id = "layer-title",
                    name = "Lower Third Title Banner",
                    sourceType = SourceType.TEXT_BANNER,
                    x = 0.05f, y = 0.82f, width = 0.45f, height = 0.12f,
                    customText = "LIVE: Tech Talk & Mobile Architecture"
                )
            ),
            isProgramActive = true,
            isPreviewActive = false
        ),
        Scene(
            id = "scene-screen",
            name = "Gameplay / Screen Share + PiP",
            layers = listOf(
                LayerConfig(
                    id = "layer-screen",
                    name = "Android Tablet Display",
                    sourceType = SourceType.SCREEN_CAPTURE,
                    x = 0f, y = 0f, width = 1f, height = 1f
                ),
                LayerConfig(
                    id = "layer-cam-pip",
                    name = "Presenter PiP Facecam",
                    sourceType = SourceType.CAMERA,
                    x = 0.72f, y = 0.05f, width = 0.25f, height = 0.25f
                )
            ),
            isProgramActive = false,
            isPreviewActive = true
        )
    )

    private val _scenes = MutableStateFlow<List<Scene>>(defaultScenes)
    val scenes: StateFlow<List<Scene>> = _scenes.asStateFlow()

    private val _previewSceneId = MutableStateFlow("scene-screen")
    val previewSceneId: StateFlow<String> = _previewSceneId.asStateFlow()

    private val _programSceneId = MutableStateFlow("scene-talk")
    val programSceneId: StateFlow<String> = _programSceneId.asStateFlow()

    fun selectPreviewScene(sceneId: String) {
        _previewSceneId.value = sceneId
    }

    fun transitionToProgram(newProgramId: String) {
        val currentProg = _programSceneId.value
        _programSceneId.value = newProgramId
        _previewSceneId.value = currentProg
    }

    fun addScene(name: String) {
        val newScene = Scene(
            id = UUID.randomUUID().toString(),
            name = name,
            layers = emptyList()
        )
        _scenes.value = _scenes.value + newScene
    }

    fun toggleLayerVisibility(sceneId: String, layerId: String) {
        _scenes.value = _scenes.value.map { scene ->
            if (scene.id == sceneId) {
                scene.copy(layers = scene.layers.map { layer ->
                    if (layer.id == layerId) layer.copy(isVisible = !layer.isVisible) else layer
                })
            } else scene
        }
    }

    fun updateLayerTransform(sceneId: String, layerId: String, x: Float, y: Float, w: Float, h: Float) {
        _scenes.value = _scenes.value.map { scene ->
            if (scene.id == sceneId) {
                scene.copy(layers = scene.layers.map { layer ->
                    if (layer.id == layerId) layer.copy(x = x, y = y, width = w, height = h) else layer
                })
            } else scene
        }
    }
}
