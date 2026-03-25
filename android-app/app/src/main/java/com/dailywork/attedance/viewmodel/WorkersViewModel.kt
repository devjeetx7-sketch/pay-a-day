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

data class WorkerItem(
    val id: String,
    val name: String,
    val phone: String,
    val aadhar: String,
    val age: String,
    val workType: String,
    val wage: Double,
    val contractorId: String
)

data class WorkersState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val role: String = "",
    val workers: List<WorkerItem> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isPremium: Boolean = false
)

class WorkersViewModel(private val repository: UserPreferencesRepository) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(WorkersState())
    val state: StateFlow<WorkersState> = _state

    private var workersListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        viewModelScope.launch {
            repository.userRoleFlow.collect { role ->
                if (role != null) {
                    _state.value = _state.value.copy(role = role)
                    if (role == "contractor") {
                        setupListener()
                        setupUserListener()
                    }
                }
            }
        }
    }

    private fun setupUserListener() {
        val user = auth.currentUser ?: return
        userListener?.remove()
        userListener = db.collection("users").document(user.uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
            _state.value = _state.value.copy(
                isPremium = snapshot.getBoolean("isPremium") ?: false
            )
        }
    }

    fun refresh() {
        _state.value = _state.value.copy(isRefreshing = true)
        setupListener()
        setupUserListener()
    }

    private fun setupListener() {
        val user = auth.currentUser ?: return

        workersListener?.remove()

        workersListener = db.collection("workers")
            .whereEqualTo("contractorId", user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _state.value = _state.value.copy(isLoading = false, isRefreshing = false, errorMessage = error?.message)
                    return@addSnapshotListener
                }

                val workerList = snapshot.documents.mapNotNull { doc ->
                    WorkerItem(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        phone = doc.getString("phone") ?: "",
                        aadhar = doc.getString("aadhar") ?: "",
                        age = doc.getString("age") ?: "",
                        workType = doc.getString("workType") ?: "Labour",
                        wage = doc.getDouble("wage") ?: 500.0,
                        contractorId = doc.getString("contractorId") ?: ""
                    )
                }

                _state.value = _state.value.copy(
                    workers = workerList,
                    isLoading = false,
                    isRefreshing = false
                )
            }
    }

    fun saveWorker(worker: WorkerItem) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)

            try {
                val data: MutableMap<String, Any> = mutableMapOf(
                    "name" to worker.name,
                    "phone" to worker.phone,
                    "aadhar" to worker.aadhar,
                    "age" to worker.age,
                    "workType" to worker.workType,
                    "wage" to worker.wage,
                    "contractorId" to user.uid
                )

                if (worker.id.isEmpty()) {
                    data["created_at"] = com.google.firebase.firestore.FieldValue.serverTimestamp()
                    db.collection("workers").add(data).await()
                } else {
                    db.collection("workers").document(worker.id).set(data, SetOptions.merge()).await()
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = "Failed to save worker: ${e.message}")
            } finally {
                _state.value = _state.value.copy(isSaving = false)
            }
        }
    }

    fun deleteWorker(workerId: String) {
        viewModelScope.launch {
            try {
                db.collection("workers").document(workerId).delete().await()
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = "Failed to delete worker")
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        workersListener?.remove()
        userListener?.remove()
    }
}
