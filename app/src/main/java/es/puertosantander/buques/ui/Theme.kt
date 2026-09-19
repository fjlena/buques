package es.puertosantander.buques.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AzulPuerto = Color(0xFF0B3C5D)
private val AzulClaro = Color(0xFF1D6FA5)
private val Cian = Color(0xFF7FC6E8)
private val Ambar = Color(0xFFB26A00)
private val AmbarClaro = Color(0xFFFFB74D)

private val Claro = lightColorScheme(
    primary = AzulPuerto,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E9F5),
    onPrimaryContainer = AzulPuerto,
    secondary = AzulClaro,
    tertiary = Ambar,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE3BC),
    onTertiaryContainer = Color(0xFF5C3400),
    surface = Color(0xFFF7FAFC),
    background = Color(0xFFF7FAFC)
)

private val Oscuro = darkColorScheme(
    primary = Cian,
    onPrimary = Color(0xFF00344D),
    primaryContainer = Color(0xFF004C6E),
    onPrimaryContainer = Cian,
    secondary = Color(0xFF9CCBE8),
    tertiary = AmbarClaro,
    onTertiary = Color(0xFF3F2500),
    tertiaryContainer = Color(0xFF5C3400),
    onTertiaryContainer = Color(0xFFFFE3BC)
)

@Composable
fun BuquesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro,
        content = content
    )
}
