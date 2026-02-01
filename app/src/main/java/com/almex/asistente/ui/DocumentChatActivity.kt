package com.almex.asistente.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.almex.asistente.databinding.ActivityDocumentChatBinding
import com.almex.asistente.ui.adapter.DocumentChatAdapter
import com.almex.asistente.ui.viewmodel.DocumentChatViewModel
import java.io.File

class DocumentChatActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDocumentChatBinding
    private lateinit var viewModel: DocumentChatViewModel
    private lateinit var adapter: DocumentChatAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val documentId = intent.getLongExtra("document_id", -1)
        if (documentId == -1L) {
            finish()
            return
        }
        
        viewModel = ViewModelProvider(this)[DocumentChatViewModel::class.java]
        viewModel.loadDocument(documentId)
        
        setupUI()
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupUI() {
        binding.apply {
            toolbar.setNavigationOnClickListener { finish() }
            
            btnSendMessage.setOnClickListener {
                val message = etChatInput.text.toString().trim()
                if (message.isNotEmpty()) {
                    viewModel.sendMessage(message)
                    etChatInput.text?.clear()
                }
            }
            
            btnOpenDocument.setOnClickListener {
                viewModel.currentDocument.value?.let { document ->
                    openDocument(document.filePath)
                }
            }
            
            btnSummarizeDocument.setOnClickListener {
                viewModel.summarizeDocument()
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = DocumentChatAdapter()
        
        binding.recyclerViewChat.apply {
            layoutManager = LinearLayoutManager(this@DocumentChatActivity).apply {
                stackFromEnd = true
            }
            adapter = this@DocumentChatActivity.adapter
        }
    }
    
    private fun observeViewModel() {
        viewModel.currentDocument.observe(this) { document ->
            document?.let {
                binding.toolbar.title = it.title
                binding.documentInfo.text = "${it.fileType.name} • ${it.wordCount} palabras"
            }
        }
        
        viewModel.chatMessages.observe(this) { messages ->
            adapter.submitList(messages) {
                // Scroll al último mensaje
                if (messages.isNotEmpty()) {
                    binding.recyclerViewChat.scrollToPosition(messages.size - 1)
                }
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
            
            binding.btnSendMessage.isEnabled = !isLoading
        }
        
        viewModel.errorMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun openDocument(filePath: String) {
        try {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, getMimeType(filePath))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "No hay aplicación disponible para abrir este archivo", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error abriendo documento: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun getMimeType(filePath: String): String {
        return when {
            filePath.endsWith(".pdf") -> "application/pdf"
            filePath.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            filePath.endsWith(".txt") -> "text/plain"
            else -> "*/*"
        }
    }
}