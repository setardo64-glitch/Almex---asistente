package com.almex.asistente.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    
    @Query("SELECT * FROM schedules WHERE dayOfWeek = :dayOfWeek AND isActive = 1 ORDER BY startTime ASC")
    fun getSchedulesForDay(dayOfWeek: Int): Flow<List<ScheduleEntity>>
    
    @Query("SELECT * FROM schedules WHERE dayOfWeek = :dayOfWeek AND isActive = 1 ORDER BY startTime ASC")
    suspend fun getSchedulesForDaySync(dayOfWeek: Int): List<ScheduleEntity>
    
    @Query("SELECT * FROM schedules WHERE isActive = 1 ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllActiveSchedules(): Flow<List<ScheduleEntity>>
    
    @Query("SELECT * FROM schedules WHERE isActive = 1 ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getAllActiveSchedulesSync(): List<ScheduleEntity>
    
    @Insert
    suspend fun insertSchedule(schedule: ScheduleEntity): Long
    
    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)
    
    @Delete
    suspend fun deleteSchedule(schedule: ScheduleEntity)
    
    @Query("UPDATE schedules SET isActive = 0 WHERE id = :scheduleId")
    suspend fun deactivateSchedule(scheduleId: Long)
    
    @Query("SELECT COUNT(*) FROM schedules WHERE dayOfWeek = :dayOfWeek AND isActive = 1")
    suspend fun getScheduleCountForDay(dayOfWeek: Int): Int
    
    // Consulta optimizada para validación de colisiones
    @Query("""
        SELECT * FROM schedules 
        WHERE dayOfWeek = :dayOfWeek 
        AND isActive = 1 
        AND id != :excludeId
        AND (
            (startTime <= :startTime AND endTime > :startTime) OR
            (startTime < :endTime AND endTime >= :endTime) OR
            (startTime >= :startTime AND endTime <= :endTime)
        )
    """)
    suspend fun findConflictingSchedules(
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        excludeId: Long = -1
    ): List<ScheduleEntity>
}