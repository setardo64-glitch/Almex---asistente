package com.almex.asistente.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "document_chat")
data class DocumentChatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val documentId: Long,
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String = ChatActionType.MESSAGE.name
)

@Dao
interface DocumentChatDao {
    
    @Query("SELECT * FROM document_chat WHERE documentId = :documentId ORDER BY timestamp ASC")
    fun getChatHistory(documentId: Long): Flow<List<DocumentChatEntity>>
    
    @Query("SELECT * FROM document_chat WHERE documentId = :documentId ORDER BY timestamp ASC")
    suspend fun getChatHistorySync(documentId: Long): List<DocumentChatEntity>
    
    @Insert
    suspend fun insertChatMessage(message: DocumentChatEntity)
    
    @Query("DELETE FROM document_chat WHERE documentId = :documentId")
    suspend fun clearChatHistory(documentId: Long)
    
    @Query("SELECT COUNT(*) FROM document_chat WHERE documentId = :documentId")
    suspend fun getChatMessageCount(documentId: Long): Int
    
    // Limpiar mensajes antiguos para optimizar rendimiento
    @Query("""
        DELETE FROM document_chat 
        WHERE documentId = :documentId 
        AND id NOT IN (
            SELECT id FROM document_chat 
            WHERE documentId = :documentId 
            ORDER BY timestamp DESC 
            LIMIT 100
        )
    """)
    suspend fun cleanOldChatMessages(documentId: Long)
}