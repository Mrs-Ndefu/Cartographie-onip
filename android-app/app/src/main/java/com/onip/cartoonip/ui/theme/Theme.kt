package com.onip.cartoonip.ui.theme

import android.app.Activity
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

private val OnipBlue = Color(0xFF1D4E89)
private val OnipBlueDark = Color(0xFF0F2D52)
private val OnipAccent = Color(0xFF2E9E5B)

private val LightColors = lightColorScheme(
    primary = OnipBlue,
    secondary = OnipAccent,
    tertiary = OnipBlueDark,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FA8D9),
    secondary = OnipAccent,
    tertiary = Color(0xFF3E6AA0),
)

@Composable
fun CartoOnipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
