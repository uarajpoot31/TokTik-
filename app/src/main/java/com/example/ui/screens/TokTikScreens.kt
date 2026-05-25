@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui.screens

import android.widget.Toast
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
                            text = "OTP successfully sent to secure simulation logs!",
                            fontSize = 11.sp,
                            color = CyanSecondary,
                            modifier = Modifier.padding(top = 4.dp).align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (usernameInput.trim().isEmpty()) {
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
                                    username = usernameInput,
                                    isGoogle = false,
                                    isFB = false
                                )
                                if (isSignUpMode && bioInput.isNotEmpty()) {
                                    viewModel.updateProfileSettings(bioInput, "instagram.com/$usernameInput", false)
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
                        Toast.makeText(context, "Google simulation account authorized!", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "Facebook simulation account authorized!", Toast.LENGTH_SHORT).show()
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
            if (videos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = RedPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Optimizing feed codecs...", color = TextGray)
                    }
                }
            } else {
                val currentVideo = videos.getOrNull(currentIndex % videos.size)
                if (currentVideo != null) {
                    // Full-bleed Video details rendering with customized overlays
                    Box(modifier = Modifier.fillMaxSize()) {

                        // Underlay Background static image (since we simulate native player beautifully)
                        AsyncImage(
                            model = currentVideo.userAvatar,
                            contentDescription = "Fallback background",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(alpha = 0.15f)
                        )

                        // Interactive Video Canvas simulating audio progress and playback
                        VideoPlayerMock(
                            video = currentVideo,
                            onDoubleTap = {
                                viewModel.toggleLike(currentVideo.id)
                            }
                        )

                        // Navigation overlay (Top bar Following vs For You)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        feedFilterMode = "Following"
                                        Toast.makeText(context, "Switching to following feed creators!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    "Following",
                                    color = if (feedFilterMode == "Following") Color.White else Color.White.copy(alpha = 0.6f),
                                    fontWeight = if (feedFilterMode == "Following") FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 16.sp
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
                                    "For You",
                                    color = if (feedFilterMode == "For You") Color.White else Color.White.copy(alpha = 0.6f),
                                    fontWeight = if (feedFilterMode == "For You") FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 16.sp
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
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (currentVideo.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (currentVideo.isLiked) RedPrimary else Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = formattedCount(currentVideo.likesCount),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Comments Button (Interactive)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = { showCommentDrawerForVideo = currentVideo.id },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Comment,
                                        contentDescription = "Comments",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Text(
                                    text = formattedCount(currentVideo.commentsCount),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Gift Sender Panel (Interactive monetization coins system)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = { showShareDrawerForVideo = currentVideo.id },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CardGiftcard,
                                        contentDescription = "Gifts Support",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Text(
                                    text = "GIFT",
                                    color = GoldAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Share and save panel
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(
                                    onClick = {
                                        viewModel.toggleSave(currentVideo.id)
                                        Toast.makeText(context, if (currentVideo.isSaved) "Video removed from bookmarks!" else "Saved to Bookmarks folder!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (currentVideo.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (currentVideo.isSaved) GoldAccent else Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Text(
                                    text = formattedCount(currentVideo.sharesCount),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
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
                                    .size(40.dp)
                                    .rotate(rotationDegrees)
                                    .background(Color(0xFF1E1E24), CircleShape)
                                    .border(4.dp, Color.Black, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color.Red, CircleShape)
                                )
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
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

// Sub-component: Mock player renderer showing double tap floating hearts
@Composable
fun VideoPlayerMock(video: CachedVideo, onDoubleTap: () -> Unit) {
    var isPlaying by remember { mutableStateOf(true) }
    var likesTapCount by remember { mutableStateOf(0) }
    var scaleHeart by remember { mutableStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                isPlaying = !isPlaying
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        likesTapCount++
                        onDoubleTap()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Overlay running playback visual sign
        if (!isPlaying) {
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

        // Static progress line loading indicator mimicking vertical streaming
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
                    .fillMaxWidth(0.35f) // simulated progress state duration
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

            // Quick bypass admin moderation dashboard panel shortcut for testers
            TextButton(
                onClick = { onNavigate(TokTikRoute.ADMIN_PANEL) },
                colors = ButtonDefaults.textButtonColors(contentColor = PinkAccent)
            ) {
                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tester? Open Moderator Admin Panel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

    val context = LocalContext.current

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Coins Wallet Hub 💰", fontWeight = FontWeight.Bold, color = Color.White) },
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
            // Balanced main coin banner with glassmorphism looks
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(GoldAccent, Color.Transparent))),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(48.dp))
                    Text("Total Coins Balance", color = TextGray, fontSize = 13.sp)
                    Text("${session?.coins ?: 0}", fontWeight = FontWeight.Bold, fontSize = 42.sp, color = Color.White)
                    Text("Support creators during livestream battles utilizing coins!", fontSize = 11.sp, color = TextGray, textAlign = TextAlign.Center)
                }
            }

            // Quick Purchase Coin Bundle list options
            Text("Recharge Simulated Package Bundle ⚡", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(100 to "$0.99", 500 to "$4.99", 1000 to "$8.99").forEach { (amount, cost) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                        border = BorderStroke(0.5.dp, BorderColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.purchaseCoinsPack(amount)
                                Toast.makeText(context, "Added $amount coins bundle simulation!", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("+$amount", fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 15.sp)
                            Text("Coins", color = Color.White, fontSize = 11.sp)
                            Box(
                                modifier = Modifier
                                    .background(RedPrimary, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(cost, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Wallet ledger audit credits logs history list
            Text("Wallet Transaction Audit History 🧾", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)

            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("Log records empty.", color = TextGray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(SurfaceSemiDark, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(tx.description, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Codecs virtual credit ledger", fontSize = 9.sp, color = TextGray)
                            }

                            Text(
                                text = (if (tx.isCredit) "+" else "-") + "${tx.amount} C",
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

// 13. ADMIN MODERATOR ACTION PANEL VIEW
@Composable
fun AdminPanelScreen(viewModel: TokTikViewModel, onBackPressed: () -> Unit) {
    val videos by viewModel.videos.collectAsState()
    val session by viewModel.session.collectAsState()

    val context = LocalContext.current
    var simulatedTotalBannedFlag by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Moderator Administration Desk 🛡️", fontWeight = FontWeight.Bold, color = Color.White) },
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
            Text("Content Moderation & Reports", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)

            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total Verified Creators: ${videos.distinctBy { it.username }.size}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Pending Inappropriate Content Reports: 1", color = RedPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Gemini AI Automated Filter Protection: ENABLED", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Reports item lists simulation
            Text("Active Content Incident Ticket Reports", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

            Card(colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Anonymous User Report #0412", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .background(Color.Yellow.copy(0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("PENDING", color = Color.Yellow, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text("Report Object: Video Reels of @neon_rider", fontSize = 11.sp, color = TextGray)
                    Text("Issue Reported: Simulated speed racing might encourage unsafe habits.", fontSize = 11.sp, color = Color.LightGray)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Incident report marked resolved (No fault found)!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("DISMISS", fontSize = 10.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Creator neon_rider warned successfully!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("WARN CREATOR", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }

            // Simulated user database ban administration list
            Text("TokTik Creators Ban Control Desk", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

            Card(colors = CardDefaults.cardColors(containerColor = SurfaceSemiDark)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("neon_rider", "chef_elite", "golden_paws", "keyboard_clicks").forEach { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("@$user", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)

                            Button(
                                onClick = {
                                    Toast.makeText(context, "Simulation Action: User @$user access controls toggled!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                                border = BorderStroke(0.5.dp, BorderColor)
                            ) {
                                Text("LIMIT PRIVILEGES", fontSize = 9.sp, color = RedPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Divider(color = BorderColor, thickness = 0.5.dp)
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
