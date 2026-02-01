package com.almex.asistente.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    
    @Query("SELECT * FROM conversations ORDER BY timestamp DESC LIMIT 10")
    fun getRecentConversations(): Flow<List<ConversationEntity>>
    
    @Query("SELECT summary FROM conversations ORDER BY timestamp DESC LIMIT 5")
    suspend fun getRecentSummaries(): List<String>
    
    @Insert
    suspend fun insertConversation(conversation: ConversationEntity)
    
    @Query("DELETE FROM conversations WHERE id NOT IN (SELECT id FROM conversations ORDER BY timestamp DESC LIMIT 50)")
    suspend fun cleanOldConversations()
    
    @Query("SELECT COUNT(*) FROM conversations")
    suspend fun getConversationCount(): Int
}