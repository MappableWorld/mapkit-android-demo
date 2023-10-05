package world.mappable.mapkitdemo.objects

import world.mappable.mapkitdemo.common.CommonColors

object ColorUtils {
    private var colorIndex = 0
    private var colors = listOf(
        CommonColors.red, CommonColors.yellow, CommonColors.lime
    )

    /**
     * Returns a different color every time.
     */
    fun polylineColor(): Int {
        return colors[colorIndex].also {
            colorIndex = (colorIndex + 1) % colors.size
        }
    }
}
