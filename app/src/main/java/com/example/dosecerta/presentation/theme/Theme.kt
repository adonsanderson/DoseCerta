package com.example.dosecerta.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DoseCertaLightColorScheme = lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = TealOnPrimaryLight,
    primaryContainer = TealPrimaryContainerLight,
    onPrimaryContainer = TealOnPrimaryContainerLight,
    secondary = SageSecondaryLight,
    onSecondary = SageOnSecondaryLight,
    secondaryContainer = SageSecondaryContainerLight,
    onSecondaryContainer = SageOnSecondaryContainerLight,
    tertiary = CoralTertiaryLight,
    onTertiary = CoralOnTertiaryLight,
    tertiaryContainer = CoralTertiaryContainerLight,
    onTertiaryContainer = CoralOnTertiaryContainerLight,
    background = PorcelainBackgroundLight,
    onBackground = InkOnBackgroundLight,
    surface = SurfaceLight,
    onSurface = InkOnBackgroundLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

private val DoseCertaDarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = TealOnPrimaryDark,
    primaryContainer = TealPrimaryContainerDark,
    onPrimaryContainer = TealOnPrimaryContainerDark,
    secondary = SageSecondaryDark,
    onSecondary = SageOnSecondaryDark,
    secondaryContainer = SageSecondaryContainerDark,
    onSecondaryContainer = SageOnSecondaryContainerDark,
    tertiary = CoralTertiaryDark,
    onTertiary = CoralOnTertiaryDark,
    tertiaryContainer = CoralTertiaryContainerDark,
    onTertiaryContainer = CoralOnTertiaryContainerDark,
    background = DeepSlateBackgroundDark,
    onBackground = CloudOnBackgroundDark,
    surface = SurfaceDark,
    onSurface = CloudOnBackgroundDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

@Immutable
data class DoseStatusColors(
    val taken: Color,
    val takenContainer: Color,
    val overdue: Color,
    val overdueContainer: Color
)

private val LightDoseStatusColors = DoseStatusColors(
    taken = TakenEmeraldLight,
    takenContainer = TakenEmeraldContainerLight,
    overdue = OverdueAmberLight,
    overdueContainer = OverdueAmberContainerLight
)

private val DarkDoseStatusColors = DoseStatusColors(
    taken = TakenEmeraldDark,
    takenContainer = TakenEmeraldContainerDark,
    overdue = OverdueAmberDark,
    overdueContainer = OverdueAmberContainerDark
)

val LocalDoseStatusColors = staticCompositionLocalOf { LightDoseStatusColors }

@Composable
fun DoseCertaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DoseCertaDarkColorScheme else DoseCertaLightColorScheme
    val statusColors = if (darkTheme) DarkDoseStatusColors else LightDoseStatusColors

    CompositionLocalProvider(LocalDoseStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DoseCertaTypography,
            shapes = DoseCertaShapes,
            content = content
        )
    }
}
