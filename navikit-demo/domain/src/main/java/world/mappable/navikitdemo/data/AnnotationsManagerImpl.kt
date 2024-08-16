package world.mappable.navikitdemo.data

import android.content.Context
import android.util.Log
import android.widget.Toast
import world.mappable.mapkit.navigation.automotive.Annotator
import world.mappable.mapkit.navigation.automotive.AnnotatorListener
import world.mappable.navikitdemo.domain.AnnotationsManager
import world.mappable.navikitdemo.domain.NavigationHolder
import world.mappable.navikitdemo.domain.SettingsManager
import world.mappable.navikitdemo.domain.SpeakerManager
import world.mappable.navikitdemo.domain.models.AnnotatedEventsType
import world.mappable.navikitdemo.domain.models.AnnotatedRoadEventsType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnotationsManagerImpl @Inject constructor(
    navigationHolder: NavigationHolder,
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager,
    private val speaker: SpeakerManager,
) : AnnotationsManager {

    private val scope = MainScope() + Dispatchers.Main.immediate
    private var annotator: Annotator = navigationHolder.navigation.value.guidance.annotator

    private val annotatorListener = object : AnnotatorListener {
        override fun manoeuvreAnnotated() {
            Log.d(TAG, "manoeuvreAnnotated")
        }

        override fun roadEventAnnotated() {
            Log.d(TAG, "roadEventAnnotated")
        }

        override fun speedingAnnotated() {
            Log.d(TAG, "speedingAnnotated")
        }

        override fun fasterAlternativeAnnotated() {
            Log.d(TAG, "fasterAlternativeAnnotated")
        }
    }

    init {
        annotator.apply {
            setSpeaker(speaker)
            addListener(annotatorListener)
        }

        navigationHolder.navigation
            .onEach { recreateAnnotator(it.guidance.annotator) }
            .launchIn(scope)

        speaker.phrases()
            .onEach {
                tryShowAnnotationToast(it)
            }
            .launchIn(scope)
    }

    override fun setAnnotationsEnabled(isEnabled: Boolean) {
        with(annotator) {
            if (isEnabled) unmute() else mute()
        }
    }

    override fun setAnnotatedEventEnabled(event: AnnotatedEventsType, isEnabled: Boolean) {
        annotator.annotatedEvents =
            applyEventAvailabilityToMask(
                event.mapkitEnum.value,
                isEnabled,
                annotator.annotatedEvents
            )
    }

    override fun setAnnotatedRoadEventEnabled(event: AnnotatedRoadEventsType, isEnabled: Boolean) {
        annotator.annotatedRoadEvents =
            applyEventAvailabilityToMask(
                event.mapkitEnum.value,
                isEnabled,
                annotator.annotatedRoadEvents
            )
    }

    private fun applyEventAvailabilityToMask(event: Int, isEnabled: Boolean, mask: Int): Int {
        return if (isEnabled) mask or event else mask and event.inv()
    }

    private fun tryShowAnnotationToast(message: String) {
        if (settingsManager.textAnnotations.value) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun recreateAnnotator(otherAnnotator: Annotator) {
        annotator.apply {
            removeListener(annotatorListener)
            setSpeaker(null)
        }
        annotator = otherAnnotator
        annotator.apply {
            setSpeaker(speaker)
            addListener(annotatorListener)
        }
    }

    private companion object {
        const val TAG = "AnnotationsManagerImpl"
    }
}
