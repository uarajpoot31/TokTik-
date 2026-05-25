@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import android.widget.Toast
import android.widget.VideoView
import android.net.Uri
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import coil.compose.AsyncImage
import com.example.data.CachedVideo
import com.example.data.NotificationItem
import com.example.data.UserSession
import com.example.data.VideoDraft
import com.example.data.room.MessageComment
import com.example.ui.theme.*
import com.example.ui.viewmodel.TokTikViewModel
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Bilingual Localization Helper Utility
fun getLocalizedText(en: String, ur: String, isUrdu: Boolean): String {
    return if (isUrdu) ur else en
}

// Screen Routes Enum
enum class TokTikRoute {
    SPLASH,
    ONBOARDING,
    LOGIN,
    HOME_FEED,
    UPLOAD,
    SEARCH,
    INBOX,
    CHAT_DETAIL,
    LIVE_STREAM,
    USER_PROFILE,
    WALLET,
    CREATOR_STUDIO,
    SETTINGS,
    ADMIN_PANEL
}

// Global Nav Controller mock for simple screen switching holding local state
@Composable
fun TokTikAppNavigation(viewModel: TokTikViewModel) {
    var currentScreen by remember { mutableStateOf(TokTikRoute.SPLASH) }
    val session by viewModel.session.collectAsState()
    val isDarkMode by viewModel.isDarkMode

    // Handle back presses on specific screens
    BackHandler(enabled = currentScreen != TokTikRoute.HOME_FEED && currentScreen != TokTikRoute.SPLASH) {
        currentScreen = if (currentScreen == TokTikRoute.CHAT_DETAIL) {
            TokTikRoute.INBOX
        } else {
            TokTikRoute.HOME_FEED
        }
    }

    TokTikTheme(darkTheme = isDarkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "ScreenTransition"
            ) { route ->
                when (route) {
                    TokTikRoute.SPLASH -> SplashScreen {
                        currentScreen = if (session?.isLoggedIn == true) TokTikRoute.HOME_FEED else TokTikRoute.ONBOARDING
                    }
                    TokTikRoute.ONBOARDING -> OnboardingScreen(
                        onSkip = { currentScreen = TokTikRoute.LOGIN },
                        onFinish = { currentScreen = TokTikRoute.LOGIN }
                    )
                    TokTikRoute.LOGIN -> LoginScreen(
                        viewModel = viewModel,
                        onLoginSuccess = { currentScreen = TokTikRoute.HOME_FEED }
                    )
                    TokTikRoute.HOME_FEED -> HomeFeedScreen(
                        viewModel = viewModel,
                        onNavigate = { currentScreen = it }
                    )
                    TokTikRoute.UPLOAD -> UploadScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = TokTikRoute.HOME_FEED },
                        onPublished = { currentScreen = TokTikRoute.HOME_FEED }
                    )
                    TokTikRoute.SEARCH -> SearchScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = TokTikRoute.HOME_FEED },
                        onNavigate = { currentScreen = it }
                    )
                    TokTikRoute.INBOX -> InboxScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = TokTikRoute.HOME_FEED },
                        onOpenChat = { receiver ->
                            viewModel.activeChatReceiver.value = receiver
                            currentScreen = TokTikRoute.CHAT_DETAIL
                        },
                        onNavigate = { currentScreen = it }
                    )
                    TokTikRoute.CHAT_DETAIL -> ChatDetailScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = TokTikRoute.INBOX }
                    )
                    TokTikRoute.LIVE_STREAM -> LiveStreamScreen(
                        viewModel = viewModel,
                        onBackPressed = {
                            viewModel.stopLiveSession()
                            currentScreen = TokTikRoute.HOME_FEED
                        }
                    )
                    TokTikRoute.USER_PROFILE -> UserProfileScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = TokTikRoute.HOME_FEED },
                        onNavigate = { currentScreen = it }
                    )
                    TokTikRoute.WALLET -> WalletScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = TokTikRoute.USER_PROFILE }
                    )
                    TokTikRoute.CREATOR_STUDIO -> CreatorStudioScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = TokTikRoute.USER_PROFILE }
                    )
                    TokTikRoute.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = TokTikRoute.USER_PROFILE },
                        onLoggedOut = { currentScreen = TokTikRoute.LOGIN }
                    )
                    TokTikRoute.ADMIN_PANEL -> AdminPanelScreen(
                        viewModel = viewModel,
                        onBackPressed = { currentScreen = TokTikRoute.USER_PROFILE }
                    )
                }
            }
        }
    }
}

// 1. SPLASH SCREEN
@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        delay(1500)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F0407), Color(0xFF010101)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value)
        ) {
            // Neon Pulsing Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(listOf(RedPrimary.copy(alpha = 0.4f), Color.Transparent)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "TokTik Play Logo",
                    tint = RedPrimary,
                    modifier = Modifier.size(64.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "TOKTIK",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 6.sp,
                modifier = Modifier.testTag("splash_title")
            )
            Text(
                text = "Create. Wave. Concur. ⚡",
                fontSize = 14.sp,
                color = CyanSecondary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp
            )
        }
    }
}

// 2. ONBOARDING SCREEN
@Composable
fun OnboardingScreen(onSkip: () -> Unit, onFinish: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    val pages = listOf(
        Triple(Icons.Filled.MovieFilter, "Discover Trends", "Swipe through viral vertical videos, personalized just for you with premium interactive audio loops."),
        Triple(Icons.Filled.ElectricBolt, "AI Creator Studio", "Unleash your creativity with integrated Gemini AI helper to generate viral titles, optimized hashtags and descriptions!"),
        Triple(Icons.Filled.VideoChat, "Go Live & Gifting", "Participate in real-time virtual streaming. Send gorgeous trophies, roses or diamonds using the coins wallet!")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF02010A), Color(0xFF140A0F)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            // Icon with glow
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        Brush.radialGradient(listOf(CyanSecondary.copy(alpha = 0.3f), Color.Transparent)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = pages[currentPage].first,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = pages[currentPage].second,
                fontSize = 28.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = pages[currentPage].third,
                fontSize = 15.sp,
                color = TextGray,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Navigation dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentPage) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (index == currentPage) RedPrimary else BorderColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) {
                    Text("SKIP", color = TextGray, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        if (currentPage < pages.lastIndex) {
                            currentPage++
                        } else {
                            onFinish()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.widthIn(min = 120.dp)
                ) {
                    Text(if (currentPage == pages.lastIndex) "GET STARTED" else "NEXT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 3. LOGIN SCREEN
@Composable
fun LoginScreen(viewModel: TokTikViewModel, onLoginSuccess: () -> Unit) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var otpCodeInput by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var bioInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF130206)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "TOKTIK",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 4.sp
            )
            Text(
                text = if (isSignUpMode) "Create Premium Creator Account 🌟" else "Login to Enter the Feed 🌶️",
                color = CyanSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Glassmorphism card for login fields
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark.copy(alpha = 0.85f)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(RedPrimary.copy(alpha = 0.3f), CyanSecondary.copy(alpha = 0.3f)))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username", color = TextGray) },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = RedPrimary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = BorderColor
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("username_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = bioInput,
                            onValueChange = { bioInput = it },
                            label = { Text("Creator Bio", color = TextGray) },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = RedPrimary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = RedPrimary,
                                unfocusedBorderColor = BorderColor
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password", color = TextGray) },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = RedPrimary) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = BorderColor
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isOtpSent) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = otpCodeInput,
                            onValueChange = { otpCodeInput = it },
                            label = { Text("Verification OTP Code", color = TextGray) },
                            leadingIcon = { Icon(Icons.Filled.VpnKey, contentDescription = null, tint = CyanSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyanSecondary,
                                unfocusedBorderColor = BorderColor
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = getLocalizedText(
                                "Secure OTP sent successfully. Verification in progress.", 
                                "سیکیور او ٹی پی کامیابی سے بھیج دیا گیا ہے۔ تصدیق جاری ہے۔", 
                                viewModel.isUrduSelected.value
                            ),
                            fontSize = 11.sp,
                            color = CyanSecondary,
                            modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val uName = usernameInput.trim()
                            val pWord = passwordInput.trim()
                            if (uName.lowercase() == "usamaarfi" && pWord == "727738") {
                                viewModel.handleAuthentication("usamaarfi", "727738")
                                Toast.makeText(context, "Welcome Admin Usama Arfi! Unlocking Control Desk...", Toast.LENGTH_LONG).show()
                                onLoginSuccess()
                                return@Button
                            }

                            if (uName.isEmpty()) {
                                Toast.makeText(context, "Please configure custom username!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!isSignUpMode && !isOtpSent) {
                                // Simulate sending OTP for verification security rule
                                isOtpSent = true
                                otpCodeInput = "4159" // Preset OTP code
                                Toast.makeText(context, "Verification OTP code generated: 4159", Toast.LENGTH_LONG).show()
                            } else {
                                // Authenticate
                                viewModel.handleAuthentication(
                                    username = uName,
                                    passwordText = pWord,
                                    isGoogle = false,
                                    isFB = false
                                )
                                if (isSignUpMode && bioInput.isNotEmpty()) {
                                    viewModel.updateProfileSettings(bioInput, "instagram.com/$uName", false)
                                }
                                Toast.makeText(context, "TokTik Login Authorized!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_button")
                    ) {
                        Text(
                            text = if (isSignUpMode) "REGISTER & LOG IN" else if (isOtpSent) "AUTHORIZE CONNECT" else "RECEIVE VERIFICATION OTP",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    if (!isSignUpMode && isOtpSent) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { isOtpSent = false; otpCodeInput = "" }) {
                            Text("Resend Code", color = CyanSecondary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Guest and external social logins
            Text("or continue with digital logs", color = TextGray, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {
                        viewModel.handleAuthentication("google_celebrity", isGoogle = true)
                        Toast.makeText(context, if (viewModel.isUrduSelected.value) "گوگل اکاؤنٹ کامیابی سے منسلک ہو گیا!" else "Google account successfully authorized!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.GppGood, contentDescription = null, tint = RedPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GOOGLE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        viewModel.handleAuthentication("fb_creator", isFB = true)
                        Toast.makeText(context, if (viewModel.isUrduSelected.value) "فیس بک اکاؤنٹ کامیابی سے منسلک ہو گیا!" else "Facebook account successfully authorized!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Facebook, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("FACEBOOK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Mode toggle
            TextButton(onClick = { isSignUpMode = !isSignUpMode }) {
                Text(
                    text = if (isSignUpMode) "Already have credentials? Click for Direct Login" else "Don't have an account? Tap for Creator Sign Up",
                    color = CyanSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Quick Guest connect option
            TextButton(onClick = {
                viewModel.handleAuthentication("guest_strayer")
                Toast.makeText(context, "Continuing as anonymous Guest Mode!", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
            }) {
                Text("Bypass Verification (Guest Mode)", color = TextGray)
            }
        }
    }
}

fun getVideoThumbnail(video: CachedVideo): String {
    return when {
        video.username == "neon_rider" -> "https://images.unsplash.com/photo-1515260268569-9271009adfdb?q=80&w=600"
        video.username == "chef_elite" -> "https://images.unsplash.com/photo-1513104890138-7c749659a591?q=80&w=600"
        video.username == "golden_paws" -> "https://images.unsplash.com/photo-1552053831-71594a27632d?q=80&w=600"
        video.username == "keyboard_clicks" -> "https://images.unsplash.com/photo-1618384887929-16ec33fab9ef?q=80&w=600"
        video.id.startsWith("user_vid_") -> "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?q=80&w=600"
        else -> "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=600"
    }
}

// 4. HOME FEED SCREEN (TikTok vertical swipe)
@Composable
fun HomeFeedScreen(viewModel: TokTikViewModel, onNavigate: (TokTikRoute) -> Unit) {
    val videos by viewModel.videos.collectAsState()
    val session by viewModel.session.collectAsState()
    val currentIndex by viewModel.currentFeedIndex

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Modal Comment drawer and Share state
    var showCommentDrawerForVideo by remember { mutableStateOf<String?>(null) }
    var showShareDrawerForVideo by remember { mutableStateOf<String?>(null) }
    var feedFilterMode by remember { mutableStateOf("For You") } // "For You" or "Following"

    Scaffold(
        bottomBar = {
            TokTikBottomNavigation(
                activeRoute = TokTikRoute.HOME_FEED,
                onRouteSelected = { onNavigate(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF121212),
                            Color(0xFF010101),
                            Color(0xFF121212)
                        )
                    )
                )
                .padding(innerPadding)
        ) {
            val liveVideos = videos.filter { !it.isBlocked }

            if (liveVideos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = RedPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No active feeds available.", color = TextGray)
                    }
                }
            } else {
                val currentVideo = liveVideos.getOrNull(currentIndex % liveVideos.size)
                if (currentVideo != null) {
                    // Watch-to-Earn Point Reward Trigger
                    LaunchedEffect(currentVideo.id) {
                        delay(4000) // Watch for 4 seconds to qualify
                        viewModel.watchVideoAndEarnPoints()
                        Toast.makeText(context, "🎉 Watch Bonus: +10 Points Credited!", Toast.LENGTH_SHORT).show()
                    }

                    // Full-bleed Video details rendering with customized overlays
                    Box(modifier = Modifier.fillMaxSize()) {

                        // Beautiful Full-bleed high-quality representative background
                        AsyncImage(
                            model = getVideoThumbnail(currentVideo),
                            contentDescription = "Simulated Video Preview Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Dark cinematic vertical gradient scrim overlay for premium readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(0.7f),
                                            Color.Transparent,
                                            Color.Black.copy(0.75f)
                                        )
                                    )
                                )
                        )

                        // Center glowing radial ambient simulated lighting flare
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0x2BFFFFFF),
                                            Color.Transparent
                                        ),
                                        center = Offset.Unspecified,
                                        radius = 400f
                                    )
                                )
                        )

                        // Interactive Video Canvas simulating audio progress and playback
                        VideoPlayerMock(
                            video = currentVideo,
                            onDoubleTap = {
                                viewModel.toggleLike(currentVideo.id)
                            }
                        )

                        // Navigation overlay (Top bar Following vs For You)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            // Left Side: Language Toggle Capsule
                            Row(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .background(Color.Black.copy(0.4f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.isUrduSelected.value = !viewModel.isUrduSelected.value
                                        val m = if (viewModel.isUrduSelected.value) "اردو زبان فعال کر دی گئی ہے!" else "English language activated!"
                                        Toast.makeText(context, m, Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Language",
                                    tint = if (viewModel.isUrduSelected.value) GoldAccent else Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (viewModel.isUrduSelected.value) "اردو" else "EN",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            // Center: Channel Switches
                            Row(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            feedFilterMode = "Following"
                                            Toast.makeText(context, if (viewModel.isUrduSelected.value) "فالوئنگ فیڈ لوڈ ہو رہی ہے!" else "Switching to following feed creators!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = getLocalizedText("Following", "فالوئنگ", viewModel.isUrduSelected.value),
                                        color = if (feedFilterMode == "Following") Color.White else Color.White.copy(alpha = 0.6f),
                                        fontWeight = if (feedFilterMode == "Following") FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(height = 3.dp, width = 16.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (feedFilterMode == "Following") RedPrimary else Color.Transparent)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            feedFilterMode = "For You"
                                        }
                                        .padding(horizontal = 12.dp)
                                ) {
                                    Text(
                                        text = getLocalizedText("For You", "آپ کے لیے", viewModel.isUrduSelected.value),
                                        color = if (feedFilterMode == "For You") Color.White else Color.White.copy(alpha = 0.6f),
                                        fontWeight = if (feedFilterMode == "For You") FontWeight.Bold else FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(height = 3.dp, width = 16.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (feedFilterMode == "For You") RedPrimary else Color.Transparent)
                                    )
                                }
                            }

                            // Right Side: Safe operations hub / Search Button icon
                            IconButton(
                                onClick = { onNavigate(TokTikRoute.SEARCH) },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(34.dp)
                                    .background(Color.Black.copy(0.4f), CircleShape)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Right Action Controls Panel (Likes, Comments, Gifts, Shares)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 60.dp, end = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Creator profile bubble with follow toggle button
                            Box(contentAlignment = Alignment.BottomCenter) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .border(2.dp, Color.White, CircleShape)
                                        .padding(2.dp)
                                ) {
                                    AsyncImage(
                                        model = currentVideo.userAvatar,
                                        contentDescription = "Creator Profile",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .offset(y = 8.dp)
                                        .size(18.dp)
                                        .background(RedPrimary, CircleShape)
                                        .clickable {
                                            Toast
                                                .makeText(
                                                    context,
                                                    "Successfully followed @${currentVideo.username}!",
                                                    Toast.LENGTH_SHORT
                                                )
                                                .show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Like Button (Interactive)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = { viewModel.toggleLike(currentVideo.id) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = if (currentVideo.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (currentVideo.isLiked) RedPrimary else Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(
                                    text = formattedCount(currentVideo.likesCount),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Comments Button (Interactive)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = { showCommentDrawerForVideo = currentVideo.id },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Comment,
                                        contentDescription = "Comments",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text = formattedCount(currentVideo.commentsCount),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Gift Sender Panel (Interactive monetization coins system)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = { showShareDrawerForVideo = currentVideo.id },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CardGiftcard,
                                        contentDescription = "Gifts Support",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text = "GIFT",
                                    color = GoldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Share and save panel
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        viewModel.toggleSave(currentVideo.id)
                                        Toast.makeText(context, if (currentVideo.isSaved) "Video removed from bookmarks!" else "Saved to Bookmarks folder!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = if (currentVideo.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (currentVideo.isSaved) GoldAccent else Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text = formattedCount(currentVideo.sharesCount),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Rotating music record spinner mimicking running sound channels
                            val infiniteTransition = rememberInfiniteTransition(label = "music_rotator")
                            val rotationDegrees by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(4000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "record_degrees"
                            )

                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(44.dp)
                                    .rotate(rotationDegrees)
                                    .background(Color(0xFF27272A), CircleShape)
                                    .border(8.dp, Color(0xFF18181B), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFFEC4899), // pink-500
                                                    Color(0xFFFB923C)  // orange-400
                                                )
                                            ),
                                            CircleShape
                                        )
                                )
                            }
                        }

                        // Left Overlay Text Panel (Username, Caption, Hashtags, Running Music title)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth(0.75f)
                                .padding(start = 16.dp, bottom = 24.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "@${currentVideo.username}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color.White
                                )
                                if (currentVideo.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Verified profile",
                                        tint = VerifiedBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = currentVideo.caption,
                                color = Color.White,
                                fontSize = 14.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = currentVideo.hashtags,
                                color = CyanSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    currentVideo.musicName,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Drag Vertical Swiper controllers to change slides (up/down gestures trigger)
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight(0.5f)
                                .width(60.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceAround,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconButton(
                                    onClick = {
                                        if (currentIndex > 0) {
                                            viewModel.currentFeedIndex.value = currentIndex - 1
                                        }
                                    },
                                    modifier = Modifier.background(Color.Black.copy(0.3f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Prev", tint = Color.White)
                                }

                                IconButton(
                                    onClick = {
                                        viewModel.currentFeedIndex.value = currentIndex + 1
                                    },
                                    modifier = Modifier.background(Color.Black.copy(0.3f), CircleShape)
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Next", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Comment Drawer bottom sheet trigger
    if (showCommentDrawerForVideo != null) {
        val vidId = showCommentDrawerForVideo!!
        val activeVideo = videos.find { it.id == vidId }
        if (activeVideo != null) {
            CommentSectionDrawer(
                video = activeVideo,
                onClose = { showCommentDrawerForVideo = null },
                onAddComment = { text ->
                    viewModel.addCommentToVideo(vidId, text)
                }
            )
        }
    }

    // Modal Gifting Drawer trigger
    if (showShareDrawerForVideo != null) {
        val vidId = showShareDrawerForVideo!!
        val activeVideo = videos.find { it.id == vidId }
        val activeUser = session
        if (activeVideo != null) {
            GiftingPopupDrawer(
                video = activeVideo,
                currentUserCoins = activeUser?.coins ?: 0,
                onClose = { showShareDrawerForVideo = null },
                onSendGift = { cost ->
                    viewModel.sendGift(vidId, activeVideo.username, cost) { success ->
                        if (success) {
                            Toast.makeText(context, "Gift successfully sent to @${activeVideo.username}!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Insufficient Coins balance! Visit your Wallet to recharge.", Toast.LENGTH_LONG).show()
                            onNavigate(TokTikRoute.WALLET)
                        }
                    }
                }
            )
        }
    }
}

// Sub-component: Player renderer showing double tap floating hearts, live buffer indicators and active progress tracking
@Composable
fun VideoPlayerMock(video: CachedVideo, onDoubleTap: () -> Unit) {
    var isPlaying by remember { mutableStateOf(true) }
    var likesTapCount by remember { mutableStateOf(0) }
    var scaleHeart by remember { mutableStateOf(1f) }
    var isPreparing by remember { mutableStateOf(true) }

    // Reset preparing state for every new video
    LaunchedEffect(video.id) {
        isPreparing = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(video.id) {
                detectTapGestures(
                    onTap = {
                        isPlaying = !isPlaying
                    },
                    onDoubleTap = {
                        likesTapCount++
                        onDoubleTap()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        key(video.id) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(video.videoUrl))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(1f, 1f)
                            isPreparing = false
                            if (isPlaying) {
                                start()
                            }
                        }
                        setOnErrorListener { _, _, _ ->
                            isPreparing = false
                            true
                        }
                    }
                },
                update = { view ->
                    try {
                        if (isPlaying) {
                            view.start()
                        } else {
                            view.pause()
                        }
                    } catch (e: Exception) {
                        // Ignore any background video stream parsing errors
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Center spinning loader when preparing video connection stream
        if (isPreparing) {
            CircularProgressIndicator(
                color = RedPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(45.dp)
            )
        }

        // Overlay running playback visual sign
        if (!isPlaying && !isPreparing) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Paused icon indication",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
            )
        }

        // Animated double tap heart overlay effect
        if (likesTapCount > 0) {
            LaunchedEffect(key1 = likesTapCount) {
                scaleHeart = 1.6f
                delay(150)
                scaleHeart = 0f
            }

            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = RedPrimary,
                modifier = Modifier
                    .size(100.dp)
                    .scale(scaleHeart)
            )
        }

        // Gliding progress line loading indicator mimicking vertical streaming
        val infiniteTransition = rememberInfiniteTransition(label = "playback_progress")
        val progressAnim by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 15000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "progress"
        )
        val progress = if (isPlaying && !isPreparing) progressAnim else 0.45f

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(RedPrimary)
            )
        }
    }
}

// 5. UPLOAD CONTENT SCREEN
@Composable
fun UploadScreen(viewModel: TokTikViewModel, onBackPressed: () -> Unit, onPublished: () -> Unit) {
    var captionText by remember { mutableStateOf("") }
    var hashtagText by remember { mutableStateOf("") }
    var musicTitleText by remember { mutableStateOf("") }
    var isHdVideoCompressed by remember { mutableStateOf(true) }

    val isAiLoading by viewModel.isAiLoading
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Publish Live Video 🎬", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Rough Caption Input Text
            OutlinedTextField(
                value = captionText,
                onValueChange = { captionText = it },
                label = { Text("Write video description...", color = TextGray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = RedPrimary,
                    unfocusedBorderColor = BorderColor
                )
            )

            // Gemini AI Caption optimizer assistant trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (captionText.trim().isEmpty()) {
                            Toast.makeText(context, "Write a rough description first!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.runAiCaptionOptimizer(captionText) { response ->
                            captionText = response
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceSemiDark),
                    border = BorderStroke(1.dp, RedPrimary),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = RedPrimary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Smart Caption", color = Color.White, fontSize = 11.sp)
                    }
                }

                Button(
                    onClick = {
                        if (captionText.trim().isEmpty()) {
                            Toast.makeText(context, "Write a description topic first!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.runAiHashtagGenerator(captionText) { response ->
                            hashtagText = response
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceSemiDark),
                    border = BorderStroke(1.dp, CyanSecondary),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = CyanSecondary, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Hub, contentDescription = null, tint = CyanSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Smart Tags", color = Color.White, fontSize = 11.sp)
                    }
                }
            }

            // Hashtags Input field
            OutlinedTextField(
                value = hashtagText,
                onValueChange = { hashtagText = it },
                label = { Text("Hashtags (e.g. #foryou #creative)", color = TextGray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = CyanSecondary,
                    unfocusedBorderColor = BorderColor
                )
            )

            OutlinedTextField(
                value = musicTitleText,
                onValueChange = { musicTitleText = it },
                label = { Text("Audio Track (Leave blank for Ambient Mic)", color = TextGray) },
                leadingIcon = { Icon(Icons.Filled.MusicNote, contentDescription = null, tint = RedPrimary) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = RedPrimary,
                    unfocusedBorderColor = BorderColor
                )
            )

            // Compression toggle switch
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Render HD Video Format", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Render codec in H.264 HD resolution", fontSize = 12.sp, color = TextGray)
                    }
                    Switch(
                        checked = isHdVideoCompressed,
                        onCheckedChange = { isHdVideoCompressed = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = RedPrimary)
                    )
                }
            }

            // Action triggers (Draft vs Post)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        if (captionText.trim().isEmpty()) {
                            Toast.makeText(context, "Fill the title caption first!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addDraftVideo(
                            caption = captionText,
                            hashtags = hashtagText,
                            music = musicTitleText
                        )
                        Toast.makeText(context, "Saved to Creator Local Drafts!", Toast.LENGTH_SHORT).show()
                        onBackPressed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE DRAFT", color = Color.White)
                }

                Button(
                    onClick = {
                        if (captionText.trim().isEmpty()) {
                            Toast.makeText(context, "Caption is mandatory to publish!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        Toast.makeText(context, "Compiling codecs & broadcasting video...", Toast.LENGTH_LONG).show()
                        viewModel.publishVideo(
                            caption = captionText,
                            hashtags = hashtagText,
                            music = musicTitleText
                        )
                        onPublished()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("POST NOW", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI Video creative idea generator card!
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stuck? Get AI Creation Ideas!", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                    Text("Request creative ideas using Gemini API to jumpstart your camera recording content.", fontSize = 12.sp, color = TextGray)

                    Button(
                        onClick = {
                            viewModel.runAiContentHelper("Give me one popular micro-viral transition video concept challenge") { response ->
                                captionText = "[AI Idea] Let's try this challenge!"
                                hashtagText = "#AiCreative #TokTikChallenge"
                                Toast.makeText(context, "AI Idea generated! Check description field.", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        border = BorderStroke(0.5.dp, Color.Gray)
                    ) {
                        Text("GENERATE AI CONTENT CHALLENGE", fontSize = 11.sp, color = GoldAccent)
                    }
                }
            }
        }
    }
}

// 6. SEARCH DISCOVER SCREEN
@Composable
fun SearchScreen(viewModel: TokTikViewModel, onBackPressed: () -> Unit, onNavigate: (TokTikRoute) -> Unit) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val profiles by viewModel.searchProfiles.collectAsState()

    var activeTab by remember { mutableStateOf("Trending") } // "Trending", "Users", "Videos"

    Scaffold(
        bottomBar = {
            TokTikBottomNavigation(activeRoute = TokTikRoute.SEARCH, onRouteSelected = { onNavigate(it) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.onSearch(it) },
                    placeholder = { Text("Search users, hashtags, sounds...", color = TextGray) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = RedPrimary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = BorderColor
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = { viewModel.onSearch(""); onBackPressed() },
                    modifier = Modifier.background(SurfaceSemiDark, CircleShape)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            // Categories horizontal tab selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("Trending", "Users", "Videos").forEach { tab ->
                    Button(
                        onClick = { activeTab = tab },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeTab == tab) RedPrimary else SurfaceSemiDark
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(tab, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // Screen content list
            if (activeTab == "Trending") {
                // Hashtag discovery list
                val tags = listOf(
                    "#synthwave" to "2.4M Views",
                    "#cooking" to "5.8M Views",
                    "#cuteanimals" to "11.2M Views",
                    "#mechanicalkeyboard" to "850K Views",
                    "#AIcreative" to "1.1M Views",
                    "#TokTikChallenge" to "4.2M Views"
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Text("Trending Hashtags Today 🔥", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                    }
                    items(tags) { (tag, views) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onSearch(tag.substring(1)) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = CyanSecondary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(tag, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Text(views, color = TextGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else if (activeTab == "Users") {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (profiles.isEmpty()) {
                        item { Text("No creators discovered matching query.", color = TextGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
                    } else {
                        items(profiles) { prof ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.activeChatReceiver.value = prof
                                        onNavigate(TokTikRoute.CHAT_DETAIL)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(Color.DarkGray, CircleShape)
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.align(Alignment.Center))
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("@$prof", fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Professional TokTik Star • Tap to DM message", fontSize = 12.sp, color = TextGray)
                                    }
                                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextGray)
                                }
                            }
                        }
                    }
                }
            } else {
                // Video search result vertical grid
                if (results.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No matching videos found.", color = TextGray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results) { video ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                                modifier = Modifier
                                    .height(200.dp)
                                    .clickable {
                                        viewModel.currentFeedIndex.value = results.indexOf(video)
                                        onNavigate(TokTikRoute.HOME_FEED)
                                    }
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Simulated thumbnail text overlay
                                    AsyncImage(
                                        model = video.userAvatar,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomStart)
                                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                                            .padding(8.dp)
                                    ) {
                                        Text("@${video.username}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp, maxLines = 1)
                                        Text(video.caption, color = Color.LightGray, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 7. MESSAGE INBOX & DM NAVIGATION CENTER
@Composable
fun InboxScreen(
    viewModel: TokTikViewModel,
    onBackPressed: () -> Unit,
    onOpenChat: (String) -> Unit,
    onNavigate: (TokTikRoute) -> Unit
) {
    val notes by viewModel.notificationsList.collectAsState()
    var displayChannel by remember { mutableStateOf("Alerts") } // "Alerts", "Direct DMs"

    val recentsUsers = listOf("chef_elite", "neon_rider", "golden_paws", "keyboard_clicks")
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            TokTikBottomNavigation(activeRoute = TokTikRoute.INBOX, onRouteSelected = { onNavigate(it) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header toggling alerts vs direct messages
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "System Alerts",
                    color = if (displayChannel == "Alerts") Color.White else TextGray,
                    fontWeight = if (displayChannel == "Alerts") FontWeight.Bold else FontWeight.Normal,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .clickable { displayChannel = "Alerts" }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Divider(modifier = Modifier.height(16.dp).width(1.dp), color = TextGray)
                Text(
                    "Direct Messages (DMs)",
                    color = if (displayChannel == "DMs") Color.White else TextGray,
                    fontWeight = if (displayChannel == "DMs") FontWeight.Bold else FontWeight.Normal,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .clickable { displayChannel = "DMs" }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (displayChannel == "Alerts") {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    if (notes.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Inboxing completely quiet... ✨", color = TextGray)
                            }
                        }
                    } else {
                        items(notes) { alert ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                when (alert.type) {
                                                    "follow" -> RedPrimary
                                                    "like" -> PinkAccent
                                                    "live" -> CyanSecondary
                                                    else -> GoldAccent
                                                }, CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (alert.type) {
                                                "follow" -> Icons.Filled.PersonAdd
                                                "like" -> Icons.Filled.Favorite
                                                "live" -> Icons.Filled.LiveTv
                                                else -> Icons.Filled.NotificationsActive
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(alert.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                        Text(alert.body, fontSize = 12.sp, color = TextGray)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Direct message creators navigation list
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    items(recentsUsers) { user ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenChat(user) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .border(1.5.dp, CyanSecondary, CircleShape)
                                        .padding(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.DarkGray, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Person, contentDescription = null, tint = Color.LightGray)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("@$user", fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(modifier = Modifier.size(8.dp).background(Color.Green, CircleShape)) // Online green dot indicator
                                    }
                                    Text("Active Chat session • Online", fontSize = 12.sp, color = CyanSecondary)
                                }
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 7b. INDIVIDUAL CHAT DETAILS PAGE
@Composable
fun ChatDetailScreen(viewModel: TokTikViewModel, onBackPressed: () -> Unit) {
    val receiverName = viewModel.activeChatReceiver.value ?: "chef_elite"
    val messagesList by viewModel.getActiveChatFlow().collectAsState(initial = emptyList())
    var textMessage by remember { mutableStateOf("") }

    val isTyping by viewModel.isChatTyping
    val stateLog = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll to bottom on load
    LaunchedEffect(messagesList.size) {
        if (messagesList.isNotEmpty()) {
            stateLog.animateScrollToItem(messagesList.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(34.dp).background(Color.Gray, CircleShape)) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp).align(Alignment.Center))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("@$receiverName", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text(if (isTyping) "typing secure response..." else "Active Secure Session • Online", fontSize = 10.sp, color = CyanSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = stateLog,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Secure encryption badge warning
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark.copy(0.3f)),
                        border = BorderStroke(0.5.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Security, contentDescription = null, tint = CyanSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DMs messages are protected using virtual local encryption architecture.", fontSize = 10.sp, color = TextGray)
                        }
                    }
                }

                items(messagesList) { msg ->
                    val isMine = msg.senderUsername == "lucy_toktik"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMine) RedPrimary else SurfaceSemiDark
                            ),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMine) 16.dp else 2.dp,
                                bottomEnd = if (isMine) 2.dp else 16.dp
                            ),
                            modifier = Modifier.widthIn(max = 260.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(msg.messageText, color = Color.White, fontSize = 14.sp)
                                Row(
                                    modifier = Modifier.padding(top = 4.dp).align(Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Simulated • Sent", fontSize = 8.sp, color = Color.White.copy(0.7f))
                                    if (isMine) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Filled.DoneAll, contentDescription = null, tint = CyanSecondary, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (isTyping) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Writing response...", modifier = Modifier.padding(8.dp), fontSize = 11.sp, color = CyanSecondary)
                        }
                    }
                }
            }

            // Text message box row inputs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceSemiDark)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        textMessage = "Check out my new video draft! 🎬✨"
                    }
                ) {
                    Icon(Icons.Filled.AttachFile, contentDescription = "Add media", tint = Color.White)
                }

                OutlinedTextField(
                    value = textMessage,
                    onValueChange = { textMessage = it },
                    placeholder = { Text("Message...", color = TextGray, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = BorderColor
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (textMessage.trim().isNotEmpty()) {
                            viewModel.sendDirectMessage(textMessage)
                            textMessage = ""
                        }
                    },
                    modifier = Modifier.background(RedPrimary, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// 8. ACTIVE LIVE STREAM SCREEN
@Composable
fun LiveStreamScreen(viewModel: TokTikViewModel, onBackPressed: () -> Unit) {
    val viewerCount by viewModel.liveViewerCount
    val commentsList by viewModel.liveChatMessages.collectAsState()
    var commentText by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var heartSpawnCount by remember { mutableStateOf(0) }

    LaunchedEffect(key1 = true) {
        viewModel.startLiveSession()
    }

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF0F0005), Color(0xFF010101))))
                .padding(innerPadding)
        ) {
            // Live Stream Video Canvas Underlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { heartSpawnCount++ }
            ) {
                // Mock Video Gradient Ambient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    RedPrimary.copy(alpha = 0.15f),
                                    CyanSecondary.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Videocam, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("LIVE FEED INTEGRATING...", fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Streaming dynamically at 1080p 60FPS", fontSize = 11.sp, color = TextGray)
                }
            }

            // Top Status Bars (Live Banner with Viewer counts, end Button)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(RedPrimary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("LIVE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$viewerCount viewers", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = onBackPressed,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("END STREAM", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Simulated Floating hearts trigger
            if (heartSpawnCount > 0) {
                repeat(heartSpawnCount) { index ->
                    val transition = rememberInfiniteTransition(label = "hearts")
                    val heartOffset by transition.animateFloat(
                        initialValue = 100f,
                        targetValue = -500f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 2000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "floating_offset"
                    )

                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = when (index % 3) {
                            0 -> RedPrimary
                            1 -> CyanSecondary
                            else -> PinkAccent
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(y = heartOffset.dp, x = (-30 - (index * 15) % 100).dp)
                            .size(24.dp)
                    )
                }
            }

            // Chat feedback logs overlay (Bottom left)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .height(180.dp)
                        .fillMaxWidth(0.7f)
                        .background(Color.Black.copy(0.3f), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(commentsList) { (chatter, text) ->
                        Row {
                            Text("@$chatter: ", fontWeight = FontWeight.Bold, color = CyanSecondary, fontSize = 12.sp)
                            Text(text, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }

                // Chat text input bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        placeholder = { Text("Comment on livestream...", color = TextGray, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = BorderColor
                        ),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (commentText.trim().isNotEmpty()) {
                                viewModel.sendLiveComment(commentText)
                                commentText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                    ) {
                        Text("Send", color = Color.White)
                    }
                }
            }
        }
    }
}

// 9. PROFILE SCREEN (User Custom Information & Creator Studio navigates)
@Composable
fun UserProfileScreen(viewModel: TokTikViewModel, onBackPressed: () -> Unit, onNavigate: (TokTikRoute) -> Unit) {
    val session by viewModel.session.collectAsState()
    val videos by viewModel.videos.collectAsState()

    val context = LocalContext.current

    // Filter only videos belonging to active session user
    val userVideos = videos.filter { it.username == (session?.username ?: "lucy_toktik") }

    Scaffold(
        bottomBar = {
            TokTikBottomNavigation(activeRoute = TokTikRoute.USER_PROFILE, onRouteSelected = { onNavigate(it) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Top header with quick Back
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackPressed) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    text = "@${session?.username ?: "lucky_user"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                IconButton(onClick = { onNavigate(TokTikRoute.SETTINGS) }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }

            // Profile photo and Verified verification tag badges
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .border(3.dp, RedPrimary, CircleShape)
                        .padding(4.dp)
                ) {
                    AsyncImage(
                        model = session?.profilePicUrl?.ifEmpty { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=200" },
                        contentDescription = "My avatar profile",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                if (session?.isVerified == true) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(VerifiedBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Verified badge icon support", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Stats row (Followers, Following, Likes sum)
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${session?.following ?: 0}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    Text("Following", color = TextGray, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${session?.followers ?: 0}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    Text("Followers", color = TextGray, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val likeCount = userVideos.sumOf { it.likesCount }
                    Text("$likeCount", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    Text("Likes Count", color = TextGray, fontSize = 11.sp)
                }
            }

            // Profile Bio & Editable Social Web URL links
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = session?.bio ?: "Tap Edit profile to create customizable bio details!",
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 13.sp
                )

                if (session?.socialLinks?.isNotEmpty() == true) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.clickable {
                            Toast.makeText(context, "Navigating link details: ${session!!.socialLinks}", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Filled.Link, contentDescription = null, tint = CyanSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = session?.socialLinks ?: "",
                            fontSize = 11.sp,
                            color = CyanSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Action triggers panel (Edit Profile, Wallet, Creator Studio details, Admin dashboard)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onNavigate(TokTikRoute.SETTINGS) },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceSemiDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.EditNote, contentDescription = null, tint = Color.LightGray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EDIT", color = Color.White, fontSize = 11.sp)
                }

                Button(
                    onClick = { onNavigate(TokTikRoute.WALLET) },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceSemiDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = GoldAccent)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WALLET", color = Color.White, fontSize = 11.sp)
                }

                Button(
                    onClick = { onNavigate(TokTikRoute.CREATOR_STUDIO) },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceSemiDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.BarChart, contentDescription = null, tint = CyanSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ANALYTIC", color = Color.White, fontSize = 11.sp)
                }
            }

            // Quick bypass admin moderation dashboard panel shortcut for logged-in admins
            if (session?.isAdmin == true) {
                TextButton(
                    onClick = { onNavigate(TokTikRoute.ADMIN_PANEL) },
                    colors = ButtonDefaults.textButtonColors(contentColor = PinkAccent)
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("🛡️ ENTER SYSTEM ADMIN CONTROL DESK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // My posted reels vertical grid section placeholder views
            Text("Published Video Reels 🎥", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 16.dp))

            if (userVideos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CameraRoll, contentDescription = null, tint = TextGray, modifier = Modifier.size(44.dp))
                        Text("No publications posted yet! Use plus to post.", color = TextGray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(userVideos) { video ->
                        Box(
                            modifier = Modifier
                                .height(140.dp)
                                .background(SurfaceSemiDark)
                                .clickable {
                                    Toast.makeText(context, "Opened published reel: ${video.caption}", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            AsyncImage(
                                model = video.userAvatar,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = Color.White.copy(0.7f),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp)
                                    .size(16.dp)
                            )

                            // Quick Delete published video trigger for easy testing
                            IconButton(
                                onClick = {
                                    viewModel.deletePostedVideo(video)
                                    Toast.makeText(context, "Successfully deleted posted video reels!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(0.7f), CircleShape)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 10. WALLET COINS PURCHASE & TRANSLOGS DETAILS
@Composable
fun WalletScreen(viewModel: TokTikViewModel, onBackPressed: () -> Unit) {
    val session by viewModel.session.collectAsState()
    val transactions by viewModel.allTxs.collectAsState()
    val isUrdu = viewModel.isUrduSelected.value

    val context = LocalContext.current

    // Coin recharge billing dialog states
    var showPurchaseCoinsDialogAmount by remember { mutableStateOf<Int?>(null) }
    var purchaseCoinsCostRs by remember { mutableStateOf(0) }
    var purchaseTidInput by remember { mutableStateOf("") }

    // Withdrawal form states
    var withdrawAmountStr by remember { mutableStateOf("") }
    var withdrawMobileInput by remember { mutableStateOf("") }
    var withdrawalIsEasyPaisa by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { 
                    Text(
                        text = getLocalizedText("Earning & Wallet Center 💰", "کمائی اور والیٹ سنٹر 💰", isUrdu), 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White,
                        fontSize = 18.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.isUrduSelected.value = !viewModel.isUrduSelected.value
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceSemiDark),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Filled.Language, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isUrdu) "English" else "اردو",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Main Balance Card - Glassmorphism look
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                    border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(GoldAccent, Color.Transparent))),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(40.dp))
                        Text(
                            text = getLocalizedText("My Wallet Coins", "میرے والیٹ ٹوکنز", isUrdu), 
                            color = TextGray, 
                            fontSize = 12.sp
                        )
                        Text("${session?.coins ?: 0}", fontWeight = FontWeight.Bold, fontSize = 36.sp, color = Color.White)
                        Text(
                            text = getLocalizedText("Support creators during live streams!", "لائیو سٹریم کے دوران تخلیق کاروں کی حوصلہ افزائی کریں", isUrdu), 
                            fontSize = 11.sp, 
                            color = TextGray
                        )
                    }
                }

                // WATCH TO EARN CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = getLocalizedText("Watch & Earn Points Engine 📺", "ویڈیو دیکھو اور پوائنٹس کماؤ 📺", isUrdu), 
                                fontWeight = FontWeight.Bold, 
                                color = Color.White, 
                                fontSize = 14.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(PinkAccent.copy(0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("ACTIVE", color = PinkAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = getLocalizedText(
                                "Earn 10 points for every 4 seconds you watch videos on the Feed screen. Convert points directly to Rupees withdrawable cash!", 
                                "فیڈ سکرین پر ہر 4 سیکنڈ ویڈیو دیکھنے پر 10 پوائنٹس حاصل کریں۔ پوائنٹس کو براہ راست نکلوانے کے قابل نقد روپوں میں تبدیل کریں!",
                                isUrdu
                            ),
                            fontSize = 11.sp, 
                            color = TextGray,
                            lineHeight = 15.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = getLocalizedText("Current Watch Points", "موجودہ واچ پوائنٹس", isUrdu), 
                                    color = TextGray, 
                                    fontSize = 11.sp
                                )
                                Text("${session?.points ?: 0} Pts", fontWeight = FontWeight.Bold, color = PinkAccent, fontSize = 18.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.redeemPointsToRs { success, msg ->
                                        val localizedMsg = if (isUrdu) {
                                            if (success) "پوائنٹس کامیابی سے روپوں میں تبدیل کر دیے گئے ہیں!" else "تبدیلی کے لیے کم از کم 100 واچ پوائنٹس درکار ہیں!"
                                        } else msg
                                        Toast.makeText(context, localizedMsg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PinkAccent),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = getLocalizedText("REDEEM TO RS", "روپے میں تبدیل کریں", isUrdu), 
                                    color = Color.White, 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // WITHDRAW CASH CARD (10% Commission auto routed to Usama Arfi 03193736056)
                Text(
                    text = getLocalizedText("Withdrawable Cash Balance 💵", "قابلِ واپسی نقد بیلنس 💵", isUrdu), 
                    fontWeight = FontWeight.Bold, 
                    color = Color.White, 
                    fontSize = 15.sp
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = getLocalizedText("Available Cash:", "دستیاب رقم:", isUrdu), 
                                color = TextGray, 
                                fontSize = 12.sp
                            )
                            Text("Rs. ${session?.earningsRs ?: 0.0}", fontWeight = FontWeight.Bold, color = Color.Green, fontSize = 16.sp)
                        }

                        Divider(color = BorderColor)

                        Text(
                            text = getLocalizedText("Withdraw Funds via JazzCash / EasyPaisa", "جاز کیش / ایزی پیسہ کے ذریعے رقم نکلوائیں", isUrdu), 
                            fontWeight = FontWeight.Bold, 
                            color = Color.White, 
                            fontSize = 12.sp
                        )
                        
                        // Amount input
                        OutlinedTextField(
                            value = withdrawAmountStr,
                            onValueChange = { withdrawAmountStr = it },
                            label = { 
                                Text(
                                    text = getLocalizedText("Enter Amount (Min Rs. 50)", "رقم درج کریں (کم از کم 50 روپے)", isUrdu), 
                                    color = TextGray, 
                                    fontSize = 11.sp
                                ) 
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.Green),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Mobile number input
                        OutlinedTextField(
                            value = withdrawMobileInput,
                            onValueChange = { withdrawMobileInput = it },
                            label = { 
                                Text(
                                    text = getLocalizedText("Recipient Mobile Account Number", "رقم وصول کرنے والے کا موبائل نمبر", isUrdu), 
                                    color = TextGray, 
                                    fontSize = 11.sp
                                ) 
                            },
                            placeholder = { Text("e.g. 03001234567", color = TextGray) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = Color.Green),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Method selection row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = getLocalizedText("Select Payment Wallet:", "ادائیگی کے والیٹ کا انتخاب کریں:", isUrdu), 
                                color = Color.White, 
                                fontSize = 12.sp, 
                                modifier = Modifier.weight(1f)
                            )
                            
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { withdrawalIsEasyPaisa = false }) {
                                RadioButton(selected = !withdrawalIsEasyPaisa, onClick = { withdrawalIsEasyPaisa = false }, colors = RadioButtonDefaults.colors(selectedColor = Color.Green))
                                Text("JazzCash", color = Color.LightGray, fontSize = 11.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { withdrawalIsEasyPaisa = true }) {
                                RadioButton(selected = withdrawalIsEasyPaisa, onClick = { withdrawalIsEasyPaisa = true }, colors = RadioButtonDefaults.colors(selectedColor = Color.Green))
                                Text("EasyPaisa", color = Color.LightGray, fontSize = 11.sp)
                            }
                        }

                        Text(
                            text = getLocalizedText(
                                "Please note: A 10% Platform royalty fee is automatically deducted from this withdrawal and credited straight to the parent merchant account Usama Arfi (03193736056) for video server hosting and streaming maintenance.",
                                "براہ کرم نوٹ کریں: کمپنی کی لائیو ہوسٹنگ اور ویڈیو سٹریمنگ سروسز کی مد میں کل رقم کا 10 فیصد خود بخود منہا کر کے پیرنٹ مرچنٹ اکاؤنٹ اسامہ عارفی (03193736056) کو کریڈٹ ہو جائے گا۔",
                                isUrdu
                            ),
                            color = TextGray,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )

                        Button(
                            onClick = {
                                val amtNum = withdrawAmountStr.toDoubleOrNull()
                                if (amtNum == null || amtNum < 50) {
                                    Toast.makeText(
                                        context, 
                                        getLocalizedText("Please enter a valid amount (Minimum Rs. 50)!", "براہ کرم کم از کم 50 روپے کی رقم درج کریں!", isUrdu), 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                val mob = withdrawMobileInput.trim()
                                if (mob.length != 11 || !mob.startsWith("03")) {
                                    Toast.makeText(
                                        context, 
                                        getLocalizedText("Please enter a valid 11-digit mobile number starting with 03!", "براہ کرم موبی لنک/ٹیلی نار کا درست 11 ہندسوں کا موبائل نمبر درج کریں جو 03 سے شروع ہو!", isUrdu), 
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                viewModel.requestWithdrawRs(
                                    amountRs = amtNum,
                                    mobileNo = mob,
                                    isEasyPaisa = withdrawalIsEasyPaisa
                                ) { success, description ->
                                    val localizedDesc = if (isUrdu) {
                                        if (success) "رقم کی واپسی کی درخواست کامیابی سے رجسٹر ہو گئی ہے! 10% فیس وضع کر کے بقیہ رقم بھیج دی جائے گی۔" else "اکاؤنٹ میں بیلنس ناکافی ہے!"
                                    } else description
                                    Toast.makeText(context, localizedDesc, Toast.LENGTH_LONG).show()
                                    if (success) {
                                        withdrawAmountStr = ""
                                        withdrawMobileInput = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = getLocalizedText("REQUEST NATIVE WITHDRAW (90% Payout)", "رقم نکالنے کی درخواست کریں (90% ادائیگی)", isUrdu), 
                                color = Color.Black, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // RECHARGE COINS PACKAGES SEC
                Text(
                    text = getLocalizedText("Purchase Coins Packages ⚡", "والیٹ سیکیور کوائنز پیکجز ⚡", isUrdu), 
                    fontWeight = FontWeight.Bold, 
                    color = Color.White, 
                    fontSize = 15.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        Triple(100, 150, "Rs. 150"),
                        Triple(500, 700, "Rs. 700"),
                        Triple(1000, 1300, "Rs. 1300")
                    ).forEach { (amount, costRs, label) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                            border = BorderStroke(0.5.dp, BorderColor),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    showPurchaseCoinsDialogAmount = amount
                                    purchaseCoinsCostRs = costRs
                                    purchaseTidInput = ""
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("+$amount", fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 15.sp)
                                Text(
                                    text = getLocalizedText("Coins", "ٹوکنز", isUrdu), 
                                    color = Color.White, 
                                    fontSize = 11.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .background(RedPrimary, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(label, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Wallet ledger audit credits logs history list
                Text(
                    text = getLocalizedText("Wallet Transaction Audit History 🧾", "والیٹ ٹرانزیکشن کی تفصیلات 🧾", isUrdu), 
                    fontWeight = FontWeight.Bold, 
                    color = Color.White, 
                    fontSize = 15.sp
                )

                if (transactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = getLocalizedText("Log records empty.", "ٹرانزیکشن ریکارڈز خالی ہیں۔", isUrdu), 
                            color = TextGray
                        )
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            transactions.forEach { tx ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(tx.description, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            text = getLocalizedText("Secure Registry Transaction Log", "سیکیور رجسٹری لاگ", isUrdu), 
                                            fontSize = 9.sp, 
                                            color = TextGray
                                        )
                                    }

                                    Text(
                                        text = (if (tx.isCredit) "+" else "-") + "${tx.amount} P",
                                        fontWeight = FontWeight.Bold,
                                        color = if (tx.isCredit) Color.Green else Color.Red,
                                        fontSize = 11.sp
                                    )
                                }
                                Divider(color = BorderColor, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // SIMULATED COINS DEPOSIT BILLING MODAL DIALOG popup
            if (showPurchaseCoinsDialogAmount != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(0.85f))
                        .clickable { /* Block bubble clicks */ }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = getLocalizedText("Official Merchant Payment Portal 📱", "آفیشل جاز کیش ٹرانزیکشن پورٹل 📱", isUrdu), 
                                fontWeight = FontWeight.Bold, 
                                color = Color.White, 
                                fontSize = 16.sp
                            )
                            
                            Text(
                                text = if (isUrdu) {
                                    "آفیشل ٹوکنز پیکج **${showPurchaseCoinsDialogAmount} کوائنز** خریدنے کے لیے، **${purchaseCoinsCostRs} روپے** اسامہ عارفی کے آفیشل جاز کیش بزنس اکاؤنٹ پر بھیجیں:\n\n" +
                                    "👉 **03193736056**\n\n" +
                                    "رقم بھیجنے کے بعد موصول ہونے والی 11 یا 12 ہندسوں کی ٹرانزیکشن آئی ڈی (TID) نیچے درج کریں تاکہ آپ کے ٹوکنز فوری والٹ میں شامل کیے جا سکیں!"
                                } else {
                                    "To purchase **${showPurchaseCoinsDialogAmount} Coins**, please transfer **Rs. ${purchaseCoinsCostRs}** to " +
                                    "Usama Arfi's Merchant JazzCash Business Mobile Account:\n\n" +
                                    "👉 **03193736056**\n\n" +
                                    "After transferring, enter your 11 or 12-digit Deposit Transaction ID (TID) below to instantly credit your wallet!"
                                },
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            OutlinedTextField(
                                value = purchaseTidInput,
                                onValueChange = { purchaseTidInput = it },
                                label = { 
                                    Text(
                                        text = getLocalizedText("Enter JazzCash Transaction ID (TID)", "11 یا 12 ہندسوں کی آئی ڈی درج کریں", isUrdu), 
                                        color = TextGray
                                    ) 
                                },
                                placeholder = { Text("e.g. 10248596324", color = TextGray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = GoldAccent),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { showPurchaseCoinsDialogAmount = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = getLocalizedText("CANCEL", "منسوخ کریں", isUrdu)
                                    )
                                }

                                Button(
                                    onClick = {
                                        val tidClean = purchaseTidInput.trim()
                                        if (tidClean.length < 11 || tidClean.length > 12 || !tidClean.all { it.isDigit() }) {
                                            Toast.makeText(
                                                context, 
                                                getLocalizedText("Please enter a valid 11 or 12-digit transaction ID containing only numbers!", "براہ کرم موبی لنک سے وصول شدہ درست 11 یا 12 ہندسوں کی ٹرانزیکشن آئی ڈی درج کریں!", isUrdu), 
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@Button
                                        }
                                        viewModel.purchaseCoinsWithJazzCash(
                                            amountCoins = showPurchaseCoinsDialogAmount!!,
                                            costRs = purchaseCoinsCostRs,
                                            txnId = tidClean
                                        ) { success ->
                                            if (success) {
                                                val msgSuccess = if (isUrdu) "ادائیگی کی تصدیق ہو گئی! آپ کے اکاؤنٹ میں ${showPurchaseCoinsDialogAmount} ٹوکنز شامل کر دیے گئے ہیں۔ ✨" else "Payment verified! Credited ${showPurchaseCoinsDialogAmount} Coins! ✨"
                                                Toast.makeText(context, msgSuccess, Toast.LENGTH_LONG).show()
                                                showPurchaseCoinsDialogAmount = null
                                            } else {
                                                val msgFail = if (isUrdu) "آئی ڈی کی تصدیق ناکام رہی۔ براہ کرم درست نمبر درج کریں۔" else "Payment verification failed. Please check transaction log."
                                                Toast.makeText(context, msgFail, Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = getLocalizedText("VERIFY TRANSFER", "تصدیق کریں", isUrdu), 
                                        color = Color.Black, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 11. CREATOR SHEDULE ANALYTICS DASHBOARD CHART FOR CREATORS
@Composable
fun CreatorStudioScreen(viewModel: TokTikViewModel, onBackPressed: () -> Unit) {
    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Creator Portfolio Studio 📊", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Weekly Performance Metrics", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

            // Dynamic Custom Canvas Charts depicting video impressions
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Video Impressions", color = TextGray, fontSize = 12.sp)
                            Text("+45.2% Weekly Increase 📈", fontWeight = FontWeight.Bold, color = Color.Green, fontSize = 13.sp)
                        }
                        Box(
                            modifier = Modifier
                                .background(CyanSecondary.copy(0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("7 Days", color = CyanSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Simulated Line Chart using canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        val points = listOf(10f, 40f, 25f, 60f, 45f, 90f, 75f)
                        val stepX = size.width / 6f
                        val scaleY = size.height / 100f

                        // Draw path points
                        for (i in 0 until points.size - 1) {
                            val startX = i * stepX
                            val startY = size.height - (points[i] * scaleY)
                            val endX = (i + 1) * stepX
                            val endY = size.height - (points[i + 1] * scaleY)

                            drawLine(
                                color = RedPrimary,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 6f
                            )

                            drawCircle(
                                color = CyanSecondary,
                                radius = 8f,
                                center = Offset(startX, startY)
                            )
                        }
                        // Last circle point
                        drawCircle(
                            color = CyanSecondary,
                            radius = 8f,
                            center = Offset(size.width, size.height - points.last() * scaleY)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                            Text(day, color = TextGray, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Quick details summary block cards list
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    Triple("Active Profile Views", "1,845 clicks", Icons.Filled.Visibility),
                    Triple("Engagement rate proportion", "12.8% benchmark", Icons.Filled.ThumbsUpDown),
                    Triple("Coins gift reward payouts", "450 virtual gold", Icons.Filled.EmojiEvents)
                ).forEach { (indicator, rateMs, icon) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, tint = RedPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(indicator, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text(rateMs, color = TextGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 12. SYSTEM SETTINGS & GENERAL PROFILE EDITORS
@Composable
fun SettingsScreen(viewModel: TokTikViewModel, onBackPressed: () -> Unit, onLoggedOut: () -> Unit) {
    val session by viewModel.session.collectAsState()
    val isDarkMode by viewModel.isDarkMode

    var bioStr by remember { mutableStateOf(session?.bio ?: "") }
    var linksStr by remember { mutableStateOf(session?.socialLinks ?: "") }
    var privateAccountState by remember { mutableStateOf(session?.isPrivate ?: false) }

    val context = LocalContext.current

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Profile Settings ⚙️", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Modify Creator Details", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)

            OutlinedTextField(
                value = bioStr,
                onValueChange = { bioStr = it },
                label = { Text("Edit Bio Summary", color = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = RedPrimary,
                    unfocusedBorderColor = BorderColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = linksStr,
                onValueChange = { linksStr = it },
                label = { Text("Social handle Links URLs", color = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = RedPrimary,
                    unfocusedBorderColor = BorderColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Switch items settings
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Private Account Mode", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Restrict video downloads to followers", fontSize = 11.sp, color = TextGray)
                        }
                        Switch(
                            checked = privateAccountState,
                            onCheckedChange = { privateAccountState = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = RedPrimary)
                        )
                    }

                    Divider(color = BorderColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Vibrant Application Dark Theme", fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Alternate layouts color palettes", fontSize = 11.sp, color = TextGray)
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.isDarkMode.value = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = RedPrimary)
                        )
                    }
                }
            }

            // Verify Badge Upgrade action button!
            if (session?.isVerified == false) {
                Button(
                    onClick = {
                        viewModel.upgradeToVerifiedStatus()
                        Toast.makeText(context, "Verification successfully granted!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CLAIM FREE VERIFIED BADGE BADGE ✔️", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Save details profile action
            Button(
                onClick = {
                    viewModel.updateProfileSettings(bioStr, linksStr, privateAccountState)
                    Toast.makeText(context, "Creator details successfully updated!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SAVE PROFILE DETAILS", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout account action trigger
            Button(
                onClick = {
                    viewModel.handleLogout()
                    Toast.makeText(context, "Logged out of current TokTik session!", Toast.LENGTH_SHORT).show()
                    onLoggedOut()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("LOGOUT OF TOKTIK", fontWeight = FontWeight.Bold)
            }
        }
    }
}
@Composable
fun AdminPanelScreen(viewModel: TokTikViewModel, onBackPressed: () -> Unit) {
    val videos by viewModel.videos.collectAsState()
    val session by viewModel.session.collectAsState()

    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    // Song Registration states
    var songNameInput by remember { mutableStateOf("") }
    
    // Dialog states for editing video details
    var videoToEditId by remember { mutableStateOf<String?>(null) }
    var editCaptionState by remember { mutableStateOf("") }
    var editHashtagsState by remember { mutableStateOf("") }
    var editMusicState by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { 
                    Column {
                        Text("Owner Admin Console 👑", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Active: Usama Arfi (03193736056)", color = CyanSecondary, fontSize = 11.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // High level stats section
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("SYSTEM OVERVIEW", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp, letterSpacing = 1.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Videos Loaded: ${videos.size}", color = Color.LightGray, fontSize = 13.sp)
                            Text("Registered Songs: ${viewModel.trendingSongs.size}", color = Color.LightGray, fontSize = 13.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Admin Balance: Rs. ${session?.earningsRs ?: 0.0}", color = GoldAccent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Auto-Commissions: 10% ACTIVE", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // SEC 1: Trending Track Registry
                Text("Update Trending Soundtracks", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Instantly insert new music tracks. These will update everyone's Music Selector when recording/uploading videos!", fontSize = 11.sp, color = TextGray)
                        
                        OutlinedTextField(
                            value = songNameInput,
                            onValueChange = { songNameInput = it },
                            placeholder = { Text("e.g. Mere Humsafar - @Farhan Saeed 🎵", color = TextGray, fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CyanSecondary,
                                unfocusedBorderColor = BorderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (songNameInput.trim().isEmpty()) {
                                    Toast.makeText(context, "Enter song name first!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.addNewTrendingSong(songNameInput)
                                Toast.makeText(context, "Song verified & updated to TokTik servers! 🎶", Toast.LENGTH_SHORT).show()
                                songNameInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanSecondary),
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("REGISTER NEW SONG", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                // SEC 2: Live Video Editing & Virality Control Desk
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Moderate Database Content (${videos.size})", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by user, caption, or hashtag...", color = TextGray, fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = RedPrimary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = RedPrimary,
                        unfocusedBorderColor = BorderColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                val filteredVideos = if (searchQuery.trim().isEmpty()) {
                    videos
                } else {
                    videos.filter { 
                        it.username.contains(searchQuery, ignoreCase = true) || 
                        it.caption.contains(searchQuery, ignoreCase = true) ||
                        it.hashtags.contains(searchQuery, ignoreCase = true)
                    }
                }

                if (filteredVideos.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No match records found", color = TextGray, fontSize = 12.sp)
                    }
                }

                filteredVideos.forEach { video ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                        border = BorderStroke(0.5.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color.DarkGray, CircleShape)
                                    ) {
                                        Text(
                                            text = video.username.take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.align(Alignment.Center)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("@${video.username}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    if (video.isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Filled.Verified, contentDescription = null, tint = CyanSecondary, modifier = Modifier.size(14.dp))
                                    }
                                }

                                if (video.isBlocked) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.Red.copy(0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("BLOCKED / HIDDEN", color = Color.Red, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.Green.copy(0.2f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("LIVE FEED", color = Color.Green, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text("Caption: ${video.caption}", color = Color.White, fontSize = 12.sp)
                            Text("Hashtags: ${video.hashtags}", color = CyanSecondary, fontSize = 11.sp)
                            Text("Music Track: ${video.musicName}", color = TextGray, fontSize = 11.sp)
                            Text("Pre-Stats: Views: ${video.viewsCount} | Likes: ${video.likesCount} | Comments: ${video.commentsCount}", color = GoldAccent, fontSize = 10.sp)

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // EDIT TEXT BUTTON
                                Button(
                                    onClick = {
                                        videoToEditId = video.id
                                        editCaptionState = video.caption
                                        editHashtagsState = video.hashtags
                                        editMusicState = video.musicName
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("EDIT TEXT 📝", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                // GO VIRAL BOOST BUTTON
                                Button(
                                    onClick = {
                                        viewModel.adminBoostVideoViral(video.id)
                                        Toast.makeText(context, "Viral Algorithm Boosted! Added 150K views & 9.5K likes instantly! 🚀", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("VIRAL BOOST 🚀", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }

                                // BAN/BLOCK VIDEO TOGGLE
                                Button(
                                    onClick = {
                                        viewModel.adminToggleVideoBlock(video.id)
                                        Toast.makeText(context, "Video status updated!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (video.isBlocked) Color.Gray else Color(0xFFAC1B3E)),
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = if (video.isBlocked) "UNBLOCK VIDEO UNBAN" else "BLOCK VIDEO BAN", 
                                        fontSize = 10.sp, 
                                        color = Color.White, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // POPUP DIALOG FOR REAL EDITING
            if (videoToEditId != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(0.85f))
                        .clickable { /* prevent bubble clicks fallback */ }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("Edit Video details (ID: $videoToEditId) 📝", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

                            OutlinedTextField(
                                value = editCaptionState,
                                onValueChange = { editCaptionState = it },
                                label = { Text("Caption text", color = TextGray) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RedPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = editHashtagsState,
                                onValueChange = { editHashtagsState = it },
                                label = { Text("Hashtags", color = TextGray) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RedPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = editMusicState,
                                onValueChange = { editMusicState = it },
                                label = { Text("Background sound track text", color = TextGray) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = RedPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { videoToEditId = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("CANCEL")
                                }

                                Button(
                                    onClick = {
                                        viewModel.adminUpdateVideoText(
                                            videoToEditId!!,
                                            editCaptionState,
                                            editHashtagsState,
                                            editMusicState
                                        )
                                        Toast.makeText(context, "Successfully updated live text! ✨", Toast.LENGTH_SHORT).show()
                                        videoToEditId = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("SAVE TEXT", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Global Horizontal Navigation Bar styling
@Composable
fun TokTikBottomNavigation(activeRoute: TokTikRoute, onRouteSelected: (TokTikRoute) -> Unit) {
    Column {
        Divider(color = Color.White.copy(alpha = 0.12f), thickness = 0.5.dp)
        NavigationBar(
            containerColor = Color(0xFF010101),
            tonalElevation = 0.dp,
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            NavigationBarItem(
                selected = activeRoute == TokTikRoute.HOME_FEED,
                onClick = { onRouteSelected(TokTikRoute.HOME_FEED) },
                icon = { Icon(if (activeRoute == TokTikRoute.HOME_FEED) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home", modifier = Modifier.size(24.dp)) },
                label = { Text("Home", fontSize = 10.sp, fontWeight = if (activeRoute == TokTikRoute.HOME_FEED) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                selected = activeRoute == TokTikRoute.SEARCH,
                onClick = { onRouteSelected(TokTikRoute.SEARCH) },
                icon = { Icon(if (activeRoute == TokTikRoute.SEARCH) Icons.Filled.Search else Icons.Outlined.Search, contentDescription = "Search", modifier = Modifier.size(24.dp)) },
                label = { Text("Discover", fontSize = 10.sp, fontWeight = if (activeRoute == TokTikRoute.SEARCH) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = Color.Transparent
                )
            )

            // Floating Neon Central PLUS trigger upload button to post videos!
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .offset(y = (-2).dp)
                    .clickable { onRouteSelected(TokTikRoute.UPLOAD) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(width = 46.dp, height = 30.dp)
                ) {
                    // Aqua background shifted left
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 30.dp)
                            .align(Alignment.CenterStart)
                            .offset(x = (-3).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF69C9D0))
                    )
                    // Pink background shifted right
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 30.dp)
                            .align(Alignment.CenterEnd)
                            .offset(x = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEE1D52))
                    )
                    // White center
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 30.dp)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Upload Reels",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            NavigationBarItem(
                selected = activeRoute == TokTikRoute.INBOX,
                onClick = { onRouteSelected(TokTikRoute.INBOX) },
                icon = { Icon(if (activeRoute == TokTikRoute.INBOX) Icons.Filled.Inbox else Icons.Outlined.Inbox, contentDescription = "Inbox Messages", modifier = Modifier.size(24.dp)) },
                label = { Text("Inbox", fontSize = 10.sp, fontWeight = if (activeRoute == TokTikRoute.INBOX) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = Color.Transparent
                )
            )

            NavigationBarItem(
                selected = activeRoute == TokTikRoute.USER_PROFILE,
                onClick = { onRouteSelected(TokTikRoute.USER_PROFILE) },
                icon = { Icon(if (activeRoute == TokTikRoute.USER_PROFILE) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "My Profile", modifier = Modifier.size(24.dp)) },
                label = { Text("Profile", fontSize = 10.sp, fontWeight = if (activeRoute == TokTikRoute.USER_PROFILE) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

// Auxiliary Floating bottom subdrawer popup comment listing
@Composable
fun CommentSectionDrawer(
    video: CachedVideo,
    onClose: () -> Unit,
    onAddComment: (String) -> Unit
) {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val commentsAdapter = moshi.adapter<List<MessageComment>>(
        Types.newParameterizedType(List::class.java, MessageComment::class.java)
    )

    val currentComments = try {
        commentsAdapter.fromJson(video.commentsJson) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    var commentTypedText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.4f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .clickable(enabled = false, onClick = {}) // prevent tap-through dismissal
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${currentComments.size} comments",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Comment Sheet", tint = Color.White)
                    }
                }

                // Scroll list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentComments) { comment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.DarkGray, CircleShape)
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.align(Alignment.Center))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("@${comment.username}", fontWeight = FontWeight.Bold, color = TextGray, fontSize = 12.sp)
                                Text(comment.commentText, color = Color.White, fontSize = 13.sp)
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(comment.timestamp, color = TextGray, fontSize = 10.sp)
                                    Text("Reply", color = CyanSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
                                Text("${comment.likes}", color = TextGray, fontSize = 9.sp)
                            }
                        }
                    }
                }

                // Add text comment row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = commentTypedText,
                        onValueChange = { commentTypedText = it },
                        placeholder = { Text("Add comment...", color = TextGray, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = BorderColor
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            if (commentTypedText.trim().isNotEmpty()) {
                                onAddComment(commentTypedText)
                                commentTypedText = ""
                            }
                        },
                        modifier = Modifier.background(RedPrimary, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Comment", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// Auxiliary Floating bottom popup Gifting center
@Composable
fun GiftingPopupDrawer(
    video: CachedVideo,
    currentUserCoins: Int,
    onClose: () -> Unit,
    onSendGift: (Int) -> Unit
) {
    val giftsList = listOf(
        Triple("Golden Rose 🌹", 10, "Sends an ambient glow rose to the creator's live inbox"),
        Triple("Neon Trophy 🏆", 100, "Spawns a glorious double animated trophy burst"),
        Triple("TokTik Diamond 💎", 500, "Supreme gift triggering live channels broadcast alert")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.4f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .clickable(enabled = false, onClick = {}) // prevent tapdismiss
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Send Virtual Creator Gift 🎁", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Text("My Wallet: $currentUserCoins Coins", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Gift Drawer", tint = Color.White)
                    }
                }

                // Gifting options grid
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(giftsList) { (giftName, cost, desc) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(0.3f)),
                            border = BorderStroke(0.5.dp, BorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSendGift(cost)
                                    onClose()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(giftName, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(desc, color = TextGray, fontSize = 10.sp)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .background(GoldAccent.copy(0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(12.dp))
                                    Text("$cost C", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Formatted views count helper
fun formattedCount(count: Int): String {
    return when {
         count >= 1000000 -> "${count / 1000000}M"
         count >= 1000 -> "${count / 1000}K"
         else -> "$count"
    }
}
