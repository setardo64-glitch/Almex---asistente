package com.almex.asistente.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.almex.asistente.databinding.ActivityHoracioBinding
import com.almex.asistente.ui.adapter.DayScheduleAdapter
import com.almex.asistente.ui.viewmodel.HoracioViewModel
import com.almex.asistente.service.ReminderService

class HoracioActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityHoracioBinding
    private lateinit var viewModel: HoracioViewModel
    private lateinit var adapter: DayScheduleAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHoracioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[HoracioViewModel::class.java]
        
        setupUI()
        setupRecyclerView()
        observeViewModel()
        
        // Iniciar servicio de recordatorios
        ReminderService.start(this)
    }
    
    private fun setupUI() {
        binding.apply {
            toolbar.setNavigationOnClickListener { finish() }
            
            fabAddSchedule.setOnClickListener {
                viewModel.showAddScheduleDialog(this@HoracioActivity)
            }
            
            // Configurar selector de días
            setupDaySelector()
        }
    }
    
    private fun setupDaySelector() {
        val dayButtons = listOf(
            binding.btnMonday,
            binding.btnTuesday,
            binding.btnWednesday,
            binding.btnThursday,
            binding.btnFriday,
            binding.btnSaturday,
            binding.btnSunday
        )
        
        dayButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                viewModel.selectDay(index + 1)
                updateDayButtonsState(index)
            }
        }
        
        // Seleccionar día actual por defecto
        val today = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
        val todayIndex = when(today) {
            java.util.Calendar.MONDAY -> 0
            java.util.Calendar.TUESDAY -> 1
            java.util.Calendar.WEDNESDAY -> 2
            java.util.Calendar.THURSDAY -> 3
            java.util.Calendar.FRIDAY -> 4
            java.util.Calendar.SATURDAY -> 5
            java.util.Calendar.SUNDAY -> 6
            else -> 0
        }
        
        viewModel.selectDay(todayIndex + 1)
        updateDayButtonsState(todayIndex)
    }
    
    private fun updateDayButtonsState(selectedIndex: Int) {
        val dayButtons = listOf(
            binding.btnMonday,
            binding.btnTuesday,
            binding.btnWednesday,
            binding.btnThursday,
            binding.btnFriday,
            binding.btnSaturday,
            binding.btnSunday
        )
        
        dayButtons.forEachIndexed { index, button ->
            button.isSelected = index == selectedIndex
        }
    }
    
    private fun setupRecyclerView() {
        adapter = DayScheduleAdapter(
            onEditClick = { schedule ->
                viewModel.editSchedule(this, schedule)
            },
            onDeleteClick = { schedule ->
                viewModel.deleteSchedule(schedule)
            }
        )
        
        binding.recyclerViewSchedules.apply {
            layoutManager = LinearLayoutManager(this@HoracioActivity)
            adapter = this@HoracioActivity.adapter
        }
    }
    
    private fun observeViewModel() {
        viewModel.selectedDaySchedules.observe(this) { schedules ->
            adapter.submitList(schedules)
            
            binding.emptyStateText.visibility = if (schedules.isEmpty()) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
        
        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
        
        viewModel.successMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}