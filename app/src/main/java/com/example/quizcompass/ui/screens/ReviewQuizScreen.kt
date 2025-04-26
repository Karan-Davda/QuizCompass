package com.example.quizcompass.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.quizcompass.ui.navigation.Screen
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ReviewQuizScreen(
    quizId: String,
    attemptId: String,
    source: String?,  // ✅ New: who called this screen (home OR records)
    navController: NavController
) {
    var questions by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var answers by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var score by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()

        val questionsSnapshot = db.collection("quizzes")
            .document(quizId)
            .collection("questions")
            .get()
            .await()

        questions = questionsSnapshot.documents.mapNotNull { it.data?.toMutableMap()?.apply { put("id", it.id) } }

        val attemptSnapshot = db.collection("quizzes")
            .document(quizId)
            .collection("attempts")
            .document(attemptId)
            .get()
            .await()

        val fetchedAnswers = attemptSnapshot.get("answers") as? List<Map<String, Any>> ?: emptyList()
        answers = fetchedAnswers

        score = (attemptSnapshot.getLong("score") ?: 0L).toInt()

        isLoading = false
    }

    // ✅ Back behavior depending on where user came from
    BackHandler {
        if (source == "records") {
            navController.navigate(Screen.AttemptRecords.route) {
                popUpTo(Screen.Home.route) { inclusive = false }
            }
        } else {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = true }
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Review Answers") }) }) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Text(
                    "Score: $score",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                questions.forEachIndexed { index, question ->
                    val questionText = question["text"].toString()
                    val options = question["options"] as? List<*> ?: emptyList<Any>()
                    val correctIndexes = (question["correctAnswers"] as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()
                    val questionId = question["id"] as? String ?: ""
                    val selectedAnswer = answers.find { it["questionId"] == questionId }
                    val selectedIndexes = selectedAnswer?.get("selected") as? List<Int> ?: emptyList()
                    val earnedMarks = (selectedAnswer?.get("earnedMarks") as? Long)?.toInt() ?: 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Q${index + 1}: $questionText", style = MaterialTheme.typography.subtitle1)
                            Spacer(modifier = Modifier.height(8.dp))

                            options.forEachIndexed { optIndex, opt ->
                                val isCorrectOption = correctIndexes.contains(optIndex)
                                val isSelectedOption = selectedIndexes.contains(optIndex)

                                val color = when {
                                    isSelectedOption && isCorrectOption -> Color(0xFF388E3C) // ✅ Green
                                    isSelectedOption && !isCorrectOption -> Color(0xFFD32F2F) // ❌ Red
                                    !isSelectedOption && isCorrectOption -> Color(0xFF1976D2) // 🔵 Blue
                                    else -> MaterialTheme.colors.onSurface
                                }

                                Text(
                                    text = "• ${opt.toString()}",
                                    color = color,
                                    style = MaterialTheme.typography.body2,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Earned Marks: $earnedMarks",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.primary
                            )
                        }
                    }
                }
            }
        }
    }
}