package com.cronwork.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cronwork.core.SpecialDay
import com.cronwork.core.SpecialDayType
import com.cronwork.core.formatHm
import com.cronwork.core.parseHm
import com.cronwork.timer.TimerController
import com.cronwork.ui.SPANISH
import com.cronwork.ui.capitalized
import com.cronwork.ui.history.label
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy", SPANISH)

@Composable
fun SpecialDaysScreen(timer: TimerController, launch: (suspend () -> Unit) -> Unit) {
    val snapshot by timer.snapshots.collectAsStateWithLifecycle(initialValue = null)
    val specials = snapshot?.specials ?: return
    var adding by remember { mutableStateOf(false) }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Los días especiales cambian el objetivo de ese día (normalmente 0:00).", style = MaterialTheme.typography.bodySmall)
        Button(onClick = { adding = true }) { Text("Añadir día") }
        if (specials.isEmpty()) Text("No hay días especiales", color = MaterialTheme.colorScheme.onSurfaceVariant)
        specials.sortedByDescending { it.date }.forEach { d ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(d.date.format(dateFormat).capitalized())
                    Text("${d.type.label()} · objetivo ${formatHm(d.targetMinutes * 60L)}", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = { launch { timer.deleteSpecialDay(d.date) } }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Borrar")
                }
            }
        }
    }

    if (adding) AddSpecialDayDialog(timer.today(), onDismiss = { adding = false }) { day -> launch { timer.upsertSpecialDay(day) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSpecialDayDialog(initial: LocalDate, onDismiss: () -> Unit, onSave: (SpecialDay) -> Unit) {
    var date by remember { mutableStateOf(initial) }
    var type by remember { mutableStateOf(SpecialDayType.HOLIDAY) }
    var target by remember { mutableStateOf("0:00") }
    var picking by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Día especial") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { picking = true }) { Text(date.format(dateFormat).capitalized()) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpecialDayType.entries.forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.label()) })
                    }
                }
                OutlinedTextField(
                    value = target, onValueChange = { target = it }, label = { Text("Objetivo del día (H:MM)") },
                    isError = parseHm(target) == null, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                parseHm(target)?.let { onSave(SpecialDay(date, type, it)); onDismiss() }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    if (picking) {
        // DatePicker works with UTC midnight millis.
        val state = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    picking = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancelar") } },
        ) { DatePicker(state = state) }
    }
}
