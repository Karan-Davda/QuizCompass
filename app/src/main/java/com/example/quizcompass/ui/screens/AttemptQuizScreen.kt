package com.example.quizcompass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.quizcompass.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

@Composable
fun AttemptQuizScreen(quizId: String, navController: NavController) {
    var questions by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var currentIndex by remember { mutableStateOf(0) }
    var selectedAnswers by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var showResult by remember { mutableStateOf(false) }
    var score by remember { mutableStateOf(0) }
    val startedAt = remember { System.currentTimeMillis() }
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
    var attemptDocId by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val snapshot = db.collection("quizzes")
            .document(quizId)
            .collection("questions")
            .get()
            .await()

        questions = snapshot.documents.mapIndexed { index, doc ->
            val data = doc.data ?: emptyMap()
            data + mapOf("id" to doc.id, "index" to index)
        }

        val newAttempt = hashMapOf(
            "userId" to userId,
            "startedAt" to Date(startedAt),
            "completedAt" to null,
            "answers" to emptyList<Map<String, Any>>(),
            "score" to 0
        )

        val ref = db.collection("quizzes")
            .document(quizId)
            .collection("attempts")
            .add(newAttempt)
            .await()

        attemptDocId = ref.id
    }

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (showResult) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Quiz Completed!", style = MaterialTheme.typography.h5)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Your Score: $score", style = MaterialTheme.typography.h6)
                Spacer(modifier = Modifier.height(24.dp))

                Button(onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }) {
                    Text("Back to Home")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(onClick = {
                    navController.navigate(Screen.ReviewQuiz.withId(quizId, attemptDocId, "home"))
                }) {
                    Text("Review Answers")
                }
            }
        }
        return
    }

    val currentQuestion = questions[currentIndex]
    val questionText = currentQuestion["text"].toString()
    val options = currentQuestion["options"] as? List<*> ?: emptyList<Any>()
    val correctAnswers = (currentQuestion["correctAnswers"] as? List<*>)?.mapNotNull { (it as? Long)?.toInt() } ?: emptyList()
    val marks = (currentQuestion["marks"] as? Long)?.toInt() ?: 1
    val questionId = currentQuestion["id"]?.toString() ?: currentIndex.toString()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Question ${currentIndex + 1} of ${questions.size}", style = MaterialTheme.typography.subtitle2)
        Spacer(modifier = Modifier.height(12.dp))

        Text(questionText, style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(16.dp))

        options.forEachIndexed { index, opt ->
            val selected = selectedAnswers[currentIndex] == index
            val isCorrect = correctAnswers.contains(index)

            OutlinedButton(
                onClick = {
                    selectedAnswers[currentIndex] = index
                    val earnedMarks = if (isCorrect) marks else 0
                    score += earnedMarks

                    val newAnswer = mapOf(
                        "questionId" to questionId,
                        "selected" to listOf(index),
                        "textAnswer" to null,
                        "earnedMarks" to earnedMarks,
                        "correct" to correctAnswers  // ✅ store correct indexes properly
                    )

                    db.collection("quizzes")
                        .document(quizId)
                        .collection("attempts")
                        .document(attemptDocId)
                        .update(
                            "answers", FieldValue.arrayUnion(newAnswer),
                            "score", FieldValue.increment(earnedMarks.toLong())
                        )

                    if (currentIndex < questions.lastIndex) {
                        currentIndex++
                    } else {
                        db.collection("quizzes")
                            .document(quizId)
                            .collection("attempts")
                            .document(attemptDocId)
                            .update("completedAt", Date())
                            .addOnSuccessListener {
                                showResult = true
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = when {
                        selected && isCorrect -> Color(0xFFA5D6A7)
                        selected && !isCorrect -> Color(0xFFEF9A9A)
                        else -> MaterialTheme.colors.surface
                    }
                )
            ) {
                Text(opt.toString())
            }
        }
    }
}