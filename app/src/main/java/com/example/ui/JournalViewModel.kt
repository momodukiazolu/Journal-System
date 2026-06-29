package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class JournalViewModel(
    application: Application,
    private val repository: JournalRepository
) : AndroidViewModel(application) {

    // --- State Flows ---
    val tradingPlan: StateFlow<TradingPlanEntity> = repository.tradingPlan
        .map { it ?: TradingPlanEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TradingPlanEntity())

    val trades: StateFlow<List<TradeEntity>> = repository.allTrades
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeLocks: StateFlow<List<RiskLockEventEntity>> = flow {
        while (true) {
            emit(repository.getActiveLocksSync(System.currentTimeMillis()))
            kotlinx.coroutines.delay(10000) // update every 10s
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val psychologyEntries: StateFlow<List<PsychologyEntryEntity>> = repository.allPsychologyEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val disciplineScores: StateFlow<List<DisciplineScoreEntity>> = repository.allDisciplineScores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())



    // --- Broker Integration State ---
    val brokerManager = BrokerManager(application)
    
    private val _isPaperMode = MutableStateFlow(brokerManager.isPaperMode())
    val isPaperMode: StateFlow<Boolean> = _isPaperMode.asStateFlow()

    private val _oandaConnected = MutableStateFlow(brokerManager.isOandaConnected())
    val oandaConnected: StateFlow<Boolean> = _oandaConnected.asStateFlow()

    private val _brokerBalance = MutableStateFlow(0.0)
    val brokerBalance: StateFlow<Double> = _brokerBalance.asStateFlow()

    private val _brokerCurrency = MutableStateFlow("USD")
    val brokerCurrency: StateFlow<String> = _brokerCurrency.asStateFlow()

    private val _brokerOpenPositionsCount = MutableStateFlow(0)
    val brokerOpenPositionsCount: StateFlow<Int> = _brokerOpenPositionsCount.asStateFlow()

    private val _paperPositions = MutableStateFlow(brokerManager.getPaperPositions())
    val paperPositions: StateFlow<List<PaperPosition>> = _paperPositions.asStateFlow()

    private val _brokerError = MutableStateFlow<String?>(null)
    val brokerError: StateFlow<String?> = _brokerError.asStateFlow()

    private val _brokerLoading = MutableStateFlow(false)
    val brokerLoading: StateFlow<Boolean> = _brokerLoading.asStateFlow()

    // Mock live tick pricing
    private val _livePrices = MutableStateFlow(mapOf(
        "EUR/USD" to 1.08542,
        "GBP/USD" to 1.26487,
        "USD/JPY" to 155.240,
        "XAU/USD" to 2322.65
    ))
    val livePrices: StateFlow<Map<String, Double>> = _livePrices.asStateFlow()

    init {
        // Start price tick loop
        viewModelScope.launch {
            val random = java.util.Random()
            while (true) {
                kotlinx.coroutines.delay(2000)
                val current = _livePrices.value.toMutableMap()
                current["EUR/USD"] = ((current["EUR/USD"] ?: 1.0850) + (random.nextDouble() - 0.5) * 0.0002).coerceAtLeast(0.0001)
                current["GBP/USD"] = ((current["GBP/USD"] ?: 1.2650) + (random.nextDouble() - 0.5) * 0.0003).coerceAtLeast(0.0001)
                current["USD/JPY"] = ((current["USD/JPY"] ?: 155.20) + (random.nextDouble() - 0.5) * 0.05).coerceAtLeast(1.0)
                current["XAU/USD"] = ((current["XAU/USD"] ?: 2320.0) + (random.nextDouble() - 0.5) * 0.80).coerceAtLeast(1.0)
                _livePrices.value = current
            }
        }
        
        // Ensure default plan is initialized
        viewModelScope.launch {
            val existing = repository.getTradingPlanSync()
            if (existing == null) {
                repository.saveTradingPlan(TradingPlanEntity())
            }
            // Check locks on startup
            checkAndTriggerLocks()
            recalculateTodayDisciplineScore()
            
            // Load broker account info
            refreshBrokerSummary()
        }
    }

    fun refreshBrokerSummary() {
        viewModelScope.launch {
            _brokerLoading.value = true
            _brokerError.value = null
            if (brokerManager.isPaperMode()) {
                _brokerBalance.value = brokerManager.getPaperBalance()
                _brokerCurrency.value = "USD"
                _paperPositions.value = brokerManager.getPaperPositions()
                _brokerOpenPositionsCount.value = _paperPositions.value.size
                _brokerLoading.value = false
            } else {
                if (brokerManager.isOandaConnected()) {
                    val res = brokerManager.getOandaSummary()
                    res.onSuccess { summary ->
                        _brokerBalance.value = summary.balance.toDoubleOrNull() ?: 0.0
                        _brokerCurrency.value = summary.currency
                        _brokerOpenPositionsCount.value = summary.openPositionCount
                        _oandaConnected.value = true
                        _brokerError.value = null
                    }.onFailure { err ->
                        _brokerError.value = "Failed to connect to OANDA: ${err.localizedMessage}"
                        // Fallback to offline paper mode
                        switchBrokerMode(true)
                    }
                } else {
                    _oandaConnected.value = false
                    _brokerBalance.value = 0.0
                    _brokerOpenPositionsCount.value = 0
                }
                _brokerLoading.value = false
            }
        }
    }

    fun switchBrokerMode(isPaperMode: Boolean) {
        brokerManager.setPaperMode(isPaperMode)
        _isPaperMode.value = isPaperMode
        refreshBrokerSummary()
    }

    fun connectOanda(token: String, accountId: String, environment: String, onResult: (Result<OandaAccountSummary>) -> Unit) {
        viewModelScope.launch {
            _brokerLoading.value = true
            _brokerError.value = null
            val result = brokerManager.validateAndConnectOanda(token, accountId, environment)
            result.onSuccess { summary ->
                _oandaConnected.value = true
                _isPaperMode.value = false
                brokerManager.setPaperMode(false)
                _brokerBalance.value = summary.balance.toDoubleOrNull() ?: 0.0
                _brokerCurrency.value = summary.currency
                _brokerOpenPositionsCount.value = summary.openPositionCount
                _brokerError.value = null
                onResult(Result.success(summary))
            }.onFailure { err ->
                _brokerError.value = "Connection failed: ${err.localizedMessage}"
                onResult(Result.failure(err))
            }
            _brokerLoading.value = false
        }
    }

    fun disconnectOanda() {
        brokerManager.logoutOanda()
        _oandaConnected.value = false
        switchBrokerMode(true)
    }

    fun placeBrokerTrade(instrument: String, direction: String, lotSize: Double, entryPrice: Double, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            _brokerLoading.value = true
            _brokerError.value = null
            
            // Check active risk locks
            val active = repository.getActiveLocksSync(System.currentTimeMillis())
            if (active.isNotEmpty()) {
                _brokerLoading.value = false
                onResult(Result.failure(IllegalStateException("Execution Blocked: Active Risk Locks are in effect!")))
                return@launch
            }

            if (_isPaperMode.value) {
                // Execute paper trade
                val result = brokerManager.placePaperOrder(instrument, direction, lotSize, entryPrice)
                result.onSuccess { position ->
                    refreshBrokerSummary()
                    // Auto-log the trade in our local Journal database for a seamless SMC journal workflow!
                    logTrade(
                        pair = instrument,
                        direction = direction,
                        entryPrice = entryPrice,
                        stopLoss = if (direction == "LONG") entryPrice * 0.99 else entryPrice * 1.01,
                        takeProfit = if (direction == "LONG") entryPrice * 1.02 else entryPrice * 0.98,
                        lotSize = lotSize,
                        session = "PAPER_BROKER",
                        checklistHtfBias = true,
                        checklistLiquiditySweep = true,
                        checklistBos = true,
                        checklistChoch = true,
                        checklistOrderBlock = true,
                        checklistFvg = true,
                        checklistMitigationBlock = true,
                        checklistPremiumZone = true,
                        checklistDiscountZone = true,
                        checklistSessionAlignment = true
                    )
                    onResult(Result.success("Paper order filled at $entryPrice for $lotSize lots."))
                }.onFailure { err ->
                    _brokerError.value = err.localizedMessage
                    refreshBrokerSummary() // Refresh because it might have closed a hedging position
                    onResult(Result.failure(err))
                }
            } else {
                // Execute real OANDA order
                // For OANDA we need units (lot size * multiplier)
                val unitsMultiplier = if (instrument.contains("XAU") || instrument.contains("GOLD")) 100.0 else 100000.0
                val units = lotSize * unitsMultiplier
                val signedUnits = if (direction == "LONG") units else -units
                
                val result = brokerManager.placeOandaOrder(instrument, signedUnits)
                result.onSuccess { fill ->
                    refreshBrokerSummary()
                    // Auto-log the trade in local database as well!
                    val filledPrice = fill.price.toDoubleOrNull() ?: entryPrice
                    logTrade(
                        pair = instrument,
                        direction = direction,
                        entryPrice = filledPrice,
                        stopLoss = if (direction == "LONG") filledPrice * 0.99 else filledPrice * 1.01,
                        takeProfit = if (direction == "LONG") filledPrice * 1.02 else filledPrice * 0.98,
                        lotSize = lotSize,
                        session = "OANDA_BROKER",
                        checklistHtfBias = true,
                        checklistLiquiditySweep = true,
                        checklistBos = true,
                        checklistChoch = true,
                        checklistOrderBlock = true,
                        checklistFvg = true,
                        checklistMitigationBlock = true,
                        checklistPremiumZone = true,
                        checklistDiscountZone = true,
                        checklistSessionAlignment = true
                    )
                    onResult(Result.success("OANDA order #${fill.id} filled at price $filledPrice."))
                }.onFailure { err ->
                    _brokerError.value = "OANDA Order Failed: ${err.localizedMessage}"
                    onResult(Result.failure(err))
                }
            }
            _brokerLoading.value = false
        }
    }

    fun closePaperPositionByIndex(index: Int, currentPrice: Double) {
        viewModelScope.launch {
            val pnl = brokerManager.closePaperPosition(index, currentPrice)
            refreshBrokerSummary()
            // Find the oldest open trade in the database for the instrument to auto-close!
            val openTrades = trades.value.filter { it.status == "OPEN" }
            if (openTrades.isNotEmpty()) {
                val oldest = openTrades.firstOrNull()
                if (oldest != null) {
                    closeTrade(
                        tradeId = oldest.id,
                        exitPrice = currentPrice,
                        notes = "Closed automatically via Live Trading Panel. Net realized: $${String.format("%.2f", pnl)}",
                        qFollowedHtfBias = true,
                        qWaitLiquidity = true,
                        qEntryConfirmed = true,
                        qFollowedRiskRules = true,
                        qMoveStopLossEmotionally = false,
                        qOvertrade = false
                    )
                }
            }
        }
    }

    fun resetPaperTrading() {
        brokerManager.clearPaperAccount()
        refreshBrokerSummary()
    }

    // --- Position Size Calculator ---
    fun calculatePositionSize(
        balance: Double,
        riskPercent: Double,
        entryPrice: Double,
        stopLoss: Double,
        instrument: String,
        direction: String
    ): Double {
        if (entryPrice <= 0.0 || stopLoss <= 0.0 || entryPrice == stopLoss) return 0.0
        
        val riskAmount = balance * (riskPercent / 100.0)
        val stopDistance = abs(entryPrice - stopLoss)
        
        // standard lot size multiplier
        val pipValueMultiplier = when {
            instrument.uppercase().contains("XAU") || instrument.uppercase().contains("GOLD") -> {
                // Gold pip size typically 0.1 move. 1 lot = 100 oz. $10 value per point move.
                100.0 // Risk Amount / (stop distance * 100) or let's use:
            }
            instrument.uppercase().contains("JPY") -> {
                // JPY pairs: pip is 2nd decimal
                1000.0
            }
            // Forex typical
            else -> 100000.0
        }
        
        val lotSize = riskAmount / (stopDistance * (pipValueMultiplier / 10.0))
        // Round down to standard broker precision (0.01 micro lot)
        val rounded = (lotSize * 100.0).toInt() / 100.0
        return if (rounded < 0.01) 0.01 else rounded
    }

    // --- Journal CRUD Methods ---
    fun logTrade(
        pair: String,
        direction: String,
        entryPrice: Double,
        stopLoss: Double,
        takeProfit: Double?,
        lotSize: Double,
        session: String,
        checklistHtfBias: Boolean,
        checklistLiquiditySweep: Boolean,
        checklistBos: Boolean,
        checklistChoch: Boolean,
        checklistOrderBlock: Boolean,
        checklistFvg: Boolean,
        checklistMitigationBlock: Boolean,
        checklistPremiumZone: Boolean,
        checklistDiscountZone: Boolean,
        checklistSessionAlignment: Boolean,
        screenshotBefore: String? = null
    ) {
        viewModelScope.launch {
            val plan = tradingPlan.value
            
            // Calculate setup score
            val checkedCount = listOf(
                checklistHtfBias, checklistLiquiditySweep, checklistBos, checklistChoch,
                checklistOrderBlock, checklistFvg, checklistMitigationBlock,
                checklistPremiumZone, checklistDiscountZone, checklistSessionAlignment
            ).count { it }
            
            // Check plan compliance: all required items in plan must be checked
            var planCompliant = true
            if (plan.reqLiquiditySweep && !checklistLiquiditySweep) planCompliant = false
            if (plan.reqBos && !checklistBos) planCompliant = false
            if (plan.reqChoch && !checklistChoch) planCompliant = false
            if (plan.reqFvg && !checklistFvg) planCompliant = false
            
            // Minimum R:R check
            val potentialReward = takeProfit?.let { abs(it - entryPrice) } ?: 0.0
            val stopDistance = abs(entryPrice - stopLoss)
            val plannedRr = if (stopDistance > 0) potentialReward / stopDistance else 0.0
            if (plannedRr < plan.minRr) planCompliant = false

            val newTrade = TradeEntity(
                pair = pair,
                direction = direction,
                entryPrice = entryPrice,
                stopLoss = stopLoss,
                takeProfit = takeProfit,
                lotSize = lotSize,
                session = session,
                entryScore = checkedCount,
                revisedScore = checkedCount,
                planCompliant = planCompliant,
                checklistHtfBias = checklistHtfBias,
                checklistLiquiditySweep = checklistLiquiditySweep,
                checklistBos = checklistBos,
                checklistChoch = checklistChoch,
                checklistOrderBlock = checklistOrderBlock,
                checklistFvg = checklistFvg,
                checklistMitigationBlock = checklistMitigationBlock,
                checklistPremiumZone = checklistPremiumZone,
                checklistDiscountZone = checklistDiscountZone,
                checklistSessionAlignment = checklistSessionAlignment,
                screenshotBefore = screenshotBefore,
                status = "OPEN"
            )
            
            repository.insertTrade(newTrade)
            checkAndTriggerLocks()
            recalculateTodayDisciplineScore()
        }
    }

    fun closeTrade(
        tradeId: Int,
        exitPrice: Double,
        notes: String? = null,
        screenshotAfter: String? = null,
        // Self-reported correction questions:
        qFollowedHtfBias: Boolean,
        qWaitLiquidity: Boolean,
        qEntryConfirmed: Boolean,
        qFollowedRiskRules: Boolean,
        qMoveStopLossEmotionally: Boolean,
        qOvertrade: Boolean
    ) {
        viewModelScope.launch {
            val trade = repository.getTradeByIdSync(tradeId) ?: return@launch
            val plan = tradingPlan.value
            
            // Pip/Value multiplier for P&L computation
            val multiplier = when {
                trade.pair.uppercase().contains("XAU") || trade.pair.uppercase().contains("GOLD") -> 100.0
                trade.pair.uppercase().contains("JPY") -> 1000.0
                else -> 100000.0
            }
            
            val directionSign = if (trade.direction.uppercase() == "LONG") 1.0 else -1.0
            val profitLossPoints = (exitPrice - trade.entryPrice) * directionSign
            val calculatedPnl = profitLossPoints * trade.lotSize * (multiplier / 10.0)
            
            // Run Trade Correction Engine - Rule-based Mistake Tagging
            val tags = mutableListOf<String>()
            
            // 1. No Confirmation
            if (trade.entryScore < 4 || !qEntryConfirmed) {
                tags.add("No Confirmation")
            }
            // 2. Early Entry
            if (!trade.checklistBos && !trade.checklistChoch) {
                tags.add("Early Entry")
            }
            // 3. Late Entry (Check if entry score is low and no OB or FVG but session is aligned)
            if (!trade.checklistOrderBlock && !trade.checklistFvg && trade.checklistSessionAlignment) {
                tags.add("Late Entry")
            }
            // 4. Counter-Trend Trading
            if (!trade.checklistHtfBias || !qFollowedHtfBias) {
                tags.add("Counter-Trend Trading")
            }
            // 5. Over-Leveraging
            val recommendedLot = calculatePositionSize(
                plan.accountBalance, plan.riskPerTradePercent,
                trade.entryPrice, trade.stopLoss, trade.pair, trade.direction
            )
            if (recommendedLot > 0.0 && trade.lotSize > recommendedLot * 1.1) {
                tags.add("Over-Leveraging")
            }
            // 6. Overtrading
            val todayTrades = getTodayTradesSync()
            if (todayTrades.size > plan.maxTradesPerDay || qOvertrade) {
                tags.add("Overtrading")
            }
            // 7. FOMO
            if (trade.entryScore < 6 && trade.checklistSessionAlignment && !qWaitLiquidity) {
                tags.add("FOMO")
            }
            // 8. Revenge Trading (Opened within 30 mins of a prior losing trade)
            val sortedToday = todayTrades.filter { it.status == "CLOSED" && it.id != trade.id }
                .sortedByDescending { it.date }
            if (sortedToday.isNotEmpty()) {
                val lastTrade = sortedToday.first()
                if ((lastTrade.profitLoss ?: 0.0) < 0.0 && abs(trade.date - lastTrade.date) < 30 * 60 * 1000) {
                    tags.add("Revenge Trading")
                }
            }
            
            val mistakeTagsStr = tags.distinct().joinToString(",")
            
            // Qualitative AI Feedback Generation
            val strengths = mutableListOf<String>()
            val weaknesses = mutableListOf<String>()
            var recommendation = "Keep building setup discipline and strictly follow the Trading Plan."
            
            if (trade.checklistHtfBias) strengths.add("Successfully aligned entry with High Time Frame (HTF) market bias.")
            if (trade.checklistLiquiditySweep) strengths.add("Waited for a liquidity sweep of minor structural points.")
            if (trade.checklistSessionAlignment) strengths.add("Executed inside prime trading session window.")
            if (strengths.isEmpty()) strengths.add("Maintained clean charting documentation on entry.")
            
            for (tag in tags) {
                when (tag) {
                    "No Confirmation" -> {
                        weaknesses.add("Entered trade without waiting for clear confirmation criteria (Score: ${trade.entryScore}/10).")
                        recommendation = "Do not buy or sell without a confirmed break of structure (BOS) or change of character (CHOCH) in your execution timeframe."
                    }
                    "Early Entry" -> {
                        weaknesses.add("Jumped the gun before market structure aligned.")
                        recommendation = "Always let the candle close on your entry timeframe to confirm structural mitigation before pressing execute."
                    }
                    "Late Entry" -> {
                        weaknesses.add("Chased the move long after the mitigation block / Order Block was left behind.")
                        recommendation = "Set limit orders at the FVG / OB instead of market executing once the expansion has already occurred."
                    }
                    "Counter-Trend Trading" -> {
                        weaknesses.add("Traded against the prevailing higher timeframe trend structure.")
                        recommendation = "Look for setups strictly aligning with 4H/1D direction, or dramatically reduce risk if counter-trending."
                    }
                    "Over-Leveraging" -> {
                        weaknesses.add("Oversized the position size (used ${trade.lotSize} lots vs recommended ${recommendedLot} lots).")
                        recommendation = "Use the built-in Position Size Calculator and input exact values before execution. Never guestimate lot size."
                    }
                    "Overtrading" -> {
                        weaknesses.add("Traded beyond your daily volume guidelines (Max: ${plan.maxTradesPerDay} trades).")
                        recommendation = "Step away from the screens. Turn off the computer after hitting your maximum daily trade allowance."
                    }
                    "FOMO" -> {
                        weaknesses.add("Gave in to fear of missing out, taking a low-scoring impulse entry.")
                        recommendation = "SMC is about patience. Remind yourself that the market prints high-quality setups every single day. Let this one go."
                    }
                    "Revenge Trading" -> {
                        weaknesses.add("Attempted to quickly win back losses right after a prior trade failed.")
                        recommendation = "Revenge trading is account-suicide. Put a mandatory 30-minute lock on your phone and screens after every single loss."
                    }
                }
            }
            if (weaknesses.isEmpty()) {
                weaknesses.add("No significant execution mistakes detected on this trade.")
                recommendation = "Excellent execution! Repeat this exact process and let the statistics do the work."
            }

            val updatedTrade = trade.copy(
                exitPrice = exitPrice,
                profitLoss = calculatedPnl,
                notes = notes,
                screenshotAfter = screenshotAfter,
                status = "CLOSED",
                qFollowedHtfBias = qFollowedHtfBias,
                qWaitLiquidity = qWaitLiquidity,
                qEntryConfirmed = qEntryConfirmed,
                qFollowedRiskRules = qFollowedRiskRules,
                qMoveStopLossEmotionally = qMoveStopLossEmotionally,
                qOvertrade = qOvertrade,
                mistakeTags = mistakeTagsStr,
                aiStrengths = strengths.joinToString("\n"),
                aiWeaknesses = weaknesses.joinToString("\n"),
                aiRecommendation = recommendation,
                isCorrectionCompleted = true,
                correctionDueBy = System.currentTimeMillis()
            )
            
            repository.insertTrade(updatedTrade)
            checkAndTriggerLocks()
            recalculateTodayDisciplineScore()
        }
    }

    fun deleteTrade(id: Int) {
        viewModelScope.launch {
            repository.deleteTradeById(id)
            checkAndTriggerLocks()
            recalculateTodayDisciplineScore()
        }
    }

    // --- Daily Psychology Logging ---
    fun savePsychologyEntry(
        emotionalState: String,
        confidenceLevel: Int,
        feeling: String,
        influence: String,
        learn: String,
        tags: String
    ) {
        viewModelScope.launch {
            val dateStr = getTodayDateString()
            val entry = PsychologyEntryEntity(
                dateString = dateStr,
                emotionalState = emotionalState,
                confidenceLevel = confidenceLevel,
                notesFeeling = feeling,
                notesInfluence = influence,
                notesLearn = learn,
                tags = tags
            )
            repository.insertPsychologyEntry(entry)
            recalculateTodayDisciplineScore()
        }
    }

    // --- Risk Management Center & Locks ---
    private suspend fun getTodayTradesSync(): List<TradeEntity> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfToday = calendar.timeInMillis
        
        return repository.getTradesByDateRangeSync(startOfToday, endOfToday)
    }

    suspend fun checkAndTriggerLocks() {
        val plan = repository.getTradingPlanSync() ?: return
        val todayTrades = getTodayTradesSync()
        val closedToday = todayTrades.filter { it.status == "CLOSED" }
        
        // 1. Daily Loss Check
        val realizedLossToday = closedToday.filter { (it.profitLoss ?: 0.0) < 0.0 }
            .sumOf { it.profitLoss ?: 0.0 }
        val dailyLossLimit = plan.accountBalance * (plan.maxDailyLossPercent / 100.0)
        if (abs(realizedLossToday) >= dailyLossLimit && realizedLossToday < 0) {
            val alreadyLocked = repository.getActiveLocksSync(System.currentTimeMillis())
                .any { it.lockType == "DAILY_LOSS" }
            if (!alreadyLocked) {
                repository.insertRiskLock(
                    RiskLockEventEntity(
                        lockType = "DAILY_LOSS",
                        triggerValue = abs(realizedLossToday),
                        unlockTime = getTomorrowMidnight()
                    )
                )
            }
        }
        
        // 2. Max Trades Limit Check
        if (todayTrades.size >= plan.maxTradesPerDay) {
            val alreadyLocked = repository.getActiveLocksSync(System.currentTimeMillis())
                .any { it.lockType == "TRADE_LIMIT" }
            if (!alreadyLocked) {
                repository.insertRiskLock(
                    RiskLockEventEntity(
                        lockType = "TRADE_LIMIT",
                        triggerValue = todayTrades.size.toDouble(),
                        unlockTime = getTomorrowMidnight()
                    )
                )
            }
        }
        
        // 3. Consecutive Closed Loss Check
        var consecutiveLosses = 0
        // Sort closed trades by date ascending to evaluate chronologically
        val sortedClosedToday = closedToday.sortedBy { it.date }
        for (trade in sortedClosedToday) {
            val pnl = trade.profitLoss ?: 0.0
            if (pnl < 0.0) {
                consecutiveLosses++
            } else if (pnl > 0.0) {
                consecutiveLosses = 0 // Reset on win (Break-even is neutral, ignored)
            }
        }
        if (consecutiveLosses >= plan.maxConsecutiveLosses) {
            val alreadyLocked = repository.getActiveLocksSync(System.currentTimeMillis())
                .any { it.lockType == "CONSECUTIVE_LOSS" }
            if (!alreadyLocked) {
                val cooldownDurationMs = when (plan.cooldownDuration) {
                    "1 hour" -> 1 * 60 * 60 * 1000L
                    "4 hours" -> 4 * 60 * 60 * 1000L
                    else -> getTomorrowMidnight() - System.currentTimeMillis() // Rest of Day
                }
                repository.insertRiskLock(
                    RiskLockEventEntity(
                        lockType = "CONSECUTIVE_LOSS",
                        triggerValue = consecutiveLosses.toDouble(),
                        unlockTime = System.currentTimeMillis() + cooldownDurationMs
                    )
                )
            }
        }
    }

    fun bypassRiskLocksManualOverride() {
        viewModelScope.launch {
            repository.clearAllActiveLocks(System.currentTimeMillis())
            // Record override and deduct points from today's Discipline Score
            val dateStr = getTodayDateString()
            val existingScore = repository.getDisciplineScoreByDateSync(dateStr)
            if (existingScore != null) {
                // Deduct 20 points for override penalty, floor at 0
                val penalizedScore = (existingScore.score - 20.0).coerceAtLeast(0.0)
                repository.insertDisciplineScore(
                    existingScore.copy(
                        score = penalizedScore,
                        followedRisk = false // risk rule violated
                    )
                )
            }
            checkAndTriggerLocks()
        }
    }

    // --- Discipline Score Calculation Engine ---
    suspend fun recalculateTodayDisciplineScore() {
        val plan = repository.getTradingPlanSync() ?: return
        val dateStr = getTodayDateString()
        val todayTrades = getTodayTradesSync()
        val closedToday = todayTrades.filter { it.status == "CLOSED" }
        
        // Component 1: Followed Risk Rules (20 pts)
        // No Over-Leveraging tag today and no manual override used today
        var followedRisk = true
        for (t in closedToday) {
            if (t.mistakeTags.contains("Over-Leveraging")) {
                followedRisk = false
            }
        }
        
        // Component 2: Followed SMC Rules (20 pts)
        // Average entryScore >= 6 and no counter-trend / no confirmation tags
        var followedSmc = true
        var isSmcApplicable = todayTrades.isNotEmpty()
        if (isSmcApplicable) {
            val avgScore = todayTrades.map { it.entryScore }.average()
            val containsViolations = closedToday.any { 
                it.mistakeTags.contains("Counter-Trend Trading") || 
                it.mistakeTags.contains("No Confirmation")
            }
            if (avgScore < 6.0 || containsViolations) {
                followedSmc = false
            }
        }
        
        // Component 3: No Overtrading (20 pts)
        // Trades taken <= maxTradesPerDay and no overtrading tag
        var noOvertrading = true
        if (todayTrades.size > plan.maxTradesPerDay || closedToday.any { it.mistakeTags.contains("Overtrading") }) {
            noOvertrading = false
        }
        
        // Component 4: Respected Daily Limit (20 pts)
        // No Daily Loss lock triggered today
        val activeLocksToday = repository.getActiveLocksSync(System.currentTimeMillis())
        val respectedDailyLimit = activeLocksToday.none { it.lockType == "DAILY_LOSS" }
        
        // Component 5: Journal Completed (20 pts)
        // Every trade today has correction completed, and psychology entry exists for today
        val psyEntry = repository.getPsychologyEntryByDateSync(dateStr)
        val hasPsychology = psyEntry != null
        val allCorrectionsDone = todayTrades.all { it.isCorrectionCompleted }
        val journalCompleted = hasPsychology && allCorrectionsDone

        // Compute aggregate score
        var earnedPoints = 0.0
        var applicablePoints = 0.0
        
        // 1. Risk
        earnedPoints += if (followedRisk) 20.0 else 0.0
        applicablePoints += 20.0
        
        // 2. SMC Rules (Exclude if zero trades taken today - D8)
        if (isSmcApplicable) {
            earnedPoints += if (followedSmc) 20.0 else 0.0
            applicablePoints += 20.0
        }
        
        // 3. Overtrading
        earnedPoints += if (noOvertrading) 20.0 else 0.0
        applicablePoints += 20.0
        
        // 4. Daily Limit
        earnedPoints += if (respectedDailyLimit) 20.0 else 0.0
        applicablePoints += 20.0
        
        // 5. Journal completed
        earnedPoints += if (journalCompleted) 20.0 else 0.0
        applicablePoints += 20.0
        
        val finalScore = if (applicablePoints > 0) (earnedPoints / applicablePoints) * 100.0 else 100.0
        
        repository.insertDisciplineScore(
            DisciplineScoreEntity(
                dateString = dateStr,
                score = finalScore,
                followedRisk = followedRisk,
                followedSmc = if (isSmcApplicable) followedSmc else true,
                noOvertrading = noOvertrading,
                respectedDailyLimit = respectedDailyLimit,
                journalCompleted = journalCompleted
            )
        )
    }

    // --- Trading Plan Settings ---
    fun updateTradingPlan(plan: TradingPlanEntity) {
        viewModelScope.launch {
            repository.saveTradingPlan(plan)
            checkAndTriggerLocks()
            recalculateTodayDisciplineScore()
        }
    }

    // --- Time Helpers ---
    private fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getTomorrowMidnight(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

class JournalViewModelFactory(
    private val application: Application,
    private val repository: JournalRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            return JournalViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
