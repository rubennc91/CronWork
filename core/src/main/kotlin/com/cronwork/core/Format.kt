package com.cronwork.core

import kotlin.math.abs

/** Countdown "H:MM:SS"; once past zero shows the extra time as "+H:MM:SS". */
fun formatRemaining(remainingSeconds: Long): String {
    val prefix = if (remainingSeconds < 0) "+" else ""
    val s = abs(remainingSeconds)
    return "%s%d:%02d:%02d".format(prefix, s / 3600, (s % 3600) / 60, s % 60)
}

/** Duration "H:MM" (seconds are dropped). */
fun formatHm(seconds: Long): String {
    val s = abs(seconds)
    return "%d:%02d".format(s / 3600, (s % 3600) / 60)
}

/** Balance "+H:MM" / "−H:MM" / "0:00". */
fun formatSigned(seconds: Long): String {
    val body = formatHm(seconds)
    return when {
        seconds / 60 == 0L -> "0:00"
        seconds > 0 -> "+$body"
        else -> "−$body"
    }
}
