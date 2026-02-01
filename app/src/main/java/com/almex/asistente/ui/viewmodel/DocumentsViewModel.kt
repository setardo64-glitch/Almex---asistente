package com.almex.asistente.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almex.asistente.AlmexApplication
import com.almex.asistente.api.GroqApiClient
import com.almex.asistente.data.database.DocumentEntity
import com.almex.asistente.data.database.DocumentType
import com.almex.asistente.data.repository.StorageInfo
import kotlinx.coroutines.launch

class DocumentsViewModel : ViewModel() {
    
    private val documentRepository = AlmexApplication.instance.documentRepository
    private val groqClient = GroqApiClient()
    
    private val _documents = MutableLiveData<List<DocumentEntity>>()
    val documents: LiveData<List<DocumentEntity>> = _documents
    
    private val _storageInfo = MutableLiveData<StorageInfo>()
    val storageInfo: LiveData<StorageInfo> = _storageInfo
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private var allDocuments: List<DocumentEntity> = emptyList()
    
    init {
        loadDocuments()
        loadStorageInfo()
    }
    
    private fun loadDocuments() {
        viewModelScope.launch {
            documentRepository.getAllDocuments().collect { documentList ->
                allDocuments = documentList
                _documents.value = documentList
            }
        }
    }
    
    private fun loadStorageInfo() {
        viewModelScope.launch {
            try {
                val info = documentRepository.getStorageInfo()
                _storageInfo.value = info
            } catch (e: Exception) {
                _errorMessage.value = "Error cargando información de almacenamiento"
                clearMessages()
            }
        }
    }
    
    fun createDocument(title: String, prompt: String, type: DocumentType) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Generar contenido con IA
                val aiPrompt = """
                    Crea un documento profesional sobre: $prompt
                    
                    Título: $title
                    
                    Instrucciones:
                    - Estructura clara con introducción, desarrollo y conclusión
                    - Contenido informativo y bien organizado
                    - Longitud apropiada (500-1000 palabras)
                    - Tono profesional pero accesible
                    - Incluye puntos clave y ejemplos cuando sea relevante
                """.trimIndent()
                
                val content = groqClient.sendMessage(aiPrompt)
                
                if (content.isNotEmpty() && !content.startsWith("Error")) {
                    val result = documentRepository.createDocument(title, content, type)
                    
                    result.fold(
                        onSuccess = { document ->
                            _successMessage.value = "Documento '${document.title}' creado exitosamente"
                            loadStorageInfo() // Actualizar info de almacenamiento
                        },
                        onFailure = { exception ->
                            _errorMessage.value = exception.message ?: "Error creando documento"
                        }
                    )
                } else {
                    _errorMessage.value = "Error generando contenido con IA"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
                clearMessages()
            }
        }
    }
    
    fun deleteDocument(document: DocumentEntity) {
        viewModelScope.launch {
            try {
                val result = documentRepository.deleteDocument(document)
                
                result.fold(
                    onSuccess = {
                        _successMessage.value = "Documento eliminado"
                        loadStorageInfo()
                    },
                    onFailure = { exception ->
                        _errorMessage.value = exception.message ?: "Error eliminando documento"
                    }
                )
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                clearMessages()
            }
        }
    }
    
    fun searchDocuments(query: String) {
        viewModelScope.launch {
            try {
                val results = documentRepository.searchDocuments(query)
                _documents.value = results
            } catch (e: Exception) {
                _errorMessage.value = "Error en búsqueda: ${e.message}"
                clearMessages()
            }
        }
    }
    
    fun filterByType(type: DocumentType?) {
        viewModelScope.launch {
            try {
                val filtered = if (type == null) {
                    allDocuments
                } else {
                    documentRepository.getDocumentsByType(type)
                }
                _documents.value = filtered
            } catch (e: Exception) {
                _errorMessage.value = "Error filtrando documentos: ${e.message}"
                clearMessages()
            }
        }
    }
    
    fun clearSearch() {
        _documents.value = allDocuments
    }
    
    private fun clearMessages() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _errorMessage.value = ""
            _successMessage.value = ""
        }
    }
}