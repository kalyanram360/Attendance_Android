package com.example.attendance_android
sealed class NavRoutes(val route: String) {
    object Splash : NavRoutes("splash")
    object Onboarding : NavRoutes("onboarding")
    object Home : NavRoutes("home")

}
