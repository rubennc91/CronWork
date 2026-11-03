package com.cronwork.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cronwork.core.WorkCalculator
import com.cronwork.core.formatHm
import com.cronwork.core.formatSigned
import com.cronwork.timer.TimerController
import com.cronwork.ui.balanceColor
import com.cronwork.ui.formatWorked
import com.cronwork.ui.capitalized
import com.cronwork.ui.components.DayEditor
import com.cronwork.ui.dayTitleFormat
import java.time.LocalDate

@Composable
fun DayDetailScreen(date: LocalDate, timer: TimerController, launch: (suspend () -> Unit) -> Unit) {
    val snapshot by timer.snapshots.collectAsStateWithLifecycle(initialValue = null)
    val snap = snapshot ?: return
    val summary = WorkCalculator.daySummary(
        date, snap.sessions, snap.adjustments, snap.specials.firstOrNull { it.date == date },
        snap.settings, timer.now(), timer.zone(),
    )
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(date.format(dayTitleFormat).capitalized(), style = MaterialTheme.typography.titleLarge)
        Text("Trabajado ${formatWorked(summary.workedSeconds)} de ${formatHm(summary.targetSeconds)}")
        Text("Saldo ${formatSigned(summary.balanceSeconds)}", color = balanceColor(summary.balanceSeconds))
        DayEditor(date = date, summary = summary, snapshot = snap, timer = timer, launch = launch)
    }
}
