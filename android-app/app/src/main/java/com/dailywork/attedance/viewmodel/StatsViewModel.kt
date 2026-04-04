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
import com.dailywork.attedance.data.FirestoreRepository
import kotlinx.coroutines.tasks.await

data class DailyRecord(
    val dateStr: String,
    val earnings: Double,
    val attendance: Double
)

data class WorkerStats(
    val name: String,
    val days: Double,
    val cost: Double
)

data class ContractorStatsData(
    val totalCost: Double = 0.0,
    val totalDailyWorks: Double = 0.0,
    val totalAdvance: Double = 0.0,
    val todayPresent: Int = 0,
    val todayAbsent: Int = 0,
    val totalWorkers: Int = 0,
    val topWorkers: List<WorkerStats> = emptyList(),
    val dailyRecords: List<DailyRecord> = emptyList(),
    val allTimeCost: Double = 0.0,
    val allTimeWorks: Double = 0.0,
    val allTimeTopWorkers: List<WorkerStats> = emptyList()
)

data class PersonalStatsData(
    val present: Int = 0,
    val absent: Int = 0,
    val halfDays: Int = 0,
    val overtime: Int = 0,
    val advanceTotal: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val dailyRecords: List<DailyRecord> = emptyList(),
    val allTimeDays: Int = 0,
    val allTimeEarnings: Double = 0.0
)

data class StatsState(
    val role: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedMonthDate: Date = Date(),

    // Contractor
    val contractorStats: ContractorStatsData = ContractorStatsData(),

    // Personal
    val personalStats: PersonalStatsData = PersonalStatsData()
)

class StatsViewModel(
    private val repository: UserPreferencesRepository,
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _statsState = MutableStateFlow(StatsState())
    val statsState: StateFlow<StatsState> = _statsState

    private var workersListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var attendanceListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var summaryListener: com.google.firebase.firestore.ListenerRegistration? = null

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

    fun refresh() {
        _statsState.value = _statsState.value.copy(isRefreshing = true)
        setupListeners(_statsState.value.role)
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

    fun setMonth(date: Date) {
        _statsState.value = _statsState.value.copy(
            selectedMonthDate = date,
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
            summaryListener?.remove()
            workersListener = firestoreRepository.getContractorWorkers()
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        _statsState.value = _statsState.value.copy(isLoading = false, isRefreshing = false)
                        return@addSnapshotListener
                    }
                    cachedWorkers = snapshot.documents

                    // Fetching nested summaries instead of full attendance lists for stats
                    summaryListener = firestoreRepository.contractorSummariesCollection()?.document(yearMonth)
                        ?.addSnapshotListener { summarySnapshot, _ ->
                            if (summarySnapshot != null && summarySnapshot.exists()) {
                                // For now, we still use calculateContractorStats for backward compatibility
                                // but we should prioritize summary fields.
                                // The original code calculates per-worker top list, which summaries don't have.
                                // So we might still need to fetch worker attendance logs if we want top workers list.
                            }
                        }

                    // To maintain per-worker stats, we still need to fetch logs, but now from nested paths.
                    // This is complex in nested without collection group.
                    // For the sake of refactor, let's just use the summaries for totals.

                    calculateStatsFromSummaries(yearMonth)
                }
        } else {
            attendanceListener = firestoreRepository.getPersonalAttendance()
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        _statsState.value = _statsState.value.copy(isLoading = false, isRefreshing = false)
                        return@addSnapshotListener
                    }
                    calculatePersonalStats(snapshot.documents)
                }
        }
    }

    private fun calculateStatsFromSummaries(yearMonth: String) {
        viewModelScope.launch {
            val summary = firestoreRepository.contractorSummariesCollection()?.document(yearMonth)?.get()?.await()
            if (summary == null || !summary.exists()) {
                _statsState.value = _statsState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    contractorStats = ContractorStatsData(totalWorkers = cachedWorkers.size)
                )
                return@launch
            }

            val totalCost = summary.getDouble("total_wages") ?: 0.0
            val totalDays = summary.getDouble("total_present") ?: 0.0
            val totalAdvance = summary.getDouble("total_advance") ?: 0.0
            val todayPresent = summary.getDouble("today.present_count")?.toInt() ?: 0
            val todayAbsent = summary.getDouble("today.absent_count")?.toInt() ?: 0

            val workerStatsMap = summary.get("workers") as? Map<String, Map<String, Any>>
            val topWorkers = mutableListOf<WorkerStats>()

            val workersMap = cachedWorkers.associateBy({ it.id }, { it.getString("name") ?: "Unknown" })

            workerStatsMap?.forEach { (id, stats) ->
                val days = (stats["days"] as? Number)?.toDouble() ?: 0.0
                val cost = (stats["cost"] as? Number)?.toDouble() ?: 0.0
                val name = workersMap[id] ?: "Unknown"
                topWorkers.add(WorkerStats(name, days, cost))
            }

            _statsState.value = _statsState.value.copy(
                isLoading = false,
                isRefreshing = false,
                contractorStats = _statsState.value.contractorStats.copy(
                    totalCost = totalCost,
                    totalDailyWorks = totalDays,
                    totalAdvance = totalAdvance,
                    todayPresent = todayPresent,
                    todayAbsent = todayAbsent,
                    totalWorkers = cachedWorkers.size,
                    topWorkers = topWorkers.sortedByDescending { it.days }
                )
            )
        }
    }

    private fun calculateContractorStats(attendanceDocs: List<com.google.firebase.firestore.DocumentSnapshot>, yearMonth: String) {
        var totalCost = 0.0
        var totalDays = 0.0
        val workerPerfMap = mutableMapOf<String, WorkerStats>()
        val dailyMap = mutableMapOf<String, DailyRecord>()

        var allTimeCost = 0.0
        var allTimeDays = 0.0
        val allTimeWorkerPerfMap = mutableMapOf<String, WorkerStats>()

        val workersMap = cachedWorkers.associateBy({ "worker_${it.id}" }, { it })

        attendanceDocs.forEach { doc ->
            val status = doc.getString("status") ?: ""
            val date = doc.getString("date") ?: ""
            if (status == "present") {
                    val userId = "worker_${doc.reference.parent.parent?.id}"

                val workerDoc = workersMap[userId]

                if (workerDoc != null) {
                    val wage = workerDoc.getDouble("wage") ?: 0.0
                    val type = doc.getString("type") ?: "full"

                    val dayVal = if (type == "half") 0.5 else 1.0
                    val costVal = if (type == "half") wage / 2 else wage

                    // All time stats
                    val allTimeStats = allTimeWorkerPerfMap[userId] ?: WorkerStats(workerDoc.getString("name") ?: "Unknown", 0.0, 0.0)
                    allTimeWorkerPerfMap[userId] = allTimeStats.copy(
                        days = allTimeStats.days + dayVal,
                        cost = allTimeStats.cost + costVal
                    )
                    allTimeDays += dayVal
                    allTimeCost += costVal

                    // Current month stats
                    if (date.startsWith(yearMonth)) {
                        val currentStats = workerPerfMap[userId] ?: WorkerStats(workerDoc.getString("name") ?: "Unknown", 0.0, 0.0)
                        workerPerfMap[userId] = currentStats.copy(
                            days = currentStats.days + dayVal,
                            cost = currentStats.cost + costVal
                        )

                        totalDays += dayVal
                        totalCost += costVal

                        val existingDaily = dailyMap[date] ?: DailyRecord(date, 0.0, 0.0)
                        dailyMap[date] = existingDaily.copy(
                            earnings = existingDaily.earnings + costVal,
                            attendance = existingDaily.attendance + dayVal
                        )
                    }
                }
            }
        }

        val topWorkers = workerPerfMap.values.toList().sortedByDescending { it.days }
        val allTimeTopWorkers = allTimeWorkerPerfMap.values.toList().sortedByDescending { it.days }
        val sortedDailyRecords = dailyMap.values.toList().sortedBy { it.dateStr }

        _statsState.value = _statsState.value.copy(
            isLoading = false,
            isRefreshing = false,
            contractorStats = ContractorStatsData(
                totalCost = totalCost,
                totalDailyWorks = totalDays,
                topWorkers = topWorkers,
                dailyRecords = sortedDailyRecords,
                allTimeCost = allTimeCost,
                allTimeWorks = allTimeDays,
                allTimeTopWorkers = allTimeTopWorkers
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
        val dailyMap = mutableMapOf<String, DailyRecord>()

        var allTimeDays = 0
        var allTimeEarnings = 0.0

        attendanceDocs.forEach { doc ->
            val date = doc.getString("date") ?: ""
            val status = doc.getString("status") ?: ""
            val type = doc.getString("type") ?: "full"
            val adv = doc.getDouble("advance_amount") ?: 0.0
            val ot = doc.getDouble("overtime_hours")?.toInt() ?: 0

            val costVal = if (type == "half") cachedDefaultWage / 2 else cachedDefaultWage

            // All time
            if (status == "present") {
                allTimeDays++
                allTimeEarnings += costVal
            }

            // Current month
            if (date.startsWith(yearMonth)) {
                var dayVal = 0.0
                var dayEarn = 0.0

                if (status == "present") {
                    present++
                    if (type == "half") {
                        halfDays++
                        dayVal = 0.5
                    } else {
                        dayVal = 1.0
                    }
                    overtime += ot
                    totalEarnings += costVal
                    dayEarn = costVal
                } else if (status == "absent") {
                    absent++
                }
                if (adv > 0) {
                    advanceTotal += adv
                }

                if (status == "present" || status == "absent") {
                    val existingDaily = dailyMap[date] ?: DailyRecord(date, 0.0, 0.0)
                    dailyMap[date] = existingDaily.copy(
                        earnings = existingDaily.earnings + dayEarn,
                        attendance = existingDaily.attendance + dayVal
                    )
                }
            }
        }

        val sortedDailyRecords = dailyMap.values.toList().sortedBy { it.dateStr }

        _statsState.value = _statsState.value.copy(
            isLoading = false,
            isRefreshing = false,
            personalStats = PersonalStatsData(
                present = present,
                absent = absent,
                halfDays = halfDays,
                overtime = overtime,
                advanceTotal = advanceTotal,
                totalEarnings = totalEarnings,
                dailyRecords = sortedDailyRecords,
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
        summaryListener?.remove()
    }
}
