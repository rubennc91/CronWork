package com.cronwork

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.cronwork.notify.Actions
import com.cronwork.ui.CronWorkNavHost
import com.cronwork.ui.CronWorkTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** Set when opened from "Paré hace…": the Today screen opens the running session for editing. */
    private var editOpenSession by mutableStateOf(false)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            lifecycleScope.launch { container.timer.refresh() }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // On recreation (e.g. rotation) the launch intent is the same one: don't reopen the dialog.
        if (savedInstanceState == null) readIntent(intent)
        askNotificationPermission()
        val timer = container.timer
        setContent {
            CronWorkTheme {
                Surface {
                    val scope = rememberCoroutineScope()
                    CronWorkNavHost(
                        timer = timer,
                        launch = { block -> scope.launch { block() } },
                        editOpenSessionRequest = editOpenSession,
                        onEditOpenSessionHandled = { editOpenSession = false },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { container.timer.refresh() }
    }

    private fun readIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(Actions.EXTRA_EDIT_OPEN_SESSION, false) == true) {
            intent.removeExtra(Actions.EXTRA_EDIT_OPEN_SESSION)
            editOpenSession = true
            container.notifier.cancelForgot()
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
