package com.example.quizcompass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.quizcompass.ui.components.BottomNavBar
import com.example.quizcompass.ui.navigation.Screen
import com.example.quizcompass.ui.screens.AttemptQuizScreen
import com.example.quizcompass.ui.screens.ConfigureQuizScreen
import com.example.quizcompass.ui.screens.CreateQuizScreen
import com.example.quizcompass.ui.screens.HomeScreen
import com.example.quizcompass.ui.screens.ProfileScreen
import com.example.quizcompass.ui.screens.auth.LoginScreen
import com.example.quizcompass.ui.screens.auth.SignupScreen
import com.example.quizcompass.ui.screens.ReviewQuizScreen
import com.example.quizcompass.ui.theme.QuizCompassTheme
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuizCompassApp()
        }
    }
}

@Composable
fun QuizCompassApp() {
    val navController = rememberNavController()
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    val showBottomBar = remember { mutableStateOf(false) }

    // ✅ Listen to Auth state changes
    DisposableEffect(Unit) {
        val authListener = FirebaseAuth.AuthStateListener {
            currentUser = it.currentUser

            // 👇 Automatically navigate on logout
            if (currentUser == null) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
        onDispose {
            FirebaseAuth.getInstance().removeAuthStateListener(authListener)
        }
    }

    // ✅ Update bottom bar visibility when destination changes
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            showBottomBar.value =
                destination.route == Screen.Home.route || destination.route == Screen.Profile.route
        }
    }

    val startDestination = if (currentUser != null) Screen.Home.route else Screen.Login.route

    Scaffold(
        bottomBar = {
            if (showBottomBar.value) {
                BottomNavBar(navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Login.route) {
                LoginScreen(navController)
            }
            composable(Screen.SignUp.route) {
                SignupScreen(navController)
            }
            composable(Screen.Home.route) {
                if (currentUser != null) {
                    HomeScreen(navController)
                }
            }
            composable(Screen.Profile.route) {
                if (currentUser != null) {
                    ProfileScreen(navController)
                }
            }
            composable(Screen.CreateQuiz.route) {
                CreateQuizScreen(navController)
            }
            composable(
                route = Screen.ConfigureQuiz.route,
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) {
                val quizId = it.arguments?.getString("quizId") ?: ""
                ConfigureQuizScreen(navController, quizId)
            }
            composable(
                route = Screen.AttemptQuiz.route,
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) {
                val quizId = it.arguments?.getString("quizId") ?: ""
                AttemptQuizScreen(quizId = quizId, navController = navController) // ✅ FIXED
            }
            composable(
                route = Screen.ReviewQuiz.route,
                arguments = listOf(navArgument("quizId") { type = NavType.StringType })
            ) {
                val quizId = it.arguments?.getString("quizId") ?: ""
                ReviewQuizScreen(quizId = quizId, navController = navController)
            }

        }
    }
}