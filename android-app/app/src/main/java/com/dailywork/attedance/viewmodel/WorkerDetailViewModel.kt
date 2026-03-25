package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WorkerDetailViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(PassbookState())
    val state: StateFlow<PassbookState> = _state

    private var workerId: String? = null
    private var workerListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var attendanceListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null

    private var cachedDocs: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()

    fun initialize(id: String) {
        if (workerId == id) return
        workerId = id
        setupListeners()
        setupUserListener()
    }

    private fun setupUserListener() {
        val user = auth.currentUser ?: return
        userListener?.remove()
        userListener = db.collection("users").document(user.uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
            _state.value = _state.value.copy(
                isPremium = snapshot.getBoolean("isPremium") ?: false
            )
        }
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

        workerListener?.remove()
        attendanceListener?.remove()

        workerListener = db.collection("workers").document(wId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val joinedDateLong = snapshot.getTimestamp("created_at")?.toDate()?.time ?: Date().time
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    _state.value = _state.value.copy(
                        name = snapshot.getString("name") ?: "Worker",
                        workType = snapshot.getString("workType") ?: "Labour",
                        dailyWage = snapshot.getDouble("wage") ?: 500.0,
                        joinedDate = sdf.format(Date(joinedDateLong))
                    )
                    calculatePassbook(cachedDocs)
                }
            }

        val cal = Calendar.getInstance()
        cal.time = _state.value.selectedMonthDate
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val yearMonth = sdfMonth.format(cal.time)

        attendanceListener = db.collection("attendance")
            .whereEqualTo("contractorId", user.uid)
            .whereEqualTo("user_id", "worker_$wId")
            .whereGreaterThanOrEqualTo("date", "$yearMonth-01")
            .whereLessThanOrEqualTo("date", "$yearMonth-31")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _state.value = _state.value.copy(isLoading = false, isRefreshing = false)
                    return@addSnapshotListener
                }
                cachedDocs = snapshot.documents
                calculatePassbook(snapshot.documents)
            }
    }

    private fun calculatePassbook(docs: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val cal = Calendar.getInstance()
        cal.time = _state.value.selectedMonthDate

        val today = Calendar.getInstance()
        val isCurrentMonth = today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && today.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
        val passedDays = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        var present = 0
        var absent = 0
        var half = 0
        var totalAdv = 0.0

        val logMap = mutableMapOf<String, PassbookLog>()

        docs.forEach { doc ->
            val date = doc.getString("date") ?: return@forEach
            val status = doc.getString("status") ?: ""
            val type = doc.getString("type") ?: "full"
            val adv = doc.getDouble("advance_amount") ?: 0.0
            val note = doc.getString("note")

            if (status == "present") {
                present++
                if (type == "half") half++
            } else if (status == "absent") {
                absent++
            }
            if (adv > 0) totalAdv += adv

            val existingLog = logMap[date]
            logMap[date] = PassbookLog(
                date = date,
                status = if (status != "advance") status else existingLog?.status ?: "advance",
                type = if (status != "advance") type else existingLog?.type,
                note = note ?: existingLog?.note,
                advanceAmount = (existingLog?.advanceAmount ?: 0.0) + adv
            )
        }

        val effectiveDays = present - (half * 0.5)
        val wage = _state.value.dailyWage
        val grossEarned = effectiveDays * wage
        val finalBalance = grossEarned - totalAdv

        val attRate = if (passedDays > 0) ((present.toDouble() / passedDays) * 100).toInt().coerceIn(0, 100) else 0

        val sortedLogs = logMap.values.toList().sortedByDescending { it.date }

        _state.value = _state.value.copy(
            isLoading = false,
            isRefreshing = false,
            presentDays = present,
            absentDays = absent,
            halfDays = half,
            totalDailyWorks = effectiveDays,
            passedDays = passedDays,
            attendanceRate = attRate,
            grossEarned = grossEarned,
            totalAdvance = totalAdv,
            finalBalance = finalBalance,
            logs = sortedLogs
        )
    }

    override fun onCleared() {
        super.onCleared()
        workerListener?.remove()
        attendanceListener?.remove()
        userListener?.remove()
    }
}
