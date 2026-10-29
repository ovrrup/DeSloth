package com.desloth.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.desloth.app.data.local.entities.TargetAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TargetAppDao {

    @Query("SELECT * FROM target_apps ORDER BY tier ASC, appName ASC")
    fun getAll(): Flow<List<TargetAppEntity>>

    @Query("SELECT * FROM target_apps ORDER BY tier ASC, appName ASC")
    suspend fun getAllSync(): List<TargetAppEntity>

    @Query("SELECT * FROM target_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): TargetAppEntity?

    @Query("SELECT * FROM target_apps WHERE packageName = :packageName LIMIT 1")
    fun getByPackageFlow(packageName: String): Flow<TargetAppEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: TargetAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<TargetAppEntity>)

    @Update
    suspend fun update(app: TargetAppEntity)

    @Delete
    suspend fun delete(app: TargetAppEntity)

    @Query("DELETE FROM target_apps WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("DELETE FROM target_apps")
    suspend fun clearAll()
}
