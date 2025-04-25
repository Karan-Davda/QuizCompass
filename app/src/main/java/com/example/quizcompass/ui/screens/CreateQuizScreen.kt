package com.example.quizcompass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.quizcompass.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun CreateQuizScreen(navController: NavController) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var totalMarks by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("public") }
    var loading by remember { mutableStateOf(false) }
    val visibilities = listOf("public", "invite-only", "private")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Create Quiz") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Quiz Title") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Quiz Description") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = totalMarks,
                onValueChange = {
                    if (it.all { char -> char.isDigit() }) totalMarks = it
                },
                label = { Text("Total Marks") },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Quiz Visibility", style = MaterialTheme.typography.subtitle1)
            visibilities.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = visibility == option,
                        onClick = { visibility = option }
                    )
                    Text(text = option.replaceFirstChar { it.uppercase() })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    loading = true
                    val db = FirebaseFirestore.getInstance()
                    val quizId = UUID.randomUUID().toString()
                    val quizData = hashMapOf(
                        "title" to title,
                        "description" to description,
                        "visibility" to visibility,
                        "creatorId" to FirebaseAuth.getInstance().currentUser?.uid,
                        "createdAt" to System.currentTimeMillis(),
                        "totalMarks" to totalMarks.toIntOrNull()
                    )

                    db.collection("quizzes")
                        .document(quizId)
                        .set(quizData)
                        .addOnSuccessListener {
                            loading = false
                            navController.navigate(Screen.ConfigureQuiz.withId(quizId))
                        }
                        .addOnFailureListener {
                            loading = false
                            // optionally show error
                        }
                },
                enabled = title.isNotBlank() && totalMarks.isNotBlank() && !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Saving..." else "Next: Add Questions")
            }
        }
    }
}