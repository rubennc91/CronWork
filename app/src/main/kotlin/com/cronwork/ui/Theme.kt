package com.cronwork.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Blue = Color(0xFF1E5AA8)

@Composable
fun CronWorkTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF9EC3FF), secondary = Color(0xFF8FD3A8))
    } else {
        lightColorScheme(primary = Blue, secondary = Color(0xFF2E7D4F))
    }
    MaterialTheme(colorScheme = colors, content = content)
}

val SPANISH: Locale = Locale.forLanguageTag("es-ES")
val dayTitleFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", SPANISH)
val shortDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d", SPANISH)
val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", SPANISH)

fun String.capitalized(): String = replaceFirstChar { it.titlecase(SPANISH) }

/** Worked time "H:MM", with "−" when negative adjustments leave the day below zero. */
fun formatWorked(seconds: Long): String = (if (seconds / 60 < 0) "−" else "") + com.cronwork.core.formatHm(seconds)

/** Green for positive balances, red for negative. */
@Composable
fun balanceColor(seconds: Long): Color = when {
    seconds / 60 > 0 -> MaterialTheme.colorScheme.secondary
    seconds / 60 < 0 -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
