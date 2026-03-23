package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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

    private val _dashboardState = MutableStateFlow(DashboardState(isLoading = true))
    val dashboardState: StateFlow<DashboardState> = _dashboardState

    private var userListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var workersListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var attendanceListener: com.google.firebase.firestore.ListenerRegistration? = null

    // Cache locally to recalculate efficiently on either snapshot change
    private var cachedWorkers: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()
    private var cachedAttendance: List<com.google.firebase.firestore.DocumentSnapshot> = emptyList()
    private var cachedDefaultWage: Double = 500.0

    init {
        viewModelScope.launch {
            repository.userRoleFlow.collect { role ->
                if (role != null) {
                    _dashboardState.value = _dashboardState.value.copy(role = role)
                    setupListeners(role)
                }
            }
        }
    }

    private fun setupListeners(role: String) {
        val user = auth.currentUser ?: return

        userListener?.remove()
        workersListener?.remove()
        attendanceListener?.remove()

        userListener = db.collection("users").document(user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val name = snapshot.getString("name") ?: user.displayName ?: "User"
                val photoUrl = user.photoUrl?.toString() ?: ""
                cachedDefaultWage = snapshot.getDouble("daily_wage") ?: 500.0

                _dashboardState.value = _dashboardState.value.copy(
                    name = name,
                    photoUrl = photoUrl
                )

                recalculateStats()
            }

        if (role == "contractor") {
            workersListener = db.collection("workers")
                .whereEqualTo("contractorId", user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    cachedWorkers = snapshot.documents
                    recalculateStats()
                }

            attendanceListener = db.collection("attendance")
                .whereEqualTo("contractorId", user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    cachedAttendance = snapshot.documents
                    recalculateStats()
                }
        } else {
            attendanceListener = db.collection("attendance")
                .whereEqualTo("user_id", user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    cachedAttendance = snapshot.documents
                    recalculateStats()
                }
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
                workersMap["worker_${doc.id}"] = doc.getDouble("wage") ?: 0.0
            }

            for (att in cachedAttendance) {
                val date = att.getString("date") ?: ""
                val status = att.getString("status") ?: ""
                val type = att.getString("type") ?: "full"
                val workerId = att.getString("user_id") ?: ""
                val advance = att.getDouble("advance_amount") ?: 0.0
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
                val date = doc.getString("date") ?: ""
                val status = doc.getString("status") ?: ""
                val type = doc.getString("type") ?: "full"

                val dailyValue = if (status == "present") {
                    if (type == "half") cachedDefaultWage / 2 else cachedDefaultWage
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
            val todayStr = sdf.format(Date())
            val docId = "${user.uid}_$todayStr"

            _dashboardState.value = _dashboardState.value.copy(
                todayStatus = "present",
                overtimeHours = overtimeHours,
                todayNote = note
            )

            val attendanceData = hashMapOf(
                "user_id" to user.uid,
                "date" to todayStr,
                "status" to "present",
                "type" to type,
                "overtime_hours" to overtimeHours,
                "note" to note,
                "daily_wage" to cachedDefaultWage,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            try {
                db.collection("attendance").document(docId).set(attendanceData, SetOptions.merge()).await()
            } catch (e: Exception) {
            }
        }
    }

    fun markAbsent(reason: String, note: String) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val docId = "${user.uid}_$todayStr"

            _dashboardState.value = _dashboardState.value.copy(
                todayStatus = "absent",
                overtimeHours = 0,
                todayNote = note
            )

            val attendanceData = hashMapOf(
                "user_id" to user.uid,
                "date" to todayStr,
                "status" to "absent",
                "reason" to reason,
                "note" to note,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            try {
                db.collection("attendance").document(docId).set(attendanceData, SetOptions.merge()).await()
            } catch (e: Exception) {
            }
        }
    }

    fun addAdvance(amount: Double, workerId: String? = null) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())

            val isContractor = _dashboardState.value.role == "contractor"
            val targetUserId = if (isContractor && workerId != null) "worker_$workerId" else user.uid

            val docId = "${targetUserId}_${todayStr}_advance"

            val advanceData = hashMapOf(
                "user_id" to targetUserId,
                "date" to todayStr,
                "status" to "advance",
                "advance_amount" to amount,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            if (isContractor) {
                advanceData["contractorId"] = user.uid
            }

            try {
                db.collection("attendance").document(docId).set(advanceData, SetOptions.merge()).await()
            } catch (e: Exception) {
            }
        }
    }

    fun removeAttendance() {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            val docId = "${user.uid}_$todayStr"

            _dashboardState.value = _dashboardState.value.copy(
                todayStatus = null,
                overtimeHours = 0,
                todayNote = null
            )

            try {
                db.collection("attendance").document(docId).delete().await()
            } catch (e: Exception) {
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        workersListener?.remove()
        attendanceListener?.remove()
    }
}
