package com.almex.asistente.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    
    @Query("SELECT * FROM documents WHERE isActive = 1 ORDER BY lastModified DESC")
    fun getAllActiveDocuments(): Flow<List<DocumentEntity>>
    
    @Query("SELECT * FROM documents WHERE isActive = 1 ORDER BY lastModified DESC")
    suspend fun getAllActiveDocumentsSync(): List<DocumentEntity>
    
    @Query("SELECT * FROM documents WHERE id = :documentId AND isActive = 1")
    suspend fun getDocumentById(documentId: Long): DocumentEntity?
    
    @Query("SELECT * FROM documents WHERE fileName = :fileName AND isActive = 1")
    suspend fun getDocumentByFileName(fileName: String): DocumentEntity?
    
    @Insert
    suspend fun insertDocument(document: DocumentEntity): Long
    
    @Update
    suspend fun updateDocument(document: DocumentEntity)
    
    @Query("UPDATE documents SET isActive = 0 WHERE id = :documentId")
    suspend fun deactivateDocument(documentId: Long)
    
    @Query("UPDATE documents SET lastModified = :timestamp WHERE id = :documentId")
    suspend fun updateLastModified(documentId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM documents WHERE isActive = 1")
    suspend fun getActiveDocumentCount(): Int
    
    @Query("SELECT SUM(fileSize) FROM documents WHERE isActive = 1")
    suspend fun getTotalStorageUsed(): Long?
    
    // Búsqueda de documentos
    @Query("""
        SELECT * FROM documents 
        WHERE isActive = 1 
        AND (title LIKE '%' || :query || '%' OR contentPreview LIKE '%' || :query || '%')
        ORDER BY lastModified DESC
    """)
    suspend fun searchDocuments(query: String): List<DocumentEntity>
    
    // Obtener documentos por tipo
    @Query("SELECT * FROM documents WHERE fileType = :type AND isActive = 1 ORDER BY lastModified DESC")
    suspend fun getDocumentsByType(type: DocumentType): List<DocumentEntity>
}