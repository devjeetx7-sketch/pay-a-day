package com.dailywork.attedance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource

@Dao
interface AttendanceDao {
    // For personal role
    @Query("SELECT * FROM attendance WHERE userId = :userId AND monthId = :monthId")
    fun getPersonalAttendanceFlow(userId: String, monthId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE userId = :userId AND monthId = :monthId ORDER BY timestamp DESC")
    fun getPersonalAttendancePagingSource(userId: String, monthId: String): PagingSource<Int, AttendanceEntity>

    // For contractor role
    @Query("SELECT * FROM attendance WHERE contractorId = :contractorId AND monthId = :monthId")
    fun getContractorAttendanceFlow(contractorId: String, monthId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE contractorId = :contractorId AND userId = :workerUserId AND monthId = :monthId ORDER BY timestamp DESC")
    fun getWorkerAttendancePagingSource(contractorId: String, workerUserId: String, monthId: String): PagingSource<Int, AttendanceEntity>

    // Aggregations
    @Query("SELECT COUNT(*) FROM attendance WHERE userId = :userId AND monthId = :monthId AND status = :status AND type = :type")
    fun getPersonalStatusCountFlow(userId: String, monthId: String, status: String, type: String? = null): Flow<Int>

    @Query("SELECT SUM(advanceAmount) FROM attendance WHERE userId = :userId AND monthId = :monthId AND status = 'advance'")
    fun getPersonalAdvanceSumFlow(userId: String, monthId: String): Flow<Double?>

    @Query("SELECT SUM(overtimeHours) FROM attendance WHERE userId = :userId AND monthId = :monthId AND status = 'present'")
    fun getPersonalOvertimeSumFlow(userId: String, monthId: String): Flow<Int?>

    @Query("SELECT COUNT(*) FROM attendance WHERE contractorId = :contractorId AND userId = :workerUserId AND monthId = :monthId AND status = :status AND type = :type")
    fun getWorkerStatusCountFlow(contractorId: String, workerUserId: String, monthId: String, status: String, type: String? = null): Flow<Int>

    @Query("SELECT SUM(advanceAmount) FROM attendance WHERE contractorId = :contractorId AND userId = :workerUserId AND monthId = :monthId AND status = 'advance'")
    fun getWorkerAdvanceSumFlow(contractorId: String, workerUserId: String, monthId: String): Flow<Double?>

    // All time queries
    @Query("SELECT COUNT(*) FROM attendance WHERE userId = :userId AND status = 'present'")
    fun getPersonalAllTimePresentFlow(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance WHERE userId = :userId AND status = 'present' AND type = 'half'")
    fun getPersonalAllTimeHalfFlow(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attendanceRecords: List<AttendanceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attendance: AttendanceEntity)

    @Query("DELETE FROM attendance WHERE id = :id")
    suspend fun deleteById(id: String)
}
