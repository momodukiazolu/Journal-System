package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trading_plans")
data class TradingPlanEntity(
    @PrimaryKey val id: Int = 1,
    val accountBalance: Double = 10000.0,
    val riskPerTradePercent: Double = 1.0,
    val maxDailyLossPercent: Double = 5.0,
    val maxWeeklyLossPercent: Double = 10.0,
    val maxDrawdownPercent: Double = 10.0,
    val maxTradesPerDay: Int = 3,
    val maxConsecutiveLosses: Int = 2,
    val dailyResetTime: String = "00:00",
    val cooldownDuration: String = "Rest of Day",
    
    // Entry Rules Checklist Requirements
    val reqLiquiditySweep: Boolean = true,
    val reqBos: Boolean = true,
    val reqChoch: Boolean = true,
    val reqFvg: Boolean = false,
    val minRr: Double = 2.0,
    
    // Prop Firm Settings
    val propFirmMode: Boolean = false,
    val propFirmPreset: String = "Custom", // FTMO, FundedNext, Custom
    val propFirmAccountSize: Double = 10000.0,
    val propFirmDailyDrawdownLimit: Double = 5.0,
    val propFirmMaxDrawdownLimit: Double = 10.0,
    val propFirmProfitTarget: Double = 8.0,
    val rulesLastVerifiedAt: Long = System.currentTimeMillis()
)
