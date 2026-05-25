package com.example.data.room

import androidx.room.*
import com.example.data.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSessionDao {
    @Query("SELECT * FROM user_sessions WHERE id = 'current' LIMIT 1")
    fun getSessionFlow(): Flow<UserSession?>

    @Query("SELECT * FROM user_sessions WHERE id = 'current' LIMIT 1")
    suspend fun getSessionDirect(): UserSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UserSession)

    @Query("DELETE FROM user_sessions")
    suspend fun clearSession()
}

@Dao
interface VideoDraftDao {
    @Query("SELECT * FROM video_drafts ORDER BY timestamp DESC")
    fun getAllDraftsFlow(): Flow<List<VideoDraft>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: VideoDraft)

    @Query("DELETE FROM video_drafts WHERE id = :id")
    suspend fun deleteDraftById(id: Int)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE (senderUsername = :u1 AND receiverUsername = :u2) OR (senderUsername = :u2 AND receiverUsername = :u1) ORDER BY timestamp ASC")
    fun getMessagesFlow(u1: String, u2: String): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("UPDATE chat_messages SET isSeen = 1 WHERE senderUsername = :sender AND receiverUsername = :receiver")
    suspend fun markMessagesAsSeen(sender: String, receiver: String)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllMessagesFlow(): Flow<List<ChatMessage>>
}

@Dao
interface WalletTxDao {
    @Query("SELECT * FROM wallet_txs ORDER BY timestamp DESC")
    fun getAllTxsFlow(): Flow<List<WalletTx>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTx(tx: WalletTx)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotificationsFlow(): Flow<List<NotificationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(note: NotificationItem)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()
}

@Dao
interface CachedVideoDao {
    @Query("SELECT * FROM cached_videos")
    fun getAllVideosFlow(): Flow<List<CachedVideo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<CachedVideo>)

    @Update
    suspend fun updateVideo(video: CachedVideo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: CachedVideo)

    @Delete
    suspend fun deleteVideo(video: CachedVideo)
}
