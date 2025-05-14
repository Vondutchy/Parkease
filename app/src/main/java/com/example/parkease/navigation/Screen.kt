package com.example.parkease.navigation

sealed class Screen(val route: String) {
    object Greeting : Screen("greeting")
    object SignIn : Screen("signin")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Calendar : Screen("calendar")
    object Profile : Screen("profile")
    object Parking : Screen("parking/{floor}") {
        fun createRoute(floor: String) = "parking/$floor"
    }
    object ParkingWithDate : Screen("parking/{floor}/{date}/{start}/{end}") {
        fun createRoute(floor: String, date: String, start: String, end: String) =
            "parking/$floor/$date/$start/$end"
    }
} 