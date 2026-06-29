package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "psychology_entries")
data class PsychologyEntryEntity(
    @PrimaryKey val dateString: String, // format "yyyy-MM-dd"
    val emotionalState: String, // Excellent, Good, Neutral, Stressed, Emotional
    val confidenceLevel: Int, // 1 to 10
    val notesFeeling: String = "",
    val notesInfluence: String = "",
    val notesLearn: String = "",
    val tags: String = "" // comma-separated, e.g. "Greed,Fear,FOMO"
)
