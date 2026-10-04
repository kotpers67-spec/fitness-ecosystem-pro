package com.athleteapp.pro.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CyberLimeBackground = Color(0xFF0D0D0D)
val CyberLimeSurface = Color(0xFF171717)
val CyberLimePrimary = Color(0xFFCCFF00)
val CyberLimeOnPrimary = Color(0xFF0D0D0D)
val CyberLimeText = Color(0xFFF4F4F5)
val CyberLimeBorder = Color(0x33CCFF00)

val ElectricBlueBackground = Color(0xFF0B0F19)
val ElectricBlueSurface = Color(0xFF131C2E)
val ElectricBluePrimary = Color(0xFF38BDF8)
val ElectricBlueOnPrimary = Color(0xFF0B0F19)
val ElectricBlueText = Color(0xFFF0F9FF)
val ElectricBlueBorder = Color(0x3338BDF8)

val CrimsonPowerBackground = Color(0xFF18181B)
val CrimsonPowerSurface = Color(0xFF27272A)
val CrimsonPowerPrimary = Color(0xFFF43F5E)
val CrimsonPowerOnPrimary = Color(0xFF0F172A)
val CrimsonPowerText = Color(0xFFF8FAFC)
val CrimsonPowerBorder = Color(0x33EF4444)

val CleanSwissBackground = Color(0xFFF9FAFB)
val CleanSwissSurface = Color(0xFFFFFFFF)
val CleanSwissPrimary = Color(0xFF111827)
val CleanSwissOnPrimary = Color(0xFFFFFFFF)
val CleanSwissText = Color(0xFF111827)
val CleanSwissBorder = Color(0xFFE5E7EB)

val NordicEmeraldBackground = Color(0xFF06201B)
val NordicEmeraldSurface = Color(0xFF0E332B)
val NordicEmeraldPrimary = Color(0xFF10B981)
val NordicEmeraldOnPrimary = Color(0xFF06201B)
val NordicEmeraldText = Color(0xFFECFDF5)
val NordicEmeraldBorder = Color(0x3310B981)

// 6. Dark Theme (AMOLED Black #000000 + Neutral Slate)
val DarkThemeBackground = Color(0xFF000000)
val DarkThemeSurface = Color(0xFF121212)
val DarkThemePrimary = Color(0xFFE4E4E7)
val DarkThemeOnPrimary = Color(0xFF09090B)
val DarkThemeText = Color(0xFFFAFAFA)
val DarkThemeBorder = Color(0x33A1A1AA)

enum class AthleteThemePreset(val displayName: String) {
    DARK_THEME("Темная тема (AMOLED)"),
    CYBER_LIME("Cyber Lime"),
    ELECTRIC_BLUE("Electric Blue"),
    CRIMSON_POWER("Crimson Power"),
    CLEAN_SWISS("Clean Swiss (Светлая)"),
    NORDIC_EMERALD("Nordic Emerald")
}

@Composable
fun AthleteProTheme(
    themeName: String = "Cyber Lime",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "Темная тема (AMOLED)", "Dark Theme", "AMOLED Dark" -> darkColorScheme(
            background = DarkThemeBackground,
            surface = DarkThemeSurface,
            primary = DarkThemePrimary,
            onPrimary = DarkThemeOnPrimary,
            onBackground = DarkThemeText,
            onSurface = DarkThemeText,
            surfaceVariant = Color(0xFF1E1E24),
            outline = DarkThemeBorder
        )
        "Electric Blue" -> darkColorScheme(
            background = ElectricBlueBackground,
            surface = ElectricBlueSurface,
            primary = ElectricBluePrimary,
            onPrimary = ElectricBlueOnPrimary,
            onBackground = ElectricBlueText,
            onSurface = ElectricBlueText,
            surfaceVariant = Color(0xFF1E293B),
            outline = ElectricBlueBorder
        )
        "Crimson Power" -> darkColorScheme(
            background = CrimsonPowerBackground,
            surface = CrimsonPowerSurface,
            primary = CrimsonPowerPrimary,
            onPrimary = CrimsonPowerOnPrimary,
            onBackground = CrimsonPowerText,
            onSurface = CrimsonPowerText,
            surfaceVariant = Color(0xFF3F3F46),
            outline = CrimsonPowerBorder
        )
        "Clean Swiss", "Clean Swiss (Светлая)" -> lightColorScheme(
            background = CleanSwissBackground,
            surface = CleanSwissSurface,
            primary = CleanSwissPrimary,
            onPrimary = CleanSwissOnPrimary,
            onBackground = CleanSwissText,
            onSurface = CleanSwissText,
            surfaceVariant = Color(0xFFF3F4F6),
            outline = CleanSwissBorder
        )
        "Nordic Emerald" -> darkColorScheme(
            background = NordicEmeraldBackground,
            surface = NordicEmeraldSurface,
            primary = NordicEmeraldPrimary,
            onPrimary = NordicEmeraldOnPrimary,
            onBackground = NordicEmeraldText,
            onSurface = NordicEmeraldText,
            surfaceVariant = Color(0xFF14453A),
            outline = NordicEmeraldBorder
        )
        else -> darkColorScheme(
            background = CyberLimeBackground,
            surface = CyberLimeSurface,
            primary = CyberLimePrimary,
            onPrimary = CyberLimeOnPrimary,
            onBackground = CyberLimeText,
            onSurface = CyberLimeText,
            surfaceVariant = Color(0xFF262626),
            outline = CyberLimeBorder
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
