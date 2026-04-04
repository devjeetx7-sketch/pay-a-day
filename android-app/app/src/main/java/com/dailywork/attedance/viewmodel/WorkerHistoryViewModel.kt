package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class HistoryRecord(
    val id: String,
    val workerId: String,
    val workerName: String,
    val date: String,
    val type: String, // "Attendance", "Payment"
    val description: String,
    val amount: Double? = null,
    val status: String? = null, // "Present", "Absent", "Half Day"
    val timestamp: Long = 0
)

data class WorkerHistoryState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val records: List<HistoryRecord> = emptyList(),
    val filteredRecords: List<HistoryRecord> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: String = "All"
)

class WorkerHistoryViewModel(
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val _state = MutableStateFlow(WorkerHistoryState())
    val state: StateFlow<WorkerHistoryState> = _state

    private val attendanceListeners = mutableMapOf<String, ListenerRegistration>()
    private var workersListener: ListenerRegistration? = null
    private val workerNames = mutableMapOf<String, String>()

    init {
        setupWorkersListener()
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        setupWorkersListener()
    }

    private fun setupWorkersListener() {
        val user = auth.currentUser ?: return
        workersListener?.remove()

        workersListener = firestoreRepository.getContractorWorkers()
            ?.addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _state.update { it.copy(isLoading = false, isRefreshing = false) }
                    return@addSnapshotListener
                }

                val workers = snapshot.documents.map { doc ->
                    val id = doc.id
                    val name = doc.getString("name") ?: "Unknown"
                    workerNames[id] = name
                    id
                }

                _state.update { it.copy(isLoading = false, isRefreshing = false) }
                setupAttendanceListeners(workers)
            }
    }

    private fun setupAttendanceListeners(workerIds: List<String>) {
        // Clear old listeners that are no longer needed
        val currentKeys = attendanceListeners.keys.toSet()
        val removedKeys = currentKeys - workerIds.toSet()
        removedKeys.forEach {
            attendanceListeners[it]?.remove()
            attendanceListeners.remove(it)
        }

        workerIds.forEach { workerId ->
            if (!attendanceListeners.containsKey(workerId)) {
                val listener = firestoreRepository.getContractorAttendance(workerId)
                    ?.orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    ?.limit(20)
                    ?.addSnapshotListener { snapshot, error ->
                        if (error == null && snapshot != null) {
                            updateRecords(workerId, snapshot.documents)
                        }
                    }
                if (listener != null) {
                    attendanceListeners[workerId] = listener
                }
            }
        }
    }

    private fun updateRecords(workerId: String, documents: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val name = workerNames[workerId] ?: "Unknown"
        val newRecords = mutableListOf<HistoryRecord>()

        documents.forEach { doc ->
            val date = doc.getString("date") ?: ""
            val status = doc.getString("status") ?: ""
            val advance = doc.getDouble("advance_amount") ?: 0.0
            val type = doc.getString("type") ?: "full"
            val note = doc.getString("note") ?: ""
            val timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L

            if (status == "present" || status == "absent") {
                newRecords.add(
                    HistoryRecord(
                        id = "${workerId}_${doc.id}_att",
                        workerId = workerId,
                        workerName = name,
                        date = formatDate(date),
                        type = "Attendance",
                        description = if (status == "present") "Daily Work" else "Absent: $note",
                        status = when {
                            status == "absent" -> "Absent"
                            type == "half" -> "Half Day"
                            else -> "Present"
                        },
                        timestamp = timestamp
                    )
                )
            }

            if (advance > 0) {
                newRecords.add(
                    HistoryRecord(
                        id = "${workerId}_${doc.id}_pay",
                        workerId = workerId,
                        workerName = name,
                        date = formatDate(date),
                        type = "Payment",
                        description = "Advance Payment",
                        amount = advance,
                        timestamp = timestamp
                    )
                )
            }
        }

        _state.update { current ->
            // Remove existing records for this worker and add new ones
            val filteredList = current.records.filter { it.workerId != workerId }
            val updatedList = (filteredList + newRecords).sortedByDescending {
                if (it.timestamp > 0) it.timestamp else parseDateToLong(it.date)
            }
            current.copy(records = updatedList).also { applyFilters(it) }
        }
    }

    private fun applyFilters(currentState: WorkerHistoryState) {
        var filtered = currentState.records

        if (currentState.searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.workerName.contains(currentState.searchQuery, ignoreCase = true)
            }
        }

        if (currentState.selectedFilter != "All") {
            filtered = filtered.filter { it.type == currentState.selectedFilter }
        }

        _state.update { it.copy(filteredRecords = filtered) }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        applyFilters(_state.value)
    }

    fun onFilterChange(filter: String) {
        _state.update { it.copy(selectedFilter = filter) }
        applyFilters(_state.value)
    }

    private fun formatDate(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr)
            val outputSdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            outputSdf.format(date!!)
        } catch (e: Exception) {
            dateStr
        }
    }

    private fun parseDateToLong(dateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.parse(dateStr)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    override fun onCleared() {
        super.onCleared()
        workersListener?.remove()
        attendanceListeners.values.forEach { it.remove() }
        attendanceListeners.clear()
    }
}
