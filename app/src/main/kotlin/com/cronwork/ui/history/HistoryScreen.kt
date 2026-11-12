package com.cronwork.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cronwork.core.WorkCalculator
import com.cronwork.core.formatHm
import com.cronwork.core.formatSigned
import com.cronwork.timer.TimerController
import com.cronwork.ui.SPANISH
import com.cronwork.ui.balanceColor
import com.cronwork.ui.formatWorked
import com.cronwork.ui.capitalized
import com.cronwork.ui.shortDayFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val weekTitle = DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", SPANISH)


@Composable
fun HistoryScreen(timer: TimerController, launch: (suspend () -> Unit) -> Unit, onOpenDay: (LocalDate) -> Unit) {
    val context = LocalContext.current
    val snapshot by timer.snapshots.collectAsStateWithLifecycle(initialValue = null)
    val snap = snapshot ?: return
    var weekEpochDay by rememberSaveable { mutableStateOf(WorkCalculator.weekStart(timer.today()).toEpochDay()) }
    val weekStart = LocalDate.ofEpochDay(weekEpochDay)
    val now = timer.now()
    val zone = timer.zone()
    val week = WorkCalculator.weekSummary(weekStart, snap.sessions, snap.adjustments, snap.specials, snap.settings, now, zone)
    val total = WorkCalculator.totalBalanceSeconds(snap.sessions, snap.adjustments, snap.specials, snap.settings, now, zone)
    val specials = snap.specials.associateBy { it.date }
    val today = timer.today()

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { weekEpochDay -= 7 }) { Icon(Icons.Filled.KeyboardArrowLeft, "Semana anterior") }
            Text("Semana del ${weekStart.format(weekTitle)}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { weekEpochDay += 7 }) { Icon(Icons.Filled.KeyboardArrowRight, "Semana siguiente") }
        }

        Card {
            Column(Modifier.padding(vertical = 4.dp)) {
                week.days.forEach { d ->
                    val future = d.date.isAfter(today)
                    Row(
                        Modifier.fillMaxWidth().clickable { onOpenDay(d.date) }.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                d.date.format(shortDayFormat).capitalized(),
                                fontWeight = if (d.date == today) FontWeight.Bold else FontWeight.Normal,
                            )
                            specials[d.date]?.let { Text(it.displayName(), style = MaterialTheme.typography.labelSmall) }
                        }
                        Text("${formatWorked(d.workedSeconds)} / ${formatHm(d.targetSeconds)}", Modifier.padding(end = 12.dp))
                        Text(
                            if (future) "" else formatSigned(d.balanceSeconds),
                            color = balanceColor(d.balanceSeconds),
                        )
                    }
                    HorizontalDivider()
                }
            }
        }

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Trabajado: ${formatWorked(week.workedSeconds)} de ${formatHm(week.targetSeconds)}")
                Row {
                    Text("Saldo de la semana: ")
                    Text(formatSigned(week.balanceSeconds), color = balanceColor(week.balanceSeconds), fontWeight = FontWeight.Bold)
                }
                Row {
                    Text("Bolsa acumulada: ")
                    Text(formatSigned(total), color = balanceColor(total), fontWeight = FontWeight.Bold)
                }
            }
        }

        OutlinedButton(onClick = { launch { shareCsv(context, timer) } }, modifier = Modifier.fillMaxWidth()) {
            Text("Exportar CSV (todo el histórico)")
        }
    }
}
