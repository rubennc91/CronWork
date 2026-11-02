package com.cronwork.timer

import com.cronwork.core.EditError
import com.cronwork.core.Session
import com.cronwork.core.SessionRules
import com.cronwork.core.Settings
import com.cronwork.core.TimerState
import com.cronwork.data.SettingsRepository
import com.cronwork.data.WorkRepository
import com.cronwork.core.Adjustment
import com.cronwork.core.SpecialDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Something that must reflect the timer state outside the app: notification, alarms, widget. */
interface TimerSideEffects {
    suspend fun sync(state: TimerState, settings: Settings)
}

/** All stored data needed to compute a [TimerState]. */
data class WorkSnapshot(
    val sessions: List<Session>,
    val adjustments: List<Adjustment>,
    val specials: List<SpecialDay>,
    val settings: Settings,
)

/**
 * Single entry point for start/stop/adjust/edit. Every change ends with [refresh],
 * so notification, alarms and widget never drift from the stored data.
 */
class TimerController(
    private val repo: WorkRepository,
    private val settingsRepo: SettingsRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    private val effects = mutableListOf<TimerSideEffects>()
    private val mutex = Mutex()

    fun addSideEffects(e: TimerSideEffects) { effects += e }

    fun now(): Instant = clock.instant()
    fun zone(): ZoneId = zone.invoke()
    fun today(): LocalDate = now().atZone(zone()).toLocalDate()

    val snapshots: Flow<WorkSnapshot> =
        combine(repo.sessions, repo.adjustments, repo.specials, settingsRepo.settings) { s, a, sp, st ->
            WorkSnapshot(s, a, sp, st)
        }

    /** Live state, recomputed every second while collected. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: Flow<TimerState> = snapshots.flatMapLatest { snap ->
        flow {
            while (true) {
                emit(compute(snap))
                delay(1000)
            }
        }
    }

    fun compute(snap: WorkSnapshot, at: Instant = now()): TimerState =
        TimerState.compute(snap.sessions, snap.adjustments, snap.specials, snap.settings, at, zone())

    suspend fun snapshot(): WorkSnapshot =
        WorkSnapshot(repo.allSessions(), repo.allAdjustments(), repo.allSpecials(), settingsRepo.current())

    suspend fun toggle() {
        mutex.withLock {
            if (repo.openSession() == null) repo.startSession(now()) else repo.stopOpenSession(now())
        }
        refresh()
    }

    suspend fun start() {
        mutex.withLock { repo.startSession(now()) }
        refresh()
    }

    /** Stops the running session at [at] (default: now). Does nothing if none is running. */
    suspend fun stop(at: Instant? = null) {
        mutex.withLock { repo.stopOpenSession(minOf(at ?: now(), now())) }
        refresh()
    }

    suspend fun adjust(date: LocalDate, minutes: Int, note: String?) {
        if (minutes == 0) return
        repo.addAdjustment(date, minutes, note?.takeIf { it.isNotBlank() }, now())
        refresh()
    }

    suspend fun deleteAdjustment(id: Long) {
        repo.deleteAdjustment(id)
        refresh()
    }

    /** Saves a new (id = 0) or edited session after validating it. */
    suspend fun editSession(s: Session): EditError? {
        val error = mutex.withLock {
            SessionRules.validate(s, repo.allSessions(), now())?.let { return@withLock it }
            repo.upsertSession(s)
            null
        }
        if (error == null) refresh()
        return error
    }

    suspend fun deleteSession(id: Long) {
        repo.deleteSession(id)
        refresh()
    }

    suspend fun upsertSpecialDay(d: SpecialDay) {
        repo.upsertSpecialDay(d)
        refresh()
    }

    suspend fun deleteSpecialDay(date: LocalDate) {
        repo.deleteSpecialDay(date)
        refresh()
    }

    suspend fun updateSettings(transform: (Settings) -> Settings) {
        settingsRepo.update(transform)
        refresh()
    }

    /** Recomputes the state and pushes it to notification, alarms and widget. */
    suspend fun refresh() {
        val snap = snapshot()
        val st = compute(snap)
        effects.forEach { it.sync(st, snap.settings) }
    }
}
