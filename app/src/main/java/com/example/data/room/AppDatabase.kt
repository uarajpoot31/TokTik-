package com.example.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.*

@Database(
    entities = [
        UserSession::class,
        VideoDraft::class,
        ChatMessage::class,
        WalletTx::class,
        NotificationItem::class,
        CachedVideo::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userSessionDao(): UserSessionDao
    abstract fun videoDraftDao(): VideoDraftDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun walletTxDao(): WalletTxDao
    abstract fun notificationDao(): NotificationDao
    abstract fun cachedVideoDao(): CachedVideoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "toktik_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
