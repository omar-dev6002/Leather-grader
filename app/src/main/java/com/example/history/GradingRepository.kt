package com.example.history

import kotlinx.coroutines.flow.Flow

class GradingRepository(private val dao: GradingResultDao) {
    val allResults: Flow<List<GradingResultEntity>> = dao.getAllResults()

    suspend fun insertResult(result: GradingResultEntity) {
        dao.insertResult(result)
        dao.keepOnlyLast10()
    }

    suspend fun deleteResult(id: Int) {
        dao.deleteResultById(id)
    }
}
