package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val pair: String, // e.g. "EURUSD", "XAUUSD", "GBPUSD"
    val direction: String, // "LONG" or "SHORT"
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double?,
    val exitPrice: Double? = null,
    val lotSize: Double,
    val profitLoss: Double? = null,
    val status: String = "OPEN", // "OPEN", "CLOSED"
    val session: String = "New York", // London, New York, Asian, Overlap
    val entryScore: Int = 0, // Setup Quality Score (0-10)
    val revisedScore: Int = 0,
    val planCompliant: Boolean = true,
    
    // SMC Checklist Items (0-10 Market Structure points)
    val checklistHtfBias: Boolean = false,
    val checklistLiquiditySweep: Boolean = false,
    val checklistBos: Boolean = false,
    val checklistChoch: Boolean = false,
    val checklistOrderBlock: Boolean = false,
    val checklistFvg: Boolean = false,
    val checklistMitigationBlock: Boolean = false,
    val checklistPremiumZone: Boolean = false,
    val checklistDiscountZone: Boolean = false,
    val checklistSessionAlignment: Boolean = false,
    
    // Execution Correction Questions (Self-reported)
    val qFollowedHtfBias: Boolean = true,
    val qWaitLiquidity: Boolean = true,
    val qEntryConfirmed: Boolean = true,
    val qFollowedRiskRules: Boolean = true,
    val qMoveStopLossEmotionally: Boolean = false,
    val qOvertrade: Boolean = false,
    
    // Mistake Tags (Comma-separated, e.g., "FOMO,Revenge Trading")
    val mistakeTags: String = "", 
    val aiStrengths: String = "",
    val aiWeaknesses: String = "",
    val aiRecommendation: String = "",
    val isCorrectionCompleted: Boolean = false,
    val correctionDueBy: Long? = null,
    val notes: String? = null,
    
    // Visual documentation slots (simulation URIs or file paths)
    val screenshotBefore: String? = null,
    val screenshotDuring: String? = null,
    val screenshotAfter: String? = null
)
