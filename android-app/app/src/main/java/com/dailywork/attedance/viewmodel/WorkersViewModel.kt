package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.SyncRepository
import com.dailywork.attedance.data.UserPreferencesRepository
import com.dailywork.attedance.data.WorkerEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

typealias WorkerItem = WorkerEntity

data class WorkersState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val role: String = "",
    val workers: List<WorkerItem> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isPremium: Boolean = false
)

class WorkersViewModel(
    private val repository: UserPreferencesRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow(WorkersState())
    val state: StateFlow<WorkersState> = _state

    private var workersJob: Job? = null
    private var userJob: Job? = null

    init {
        viewModelScope.launch {
            repository.userRoleFlow.collectLatest { role ->
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
        userJob?.cancel()
        userJob = viewModelScope.launch {
            syncRepository.getUserFlow(user.uid).collectLatest { userEntity ->
                if (userEntity != null) {
                    _state.value = _state.value.copy(isPremium = userEntity.isPremium)
                }
            }
        }
        viewModelScope.launch { syncRepository.syncUser(user.uid) }
    }

    fun refresh() {
        _state.value = _state.value.copy(isRefreshing = true)
        setupListener()
        setupUserListener()
    }

    private fun setupListener() {
        val user = auth.currentUser ?: return

        workersJob?.cancel()

        workersJob = viewModelScope.launch {
            syncRepository.getWorkersFlow(user.uid).collectLatest { workerList ->
                _state.value = _state.value.copy(
                    workers = workerList,
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
        viewModelScope.launch { syncRepository.syncWorkers(user.uid) }
    }

    fun saveWorker(worker: WorkerItem) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)

            try {
                val workerToSave = worker.copy(
                    id = if (worker.id.isEmpty()) java.util.UUID.randomUUID().toString() else worker.id,
                    contractorId = user.uid,
                    timestamp = System.currentTimeMillis()
                )
                syncRepository.saveWorkerOptimistically(workerToSave)
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
                syncRepository.deleteWorkerOptimistically(workerId)
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
        workersJob?.cancel()
        userJob?.cancel()
    }
}
