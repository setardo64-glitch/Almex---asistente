package com.almex.asistente

import android.app.Application
import com.almex.asistente.data.database.AlmexDatabase
import com.almex.asistente.data.repository.ConversationRepository
import com.almex.asistente.data.repository.ScheduleRepository
import com.almex.asistente.data.repository.DocumentRepository
import com.almex.asistente.document.DocumentGenerator

class AlmexApplication : Application() {
    
    val database by lazy { AlmexDatabase.getDatabase(this) }
    val conversationRepository by lazy { ConversationRepository(database.conversationDao()) }
    val scheduleRepository by lazy { ScheduleRepository(database.scheduleDao()) }
    val documentRepository by lazy { 
        DocumentRepository(
            database.documentDao(), 
            database.documentChatDao(),
            DocumentGenerator(this)
        ) 
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: AlmexApplication
            private set
    }
}