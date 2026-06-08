package com.example.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grading_results")
data class GradingResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val imageUri: String,
    val grade: String,
    val confidence: Float,
    val reasoning: String,
    val timestamp: Long = System.currentTimeMillis()
)
