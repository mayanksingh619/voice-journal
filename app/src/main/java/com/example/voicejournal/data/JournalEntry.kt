package com.example.voicejournal.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey val id: String,
    val text: String,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val source: String = "typed",
    val reminderAt: Long? = null
)
