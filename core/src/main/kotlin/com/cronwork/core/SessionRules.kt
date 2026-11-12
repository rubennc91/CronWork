package com.cronwork.core

import java.time.Instant

sealed interface EditError {
    data object EndBeforeStart : EditError
    data object Overlaps : EditError
    data object InFuture : EditError
}

object SessionRules {

    /** Checks an edited or new session against the others. An open session counts up to [now]. */
    fun validate(edited: Session, others: List<Session>, now: Instant): EditError? {
        val end = edited.end
        if (end != null && end.isBefore(edited.start)) return EditError.EndBeforeStart
        if (edited.start.isAfter(now) || (end != null && end.isAfter(now))) return EditError.InFuture
        val editedEnd = end ?: now
        val overlaps = others.any { other ->
            other.id != edited.id &&
                edited.start.isBefore(other.end ?: now) && other.start.isBefore(editedEnd)
        }
        return if (overlaps) EditError.Overlaps else null
    }

    fun canStart(sessions: List<Session>): Boolean = sessions.none { it.end == null }
}
