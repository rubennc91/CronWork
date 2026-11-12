package com.cronwork.ui.history

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.cronwork.core.CsvExporter
import com.cronwork.timer.TimerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Writes the whole history as CSV to the cache and opens the share sheet. */
suspend fun shareCsv(context: Context, timer: TimerController) {
    val file = withContext(Dispatchers.IO) {
        val snap = timer.snapshot()
        val csv = CsvExporter.export(snap.sessions, snap.adjustments, snap.specials, snap.settings, timer.now(), timer.zone())
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // keep only the latest export
        File(dir, "cronwork-${timer.today()}.csv").apply { writeText(csv, Charsets.UTF_8) }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/csv")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, "Horas trabajadas (CronWork)")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, "Exportar horas"))
}
