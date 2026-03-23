package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SettingsState(
    val name: String = "",
    val dailyWage: Double = 500.0,
    val workType: String = "",
    val role: String = "",
    val isSaving: Boolean = false,
    val savedMessage: String? = null
)

class SettingsViewModel(private val repository: UserPreferencesRepository) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        setupListener()
    }

    private fun setupListener() {
        val user = auth.currentUser ?: return

        userListener = db.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    _state.value = _state.value.copy(
                        name = snapshot.getString("name") ?: user.displayName ?: "",
                        dailyWage = snapshot.getDouble("daily_wage") ?: 500.0,
                        workType = snapshot.getString("workType") ?: "",
                        role = snapshot.getString("role") ?: ""
                    )
                }
            }
    }

    fun saveName(newName: String) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            _state.value = _state.value.copy(isSaving = true)
            try {
                db.collection("users").document(user.uid).set(mapOf("name" to newName), SetOptions.merge()).await()
                _state.value = _state.value.copy(savedMessage = "Name saved successfully!")
            } catch (e: Exception) {
                _state.value = _state.value.copy(savedMessage = "Failed to save name.")
            } finally {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }

    fun saveWage(newWage: Double) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            _state.value = _state.value.copy(isSaving = true)
            try {
                db.collection("users").document(user.uid).set(mapOf("daily_wage" to newWage), SetOptions.merge()).await()
                _state.value = _state.value.copy(savedMessage = "Wage saved successfully!")
            } catch (e: Exception) {
                _state.value = _state.value.copy(savedMessage = "Failed to save wage.")
            } finally {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }

    fun saveWorkType(newWorkType: String) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            _state.value = _state.value.copy(isSaving = true)
            try {
                db.collection("users").document(user.uid).set(mapOf("workType" to newWorkType), SetOptions.merge()).await()
                _state.value = _state.value.copy(savedMessage = "Work type saved successfully!")
            } catch (e: Exception) {
                _state.value = _state.value.copy(savedMessage = "Failed to save work type.")
            } finally {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(savedMessage = null)
    }

    fun changeRole(onRoleCleared: () -> Unit) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            _state.value = _state.value.copy(isSaving = true)
            try {
                db.collection("users").document(user.uid).set(mapOf("role" to ""), SetOptions.merge()).await()
                // Just clear session or role to trigger re-selection
                repository.clearSession()
                onRoleCleared()
            } catch (e: Exception) {
                _state.value = _state.value.copy(savedMessage = "Failed to change role.")
            } finally {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }

    fun logout() {
        auth.signOut()
        viewModelScope.launch {
            repository.clearSession()
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}
