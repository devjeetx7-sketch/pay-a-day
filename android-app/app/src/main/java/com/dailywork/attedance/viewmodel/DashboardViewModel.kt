package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class DashboardState(
    val role: String = "",
    val isLoading: Boolean = false,
    val totalWorkers: String = "0",
    val presentWorkers: String = "0",
    val pendingAmount: String = "Rs. 0",
    val daysWorked: String = "0",
    val earnings: String = "Rs. 0"
)

class DashboardViewModel(private val repository: UserPreferencesRepository) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState

    init {
        viewModelScope.launch {
            repository.userRoleFlow.collect { role ->
                if (role != null) {
                    loadDashboardData(role)
                }
            }
        }
    }

    private fun loadDashboardData(role: String) {
        viewModelScope.launch {
            _dashboardState.value = _dashboardState.value.copy(isLoading = true, role = role)

            val uid = auth.currentUser?.uid
            if (uid == null) {
                _dashboardState.value = _dashboardState.value.copy(isLoading = false)
                return@launch
            }

            try {
                if (role == "CONTRACTOR") {
                    val workersSnapshot = db.collection("workers")
                        .whereEqualTo("contractorId", uid)
                        .get()
                        .await()

                    val totalWorkers = workersSnapshot.size()
                    var pendingTotal = 0.0

                    for (doc in workersSnapshot.documents) {
                        val attendanceSnap = db.collection("attendance")
                            .whereEqualTo("workerId", doc.id)
                            .get().await()

                        // Simple balance calculation (wages - advance)
                        var earnings = 0.0
                        var advance = 0.0
                        for (att in attendanceSnap) {
                            val status = att.getString("status")
                            val wage = att.getDouble("wage") ?: 0.0
                            val adv = att.getDouble("advance") ?: 0.0
                            if (status == "PRESENT") {
                                earnings += wage
                            } else if (status == "HALF_DAY") {
                                earnings += wage / 2
                            }
                            advance += adv
                        }
                        pendingTotal += (earnings - advance)
                    }

                    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val presentSnapshot = db.collection("attendance")
                        .whereEqualTo("contractorId", uid)
                        .whereEqualTo("date", todayStr)
                        .whereIn("status", listOf("PRESENT", "HALF_DAY"))
                        .get()
                        .await()

                    val presentWorkers = presentSnapshot.size()

                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        totalWorkers = totalWorkers.toString(),
                        presentWorkers = presentWorkers.toString(),
                        pendingAmount = "Rs. ${pendingTotal.toInt()}"
                    )
                } else {
                    val attendanceSnapshot = db.collection("attendance")
                        .whereEqualTo("workerId", uid)
                        .get()
                        .await()

                    var totalWorked = 0
                    var earnings = 0.0
                    for (doc in attendanceSnapshot) {
                        val status = doc.getString("status")
                        val wage = doc.getDouble("wage") ?: 0.0
                        if (status == "PRESENT") {
                            totalWorked++
                            earnings += wage
                        } else if (status == "HALF_DAY") {
                            totalWorked++
                            earnings += (wage / 2)
                        }
                    }

                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        daysWorked = totalWorked.toString(),
                        earnings = "Rs. ${earnings.toInt()}"
                    )
                }
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(isLoading = false)
            }
        }
    }
}
