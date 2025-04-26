package com.example.quizcompass.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object CreateQuiz : Screen("createQuiz")
    object ConfigureQuiz : Screen("configureQuiz/{quizId}") {
        fun withId(quizId: String) = "configureQuiz/$quizId"
    }
    object AttemptQuiz : Screen("attemptQuiz/{quizId}") {
        fun withId(quizId: String) = "attemptQuiz/$quizId"
    }
    object ReviewQuiz : Screen("reviewQuiz/{quizId}/{attemptId}") {
        fun withId(quizId: String, attemptId: String) = "reviewQuiz/$quizId/$attemptId"
    }
    object EditQuestion : Screen("editQuestion/{quizId}/{questionId}") {
        fun withId(quizId: String, questionId: String) = "editQuestion/$quizId/$questionId"
    }
}