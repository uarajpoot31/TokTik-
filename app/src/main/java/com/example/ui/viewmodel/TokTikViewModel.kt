package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
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
    fun handleAuthentication(username: String, isGoogle: Boolean = false, isFB: Boolean = false) {
        viewModelScope.launch {
            val prebuiltAvatar = when {
                isGoogle -> "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=200"
                isFB -> "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?q=80&w=200"
                else -> "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200"
            }
            val cleanUsername = if (username.trim().isEmpty()) "toktik_star" else username.trim().lowercase().replace(" ", "_")
            val newSess = UserSession(
                id = "current",
                username = cleanUsername,
                bio = "Hi TokTik community! Content Creator in progress 💫",
                profilePicUrl = prebuiltAvatar,
                followers = 15,
                following = 24,
                coins = 300,
                isVerified = false,
                isPrivate = false,
                isLoggedIn = true,
                isGuest = false,
                socialLinks = "instagram.com/$cleanUsername"
            )
            repository.updateSession(newSess)

            // Trigger welcome push alert
            repository.checkAndSeedDatabase()
            repository.updateSession(newSess)
            triggerPushNotification(
                type = "system",
                title = "Welcome $cleanUsername!",
                body = "Ready to explore TokTik premium trends? 🌟 Your first 300 coins bonus was credited."
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
                path = "file://simulated_draft_video_${System.currentTimeMillis()}.mp4"
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
                simulatedPath = "file://final_path_${System.currentTimeMillis()}.mp4"
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
