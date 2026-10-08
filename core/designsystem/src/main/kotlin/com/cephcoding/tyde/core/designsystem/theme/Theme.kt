package com.cephcoding.tyde.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val TydeDarkColorScheme = darkColorScheme(
    primary = TydeBlue80,
    onPrimary = TydeBlue20,
    secondary = TydeTeal80,
    background = TydeNeutral10,
    surface = TydeNeutral20,
    onBackground = TydeNeutral90,
    onSurface = TydeNeutral90,
    error = TydeError80
)

private val TydeLightColorScheme = lightColorScheme(
    primary = TydeBlue40,
    onPrimary = TydeNeutral99,
    secondary = TydeTeal40,
    background = TydeNeutral99,
    surface = TydeNeutral95,
    onBackground = TydeNeutral10,
    onSurface = TydeNeutral10,
    error = TydeError40
)

@Composable
fun TydeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> TydeDarkColorScheme
        else -> TydeLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TydeTypography,
        shapes = TydeShapes,
        content = content
    )
}
