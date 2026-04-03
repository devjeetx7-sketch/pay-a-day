package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.dailywork.attedance.data.FirestoreRepository

class AuthViewModel(
    val repository: UserPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    val userRoleFlow = repository.userRoleFlow
    val authTokenFlow = repository.authTokenFlow

    init {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                repository.saveAuthToken(currentUser.uid)
                _loginState.value = LoginState.Success(currentUser.uid)
            }
        }
    }

    fun loginWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                if (email.isNotEmpty() && pass.isNotEmpty()) {
                    val result = auth.signInWithEmailAndPassword(email, pass).await()
                    val uid = result.user?.uid ?: ""
                    repository.saveAuthToken(uid)
                    _loginState.value = LoginState.Success(uid)
                } else {
                    _loginState.value = LoginState.Error("Email and password cannot be empty")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun registerWithEmail(email: String, pass: String, name: String) {
         viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                if (email.isNotEmpty() && pass.isNotEmpty()) {
                    val result = auth.createUserWithEmailAndPassword(email, pass).await()
                    val uid = result.user?.uid ?: ""

                    if (uid.isNotEmpty()) {
                        val userData = mapOf(
                            "uid" to uid,
                            "name" to name,
                            "email" to email,
                            "createdAt" to System.currentTimeMillis()
                        )
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .set(userData)
                            .await()
                    }

                    repository.saveAuthToken(uid)
                    _loginState.value = LoginState.Success(uid)
                } else {
                    _loginState.value = LoginState.Error("Email and password cannot be empty")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun loginWithGoogleCredential(idToken: String) {
        viewModelScope.launch {
             _loginState.value = LoginState.Loading
             try {
                 val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                 val result = auth.signInWithCredential(credential).await()
                 val user = result.user
                 if (user != null) {
                     val uid = user.uid

                     // Upsert user document just like email registration to prevent crash on new Google user
                     val userData = mapOf(
                        "uid" to uid,
                        "name" to (user.displayName ?: ""),
                        "email" to (user.email ?: ""),
                        "createdAt" to System.currentTimeMillis()
                     )
                     com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .set(userData, com.google.firebase.firestore.SetOptions.merge())
                        .await()

                     repository.saveAuthToken(uid)
                     _loginState.value = LoginState.Success(uid)
                 } else {
                     _loginState.value = LoginState.Error("Google Sign-In failed: Null user")
                 }
             } catch (e: Exception) {
                 _loginState.value = LoginState.Error(e.message ?: "Google Sign-In failed")
             }
        }
    }

    fun setLoginError(error: String) {
        _loginState.value = LoginState.Error(error)
    }

    fun savePreferences(role: String, language: String) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                try {
                    val updates = mapOf(
                        "role" to role,
                        "language" to language
                    )
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .set(updates, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                    repository.saveUserRole(role)
                    repository.saveLanguage(language)
                } catch (e: Exception) {
                    // Handle error if needed
                }
            }
        }
    }

    fun saveRole(role: String) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                try {
                    val updates = mapOf("role" to role)
                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .set(updates, com.google.firebase.firestore.SetOptions.merge())
                        .await()
                    repository.saveUserRole(role)
                } catch (e: Exception) {
                    // Handle error if needed
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            auth.signOut()
            repository.clearSession()
            _loginState.value = LoginState.Idle
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val token: String) : LoginState()
    data class Error(val message: String) : LoginState()
}
