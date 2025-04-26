package com.example.quizcompass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun EditQuestionScreen(navController: NavController, quizId: String, questionId: String) {
    var questionText by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(mutableListOf<String>()) }
    var correctAnswerIndex by remember { mutableIntStateOf(0) }
    var marks by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }

    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        val snapshot = db.collection("quizzes")
            .document(quizId)
            .collection("questions")
            .document(questionId)
            .get()
            .await()

        val data = snapshot.data
        if (data != null) {
            questionText = data["text"] as? String ?: ""
            val loadedOptions = (data["options"] as? List<*>)?.filterIsInstance<String>() ?: listOf()
            options = loadedOptions.toMutableList()
            if (options.size < 2) {
                options = options.toMutableList().apply {
                    while (size < 2) add("")
                }
            }
            correctAnswerIndex = (data["correctAnswers"] as? List<Long>)?.firstOrNull()?.toInt() ?: 0
            marks = (data["marks"] as? Long)?.toString() ?: ""
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Question") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = questionText,
                onValueChange = { questionText = it },
                label = { Text("Question") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Options", style = MaterialTheme.typography.subtitle1)

            options.forEachIndexed { index, option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = correctAnswerIndex == index,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                correctAnswerIndex = index
                            }
                        }
                    )
                    OutlinedTextField(
                        value = option,
                        onValueChange = { newValue ->
                            options = options.toMutableList().also { it[index] = newValue }
                        },
                        label = { Text("Option ${index + 1}") },
                        modifier = Modifier.weight(1f)
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    loading = true
                    val updatedQuestion = mapOf(
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
                        .set(updatedQuestion)
                        .addOnSuccessListener {
                            loading = false
                            navController.popBackStack()
                        }
                        .addOnFailureListener {
                            loading = false
                        }
                },
                enabled = questionText.isNotBlank() && marks.isNotBlank() && options.all { it.isNotBlank() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Saving..." else "Save Changes")
            }
        }
    }
}