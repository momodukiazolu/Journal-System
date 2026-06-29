package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "risk_locks")
data class RiskLockEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lockType: String, // DAILY_LOSS, TRADE_LIMIT, CONSECUTIVE_LOSS
    val triggerTime: Long = System.currentTimeMillis(),
    val triggerValue: Double = 0.0,
    val unlockTime: Long = 0L,
    val resolvedVia: String = "TIME_ELAPSED" // TIME_ELAPSED or OVERRIDE
)
