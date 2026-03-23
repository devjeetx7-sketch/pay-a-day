package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardState(
    val role: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val isLoading: Boolean = false,

    // Contractor Stats
    val totalWorkers: String = "0",
    val todayPresent: String = "0",
    val totalPaidMonth: String = "0",
    val pendingAmount: String = "0",

    // Personal Stats
    val todayEarned: String = "0",
    val monthEarned: String = "0",

    // Personal Attendance State
    val todayStatus: String? = null,
    val overtimeHours: Int = 0,
    val todayNote: String? = null
)

class DashboardViewModel(private val repository: UserPreferencesRepository) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState

    init {
        viewModelScope.launch {
            val role = repository.userRoleFlow.firstOrNull()
            if (role != null) {
                loadDashboardData(role)
            }
        }
    }

    fun loadDashboardData(role: String) {
        viewModelScope.launch {
            _dashboardState.value = _dashboardState.value.copy(isLoading = true, role = role)

            val user = auth.currentUser
            if (user == null) {
                _dashboardState.value = _dashboardState.value.copy(isLoading = false)
                return@launch
            }

            val userDoc = db.collection("users").document(user.uid).get().await()
            val name = userDoc.getString("name") ?: user.displayName ?: "User"
            val photoUrl = user.photoUrl?.toString() ?: ""
            val defaultWage = userDoc.getDouble("daily_wage") ?: 500.0

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val currentMonthStr = sdfMonth.format(Date())

            try {
                if (role == "contractor") {
                    val workersSnapshot = db.collection("workers")
                        .whereEqualTo("contractorId", user.uid)
                        .get()
                        .await()

                    val totalWorkers = workersSnapshot.size()
                    var totalAdvanceMonth = 0.0
                    var totalEarningsMonth = 0.0
                    var pendingTotal = 0.0

                    val workersMap = mutableMapOf<String, Double>()
                    for (doc in workersSnapshot.documents) {
                        workersMap["worker_${doc.id}"] = doc.getDouble("wage") ?: 0.0
                    }

                    val attendanceSnap = db.collection("attendance")
                        .whereEqualTo("contractorId", user.uid)
                        .get().await()

                    var todayPresentCount = 0

                    for (att in attendanceSnap.documents) {
                        val date = att.getString("date") ?: ""
                        val status = att.getString("status") ?: ""
                        val type = att.getString("type") ?: "full"
                        val workerId = att.getString("user_id") ?: ""
                        val advance = att.getDouble("advance_amount") ?: 0.0
                        val wage = workersMap[workerId] ?: 0.0

                        if (date == todayStr && status == "present") {
                            todayPresentCount++
                        }

                        // Pending Calculation (All Time)
                        if (status == "present") {
                            pendingTotal += if (type == "half") wage / 2 else wage
                        } else if (status == "advance") {
                            pendingTotal -= advance
                        }

                        // Monthly Paid Calculation
                        if (date.startsWith(currentMonthStr) && status == "advance") {
                            totalAdvanceMonth += advance
                        }
                    }

                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        name = name,
                        photoUrl = photoUrl,
                        totalWorkers = totalWorkers.toString(),
                        todayPresent = todayPresentCount.toString(),
                        totalPaidMonth = totalAdvanceMonth.toInt().toString(),
                        pendingAmount = pendingTotal.toInt().toString()
                    )
                } else {
                    // Personal Dashboard
                    val attendanceSnapshot = db.collection("attendance")
                        .whereEqualTo("user_id", user.uid)
                        .get()
                        .await()

                    var todayEarned = 0.0
                    var monthEarned = 0.0

                    var currentTodayStatus: String? = null
                    var currentOvertime = 0
                    var currentNote: String? = null

                    for (doc in attendanceSnapshot.documents) {
                        val date = doc.getString("date") ?: ""
                        val status = doc.getString("status") ?: ""
                        val type = doc.getString("type") ?: "full"
                        val advance = doc.getDouble("advance_amount") ?: 0.0

                        val dailyValue = if (status == "present") {
                            if (type == "half") defaultWage / 2 else defaultWage
                        } else 0.0

                        if (date == todayStr) {
                            if (status != "advance") {
                                currentTodayStatus = status
                                currentOvertime = doc.getDouble("overtime_hours")?.toInt() ?: 0
                                currentNote = doc.getString("note")
                                todayEarned = dailyValue
                            }
                        }

                        if (date.startsWith(currentMonthStr)) {
                            monthEarned += dailyValue
                        }
                    }

                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        name = name,
                        photoUrl = photoUrl,
                        todayEarned = todayEarned.toInt().toString(),
                        monthEarned = monthEarned.toInt().toString(),
                        todayStatus = currentTodayStatus,
                        overtimeHours = currentOvertime,
                        todayNote = currentNote
                    )
                }
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(isLoading = false)
            }
        }
    }
}
