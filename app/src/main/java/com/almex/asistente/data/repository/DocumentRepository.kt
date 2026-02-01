package com.almex.asistente.data.repository

import com.almex.asistente.data.database.*
import com.almex.asistente.document.DocumentGenerator
import com.almex.asistente.document.DocumentGenerationResult
import kotlinx.coroutines.flow.Flow

class DocumentRepository(
    private val documentDao: DocumentDao,
    private val chatDao: DocumentChatDao,
    private val documentGenerator: DocumentGenerator
) {
    
    fun getAllDocuments(): Flow<List<DocumentEntity>> {
        return documentDao.getAllActiveDocuments()
    }
    
    suspend fun getDocumentById(id: Long): DocumentEntity? {
        return documentDao.getDocumentById(id)
    }
    
    suspend fun createDocument(
        title: String,
        content: String,
        type: DocumentType,
        fileName: String? = null
    ): Result<DocumentEntity> {
        return try {
            val generationResult = when (type) {
                DocumentType.PDF -> documentGenerator.generatePDF(title, content, fileName)
                DocumentType.DOCX -> documentGenerator.generateDOCX(title, content, fileName)
                DocumentType.TXT -> documentGenerator.generateTXT(title, content, fileName)
            }
            
            when (generationResult) {
                is DocumentGenerationResult.Success -> {
                    val document = DocumentEntity(
                        title = title,
                        fileName = generationResult.fileName,
                        filePath = generationResult.filePath,
                        fileType = type,
                        contentPreview = content.take(200),
                        wordCount = generationResult.wordCount,
                        fileSize = generationResult.fileSize
                    )
                    
                    val documentId = documentDao.insertDocument(document)
                    val savedDocument = document.copy(id = documentId)
                    
                    // Agregar mensaje inicial al chat
                    addChatMessage(
                        documentId,
                        "Documento '$title' creado exitosamente",
                        isUser = false,
                        actionType = ChatActionType.GENERATION
                    )
                    
                    Result.success(savedDocument)
                }
                is DocumentGenerationResult.Error -> {
                    Result.failure(Exception(generationResult.message))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteDocument(document: DocumentEntity): Result<Unit> {
        return try {
            // Eliminar archivo físico
            val deleted = documentGenerator.deleteDocument(document.filePath)
            
            if (deleted) {
                // Desactivar en base de datos
                documentDao.deactivateDocument(document.id)
                // Limpiar historial de chat
                chatDao.clearChatHistory(document.id)
                Result.success(Unit)
            } else {
                Result.failure(Exception("No se pudo eliminar el archivo"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun searchDocuments(query: String): List<DocumentEntity> {
        return documentDao.searchDocuments(query)
    }
    
    suspend fun getDocumentsByType(type: DocumentType): List<DocumentEntity> {
        return documentDao.getDocumentsByType(type)
    }
    
    suspend fun readDocumentContent(document: DocumentEntity): String {
        return documentGenerator.readDocumentContent(document.filePath)
    }
    
    suspend fun getDocumentSummaryForAI(documentId: Long): String {
        val document = documentDao.getDocumentById(documentId) ?: return "Documento no encontrado"
        val content = documentGenerator.readDocumentContent(document.filePath)
        
        return """
            Documento: ${document.title}
            Tipo: ${document.fileType.name}
            Creado: ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(document.createdAt))}
            Palabras: ${document.wordCount}
            
            Contenido:
            ${content.take(1000)}${if (content.length > 1000) "..." else ""}
        """.trimIndent()
    }
    
    // Funciones de Chat
    fun getChatHistory(documentId: Long): Flow<List<DocumentChatEntity>> {
        return chatDao.getChatHistory(documentId)
    }
    
    suspend fun addChatMessage(
        documentId: Long,
        message: String,
        isUser: Boolean,
        actionType: ChatActionType = ChatActionType.MESSAGE
    ) {
        val chatMessage = DocumentChatEntity(
            documentId = documentId,
            message = message,
            isUser = isUser,
            actionType = actionType.name
        )
        
        chatDao.insertChatMessage(chatMessage)
        
        // Actualizar timestamp del documento
        documentDao.updateLastModified(documentId)
        
        // Limpiar mensajes antiguos si hay demasiados
        val messageCount = chatDao.getChatMessageCount(documentId)
        if (messageCount > 100) {
            chatDao.cleanOldChatMessages(documentId)
        }
    }
    
    suspend fun getStorageInfo(): StorageInfo {
        val documentCount = documentDao.getActiveDocumentCount()
        val totalSize = documentDao.getTotalStorageUsed() ?: 0L
        
        return StorageInfo(
            documentCount = documentCount,
            totalSizeBytes = totalSize,
            totalSizeMB = totalSize / (1024 * 1024).toDouble()
        )
    }
    
    suspend fun updateDocument(
        document: DocumentEntity,
        newTitle: String? = null,
        newContent: String? = null
    ): Result<DocumentEntity> {
        return try {
            val updatedDocument = document.copy(
                title = newTitle ?: document.title,
                lastModified = System.currentTimeMillis()
            )
            
            // Si hay nuevo contenido, regenerar el archivo
            if (newContent != null) {
                val generationResult = when (document.fileType) {
                    DocumentType.PDF -> documentGenerator.generatePDF(
                        updatedDocument.title, 
                        newContent, 
                        document.fileName
                    )
                    DocumentType.DOCX -> documentGenerator.generateDOCX(
                        updatedDocument.title, 
                        newContent, 
                        document.fileName
                    )
                    DocumentType.TXT -> documentGenerator.generateTXT(
                        updatedDocument.title, 
                        newContent, 
                        document.fileName
                    )
                }
                
                when (generationResult) {
                    is DocumentGenerationResult.Success -> {
                        val finalDocument = updatedDocument.copy(
                            contentPreview = newContent.take(200),
                            wordCount = generationResult.wordCount,
                            fileSize = generationResult.fileSize
                        )
                        
                        documentDao.updateDocument(finalDocument)
                        Result.success(finalDocument)
                    }
                    is DocumentGenerationResult.Error -> {
                        Result.failure(Exception(generationResult.message))
                    }
                }
            } else {
                documentDao.updateDocument(updatedDocument)
                Result.success(updatedDocument)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class StorageInfo(
    val documentCount: Int,
    val totalSizeBytes: Long,
    val totalSizeMB: Double
)