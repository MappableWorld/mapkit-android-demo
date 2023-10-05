package world.mappable.mapkitdemo.search.data

import android.text.Spannable
import android.text.style.ForegroundColorSpan
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.view.isVisible
import world.mappable.mapkit.SpannableString
import world.mappable.mapkit.geometry.BoundingBox
import world.mappable.mapkit.map.VisibleRegion

fun VisibleRegion.toBoundingBox() = BoundingBox(bottomLeft, topRight)

fun SpannableString.toSpannable(@ColorInt color: Int): Spannable {
    val spannableString = android.text.SpannableString(text)
    spans.forEach {
        spannableString.setSpan(
            ForegroundColorSpan(color),
            it.begin,
            it.end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    return spannableString
}

fun <T> List<T>.takeIfNotEmpty(): List<T>? = takeIf { it.isNotEmpty() }
