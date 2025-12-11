package pt.rikmartins.clubemg.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CustomColorsPalette(
    val monthSurface1: Color = Color.Unspecified,
    val monthOnSurface1: Color = Color.Unspecified,
    val monthSurface2: Color = Color.Unspecified,
    val monthOnSurface2: Color = Color.Unspecified,
    val monthSurface3: Color = Color.Unspecified,
    val monthOnSurface3: Color = Color.Unspecified,
)

val LightCustomColorsPalette = CustomColorsPalette(
    monthSurface1 = MonthSurface1,
    monthOnSurface1 = MonthOnSurface1,
    monthSurface2 = MonthSurface2,
    monthOnSurface2 = MonthOnSurface2,
    monthSurface3 = MonthSurface3,
    monthOnSurface3 = MonthOnSurface3,
)

val DarkCustomColorsPalette = CustomColorsPalette(
    monthSurface1 = MonthSurface1Dark,
    monthOnSurface1 = MonthOnSurface1Dark,
    monthSurface2 = MonthSurface2Dark,
    monthOnSurface2 = MonthOnSurface2Dark,
    monthSurface3 = MonthSurface3Dark,
    monthOnSurface3 = MonthOnSurface3Dark,
)

val LocalCustomColorsPalette = staticCompositionLocalOf { CustomColorsPalette() }

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
)

@Composable
fun AppTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(
        LocalCustomColorsPalette provides if (useDarkTheme) DarkCustomColorsPalette else LightCustomColorsPalette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

