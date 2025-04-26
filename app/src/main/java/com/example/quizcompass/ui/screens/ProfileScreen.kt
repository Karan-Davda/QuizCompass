package com.example.quizcompass.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.quizcompass.R
import com.example.quizcompass.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(navController: NavController) {
    val user = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    var userQuizzes by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var totalAttempts by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (user?.uid != null) {
            val uid = user.uid
            try {
                val quizSnapshot = db.collection("quizzes")
                    .whereEqualTo("creatorId", uid)
                    .get()
                    .await()

                userQuizzes = quizSnapshot.documents.mapNotNull { doc ->
                    doc.data?.plus("id" to doc.id)
                }

                val attemptSnapshot = db.collection("attempts")
                    .whereEqualTo("creatorId", uid)
                    .get()
                    .await()

                totalAttempts = attemptSnapshot.size()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (user == null) {
        // If no user is logged-in
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Please login to view your profile.", style = MaterialTheme.typography.titleMedium)
        }
    } else {
        // Logged-in User UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val firstName = when {
                    !user?.displayName.isNullOrBlank() -> {
                        user.displayName!!.split(" ").firstOrNull() ?: "N/A"
                    }
                    else -> {
                        user?.email?.substringBefore("@")?.replaceFirstChar { it.uppercaseChar() } ?: "User"
                    }
                }

                Image(
                    painter = painterResource(id = R.drawable.default_user),
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(firstName, style = MaterialTheme.typography.titleMedium)
                Text(user?.email ?: "N/A", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Quiz", style = MaterialTheme.typography.bodyMedium)
                        Text("${userQuizzes.size}", style = MaterialTheme.typography.headlineSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Attempts", style = MaterialTheme.typography.bodyMedium)
                        Text("$totalAttempts", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Your Quizzes", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(userQuizzes) { quiz ->
                    val quizId = quiz["id"] as? String ?: return@items

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth()
                        ) {
                            Text(quiz["title"].toString(), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Visibility: ${quiz["visibility"]}", style = MaterialTheme.typography.bodyMedium)

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
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

            Button(
                onClick = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
        }
    }
}

fun deleteQuiz(quizId: String, onComplete: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val quizRef = db.collection("quizzes").document(quizId)

    quizRef.collection("questions").get()
        .addOnSuccessListener { snapshot ->
            val batch = db.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            batch.commit().addOnSuccessListener {
                quizRef.delete().addOnSuccessListener {
                    onComplete()
                }
            }
        }
}