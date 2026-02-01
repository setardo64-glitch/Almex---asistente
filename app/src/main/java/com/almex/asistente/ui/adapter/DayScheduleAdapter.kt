package com.almex.asistente.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.almex.asistente.data.database.ReminderType
import com.almex.asistente.data.database.ScheduleEntity
import com.almex.asistente.databinding.ItemScheduleBinding

class DayScheduleAdapter(
    private val onEditClick: (ScheduleEntity) -> Unit,
    private val onDeleteClick: (ScheduleEntity) -> Unit
) : ListAdapter<ScheduleEntity, DayScheduleAdapter.ScheduleViewHolder>(ScheduleDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemScheduleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScheduleViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ScheduleViewHolder(
        private val binding: ItemScheduleBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(schedule: ScheduleEntity) {
            binding.apply {
                tvActionName.text = schedule.actionName
                tvTimeRange.text = "${schedule.startTime} - ${schedule.endTime}"
                tvObjective.text = schedule.objective
                
                // Mostrar tipo de recordatorio
                val reminderText = when(schedule.reminderType) {
                    ReminderType.NOTIFICATION -> "🔔 Notificación"
                    ReminderType.VOICE -> "🎤 Voz"
                    ReminderType.BOTH -> "🔔🎤 Ambos"
                }
                tvReminderType.text = reminderText
                
                // Ocultar objetivo si está vacío
                tvObjective.visibility = if (schedule.objective.isEmpty()) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }
                
                // Configurar botones
                btnEdit.setOnClickListener { onEditClick(schedule) }
                btnDelete.setOnClickListener { onDeleteClick(schedule) }
                
                // Click en toda la tarjeta para editar
                root.setOnClickListener { onEditClick(schedule) }
            }
        }
    }
}

class ScheduleDiffCallback : DiffUtil.ItemCallback<ScheduleEntity>() {
    override fun areItemsTheSame(oldItem: ScheduleEntity, newItem: ScheduleEntity): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: ScheduleEntity, newItem: ScheduleEntity): Boolean {
        return oldItem == newItem
    }
}