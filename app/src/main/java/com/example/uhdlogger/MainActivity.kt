package com.example.uhdlogger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.uhdlogger.ui.theme.UHDLoggerTheme
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.*
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppSettingsDataStore.init(applicationContext)

        lifecycleScope.launch {
            AppSettingsDataStore.instance.initializeDefaults()
        }

        enableEdgeToEdge()
        setContent {
            UHDLoggerTheme {
                MainNavigation()
            }
        }
    }
}


sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Logs : Screen("logs", "UHD Logger", Icons.Default.Search)
    object Installer : Screen("installer", "Installer", Icons.Default.Build)

    companion object {
        val items = listOf(Settings, Logs, Installer)

        fun fromRoute(route: String?): Screen = when (route) {
            Settings.route -> Settings
            Logs.route -> Logs
            Installer.route -> Installer
            else -> Settings
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route
    val currentScreen = Screen.fromRoute(currentRoute)

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                title = { Text(currentScreen.title) }
            )
        },
        bottomBar = {
            NavigationBar {
                Screen.items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Settings.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Installer.route) { InstallerPage() }
            composable(Screen.Logs.route) { UHDLogPage() }
            composable(Screen.Settings.route) { SettingsPage() }
        }
    }
}



