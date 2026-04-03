package com.dailywork.attedance.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

class FirestoreRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val auth = FirebaseAuth.getInstance()
    private val userId: String? get() = auth.currentUser?.uid

    // --- Path Helpers ---

    private fun userDoc(): DocumentReference? = userId?.let { db.collection("users").document(it) }

    fun workersCollection(): CollectionReference? = userDoc()?.collection("workers")

    fun workerAttendanceCollection(workerId: String): CollectionReference? =
        workersCollection()?.document(workerId)?.collection("attendance")

    fun personalAttendanceCollection(): CollectionReference? = userDoc()?.collection("personal_attendance")

    fun workTypesCollection(): CollectionReference? = userDoc()?.collection("workTypes")

    fun summariesCollection(): CollectionReference? = userDoc()?.collection("summaries")

    // --- Worker Operations ---

    suspend fun saveWorker(workerId: String?, data: Map<String, Any>) {
        val workers = workersCollection() ?: return
        if (workerId.isNullOrEmpty()) {
            workers.add(data).await()
        } else {
            workers.document(workerId).set(data, SetOptions.merge()).await()
        }
    }

    suspend fun deleteWorker(workerId: String) {
        val workerDoc = workersCollection()?.document(workerId) ?: return

        // Cascade delete attendance
        val attendance = workerAttendanceCollection(workerId)?.get()?.await()
        attendance?.documents?.forEach { doc ->
            doc.reference.delete().await()
        }

        workerDoc.delete().await()
    }

    // --- Attendance Operations ---

    suspend fun markWorkerAttendance(workerId: String, attendanceId: String, data: Map<String, Any>) {
        val collection = workerAttendanceCollection(workerId) ?: return
        val docRef = collection.document(attendanceId)
        val oldDoc = docRef.get().await()

        val date = data["date"] as String
        val oldData = if (oldDoc.exists()) oldDoc.data else null

        docRef.set(data, SetOptions.merge()).await()
        updateSummaryWithDifference(date, oldData, data, workerId)
    }

    suspend fun markPersonalAttendance(attendanceId: String, data: Map<String, Any>) {
        val collection = personalAttendanceCollection() ?: return
        val docRef = collection.document(attendanceId)
        val oldDoc = docRef.get().await()

        val date = data["date"] as String
        val oldData = if (oldDoc.exists()) oldDoc.data else null

        docRef.set(data, SetOptions.merge()).await()
        updateSummaryWithDifference(date, oldData, data, null)
    }

    suspend fun deletePersonalAttendance(attendanceId: String, date: String) {
        val docRef = personalAttendanceCollection()?.document(attendanceId) ?: return
        val oldDoc = docRef.get().await()
        if (oldDoc.exists()) {
            val oldData = oldDoc.data
            docRef.delete().await()
            updateSummaryWithDifference(date, oldData, null, null)
        }
    }

    // --- Summary Optimization ---

    private suspend fun updateSummaryWithDifference(date: String, oldData: Map<String, Any>?, newData: Map<String, Any>?, workerId: String?) {
        val monthId = date.substring(0, 7) // YYYY-MM
        val summaryDoc = summariesCollection()?.document(monthId) ?: return

        val updates = mutableMapOf<String, Any>()

        // Helper to get values
        fun getStats(data: Map<String, Any>?): Triple<Double, Double, Double> {
            if (data == null) return Triple(0.0, 0.0, 0.0)
            val status = data["status"] as? String
            val type = data["type"] as? String
            val advance = (data["advance_amount"] as? Number)?.toDouble() ?: 0.0
            val wage = (data["wage"] as? Number)?.toDouble() ?: (data["daily_wage"] as? Number)?.toDouble() ?: 500.0

            var dayVal = 0.0
            var costVal = 0.0
            if (status == "present") {
                dayVal = if (type == "half") 0.5 else 1.0
                costVal = if (type == "half") wage / 2 else wage
            }
            return Triple(dayVal, costVal, advance)
        }

        val (oldDay, oldCost, oldAdv) = getStats(oldData)
        val (newDay, newCost, newAdv) = getStats(newData)

        val diffDay = newDay - oldDay
        val diffCost = newCost - oldCost
        val diffAdv = newAdv - oldAdv

        if (diffDay != 0.0) updates["total_present"] = FieldValue.increment(diffDay)
        if (diffCost != 0.0) updates["total_wages"] = FieldValue.increment(diffCost)
        if (diffAdv != 0.0) updates["total_advance"] = FieldValue.increment(diffAdv)

        // Per-worker stats in monthly summary
        if (workerId != null && (diffDay != 0.0 || diffCost != 0.0)) {
            updates["workers.$workerId.days"] = FieldValue.increment(diffDay)
            updates["workers.$workerId.cost"] = FieldValue.increment(diffCost)
        }

        // Today's stats
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date())
        if (date == today) {
            if (diffDay != 0.0) updates["today.present_count"] = FieldValue.increment(if (newDay > 0) 1.0 else -1.0)
        }

        if (updates.isNotEmpty()) {
            summaryDoc.set(updates, SetOptions.merge()).await()
        }
    }
}
