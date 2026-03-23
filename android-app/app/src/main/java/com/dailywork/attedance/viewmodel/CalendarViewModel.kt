package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Worker(
    val id: String,
    val name: String,
    val workType: String,
    val wage: Double
)

data class AttendanceRecord(
    val id: String,
    val userId: String,
    val date: String,
    val status: String,
    val type: String?,
    val reason: String?,
    val overtimeHours: Int?,
    val note: String?
)

data class CalendarState(
    val role: String = "",
    val isLoading: Boolean = true,

    // Contractor State
    val selectedDate: String = "", // YYYY-MM-DD
    val workers: List<Worker> = emptyList(),
    val contractorAttendance: List<AttendanceRecord> = emptyList(),

    // Personal State
    val currentMonthDate: Date = Date(), // Determines the month/year displayed
    val personalAttendance: List<AttendanceRecord> = emptyList()
)

class CalendarViewModel(private val repository: UserPreferencesRepository) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _calendarState = MutableStateFlow(CalendarState())
    val calendarState: StateFlow<CalendarState> = _calendarState

    private var workersListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var attendanceListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        _calendarState.value = _calendarState.value.copy(
            selectedDate = sdf.format(Date())
        )

        viewModelScope.launch {
            repository.userRoleFlow.collect { role ->
                if (role != null) {
                    _calendarState.value = _calendarState.value.copy(role = role)
                    setupListeners(role)
                }
            }
        }
    }

    private fun setupListeners(role: String) {
        val user = auth.currentUser ?: return

        workersListener?.remove()
        attendanceListener?.remove()

        _calendarState.value = _calendarState.value.copy(isLoading = true)

        if (role == "contractor") {
            workersListener = db.collection("workers")
                .whereEqualTo("contractorId", user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val workersList = snapshot.documents.mapNotNull { doc ->
                        Worker(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            workType = doc.getString("workType") ?: "Labour",
                            wage = doc.getDouble("wage") ?: 0.0
                        )
                    }
                    _calendarState.value = _calendarState.value.copy(workers = workersList)
                    updateContractorAttendanceListener()
                }
        } else {
            updatePersonalAttendanceListener()
        }
    }

    private fun updateContractorAttendanceListener() {
        val user = auth.currentUser ?: return
        val date = _calendarState.value.selectedDate

        attendanceListener?.remove()

        attendanceListener = db.collection("attendance")
            .whereEqualTo("contractorId", user.uid)
            .whereEqualTo("date", date)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val attendanceList = snapshot.documents.map { doc ->
                    AttendanceRecord(
                        id = doc.id,
                        userId = doc.getString("user_id") ?: "",
                        date = doc.getString("date") ?: "",
                        status = doc.getString("status") ?: "",
                        type = doc.getString("type"),
                        reason = doc.getString("reason"),
                        overtimeHours = doc.getDouble("overtime_hours")?.toInt(),
                        note = doc.getString("note")
                    )
                }
                _calendarState.value = _calendarState.value.copy(
                    contractorAttendance = attendanceList,
                    isLoading = false
                )
            }
    }

    private fun updatePersonalAttendanceListener() {
        val user = auth.currentUser ?: return

        attendanceListener?.remove()

        val currentMonthDate = _calendarState.value.currentMonthDate
        val cal = Calendar.getInstance()
        cal.time = currentMonthDate

        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthPrefix = sdfMonth.format(cal.time)
        val startDate = "$monthPrefix-01"
        val endDate = "$monthPrefix-31"

        attendanceListener = db.collection("attendance")
            .whereEqualTo("user_id", user.uid)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val monthlyAttendance = snapshot.documents.map { doc ->
                    AttendanceRecord(
                        id = doc.id,
                        userId = doc.getString("user_id") ?: "",
                        date = doc.getString("date") ?: "",
                        status = doc.getString("status") ?: "",
                        type = doc.getString("type"),
                        reason = doc.getString("reason"),
                        overtimeHours = doc.getDouble("overtime_hours")?.toInt(),
                        note = doc.getString("note")
                    )
                }

                _calendarState.value = _calendarState.value.copy(
                    personalAttendance = monthlyAttendance,
                    isLoading = false
                )
            }
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
            val userId = "worker_$workerId"

            // Find existing id if not advance
            val existing = _calendarState.value.contractorAttendance.find {
                it.userId == userId && it.status != "advance"
            }

            val docId = existing?.id ?: "${userId}_$date"

            val newData = hashMapOf(
                "user_id" to userId,
                "contractorId" to user.uid,
                "date" to date,
                "status" to status,
                "type" to if (status == "present") type else null,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            // Optimistic UI
            val optimisticRecord = AttendanceRecord(
                id = docId,
                userId = userId,
                date = date,
                status = status,
                type = if (status == "present") type else null,
                reason = null,
                overtimeHours = null,
                note = null
            )
            val newList = _calendarState.value.contractorAttendance.filter { it.id != docId } + optimisticRecord
            _calendarState.value = _calendarState.value.copy(contractorAttendance = newList)

            try {
                db.collection("attendance").document(docId).set(newData, SetOptions.merge()).await()
            } catch (e: Exception) {
                // Handle error
            }
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


    override fun onCleared() {
        super.onCleared()
        workersListener?.remove()
        attendanceListener?.remove()
    }
}
