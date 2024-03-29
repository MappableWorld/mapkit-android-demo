package world.mappable.navikitdemo

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import world.mappable.mapkit.MapKitFactory
import world.mappable.mapkit.map.MapWindow
import world.mappable.navikitdemo.databinding.ActivityAppBinding
import world.mappable.navikitdemo.domain.NavigationLayerManager
import world.mappable.navikitdemo.domain.helpers.MapTapManager
import world.mappable.navikitdemo.domain.helpers.Permission
import world.mappable.navikitdemo.domain.helpers.PermissionManager
import world.mappable.navikitdemo.domain.helpers.SettingsBinderManager
import world.mappable.navikitdemo.ui.databinding.LayoutFragmentContainerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class AppActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppBinding
    lateinit var mapWindow: MapWindow

    private val viewModel: AppActivityViewModel by viewModels()

    @Inject
    lateinit var permissionManager: PermissionManager
    @Inject
    lateinit var settingsBinderManager: SettingsBinderManager
    @Inject
    lateinit var mapTapManager: MapTapManager
    @Inject
    lateinit var navigationLayerManager: NavigationLayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // We need to initialize mapWindow field before ActivityComponent graph initialization.
        binding = ActivityAppBinding.inflate(layoutInflater)
        mapWindow = binding.viewMap.mapWindow
        super.onCreate(savedInstanceState)
        // NavHostFragment creation should be after super.onCreate call.
        LayoutFragmentContainerBinding.inflate(layoutInflater, binding.layoutFragmentContainer)
        setContentView(binding.root)

        permissionManager.request(listOf(Permission.LOCATION, Permission.NOTIFICATIONS))

        viewModel.nightModeActive(resources)
            .flowWithLifecycle(lifecycle)
            .onEach { binding.viewMap.mapWindow.map.isNightModeEnabled = it }
            .launchIn(lifecycleScope)

        settingsBinderManager.applySettingsChanges(lifecycleScope)
        mapTapManager.start(lifecycleScope)
        navigationLayerManager.initIfNeeded()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.viewMap.onStart()
    }

    override fun onStop() {
        binding.viewMap.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onPause()
    }
}
