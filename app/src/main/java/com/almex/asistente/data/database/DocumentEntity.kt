package com.almex.asistente.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val fileName: String,
    val filePath: String,
    val fileType: DocumentType,
    val contentPreview: String, // Primeros 200 caracteres para preview
    val wordCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val fileSize: Long = 0, // En bytes
    val isActive: Boolean = true
)

enum class DocumentType(val extension: String, val mimeType: String) {
    PDF(".pdf", "application/pdf"),
    DOCX(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    TXT(".txt", "text/plain")
}

data class DocumentChatMessage(
    val id: Long = 0,
    val documentId: Long,
    val message: String,
    val isUser: Boolean, // true = usuario, false = IA
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: ChatActionType = ChatActionType.MESSAGE
)

enum class ChatActionType {
    MESSAGE,        // Mensaje normal
    EDIT_REQUEST,   // Solicitud de edición
    GENERATION,     // Generación inicial
    SUMMARY,        // Resumen del documento
    QUESTION        // Pregunta sobre el documento
}