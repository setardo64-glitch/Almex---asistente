package com.almex.asistente.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.almex.asistente.data.database.DocumentEntity
import com.almex.asistente.data.database.DocumentType
import com.almex.asistente.databinding.ItemDocumentBinding
import java.text.SimpleDateFormat
import java.util.*

class DocumentsAdapter(
    private val onDocumentClick: (DocumentEntity) -> Unit,
    private val onDeleteClick: (DocumentEntity) -> Unit
) : ListAdapter<DocumentEntity, DocumentsAdapter.DocumentViewHolder>(DocumentDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val binding = ItemDocumentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DocumentViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class DocumentViewHolder(
        private val binding: ItemDocumentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(document: DocumentEntity) {
            binding.apply {
                tvDocumentTitle.text = document.title
                tvDocumentPreview.text = document.contentPreview
                
                // Información del documento
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                tvDocumentInfo.text = buildString {
                    append("${document.fileType.name} • ")
                    append("${document.wordCount} palabras • ")
                    append(formatFileSize(document.fileSize))
                }
                
                tvDocumentDate.text = dateFormat.format(Date(document.lastModified))
                
                // Icono según tipo de archivo
                val iconRes = when (document.fileType) {
                    DocumentType.PDF -> com.almex.asistente.R.drawable.ic_pdf
                    DocumentType.DOCX -> com.almex.asistente.R.drawable.ic_docx
                    DocumentType.TXT -> com.almex.asistente.R.drawable.ic_txt
                }
                ivDocumentIcon.setImageResource(iconRes)
                
                // Listeners
                root.setOnClickListener { onDocumentClick(document) }
                btnDeleteDocument.setOnClickListener { onDeleteClick(document) }
            }
        }
        
        private fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
            }
        }
    }
}

class DocumentDiffCallback : DiffUtil.ItemCallback<DocumentEntity>() {
    override fun areItemsTheSame(oldItem: DocumentEntity, newItem: DocumentEntity): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: DocumentEntity, newItem: DocumentEntity): Boolean {
        return oldItem == newItem
    }
}