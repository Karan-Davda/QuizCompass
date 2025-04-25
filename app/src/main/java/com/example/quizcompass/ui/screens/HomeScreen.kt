package com.example.quizcompass.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import kotlinx.coroutines.tasks.await
import androidx.navigation.NavController
import com.example.quizcompass.ui.navigation.Screen
import com.google.firebase.firestore.FirebaseFirestore

data class Quiz(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val totalMarks: Int = 0,
    val visibility: String = ""
)

@Composable
fun HomeScreen(navController: NavController) {
    var quizList by remember { mutableStateOf<List<Quiz>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // ✅ Load quiz list only once
    LaunchedEffect(Unit) {
        try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("quizzes")
                .whereEqualTo("visibility", "public")
                .get()
                .await()

            quizList = snapshot.documents.map { doc ->
                Quiz(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    totalMarks = (doc.getLong("totalMarks") ?: 0).toInt(),
                    visibility = doc.getString("visibility") ?: ""
                )
            }
        } catch (e: Exception) {
            quizList = emptyList()
        }
        isLoading = false
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                navController.navigate(Screen.CreateQuiz.route)
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Create Quiz")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Text(
                text = "Published Quizzes",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(16.dp)
            )

            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                quizList.isEmpty() -> Text("No public quizzes found", modifier = Modifier.padding(16.dp))
                else -> LazyColumn {
                    items(quizList) { quiz ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            elevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(quiz.title, style = MaterialTheme.typography.subtitle1)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(quiz.description, style = MaterialTheme.typography.body2)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Total Marks: ${quiz.totalMarks}", style = MaterialTheme.typography.caption)
                                }
                                Button(onClick = {
                                    navController.navigate(Screen.AttemptQuiz.withId(quiz.id))
                                }) {
                                    Text("Attempt")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}