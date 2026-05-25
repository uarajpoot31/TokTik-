package com.example.data.room

import android.content.Context
import android.util.Log
import com.example.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class MessageComment(
    val id: String,
    val username: String,
    val avatar: String,
    val commentText: String,
    val timestamp: String,
    val likes: Int,
    val isLiked: Boolean = false
)

class TokTikRepository(val db: AppDatabase) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val commentsAdapter = moshi.adapter<List<MessageComment>>(
        Types.newParameterizedType(List::class.java, MessageComment::class.java)
    )

    // Flows
    val session: Flow<UserSession?> = db.userSessionDao().getSessionFlow()
    val drafts: Flow<List<VideoDraft>> = db.videoDraftDao().getAllDraftsFlow()
    val notifications: Flow<List<NotificationItem>> = db.notificationDao().getAllNotificationsFlow()
    val videos: Flow<List<CachedVideo>> = db.cachedVideoDao().getAllVideosFlow()
    val allMessages: Flow<List<ChatMessage>> = db.chatMessageDao().getAllMessagesFlow()
    val walletTxs: Flow<List<WalletTx>> = db.walletTxDao().getAllTxsFlow()

    fun getChatMessages(sender: String, receiver: String): Flow<List<ChatMessage>> {
        return db.chatMessageDao().getMessagesFlow(sender, receiver)
    }

    // Initialize Default Seed Data if database is empty
    suspend fun checkAndSeedDatabase() {
        // 1. Seed user session if absent
        val currentSession = db.userSessionDao().getSessionDirect()
        if (currentSession == null) {
            db.userSessionDao().insertSession(
                UserSession(
                    id = "current",
                    username = "lucy_toktik",
                    bio = "TokTik Creator Studio ✨ Professional Animator & Dancer 💃 Collabs open!",
                    profilePicUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200&auto=format&fit=crop",
                    followers = 12500,
                    following = 345,
                    coins = 850,
                    points = 250,
                    earningsRs = 25.0,
                    isVerified = true,
                    isPrivate = false,
                    isLoggedIn = false,
                    isGuest = false,
                    socialLinks = "instagram.com/lucydances,youtube.com/lucytv"
                )
            )
        }

        // 2. Seed Default Videos
        val currentVideos = db.cachedVideoDao().getAllVideosFlow().firstOrNull() ?: emptyList()
        if (currentVideos.isEmpty()) {
            val defaultVideos = listOf(
                CachedVideo(
                    id = "vid_1",
                    username = "neon_rider",
                    userAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=200&auto=format&fit=crop",
                    videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-retro-futuristic-car-driving-through-a-synthwave-grid-34448-large.mp4",
                    caption = "Cruising through the synthwave grids in neon mode! 🏎️⚡ Who's up for a virtual road trip? Tell me in the comments!",
                    hashtags = "#synthwave #cyberpunk #toktik #retro #speedy",
                    musicName = "Original Sound - Retro Synth - Sunset Ride",
                    likesCount = 2390,
                    commentsCount = 45,
                    sharesCount = 512,
                    isLiked = false,
                    isSaved = false,
                    isVerified = true,
                    commentsJson = commentsAdapter.toJson(listOf(
                        MessageComment("c1", "tech_guru", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=100", "This transition is insane! Grid styling looks extremely premium.", "2h ago", 24),
                        MessageComment("c2", "cyber_alice", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=100", "Neon car model is so clean! Loving this retro futuristic loop.", "4h ago", 12)
                    ))
                ),
                CachedVideo(
                    id = "vid_2",
                    username = "chef_elite",
                    userAvatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?q=80&w=200&auto=format&fit=crop",
                    videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-freshly-baked-pizza-being-sliced-close-up-42419-large.mp4",
                    caption = "Perfect artisan pepperoni freshly baked in 90 seconds! 🍕🔥 Rate this crispiness 1-10!",
                    hashtags = "#pizza #chefskills #cooking #asmrfood #delicious",
                    musicName = "Original Sound - Chef Elite - Italian Classic",
                    likesCount = 4850,
                    commentsCount = 112,
                    sharesCount = 94,
                    isLiked = true, // Default interactive liked
                    isSaved = true,
                    isVerified = true,
                    commentsJson = commentsAdapter.toJson(listOf(
                        MessageComment("c3", "foodie_bob", "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?q=80&w=100", "That crust rise is absolutely perfect, real wood-fired style!", "1h ago", 64),
                        MessageComment("c4", "italians_do_it", "https://images.unsplash.com/photo-1560250097-0b93528c311a?q=80&w=100", "Approved by Rome! Send a slice immediately! 🇮🇹🍕", "3h ago", 45)
                    ))
                ),
                CachedVideo(
                    id = "vid_3",
                    username = "golden_paws",
                    userAvatar = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?q=80&w=200&auto=format&fit=crop",
                    videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-excited-golden-retriever-puppy-running-on-grass-41584-large.mp4",
                    caption = "First day in the park! 🐾🎾 Everything is so big and green! Can we stay forever?",
                    hashtags = "#puppy #goldenretriever #cuteanimals #dogsoftoktik #joyful",
                    musicName = "Original Sound - Sweet Melody - Cute Whistles",
                    likesCount = 9200,
                    commentsCount = 230,
                    sharesCount = 1200,
                    isLiked = false,
                    isSaved = false,
                    isVerified = false,
                    commentsJson = commentsAdapter.toJson(listOf(
                        MessageComment("c5", "dog_lover", "https://images.unsplash.com/photo-1544717305-2782549b5136?q=80&w=100", "Oh my god those floppy ears! Protect him at all costs!", "10m ago", 180),
                        MessageComment("c6", "nature_fan", "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?q=80&w=100", "Made my whole week! Thank you for this perfect content", "30m ago", 95)
                    ))
                ),
                CachedVideo(
                    id = "vid_4",
                    username = "keyboard_clicks",
                    userAvatar = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?q=80&w=200&auto=format&fit=crop",
                    videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-pressing-keys-on-a-backlit-gaming-keyboard-41712-large.mp4",
                    caption = "Building a custom creamy gasket-mount 75% mechanical board! ASMR sound test incoming ⌨️🎧🔊",
                    hashtags = "#mechanicalkeyboard #asmrsounds #desksetup #pcmasterrace #rgbKeyboard",
                    musicName = "Ambient Chill - Keyboard Click Sounds",
                    likesCount = 1860,
                    commentsCount = 68,
                    sharesCount = 135,
                    isLiked = false,
                    isSaved = false,
                    isVerified = false,
                    commentsJson = commentsAdapter.toJson(listOf(
                        MessageComment("c7", "switches_fan", "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?q=80&w=100", "Are these lubed premium gateron yellow switches? Sounds incredibly direct!", "30m ago", 18),
                        MessageComment("c8", "setup_lux", "https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=100", "Beautiful deskpad and rgb combination. Added to my inspo board.", "1h ago", 5)
                    ))
                )
            )
            db.cachedVideoDao().insertVideos(defaultVideos)
        }

        // 3. Seed notifications
        val notes = db.notificationDao().getAllNotificationsFlow().firstOrNull() ?: emptyList()
        if (notes.isEmpty()) {
            db.notificationDao().insertNotification(
                NotificationItem(
                    type = "follow",
                    title = "New follower alert!",
                    body = "@keyboard_clicks started following your creator profile.",
                    userAvatar = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?q=80&w=100"
                )
            )
            db.notificationDao().insertNotification(
                NotificationItem(
                    type = "like",
                    title = "Video Liked",
                    body = "@neon_rider liked your vertical synthwave react video.",
                    userAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=100"
                )
            )
            db.notificationDao().insertNotification(
                NotificationItem(
                    type = "live",
                    title = "Go LIVE notification",
                    body = "@chef_elite is LIVE right now baking stone pies! Tune in now!",
                    userAvatar = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?q=80&w=100"
                )
            )
        }

        // 4. Seed transaction history
        val txs = db.walletTxDao().getAllTxsFlow().firstOrNull() ?: emptyList()
        if (txs.isEmpty()) {
            db.walletTxDao().insertTx(WalletTx(description = "Google Pay recharge", amount = 500, isCredit = true))
            db.walletTxDao().insertTx(WalletTx(description = "Gifted chef Rose to @chef_elite", amount = 100, isCredit = false))
            db.walletTxDao().insertTx(WalletTx(description = "First signup coin bonus", amount = 100, isCredit = true))
        }
    }

    // Session Operations
    suspend fun updateSession(session: UserSession) {
        db.userSessionDao().insertSession(session)
    }

    suspend fun clearSession() {
        db.userSessionDao().insertSession(
            UserSession(
                id = "current",
                username = "guest",
                bio = "Log in to join the trending feed",
                profilePicUrl = "",
                followers = 0,
                following = 0,
                coins = 0,
                isVerified = false,
                isPrivate = false,
                isLoggedIn = false,
                isGuest = true
            )
        )
    }

    // Draft Operations
    suspend fun saveDraft(caption: String, hashtags: String, musicTitle: String, path: String) {
        db.videoDraftDao().insertDraft(
            VideoDraft(caption = caption, hashtags = hashtags, musicTitle = musicTitle, path = path)
        )
    }

    suspend fun deleteDraftDirect(id: Int) {
        db.videoDraftDao().deleteDraftById(id)
    }

    // Chat Message Operations
    suspend fun sendMessage(sender: String, receiver: String, text: String, mediaType: String? = null, mediaUrl: String? = null) {
        val chatMessage = ChatMessage(
            senderUsername = sender,
            receiverUsername = receiver,
            messageText = text,
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            isSeen = false
        )
        db.chatMessageDao().insertMessage(chatMessage)
    }

    suspend fun simulateReplier(userTalkingTo: String, senderText: String) {
        val responseText = when {
            senderText.contains("hi", true) || senderText.contains("hello", true) -> {
                "Hey there TokTiker! 🤙 Love your profile. How's your creating journey going?"
            }
            senderText.contains("collab", true) || senderText.contains("video", true) -> {
                "Absolutely! We should definitely do a duet or a live split stream next Saturday! What sounds do you suggest?"
            }
            senderText.contains("gift", true) || senderText.contains("coin", true) -> {
                "Oh wow, thanks so much for the gifts! Your support means the world to creator funding! 💖🏆"
            }
            else -> {
                "That's awesome! Check out the trending hashtag page, we are organizing a creator marathon!"
            }
        }

        val responseMessage = ChatMessage(
            senderUsername = userTalkingTo,
            receiverUsername = "lucy_toktik", // Current active seeded user
            messageText = responseText,
            isSeen = false
        )
        db.chatMessageDao().insertMessage(responseMessage)
    }

    suspend fun markSeen(sender: String, receiver: String) {
        db.chatMessageDao().markMessagesAsSeen(sender, receiver)
    }

    // Wallet Operations
    suspend fun buyCoins(amount: Int) {
        val curr = db.userSessionDao().getSessionDirect() ?: return
        val updatedUser = curr.copy(coins = curr.coins + amount)
        db.userSessionDao().insertSession(updatedUser)
        db.walletTxDao().insertTx(WalletTx(description = "Purchased Coins via Google Play", amount = amount, isCredit = true))
    }

    suspend fun giftCreator(videoId: String, creatorUsername: String, cost: Int): Boolean {
        val curr = db.userSessionDao().getSessionDirect() ?: return false
        if (curr.coins < cost) return false

        // Deduct
        val updatedUser = curr.copy(coins = curr.coins - cost)
        db.userSessionDao().insertSession(updatedUser)
        db.walletTxDao().insertTx(
            WalletTx(description = "Sent virtual Gift to @$creatorUsername on video $videoId", amount = cost, isCredit = false)
        )

        // Add simulated notification of gifting feedback
        db.notificationDao().insertNotification(
            NotificationItem(
                type = "system",
                title = "Gift Delivered",
                body = "You gifted a Neon Trophy to @$creatorUsername ($cost Coins). Thanks for supporting creators!"
            )
        )
        return true
    }

    // Video Feed Interactive Operations
    suspend fun toggleLikeVideo(videoId: String) {
        val currentVideo = db.cachedVideoDao().getAllVideosFlow().firstOrNull()?.find { it.id == videoId } ?: return
        val liked = !currentVideo.isLiked
        val updatedVideo = currentVideo.copy(
            isLiked = liked,
            likesCount = if (liked) currentVideo.likesCount + 1 else currentVideo.likesCount - 1
        )
        db.cachedVideoDao().updateVideo(updatedVideo)

        // Insert notification on like
        if (liked) {
            db.notificationDao().insertNotification(
                NotificationItem(
                    type = "like",
                    title = "You liked a video",
                    body = "Liked the video posted by @${currentVideo.username}",
                    userAvatar = currentVideo.userAvatar
                )
            )
        }
    }

    suspend fun toggleSaveVideo(videoId: String) {
        val currentVideo = db.cachedVideoDao().getAllVideosFlow().firstOrNull()?.find { it.id == videoId } ?: return
        val saved = !currentVideo.isSaved
        val updatedVideo = currentVideo.copy(isSaved = saved)
        db.cachedVideoDao().updateVideo(updatedVideo)
    }

    suspend fun addComment(videoId: String, commenter: String, text: String, commenterAvatar: String = "") {
        val currentVideo = db.cachedVideoDao().getAllVideosFlow().firstOrNull()?.find { it.id == videoId } ?: return

        // Decode current list
        val stringComments = currentVideo.commentsJson
        val commentList = try {
            commentsAdapter.fromJson(stringComments)?.toMutableList() ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }

        // Add new comment
        val newComment = MessageComment(
            id = "c_${System.currentTimeMillis()}",
            username = commenter,
            avatar = commenterAvatar.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde" },
            commentText = text,
            timestamp = "Just now",
            likes = 0
        )
        commentList.add(newComment)

        // Re-serialize and save
        val updatedVideo = currentVideo.copy(
            commentsCount = currentVideo.commentsCount + 1,
            commentsJson = commentsAdapter.toJson(commentList) ?: "[]"
        )
        db.cachedVideoDao().updateVideo(updatedVideo)
    }

    suspend fun publishNewVideo(caption: String, hashtags: String, music: String, simulatedPath: String) {
        val sess = db.userSessionDao().getSessionDirect() ?: return
        val videoId = "user_vid_${System.currentTimeMillis()}"
        val userVideo = CachedVideo(
            id = videoId,
            username = sess.username,
            userAvatar = sess.profilePicUrl.ifEmpty { "https://images.unsplash.com/photo-1534528741775-53994a69daeb" },
            videoUrl = "https://assets.mixkit.co/videos/preview/mixkit-girl-dancing-with-retro-neon-background-40038-large.mp4", // Cool dancing looping preset
            caption = caption,
            hashtags = hashtags,
            musicName = if (music.isNotEmpty()) music else "Original Sound - @${sess.username}",
            likesCount = 0,
            commentsCount = 0,
            sharesCount = 0,
            isLiked = false,
            isSaved = false,
            isVerified = sess.isVerified,
            commentsJson = "[]"
        )
        db.cachedVideoDao().insertVideo(userVideo)
    }

    suspend fun deleteVideoDirect(video: CachedVideo) {
        db.cachedVideoDao().deleteVideo(video)
    }

    suspend fun updateVideoDetails(video: CachedVideo) {
        db.cachedVideoDao().updateVideo(video)
    }

    suspend fun addNewVideo(video: CachedVideo) {
        db.cachedVideoDao().insertVideo(video)
    }

    suspend fun addNotification(note: NotificationItem) {
        db.notificationDao().insertNotification(note)
    }
}
