package com.almex.asistente.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dayOfWeek: Int, // 1=Lunes, 2=Martes, ..., 7=Domingo
    val actionName: String,
    val objective: String,
    val startTime: String, // Formato HH:mm
    val endTime: String,   // Formato HH:mm
    val reminderType: ReminderType,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ReminderType {
    NOTIFICATION, // Alerta leve
    VOICE,        // Modo Almex (voz)
    BOTH          // Ambos modos
}

data class TimeSlot(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) {
    fun toMinutes(): IntRange {
        val start = startHour * 60 + startMinute
        val end = endHour * 60 + endMinute
        return start..end
    }
    
    fun overlaps(other: TimeSlot): Boolean {
        val thisRange = this.toMinutes()
        val otherRange = other.toMinutes()
        return thisRange.first < otherRange.last && otherRange.first < thisRange.last
    }
}