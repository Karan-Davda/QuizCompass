package com.example.quizcompass.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthResult
import com.example.quizcompass.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.UserProfileChangeRequest


sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val result: AuthResult) : AuthState()
    data class Error(val error: String) : AuthState()
}

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.login(email, password)
            _authState.value = result.fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { AuthState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    fun signup(email: String, password: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, password)
                    .await()

                val displayName = "$firstName $lastName"
                result.user?.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .build()
                )?.await()

                _authState.value = AuthState.Success(result)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Something went wrong")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun logout() = repository.logout()
    fun currentUserId() = repository.currentUserId()
}