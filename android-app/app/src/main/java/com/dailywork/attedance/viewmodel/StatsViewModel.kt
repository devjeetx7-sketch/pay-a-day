package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WorkerStats(
    val name: String,
    val days: Double,
    val cost: Double
)

data class ContractorStatsData(
    val totalCost: Double = 0.0,
    val totalDailyWorks: Double = 0.0,
    val topWorkers: List<WorkerStats> = emptyList()
)

data class PersonalStatsData(
    val present: Int = 0,
    val absent: Int = 0,
    val halfDays: Int = 0,
    val overtime: Int = 0,
    val advanceTotal: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val allTimeDays: Int = 0,
    val allTimeEarnings: Double = 0.0
)

data class StatsState(
    val role: String = "",
    val isLoading: Boolean = true,
    val selectedMonthDate: Date = Date(),

    // Contractor
    val contractorStats: ContractorStatsData = ContractorStatsData(),

    // Personal
    val personalStats: PersonalStatsData = PersonalStatsData()
)

class StatsViewModel(private val repository: UserPreferencesRepository) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _statsState = MutableStateFlow(StatsState())
    val statsState: StateFlow<StatsState> = _statsState

    private var workersListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var attendanceListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null

    private var cachedDefaultWage: Double = 500.0
    private var cachedWorkers: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()

    init {
        viewModelScope.launch {
            repository.userRoleFlow.collect { role ->
                if (role != null) {
                    _statsState.value = _statsState.value.copy(role = role)
                    setupListeners(role)
                }
            }
        }
    }

    fun changeMonth(offset: Int) {
        val cal = Calendar.getInstance()
        cal.time = _statsState.value.selectedMonthDate
        cal.add(Calendar.MONTH, offset)
        _statsState.value = _statsState.value.copy(
            selectedMonthDate = cal.time,
            isLoading = true
        )
        setupListeners(_statsState.value.role)
    }

    private fun setupListeners(role: String) {
        val user = auth.currentUser ?: return

        userListener?.remove()
        workersListener?.remove()
        attendanceListener?.remove()

        userListener = db.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    cachedDefaultWage = snapshot.getDouble("daily_wage") ?: 500.0
                    calculatePersonalStats(emptyList()) // trigger re-calculation if needed, but attendance listener handles it
                }
            }

        val cal = Calendar.getInstance()
        cal.time = _statsState.value.selectedMonthDate
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val yearMonth = sdfMonth.format(cal.time)

        if (role == "contractor") {
            workersListener = db.collection("workers")
                .whereEqualTo("contractorId", user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    cachedWorkers = snapshot.documents

                    attendanceListener?.remove()
                    attendanceListener = db.collection("attendance")
                        .whereEqualTo("contractorId", user.uid)
                        // .whereGreaterThanOrEqualTo("date", "$yearMonth-01") // Could optimize, but matching logic
                        .addSnapshotListener { attSnapshot, attError ->
                            if (attError != null || attSnapshot == null) return@addSnapshotListener
                            calculateContractorStats(attSnapshot.documents, yearMonth)
                        }
                }
        } else {
            // For personal, we fetch ALL to compute allTime stats just like web, but ideally should be optimized
            attendanceListener = db.collection("attendance")
                .whereEqualTo("user_id", user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    calculatePersonalStats(snapshot.documents)
                }
        }
    }

    private fun calculateContractorStats(attendanceDocs: List<com.google.firebase.firestore.DocumentSnapshot>, yearMonth: String) {
        var totalCost = 0.0
        var totalDays = 0.0
        val workerPerfMap = mutableMapOf<String, WorkerStats>()

        val workersMap = cachedWorkers.associateBy({ "worker_${it.id}" }, { it })

        attendanceDocs.forEach { doc ->
            val date = doc.getString("date") ?: ""
            val status = doc.getString("status") ?: ""
            if (date.startsWith(yearMonth) && status == "present") {
                val userId = doc.getString("user_id") ?: ""
                val workerDoc = workersMap[userId]

                if (workerDoc != null) {
                    val wage = workerDoc.getDouble("wage") ?: 0.0
                    val type = doc.getString("type") ?: "full"

                    val dayVal = if (type == "half") 0.5 else 1.0
                    val costVal = if (type == "half") wage / 2 else wage

                    val currentStats = workerPerfMap[userId] ?: WorkerStats(workerDoc.getString("name") ?: "Unknown", 0.0, 0.0)
                    workerPerfMap[userId] = currentStats.copy(
                        days = currentStats.days + dayVal,
                        cost = currentStats.cost + costVal
                    )

                    totalDays += dayVal
                    totalCost += costVal
                }
            }
        }

        val topWorkers = workerPerfMap.values.toList().sortedByDescending { it.days }

        _statsState.value = _statsState.value.copy(
            isLoading = false,
            contractorStats = ContractorStatsData(
                totalCost = totalCost,
                totalDailyWorks = totalDays,
                topWorkers = topWorkers
            )
        )
    }

    private fun calculatePersonalStats(attendanceDocs: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val cal = Calendar.getInstance()
        cal.time = _statsState.value.selectedMonthDate
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val yearMonth = sdfMonth.format(cal.time)

        var present = 0
        var absent = 0
        var halfDays = 0
        var overtime = 0
        var advanceTotal = 0.0
        var totalEarnings = 0.0

        var allTimeDays = 0
        var allTimeEarnings = 0.0

        attendanceDocs.forEach { doc ->
            val date = doc.getString("date") ?: ""
            val status = doc.getString("status") ?: ""
            val type = doc.getString("type") ?: "full"
            val adv = doc.getDouble("advance_amount") ?: 0.0
            val ot = doc.getDouble("overtime_hours")?.toInt() ?: 0

            val dayVal = if (type == "half") 0.5 else 1.0
            val costVal = if (type == "half") cachedDefaultWage / 2 else cachedDefaultWage

            // All time
            if (status == "present") {
                allTimeDays++
                allTimeEarnings += costVal
            }

            // Current month
            if (date.startsWith(yearMonth)) {
                if (status == "present") {
                    present++
                    if (type == "half") halfDays++
                    overtime += ot
                    totalEarnings += costVal
                } else if (status == "absent") {
                    absent++
                }
                if (adv > 0) {
                    advanceTotal += adv
                }
            }
        }

        _statsState.value = _statsState.value.copy(
            isLoading = false,
            personalStats = PersonalStatsData(
                present = present,
                absent = absent,
                halfDays = halfDays,
                overtime = overtime,
                advanceTotal = advanceTotal,
                totalEarnings = totalEarnings,
                allTimeDays = allTimeDays,
                allTimeEarnings = allTimeEarnings
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        workersListener?.remove()
        attendanceListener?.remove()
    }
}
