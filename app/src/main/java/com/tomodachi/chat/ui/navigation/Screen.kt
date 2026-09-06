package com.tomodachi.chat.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Chat : Screen("chat")
    data object Profile : Screen("profile")
    data object Settings : Screen("settings")
    data object Admin : Screen("admin")
}
