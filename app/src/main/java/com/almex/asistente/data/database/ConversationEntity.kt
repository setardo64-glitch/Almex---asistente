package com.almex.asistente.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val userMessage: String,
    val assistantResponse: String,
    val summary: String,
    val context: String? = null
)