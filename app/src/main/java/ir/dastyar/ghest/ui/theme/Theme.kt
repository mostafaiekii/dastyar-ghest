package ir.dastyar.ghest.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val label: String) {
    SYSTEM("سیستم"), LIGHT("روشن"), DARK("تاریک")
}

// سبز مالی: زمردی معتبر + پس‌زمینه‌های سبزفام
private val LightColors = lightColorScheme(
    primary = Color(0xFF059669),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA7F3D0),
    onPrimaryContainer = Color(0xFF022C22),
    secondary = Color(0xFF0D9488),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Color(0xFF0369A1),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF082F49),
    background = Color(0xFFF4FAF7),
    onBackground = Color(0xFF122019),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14271F),
    surfaceVariant = Color(0xFFE6F2EC),
    onSurfaceVariant = Color(0xFF41594E),
    surfaceContainer = Color(0xFFEFF6F2),
    surfaceContainerHigh = Color(0xFFE9F1EC),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF450A0A),
    outline = Color(0xFFB3C9BE),
    outlineVariant = Color(0xFFD6E5DC)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF065F46),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF082F49),
    tertiaryContainer = Color(0xFF0C4A6E),
    onTertiaryContainer = Color(0xFFBAE6FD),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF101A2C),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1A2740),
    onSurfaceVariant = Color(0xFF9FB3C8),
    surfaceContainer = Color(0xFF152238),
    surfaceContainerHigh = Color(0xFF1A2943),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    outline = Color(0xFF3C5170),
    outlineVariant = Color(0xFF24344E)
)

// رنگ‌های وضعیت سررسید
val SuccessGreen = Color(0xFF16A34A)
val WarnAmber = Color(0xFFD97706)
val DangerRed = Color(0xFFDC2626)

@Composable
fun GhestTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
