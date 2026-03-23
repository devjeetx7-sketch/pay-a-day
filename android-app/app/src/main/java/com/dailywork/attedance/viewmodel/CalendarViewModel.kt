package com.dailywork.attedance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.attedance.data.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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

data class WorkerWithAttendance(
    val id: String,
    val name: String,
    val workType: String,
    val wage: Double,
    val attendanceId: String?,
    val status: String?,
    val type: String?
)

data class DayData(
    val status: String,
    val type: String? = null,
    val reason: String? = null,
    val overtime_hours: Int = 0,
    val note: String? = null,
    val advance_amount: Int = 0
)

data class CalendarState(
    val role: String = "",
    val isLoading: Boolean = false,

    // Contractor State
    val selectedDateStr: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val contractorWorkers: List<WorkerWithAttendance> = emptyList(),

    // Personal State
    val currentMonthDate: Date = Date(), // Represents the month currently being viewed
    val personalDayMap: Map<Int, DayData> = emptyMap()
)

class CalendarViewModel(private val repository: UserPreferencesRepository) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        viewModelScope.launch {
            repository.userRoleFlow.collect { role ->
                if (role != null) {
                    _state.value = _state.value.copy(role = role)
                    loadData(role)
                }
            }
        }
    }

    fun loadData(role: String) {
        if (role == "contractor") {
            loadContractorData(_state.value.selectedDateStr)
        } else {
            loadPersonalData(_state.value.currentMonthDate)
        }
    }

    // --- Contractor Methods ---

    fun changeContractorDate(dateStr: String) {
        _state.value = _state.value.copy(selectedDateStr = dateStr)
        loadContractorData(dateStr)
    }

    private fun loadContractorData(dateStr: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val user = auth.currentUser ?: return@launch
            try {
                // 1. Get Workers
                val wSnap = db.collection("workers")
                    .whereEqualTo("contractorId", user.uid)
                    .get().await()

                val workers = wSnap.documents.map {
                    WorkerWithAttendance(
                        id = it.id,
                        name = it.getString("name") ?: "",
                        workType = it.getString("workType") ?: "Labour",
                        wage = it.getDouble("wage") ?: 0.0,
                        attendanceId = null, status = null, type = null
                    )
                }

                // 2. Get Attendance
                val attSnap = db.collection("attendance")
                    .whereEqualTo("contractorId", user.uid)
                    .whereEqualTo("date", dateStr)
                    .get().await()

                val attMap = attSnap.documents.associateBy {
                    it.getString("user_id")?.removePrefix("worker_") ?: ""
                }

                val finalWorkers = workers.map { w ->
                    val att = attMap[w.id]
                    if (att != null && att.getString("status") != "advance") {
                        w.copy(
                            attendanceId = att.id,
                            status = att.getString("status"),
                            type = att.getString("type")
                        )
                    } else {
                        w
                    }
                }

                _state.value = _state.value.copy(contractorWorkers = finalWorkers, isLoading = false)

            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun markContractorAttendance(workerId: String, status: String, type: String = "full") {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val dateStr = _state.value.selectedDateStr
            val docId = "worker_${workerId}_${dateStr}"

            val newData = hashMapOf(
                "user_id" to "worker_$workerId",
                "contractorId" to user.uid,
                "date" to dateStr,
                "status" to status,
                "type" to type,
                "timestamp" to FieldValue.serverTimestamp()
            )

            try {
                // Optimistic UI
                val updatedWorkers = _state.value.contractorWorkers.map {
                    if (it.id == workerId) {
                        it.copy(attendanceId = docId, status = status, type = type)
                    } else it
                }
                _state.value = _state.value.copy(contractorWorkers = updatedWorkers)

                db.collection("attendance").document(docId).set(newData, SetOptions.merge()).await()
            } catch (e: Exception) {
                loadContractorData(dateStr)
            }
        }
    }

    // --- Personal Methods ---

    fun changePersonalMonth(delta: Int) {
        val cal = Calendar.getInstance()
        cal.time = _state.value.currentMonthDate
        cal.add(Calendar.MONTH, delta)
        _state.value = _state.value.copy(currentMonthDate = cal.time)
        loadPersonalData(cal.time)
    }

    private fun loadPersonalData(monthDate: Date) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val user = auth.currentUser ?: return@launch

            val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val monthStr = sdfMonth.format(monthDate)

            try {
                val snap = db.collection("attendance")
                    .whereEqualTo("user_id", user.uid)
                    .get().await()

                val dayMap = mutableMapOf<Int, DayData>()
                for (doc in snap.documents) {
                    val date = doc.getString("date") ?: continue
                    if (date.startsWith(monthStr)) {
                        val dayInt = date.substring(8, 10).toInt()

                        val status = doc.getString("status") ?: ""
                        val type = doc.getString("type")
                        val reason = doc.getString("reason")
                        val overtime = doc.getDouble("overtime_hours")?.toInt() ?: 0
                        val note = doc.getString("note")
                        val advance = doc.getDouble("advance_amount")?.toInt() ?: 0

                        val existing = dayMap[dayInt]
                        if (existing != null) {
                             if (status == "advance") {
                                 dayMap[dayInt] = existing.copy(advance_amount = existing.advance_amount + advance)
                             } else {
                                 dayMap[dayInt] = existing.copy(
                                     status = status, type = type, reason = reason,
                                     overtime_hours = overtime, note = note
                                 )
                             }
                        } else {
                            dayMap[dayInt] = DayData(status, type, reason, overtime, note, advance)
                        }
                    }
                }
                _state.value = _state.value.copy(personalDayMap = dayMap, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun savePersonalDay(dateStr: String, data: DayData, dailyWage: Double) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val docId = "${user.uid}_$dateStr"

            val updates = hashMapOf<String, Any>(
                "user_id" to user.uid,
                "date" to dateStr,
                "status" to data.status,
                "timestamp" to FieldValue.serverTimestamp()
            )

            if (data.status == "present") {
                updates["type"] = data.type ?: "full"
                updates["overtime_hours"] = data.overtime_hours
                updates["daily_wage"] = dailyWage
            } else if (data.status == "absent") {
                updates["reason"] = data.reason ?: "sick"
            }
            if (!data.note.isNullOrEmpty()) updates["note"] = data.note
            if (data.advance_amount > 0) updates["advance_amount"] = data.advance_amount

            try {
                db.collection("attendance").document(docId).set(updates, SetOptions.merge()).await()
                loadPersonalData(_state.value.currentMonthDate)
            } catch (e: Exception) {
            }
        }
    }

    fun removePersonalDay(dateStr: String) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val docId = "${user.uid}_$dateStr"
            try {
                db.collection("attendance").document(docId).delete().await()
                loadPersonalData(_state.value.currentMonthDate)
            } catch (e: Exception) {}
        }
    }
}
