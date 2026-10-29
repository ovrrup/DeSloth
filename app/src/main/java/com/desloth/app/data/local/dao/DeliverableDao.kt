package com.desloth.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.desloth.app.data.local.entities.DeliverableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliverableDao {

    @Query("SELECT * FROM deliverables ORDER BY isMicroTask ASC, id ASC")
    fun getAll(): Flow<List<DeliverableEntity>>

    @Query("SELECT * FROM deliverables ORDER BY isMicroTask ASC, id ASC")
    suspend fun getAllSync(): List<DeliverableEntity>

    @Query("SELECT * FROM deliverables WHERE completedToday = 1 AND lastCompletedEpochDay = :epochDay")
    suspend fun getCompletedToday(epochDay: Long): List<DeliverableEntity>

    @Query("SELECT * FROM deliverables WHERE isMicroTask = 1 AND completedToday = 0 LIMIT 1")
    suspend fun getFirstUncompletedMicroTask(): DeliverableEntity?

    @Query("SELECT * FROM deliverables WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): DeliverableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deliverable: DeliverableEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(deliverables: List<DeliverableEntity>)

    @Update
    suspend fun update(deliverable: DeliverableEntity)

    @Delete
    suspend fun delete(deliverable: DeliverableEntity)

    @Query("DELETE FROM deliverables WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE deliverables SET completedToday = :completedToday, lastCompletedEpochDay = :epochDay WHERE id = :id")
    suspend fun markCompleted(id: Long, completedToday: Boolean, epochDay: Long)

    /**
     * Resets tasks if they were completed on a prior epoch day (midnight roll).
     */
    @Query("UPDATE deliverables SET completedToday = 0 WHERE lastCompletedEpochDay < :currentEpochDay")
    suspend fun resetCompletionIfOutdated(currentEpochDay: Long)

    @Query("DELETE FROM deliverables")
    suspend fun clearAll()
}
