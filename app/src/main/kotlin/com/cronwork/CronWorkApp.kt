package com.cronwork

import android.app.Application

class CronWorkApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Manual dependency container; filled in as features are added. */
class AppContainer(val app: Application)
