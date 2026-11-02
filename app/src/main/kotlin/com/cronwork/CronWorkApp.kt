package com.cronwork

import android.app.Application
import android.content.Context
import com.cronwork.data.CronWorkDatabase
import com.cronwork.data.SettingsRepository
import com.cronwork.data.WorkRepository
import com.cronwork.timer.TimerController

class CronWorkApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency container. */
class AppContainer(val app: Application) {
    val database = CronWorkDatabase.create(app)
    val workRepository = WorkRepository(database)
    val settingsRepository = SettingsRepository(app)
    val timer = TimerController(workRepository, settingsRepository)
}

val Context.container: AppContainer get() = (applicationContext as CronWorkApp).container
