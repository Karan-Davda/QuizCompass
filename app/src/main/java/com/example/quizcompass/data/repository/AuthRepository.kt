package com.example.quizcompass.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun login(email: String, password: String): Result<AuthResult> = try {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun signup(
        email: String,
        password: String,
        firstName: String,
        lastName: String
    ): Result<AuthResult> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val userId = result.user?.uid ?: throw Exception("User ID is null")

        val userDoc = mapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "displayName" to "$firstName $lastName",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "stats" to mapOf(
                "averageScore" to 0,
                "quizzesCreated" to 0,
                "quizzesAttempted" to 0,
                "totalAchievedScore" to 0,
                "totalScore" to 0
            )
        )

        db.collection("users").document(userId).set(userDoc).await()
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun logout() {
        auth.signOut()
    }

    fun currentUserId(): String? = auth.currentUser?.uid
}