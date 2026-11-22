package cl.duoc.esucar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsuCarColorScheme = lightColorScheme(
    primary = Green700,
    onPrimary = Color.White,
    primaryContainer = Green50,
    onPrimaryContainer = GreenDark,
    secondary = Blue700,
    onSecondary = Color.White,
    secondaryContainer = Blue50,
    onSecondaryContainer = Blue700,
    background = AppBackground,
    onBackground = Navy950,
    surface = Color.White,
    onSurface = Navy950,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate200,
    outlineVariant = Slate300,
    error = Red600,
    onError = Color.White,
    errorContainer = Amber100,
    onErrorContainer = Amber800
)

@Composable
fun ESUCARTheme(contenido: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsuCarColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = contenido
    )
}
