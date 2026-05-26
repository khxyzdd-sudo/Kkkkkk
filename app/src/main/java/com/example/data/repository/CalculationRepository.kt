package com.example.data.repository

import com.example.data.database.CalculationHistory
import com.example.data.database.CalculationHistoryDao
import kotlinx.coroutines.flow.Flow

class CalculationRepository(private val dao: CalculationHistoryDao) {
    val allHistory: Flow<List<CalculationHistory>> = dao.getAllHistory()

    suspend fun insert(history: CalculationHistory) {
        dao.insertHistory(history)
    }

    suspend fun clearAll() {
        dao.clearHistory()
    }

    suspend fun delete(history: CalculationHistory) {
        dao.deleteHistory(history)
    }
}
