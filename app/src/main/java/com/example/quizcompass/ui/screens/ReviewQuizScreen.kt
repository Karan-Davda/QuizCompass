package com.example.quizcompass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ReviewQuizScreen(quizId: String, navController: NavController) {
    var questions by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedAnswers by remember { mutableStateOf<Map<String, List<Int>>>(emptyMap()) }
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
            .orderBy("completedAt")
            .get()
            .await()

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val userAttempt = attemptSnapshot.documents.firstOrNull { it.getString("userId") == currentUserId }

        val attemptData = userAttempt?.get("answers") as? List<Map<String, Any>>
        selectedAnswers = attemptData?.associate {
            val questionId = it["questionId"] as? String ?: ""
            val selected = it["selected"] as? List<*> ?: emptyList<Any>()
            questionId to selected.mapNotNull { sel -> sel as? Int }
        } ?: emptyMap()

        // Calculate score
        score = questions.sumOf { q ->
            val qId = q["id"] as? String ?: return@sumOf 0
            val selected = selectedAnswers[qId].orEmpty()
            val correct = (q["correctAnswers"] as? List<*>)?.mapNotNull { it as? Int } ?: emptyList()
            val marks = (q["marks"] as? Long)?.toInt() ?: 0
            if (selected.sorted() == correct.sorted()) marks else 0
        }

        isLoading = false
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Review Answers") }) }) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {
                Text("Score: $score", style = MaterialTheme.typography.h6, modifier = Modifier.padding(bottom = 16.dp))

                questions.forEachIndexed { index, question ->
                    val questionText = question["text"].toString()
                    val options = question["options"] as? List<*> ?: emptyList<Any>()
                    val correctAnswers = question["correctAnswers"] as? List<*> ?: emptyList<Int>()
                    val selected = selectedAnswers[question["id"] as String].orEmpty()

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        elevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Q${index + 1}: $questionText", style = MaterialTheme.typography.subtitle1)
                            Spacer(modifier = Modifier.height(8.dp))

                            options.forEachIndexed { optIndex, opt ->
                                val isCorrect = optIndex in correctAnswers.mapNotNull { it as? Int }
                                val isSelected = optIndex in selected

                                val color = when {
                                    isCorrect && isSelected -> Color.Green
                                    isCorrect -> Color.Blue
                                    isSelected -> Color.Red
                                    else -> MaterialTheme.colors.onSurface
                                }

                                Text(
                                    text = "• ${opt.toString()}",
                                    color = color,
                                    style = MaterialTheme.typography.body2,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}