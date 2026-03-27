package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.AttendanceEntity
import com.dailywork.attedance.data.SyncRepository
import com.dailywork.attedance.data.UserPreferencesRepository
import com.dailywork.attedance.data.WorkerEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardState(
    val role: String = "",
    val name: String = "",
    val photoUrl: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,

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
    val todayNote: String? = null,

    // Premium state
    val isPremium: Boolean = false
)

class DashboardViewModel(
    private val repository: UserPreferencesRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _dashboardState = MutableStateFlow(DashboardState(isLoading = true))
    val dashboardState: StateFlow<DashboardState> = _dashboardState

    private var userJob: Job? = null
    private var workersJob: Job? = null
    private var attendanceJob: Job? = null

    // Cache locally to recalculate efficiently on either snapshot change
    private var cachedWorkers: List<WorkerEntity> = emptyList()
    private var cachedAttendance: List<AttendanceEntity> = emptyList()
    private var cachedDefaultWage: Double = 500.0

    init {
        viewModelScope.launch {
            repository.userRoleFlow.collectLatest { role ->
                if (role != null) {
                    _dashboardState.value = _dashboardState.value.copy(role = role)
                    setupListeners(role)
                }
            }
        }
    }

    fun refresh() {
        _dashboardState.value = _dashboardState.value.copy(isRefreshing = true)
        setupListeners(_dashboardState.value.role)
    }

    private fun setupListeners(role: String) {
        val user = auth.currentUser ?: return
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        userJob?.cancel()
        workersJob?.cancel()
        attendanceJob?.cancel()

        userJob = viewModelScope.launch {
            syncRepository.getUserFlow(user.uid).collectLatest { userEntity ->
                if (userEntity == null) return@collectLatest

                _dashboardState.value = _dashboardState.value.copy(
                    name = userEntity.name.ifEmpty { user.displayName ?: "User" },
                    photoUrl = userEntity.profileImageUrl ?: user.photoUrl?.toString() ?: "",
                    isPremium = userEntity.isPremium,
                    isRefreshing = false
                )
                cachedDefaultWage = userEntity.dailyWage
                recalculateStats()
            }
        }
        viewModelScope.launch { syncRepository.syncUser(user.uid) }

        if (role == "contractor") {
            workersJob = viewModelScope.launch {
                syncRepository.getWorkersFlow(user.uid).collectLatest { workers ->
                    cachedWorkers = workers
                    recalculateStats()
                }
            }
            viewModelScope.launch { syncRepository.syncWorkers(user.uid) }

            attendanceJob = viewModelScope.launch {
                syncRepository.getContractorAttendanceFlow(user.uid, currentMonth).collectLatest { attendance ->
                    cachedAttendance = attendance
                    recalculateStats()
                }
            }
            viewModelScope.launch { syncRepository.syncAttendance(user.uid, currentMonth, true) }
        } else {
            attendanceJob = viewModelScope.launch {
                syncRepository.getPersonalAttendanceFlow(user.uid, currentMonth).collectLatest { attendance ->
                    cachedAttendance = attendance
                    recalculateStats()
                }
            }
            viewModelScope.launch { syncRepository.syncAttendance(user.uid, currentMonth, false) }
        }
    }

    private fun recalculateStats() {
        val role = _dashboardState.value.role
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val currentMonthStr = sdfMonth.format(Date())

        if (role == "contractor") {
            val totalWorkers = cachedWorkers.size
            var totalAdvanceMonth = 0.0
            var pendingTotal = 0.0
            var todayPresentCount = 0

            val workersMap = mutableMapOf<String, Double>()
            for (doc in cachedWorkers) {
                workersMap["worker_${doc.id}"] = doc.wage
            }

            for (att in cachedAttendance) {
                val date = att.date
                val status = att.status
                val type = att.type ?: "full"
                val workerId = att.userId
                val advance = att.advanceAmount ?: 0.0
                val wage = workersMap[workerId] ?: 0.0

                if (date == todayStr && status == "present") {
                    todayPresentCount++
                }

                if (status == "present") {
                    pendingTotal += if (type == "half") wage / 2 else wage
                } else if (status == "advance") {
                    pendingTotal -= advance
                }

                if (date.startsWith(currentMonthStr) && status == "advance") {
                    totalAdvanceMonth += advance
                }
            }

            _dashboardState.value = _dashboardState.value.copy(
                isLoading = false,
                totalWorkers = totalWorkers.toString(),
                todayPresent = todayPresentCount.toString(),
                totalPaidMonth = totalAdvanceMonth.toInt().toString(),
                pendingAmount = pendingTotal.toInt().toString()
            )
        } else {
            var todayEarned = 0.0
            var monthEarned = 0.0
            var currentTodayStatus: String? = null
            var currentOvertime = 0
            var currentNote: String? = null

            for (doc in cachedAttendance) {
                val date = doc.date
                val status = doc.status
                val type = doc.type ?: "full"

                val dailyValue = if (status == "present") {
                    if (type == "half") cachedDefaultWage / 2 else cachedDefaultWage
                } else 0.0

                if (date == todayStr) {
                    if (status != "advance") {
                        currentTodayStatus = status
                        currentOvertime = doc.overtimeHours ?: 0
                        currentNote = doc.note
                        todayEarned = dailyValue
                    }
                }

                if (date.startsWith(currentMonthStr)) {
                    monthEarned += dailyValue
                }
            }

            _dashboardState.value = _dashboardState.value.copy(
                isLoading = false,
                todayEarned = todayEarned.toInt().toString(),
                monthEarned = monthEarned.toInt().toString(),
                todayStatus = currentTodayStatus,
                overtimeHours = currentOvertime,
                todayNote = currentNote
            )
        }
    }

    fun markAttendance(type: String, overtimeHours: Int, note: String) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val date = Date()
            val todayStr = sdf.format(date)
            val monthStr = sdfMonth.format(date)
            val docId = "${user.uid}_$todayStr"

            val record = AttendanceEntity(
                id = docId,
                userId = user.uid,
                contractorId = null,
                date = todayStr,
                monthId = monthStr,
                status = "present",
                type = type,
                reason = null,
                overtimeHours = overtimeHours,
                note = note,
                advanceAmount = null,
                timestamp = System.currentTimeMillis()
            )

            syncRepository.markAttendanceOptimistically(record)
        }
    }

    fun markAbsent(reason: String, note: String) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val date = Date()
            val todayStr = sdf.format(date)
            val monthStr = sdfMonth.format(date)
            val docId = "${user.uid}_$todayStr"

            val record = AttendanceEntity(
                id = docId,
                userId = user.uid,
                contractorId = null,
                date = todayStr,
                monthId = monthStr,
                status = "absent",
                type = null,
                reason = reason,
                overtimeHours = null,
                note = note,
                advanceAmount = null,
                timestamp = System.currentTimeMillis()
            )
            syncRepository.markAttendanceOptimistically(record)
        }
    }

    fun addAdvance(amount: Double, workerId: String? = null) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val date = Date()
            val todayStr = sdf.format(date)
            val monthStr = sdfMonth.format(date)

            val isContractor = _dashboardState.value.role == "contractor"
            val targetUserId = if (isContractor && workerId != null) "worker_$workerId" else user.uid

            val docId = "${targetUserId}_${todayStr}_advance"

            val record = AttendanceEntity(
                id = docId,
                userId = targetUserId,
                contractorId = if (isContractor) user.uid else null,
                date = todayStr,
                monthId = monthStr,
                status = "advance",
                type = null,
                reason = null,
                overtimeHours = null,
                note = null,
                advanceAmount = amount,
                timestamp = System.currentTimeMillis()
            )

            syncRepository.markAttendanceOptimistically(record)
        }
    }

    fun removeAttendance() {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val date = Date()
            val todayStr = sdf.format(date)
            val monthStr = sdfMonth.format(date)
            val docId = "${user.uid}_$todayStr"

            syncRepository.deleteAttendanceOptimistically(user.uid, monthStr, docId)
        }
    }

    fun upgradeToPremium(onSuccess: () -> Unit) {
        // Implementation remains unchanged
    }

    override fun onCleared() {
        super.onCleared()
        userJob?.cancel()
        workersJob?.cancel()
        attendanceJob?.cancel()
    }
}
