package com.cronwork.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cronwork.core.DaySummary
import com.cronwork.core.Session
import com.cronwork.core.formatHm
import com.cronwork.timer.TimerController
import com.cronwork.timer.WorkSnapshot
import com.cronwork.ui.timeFormat
import java.time.Duration
import java.time.LocalDate

/** Quick adjustments plus the editable list of sessions and adjustments of [date]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayEditor(
    date: LocalDate,
    summary: DaySummary,
    snapshot: WorkSnapshot,
    timer: TimerController,
    launch: (suspend () -> Unit) -> Unit,
    editOpenSessionRequest: Boolean = false,
    onEditOpenSessionHandled: () -> Unit = {},
) {
    val zone = timer.zone()
    val sessions = snapshot.sessions.filter { it.start.atZone(zone).toLocalDate() == date }
    val adjustments = snapshot.adjustments.filter { it.date == date }
    var editing by remember { mutableStateOf<Session?>(null) }
    var adding by remember { mutableStateOf(false) }
    var customAdjust by remember { mutableStateOf(false) }

    LaunchedEffect(editOpenSessionRequest) {
        if (editOpenSessionRequest) {
            snapshot.sessions.firstOrNull { it.end == null }?.let { editing = it }
            onEditOpenSessionHandled()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ajuste rápido", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(-30, -15, -5, 5, 15, 30).forEach { m ->
                AssistChip(
                    onClick = { launch { timer.adjust(date, m, null) } },
                    label = { Text(if (m > 0) "+$m" else "−${-m}") },
                )
            }
            AssistChip(onClick = { customAdjust = true }, label = { Text("Otro…") })
        }

        if (summary.crossesMidnight) Warning("Hay un tramo que pasa de medianoche: cuenta entero para este día. Revísalo si te olvidaste de parar.")
        if (summary.adjustmentClamped) Warning("Los ajustes restan más de lo trabajado: el día cuenta como 0.")
        if (summary.pauseApplied) Text("Incluye la pausa computable.", style = MaterialTheme.typography.bodySmall)

        Text("Tramos", style = MaterialTheme.typography.titleSmall)
        if (sessions.isEmpty()) Text("Sin tramos", color = MaterialTheme.colorScheme.onSurfaceVariant)
        sessions.forEach { s ->
            val endText = s.end?.atZone(zone)?.format(timeFormat) ?: "en marcha"
            val duration = Duration.between(s.start, s.end ?: timer.now()).seconds
            Row(
                Modifier.fillMaxWidth().clickable { editing = s }.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${s.start.atZone(zone).format(timeFormat)} – $endText")
                Text(formatHm(duration), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
        }
        OutlinedButton(onClick = { adding = true }) { Text("Añadir tramo") }

        if (adjustments.isNotEmpty()) {
            Text("Ajustes", style = MaterialTheme.typography.titleSmall)
            adjustments.forEach { a ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (if (a.minutes > 0) "+" else "−") + formatHm(kotlin.math.abs(a.minutes) * 60L) +
                            (a.note?.let { " · $it" } ?: ""),
                        Modifier.weight(1f),
                    )
                    IconButton(onClick = { launch { timer.deleteAdjustment(a.id) } }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Borrar ajuste")
                    }
                }
            }
        }
    }

    editing?.let { s ->
        SessionEditDialog(
            date = date, session = s, zone = zone,
            onDismiss = { editing = null },
            onSave = { timer.editSession(it) },
            onDelete = { launch { timer.deleteSession(s.id) } },
            launch = launch,
        )
    }
    if (adding) {
        SessionEditDialog(
            date = date, session = null, zone = zone,
            onDismiss = { adding = false },
            onSave = { timer.editSession(it) },
            onDelete = null,
            launch = launch,
        )
    }
    if (customAdjust) {
        AdjustDialog(onDismiss = { customAdjust = false }, onSave = { m, note -> launch { timer.adjust(date, m, note) } })
    }
}

@Composable
fun Warning(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(text, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}
