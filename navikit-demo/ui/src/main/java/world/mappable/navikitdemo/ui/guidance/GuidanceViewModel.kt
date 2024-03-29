package world.mappable.navikitdemo.ui.guidance

import androidx.lifecycle.ViewModel
import world.mappable.mapkit.directions.driving.DrivingRoute
import world.mappable.mapkit.geometry.geo.PolylineUtils
import world.mappable.mapkit.location.Location
import world.mappable.mapkit.navigation.automotive.Guidance
import world.mappable.mapkit.navigation.automotive.SpeedLimitStatus
import world.mappable.navikitdemo.domain.LocationManager
import world.mappable.navikitdemo.domain.NavigationManager
import world.mappable.navikitdemo.domain.SettingsManager
import world.mappable.navikitdemo.domain.SimulationManager
import world.mappable.navikitdemo.domain.helpers.BackgroundServiceManager
import world.mappable.navikitdemo.domain.isGuidanceActive
import world.mappable.navikitdemo.domain.utils.distanceLeft
import world.mappable.navikitdemo.domain.utils.localizeDistance
import world.mappable.navikitdemo.domain.utils.localizeSpeed
import world.mappable.navikitdemo.domain.utils.timeWithTraffic
import world.mappable.navikitdemo.ui.guidance.views.SpeedLimitViewState
import world.mappable.navikitdemo.ui.guidance.views.maneuver.UpcomingManeuverViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class GuidanceViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val locationManager: LocationManager,
    private val guidance: Guidance,
    private val simulationManager: SimulationManager,
    private val settingsManager: SettingsManager,
    private val backgroundServiceManager: BackgroundServiceManager,
) : ViewModel() {

    enum class SimulationSpeedChange {
        DECREES,
        INCREASE,
    }

    @OptIn(FlowPreview::class)
    val guidanceFinished: Flow<Unit> = navigationManager.currentRoute
        .map { it == null }
        .debounce(1.seconds)
        .filter { it }
        .map { }

    fun startGuidanceIfNeeded(nullableRoute: DrivingRoute?) {
        val route = nullableRoute ?: return
        if (navigationManager.isGuidanceActive) return
        navigationManager.startGuidance(route)
    }

    fun startBackgroundGuidanceServiceIfNeeded() {
        if (settingsManager.background.value && navigationManager.isGuidanceActive) {
            backgroundServiceManager.startService()
        }
    }

    fun stopGuidance() = navigationManager.stopGuidance()

    fun guidanceUiState(): Flow<GuidanceUiState> {
        return combine(
            navigationManager.roadName,
            navigationManager.roadFlags,
            locationManager.location(),
            settingsManager.simulationSpeed.changes(),
            simulationManager.simulationActive,
        ) { roadName, roadFlags, location, simulationSpeed, simulationActive ->
            val route = guidance.currentRoute
            val time = route?.timeWithTraffic()?.text ?: "undefined"
            val distance = route?.distanceLeft()?.text ?: "undefined"

            GuidanceUiState(
                roadNameText = "Street: $roadName",
                roadFlagsText = if (roadFlags.isNotEmpty()) "Flags: $roadFlags" else "",
                timeLeftText = "Time: $time",
                distanceLeftText = "Distance: $distance",
                speedLimitViewState = location?.toSpeedLimitViewState(),
                simulationSpeed = simulationSpeed.toDouble().localizeSpeed(),
                simulationPanelVisible = simulationActive,
            )
        }
            .distinctUntilChanged()
    }

    fun changeSimulationSpeed(change: SimulationSpeedChange) {
        val diff = when (change) {
            SimulationSpeedChange.DECREES -> -SIMULATION_SPEED_STEP
            SimulationSpeedChange.INCREASE -> SIMULATION_SPEED_STEP
        }

        with(settingsManager.simulationSpeed) {
            val newSpeed = (value + diff).coerceIn(SIMULATION_SPEED_RANGE)
            value = newSpeed.toFloat()
        }
    }

    fun upcomingManeuverViewState(): Flow<UpcomingManeuverViewState?> {
        return combine(
            navigationManager.upcomingManeuvers,
            navigationManager.upcomingLaneSigns,
            navigationManager.currentRoute,
            locationManager.location(),
        ) { manoeuvres, laneSigns, currentRoute, _ ->
            val route = currentRoute ?: return@combine null
            val nextManeuver = manoeuvres.firstOrNull() ?: return@combine null
            val manoeuvrePosition =
                nextManeuver.position.positionOnRoute(route.routeId) ?: return@combine null

            val distanceToManeuver = PolylineUtils.distanceBetweenPolylinePositions(
                route.geometry,
                route.position,
                manoeuvrePosition
            ).toInt() / 10 * 10

            if (distanceToManeuver == 0) return@combine null

            val nextLaneSign = laneSigns.find {
                // Shows laneSigns data only if it's position equals with the nextManeuver
                val signPosition = it.position.positionOnRoute(route.routeId) ?: return@find false
                manoeuvrePosition.segmentIndex == signPosition.segmentIndex
            }?.laneSign

            UpcomingManeuverViewState(
                action = nextManeuver.annotation.action,
                distance = distanceToManeuver.localizeDistance(),
                nextStreet = nextManeuver.annotation.toponym,
                laneSign = nextLaneSign,
            )
        }
            .distinctUntilChanged()
    }

    private fun Location.toSpeedLimitViewState(): SpeedLimitViewState? {
        val currentSpeed = speed ?: return null
        val limitSpeed = navigationManager.speedLimit()?.value ?: return null
        val speedLimitExceeded = when (navigationManager.speedLimitStatus()) {
            SpeedLimitStatus.BELOW_LIMIT -> false
            SpeedLimitStatus.STRICT_LIMIT_EXCEEDED, SpeedLimitStatus.TOLERANT_LIMIT_EXCEEDED -> true
        }
        return SpeedLimitViewState(currentSpeed, limitSpeed, speedLimitExceeded)
    }

    private companion object {
        // Internal MapKit restriction for simulation speed
        // in m/s.
        val SIMULATION_SPEED_RANGE = 0.0..40.0
        const val SIMULATION_SPEED_STEP = 2.0
    }
}
