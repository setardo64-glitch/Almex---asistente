package com.almex.asistente.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.almex.asistente.AlmexApplication
import com.almex.asistente.R
import com.almex.asistente.api.GroqApiClient
import kotlinx.coroutines.*
import kotlin.math.sqrt

class VoiceListenerService : Service() {
    
    private var audioRecord: AudioRecord? = null
    private var isListening = false
    private var isActiveListening = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var activeListeningJob: Job? = null
    private var lastVoiceActivity = 0L
    
    private val groqClient = GroqApiClient()
    private val repository = AlmexApplication.instance.conversationRepository
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "almex_voice_service"
        private const val SAMPLE_RATE = 16000
        private const val WAKE_WORD_THRESHOLD = 0.02f
        private const val SILENCE_TIMEOUT = 10000L // 10 segundos
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification("Escuchando 'Almex'..."))
        startPassiveListening()
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        releaseWakeLock()
        serviceScope.cancel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Almex Voice Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Servicio de escucha de voz de Almex"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Almex Asistente")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AlmexAsistente::VoiceService"
        )
        wakeLock?.acquire(10*60*1000L) // 10 minutos máximo
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }
    
    private fun startPassiveListening() {
        if (isListening) return
        
        serviceScope.launch {
            try {
                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                
                audioRecord?.startRecording()
                isListening = true
                
                val buffer = ShortArray(bufferSize)
                
                while (isListening) {
                    val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    
                    if (bytesRead > 0) {
                        val amplitude = calculateAmplitude(buffer, bytesRead)
                        
                        if (!isActiveListening && detectWakeWord(buffer, bytesRead, amplitude)) {
                            startActiveListening()
                        }
                        
                        if (isActiveListening) {
                            if (amplitude > WAKE_WORD_THRESHOLD) {
                                lastVoiceActivity = System.currentTimeMillis()
                            } else if (System.currentTimeMillis() - lastVoiceActivity > SILENCE_TIMEOUT) {
                                stopActiveListening()
                            }
                        }
                    }
                    
                    delay(50) // Pequeña pausa para eficiencia
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun calculateAmplitude(buffer: ShortArray, length: Int): Float {
        var sum = 0.0
        for (i in 0 until length) {
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        return sqrt(sum / length).toFloat() / Short.MAX_VALUE
    }
    
    private fun detectWakeWord(buffer: ShortArray, length: Int, amplitude: Float): Boolean {
        // Implementación simplificada de detección de wake word
        // En una implementación real, usarías un modelo de ML más sofisticado
        return amplitude > WAKE_WORD_THRESHOLD * 2
    }
    
    private fun startActiveListening() {
        if (isActiveListening) return
        
        isActiveListening = true
        lastVoiceActivity = System.currentTimeMillis()
        
        val notification = createNotification("Escuchando comando...")
        startForeground(NOTIFICATION_ID, notification)
        
        activeListeningJob = serviceScope.launch {
            try {
                // Aquí implementarías la captura de audio para enviar a Groq
                val audioData = captureAudioForProcessing()
                val transcription = groqClient.transcribeAudio(audioData)
                
                if (transcription.isNotEmpty()) {
                    val context = repository.getContextForAI()
                    val response = groqClient.sendMessage(transcription, context)
                    
                    // Guardar conversación
                    val summary = generateSummary(transcription, response)
                    repository.saveConversation(transcription, response, summary)
                    
                    // Aquí podrías implementar TTS para responder
                    playResponse(response)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopActiveListening()
            }
        }
    }
    
    private fun stopActiveListening() {
        isActiveListening = false
        activeListeningJob?.cancel()
        
        val notification = createNotification("Escuchando 'Almex'...")
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private suspend fun captureAudioForProcessing(): ByteArray {
        // Implementación simplificada - captura 3 segundos de audio
        delay(3000)
        return ByteArray(0) // Placeholder
    }
    
    private fun generateSummary(userMessage: String, response: String): String {
        return "Usuario: ${userMessage.take(50)}... | Respuesta: ${response.take(50)}..."
    }
    
    private fun playResponse(response: String) {
        // Implementar TTS aquí
    }
    
    private fun stopListening() {
        isListening = false
        isActiveListening = false
        activeListeningJob?.cancel()
        
        audioRecord?.apply {
            if (state == AudioRecord.STATE_INITIALIZED) {
                stop()
                release()
            }
        }
        audioRecord = null
    }
}