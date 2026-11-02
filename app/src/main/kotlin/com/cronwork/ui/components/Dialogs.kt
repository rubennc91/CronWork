package com.cronwork.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cronwork.core.EditError
import com.cronwork.core.Session
import com.cronwork.core.parseHm
import com.cronwork.ui.timeFormat
import java.time.LocalDate
import java.time.ZoneId

fun EditError.message(): String = when (this) {
    EditError.EndBeforeStart -> "El fin es anterior al inicio"
    EditError.Overlaps -> "Se solapa con otro tramo"
    EditError.InFuture -> "No puede terminar en el futuro"
}

/**
 * Creates or edits a session on [date]. Times are "HH:MM" on that day.
 * For the running session the end may be left empty to keep it running.
 * [onSave] returns an error to show, or null when saved.
 */
@Composable
fun SessionEditDialog(
    date: LocalDate,
    session: Session?,
    zone: ZoneId,
    onDismiss: () -> Unit,
    onSave: suspend (Session) -> EditError?,
    onDelete: (() -> Unit)?,
    launch: (suspend () -> Unit) -> Unit,
) {
    val isOpen = session != null && session.end == null
    var start by remember { mutableStateOf(session?.start?.atZone(zone)?.format(timeFormat) ?: "") }
    var end by remember { mutableStateOf(session?.end?.atZone(zone)?.format(timeFormat) ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    val startDate = session?.start?.atZone(zone)?.toLocalDate() ?: date

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (session == null) "Añadir tramo" else if (isOpen) "Tramo en marcha" else "Editar tramo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = start, onValueChange = { start = it }, label = { Text("Inicio (HH:MM)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = end, onValueChange = { end = it },
                    label = { Text(if (isOpen) "Fin (vacío = sigue en marcha)" else "Fin (HH:MM)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val startMin = parseHm(start)
                val endMin = if (end.isBlank()) null else parseHm(end)
                when {
                    startMin == null || startMin >= 24 * 60 -> error = "Hora de inicio no válida"
                    end.isNotBlank() && (endMin == null || endMin > 24 * 60) -> error = "Hora de fin no válida"
                    end.isBlank() && !isOpen -> error = "Indica la hora de fin"
                    else -> {
                        val startInstant = startDate.atStartOfDay(zone).plusMinutes(startMin.toLong()).toInstant()
                        val endInstant = endMin?.let { startDate.atStartOfDay(zone).plusMinutes(it.toLong()).toInstant() }
                        val edited = Session(session?.id ?: 0, startInstant, endInstant)
                        launch {
                            val result = onSave(edited)
                            if (result == null) onDismiss() else error = result.message()
                        }
                    }
                }
            }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = { onDelete(); onDismiss() }) { Text("Borrar") }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}

/** Free amount adjustment: minutes to add or subtract plus an optional note. */
@Composable
fun AdjustDialog(onDismiss: () -> Unit, onSave: (minutes: Int, note: String?) -> Unit) {
    var add by remember { mutableStateOf(true) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajustar tiempo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = add, onClick = { add = true }, label = { Text("Sumar") })
                    FilterChip(selected = !add, onClick = { add = false }, label = { Text("Restar") })
                }
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it }, label = { Text("Tiempo (H:MM o minutos)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Nota (opcional)") }, singleLine = true)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Plain numbers are minutes ("45"); "1:15" is hours and minutes.
                val minutes = if (amount.contains(":")) parseHm(amount) else amount.trim().toIntOrNull()?.takeIf { it >= 0 }
                if (minutes == null || minutes == 0) {
                    error = "Indica un tiempo válido"
                } else {
                    onSave(if (add) minutes else -minutes, note.ifBlank { null })
                    onDismiss()
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
