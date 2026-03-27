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
import java.util.Calendar
import java.util.Date
import java.util.Locale

typealias Worker = WorkerEntity

data class AttendanceRecord(
    val id: String,
    val userId: String,
    val date: String,
    val status: String,
    val type: String?,
    val reason: String?,
    val overtimeHours: Int?,
    val note: String?,
    val advanceAmount: Double?
)

data class CalendarState(
    val role: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,

    // Contractor State
    val selectedDate: String = "", // YYYY-MM-DD
    val workers: List<Worker> = emptyList(),
    val contractorAttendance: List<AttendanceRecord> = emptyList(),

    // Personal State
    val currentMonthDate: Date = Date(), // Determines the month/year displayed
    val personalAttendance: List<AttendanceRecord> = emptyList()
)

class CalendarViewModel(
    private val repository: UserPreferencesRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _calendarState = MutableStateFlow(CalendarState())
    val calendarState: StateFlow<CalendarState> = _calendarState

    private var workersJob: Job? = null
    private var attendanceJob: Job? = null

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        _calendarState.value = _calendarState.value.copy(
            selectedDate = sdf.format(Date())
        )

        viewModelScope.launch {
            repository.userRoleFlow.collectLatest { role ->
                if (role != null) {
                    _calendarState.value = _calendarState.value.copy(role = role)
                    setupListeners(role)
                }
            }
        }
    }

    fun refresh() {
        _calendarState.value = _calendarState.value.copy(isRefreshing = true)
        setupListeners(_calendarState.value.role)
    }

    private fun setupListeners(role: String) {
        val user = auth.currentUser ?: return

        workersJob?.cancel()
        attendanceJob?.cancel()

        _calendarState.value = _calendarState.value.copy(isLoading = true)

        if (role == "contractor") {
            workersJob = viewModelScope.launch {
                syncRepository.getWorkersFlow(user.uid).collectLatest { workers ->
                    _calendarState.value = _calendarState.value.copy(workers = workers, isRefreshing = false)
                    updateContractorAttendanceListener()
                }
            }
            viewModelScope.launch { syncRepository.syncWorkers(user.uid) }
        } else {
            updatePersonalAttendanceListener()
        }
    }

    private fun updateContractorAttendanceListener() {
        val user = auth.currentUser ?: return
        val date = _calendarState.value.selectedDate
        val monthId = date.substring(0, 7)

        attendanceJob?.cancel()

        attendanceJob = viewModelScope.launch {
            syncRepository.getContractorAttendanceFlow(user.uid, monthId).collectLatest { attendance ->
                // Filter client side for specific date (Room returns whole month)
                val dailyAttendance = attendance.filter { it.date == date }.map { doc ->
                    AttendanceRecord(
                        id = doc.id,
                        userId = doc.userId,
                        date = doc.date,
                        status = doc.status,
                        type = doc.type,
                        reason = doc.reason,
                        overtimeHours = doc.overtimeHours,
                        note = doc.note,
                        advanceAmount = doc.advanceAmount
                    )
                }

                _calendarState.value = _calendarState.value.copy(
                    contractorAttendance = dailyAttendance,
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
        viewModelScope.launch { syncRepository.syncAttendance(user.uid, monthId, true) }
    }

    private fun updatePersonalAttendanceListener() {
        val user = auth.currentUser ?: return
        attendanceJob?.cancel()

        val currentMonthDate = _calendarState.value.currentMonthDate
        val cal = Calendar.getInstance()
        cal.time = currentMonthDate

        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthId = sdfMonth.format(cal.time)

        attendanceJob = viewModelScope.launch {
            syncRepository.getPersonalAttendanceFlow(user.uid, monthId).collectLatest { attendance ->
                val monthlyAttendance = attendance.map { doc ->
                    AttendanceRecord(
                        id = doc.id,
                        userId = doc.userId,
                        date = doc.date,
                        status = doc.status,
                        type = doc.type,
                        reason = doc.reason,
                        overtimeHours = doc.overtimeHours,
                        note = doc.note,
                        advanceAmount = doc.advanceAmount
                    )
                }

                _calendarState.value = _calendarState.value.copy(
                    personalAttendance = monthlyAttendance,
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
        viewModelScope.launch { syncRepository.syncAttendance(user.uid, monthId, false) }
    }

    // --- Contractor Actions ---

    fun changeContractorDate(offsetDays: Int) {
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(_calendarState.value.selectedDate) ?: Date()
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        _calendarState.value = _calendarState.value.copy(
            selectedDate = sdf.format(cal.time),
            isLoading = true
        )
        updateContractorAttendanceListener()
    }

    fun markContractorAttendance(workerId: String, status: String, type: String) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val date = _calendarState.value.selectedDate
            val monthId = date.substring(0, 7)
            val userId = "worker_$workerId"

            // Find existing id if not advance
            val existing = _calendarState.value.contractorAttendance.find {
                it.userId == userId && it.status != "advance"
            }

            val docId = existing?.id ?: "${userId}_$date"

            val record = AttendanceEntity(
                id = docId,
                userId = userId,
                contractorId = user.uid,
                date = date,
                monthId = monthId,
                status = status,
                type = if (status == "present") type else null,
                reason = null,
                overtimeHours = null,
                note = null,
                advanceAmount = null,
                timestamp = System.currentTimeMillis()
            )

            syncRepository.markAttendanceOptimistically(record)
        }
    }

    // --- Personal Actions ---

    fun changePersonalMonth(offsetMonths: Int) {
        val cal = Calendar.getInstance()
        cal.time = _calendarState.value.currentMonthDate
        cal.add(Calendar.MONTH, offsetMonths)
        _calendarState.value = _calendarState.value.copy(
            currentMonthDate = cal.time,
            isLoading = true
        )
        updatePersonalAttendanceListener()
    }

    fun markPersonalAttendance(
        date: String,
        status: String,
        type: String,
        reason: String,
        overtimeHours: Int,
        note: String,
        advanceAmount: Double
    ) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val monthId = date.substring(0, 7)

            val docId = "${user.uid}_$date"

            if (status == "present" || status == "absent") {
                val record = AttendanceEntity(
                    id = docId,
                    userId = user.uid,
                    contractorId = null,
                    date = date,
                    monthId = monthId,
                    status = status,
                    type = if (status == "present") type else null,
                    reason = if (status == "absent") reason else null,
                    overtimeHours = if (status == "present") overtimeHours else 0,
                    note = if (note.isNotEmpty()) note else null,
                    advanceAmount = null,
                    timestamp = System.currentTimeMillis()
                )
                syncRepository.markAttendanceOptimistically(record)
            }

            val advanceDocId = "${user.uid}_${date}_advance"
            if (advanceAmount > 0) {
                val advanceRecord = AttendanceEntity(
                    id = advanceDocId,
                    userId = user.uid,
                    contractorId = null,
                    date = date,
                    monthId = monthId,
                    status = "advance",
                    type = null,
                    reason = null,
                    overtimeHours = null,
                    note = null,
                    advanceAmount = advanceAmount,
                    timestamp = System.currentTimeMillis()
                )
                syncRepository.markAttendanceOptimistically(advanceRecord)
            } else {
                syncRepository.deleteAttendanceOptimistically(user.uid, monthId, advanceDocId)
            }
        }
    }

    fun removePersonalAttendance(date: String) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val monthId = date.substring(0, 7)
            val docId = "${user.uid}_$date"
            val advanceDocId = "${user.uid}_${date}_advance"

            syncRepository.deleteAttendanceOptimistically(user.uid, monthId, docId)
            syncRepository.deleteAttendanceOptimistically(user.uid, monthId, advanceDocId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        workersJob?.cancel()
        attendanceJob?.cancel()
    }
}
