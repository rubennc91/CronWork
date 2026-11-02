package com.cronwork.notify

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.cronwork.MainActivity

/** Broadcast actions shared by notification, alarms and widget. */
object Actions {
    const val STOP = "com.cronwork.action.STOP"
    const val TOGGLE = "com.cronwork.action.TOGGLE"
    const val START = "com.cronwork.action.START"
    const val STILL_WORKING = "com.cronwork.action.STILL_WORKING"
    const val END_OF_DAY = "com.cronwork.action.END_OF_DAY"
    const val FORGOT_CHECK = "com.cronwork.action.FORGOT_CHECK"
    const val DAY_CHANGED = "com.cronwork.action.DAY_CHANGED"

    const val EXTRA_EDIT_OPEN_SESSION = "edit_open_session"

    fun broadcast(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode,
            Intent(context, TimerActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun openApp(context: Context, editOpenSession: Boolean = false): PendingIntent =
        PendingIntent.getActivity(
            context, if (editOpenSession) 101 else 100,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_EDIT_OPEN_SESSION, editOpenSession),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
