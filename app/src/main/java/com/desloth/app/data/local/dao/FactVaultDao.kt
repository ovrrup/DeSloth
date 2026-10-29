package com.desloth.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.desloth.app.data.local.entities.FactVaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FactVaultDao {

    @Query("SELECT * FROM fact_vault ORDER BY id ASC")
    fun getAll(): Flow<List<FactVaultEntity>>

    @Query("SELECT * FROM fact_vault ORDER BY id ASC")
    suspend fun getAllSync(): List<FactVaultEntity>

    @Query("SELECT * FROM fact_vault WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): FactVaultEntity?

    @Query("SELECT * FROM fact_vault ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomFact(): FactVaultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fact: FactVaultEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(facts: List<FactVaultEntity>)

    @Update
    suspend fun update(fact: FactVaultEntity)

    @Delete
    suspend fun delete(fact: FactVaultEntity)

    @Query("DELETE FROM fact_vault WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM fact_vault")
    suspend fun clearAll()
}
