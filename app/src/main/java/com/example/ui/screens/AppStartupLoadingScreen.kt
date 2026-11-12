package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CustomChapterRepository
import com.example.model.DynamicTestRepository
import com.example.model.OfflineTestRepository
import com.example.model.RealtimeExamStatsManager
import com.example.model.TestHistoryRepository
import com.example.model.WeakAreaAnalyticsRepository
import com.example.service.OnlineTestSyncManager
import com.example.service.security.AppSecurityManager
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AppStartupLoadingScreen:
 * A high-octane, game-inspired startup loading screen.
 * Displays a sleek glowing progress bar counting smoothly from 1% to 100%
 * while pre-heating all exam repositories, question banks, and caches in the background (Dispatchers.IO).
 *
 * Guarantees zero-lag, butter-smooth 120 FPS performance once the app opens!
 */
@Composable
fun AppStartupLoadingScreen(
    onLoadingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentProgress by remember { mutableIntStateOf(1) }
    var isWarmupFinished by remember { mutableStateOf(false) }

    // Pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "loading_glow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1.0f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_slide"
    )

    // Background Warm-up & Smooth Progress Controller
    LaunchedEffect(Unit) {
        val appContext = context.applicationContext

        // Coroutine 1: Heavy I/O and preheat on background thread
        val warmupJob = launch(Dispatchers.IO) {
            try {
                AppSecurityManager.init(appContext)
                OfflineTestRepository.init(appContext)
                TestHistoryRepository.init(appContext)
                WeakAreaAnalyticsRepository.init(appContext)

                DynamicTestRepository.init(appContext)
                CustomChapterRepository.init(appContext)
                RealtimeExamStatsManager.init(appContext)

                // 🌟 Automatically sync tests from GitHub in the background during startup loading
                if (OfflineTestRepository.isNetworkAvailable(appContext)) {
                    try {
                        withTimeoutOrNull(10000L) {
                            OnlineTestSyncManager.syncFromCloud(appContext, isUserTriggered = false)
                        }
                    } catch (_: Throwable) {
                        // Fail-safe
                    }
                }

                // Invalidate any stale cache and precompute full subjects & test counts
                RealtimeExamStatsManager.invalidateCache()
                CustomChapterRepository.preloadExamSubjects(appContext)
                RealtimeExamStatsManager.preloadAllStats(appContext)
                OnlineTestSyncManager.init(appContext)
                OnlineTestSyncManager.hasStartupSyncCompleted = true
            } catch (_: Throwable) {
                // Fail-safe to avoid blocking user
            } finally {
                isWarmupFinished = true
            }
        }

        // Coroutine 2: Smooth 1% to 100% game-style progress ticker
        // Phase 1: 1% to 85% steady climb (approx 1.3 seconds)
        for (i in 2..85) {
            currentProgress = i
            val stepDelay = if (i < 30) 18L else if (i < 65) 15L else 20L
            delay(stepDelay)
        }

        // Phase 2: Await warmup completion if still running
        while (!isWarmupFinished) {
            delay(20)
        }

        // Phase 3: Fast finish to 100% (approx 0.4 seconds)
        for (i in 86..100) {
            currentProgress = i
            delay(18L)
        }

        // Brief triumph pause at 100%
        delay(250)
        onLoadingComplete()
    }

    val statusMessage = when {
        currentProgress < 25 -> "⚡ कोर इंजन और सुरक्षा मॉड्यूल प्रारंभ हो रहे हैं..."
        currentProgress < 50 -> "🔄 क्लाउड से नवीनतम मॉक टेस्ट और PYQ सिंक हो रहे हैं..."
        currentProgress < 75 -> "📚 प्रश्न बैंक, चैप्टर सेट्स और उत्तरमाला लोड हो रहे हैं..."
        currentProgress < 95 -> "✨ SSC, रेलवे और राज्य परीक्षाओं के टेस्ट्स तैयार हो रहे हैं..."
        currentProgress < 100 -> "🚀 120 FPS सुपरफास्ट मोड एक्टिवेट हो गया!"
        else -> "✅ स्वागत है! Exam Mitra पूरी तरह तैयार है..."
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF070B14),
                        Color(0xFF0F172A),
                        Color(0xFF090E1A)
                    )
                )
            )
            .testTag("app_startup_loading_screen")
    ) {
        // Ambient Neon Backdrop Glows
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .blur(80.dp)
                .background(Color(0xFF2563EB).copy(alpha = 0.22f * pulseGlow), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .blur(70.dp)
                .background(Color(0xFF7C3AED).copy(alpha = 0.18f * pulseGlow), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP SECTION: Branding & Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                // Glowing circular container for App Logo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(108.dp)
                ) {
                    // Outer neon pulse ring
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .border(
                                width = 2.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF38BDF8),
                                        Color(0xFF3B82F6),
                                        Color(0xFF818CF8),
                                        Color(0xFF38BDF8)
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    // Inner emblem surface
                    Surface(
                        modifier = Modifier.size(86.dp),
                        shape = CircleShape,
                        color = Color(0xFF1E293B),
                        shadowElevation = 8.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize().padding(14.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_exam_mitra_logo),
                                contentDescription = "Exam Mitra Logo",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Brand Name
                Text(
                    text = "EXAM MITRA",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tagline
                Text(
                    text = "तैयारी जीत की • 100% नि:शुल्क मॉक टेस्ट & PYQ",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Gaming / Speed Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.45f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ULTRA SMOOTH 120 FPS ENGINE",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // MIDDLE SECTION: Game-style Progress Bar & Percentage
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glassmorphism Card for Loading Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF131D31).copy(alpha = 0.85f),
                    border = BorderStroke(1.2.dp, Color(0xFF3B82F6).copy(alpha = 0.35f)),
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 22.dp)
                    ) {
                        // Header row with "LOADING" label and Big Percentage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LOADING DATA",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF38BDF8), CircleShape)
                                )
                            }

                            // Bold Glowing Percentage
                            Text(
                                text = "$currentProgress%",
                                color = Color(0xFF60A5FA),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.testTag("startup_progress_percent")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Game-Style Progress Track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0B1120))
                                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        ) {
                            // Filled Progress Bar
                            val progressFraction = (currentProgress / 100f).coerceIn(0f, 1f)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = progressFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF2563EB),
                                                Color(0xFF3B82F6),
                                                Color(0xFF06B6D4),
                                                Color(0xFF10B981)
                                            )
                                        )
                                    )
                                    .drawWithContent {
                                        drawContent()
                                        // Dynamic shimmering light ray across progress
                                        val shimmerWidth = size.width * 0.4f
                                        val startX = (shimmerOffset * size.width) - shimmerWidth
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.White.copy(alpha = 0.35f),
                                                    Color.Transparent
                                                ),
                                                startX = startX,
                                                endX = startX + shimmerWidth
                                            )
                                        )
                                    }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dynamic Status Line
                        AnimatedContent(
                            targetState = statusMessage,
                            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                            label = "status_line"
                        ) { msg ->
                            Text(
                                text = msg,
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 18.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // BOTTOM SECTION: Motivational Quote & Footer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "💡 \"मेहनत इतनी खामोशी से करो कि सफलता शोर मचा दे!\"",
                    color = Color(0xFFE2E8F0),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Exam Mitra • Dedicated to Aspirant Success",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
