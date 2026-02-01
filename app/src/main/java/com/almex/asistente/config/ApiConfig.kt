package com.almex.asistente.config

/**
 * Configuración de APIs y credenciales
 * 
 * INSTRUCCIONES PARA ANDRÉS:
 * 1. Reemplaza GROQ_API_KEY con tu clave de API de Groq
 * 2. Si planeas integrar Google Calendar, añade GOOGLE_API_KEY
 * 3. Mantén estas credenciales seguras y no las subas a repositorios públicos
 */
object ApiConfig {
    
    // ========== GROQ API ==========
    // Obtén tu API key en: https://console.groq.com/keys
    const val GROQ_API_KEY = "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"
    const val GROQ_BASE_URL = "https://api.groq.com/openai/v1"
    const val GROQ_MODEL = "llama3-8b-8192"
    
    // ========== GOOGLE APIs (FUTURO) ==========
    // Para integración con Google Calendar
    const val GOOGLE_API_KEY = "TU_GOOGLE_API_KEY_AQUI"
    const val GOOGLE_CLIENT_ID = "TU_GOOGLE_CLIENT_ID_AQUI"
    
    // ========== CONFIGURACIÓN DE RECORDATORIOS ==========
    const val DEFAULT_REMINDER_ADVANCE_MINUTES = 5 // Recordar 5 minutos antes
    const val MAX_SCHEDULES_PER_DAY = 20 // Límite para optimizar rendimiento
    
    // ========== CONFIGURACIÓN DE VOZ ==========
    const val TTS_LANGUAGE = "es-ES" // Español de España
    const val TTS_SPEECH_RATE = 1.0f // Velocidad normal
    const val TTS_PITCH = 1.0f // Tono normal
    
    // ========== VALIDACIÓN ==========
    fun isGroqConfigured(): Boolean {
        return GROQ_API_KEY != "TU_GROQ_API_KEY_AQUI" && GROQ_API_KEY.isNotEmpty()
    }
    
    fun isGoogleConfigured(): Boolean {
        return GOOGLE_API_KEY != "TU_GOOGLE_API_KEY_AQUI" && GOOGLE_API_KEY.isNotEmpty()
    }
}