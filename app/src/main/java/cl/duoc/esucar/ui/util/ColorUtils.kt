package cl.duoc.esucar.ui.util

import androidx.compose.ui.graphics.Color
import cl.duoc.esucar.ui.theme.Green700

fun parseCourseColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(Green700)
