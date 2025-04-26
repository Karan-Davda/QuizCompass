package com.example.quizcompass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.quizcompass.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

@Composable
fun CreateQuizScreen(
    navController: NavController,
    quizId: String? = null,
    isEditMode: Boolean = false
) {
    val db = FirebaseFirestore.getInstance()
    val user = FirebaseAuth.getInstance().currentUser

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var totalMarks by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("public") }
    var loading by remember { mutableStateOf(false) }
    val visibilities = listOf("public", "invite-only", "private")

    // Load existing quiz details if edit mode
    LaunchedEffect(key1 = isEditMode, key2 = quizId) {
        if (isEditMode && quizId != null) {
            val snapshot = db.collection("quizzes").document(quizId).get().await()
            title = snapshot.getString("title") ?: ""
            description = snapshot.getString("description") ?: ""
            totalMarks = snapshot.getLong("totalMarks")?.toString() ?: ""
            visibility = snapshot.getString("visibility") ?: "public"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (isEditMode) "Edit Quiz" else "Create Quiz") })
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
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) totalMarks = newValue
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
                    Text(option.replaceFirstChar { it.uppercase() })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    loading = true
                    val data = hashMapOf(
                        "title" to title,
                        "description" to description,
                        "visibility" to visibility,
                        "totalMarks" to totalMarks.toIntOrNull()
                    )

                    if (isEditMode && quizId != null) {
                        // Update existing quiz
                        db.collection("quizzes")
                            .document(quizId)
                            .update(data as Map<String, Any>)
                            .addOnSuccessListener {
                                loading = false
                                navController.navigate(Screen.ConfigureQuiz.withId(quizId))
                            }
                            .addOnFailureListener {
                                loading = false
                            }
                    } else {
                        // Create new quiz
                        val newQuizId = UUID.randomUUID().toString()
                        val fullData = data + mapOf(
                            "creatorId" to user?.uid,
                            "createdAt" to System.currentTimeMillis()
                        )

                        db.collection("quizzes")
                            .document(newQuizId)
                            .set(fullData)
                            .addOnSuccessListener {
                                loading = false
                                navController.navigate(Screen.ConfigureQuiz.withId(newQuizId))
                            }
                            .addOnFailureListener {
                                loading = false
                            }
                    }
                },
                enabled = title.isNotBlank() && totalMarks.isNotBlank() && !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (loading) "Saving..." else if (isEditMode) "Update Quiz" else "Next: Add Questions")
            }
        }
    }
}
