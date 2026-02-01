package com.almex.asistente.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.almex.asistente.AlmexApplication
import com.almex.asistente.R
import com.almex.asistente.data.database.ReminderType
import com.almex.asistente.data.database.ScheduleEntity
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class ReminderService : Service(), TextToSpeech.OnInitListener {
    
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val scheduleRepository = AlmexApplication.instance.scheduleRepository
    
    companion object {
        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "almex_reminders"
        private const val CHECK_INTERVAL = 60000L // 1 minuto
        
        fun start(context: Context) {
            val intent = Intent(context, ReminderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, ReminderService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeTTS()
        startReminderChecker()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createServiceNotification())
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        tts?.shutdown()
    }
    
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("es", "ES")
            isTtsReady = true
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios Almex",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de recordatorios de Horacio"
                enableVibration(true)
                setShowBadge(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createServiceNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Horacio Activo")
            .setContentText("Monitoreando recordatorios...")
            .setSmallIcon(R.drawable.ic_schedule)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun initializeTTS() {
        tts = TextToSpeech(this, this)
    }
    
    private fun startReminderChecker() {
        serviceScope.launch {
            while (isActive) {
                checkForReminders()
                delay(CHECK_INTERVAL)
            }
        }
    }
    
    private suspend fun checkForReminders() {
        try {
            val todaySchedules = scheduleRepository.getTodaySchedules()
            val currentTime = getCurrentTime()
            
            todaySchedules.forEach { schedule ->
                if (shouldTriggerReminder(schedule, currentTime)) {
                    triggerReminder(schedule)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun getCurrentTime(): String {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(Date())
    }
    
    private fun shouldTriggerReminder(schedule: ScheduleEntity, currentTime: String): Boolean {
        // Activar recordatorio exactamente a la hora de inicio
        return schedule.startTime == currentTime
    }
    
    private suspend fun triggerReminder(schedule: ScheduleEntity) {
        when (schedule.reminderType) {
            ReminderType.NOTIFICATION -> showNotificationReminder(schedule)
            ReminderType.VOICE -> speakReminder(schedule)
            ReminderType.BOTH -> {
                showNotificationReminder(schedule)
                speakReminder(schedule)
            }
        }
    }
    
    private fun showNotificationReminder(schedule: ScheduleEntity) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏰ Recordatorio - ${schedule.actionName}")
            .setContentText("${schedule.startTime} - ${schedule.objective}")
            .setSmallIcon(R.drawable.ic_schedule)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(schedule.id.toInt(), notification)
    }
    
    private suspend fun speakReminder(schedule: ScheduleEntity) = withContext(Dispatchers.Main) {
        if (isTtsReady && tts != null) {
            val message = buildVoiceMessage(schedule)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "reminder_${schedule.id}")
            } else {
                @Suppress("DEPRECATION")
                tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null)
            }
        }
    }
    
    private fun buildVoiceMessage(schedule: ScheduleEntity): String {
        val messages = listOf(
            "Andrés, es hora de ${schedule.actionName.lowercase()}",
            "Recordatorio: ${schedule.actionName}",
            "Andrés, tienes programado ${schedule.actionName} ahora"
        )
        
        return if (schedule.objective.isNotEmpty()) {
            "${messages.random()}. ${schedule.objective}"
        } else {
            messages.random()
        }
    }
}