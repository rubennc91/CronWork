package com.cronwork

import android.app.Application
import android.content.Context
import com.cronwork.data.CronWorkDatabase
import com.cronwork.data.SettingsRepository
import com.cronwork.data.WorkRepository
import com.cronwork.notify.AlarmScheduler
import com.cronwork.notify.TimerNotifier
import com.cronwork.timer.TimerController
import com.cronwork.widget.WidgetUpdater

class CronWorkApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.createChannels()
    }
}

/** Manual dependency container. */
class AppContainer(val app: Application) {
    val database = CronWorkDatabase.create(app)
    val workRepository = WorkRepository(database)
    val settingsRepository = SettingsRepository(app)
    val timer = TimerController(workRepository, settingsRepository)
    val notifier = TimerNotifier(app)
    val alarms = AlarmScheduler(app)
    val widget = WidgetUpdater(app)

    init {
        timer.addSideEffects(notifier)
        timer.addSideEffects(alarms)
        timer.addSideEffects(widget)
    }
}

val Context.container: AppContainer get() = (applicationContext as CronWorkApp).container
