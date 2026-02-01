package com.almex.asistente.data.repository

import com.almex.asistente.data.database.ConversationDao
import com.almex.asistente.data.database.ConversationEntity
import kotlinx.coroutines.flow.Flow

class ConversationRepository(private val conversationDao: ConversationDao) {
    
    fun getRecentConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.getRecentConversations()
    }
    
    suspend fun getContextForAI(): String {
        val summaries = conversationDao.getRecentSummaries()
        return if (summaries.isNotEmpty()) {
            "Contexto de conversaciones previas con Andrés:\n${summaries.joinToString("\n")}"
        } else {
            "Primera conversación con Andrés."
        }
    }
    
    suspend fun saveConversation(
        userMessage: String,
        assistantResponse: String,
        summary: String
    ) {
        val conversation = ConversationEntity(
            timestamp = System.currentTimeMillis(),
            userMessage = userMessage,
            assistantResponse = assistantResponse,
            summary = summary
        )
        conversationDao.insertConversation(conversation)
        
        // Limpia conversaciones antiguas si hay más de 50
        if (conversationDao.getConversationCount() > 50) {
            conversationDao.cleanOldConversations()
        }
    }
}