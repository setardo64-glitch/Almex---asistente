package com.almex.asistente.api

import com.almex.asistente.AlmexApplication
import com.almex.asistente.config.ApiConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class GroqApiClient {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    companion object {
        private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"
        private const val GROQ_AUDIO_URL = "https://api.groq.com/openai/v1/audio/transcriptions"
        private const val API_KEY = ApiConfig.GROQ_API_KEY // Usar configuración centralizada
    }
    
    suspend fun sendMessage(message: String, context: String = ""): String = withContext(Dispatchers.IO) {
        try {
            // Obtener información de horarios si es relevante
            val scheduleContext = if (isScheduleRelatedQuery(message)) {
                AlmexApplication.instance.scheduleRepository.getSchedulesSummaryForAI()
            } else ""
            
            // Obtener información de documentos si es relevante
            val documentContext = if (isDocumentRelatedQuery(message)) {
                getDocumentsSummaryForAI()
            } else ""
            
            val systemPrompt = """
                Eres Almex, un asistente personal para Andrés. 
                Características:
                - Respuestas concisas y útiles
                - Tono profesional pero amigable
                - Optimizado para dispositivos móviles
                - Recuerda el contexto de conversaciones previas
                - Tienes acceso a los horarios de Andrés a través de "Horacio"
                
                $context
                
                $scheduleContext
                
                $documentContext
            """.trimIndent()
            
            val requestBody = GroqChatRequest(
                model = ApiConfig.GROQ_MODEL,
                messages = listOf(
                    GroqMessage("system", systemPrompt),
                    GroqMessage("user", message)
                ),
                maxTokens = 150,
                temperature = 0.7
            )
            
            val json = gson.toJson(requestBody)
            val body = json.toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(GROQ_API_URL)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val groqResponse = gson.fromJson(responseBody, GroqChatResponse::class.java)
                return@withContext groqResponse.choices.firstOrNull()?.message?.content ?: "Error en la respuesta"
            } else {
                return@withContext "Error: ${response.code}"
            }
            
        } catch (e: Exception) {
            return@withContext "Error de conexión: ${e.message}"
        }
    }
    
    suspend fun transcribeAudio(audioData: ByteArray): String = withContext(Dispatchers.IO) {
        try {
            // Implementación simplificada para transcripción de audio
            // En una implementación real, enviarías el audio a Groq
            return@withContext "Transcripción de audio placeholder"
        } catch (e: Exception) {
            return@withContext ""
        }
    }
}

data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    @SerializedName("max_tokens") val maxTokens: Int,
    val temperature: Double
)

data class GroqMessage(
    val role: String,
    val content: String
)

data class GroqChatResponse(
    val choices: List<GroqChoice>
)

data class GroqChoice(
    val message: GroqMessage
)    

    private fun isScheduleRelatedQuery(message: String): Boolean {
        val scheduleKeywords = listOf(
            "horario", "agenda", "qué tengo", "que tengo", "programado", 
            "cita", "reunión", "actividad", "tarea", "hoy", "mañana",
            "esta semana", "próximo", "calendario", "tiempo libre"
        )
        
        return scheduleKeywords.any { keyword ->
            message.lowercase().contains(keyword)
        }
    }
    
    private fun isDocumentRelatedQuery(message: String): Boolean {
        val documentKeywords = listOf(
            "documento", "archivo", "pdf", "word", "texto", "escribir",
            "crear", "redactar", "informe", "reporte", "carta", "memo"
        )
        
        return documentKeywords.any { keyword ->
            message.lowercase().contains(keyword)
        }
    }   
 
    private suspend fun getDocumentsSummaryForAI(): String {
        return try {
            val documents = AlmexApplication.instance.documentRepository.getAllDocuments()
            // Como es Flow, necesitamos obtener el primer valor
            var documentList: List<com.almex.asistente.data.database.DocumentEntity> = emptyList()
            
            // Para simplificar, usamos una versión síncrona
            val recentDocuments = AlmexApplication.instance.database.documentDao().getAllActiveDocumentsSync()
            
            if (recentDocuments.isEmpty()) {
                "Andrés no tiene documentos creados."
            } else {
                val summary = StringBuilder("Documentos recientes de Andrés:\n")
                recentDocuments.take(5).forEach { doc ->
                    summary.append("- ${doc.title} (${doc.fileType.name}, ${doc.wordCount} palabras)\n")
                }
                summary.toString()
            }
        } catch (e: Exception) {
            "Error accediendo a documentos."
        }
    }