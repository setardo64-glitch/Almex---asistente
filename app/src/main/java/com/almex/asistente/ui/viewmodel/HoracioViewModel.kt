package com.almex.asistente.ui.viewmodel

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.almex.asistente.AlmexApplication
import com.almex.asistente.R
import com.almex.asistente.data.database.ReminderType
import com.almex.asistente.data.database.ScheduleEntity
import com.almex.asistente.data.repository.ValidationResult
import kotlinx.coroutines.launch
import java.util.*

class HoracioViewModel : ViewModel() {
    
    private val scheduleRepository = AlmexApplication.instance.scheduleRepository
    
    private val _selectedDay = MutableLiveData<Int>()
    val selectedDay: LiveData<Int> = _selectedDay
    
    private val _selectedDaySchedules = MutableLiveData<List<ScheduleEntity>>()
    val selectedDaySchedules: LiveData<List<ScheduleEntity>> = _selectedDaySchedules
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    private val _successMessage = MutableLiveData<String>()
    val successMessage: LiveData<String> = _successMessage
    
    init {
        // Seleccionar día actual por defecto
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
        selectDay(dayOfWeek)
    }
    
    fun selectDay(dayOfWeek: Int) {
        _selectedDay.value = dayOfWeek
        loadSchedulesForDay(dayOfWeek)
    }
    
    private fun loadSchedulesForDay(dayOfWeek: Int) {
        viewModelScope.launch {
            scheduleRepository.getSchedulesForDay(dayOfWeek).collect { schedules ->
                _selectedDaySchedules.value = schedules
            }
        }
    }
    
    fun showAddScheduleDialog(context: Context) {
        showScheduleDialog(context, null)
    }
    
    fun editSchedule(context: Context, schedule: ScheduleEntity) {
        showScheduleDialog(context, schedule)
    }
    
    private fun showScheduleDialog(context: Context, existingSchedule: ScheduleEntity?) {
        val dialogView = android.view.LayoutInflater.from(context)
            .inflate(R.layout.dialog_add_schedule, null)
        
        val etActionName = dialogView.findViewById<EditText>(R.id.et_action_name)
        val etObjective = dialogView.findViewById<EditText>(R.id.et_objective)
        val btnStartTime = dialogView.findViewById<Button>(R.id.btn_start_time)
        val btnEndTime = dialogView.findViewById<Button>(R.id.btn_end_time)
        val spinnerReminderType = dialogView.findViewById<Spinner>(R.id.spinner_reminder_type)
        
        var selectedStartTime = ""
        var selectedEndTime = ""
        
        // Configurar spinner de tipo de recordatorio
        val reminderTypes = arrayOf("Notificación", "Voz (Almex)", "Ambos")
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, reminderTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerReminderType.adapter = adapter
        
        // Si es edición, llenar campos
        existingSchedule?.let { schedule ->
            etActionName.setText(schedule.actionName)
            etObjective.setText(schedule.objective)
            selectedStartTime = schedule.startTime
            selectedEndTime = schedule.endTime
            btnStartTime.text = schedule.startTime
            btnEndTime.text = schedule.endTime
            
            val reminderIndex = when(schedule.reminderType) {
                ReminderType.NOTIFICATION -> 0
                ReminderType.VOICE -> 1
                ReminderType.BOTH -> 2
            }
            spinnerReminderType.setSelection(reminderIndex)
        }
        
        // Configurar selectores de tiempo
        btnStartTime.setOnClickListener {
            showTimePicker(context) { time ->
                selectedStartTime = time
                btnStartTime.text = time
            }
        }
        
        btnEndTime.setOnClickListener {
            showTimePicker(context) { time ->
                selectedEndTime = time
                btnEndTime.text = time
            }
        }
        
        val dialog = AlertDialog.Builder(context)
            .setTitle(if (existingSchedule == null) "Agregar Acción" else "Editar Acción")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val actionName = etActionName.text.toString().trim()
                val objective = etObjective.text.toString().trim()
                val reminderType = when(spinnerReminderType.selectedItemPosition) {
                    0 -> ReminderType.NOTIFICATION
                    1 -> ReminderType.VOICE
                    2 -> ReminderType.BOTH
                    else -> ReminderType.NOTIFICATION
                }
                
                if (validateInput(actionName, selectedStartTime, selectedEndTime)) {
                    saveSchedule(
                        existingSchedule?.id ?: 0,
                        actionName,
                        objective,
                        selectedStartTime,
                        selectedEndTime,
                        reminderType
                    )
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
        
        dialog.show()
    }
    
    private fun showTimePicker(context: Context, onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(context, { _, selectedHour, selectedMinute ->
            val time = String.format("%02d:%02d", selectedHour, selectedMinute)
            onTimeSelected(time)
        }, hour, minute, true).show()
    }
    
    private fun validateInput(actionName: String, startTime: String, endTime: String): Boolean {
        return when {
            actionName.isEmpty() -> {
                _errorMessage.value = "El nombre de la acción es obligatorio"
                false
            }
            startTime.isEmpty() -> {
                _errorMessage.value = "Selecciona la hora de inicio"
                false
            }
            endTime.isEmpty() -> {
                _errorMessage.value = "Selecciona la hora de fin"
                false
            }
            else -> true
        }
    }
    
    private fun saveSchedule(
        id: Long,
        actionName: String,
        objective: String,
        startTime: String,
        endTime: String,
        reminderType: ReminderType
    ) {
        viewModelScope.launch {
            val currentDay = _selectedDay.value ?: 1
            
            val schedule = ScheduleEntity(
                id = id,
                dayOfWeek = currentDay,
                actionName = actionName,
                objective = objective,
                startTime = startTime,
                endTime = endTime,
                reminderType = reminderType
            )
            
            val result = scheduleRepository.saveSchedule(schedule)
            
            result.fold(
                onSuccess = {
                    _successMessage.value = if (id == 0L) "Acción agregada" else "Acción actualizada"
                    clearMessages()
                },
                onFailure = { exception ->
                    _errorMessage.value = exception.message ?: "Error al guardar"
                    clearMessages()
                }
            )
        }
    }
    
    fun deleteSchedule(schedule: ScheduleEntity) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(schedule)
            _successMessage.value = "Acción eliminada"
            clearMessages()
        }
    }
    
    private fun clearMessages() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _errorMessage.value = ""
            _successMessage.value = ""
        }
    }
}