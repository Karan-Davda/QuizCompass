package com.example.quizcompass.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object CreateQuiz : Screen("create_quiz")
    object AttemptQuiz : Screen("attempt_quiz/{quizId}") {
        fun withId(quizId: String) = "attempt_quiz/$quizId"
    }
    object ConfigureQuiz : Screen("configure_quiz/{quizId}") {
        fun withId(id: String) = "configure_quiz/$id"
    }
    object ReviewQuiz : Screen("review_quiz/{quizId}") {
        fun withId(quizId: String) = "review_quiz/$quizId"
    }
}