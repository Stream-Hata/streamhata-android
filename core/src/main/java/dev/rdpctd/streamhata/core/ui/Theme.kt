package dev.rdpctd.streamhata.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val StreamHataBackground = Color(0xFF0F0817)
val StreamHataSurface = Color(0xFF181022)
val StreamHataSurfaceHigh = Color(0xFF22172F)
val StreamHataBorder = Color(0xFF3A2A4A)
val StreamHataText = Color(0xFFF5F1FA)
val StreamHataMuted = Color(0xFFA99BB8)
val StreamHataCyan = Color(0xFF22E0FF)
val StreamHataPink = Color(0xFFFF2EA6)
val StreamHataGreen = Color(0xFF39FF88)

private val StreamHataScheme = darkColorScheme(
    primary = StreamHataCyan,
    onPrimary = Color(0xFF061018),
    secondary = StreamHataPink,
    onSecondary = Color(0xFF1A0612),
    background = StreamHataBackground,
    onBackground = StreamHataText,
    surface = StreamHataSurface,
    onSurface = StreamHataText,
    surfaceVariant = StreamHataSurfaceHigh,
    onSurfaceVariant = StreamHataMuted,
    outline = StreamHataBorder,
    error = Color(0xFFFF6B6B)
)

private val StreamHataTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 17.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, letterSpacing = 0.4.sp)
)

private val StreamHataShapes = Shapes(
    extraSmall = RoundedCornerShape(7.dp),
    small = RoundedCornerShape(9.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun StreamHataTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = StreamHataScheme, typography = StreamHataTypography, shapes = StreamHataShapes) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F0817), Color(0xFF130B1D), Color(0xFF0F0817))
                    )
                )
        ) {
            content()
        }
    }
}
