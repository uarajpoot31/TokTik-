package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey val id: String = "current",
    val username: String = "guest_user",
    val bio: String = "Enjoying TokTik!",
    val profilePicUrl: String = "",
    val followers: Int = 142,
    val following: Int = 89,
    val coins: Int = 500,
    val points: Int = 0,
    val earningsRs: Double = 0.0,
    val isVerified: Boolean = false,
    val isPrivate: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isGuest: Boolean = true,
    val isAdmin: Boolean = false,
    val isBlocked: Boolean = false,
    val socialLinks: String = "instagram.com/toktik,twitter.com/toktik"
)

@Entity(tableName = "video_drafts")
data class VideoDraft(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val caption: String,
    val hashtags: String,
    val musicTitle: String,
    val path: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderUsername: String,
    val receiverUsername: String,
    val messageText: String,
    val mediaType: String? = null, // "image", "voice", "video"
    val mediaUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSeen: Boolean = false
)

@Entity(tableName = "wallet_txs")
data class WalletTx(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val description: String,
    val amount: Int,
    val isCredit: Boolean, // True if added, False if spent (gifted)
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "like", "comment", "follow", "live", "system"
    val title: String,
    val body: String,
    val userAvatar: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "cached_videos")
data class CachedVideo(
    @PrimaryKey val id: String,
    val username: String,
    val userAvatar: String,
    val videoUrl: String,
    val caption: String,
    val hashtags: String,
    val musicName: String,
    val likesCount: Int,
    val commentsCount: Int,
    val sharesCount: Int,
    val viewsCount: Int = 12500,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isVerified: Boolean = false,
    val isPremium: Boolean = false,
    val isBlocked: Boolean = false,
    val commentsJson: String = "[]" // Store localized mock comments
)
