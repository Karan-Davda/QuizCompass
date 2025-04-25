package com.example.quizcompass.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.quizcompass.ui.navigation.Screen
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

@Composable
fun ConfigureQuizScreen(navController: NavController, quizId: String) {
    var questionText by remember { mutableStateOf("") }
    var questionType by remember { mutableStateOf("Single Choice") }
    var options by remember { mutableStateOf(listOf("", "")) }
    var correctAnswers by remember { mutableStateOf(setOf<Int>()) }
    var marks by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var addedQuestions by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val maxOptions = 4
    val questionTypes = listOf("Single Choice", "Multiple Choice", "Q/A")
    var totalAllowedMarks by remember { mutableStateOf(0) }
    var totalAllocatedMarks by remember { mutableStateOf(0) }

    // Load total marks from quiz document and previously added questions
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val quizSnapshot = db.collection("quizzes").document(quizId).get().await()
        totalAllowedMarks = (quizSnapshot["totalMarks"] as? Long)?.toInt() ?: 0

        val questionSnapshot = db.collection("quizzes")
            .document(quizId)
            .collection("questions")
            .get()
            .await()

        addedQuestions = questionSnapshot.documents.mapNotNull { it.data }
        totalAllocatedMarks = addedQuestions.sumOf { (it["marks"] as? Long)?.toInt() ?: 0 }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Configure Quiz") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = questionText,
                onValueChange = { questionText = it },
                label = { Text("Question") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Question Type", style = MaterialTheme.typography.subtitle1)
            questionTypes.forEach { type ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = questionType == type,
                        onClick = {
                            questionType = type
                            options = listOf("", "")
                            correctAnswers = emptySet()
                        }
                    )
                    Text(text = type)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (questionType != "Q/A") {
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Checkbox(
                            checked = correctAnswers.contains(index),
                            onCheckedChange = {
                                correctAnswers = if (it) {
                                    if (questionType == "Single Choice") setOf(index)
                                    else correctAnswers + index
                                } else {
                                    correctAnswers - index
                                }
                            }
                        )
                        Text("Correct")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (options.size < maxOptions) {
                    TextButton(onClick = {
                        options = options + ""
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Option")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Option")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = marks,
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) marks = it
                },
                label = { Text("Marks") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Total Allowed Marks: $totalAllowedMarks | Allocated: $totalAllocatedMarks", style = MaterialTheme.typography.body2)

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    loading = true
                    val db = FirebaseFirestore.getInstance()
                    val questionId = UUID.randomUUID().toString()
                    val markInt = marks.toIntOrNull() ?: 0

                    val questionData = hashMapOf(
                        "text" to questionText,
                        "type" to questionType,
                        "marks" to markInt,
                        "options" to if (questionType == "Q/A") emptyList<String>() else options,
                        "correctAnswers" to if (questionType == "Q/A") emptyList<Int>() else correctAnswers.toList()
                    )

                    db.collection("quizzes")
                        .document(quizId)
                        .collection("questions")
                        .document(questionId)
                        .set(questionData)
                        .addOnSuccessListener {
                            questionText = ""
                            marks = ""
                            options = listOf("", "")
                            correctAnswers = emptySet()

                            db.collection("quizzes")
                                .document(quizId)
                                .collection("questions")
                                .get()
                                .addOnSuccessListener { newSnapshot ->
                                    addedQuestions = newSnapshot.documents.mapNotNull { it.data }
                                    totalAllocatedMarks = addedQuestions.sumOf { (it["marks"] as? Long)?.toInt() ?: 0 }
                                    loading = false
                                }
                        }
                        .addOnFailureListener {
                            loading = false
                        }
                },
                enabled = questionText.isNotBlank() && marks.isNotBlank() &&
                        marks.toIntOrNull()?.let { totalAllocatedMarks + it <= totalAllowedMarks } == true && !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Saving..." else "Add Question")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Added Questions", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))

            addedQuestions.forEachIndexed { index, q ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Q${index + 1}: ${q["text"]}", style = MaterialTheme.typography.subtitle1)
                        Text("Type: ${q["type"]} | Marks: ${q["marks"]}", style = MaterialTheme.typography.body2)
                        val optList = q["options"] as? List<*> ?: emptyList<Any>()
                        val correctList = q["correctAnswers"] as? List<*> ?: emptyList<Any>()
                        optList.forEachIndexed { i, opt ->
                            Text("• ${opt} ${if (i in correctList) "✅" else ""}")
                        }
                    }
                }
            }

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