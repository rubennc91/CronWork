package com.cronwork.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cronwork.core.TimerState
import com.cronwork.core.formatRemaining
import com.cronwork.core.formatSigned
import com.cronwork.timer.TimerController
import com.cronwork.ui.balanceColor
import com.cronwork.ui.capitalized
import com.cronwork.ui.components.DayEditor
import com.cronwork.ui.components.Warning
import com.cronwork.ui.dayTitleFormat

@Composable
fun TodayScreen(
    timer: TimerController,
    launch: (suspend () -> Unit) -> Unit,
    editOpenSessionRequest: Boolean,
    onEditOpenSessionHandled: () -> Unit,
) {
    val state by timer.state.collectAsStateWithLifecycle(initialValue = null)
    val snapshot by timer.snapshots.collectAsStateWithLifecycle(initialValue = null)
    val st = state ?: return
    val snap = snapshot ?: return

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(st.today.date.format(dayTitleFormat).capitalized(), style = MaterialTheme.typography.titleMedium)
        Counter(st)

        Button(
            onClick = { launch { timer.toggle() } },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = if (st.running) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
        ) { Text(if (st.running) "Parar" else "Iniciar", fontSize = 22.sp) }

        if (st.openSessionFromEarlierDay) {
            Warning("El contador sigue en marcha desde otro día. Si te olvidaste de pararlo, toca el tramo en el histórico de ese día para fijar la hora de fin.")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BalanceCard("Semana", st.weekBalanceSeconds, Modifier.weight(1f))
            BalanceCard("Bolsa", st.totalBalanceSeconds, Modifier.weight(1f))
        }

        DayEditor(
            date = st.today.date, summary = st.today, snapshot = snap, timer = timer, launch = launch,
            editOpenSessionRequest = editOpenSessionRequest, onEditOpenSessionHandled = onEditOpenSessionHandled,
        )
    }
}

@Composable
private fun Counter(st: TimerState) {
    val remaining = st.today.remainingSeconds
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (st.today.targetSeconds == 0L) "Hoy no es laborable" else if (remaining > 0) "Quedan" else "Tiempo extra",
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            formatRemaining(remaining),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = if (remaining > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.secondary,
        )
        if (st.today.targetSeconds > 0) {
            LinearProgressIndicator(
                progress = { (st.today.workedSeconds.toFloat() / st.today.targetSeconds).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun BalanceCard(title: String, seconds: Long, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(formatSigned(seconds), style = MaterialTheme.typography.headlineSmall, color = balanceColor(seconds))
        }
    }
}
