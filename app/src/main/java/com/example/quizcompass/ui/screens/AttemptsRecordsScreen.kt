package com.example.quizcompass.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import com.example.quizcompass.ui.navigation.Screen

data class QuizWithAttempts(
    val title: String,
    val quizId: String,
    val attempts: List<AttemptRecord>
)

data class AttemptRecord(
    val attemptId: String,
    val score: Int,
    val totalMarks: Int,
    val dateTime: Date?
)

@Composable
fun AttemptsRecordsScreen(navController: NavHostController) {
    var quizzes by remember { mutableStateOf<List<QuizWithAttempts>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val db = FirebaseFirestore.getInstance()
            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                val quizSnapshot = db.collection("quizzes")
                    .whereEqualTo("creatorId", userId)
                    .get()
                    .await()

                quizzes = quizSnapshot.documents.mapNotNull { doc ->
                    val quizId = doc.id
                    val quizTitle = doc.getString("title") ?: "Untitled Quiz"

                    val attemptsSnapshot = db.collection("quizzes")
                        .document(quizId)
                        .collection("attempts")
                        .whereEqualTo("userId", userId)
                        .get()
                        .await()

                    val attemptsList = attemptsSnapshot.documents.mapNotNull { attemptDoc ->
                        val score = (attemptDoc.getLong("score") ?: 0L).toInt()
                        val totalMarks = doc.getLong("totalMarks")?.toInt() ?: 0
                        val timestamp = attemptDoc.getTimestamp("startedAt")?.toDate()

                        AttemptRecord(
                            attemptId = attemptDoc.id,
                            score = score,
                            totalMarks = totalMarks,
                            dateTime = timestamp
                        )
                    }.sortedByDescending { it.dateTime }

                    QuizWithAttempts(
                        title = quizTitle,
                        quizId = quizId,
                        attempts = attemptsList
                    )
                }
            }

            isLoading = false
        } catch (e: Exception) {
            e.printStackTrace()
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Your Attempts") })
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                items(quizzes) { quiz ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        elevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(quiz.title, style = MaterialTheme.typography.h6)

                            Spacer(modifier = Modifier.height(8.dp))

                            if (quiz.attempts.isEmpty()) {
                                Text("No attempts yet.", style = MaterialTheme.typography.body2)
                            } else {
                                quiz.attempts.forEachIndexed { index, attempt ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                navController.navigate(
                                                    Screen.ReviewQuiz.withId(
                                                        quizId = quiz.quizId,
                                                        attemptId = attempt.attemptId,
                                                        source = "records"
                                                    )
                                                )
                                            },
                                        elevation = 2.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {
                                            Text(
                                                text = "Attempt ${quiz.attempts.size - index}",
                                                style = MaterialTheme.typography.subtitle2
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Score: ${attempt.score} / ${attempt.totalMarks}",
                                                    style = MaterialTheme.typography.body2
                                                )
                                                Text(
                                                    text = attempt.dateTime?.toFormattedString() ?: "Unknown",
                                                    style = MaterialTheme.typography.body2
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper to format Date nicely
fun Date.toFormattedString(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
    return sdf.format(this)
}