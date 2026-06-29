package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discipline_scores")
data class DisciplineScoreEntity(
    @PrimaryKey val dateString: String, // "yyyy-MM-dd"
    val score: Double, // 0 to 100
    val followedRisk: Boolean,
    val followedSmc: Boolean,
    val noOvertrading: Boolean,
    val respectedDailyLimit: Boolean,
    val journalCompleted: Boolean
)
