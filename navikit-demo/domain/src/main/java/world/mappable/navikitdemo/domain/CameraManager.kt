package world.mappable.navikitdemo.domain

import kotlinx.coroutines.CoroutineScope

interface CameraManager {
    enum class ZoomStep {
        PLUS,
        MINUS,
    }

    fun changeZoomByStep(step: ZoomStep)
    fun moveCameraToUserLocation()
    fun start(scope: CoroutineScope)
}
