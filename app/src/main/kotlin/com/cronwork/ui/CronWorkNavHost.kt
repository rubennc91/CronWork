package com.cronwork.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cronwork.timer.TimerController
import com.cronwork.ui.history.DayDetailScreen
import com.cronwork.ui.history.HistoryScreen
import com.cronwork.ui.settings.SettingsScreen
import com.cronwork.ui.settings.SpecialDaysScreen
import com.cronwork.ui.today.TodayScreen
import java.time.LocalDate

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("today", "Hoy", Icons.Filled.Home),
    Tab("history", "Histórico", Icons.Filled.DateRange),
    Tab("settings", "Ajustes", Icons.Filled.Settings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CronWorkNavHost(
    timer: TimerController,
    launch: (suspend () -> Unit) -> Unit,
    editOpenSessionRequest: Boolean,
    onEditOpenSessionHandled: () -> Unit,
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val isTab = tabs.any { it.route == route } || route == null

    Scaffold(
        topBar = {
            if (!isTab) {
                TopAppBar(
                    title = { Text(if (route == "special") "Festivos y vacaciones" else "Día") },
                    navigationIcon = {
                        IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Atrás") }
                    },
                )
            }
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = "today", modifier = Modifier.padding(padding)) {
            composable("today") { TodayScreen(timer, launch, editOpenSessionRequest, onEditOpenSessionHandled) }
            composable("history") { HistoryScreen(timer, launch) { date -> nav.navigate("day/${date.toEpochDay()}") } }
            composable("day/{epochDay}") { e ->
                val day = e.arguments?.getString("epochDay")?.toLongOrNull() ?: return@composable
                DayDetailScreen(LocalDate.ofEpochDay(day), timer, launch)
            }
            composable("settings") { SettingsScreen(timer, launch) { nav.navigate("special") } }
            composable("special") { SpecialDaysScreen(timer, launch) }
        }
    }
}
