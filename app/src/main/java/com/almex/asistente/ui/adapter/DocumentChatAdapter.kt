package com.almex.asistente.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.almex.asistente.data.database.ChatActionType
import com.almex.asistente.data.database.DocumentChatEntity
import com.almex.asistente.databinding.ItemChatMessageBinding
import java.text.SimpleDateFormat
import java.util.*

class DocumentChatAdapter : ListAdapter<DocumentChatEntity, DocumentChatAdapter.ChatViewHolder>(ChatDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ChatViewHolder(
        private val binding: ItemChatMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(message: DocumentChatEntity) {
            binding.apply {
                tvMessage.text = message.message
                
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                tvTimestamp.text = timeFormat.format(Date(message.timestamp))
                
                // Configurar apariencia según el remitente
                if (message.isUser) {
                    // Mensaje del usuario
                    messageContainer.setBackgroundResource(com.almex.asistente.R.drawable.bg_message_user)
                    tvMessage.setTextColor(
                        itemView.context.getColor(com.almex.asistente.R.color.text_primary)
                    )
                    
                    // Alinear a la derecha
                    val params = messageContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                    params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    params.horizontalBias = 1.0f
                    messageContainer.layoutParams = params
                    
                } else {
                    // Mensaje de la IA
                    messageContainer.setBackgroundResource(com.almex.asistente.R.drawable.bg_message_ai)
                    tvMessage.setTextColor(
                        itemView.context.getColor(com.almex.asistente.R.color.text_primary)
                    )
                    
                    // Alinear a la izquierda
                    val params = messageContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
                    params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                    params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                    params.horizontalBias = 0.0f
                    messageContainer.layoutParams = params
                }
                
                // Mostrar indicador de tipo de acción
                val actionType = try {
                    ChatActionType.valueOf(message.actionType)
                } catch (e: Exception) {
                    ChatActionType.MESSAGE
                }
                
                val actionIndicator = when (actionType) {
                    ChatActionType.GENERATION -> "🎯 "
                    ChatActionType.EDIT_REQUEST -> "✏️ "
                    ChatActionType.SUMMARY -> "📋 "
                    ChatActionType.QUESTION -> if (message.isUser) "❓ " else "💡 "
                    ChatActionType.MESSAGE -> ""
                }
                
                if (actionIndicator.isNotEmpty() && !message.message.startsWith(actionIndicator)) {
                    tvMessage.text = "$actionIndicator${message.message}"
                }
            }
        }
    }
}

class ChatDiffCallback : DiffUtil.ItemCallback<DocumentChatEntity>() {
    override fun areItemsTheSame(oldItem: DocumentChatEntity, newItem: DocumentChatEntity): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: DocumentChatEntity, newItem: DocumentChatEntity): Boolean {
        return oldItem == newItem
    }
}