package world.mappable.navikitdemo.data

import android.content.Context
import android.speech.tts.TextToSpeech
import world.mappable.mapkit.annotations.AnnotationLanguage
import world.mappable.mapkit.annotations.LocalizedPhrase
import world.mappable.navikitdemo.domain.SettingsManager
import world.mappable.navikitdemo.domain.SpeakerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeakerImpl @Inject constructor(
    @ApplicationContext context: Context,
    private val settingsManager: SettingsManager,
) : SpeakerManager {

    private val scope = MainScope()

    private var ttsInitialized = false
    private val tts = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ttsInitialized = true
            updateTtsLanguage()
        }
    }
    private val phrasesImpl = MutableSharedFlow<String>()

    init {
        settingsManager.annotationLanguage.changes()
            .onEach {
                updateTtsLanguage()
            }
            .launchIn(scope)
    }

    override fun phrases(): Flow<String> = phrasesImpl

    override fun reset() {
        tts.stop()
    }

    override fun say(phrase: LocalizedPhrase) {
        tts.speak(phrase.text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        scope.launch {
            phrasesImpl.emit(phrase.text)
        }
    }

    override fun duration(phrase: LocalizedPhrase): Double {
        // Heuristic formula for the russian language.
        return phrase.text.length * 0.06 + 0.6
    }

    private fun updateTtsLanguage() {
        val language = settingsManager.annotationLanguage.value
        tts.language = language.toLocale()
    }

    private fun AnnotationLanguage.toLocale(): Locale {
        return when (this) {
            AnnotationLanguage.RUSSIAN -> Locale("ru", "RU")
            AnnotationLanguage.ENGLISH -> Locale("en", "US")
            AnnotationLanguage.ITALIAN -> Locale("it", "IT")
            AnnotationLanguage.FRENCH -> Locale("fr", "FR")
            AnnotationLanguage.TURKISH -> Locale("tr", "TR")
            AnnotationLanguage.UKRAINIAN -> Locale("uk", "UA")
            AnnotationLanguage.HEBREW -> Locale("he", "IL")
            AnnotationLanguage.SERBIAN -> Locale("sr-Latn", "RS")
            AnnotationLanguage.LATVIAN -> Locale("lv", "LV")
            AnnotationLanguage.FINNISH -> Locale("fi", "FI")
            AnnotationLanguage.ROMANIAN -> Locale("ro", "RO")
            AnnotationLanguage.KYRGYZ -> Locale("ky", "KG")
            AnnotationLanguage.KAZAKH -> Locale("kk", "KZ")
            AnnotationLanguage.LITHUANIAN -> Locale("lt", "LT")
            AnnotationLanguage.ESTONIAN -> Locale("et", "EE")
            AnnotationLanguage.GEORGIAN -> Locale("ka", "GE")
            AnnotationLanguage.UZBEK -> Locale("uz", "UZ")
            AnnotationLanguage.ARMENIAN -> Locale("hy", "AM")
            AnnotationLanguage.AZERBAIJANI -> Locale("az", "AZ")
            AnnotationLanguage.ARABIC -> Locale("ar", "AE")
            AnnotationLanguage.TATAR -> Locale("tt", "RU")
            AnnotationLanguage.PORTUGUESE -> Locale("pt", "PT")
            AnnotationLanguage.LATIN_AMERICAN_SPANISH -> Locale("es-419", "BO")
            AnnotationLanguage.BASHKIR -> Locale("ba", "RU")
        }
    }
}
