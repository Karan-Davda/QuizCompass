// Updated ConfigureQuizScreen.kt with total marks validation and red warning

package com.example.quizcompass.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.quizcompass.ui.navigation.Screen
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

@Composable
fun ConfigureQuizScreen(navController: NavController, quizId: String) {
    var questionText by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(mutableListOf("", "")) }
    var correctAnswerIndex by remember { mutableIntStateOf(0) }
    var marks by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var addedQuestions by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var totalMarks by remember { mutableStateOf(0) }
    var allocatedMarks by remember { mutableStateOf(0) }

    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val quizSnapshot = db.collection("quizzes").document(quizId).get().await()
        totalMarks = quizSnapshot.getLong("totalMarks")?.toInt() ?: 0
        addedQuestions = loadQuestions(db, quizId)
        allocatedMarks = addedQuestions.sumOf { (it["marks"] as? Long)?.toInt() ?: 0 }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Configure Quiz") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            item {
                OutlinedTextField(
                    value = questionText,
                    onValueChange = { questionText = it },
                    label = { Text("Question") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Options", style = MaterialTheme.typography.subtitle1)

                options.forEachIndexed { index, option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = option,
                            onValueChange = { newValue ->
                                options = options.toMutableList().also { it[index] = newValue }
                            },
                            label = { Text("Option ${index + 1}") },
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = correctAnswerIndex == index,
                            onCheckedChange = {
                                if (it) correctAnswerIndex = index
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (options.size < 4) {
                    TextButton(onClick = {
                        options = options.toMutableList().apply { add("") }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Option")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Option")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = marks,
                    onValueChange = { marks = it },
                    label = { Text("Marks") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Total Marks: $totalMarks | Allocated: $allocatedMarks",
                    style = MaterialTheme.typography.caption,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                val enteredMarks = marks.toIntOrNull() ?: 0
                if (allocatedMarks + enteredMarks > totalMarks) {
                    Text(
                        text = "Warning: Allocated Marks Exceed Total Marks!",
                        color = Color.Red,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                val isAddEnabled = questionText.isNotBlank() &&
                        marks.isNotBlank() &&
                        options.all { it.isNotBlank() } &&
                        allocatedMarks + enteredMarks <= totalMarks

                Button(
                    onClick = {
                        loading = true
                        val questionId = UUID.randomUUID().toString()
                        val question = mapOf(
                            "id" to questionId,
                            "text" to questionText,
                            "options" to options,
                            "correctAnswers" to listOf(correctAnswerIndex),
                            "marks" to (marks.toIntOrNull() ?: 0)
                        )

                        db.collection("quizzes")
                            .document(quizId)
                            .collection("questions")
                            .document(questionId)
                            .set(question)
                            .addOnSuccessListener {
                                questionText = ""
                                options = mutableListOf("", "")
                                correctAnswerIndex = 0
                                marks = ""

                                scope.launch {
                                    addedQuestions = loadQuestions(db, quizId)
                                    allocatedMarks = addedQuestions.sumOf { (it["marks"] as? Long)?.toInt() ?: 0 }
                                    loading = false
                                }
                            }
                            .addOnFailureListener { loading = false }
                    },
                    enabled = isAddEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (loading) "Saving..." else "Add Question")
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("Added Questions", style = MaterialTheme.typography.h6)
            }

            items(addedQuestions) { q ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Q: ${q["text"]}", style = MaterialTheme.typography.subtitle1)
                            Text("Marks: ${q["marks"]}", style = MaterialTheme.typography.body2)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD0F0C0))
                                    .padding(6.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val questionId = q["id"] as? String ?: ""
                                        navController.navigate(Screen.EditQuestion.withId(quizId, questionId))
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Color(0xFF388E3C)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFCDD2))
                                    .padding(6.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        val questionId = q["id"] as? String ?: ""
                                        if (questionId.isNotEmpty()) {
                                            db.collection("quizzes")
                                                .document(quizId)
                                                .collection("questions")
                                                .document(questionId)
                                                .delete()
                                                .addOnSuccessListener {
                                                    scope.launch {
                                                        addedQuestions = loadQuestions(db, quizId)
                                                        allocatedMarks = addedQuestions.sumOf { (it["marks"] as? Long)?.toInt() ?: 0 }
                                                    }
                                                }
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFD32F2F)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Finish Quiz")
                }
            }
        }
    }
}

suspend fun loadQuestions(db: FirebaseFirestore, quizId: String): List<Map<String, Any>> {
    val snapshot = db.collection("quizzes")
        .document(quizId)
        .collection("questions")
        .get()
        .await()

    return snapshot.documents.mapNotNull { it.data }
}