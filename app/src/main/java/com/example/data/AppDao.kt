package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Trading Plan & Settings Queries ---
    @Query("SELECT * FROM trading_plans WHERE id = 1 LIMIT 1")
    fun getTradingPlan(): Flow<TradingPlanEntity?>

    @Query("SELECT * FROM trading_plans WHERE id = 1 LIMIT 1")
    suspend fun getTradingPlanSync(): TradingPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTradingPlan(plan: TradingPlanEntity)

    // --- Trade Queries ---
    @Query("SELECT * FROM trades ORDER BY date DESC")
    fun getAllTrades(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE id = :id LIMIT 1")
    fun getTradeById(id: Int): Flow<TradeEntity?>

    @Query("SELECT * FROM trades WHERE id = :id LIMIT 1")
    suspend fun getTradeByIdSync(id: Int): TradeEntity?

    @Query("SELECT * FROM trades WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    suspend fun getTradesByDateRangeSync(start: Long, end: Long): List<TradeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeEntity): Long

    @Query("DELETE FROM trades WHERE id = :id")
    suspend fun deleteTradeById(id: Int)

    // --- Psychology Entry Queries ---
    @Query("SELECT * FROM psychology_entries ORDER BY dateString DESC")
    fun getAllPsychologyEntries(): Flow<List<PsychologyEntryEntity>>

    @Query("SELECT * FROM psychology_entries")
    suspend fun getAllPsychologyEntriesSync(): List<PsychologyEntryEntity>

    @Query("SELECT * FROM psychology_entries WHERE dateString = :dateString LIMIT 1")
    fun getPsychologyEntryByDate(dateString: String): Flow<PsychologyEntryEntity?>

    @Query("SELECT * FROM psychology_entries WHERE dateString = :dateString LIMIT 1")
    suspend fun getPsychologyEntryByDateSync(dateString: String): PsychologyEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPsychologyEntry(entry: PsychologyEntryEntity)

    // --- Risk Lock Queries ---
    @Query("SELECT * FROM risk_locks WHERE unlockTime = 0 OR unlockTime > :currentTime")
    fun getActiveLocks(currentTime: Long): Flow<List<RiskLockEventEntity>>

    @Query("SELECT * FROM risk_locks WHERE unlockTime = 0 OR unlockTime > :currentTime")
    suspend fun getActiveLocksSync(currentTime: Long): List<RiskLockEventEntity>

    @Query("SELECT * FROM risk_locks")
    suspend fun getAllRiskLocksSync(): List<RiskLockEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRiskLock(lock: RiskLockEventEntity)

    @Update
    suspend fun updateRiskLock(lock: RiskLockEventEntity)

    @Query("UPDATE risk_locks SET unlockTime = :time, resolvedVia = 'OVERRIDE' WHERE unlockTime = 0 OR unlockTime > :time")
    suspend fun clearAllActiveLocks(time: Long)

    // --- Discipline Score Queries ---
    @Query("SELECT * FROM discipline_scores ORDER BY dateString DESC")
    fun getAllDisciplineScores(): Flow<List<DisciplineScoreEntity>>

    @Query("SELECT * FROM discipline_scores")
    suspend fun getAllDisciplineScoresSync(): List<DisciplineScoreEntity>

    @Query("SELECT * FROM discipline_scores WHERE dateString = :dateString LIMIT 1")
    fun getDisciplineScoreByDate(dateString: String): Flow<DisciplineScoreEntity?>

    @Query("SELECT * FROM discipline_scores WHERE dateString = :dateString LIMIT 1")
    suspend fun getDisciplineScoreByDateSync(dateString: String): DisciplineScoreEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDisciplineScore(score: DisciplineScoreEntity)
}
