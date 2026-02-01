package com.almex.asistente.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationEntity::class, ScheduleEntity::class, DocumentEntity::class, DocumentChatEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AlmexDatabase : RoomDatabase() {
    
    abstract fun conversationDao(): ConversationDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun documentDao(): DocumentDao
    abstract fun documentChatDao(): DocumentChatDao
    
    companion object {
        @Volatile
        private var INSTANCE: AlmexDatabase? = null
        
        fun getDatabase(context: Context): AlmexDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlmexDatabase::class.java,
                    "almex_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}