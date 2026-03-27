package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.dailywork.attedance.data.AttendanceEntity
import com.dailywork.attedance.data.SyncRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class WorkerDetailState(
    val workerId: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedMonthDate: Date = Date(),
    val name: String = "",
    val workType: String = "Labour",
    val dailyWage: Double = 500.0,
    val joinedDate: String = "",

    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val halfDays: Int = 0,
    val totalDailyWorks: Double = 0.0,
    val passedDays: Int = 0,
    val attendanceRate: Int = 0,

    val grossEarned: Double = 0.0,
    val totalAdvance: Double = 0.0,
    val finalBalance: Double = 0.0,

    val isPremium: Boolean = false,
    val contractorName: String = "Admin"
)

class WorkerDetailViewModel(private val syncRepository: SyncRepository) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow(WorkerDetailState())
    val state: StateFlow<WorkerDetailState> = _state

    var logsFlow: Flow<PagingData<PassbookLog>> = emptyFlow()
        private set

    private var workerId: String? = null
    private var workerJob: Job? = null
    private var userJob: Job? = null
    private var attendanceJob: Job? = null

    fun initialize(id: String) {
        if (workerId == id) return
        workerId = id
        setupListeners()
        setupUserListener()
    }

    private fun setupUserListener() {
        val user = auth.currentUser ?: return
        userJob?.cancel()
        userJob = viewModelScope.launch {
            syncRepository.getUserFlow(user.uid).collectLatest { userEntity ->
                if (userEntity != null) {
                    _state.value = _state.value.copy(
                        isPremium = userEntity.isPremium,
                        contractorName = userEntity.name.ifEmpty { "Admin" }
                    )
                }
            }
        }
        viewModelScope.launch { syncRepository.syncUser(user.uid) }
    }

    fun refresh() {
        _state.value = _state.value.copy(isRefreshing = true)
        setupListeners()
        setupUserListener()
    }

    fun changeMonth(offset: Int) {
        val cal = Calendar.getInstance()
        cal.time = _state.value.selectedMonthDate
        cal.add(Calendar.MONTH, offset)
        _state.value = _state.value.copy(
            selectedMonthDate = cal.time,
            isLoading = true
        )
        setupListeners()
    }

    private fun setupListeners() {
        val user = auth.currentUser ?: return
        val wId = workerId ?: return
        val workerUserId = "worker_$wId"

        workerJob?.cancel()
        attendanceJob?.cancel()

        workerJob = viewModelScope.launch {
            syncRepository.getWorkersFlow(user.uid).collectLatest { workers ->
                val workerEntity = workers.find { it.id == wId }
                if (workerEntity != null) {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    _state.value = _state.value.copy(
                        name = workerEntity.name.ifEmpty { "Worker" },
                        workType = workerEntity.workType.ifEmpty { "Labour" },
                        dailyWage = workerEntity.wage,
                        joinedDate = sdf.format(Date(workerEntity.timestamp))
                    )
                }
            }
        }

        val cal = Calendar.getInstance()
        cal.time = _state.value.selectedMonthDate
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val yearMonth = sdfMonth.format(cal.time)

        // Setup Paging Flow
        logsFlow = Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { syncRepository.getWorkerAttendancePagingSource(user.uid, workerUserId, yearMonth) }
        ).flow.map { pagingData: PagingData<AttendanceEntity> ->
            pagingData.map { entity: AttendanceEntity ->
                PassbookLog(
                    date = entity.date,
                    status = entity.status,
                    type = entity.type,
                    note = entity.note,
                    advanceAmount = entity.advanceAmount
                )
            }
        }.cachedIn(viewModelScope)

        attendanceJob = viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                syncRepository.getWorkerStatusCountFlow(user.uid, workerUserId, yearMonth, "present"),
                syncRepository.getWorkerStatusCountFlow(user.uid, workerUserId, yearMonth, "present", "half"),
                syncRepository.getWorkerStatusCountFlow(user.uid, workerUserId, yearMonth, "absent"),
                syncRepository.getWorkerAdvanceSumFlow(user.uid, workerUserId, yearMonth)
            ) { presentTotal, halfDays, absent, advanceTotal ->

                val today = Calendar.getInstance()
                val isCurrentMonth = today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && today.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
                val passedDays = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else cal.getActualMaximum(Calendar.DAY_OF_MONTH)

                val effectiveDays = presentTotal - (halfDays * 0.5)
                val wage = _state.value.dailyWage
                val grossEarned = effectiveDays * wage
                val totalAdv = advanceTotal ?: 0.0
                val finalBalance = grossEarned - totalAdv

                val attRate = if (passedDays > 0) ((presentTotal.toDouble() / passedDays) * 100).toInt().coerceIn(0, 100) else 0

                _state.value = _state.value.copy(
                    isLoading = false,
                    presentDays = presentTotal,
                    absentDays = absent,
                    halfDays = halfDays,
                    totalDailyWorks = effectiveDays,
                    passedDays = passedDays,
                    attendanceRate = attRate,
                    grossEarned = grossEarned,
                    totalAdvance = totalAdv,
                    finalBalance = finalBalance,
                    isRefreshing = false
                )
            }.collectLatest { }
        }
        viewModelScope.launch { syncRepository.syncAttendance(user.uid, yearMonth, true) }
    }

    override fun onCleared() {
        super.onCleared()
        workerJob?.cancel()
        attendanceJob?.cancel()
        userJob?.cancel()
    }
}
