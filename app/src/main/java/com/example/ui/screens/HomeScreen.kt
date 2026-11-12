package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.service.AuthManager
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import com.example.service.AppShareHelper
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import com.example.model.ExamCategory
import com.example.model.OfflineTestRepository
import com.example.model.QuizQuestion
import com.example.model.SavedQuestionsRepository
import com.example.model.SubExam
import com.example.model.UnlockedTestsRepository
import com.example.model.examCategories
import com.example.ui.components.ExamCategoryCard
import com.example.ui.components.ExamMitraTopBar
import com.example.ui.components.GreetingHeader
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.RewardAdUnlockDialog
import com.example.ui.components.rememberIsOnline
import androidx.compose.material3.AlertDialog
import com.example.ui.theme.CardBlueStart
import com.example.ui.theme.Primary40
import com.example.ui.theme.ScreenBackground
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    onNavigateToPractice: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        SavedQuestionsRepository.initRepository(context, scope)
    }

    val savedQuestions = SavedQuestionsRepository.getReactiveSavedQuestions()

    var selectedCategoryForSheet by remember { mutableStateOf<ExamCategory?>(null) }
    var activeTestModalTitle by remember { mutableStateOf<String?>(null) }
    var activeTestQuestions by remember { mutableStateOf<List<QuizQuestion>?>(null) }
    var activeTestDurationMinutes by remember { mutableStateOf<Int?>(null) }
    var pendingTestToUnlock by remember { mutableStateOf<Triple<String, List<QuizQuestion>?, Int?>?>(null) }
    var showSavedQuestionsDialog by remember { mutableStateOf(false) }
    var showOfflineUnlockNotice by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_content"),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // App Title Top Bar (EXAM MITRA logo + name + bell)
            item {
                ExamMitraTopBar(
                    onNotificationClick = onNavigateToNotifications
                )
            }

            // Real-time Offline Warning Banner
            item {
                val isOnline = rememberIsOnline()
                OfflineStatusBanner(isOnline = isOnline)
            }

            // Hello {User}! Choose Your Exam greeting
            item {
                val userName = AuthManager.currentUser?.displayName ?: "विद्यार्थी"
                GreetingHeader(name = userName)
            }

            // 3 Main Exam Category Cards: SSC Exam, Railway Exam, and State Exam (Individual items with unique keys for 120 FPS recycling)
            items(
                items = examCategories,
                key = { it.id }
            ) { category ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    ExamCategoryCard(
                        category = category,
                        onClick = {
                            selectedCategoryForSheet = category
                        }
                    )
                }
            }

            // 🌟 "Saved Questions" Dedicated Button directly under SSC, Railway & State Exam cards
            item(key = "saved_questions_card_button") {
                val savedInteractionSource = remember { MutableInteractionSource() }
                val isSavedPressed by savedInteractionSource.collectIsPressedAsState()
                val savedScale by animateFloatAsState(
                    targetValue = if (isSavedPressed) 0.98f else 1.0f,
                    label = "saved_btn_scale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(116.dp)
                            .scale(savedScale)
                            .testTag("saved_questions_main_button"),
                        shape = RoundedCornerShape(22.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 3.dp,
                            pressedElevation = 6.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFFF59E0B), // Warm Gold
                                            Color(0xFFD97706), // Amber
                                            Color(0xFFB45309)  // Deep Amber
                                        )
                                    )
                                )
                                .clickable(
                                    interactionSource = savedInteractionSource,
                                    indication = null,
                                    onClick = {
                                        showSavedQuestionsDialog = true
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // White circular badge container for bookmark icon
                                    Surface(
                                        modifier = Modifier.size(76.dp),
                                        shape = CircleShape,
                                        color = Color.White,
                                        shadowElevation = 2.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Bookmark,
                                                contentDescription = "Saved Questions",
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(42.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Saved Questions",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = if (savedQuestions.isNotEmpty()) "${savedQuestions.size} प्रश्न सुरक्षित • टेस्ट दें" else "सेव किए गए प्रश्न देखें",
                                            fontSize = 12.5.sp,
                                            color = Color.White.copy(alpha = 0.92f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Right Chevron Circle Button
                                Surface(
                                    modifier = Modifier.size(34.dp),
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Open Saved Questions",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Sheet for sub-exams when tapping any category card
        selectedCategoryForSheet?.let { category ->
            CategoryDetailBottomSheet(
                category = category,
                onDismiss = { selectedCategoryForSheet = null },
                onStartTest = { testTitle, questions, durationMinutes ->
                    val isUnlocked = UnlockedTestsRepository.isTestUnlocked(testTitle)
                    if (isUnlocked) {
                        // Already unlocked! Directly launch test without ad (works offline & online!)
                        activeTestModalTitle = testTitle
                        activeTestQuestions = questions ?: OfflineTestRepository.getOfflineQuestions(context, testTitle)
                        activeTestDurationMinutes = durationMinutes
                    } else {
                        // Needs 1-time ad watch to unlock lifetime free
                        if (OfflineTestRepository.isNetworkAvailable(context)) {
                            pendingTestToUnlock = Triple(testTitle, questions, durationMinutes)
                        } else {
                            showOfflineUnlockNotice = true
                        }
                    }
                }
            )
        }

        // Saved Questions Dialog Modal
        if (showSavedQuestionsDialog) {
            SavedQuestionsDialog(
                onDismiss = { showSavedQuestionsDialog = false },
                onStartTest = { testTitle, questions ->
                    showSavedQuestionsDialog = false
                    activeTestModalTitle = testTitle
                    activeTestQuestions = questions
                    activeTestDurationMinutes = null
                }
            )
        }

        // Rewarded Video Ad Unlock Dialog (1 Ad = Lifetime Free)
        pendingTestToUnlock?.let { (title, questions, durationMinutes) ->
            RewardAdUnlockDialog(
                testTitle = title,
                onDismiss = { pendingTestToUnlock = null },
                onTestUnlocked = {
                    pendingTestToUnlock = null
                    activeTestModalTitle = title
                    activeTestQuestions = questions
                    activeTestDurationMinutes = durationMinutes
                    // Automatically cache for lifetime offline use!
                    OfflineTestRepository.saveTestOffline(context, title, questions)
                }
            )
        }

        // Offline notice if user is offline and test is not yet unlocked
        if (showOfflineUnlockNotice) {
            AlertDialog(
                onDismissRequest = { showOfflineUnlockNotice = false },
                shape = RoundedCornerShape(16.dp),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚠️ ऑफ़लाइन मोड (Offline Mode)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                },
                text = {
                    Text(
                        text = "यह टेस्ट पहली बार अनलॉक करने के लिए सिर्फ 1 बार ऑनलाइन Ad देखने की आवश्यकता है।\n\nएक बार ऑनलाइन देखने के बाद यह टेस्ट आपके डिवाइस में सेव हो जाएगा और आप इसे हमेशा बिना इंटरनेट (Offline) भी दे सकते हैं!",
                        fontSize = 13.5.sp,
                        color = Color(0xFF334155),
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showOfflineUnlockNotice = false },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("समझ गया (OK)")
                    }
                }
            )
        }

        // Active Interactive Quiz / Mock Test Dialog
        activeTestModalTitle?.let { title ->
            MockTestDialog(
                testTitle = title,
                customQuestions = activeTestQuestions,
                durationMinutes = activeTestDurationMinutes,
                onDismiss = {
                    activeTestModalTitle = null
                    activeTestQuestions = null
                    activeTestDurationMinutes = null
                }
            )
        }
    }
}
