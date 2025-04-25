package com.example.quizcompass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.quizcompass.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    var userQuizzes by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // Fetch quizzes created by user
    LaunchedEffect(Unit) {
        user?.uid?.let { uid ->
            val snapshot = FirebaseFirestore.getInstance()
                .collection("quizzes")
                .whereEqualTo("creatorId", uid)
                .get()
                .await()

            userQuizzes = snapshot.documents.mapNotNull {
                it.data?.plus("id" to it.id) // store quizId for editing/deleting
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        // 👤 Profile Info
        Text("Profile", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Name: ${user?.displayName ?: "N/A"}")
        Text("Email: ${user?.email ?: "N/A"}")

        Spacer(modifier = Modifier.height(24.dp))

        // 📋 User's Quizzes
        Text("Your Quizzes", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(userQuizzes) { quiz ->
                val quizId = quiz["id"] as? String ?: return@items

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    elevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(quiz["title"].toString(), style = MaterialTheme.typography.subtitle1)
                            Text("Visibility: ${quiz["visibility"]}", style = MaterialTheme.typography.body2)
                        }

                        Row {
                            IconButton(onClick = {
                                navController.navigate(Screen.ConfigureQuiz.withId(quizId))
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Quiz")
                            }

                            IconButton(onClick = {
                                deleteQuiz(quizId) {
                                    userQuizzes = userQuizzes.filterNot { it["id"] == quizId }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Quiz")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 🚪 Logout Button
        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true } // 🔥 clear the whole stack
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}

fun deleteQuiz(quizId: String, onComplete: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val quizRef = db.collection("quizzes").document(quizId)

    // Delete all questions in subcollection
    quizRef.collection("questions").get()
        .addOnSuccessListener { snapshot ->
            val batch = db.batch()
            snapshot.documents.forEach {
                batch.delete(it.reference)
            }

            batch.commit().addOnSuccessListener {
                // Delete the quiz itself
                quizRef.delete().addOnSuccessListener {
                    onComplete()
                }
            }
        }
}