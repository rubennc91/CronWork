package com.cronwork.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cronwork.container
import com.cronwork.core.Settings
import com.cronwork.core.formatHm
import com.cronwork.core.parseHm
import com.cronwork.timer.TimerController
import java.time.DayOfWeek

private val dayLetters = listOf("L", "M", "X", "J", "V", "S", "D")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(timer: TimerController, launch: (suspend () -> Unit) -> Unit, onOpenSpecialDays: () -> Unit) {
    val snapshot by timer.snapshots.collectAsStateWithLifecycle(initialValue = null)
    val s = snapshot?.settings ?: return
    val context = LocalContext.current
    var canExact by remember { mutableStateOf(context.container.alarms.canExact()) }
    // Re-check when coming back from the system settings page.
    LifecycleResumeEffect(Unit) {
        canExact = context.container.alarms.canExact()
        onPauseOrDispose { }
    }
    fun update(t: (Settings) -> Settings) = launch { timer.updateSettings(t) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Jornada", style = MaterialTheme.typography.titleMedium)
        HmField("Horas por día (H:MM)", s.dailyTargetMinutes) { m -> update { it.copy(dailyTargetMinutes = m) } }
        Text("Días laborables")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DayOfWeek.entries.forEachIndexed { i, d ->
                FilterChip(
                    selected = d in s.workDays,
                    onClick = { update { it.copy(workDays = if (d in it.workDays) it.workDays - d else it.workDays + d) } },
                    label = { Text(dayLetters[i]) },
                )
            }
        }
        OutlinedButton(onClick = onOpenSpecialDays) { Text("Festivos y vacaciones") }

        HorizontalDivider()
        Text("Pausa computable", style = MaterialTheme.typography.titleMedium)
        Text(
            "Si tu convenio cuenta la pausa como tiempo trabajado, actívala: se suma una vez al día cuando superas el umbral. " +
                "Si paras el contador al descansar y la pausa no cuenta, déjala desactivada.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Activada", Modifier.weight(1f))
            Switch(checked = s.pauseEnabled, onCheckedChange = { on -> update { it.copy(pauseEnabled = on) } })
        }
        if (s.pauseEnabled) {
            NumberField("Minutos de pausa", s.pauseMinutes) { m -> update { it.copy(pauseMinutes = m) } }
            HmField("A partir de (H:MM trabajadas)", s.pauseThresholdMinutes) { m -> update { it.copy(pauseThresholdMinutes = m) } }
        }

        HorizontalDivider()
        Text("Avisos", style = MaterialTheme.typography.titleMedium)
        NumberField("Avisar si lleva estas horas seguidas (0 = no)", s.forgotReminderHours) { h ->
            update { it.copy(forgotReminderHours = h) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !canExact) {
            Text(
                "Las alarmas exactas no están permitidas: los avisos pueden llegar con unos minutos de retraso.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(onClick = {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                )
            }) { Text("Permitir alarmas exactas") }
        }
    }
}

/** Text field for "H:MM" values; saves whenever the text is valid. */
@Composable
private fun HmField(label: String, minutes: Int, onValid: (Int) -> Unit) {
    var text by remember { mutableStateOf(formatHm(minutes * 60L)) }
    LaunchedEffect(minutes) { if (parseHm(text) != minutes) text = formatHm(minutes * 60L) }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; parseHm(it)?.let { m -> if (m != minutes) onValid(m) } },
        label = { Text(label) },
        isError = parseHm(text) == null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumberField(label: String, value: Int, onValid: (Int) -> Unit) {
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) { if (text.trim().toIntOrNull() != value) text = value.toString() }
    fun parse(t: String) = t.trim().toIntOrNull()?.takeIf { it in 0..999 }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; parse(it)?.let { n -> if (n != value) onValid(n) } },
        label = { Text(label) },
        isError = parse(text) == null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
