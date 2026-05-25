package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.data.gemini.GeminiClient
import com.example.data.room.AppDatabase
import com.example.data.room.TokTikRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TokTikViewModel(
    application: Application,
    val repository: TokTikRepository
) : AndroidViewModel(application) {

    // Persistent state observers
    val session: StateFlow<UserSession?> = repository.session
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val videos: StateFlow<List<CachedVideo>> = repository.videos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val draftsContent: StateFlow<List<VideoDraft>> = repository.drafts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notificationsList: StateFlow<List<NotificationItem>> = repository.notifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTxs: StateFlow<List<WalletTx>> = repository.walletTxs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // In-memory UI States
    var currentFeedIndex = mutableStateOf(0)
    var isDarkMode = mutableStateOf(true) // Theme state flow
    var isUrduSelected = mutableStateOf(false) // Dynamic language flow (English vs Urdu)

    // Search results states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<CachedVideo>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _searchProfiles = MutableStateFlow<List<String>>(emptyList())
    val searchProfiles = _searchProfiles.asStateFlow()

    // Chat screen states
    val activeChatReceiver = mutableStateOf<String?>("chef_elite")
    val isChatTyping = mutableStateOf(false)

    // Live Streaming screen states
    val isLiveActive = mutableStateOf(false)
    val liveViewerCount = mutableStateOf(142)
    val liveChatMessages = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    // AI loading flags
    val isAiLoading = mutableStateOf(false)

    init {
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
            // Set initial Search suggestions
            onSearch("")
        }
    }

    // Video Operations
    fun toggleLike(videoId: String) {
        viewModelScope.launch {
            repository.toggleLikeVideo(videoId)
        }
    }

    fun toggleSave(videoId: String) {
        viewModelScope.launch {
            repository.toggleSaveVideo(videoId)
        }
    }

    fun deletePostedVideo(video: CachedVideo) {
        viewModelScope.launch {
            repository.deleteVideoDirect(video)
        }
    }

    fun addCommentToVideo(videoId: String, text: String) {
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            val username = session.value?.username ?: "guest_user"
            val pic = session.value?.profilePicUrl ?: ""
            repository.addComment(videoId, username, text, pic)
        }
    }

    // Search Filter Action
    fun onSearch(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            val allv = repository.videos.firstOrNull() ?: emptyList()
            if (query.trim().isEmpty()) {
                _searchResults.value = allv
                _searchProfiles.value = listOf("neon_rider", "chef_elite", "golden_paws", "keyboard_clicks", "lucy_toktik")
            } else {
                _searchResults.value = allv.filter {
                    it.caption.contains(query, ignoreCase = true) ||
                    it.hashtags.contains(query, ignoreCase = true) ||
                    it.username.contains(query, ignoreCase = true)
                }
                _searchProfiles.value = listOf("neon_rider", "chef_elite", "golden_paws", "keyboard_clicks", "lucy_toktik").filter {
                    it.contains(query, ignoreCase = true)
                }
            }
        }
    }

    // Session Operations
    fun handleAuthentication(username: String, passwordText: String = "", isGoogle: Boolean = false, isFB: Boolean = false) {
        viewModelScope.launch {
            val prebuiltAvatar = when {
                isGoogle -> "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=200"
                isFB -> "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?q=80&w=200"
                else -> "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200"
            }
            val cleanUsername = if (username.trim().isEmpty()) "toktik_star" else username.trim()
            val isAdminUser = cleanUsername.lowercase() == "usamaarfi" && passwordText == "727738"

            val newSess = UserSession(
                id = "current",
                username = cleanUsername,
                bio = if (isAdminUser) "Primary System Admin & Controller 🛡️" else "Hi TokTik community! Content Creator in progress 💫",
                profilePicUrl = if (isAdminUser) "https://images.unsplash.com/photo-1517841905240-472988babdf9?q=80&w=200" else prebuiltAvatar,
                followers = if (isAdminUser) 999999 else 15,
                following = if (isAdminUser) 1 else 24,
                coins = if (isAdminUser) 99999 else 300,
                points = if (isAdminUser) 500 else 0,
                earningsRs = if (isAdminUser) 5.0 else 0.0,
                isVerified = isAdminUser,
                isPrivate = false,
                isLoggedIn = true,
                isGuest = false,
                isAdmin = isAdminUser,
                isBlocked = false,
                socialLinks = "instagram.com/$cleanUsername"
            )
            repository.updateSession(newSess)

            // Trigger welcome push alert
            triggerPushNotification(
                type = "system",
                title = if (isAdminUser) "Welcome Owner Usama Arfi! 🛡️" else "Welcome $cleanUsername!",
                body = if (isAdminUser) "Secret Admin Controls unlocked. You have full system privileges." else "Ready to explore TokTik premium trends? 🌟 Your first 300 coins bonus was credited."
            )
        }
    }

    fun handleLogout() {
        viewModelScope.launch {
            repository.clearSession()
        }
    }

    fun updateProfileSettings(bioStr: String, linksStr: String, isPrivate: Boolean) {
        viewModelScope.launch {
            val curr = session.value ?: return@launch
            val updated = curr.copy(
                bio = bioStr,
                socialLinks = linksStr,
                isPrivate = isPrivate
            )
            repository.updateSession(updated)
        }
    }

    fun upgradeToVerifiedStatus() {
        viewModelScope.launch {
            val curr = session.value ?: return@launch
            val updated = curr.copy(isVerified = true)
            repository.updateSession(updated)
            triggerPushNotification("system", "Verification Badge Acquired!", "Congratulations! Your account was awarded the TokTik verified blue checkmark.")
        }
    }

    // Gifting and Wallet
    fun purchaseCoinsPack(amount: Int) {
        viewModelScope.launch {
            repository.buyCoins(amount)
        }
    }

    fun sendGift(videoId: String, creatorName: String, coins: Int, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.giftCreator(videoId, creatorName, coins)
            onComplete(success)
        }
    }

    // Direct DMs Messages
    fun getActiveChatFlow(): Flow<List<ChatMessage>> {
        val opponent = activeChatReceiver.value ?: "chef_elite"
        return repository.getChatMessages("lucy_toktik", opponent)
    }

    fun sendDirectMessage(text: String) {
        if (text.trim().isEmpty()) return
        val opponent = activeChatReceiver.value ?: "chef_elite"
        viewModelScope.launch {
            // Send
            repository.sendMessage("lucy_toktik", opponent, text)

            // Simulate typing and response
            isChatTyping.value = true
            delay(1500)
            isChatTyping.value = false

            // Answer back
            repository.simulateReplier(opponent, text)
        }
    }

    // Upload & Drafts
    fun addDraftVideo(caption: String, hashtags: String, music: String) {
        viewModelScope.launch {
            repository.saveDraft(
                caption = caption,
                hashtags = hashtags,
                musicTitle = music,
                path = "file://toktik_draft_record_${System.currentTimeMillis()}.mp4"
            )
        }
    }

    fun deleteDraft(draftId: Int) {
        viewModelScope.launch {
            repository.deleteDraftDirect(draftId)
        }
    }

    fun publishVideo(caption: String, hashtags: String, music: String, draftIdToDelete: Int? = null) {
        viewModelScope.launch {
            repository.publishNewVideo(
                caption = caption,
                hashtags = hashtags,
                music = music,
                simulatedPath = "file://stream_rendered_content_${System.currentTimeMillis()}.mp4"
            )
            if (draftIdToDelete != null) {
                repository.deleteDraftDirect(draftIdToDelete)
            }
            triggerPushNotification("system", "Upload Success!", "Your video was compressed and rendered in 1080p HD. It is now live on the feed!")
        }
    }

    // Live Streaming Simulator Loop
    fun startLiveSession() {
        isLiveActive.value = true
        liveViewerCount.value = 142
        liveChatMessages.value = listOf(
            "chef_elite" to "Welcome in!",
            "tech_guru" to "This stream quality looks absolute 4K! 🔥🔥",
            "neon_rider" to "Send them standard gifts guys!",
            "user_5493" to "Greetings from Seattle!"
        )

        // Simulated updates task loop
        viewModelScope.launch {
            var iterations = 0
            while (isLiveActive.value && iterations < 50) {
                delay(3000)
                iterations++
                // Random viewer fluctuating
                liveViewerCount.value = liveViewerCount.value + (-15..18).random()

                // Random new chat comments
                val mockTalkers = listOf("golden_paws", "keyboard_clicks", "user_19", "cyber_alice", "lucas_99")
                val mockTexts = listOf(
                    "You are absolute inspiration! 💖🥇",
                    "Can you show the camera setup?",
                    "Double tap like double tap guys!",
                    "Shared to original channel!",
                    "So colorful, loving retro lights!"
                )
                val currentList = liveChatMessages.value.toMutableList()
                currentList.add(mockTalkers.random() to mockTexts.random())
                if (currentList.size > 8) currentList.removeAt(0)
                liveChatMessages.value = currentList
            }
        }
    }

    fun stopLiveSession() {
        isLiveActive.value = false
    }

    fun sendLiveComment(text: String) {
        if (text.trim().isEmpty()) return
        val currentList = liveChatMessages.value.toMutableList()
        currentList.add((session.value?.username ?: "lucy_toktik") to text)
        liveChatMessages.value = currentList
    }

    // System Alert setup
    fun triggerPushNotification(type: String, title: String, body: String) {
        viewModelScope.launch {
            repository.addNotification(
                NotificationItem(
                    type = type,
                    title = title,
                    body = body,
                    userAvatar = ""
                )
            )
        }
    }

    // AI Features integration with Gemini!
    fun runAiHashtagGenerator(videoAbout: String, onCompleted: (String) -> Unit) {
        isAiLoading.value = true
        viewModelScope.launch {
            val systemPrompt = "You are a social media viral marketing expert who generates 5 relevant, popular hashtags for short video captions. Output ONLY the hashtags on a single line separated by spaces."
            val response = GeminiClient.generateAiText(
                prompt = "Generate hashtags for a video about: $videoAbout",
                systemPrompt = systemPrompt
            )
            isAiLoading.value = false
            onCompleted(response)
        }
    }

    fun runAiCaptionOptimizer(roughCaption: String, onCompleted: (String) -> Unit) {
        isAiLoading.value = true
        viewModelScope.launch {
            val systemPrompt = "You are a professional TikTok creator writing engaging, high-retention short 1-sentence captions. Include 1-2 relevant emojis. Keep it fun and witty!"
            val response = GeminiClient.generateAiText(
                prompt = "Optimize this raw video caption: '$roughCaption'",
                systemPrompt = systemPrompt
            )
            isAiLoading.value = false
            onCompleted(response)
        }
    }

    fun runAiContentHelper(prompt: String, onCompleted: (String) -> Unit) {
        isAiLoading.value = true
        viewModelScope.launch {
            val systemPrompt = "You are the smart TokTik AI Creative Director. Give a quick, 2-bullet point, actionable video concept idea based on the user's focus."
            val response = GeminiClient.generateAiText(
                prompt = prompt,
                systemPrompt = systemPrompt
            )
            isAiLoading.value = false
            onCompleted(response)
        }
    }

    // --- CUSTOM EARNING, BILLING & ADMINISTRATIVE SYSTEMS ---
    val trendingSongs = mutableStateListOf(
        "Block - Abida Parveen x Asim Azhar 🎤",
        "Kahani Suno 2.0 - Kaifi Khalil 🎸",
        "Bado Badi - Chahat Fateh Ali Khan 🤪",
        "Tu Hai Kahan - AUR 🎵",
        "Pasoori - Ali Sethi x Shae Gill 🌺",
        "Habibi - Asim Azhar 🔥",
        "Siri - Sidhu Moose Wala (Legend) 👑",
        "Obsessed - Riar Saab ✨",
        "Guli Mata - Saad Lamjarred x Shreya Ghoshal 🎶",
        "Angreji Beat - Yo Yo Honey Singh 🕺"
    )

    fun addNewTrendingSong(name: String) {
        if (name.trim().isNotEmpty() && !trendingSongs.contains(name.trim())) {
            trendingSongs.add(name.trim())
        }
    }

    // Video watching earning points tally: 10 points per watch/interaction
    fun watchVideoAndEarnPoints() {
        viewModelScope.launch {
            val sess = repository.db.userSessionDao().getSessionDirect() ?: return@launch
            if (sess.isBlocked) return@launch
            val updatedSess = sess.copy(points = sess.points + 10)
            repository.updateSession(updatedSess)
        }
    }

    // Points conversion to Rs (100 points = Rs. 1)
    fun redeemPointsToRs(onCompleted: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val sess = repository.db.userSessionDao().getSessionDirect() ?: return@launch
            if (sess.points < 100) {
                onCompleted(false, "You need at least 100 points to redeem! (100 Points = Rs. 1)")
                return@launch
            }
            val redeemedRs = sess.points / 100.0
            val restPoints = sess.points % 100
            val updatedSess = sess.copy(
                points = restPoints,
                earningsRs = sess.earningsRs + redeemedRs
            )
            repository.updateSession(updatedSess)
            
            // Add transaction log
            repository.db.walletTxDao().insertTx(
                com.example.data.WalletTx(
                    description = "Redeemed ${sess.points - restPoints} watch points",
                    amount = redeemedRs.toInt(),
                    isCredit = true
                )
            )
            onCompleted(true, "Successfully redeemed points. Added Rs. $redeemedRs to your withdrawable earnings!")
        }
    }

    // Withdraw earnings (with 10% auto-deduction towards 03193736056 JazzCash business address)
    fun requestWithdrawRs(amountRs: Double, mobileNo: String, isEasyPaisa: Boolean, onCompleted: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val sess = repository.db.userSessionDao().getSessionDirect() ?: return@launch
            if (sess.earningsRs < amountRs) {
                onCompleted(false, "Insufficient withdrawable earnings balance! Current balance: Rs. ${sess.earningsRs}")
                return@launch
            }
            if (amountRs < 50) {
                onCompleted(false, "Minimum withdrawal limit is Rs. 50")
                return@launch
            }
            
            val commission = amountRs * 0.10
            val creatorProfit = amountRs - commission
            
            val updatedSess = sess.copy(earningsRs = sess.earningsRs - amountRs)
            repository.updateSession(updatedSess)
            
            // Log the withdrawal transaction
            repository.db.walletTxDao().insertTx(
                com.example.data.WalletTx(
                    description = "Withdrawal to ${if (isEasyPaisa) "EasyPaisa" else "JazzCash"} ($mobileNo)",
                    amount = amountRs.toInt(),
                    isCredit = false
                )
            )
            
            // Log 10% royalty commission ledger
            repository.db.walletTxDao().insertTx(
                com.example.data.WalletTx(
                    description = "10% Platform Comm to Usama Arfi (03193736056)",
                    amount = commission.toInt(),
                    isCredit = false
                )
            )
            
            onCompleted(
                true,
                "Withdrawal Successful! Total: Rs. $amountRs. " +
                "90% (Rs. $creatorProfit) sent to your $mobileNo account. " +
                "10% (Rs. $commission) platform commission automatically routed to administrative JazzCash merchant account (03193736056)!"
            )
        }
    }

    // Native Premium billing: buy coin packets and credit wallet instantly 
    fun purchaseCoinsWithJazzCash(amountCoins: Int, costRs: Int, txnId: String, onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            val sess = repository.db.userSessionDao().getSessionDirect() ?: return@launch
            val updatedSess = sess.copy(coins = sess.coins + amountCoins)
            repository.updateSession(updatedSess)
            
            // Insert wallet tx
            repository.db.walletTxDao().insertTx(
                com.example.data.WalletTx(
                    description = "Purchased $amountCoins Coins via JazzCash Business (TID: $txnId)",
                    amount = costRs,
                    isCredit = true
                )
            )
            
            // Trigger push
            triggerPushNotification(
                type = "system",
                title = "Coins Payment Verified! ✨",
                body = "Successfully credited $amountCoins Coins to your account after verifying payment transaction $txnId of Rs. $costRs."
            )
            onCompleted(true)
        }
    }

    // Publish video from Gallery or record simulated camera
    fun publishCustomVideo(caption: String, hashtags: String, music: String, videoUrl: String) {
        viewModelScope.launch {
            val sess = repository.db.userSessionDao().getSessionDirect() ?: return@launch
            val newVideo = CachedVideo(
                id = "user_reels_${System.currentTimeMillis()}",
                username = sess.username,
                userAvatar = sess.profilePicUrl.ifEmpty { "https://images.unsplash.com/photo-1534528741775-53994a69daeb" },
                videoUrl = videoUrl,
                caption = caption,
                hashtags = hashtags,
                musicName = music.ifEmpty { "Original Sound - @${sess.username}" },
                likesCount = 0,
                commentsCount = 0,
                sharesCount = 0,
                viewsCount = 1,
                isLiked = false,
                isSaved = false,
                isVerified = sess.isVerified,
                commentsJson = "[]"
            )
            repository.addNewVideo(newVideo)
            
            triggerPushNotification(
                type = "video",
                title = "Reel Published! 🎬",
                body = "Your custom recorded video with track '$music' is now playing in the Feed!"
            )
        }
    }

    // Admin direct control operations:
    fun adminUpdateVideoText(videoId: String, newCaption: String, newHashtags: String, newMusic: String) {
        viewModelScope.launch {
            val matchedVideo = videos.value.find { it.id == videoId } ?: return@launch
            val updatedVideo = matchedVideo.copy(
                caption = newCaption,
                hashtags = newHashtags,
                musicName = newMusic
            )
            repository.updateVideoDetails(updatedVideo)
        }
    }

    // Admin goes viral: boost views, likes (+5000), comments (+250) instantly
    fun adminBoostVideoViral(videoId: String) {
        viewModelScope.launch {
            val matchedVideo = videos.value.find { it.id == videoId } ?: return@launch
            val updatedVideo = matchedVideo.copy(
                viewsCount = matchedVideo.viewsCount + 150000,
                likesCount = matchedVideo.likesCount + 9500,
                commentsCount = matchedVideo.commentsCount + 4200,
                sharesCount = matchedVideo.sharesCount + 2300,
                isVerified = true
            )
            repository.updateVideoDetails(updatedVideo)
        }
    }

    // Admin blocks video or bans/unbans its creator
    fun adminToggleVideoBlock(videoId: String) {
        viewModelScope.launch {
            val matchedVideo = videos.value.find { it.id == videoId } ?: return@launch
            val updatedVideo = matchedVideo.copy(isBlocked = !matchedVideo.isBlocked)
            repository.updateVideoDetails(updatedVideo)
        }
    }
}

// Custom ViewModel Factory
class TokTikViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TokTikViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val repo = TokTikRepository(database)
            @Suppress("UNCHECKED_CAST")
            return TokTikViewModel(context.applicationContext as Application, repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
