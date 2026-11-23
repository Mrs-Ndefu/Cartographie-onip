package com.onip.cartoonip.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val OnipBlue = Color(0xFF1177C8)
private val OnipBlueDark = Color(0xFF0B4A84)
private val OnipAccent = Color(0xFF178A17)

private val LightColors = lightColorScheme(
    primary = OnipBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3EFFA),
    onPrimaryContainer = OnipBlueDark,
    secondary = OnipAccent,
    secondaryContainer = Color(0xFFE4F3E4),
    onSecondaryContainer = Color(0xFF0F5E0F),
    tertiary = OnipBlueDark,
    background = Color(0xFFF6F8FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFEEF2F7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FA8D9),
    secondary = OnipAccent,
    tertiary = Color(0xFF3E6AA0),
)

@Composable
fun CartoOnipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Couleurs « dynamiques » (Android 12+, tirées du fond d'écran) désactivées : sur certaines
    // tablettes elles donnaient des cartes bleu foncé au texte bleu, illisibles. L'app garde
    // partout les couleurs ONIP.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
