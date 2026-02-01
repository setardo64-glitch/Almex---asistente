package com.almex.asistente.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.almex.asistente.R
import com.almex.asistente.data.database.DocumentType
import com.almex.asistente.databinding.ActivityDocumentsBinding
import com.almex.asistente.ui.adapter.DocumentsAdapter
import com.almex.asistente.ui.viewmodel.DocumentsViewModel

class DocumentsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDocumentsBinding
    private lateinit var viewModel: DocumentsViewModel
    private lateinit var adapter: DocumentsAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocumentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        viewModel = ViewModelProvider(this)[DocumentsViewModel::class.java]
        
        setupUI()
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupUI() {
        binding.apply {
            toolbar.setNavigationOnClickListener { finish() }
            
            fabCreateDocument.setOnClickListener {
                showCreateDocumentDialog()
            }
            
            // Filtros por tipo de documento
            chipAll.setOnClickListener { viewModel.filterByType(null) }
            chipPdf.setOnClickListener { viewModel.filterByType(DocumentType.PDF) }
            chipDocx.setOnClickListener { viewModel.filterByType(DocumentType.DOCX) }
            chipTxt.setOnClickListener { viewModel.filterByType(DocumentType.TXT) }
            
            // Búsqueda
            searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let { viewModel.searchDocuments(it) }
                    return true
                }
                
                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText.isNullOrEmpty()) {
                        viewModel.clearSearch()
                    }
                    return true
                }
            })
        }
    }
    
    private fun setupRecyclerView() {
        adapter = DocumentsAdapter(
            onDocumentClick = { document ->
                val intent = Intent(this, DocumentChatActivity::class.java)
                intent.putExtra("document_id", document.id)
                startActivity(intent)
            },
            onDeleteClick = { document ->
                showDeleteConfirmation(document.title) {
                    viewModel.deleteDocument(document)
                }
            }
        )
        
        binding.recyclerViewDocuments.apply {
            layoutManager = LinearLayoutManager(this@DocumentsActivity)
            adapter = this@DocumentsActivity.adapter
        }
    }
    
    private fun observeViewModel() {
        viewModel.documents.observe(this) { documents ->
            adapter.submitList(documents)
            
            binding.emptyStateText.visibility = if (documents.isEmpty()) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }
        
        viewModel.storageInfo.observe(this) { info ->
            binding.storageInfo.text = "Documentos: ${info.documentCount} | Espacio: ${"%.1f".format(info.totalSizeMB)} MB"
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
    
    private fun showCreateDocumentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_document, null)
        val etTitle = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_document_title)
        val etPrompt = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_document_prompt)
        val spinnerType = dialogView.findViewById<android.widget.Spinner>(R.id.spinner_document_type)
        
        // Configurar spinner de tipos
        val types = arrayOf("PDF", "Word (DOCX)", "Texto (TXT)")
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerType.adapter = adapter
        
        AlertDialog.Builder(this)
            .setTitle("Crear Documento")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val title = etTitle.text.toString().trim()
                val prompt = etPrompt.text.toString().trim()
                val typeIndex = spinnerType.selectedItemPosition
                
                val documentType = when (typeIndex) {
                    0 -> DocumentType.PDF
                    1 -> DocumentType.DOCX
                    2 -> DocumentType.TXT
                    else -> DocumentType.PDF
                }
                
                if (title.isNotEmpty() && prompt.isNotEmpty()) {
                    viewModel.createDocument(title, prompt, documentType)
                } else {
                    Toast.makeText(this, "Título y descripción son obligatorios", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    
    private fun showDeleteConfirmation(documentTitle: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Documento")
            .setMessage("¿Estás seguro de que quieres eliminar '$documentTitle'?")
            .setPositiveButton("Eliminar") { _, _ -> onConfirm() }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}