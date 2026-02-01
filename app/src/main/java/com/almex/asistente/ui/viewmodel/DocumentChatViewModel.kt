package com.almex.asistente.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almex.asistente.AlmexApplication
import com.almex.asistente.api.GroqApiClient
import com.almex.asistente.data.database.ChatActionType
import com.almex.asistente.data.database.DocumentChatEntity
import com.almex.asistente.data.database.DocumentEntity
import kotlinx.coroutines.launch

class DocumentChatViewModel : ViewModel() {
    
    private val documentRepository = AlmexApplication.instance.documentRepository
    private val groqClient = GroqApiClient()
    
    private val _currentDocument = MutableLiveData<DocumentEntity?>()
    val currentDocument: LiveData<DocumentEntity?> = _currentDocument
    
    private val _chatMessages = MutableLiveData<List<DocumentChatEntity>>()
    val chatMessages: LiveData<List<DocumentChatEntity>> = _chatMessages
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private var documentId: Long = -1
    
    fun loadDocument(id: Long) {
        documentId = id
        
        viewModelScope.launch {
            try {
                val document = documentRepository.getDocumentById(id)
                _currentDocument.value = document
                
                // Cargar historial de chat
                documentRepository.getChatHistory(id).collect { messages ->
                    _chatMessages.value = messages
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error cargando documento: ${e.message}"
                clearMessages()
            }
        }
    }
    
    fun sendMessage(message: String) {
        if (documentId == -1L) return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Agregar mensaje del usuario
                documentRepository.addChatMessage(
                    documentId = documentId,
                    message = message,
                    isUser = true,
                    actionType = ChatActionType.MESSAGE
                )
                
                // Obtener contexto del documento
                val documentContext = documentRepository.getDocumentSummaryForAI(documentId)
                
                // Crear prompt para la IA
                val aiPrompt = """
                    Contexto del documento:
                    $documentContext
                    
                    Usuario pregunta/solicita: $message
                    
                    Instrucciones:
                    - Responde basándote en el contenido del documento
                    - Si es una solicitud de edición, explica cómo se podría modificar
                    - Si es una pregunta, responde con información del documento
                    - Mantén un tono profesional y útil
                    - Si no puedes responder con la información disponible, dilo claramente
                """.trimIndent()
                
                val aiResponse = groqClient.sendMessage(aiPrompt)
                
                // Agregar respuesta de la IA
                val actionType = determineActionType(message)
                documentRepository.addChatMessage(
                    documentId = documentId,
                    message = aiResponse,
                    isUser = false,
                    actionType = actionType
                )
                
            } catch (e: Exception) {
                _errorMessage.value = "Error procesando mensaje: ${e.message}"
                clearMessages()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun summarizeDocument() {
        if (documentId == -1L) return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val document = _currentDocument.value ?: return@launch
                val content = documentRepository.readDocumentContent(document)
                
                val summaryPrompt = """
                    Resume el siguiente documento de manera concisa y estructurada:
                    
                    Título: ${document.title}
                    Contenido:
                    $content
                    
                    Proporciona:
                    1. Resumen ejecutivo (2-3 líneas)
                    2. Puntos clave principales
                    3. Conclusiones importantes
                """.trimIndent()
                
                val summary = groqClient.sendMessage(summaryPrompt)
                
                documentRepository.addChatMessage(
                    documentId = documentId,
                    message = "📋 **Resumen del documento:**\n\n$summary",
                    isUser = false,
                    actionType = ChatActionType.SUMMARY
                )
                
            } catch (e: Exception) {
                _errorMessage.value = "Error generando resumen: ${e.message}"
                clearMessages()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun askQuestion(question: String) {
        if (documentId == -1L) return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val document = _currentDocument.value ?: return@launch
                val content = documentRepository.readDocumentContent(document)
                
                val questionPrompt = """
                    Basándote en el siguiente documento, responde la pregunta:
                    
                    Documento: ${document.title}
                    Contenido: $content
                    
                    Pregunta: $question
                    
                    Proporciona una respuesta precisa y cita partes relevantes del documento.
                """.trimIndent()
                
                val answer = groqClient.sendMessage(questionPrompt)
                
                documentRepository.addChatMessage(
                    documentId = documentId,
                    message = "❓ **Pregunta:** $question",
                    isUser = true,
                    actionType = ChatActionType.QUESTION
                )
                
                documentRepository.addChatMessage(
                    documentId = documentId,
                    message = "💡 **Respuesta:**\n\n$answer",
                    isUser = false,
                    actionType = ChatActionType.QUESTION
                )
                
            } catch (e: Exception) {
                _errorMessage.value = "Error procesando pregunta: ${e.message}"
                clearMessages()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun determineActionType(message: String): ChatActionType {
        val lowerMessage = message.lowercase()
        
        return when {
            lowerMessage.contains("edita") || lowerMessage.contains("modifica") || 
            lowerMessage.contains("cambia") || lowerMessage.contains("actualiza") -> 
                ChatActionType.EDIT_REQUEST
                
            lowerMessage.contains("resume") || lowerMessage.contains("resumen") -> 
                ChatActionType.SUMMARY
                
            lowerMessage.contains("qué") || lowerMessage.contains("que") || 
            lowerMessage.contains("cómo") || lowerMessage.contains("como") ||
            lowerMessage.contains("cuál") || lowerMessage.contains("cual") -> 
                ChatActionType.QUESTION
                
            else -> ChatActionType.MESSAGE
        }
    }
    
    private fun clearMessages() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _errorMessage.value = ""
        }
    }
}