package com.example.quizcompass.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object AttemptRecords : Screen("attemptsRecord")

    object CreateQuiz : Screen("createQuiz") {
        fun withIdAndEdit(quizId: String, isEdit: Boolean): String {
            return "createQuiz?quizId=$quizId&edit=$isEdit"
        }
    }

    object ConfigureQuiz : Screen("configureQuiz/{quizId}") {
        fun withId(quizId: String) = "configureQuiz/$quizId"
    }

    object AttemptQuiz : Screen("attemptQuiz/{quizId}") {
        fun withId(quizId: String) = "attemptQuiz/$quizId"
    }

    object ReviewQuiz : Screen("reviewQuiz/{quizId}/{attemptId}/{source}") {
        fun withId(quizId: String, attemptId: String, source: String) =
            "reviewQuiz/$quizId/$attemptId/$source"
    }

    object EditQuestion : Screen("editQuestion/{quizId}/{questionId}") {
        fun withId(quizId: String, questionId: String) = "editQuestion/$quizId/$questionId"
    }
}
