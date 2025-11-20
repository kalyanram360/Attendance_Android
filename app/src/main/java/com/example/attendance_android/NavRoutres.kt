package com.example.attendance_android
sealed class NavRoutes(val route: String) {
    object Splash : NavRoutes("splash")
    object Onboarding : NavRoutes("onboarding")
    object Home : NavRoutes("home")

    object TeacherHome : NavRoutes("teacher_home")

    object TeacherBLE : NavRoutes("teacher_ble")

}
