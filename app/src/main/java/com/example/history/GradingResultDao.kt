package com.example.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GradingResultDao {
    @Query("SELECT * FROM grading_results ORDER BY timestamp DESC LIMIT 10")
    fun getAllResults(): Flow<List<GradingResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: GradingResultEntity)

    @Query("DELETE FROM grading_results WHERE id NOT IN (SELECT id FROM grading_results ORDER BY timestamp DESC LIMIT 10)")
    suspend fun keepOnlyLast10()

    @Query("DELETE FROM grading_results WHERE id = :id")
    suspend fun deleteResultById(id: Int)
}
