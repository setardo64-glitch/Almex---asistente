package com.almex.asistente.document

import android.content.Context
import com.almex.asistente.data.database.DocumentType
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.TextAlignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class DocumentGenerator(private val context: Context) {
    
    companion object {
        private const val DOCUMENTS_FOLDER = "AlmexDocuments"
    }
    
    private fun getDocumentsDirectory(): File {
        val documentsDir = File(context.getExternalFilesDir(null), DOCUMENTS_FOLDER)
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }
        return documentsDir
    }
    
    suspend fun generatePDF(
        title: String,
        content: String,
        fileName: String? = null
    ): DocumentGenerationResult = withContext(Dispatchers.IO) {
        try {
            val actualFileName = fileName ?: generateFileName(title, DocumentType.PDF)
            val file = File(getDocumentsDirectory(), actualFileName)
            
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)
            
            // Título del documento
            val titleParagraph = Paragraph(title)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(18f)
                .setBold()
            document.add(titleParagraph)
            
            // Fecha de creación
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val dateParagraph = Paragraph("Creado: ${dateFormat.format(Date())}")
                .setTextAlignment(TextAlignment.RIGHT)
                .setFontSize(10f)
                .setItalic()
            document.add(dateParagraph)
            
            // Espacio
            document.add(Paragraph("\n"))
            
            // Contenido principal
            val contentParagraphs = content.split("\n\n")
            contentParagraphs.forEach { paragraph ->
                if (paragraph.trim().isNotEmpty()) {
                    document.add(Paragraph(paragraph.trim()).setFontSize(12f))
                }
            }
            
            // Pie de página
            document.add(Paragraph("\n\n"))
            document.add(
                Paragraph("Generado por Almex Asistente")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(8f)
                    .setItalic()
            )
            
            document.close()
            
            DocumentGenerationResult.Success(
                filePath = file.absolutePath,
                fileName = actualFileName,
                fileSize = file.length(),
                wordCount = countWords(content)
            )
            
        } catch (e: Exception) {
            DocumentGenerationResult.Error("Error generando PDF: ${e.message}")
        }
    }
    
    suspend fun generateDOCX(
        title: String,
        content: String,
        fileName: String? = null
    ): DocumentGenerationResult = withContext(Dispatchers.IO) {
        try {
            val actualFileName = fileName ?: generateFileName(title, DocumentType.DOCX)
            val file = File(getDocumentsDirectory(), actualFileName)
            
            val document = XWPFDocument()
            
            // Título del documento
            val titleParagraph = document.createParagraph()
            val titleRun = titleParagraph.createRun()
            titleRun.setText(title)
            titleRun.isBold = true
            titleRun.fontSize = 18
            titleParagraph.alignment = org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER
            
            // Fecha de creación
            val dateParagraph = document.createParagraph()
            val dateRun = dateParagraph.createRun()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            dateRun.setText("Creado: ${dateFormat.format(Date())}")
            dateRun.isItalic = true
            dateRun.fontSize = 10
            dateParagraph.alignment = org.apache.poi.xwpf.usermodel.ParagraphAlignment.RIGHT
            
            // Espacio
            document.createParagraph()
            
            // Contenido principal
            val contentParagraphs = content.split("\n\n")
            contentParagraphs.forEach { paragraphText ->
                if (paragraphText.trim().isNotEmpty()) {
                    val paragraph = document.createParagraph()
                    val run = paragraph.createRun()
                    run.setText(paragraphText.trim())
                    run.fontSize = 12
                }
            }
            
            // Pie de página
            document.createParagraph()
            val footerParagraph = document.createParagraph()
            val footerRun = footerParagraph.createRun()
            footerRun.setText("Generado por Almex Asistente")
            footerRun.isItalic = true
            footerRun.fontSize = 8
            footerParagraph.alignment = org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER
            
            // Guardar archivo
            val fileOut = FileOutputStream(file)
            document.write(fileOut)
            fileOut.close()
            document.close()
            
            DocumentGenerationResult.Success(
                filePath = file.absolutePath,
                fileName = actualFileName,
                fileSize = file.length(),
                wordCount = countWords(content)
            )
            
        } catch (e: Exception) {
            DocumentGenerationResult.Error("Error generando DOCX: ${e.message}")
        }
    }
    
    suspend fun generateTXT(
        title: String,
        content: String,
        fileName: String? = null
    ): DocumentGenerationResult = withContext(Dispatchers.IO) {
        try {
            val actualFileName = fileName ?: generateFileName(title, DocumentType.TXT)
            val file = File(getDocumentsDirectory(), actualFileName)
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fullContent = buildString {
                appendLine(title)
                appendLine("=" * title.length)
                appendLine()
                appendLine("Creado: ${dateFormat.format(Date())}")
                appendLine()
                appendLine(content)
                appendLine()
                appendLine("---")
                appendLine("Generado por Almex Asistente")
            }
            
            file.writeText(fullContent)
            
            DocumentGenerationResult.Success(
                filePath = file.absolutePath,
                fileName = actualFileName,
                fileSize = file.length(),
                wordCount = countWords(content)
            )
            
        } catch (e: Exception) {
            DocumentGenerationResult.Error("Error generando TXT: ${e.message}")
        }
    }
    
    suspend fun readDocumentContent(filePath: String): String = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            when {
                filePath.endsWith(".txt") -> file.readText()
                filePath.endsWith(".pdf") -> readPDFContent(file)
                filePath.endsWith(".docx") -> readDOCXContent(file)
                else -> "Formato de archivo no soportado"
            }
        } catch (e: Exception) {
            "Error leyendo archivo: ${e.message}"
        }
    }
    
    private fun readPDFContent(file: File): String {
        // Implementación simplificada - en producción usarías iText para extraer texto
        return "Contenido PDF (requiere implementación completa de extracción)"
    }
    
    private fun readDOCXContent(file: File): String {
        return try {
            val document = XWPFDocument(file.inputStream())
            val content = StringBuilder()
            
            document.paragraphs.forEach { paragraph ->
                content.appendLine(paragraph.text)
            }
            
            document.close()
            content.toString()
        } catch (e: Exception) {
            "Error leyendo DOCX: ${e.message}"
        }
    }
    
    private fun generateFileName(title: String, type: DocumentType): String {
        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\s]"), "")
            .replace(Regex("\\s+"), "_")
            .take(30)
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "${sanitizedTitle}_${timestamp}${type.extension}"
    }
    
    private fun countWords(text: String): Int {
        return text.trim().split(Regex("\\s+")).size
    }
    
    fun getDocumentsFolder(): File = getDocumentsDirectory()
    
    suspend fun deleteDocument(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            file.delete()
        } catch (e: Exception) {
            false
        }
    }
}

sealed class DocumentGenerationResult {
    data class Success(
        val filePath: String,
        val fileName: String,
        val fileSize: Long,
        val wordCount: Int
    ) : DocumentGenerationResult()
    
    data class Error(val message: String) : DocumentGenerationResult()
}