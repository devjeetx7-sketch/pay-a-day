package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.dailywork.attedance.data.FirestoreRepository
import com.google.firebase.firestore.FirebaseFirestore

class AuthViewModel(
    val repository: UserPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _resendTimer = MutableStateFlow(0)
    val resendTimer: StateFlow<Int> = _resendTimer

    private var timerJob: Job? = null
    private var pendingRegistration: Map<String, String>? = null

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

    private fun canProceed(): Boolean {
        val state = _loginState.value
        return state !is LoginState.Loading &&
                state !is LoginState.OtpSending &&
                state !is LoginState.OtpVerifying &&
                state !is LoginState.OtpResending
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isStrongPassword(password: String): Boolean {
        // At least 8 characters, one uppercase, one lowercase, one digit
        val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$".toRegex()
        return passwordPattern.matches(password)
    }

    fun sendPasswordResetEmail(email: String) {
        if (!canProceed()) return
        viewModelScope.launch {
            if (email.isBlank()) {
                _loginState.value = LoginState.Error("Email cannot be empty")
                return@launch
            }
            if (!isValidEmail(email)) {
                _loginState.value = LoginState.Error("Please enter a valid email address")
                return@launch
            }

            _loginState.value = LoginState.Loading
            try {
                auth.sendPasswordResetEmail(email).await()
                _loginState.value = LoginState.PasswordResetSent
            } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                _loginState.value = LoginState.Error("No account found for this email.")
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Failed to send reset email")
            }
        }
    }

    fun resetToIdle() {
        _loginState.value = LoginState.Idle
    }

    fun loginWithEmail(email: String, pass: String) {
        if (!canProceed()) return
        viewModelScope.launch {
            if (email.isBlank() || pass.isBlank()) {
                _loginState.value = LoginState.Error("Email and password cannot be empty")
                return@launch
            }
            if (!isValidEmail(email)) {
                _loginState.value = LoginState.Error("Please enter a valid email address")
                return@launch
            }

            _loginState.value = LoginState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                val uid = result.user?.uid ?: ""

                fetchAndSaveUserRole(uid)
                repository.saveAuthToken(uid)
                _loginState.value = LoginState.Success(uid)
            } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                _loginState.value = LoginState.Error("No account found. Please create an account first.")
            } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                _loginState.value = LoginState.Error("Invalid email or password.")
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun registerWithEmail(email: String, pass: String, name: String) {
        if (!canProceed()) return
        viewModelScope.launch {
            if (name.isBlank()) {
                _loginState.value = LoginState.Error("Full name is required")
                return@launch
            }
            if (email.isBlank() || pass.isBlank()) {
                _loginState.value = LoginState.Error("Email and password cannot be empty")
                return@launch
            }
            if (!isValidEmail(email)) {
                _loginState.value = LoginState.Error("Please enter a valid email address")
                return@launch
            }
            if (!isStrongPassword(pass)) {
                _loginState.value = LoginState.Error("Password must be at least 8 characters and contain at least one uppercase letter, one lowercase letter, and one digit")
                return@launch
            }

            _loginState.value = LoginState.Loading
            try {
                // Check if account already exists
                val methods = auth.fetchSignInMethodsForEmail(email).await().signInMethods
                if (!methods.isNullOrEmpty()) {
                    _loginState.value = LoginState.Error("Account already exists. Please sign in.")
                    return@launch
                }

                pendingRegistration = mapOf(
                    "email" to email,
                    "pass" to pass,
                    "name" to name
                )
                sendOtp(email)
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Registration failed")
            }
        }
    }

    private fun sendOtp(email: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.OtpSending
            try {
                val otp = (100000..999999).random().toString()
                val timestamp = System.currentTimeMillis()
                val expiry = timestamp + (5 * 60 * 1000) // 5 minutes expiry

                // 1. Store OTP in Firestore for verification
                val otpData = mapOf(
                    "code" to otp,
                    "createdAt" to timestamp,
                    "expiresAt" to expiry,
                    "email" to email
                )
                db.collection("otps").document(email).set(otpData).await()

                // 2. Trigger Email delivery via 'mail' collection
                val mailData = mapOf(
                    "to" to listOf(email),
                    "message" to mapOf(
                        "subject" to "Your Verification Code - DailyWork",
                        "html" to """
                            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #ddd; padding: 20px; border-radius: 10px;">
                                <h2 style="color: #EF4444; text-align: center;">DailyWork Verification</h2>
                                <p>Hello,</p>
                                <p>Use the following 6-digit code to verify your email address. This code is valid for 5 minutes.</p>
                                <div style="background: #f4f4f4; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; letter-spacing: 5px; border-radius: 5px;">
                                    $otp
                                </div>
                                <p>If you did not request this code, please ignore this email.</p>
                                <p style="font-size: 12px; color: #777; text-align: center; margin-top: 20px;">
                                    &copy; ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)} DailyWork App. All rights reserved.
                                </p>
                            </div>
                        """.trimIndent()
                    )
                )
                db.collection("mail").add(mailData).await()

                android.util.Log.d("AuthViewModel", "OTP for $email: $otp")
                _loginState.value = LoginState.OtpSent
                startResendTimer()
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Failed to send OTP: ${e.message}")
            }
        }
    }

    private fun startResendTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            _resendTimer.value = 60
            while (_resendTimer.value > 0) {
                delay(1000)
                _resendTimer.value -= 1
            }
        }
    }

    fun resendOtp() {
        val email = pendingRegistration?.get("email")
        if (email != null && _resendTimer.value == 0) {
            _loginState.value = LoginState.OtpResending
            sendOtp(email)
        }
    }

    fun verifyOtp(otp: String) {
        if (!canProceed()) return
        viewModelScope.launch {
            if (otp.length != 6) {
                _loginState.value = LoginState.Error("Please enter a valid 6-digit code.")
                return@launch
            }

            _loginState.value = LoginState.OtpVerifying
            var createdUser: com.google.firebase.auth.FirebaseUser? = null
            try {
                val email = pendingRegistration?.get("email") ?: ""
                val pass = pendingRegistration?.get("pass") ?: ""
                val name = pendingRegistration?.get("name") ?: ""

                // 1. Create Auth User first (required for isOwner rule)
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                createdUser = result.user
                val uid = createdUser?.uid ?: throw Exception("Failed to create user account")

                // 2. Attempt to create User document in Firestore
                // This will succeed ONLY if the OTP matches via Security Rules
                val userData = mapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to email,
                    "otp" to otp, // OTP passed for rule verification
                    "createdAt" to System.currentTimeMillis(),
                    "role" to "contractor",
                    "isBlocked" to false,
                    "isPremium" to false
                )

                db.collection("users")
                    .document(uid)
                    .set(userData)
                    .await()

                // 3. If we reached here, verification succeeded
                // Cleanup: Delete OTP document and remove otp field from user doc
                try {
                    db.collection("otps").document(email).delete().await()
                    db.collection("users").document(uid).update("otp", com.google.firebase.firestore.FieldValue.delete()).await()
                } catch (e: Exception) {
                    // Non-critical cleanup failure
                    android.util.Log.w("AuthViewModel", "Cleanup failed: ${e.message}")
                }

                repository.saveAuthToken(uid)
                repository.saveUserRole("contractor")
                _loginState.value = LoginState.Success(uid)

            } catch (e: Exception) {
                // If firestore write failed, it's likely due to invalid/expired OTP or connection
                // Rollback: Delete the auth user so they can try again
                createdUser?.delete()?.await()

                val message = if (e is com.google.firebase.firestore.FirebaseFirestoreException &&
                    e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    "Invalid or expired OTP. Please check and try again."
                } else if (e is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    "Account already exists. Please sign in."
                } else {
                    e.message ?: "Verification failed"
                }
                _loginState.value = LoginState.Error(message)
            }
        }
    }

    fun loginWithGoogleCredential(idToken: String) {
        if (!canProceed()) return
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

                     fetchAndSaveUserRole(uid)
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

    private suspend fun fetchAndSaveUserRole(uid: String) {
        try {
            val document = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .await()

            val isBlocked = document.getBoolean("isBlocked") ?: false
            if (isBlocked) {
                _loginState.value = LoginState.Blocked
                return
            }

            val role = document.getString("role")
            if (role != null) {
                repository.saveUserRole(role)
            }
        } catch (e: Exception) {
            // Log error or handle gracefully
        }
    }

    fun logout() {
        _loginState.value = LoginState.Idle
        auth.signOut()
        viewModelScope.launch {
            repository.clearSession()
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val token: String) : LoginState()
    object Blocked : LoginState()
    data class Error(val message: String) : LoginState()
    object OtpSent : LoginState()
    object OtpSending : LoginState()
    object OtpResending : LoginState()
    object OtpVerifying : LoginState()
    object PasswordResetSent : LoginState()
}
