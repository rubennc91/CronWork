package com.cronwork.tile

import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.cronwork.container
import com.cronwork.core.Settings
import com.cronwork.core.TimerState
import com.cronwork.core.formatHm
import com.cronwork.timer.TimerSideEffects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Quick Settings tile: tap to start/stop; active while the timer runs. */
class TimerTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { show(container.timer.let { it.compute(it.snapshot()) }) }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val timer = container.timer
            timer.toggle()
            show(timer.compute(timer.snapshot()))
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun show(state: TimerState) {
        val tile = qsTile ?: return
        tile.state = if (state.running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        // Some Android skins (OnePlus/OPPO) hide the subtitle, so the time goes in the label.
        // It is refreshed each time the panel is opened.
        val remaining = state.secondsToTarget
        tile.label = when {
            !state.running -> "CronWork"
            state.openSessionFromEarlierDay -> "En marcha"
            remaining > 0 -> "Quedan ${formatHm(remaining)}"
            else -> "Extra ${formatHm(remaining)}"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (state.running) "CronWork" else "Parado"
        }
        tile.updateTile()
    }
}

/** Asks the system to refresh the tile whenever the timer state changes. */
class TileUpdater(private val context: Context) : TimerSideEffects {
    override suspend fun sync(state: TimerState, settings: Settings) {
        TileService.requestListeningState(context, ComponentName(context, TimerTileService::class.java))
    }
}
