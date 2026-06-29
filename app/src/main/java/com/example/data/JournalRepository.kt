package com.example.data

import kotlinx.coroutines.flow.Flow

class JournalRepository(private val appDao: AppDao) {
    
    // --- Trading Plan Settings ---
    val tradingPlan: Flow<TradingPlanEntity?> = appDao.getTradingPlan()
    
    suspend fun getTradingPlanSync(): TradingPlanEntity? {
        return appDao.getTradingPlanSync()
    }
    
    suspend fun saveTradingPlan(plan: TradingPlanEntity) {
        appDao.insertTradingPlan(plan)
    }
    
    // --- Trades ---
    val allTrades: Flow<List<TradeEntity>> = appDao.getAllTrades()
    
    fun getTradeById(id: Int): Flow<TradeEntity?> {
        return appDao.getTradeById(id)
    }
    
    suspend fun getTradeByIdSync(id: Int): TradeEntity? {
        return appDao.getTradeByIdSync(id)
    }
    
    suspend fun getTradesByDateRangeSync(start: Long, end: Long): List<TradeEntity> {
        return appDao.getTradesByDateRangeSync(start, end)
    }
    
    suspend fun insertTrade(trade: TradeEntity): Long {
        return appDao.insertTrade(trade)
    }
    
    suspend fun deleteTradeById(id: Int) {
        appDao.deleteTradeById(id)
    }
    
    // --- Psychology Entries ---
    val allPsychologyEntries: Flow<List<PsychologyEntryEntity>> = appDao.getAllPsychologyEntries()
    
    suspend fun getAllPsychologyEntriesSync(): List<PsychologyEntryEntity> {
        return appDao.getAllPsychologyEntriesSync()
    }
    
    fun getPsychologyEntryByDate(dateString: String): Flow<PsychologyEntryEntity?> {
        return appDao.getPsychologyEntryByDate(dateString)
    }
    
    suspend fun getPsychologyEntryByDateSync(dateString: String): PsychologyEntryEntity? {
        return appDao.getPsychologyEntryByDateSync(dateString)
    }
    
    suspend fun insertPsychologyEntry(entry: PsychologyEntryEntity) {
        appDao.insertPsychologyEntry(entry)
    }
    
    // --- Risk Locks ---
    fun getActiveLocks(currentTime: Long): Flow<List<RiskLockEventEntity>> {
        return appDao.getActiveLocks(currentTime)
    }
    
    suspend fun getAllRiskLocksSync(): List<RiskLockEventEntity> {
        return appDao.getAllRiskLocksSync()
    }
    
    suspend fun getActiveLocksSync(currentTime: Long): List<RiskLockEventEntity> {
        return appDao.getActiveLocksSync(currentTime)
    }
    
    suspend fun insertRiskLock(lock: RiskLockEventEntity) {
        appDao.insertRiskLock(lock)
    }
    
    suspend fun updateRiskLock(lock: RiskLockEventEntity) {
        appDao.updateRiskLock(lock)
    }
    
    suspend fun clearAllActiveLocks(time: Long) {
        appDao.clearAllActiveLocks(time)
    }
    
    // --- Discipline Scores ---
    val allDisciplineScores: Flow<List<DisciplineScoreEntity>> = appDao.getAllDisciplineScores()
    
    suspend fun getAllDisciplineScoresSync(): List<DisciplineScoreEntity> {
        return appDao.getAllDisciplineScoresSync()
    }
    
    fun getDisciplineScoreByDate(dateString: String): Flow<DisciplineScoreEntity?> {
        return appDao.getDisciplineScoreByDate(dateString)
    }
    
    suspend fun getDisciplineScoreByDateSync(dateString: String): DisciplineScoreEntity? {
        return appDao.getDisciplineScoreByDateSync(dateString)
    }
    
    suspend fun insertDisciplineScore(score: DisciplineScoreEntity) {
        appDao.insertDisciplineScore(score)
    }
}
