package com.almex.asistente.data.repository

import com.almex.asistente.data.database.ScheduleDao
import com.almex.asistente.data.database.ScheduleEntity
import com.almex.asistente.data.database.TimeSlot
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class ScheduleRepository(private val scheduleDao: ScheduleDao) {
    
    fun getSchedulesForDay(dayOfWeek: Int): Flow<List<ScheduleEntity>> {
        return scheduleDao.getSchedulesForDay(dayOfWeek)
    }
    
    fun getAllActiveSchedules(): Flow<List<ScheduleEntity>> {
        return scheduleDao.getAllActiveSchedules()
    }
    
    suspend fun getTodaySchedules(): List<ScheduleEntity> {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val dayOfWeek = when(today) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
        return scheduleDao.getSchedulesForDaySync(dayOfWeek)
    }
    
    suspend fun validateScheduleTime(
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        excludeId: Long = -1
    ): ValidationResult {
        
        // Validar formato de tiempo
        if (!isValidTimeFormat(startTime) || !isValidTimeFormat(endTime)) {
            return ValidationResult.InvalidFormat
        }
        
        // Validar que hora de inicio sea menor que hora de fin
        if (!isStartTimeBeforeEndTime(startTime, endTime)) {
            return ValidationResult.InvalidTimeRange
        }
        
        // Buscar conflictos en la base de datos
        val conflicts = scheduleDao.findConflictingSchedules(
            dayOfWeek, startTime, endTime, excludeId
        )
        
        return if (conflicts.isNotEmpty()) {
            ValidationResult.TimeConflict(conflicts)
        } else {
            ValidationResult.Valid
        }
    }
    
    suspend fun saveSchedule(schedule: ScheduleEntity): Result<Long> {
        return try {
            val validation = validateScheduleTime(
                schedule.dayOfWeek,
                schedule.startTime,
                schedule.endTime,
                schedule.id
            )
            
            when (validation) {
                is ValidationResult.Valid -> {
                    val id = if (schedule.id == 0L) {
                        scheduleDao.insertSchedule(schedule)
                    } else {
                        scheduleDao.updateSchedule(schedule)
                        schedule.id
                    }
                    Result.success(id)
                }
                else -> Result.failure(Exception(validation.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteSchedule(schedule: ScheduleEntity) {
        scheduleDao.deactivateSchedule(schedule.id)
    }
    
    suspend fun getSchedulesSummaryForAI(): String {
        val schedules = scheduleDao.getAllActiveSchedulesSync()
        if (schedules.isEmpty()) {
            return "Andrés no tiene horarios programados."
        }
        
        val dayNames = arrayOf("", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        val summary = StringBuilder("Horarios de Andrés:\n")
        
        schedules.groupBy { it.dayOfWeek }.forEach { (day, daySchedules) ->
            summary.append("${dayNames[day]}:\n")
            daySchedules.forEach { schedule ->
                summary.append("- ${schedule.startTime} a ${schedule.endTime}: ${schedule.actionName}")
                if (schedule.objective.isNotEmpty()) {
                    summary.append(" (${schedule.objective})")
                }
                summary.append("\n")
            }
        }
        
        return summary.toString()
    }
    
    private fun isValidTimeFormat(time: String): Boolean {
        return try {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            format.isLenient = false
            format.parse(time)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isStartTimeBeforeEndTime(startTime: String, endTime: String): Boolean {
        return try {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val start = format.parse(startTime)
            val end = format.parse(endTime)
            start?.before(end) == true
        } catch (e: Exception) {
            false
        }
    }
}

sealed class ValidationResult(val message: String) {
    object Valid : ValidationResult("Horario válido")
    object InvalidFormat : ValidationResult("Formato de hora inválido. Use HH:mm")
    object InvalidTimeRange : ValidationResult("La hora de inicio debe ser menor que la hora de fin")
    data class TimeConflict(val conflicts: List<ScheduleEntity>) : 
        ValidationResult("Conflicto de horario con: ${conflicts.joinToString { "${it.actionName} (${it.startTime}-${it.endTime})" }}")
}