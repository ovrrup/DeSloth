package com.desloth.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.desloth.app.data.local.dao.DeliverableDao
import com.desloth.app.data.local.dao.FactVaultDao
import com.desloth.app.data.local.dao.TargetAppDao
import com.desloth.app.data.local.entities.DeliverableEntity
import com.desloth.app.data.local.entities.FactVaultEntity
import com.desloth.app.data.local.entities.TargetAppEntity

@Database(
    entities = [
        TargetAppEntity::class,
        DeliverableEntity::class,
        FactVaultEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DeSlothDatabase : RoomDatabase() {

    abstract fun targetAppDao(): TargetAppDao
    abstract fun deliverableDao(): DeliverableDao
    abstract fun factVaultDao(): FactVaultDao

    companion object {
        private const val DATABASE_NAME = "desloth_local.db"

        @Volatile
        private var INSTANCE: DeSlothDatabase? = null

        fun buildDatabase(context: Context): DeSlothDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DeSlothDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
