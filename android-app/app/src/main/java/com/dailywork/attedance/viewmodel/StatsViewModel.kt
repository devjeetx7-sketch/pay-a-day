package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.AttendanceEntity
import com.dailywork.attedance.data.SyncRepository
import com.dailywork.attedance.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class MonthStat(
    val monthName: String,
    val year: String,
    val present: Int,
    val absent: Int,
    val half: Int,
    val date: Date,

    // Personal details
    val totalEarned: Double = 0.0,

    // Contractor details
    val totalPaid: Double = 0.0
)

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
    val isRefreshing: Boolean = false,
    val selectedMonthDate: Date = Date(),

    // Contractor
    val contractorStats: ContractorStatsData = ContractorStatsData(),

    // Personal
    val personalStats: PersonalStatsData = PersonalStatsData()
)

class StatsViewModel(
    private val repository: UserPreferencesRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _statsState = MutableStateFlow(StatsState())
    val statsState: StateFlow<StatsState> = _statsState

    private var workersJob: Job? = null
    private var attendanceJob: Job? = null
    private var userJob: Job? = null

    private var cachedDefaultWage: Double = 500.0
    private var cachedWorkers: List<com.dailywork.attedance.data.WorkerEntity> = emptyList()

    init {
        viewModelScope.launch {
            repository.userRoleFlow.collectLatest { role ->
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

    fun changeMonth(offsetMonths: Int) {
        val cal = Calendar.getInstance()
        cal.time = _statsState.value.selectedMonthDate
        cal.add(Calendar.MONTH, offsetMonths)
        _statsState.value = _statsState.value.copy(
            selectedMonthDate = cal.time,
            isLoading = true
        )
        setupListeners(_statsState.value.role)
    }

    private fun setupListeners(role: String) {
        val user = auth.currentUser ?: return

        userJob?.cancel()
        workersJob?.cancel()
        attendanceJob?.cancel()

        val cal = Calendar.getInstance()
        cal.time = _statsState.value.selectedMonthDate
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val yearMonth = sdfMonth.format(cal.time)

        userJob = viewModelScope.launch {
            syncRepository.getUserFlow(user.uid).collectLatest { userEntity ->
                if (userEntity != null) {
                    cachedDefaultWage = userEntity.dailyWage
                }
            }
        }
        viewModelScope.launch { syncRepository.syncUser(user.uid) }

        if (role == "contractor") {
            workersJob = viewModelScope.launch {
                syncRepository.getWorkersFlow(user.uid).collectLatest { workers ->
                    cachedWorkers = workers
                }
            }
            viewModelScope.launch { syncRepository.syncWorkers(user.uid) }

            attendanceJob = viewModelScope.launch {
                syncRepository.getContractorAttendanceFlow(user.uid, yearMonth).collectLatest { docs ->
                    calculateContractorStats(docs, yearMonth)
                }
            }
            viewModelScope.launch { syncRepository.syncAttendance(user.uid, yearMonth, true) }
        } else {
            // Using Room Aggregation Queries instead of looping lists
            observePersonalStatsAggregations(user.uid, yearMonth)
            viewModelScope.launch { syncRepository.syncAttendance(user.uid, yearMonth, false) }
        }
    }

    private fun calculateContractorStats(attendanceDocs: List<AttendanceEntity>, yearMonth: String) {
        var totalCost = 0.0
        var totalDays = 0.0
        val workerPerfMap = mutableMapOf<String, WorkerStats>()

        val workersMap = cachedWorkers.associateBy({ "worker_${it.id}" }, { it })

        attendanceDocs.forEach { doc ->
            val date = doc.date
            val status = doc.status
            if (date.startsWith(yearMonth) && status == "present") {
                val userId = doc.userId
                val workerDoc = workersMap[userId]

                if (workerDoc != null) {
                    val wage = workerDoc.wage
                    val type = doc.type ?: "full"

                    val dayVal = if (type == "half") 0.5 else 1.0
                    val costVal = if (type == "half") wage / 2 else wage

                    val currentStats = workerPerfMap[userId] ?: WorkerStats(workerDoc.name, 0.0, 0.0)
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
            isRefreshing = false,
            contractorStats = ContractorStatsData(
                totalCost = totalCost,
                totalDailyWorks = totalDays,
                topWorkers = topWorkers
            )
        )
    }

    private fun observePersonalStatsAggregations(userId: String, yearMonth: String) {
        attendanceJob?.cancel()
        attendanceJob = viewModelScope.launch {
            // Flow combine max is 5 without custom array handling, so nest combines
            kotlinx.coroutines.flow.combine(
                kotlinx.coroutines.flow.combine(
                    syncRepository.getPersonalStatusCountFlow(userId, yearMonth, "present"),
                    syncRepository.getPersonalStatusCountFlow(userId, yearMonth, "present", "half"),
                    syncRepository.getPersonalStatusCountFlow(userId, yearMonth, "absent"),
                    syncRepository.getPersonalAdvanceSumFlow(userId, yearMonth)
                ) { presentTotal, halfDays, absent, advanceTotal ->
                    listOf(presentTotal, halfDays, absent, advanceTotal)
                },
                kotlinx.coroutines.flow.combine(
                    syncRepository.getPersonalOvertimeSumFlow(userId, yearMonth),
                    syncRepository.getPersonalAllTimePresentFlow(userId),
                    syncRepository.getPersonalAllTimeHalfFlow(userId)
                ) { overtime, allTimePresent, allTimeHalf ->
                    listOf(overtime, allTimePresent, allTimeHalf)
                }
            ) { group1, group2 ->
                val presentTotal = group1[0] as Int
                val halfDays = group1[1] as Int
                val absent = group1[2] as Int
                val advanceTotal = group1[3] as Double?

                val overtime = group2[0] as Int?
                val allTimePresent = group2[1] as Int
                val allTimeHalf = group2[2] as Int

                // Calculate derived values natively without looping lists
                val fullDays = presentTotal - halfDays
                val monthEarnings = (fullDays * cachedDefaultWage) + (halfDays * (cachedDefaultWage / 2))

                val allTimeFull = allTimePresent - allTimeHalf
                val allTimeEarnings = (allTimeFull * cachedDefaultWage) + (allTimeHalf * (cachedDefaultWage / 2))

                PersonalStatsData(
                    present = presentTotal,
                    absent = absent,
                    halfDays = halfDays,
                    overtime = overtime ?: 0,
                    advanceTotal = advanceTotal ?: 0.0,
                    totalEarnings = monthEarnings,
                    allTimeDays = allTimePresent,
                    allTimeEarnings = allTimeEarnings
                )
            }.collectLatest { statsData ->
                _statsState.value = _statsState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    personalStats = statsData
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userJob?.cancel()
        workersJob?.cancel()
        attendanceJob?.cancel()
    }
}
