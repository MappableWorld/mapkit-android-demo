package world.mappable.navikitdemo.data

import android.util.Base64
import world.mappable.mapkit.LocalizedValue
import world.mappable.mapkit.RequestPoint
import world.mappable.mapkit.directions.driving.DrivingRoute
import world.mappable.mapkit.navigation.automotive.Navigation
import world.mappable.mapkit.navigation.automotive.NavigationSerialization
import world.mappable.mapkit.navigation.automotive.RouteChangeReason
import world.mappable.mapkit.navigation.automotive.SpeedLimitStatus
import world.mappable.mapkit.navigation.automotive.UpcomingLaneSign
import world.mappable.mapkit.navigation.automotive.UpcomingManoeuvre
import world.mappable.mapkit.navigation.automotive.WindshieldListener
import world.mappable.navikitdemo.domain.NavigationManager
import world.mappable.navikitdemo.domain.RequestPointsManager
import world.mappable.navikitdemo.domain.SettingsManager
import world.mappable.navikitdemo.domain.SimulationManager
import world.mappable.navikitdemo.domain.VehicleOptionsManager
import world.mappable.navikitdemo.domain.helpers.BackgroundServiceManager
import world.mappable.navikitdemo.domain.helpers.SimpleGuidanceListener
import world.mappable.navikitdemo.domain.utils.buildFlagsString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class NavigationManagerImpl @Inject constructor(
    private val navigation: Navigation,
    private val routeRequestPointsManager: RequestPointsManager,
    private val vehicleOptionsManager: VehicleOptionsManager,
    private val settingsManager: SettingsManager,
    private val simulationManager: SimulationManager,
    private val backgroundServiceManager: BackgroundServiceManager,
) : NavigationManager {

    private val roadNameImpl = MutableStateFlow("")
    override val roadName: Flow<String> = roadNameImpl.buffer()

    private val upcomingManeuversImpl = MutableStateFlow(navigation.guidance.windshield.manoeuvres)
    override val upcomingManeuvers: Flow<List<UpcomingManoeuvre>> = upcomingManeuversImpl

    private val upcomingLaneSignsImpl = MutableStateFlow(navigation.guidance.windshield.laneSigns)
    override val upcomingLaneSigns: Flow<List<UpcomingLaneSign>> = upcomingLaneSignsImpl

    private val currentRouteImpl = MutableStateFlow(navigation.guidance.currentRoute)
    override val currentRoute: StateFlow<DrivingRoute?> = currentRouteImpl

    override val roadFlags: Flow<String> = currentRoute.map { it?.buildFlagsString() ?: "" }

    private val guidanceListener = object : SimpleGuidanceListener() {
        override fun onRouteFinished() {
            CoroutineScope(Dispatchers.IO).launch {
                // Stop guidance with delay, so the route finish annotation doesn't cancel.
                delay(0.1.seconds)
                withContext(Dispatchers.Main) {
                    stopGuidance()
                }
            }
        }

        override fun onRoadNameChanged() {
            roadNameImpl.value = navigation.guidance.roadName ?: ""
        }

        override fun onCurrentRouteChanged(reason: RouteChangeReason) {
            currentRouteImpl.value = navigation.guidance.currentRoute
        }
    }

    private val windshieldListener = object : WindshieldListener {
        override fun onManoeuvresChanged() {
            upcomingManeuversImpl.value = navigation.guidance.windshield.manoeuvres
        }

        override fun onLaneSignChanged() {
            upcomingLaneSignsImpl.value = navigation.guidance.windshield.laneSigns
        }

        override fun onRoadEventsChanged() = Unit
        override fun onDirectionSignChanged() = Unit
    }

    init {
        navigation.guidance.addListener(guidanceListener)
        navigation.guidance.windshield.addListener(windshieldListener)
    }

    override fun serializeNavigation() {
        val serialized = NavigationSerialization.serialize(navigation)
        settingsManager.serializedNavigation.value =
            Base64.encodeToString(serialized, Base64.DEFAULT)
    }

    override fun requestRoutes(points: List<RequestPoint>) {
        navigation.vehicleOptions = vehicleOptionsManager.vehicleOptions()
        navigation.requestRoutes(
            points,
            navigation.guidance.location?.heading,
            null
        )
    }

    override fun startGuidance(route: DrivingRoute) {
        if (
            navigation.routes.map { it.routeId }.contains(route.routeId)
            || navigation.guidance.currentRoute == null
        ) {
            navigation.startGuidance(route)
        }
        if (settingsManager.simulation.value) {
            navigation.guidance.currentRoute?.let {
                simulationManager.startSimulation(it)
            }
        }
    }

    override fun stopGuidance() {
        routeRequestPointsManager.resetPoints()
        navigation.stopGuidance()
        navigation.resetRoutes()
        simulationManager.stopSimulation()
        backgroundServiceManager.stopService()
        settingsManager.serializedNavigation.value = ""
    }

    override fun resetRoutes() {
        navigation.resetRoutes()
    }

    override fun resume() {
        navigation.resume()
        simulationManager.resume()
    }

    override fun suspend() {
        navigation.suspend()
        simulationManager.suspend()
    }

    override fun speedLimit(): LocalizedValue? = navigation.guidance.speedLimit

    override fun speedLimitStatus(): SpeedLimitStatus = navigation.guidance.speedLimitStatus
}
