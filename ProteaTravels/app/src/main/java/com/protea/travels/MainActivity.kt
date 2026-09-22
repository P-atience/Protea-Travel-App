package com.protea.travels

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.protea.travels.data.Repo
import com.protea.travels.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: AppVM = viewModel()
            ProteaTheme(vm.settings.darkMode) { ProteaApp(vm) }
        }
    }
}

@Composable
fun ProteaApp(vm: AppVM) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    LaunchedEffect(vm.message) {
        vm.message?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show(); vm.message = null }
    }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val tabs = listOf(Triple("home", "Home", Icons.Default.Home),
        Triple("bookings", "Trips", Icons.Default.ConfirmationNumber),
        Triple("settings", "Settings", Icons.Default.Settings))
    fun clearTo(dest: String) = nav.navigate(dest) { popUpTo(nav.graph.id) { inclusive = true } }

    Scaffold(bottomBar = {
        if (route in tabs.map { it.first }) NavigationBar {
            tabs.forEach { (r, label, icon) ->
                NavigationBarItem(selected = route == r, onClick = {
                    nav.navigate(r) { popUpTo("home"); launchSingleTop = true }
                }, icon = { Icon(icon, label) }, label = { Text(label) })
            }
        }
    }) { pad ->
        NavHost(nav, "splash", Modifier.padding(pad)) {
            composable("splash") { SplashScreen(Repo.isLoggedIn) { li -> clearTo(if (li) "home" else "welcome") } }
            composable("welcome") { WelcomeScreen({ nav.navigate("register") }, { nav.navigate("login") }) }
            composable("login") { LoginScreen(vm, { nav.popBackStack() }, { nav.navigate("register") }) { clearTo("home") } }
            composable("register") { RegisterScreen(vm, { nav.popBackStack() }) { clearTo("home") } }
            composable("home") { HomeScreen(vm) { nav.navigate("search") } }
            composable("search") { SearchScreen(vm, { nav.popBackStack() }) { nav.navigate("results") } }
            composable("results") { ResultsScreen(vm, { nav.popBackStack() }) { nav.navigate("bookings") { popUpTo("home") } } }
            composable("bookings") { BookingsScreen(vm) }
            composable("settings") { SettingsScreen(vm) { clearTo("welcome") } }
        }
    }
}
// By Lesego van Heerden and Onalenna Lonake//