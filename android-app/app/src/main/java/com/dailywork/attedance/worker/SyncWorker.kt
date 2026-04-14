package com.dailywork.attedance.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dailywork.attedance.data.local.AppDatabase
import com.dailywork.attedance.data.FirestoreRepository
import org.json.JSONObject
import java.util.UUID

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getInstance(applicationContext)
        val syncDao = database.syncActionDao()
        val firestoreRepository = FirestoreRepository()

        val unsyncedActions = syncDao.getUnsyncedActions()

        if (unsyncedActions.isEmpty()) {
            return Result.success()
        }

        var allSuccess = true

        for (action in unsyncedActions) {
            try {
                val json = JSONObject(action.payload)
                when (action.actionType) {
                    "saveWorker" -> {
                        val workerId = if (json.has("workerId") && !json.isNull("workerId")) json.getString("workerId") else null
                        val dataObj = json.getJSONObject("data")
                        val map = mutableMapOf<String, Any>()
                        val keys = dataObj.keys()
                        while(keys.hasNext()){
                            val key = keys.next()
                            map[key] = dataObj.get(key)
                        }
                        firestoreRepository.saveWorker(workerId, map)
                    }
                    "deleteWorker" -> {
                        val workerId = json.getString("workerId")
                        firestoreRepository.deleteWorker(workerId)
                    }
                    "markWorkerAttendance" -> {
                        val workerId = json.getString("workerId")
                        val attendanceId = json.getString("attendanceId")
                        val dataObj = json.getJSONObject("data")
                        val map = mutableMapOf<String, Any?>()
                        val keys = dataObj.keys()
                        while(keys.hasNext()){
                            val key = keys.next()
                            map[key] = dataObj.get(key)
                        }
                        firestoreRepository.markWorkerAttendance(workerId, attendanceId, map)
                    }
                    "markPersonalAttendance" -> {
                        val attendanceId = json.getString("attendanceId")
                        val dataObj = json.getJSONObject("data")
                        val map = mutableMapOf<String, Any?>()
                        val keys = dataObj.keys()
                        while(keys.hasNext()){
                            val key = keys.next()
                            map[key] = dataObj.get(key)
                        }
                        firestoreRepository.markPersonalAttendance(attendanceId, map)
                    }
                    "deletePersonalAttendance" -> {
                        val attendanceId = json.getString("attendanceId")
                        val date = json.getString("date")
                        firestoreRepository.deletePersonalAttendance(attendanceId, date)
                    }
                    "deleteWorkerAttendance" -> {
                        val workerId = json.getString("workerId")
                        val attendanceId = json.getString("attendanceId")
                        val date = json.getString("date")
                        firestoreRepository.deleteWorkerAttendance(workerId, attendanceId, date)
                    }
                }

                // Mark as synced if no exception
                val updatedAction = action.copy(isSynced = true)
                syncDao.update(updatedAction)
                // Or we can just delete it once done
                syncDao.deleteById(action.id)
            } catch (e: Exception) {
                e.printStackTrace()
                allSuccess = false
            }
        }

        return if (allSuccess) Result.success() else Result.retry()
    }
}
