package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.DateRange
import com.example.service.GeminiQuestionParserService
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.example.model.QuizQuestion
import com.example.model.SavedQuestionsRepository
import com.example.model.sampleQuizQuestions
import com.example.ui.theme.ThemeManager
import com.example.ui.theme.ScreenBackground
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardSecondary
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.QuizScreenBg
import com.example.ui.theme.QuizQuestionTextColor
import com.example.ui.theme.QuizPillBg
import com.example.ui.theme.QuizPillText
import com.example.ui.util.TextFormatHelper
import com.example.ui.util.QuizBitmapCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image

/**
 * Isolated Countdown Timer Badge to prevent recomposition of the entire Quiz Screen every 1000ms.
 * Only this tiny pill recomposes on each second tick.
 */
@Composable
fun QuizCountdownTimerPill(
    timeLeftSeconds: Int,
    modifier: Modifier = Modifier
) {
    val isUrgent = timeLeftSeconds < 300
    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val timeFormatted = remember(minutes, seconds) {
        String.format("%02d:%02d", minutes, seconds)
    }

    val isDark = ThemeManager.isDarkMode
    val pillBg = when {
        isUrgent && isDark -> Color(0xFF7F1D1D).copy(alpha = 0.4f)
        isUrgent -> Color(0xFFFEF2F2)
        isDark -> SurfaceCardSecondary
        else -> Color(0xFFE2EBE0)
    }
    val pillBorder = when {
        isUrgent && isDark -> Color(0xFFDC2626)
        isUrgent -> Color(0xFFFECACA)
        isDark -> CardBorderColor
        else -> Color(0xFFC7D7C3)
    }
    val pillTint = when {
        isUrgent -> Color(0xFFEF4444)
        isDark -> TextSecondary
        else -> Color(0xFF2E5E2A)
    }
    val pillText = when {
        isUrgent -> Color(0xFFEF4444)
        isDark -> TextPrimary
        else -> Color(0xFF1E3A1A)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = pillBg,
        border = BorderStroke(1.dp, pillBorder),
        shadowElevation = 1.dp,
        modifier = modifier.height(28.dp).testTag("test_countdown_timer")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Time Remaining",
                tint = pillTint,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = timeFormatted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = pillText
            )
        }
    }
}

@Composable
fun OptionContentDisplay(
    prefix: String,
    optionText: String,
    textColor: Color,
    isBold: Boolean = false,
    modifier: Modifier = Modifier
) {
    val trimmed = optionText.trim()
    val isImageOption = trimmed.startsWith("data:image", true) ||
            trimmed.startsWith("http://") ||
            trimmed.startsWith("https://") ||
            (trimmed.startsWith("/") && (trimmed.endsWith(".jpg") || trimmed.endsWith(".png") || trimmed.endsWith(".webp")))

    if (isImageOption) {
        Column(modifier = modifier.padding(vertical = 4.dp)) {
            Text(
                text = prefix,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            QuestionImageDisplay(
                imageUrl = trimmed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )
        }
    } else {
        Text(
            text = TextFormatHelper.parseMarkdown("$prefix$optionText"),
            fontSize = 16.sp,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
            modifier = modifier
        )
    }
}

@Composable
fun QuestionImageDisplay(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    if (imageUrl.isNullOrBlank()) return
    val context = LocalContext.current
    var showFullImageDialog by remember { mutableStateOf(false) }

    val decodedBitmap = remember(imageUrl) {
        QuizBitmapCache.resolveBitmapSync(context, imageUrl)
    }

    val imageModel: Any = when {
        decodedBitmap != null -> decodedBitmap
        imageUrl.startsWith("/") -> java.io.File(imageUrl)
        else -> imageUrl
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp)),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "प्रश्न आकृति (Problem Figure)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                // Compact pill zoom button
                Surface(
                    onClick = { showFullImageDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "बड़ा देखें",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
            }

            // Image Container with proper click & zoom hint
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFullImageDialog = true }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (decodedBitmap != null) {
                    Image(
                        bitmap = decodedBitmap.asImageBitmap(),
                        contentDescription = "Reasoning Diagram",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 280.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    coil.compose.SubcomposeAsyncImage(
                        model = coil.request.ImageRequest.Builder(context)
                            .data(if (imageUrl.startsWith("/")) java.io.File(imageUrl) else imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Reasoning Diagram",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 280.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF8FAFC)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        loading = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF2563EB)
                                    )
                                    Text("आकृति लोड हो रही है...", fontSize = 11.5.sp, color = Color(0xFF475569))
                                }
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .padding(6.dp)
                                    .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚠️ चित्र लोड नहीं हो सका",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    if (showFullImageDialog) {
        var scale by remember { mutableStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 5f)
            if (scale > 1f) {
                offset += panChange
            } else {
                offset = Offset.Zero
            }
        }

        Dialog(
            onDismissRequest = {
                scale = 1f
                offset = Offset.Zero
                showFullImageDialog = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(18.dp)),
                color = Color.White,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "प्रश्न आकृति (HD Zoom View)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "पिंच या डबल-टैप करके ज़ूम करें (${(scale * 100).toInt()}%)",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                                showFullImageDialog = false
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Zoom Controls bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Zoom In Button
                        Surface(
                            onClick = { scale = (scale + 0.5f).coerceAtMost(5f) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                Text("ज़ूम इन (+)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                            }
                        }

                        // Zoom Out Button
                        Surface(
                            onClick = {
                                val newScale = (scale - 0.5f).coerceAtLeast(1f)
                                scale = newScale
                                if (newScale == 1f) offset = Offset.Zero
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                Text("ज़ूम आउट (-)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                            }
                        }

                        // Reset Zoom Button
                        Surface(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = "Reset", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                Text("रीसेट", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive Zoomable Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        if (scale > 1.2f) {
                                            scale = 1f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                        }
                                    }
                                )
                            }
                            .transformable(state = transformState),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (decodedBitmap != null) {
                                Image(
                                    bitmap = decodedBitmap.asImageBitmap(),
                                    contentDescription = "Reasoning Diagram Full View",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            } else {
                                coil.compose.SubcomposeAsyncImage(
                                    model = coil.request.ImageRequest.Builder(context)
                                        .data(if (imageUrl.startsWith("/")) java.io.File(imageUrl) else imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Reasoning Diagram Full View",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                    loading = {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Color(0xFF2563EB))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun isDelhiPoliceExam(title: String): Boolean {
    return com.example.model.ExamSubjectManager.isDelhiPolice(title)
}

fun getQuizSubjects(title: String, questions: List<QuizQuestion> = emptyList()): List<String> {
    return com.example.model.ExamSubjectManager.getQuizSubjects(title, questions)
}

@Composable
fun MockTestDialog(
    testTitle: String,
    customQuestions: List<QuizQuestion>? = null,
    durationMinutes: Int? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isDelhiPolice = remember(testTitle) { isDelhiPoliceExam(testTitle) }

    LaunchedEffect(Unit) {
        SavedQuestionsRepository.initRepository(context, scope)
    }

    val isOnline = remember { com.example.model.OfflineTestRepository.isNetworkAvailable(context) }

    val questions = remember(testTitle, customQuestions) {
        customQuestions
            ?: com.example.model.OfflineTestRepository.getOfflineQuestions(context, testTitle)
            ?: com.example.model.ExamSubjectManager.getQuestionsForExam(testTitle)
    }

    val isMultiSubjectFull = remember(testTitle, questions) {
        com.example.model.ExamSubjectManager.isMultiSubjectFullExam(testTitle, questions)
    }

    val examSections = remember(testTitle, questions) {
        com.example.model.ExamSubjectManager.getExamSections(testTitle, questions)
    }

    val quizSubjects = remember(examSections) {
        examSections.map { it.name }
    }

    val initialDurationMinutes = remember(testTitle, questions, durationMinutes) {
        if (durationMinutes != null && durationMinutes > 0) {
            durationMinutes
        } else {
            val isMultiSubject = isMultiSubjectFull || questions.size >= 70
            if (isMultiSubject) {
                if (questions.size > 80) 90 else 60
            } else {
                val titleLower = testTitle.lowercase()
                val isTitleMathOrReasoning = titleLower.contains("math") || titleLower.contains("गणित") ||
                        titleLower.contains("संख्यात्मक") || titleLower.contains("quant") ||
                        titleLower.contains("reason") || titleLower.contains("तर्क") ||
                        titleLower.contains("रीजनिंग") || titleLower.contains("बुद्धिमत्ता")

                val mathOrReasoningQCount = questions.count { q ->
                    val s = q.subject.lowercase()
                    s.contains("math") || s.contains("गणित") || s.contains("quant") ||
                    s.contains("reason") || s.contains("तर्क") || s.contains("रीजनिंग") ||
                    com.example.model.CustomChapterRepository.isMathsQuestion(q) ||
                    com.example.model.CustomChapterRepository.isReasoningQuestion(q)
                }

                val isMathOrReasoning = isTitleMathOrReasoning || (questions.isNotEmpty() && mathOrReasoningQCount.toFloat() / questions.size.toFloat() >= 0.4f)

                if (isMathOrReasoning) {
                    // Exactly 2 minutes per question (e.g. 15 questions = 30 mins, 25 questions = 50 mins)
                    questions.size * 2
                } else {
                    if (questions.size <= 20) maxOf(10, questions.size) else (questions.size * 1.0).toInt()
                }
            }
        }
    }

    // Silently save test and images in background for offline access without blocking the test
    LaunchedEffect(testTitle, questions) {
        if (questions.isNotEmpty()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.example.model.OfflineTestRepository.markTestUnlockedAndOffline(context, testTitle, questions)
                    com.example.model.OfflineTestRepository.saveTestAndImagesToPhone(context, testTitle, questions)
                } catch (_: Exception) {}
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }

    // Map of answers: questionIndex -> selectedOptionIndex
    val userAnswers = remember { mutableStateMapOf<Int, Int>() }
    // Marked for review set
    val markedForReview = remember { mutableStateMapOf<Int, Boolean>() }
    // Visited questions tracking: questionIndex -> Boolean
    val visitedQuestions = remember { mutableStateMapOf<Int, Boolean>() }
    // Auto-mark current question as visited
    LaunchedEffect(currentIndex) {
        visitedQuestions[currentIndex] = true
    }

    var showQuestionPalette by remember { mutableStateOf(false) }

    var isFinished by remember { mutableStateOf(false) }
    var showSubmitConfirmDialog by remember { mutableStateOf(false) }
    // Testbook-style Solution & Review Mode
    var isReviewMode by remember { mutableStateOf(false) }
    var reviewQuestionIndex by remember { mutableIntStateOf(0) }
    var showAllIndiaDetails by remember { mutableStateOf(false) }


    var showAutoSubmitBanner by remember { mutableStateOf(false) }

    // Dynamic Countdown Timer (2 min per question for Maths/Reasoning, 90 mins for full mock tests)
    var timeLeftSeconds by remember(initialDurationMinutes) { mutableIntStateOf(initialDurationMinutes * 60) }
    LaunchedEffect(isFinished) {
        while (!isFinished && timeLeftSeconds > 0) {
            delay(1000)
            timeLeftSeconds--
        }
        // Auto submit test when timer expires
        if (!isFinished && timeLeftSeconds <= 0) {
            showSubmitConfirmDialog = false
            showAutoSubmitBanner = true
            isFinished = true
        }
    }

    // Preload all quiz question images in background IO thread for zero-latency question navigation
    LaunchedEffect(questions) {
        if (questions.isNotEmpty()) {
            val urls = mutableListOf<String>()
            for (q in questions) {
                if (!q.imageUrl.isNullOrBlank()) urls.add(q.imageUrl)
                for (opt in q.options) {
                    val t = opt.trim()
                    if (t.startsWith("data:image", true) || t.startsWith("http") || (t.startsWith("/") && (t.endsWith(".jpg") || t.endsWith(".png") || t.endsWith(".webp")))) {
                        urls.add(t)
                    }
                }
            }
            if (urls.isNotEmpty()) {
                QuizBitmapCache.preloadImages(context, urls)
            }
        }
    }


                
                // Confirmation dialog before submitting the mock test
                if (showSubmitConfirmDialog) {
                    val totalAnswered = userAnswers.size
                    val totalQuestions = questions.size
                    val unattempted = totalQuestions - totalAnswered

                    Dialog(
                        onDismissRequest = { showSubmitConfirmDialog = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .clip(RoundedCornerShape(16.dp)),
                            color = Color.White,
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF22C55E),
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Submit Test?",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E281C)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Total Questions: $totalQuestions\nAttempted: $totalAnswered | Left: $unattempted\n\nKya aap test submit karke result dekhna chahte hain?",
                                    fontSize = 13.sp,
                                    color = Color(0xFF4A5568),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        onClick = { showSubmitConfirmDialog = false },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE2E8F0),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "Cancel",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                    }
                                    Surface(
                                        onClick = {
                                            showSubmitConfirmDialog = false
                                            isFinished = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF22C55E),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "Yes, Submit",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Question Palette Dialog Component
                if (showQuestionPalette) {
                    QuestionPaletteDialog(
                        questions = questions,
                        currentIndex = if (isReviewMode) reviewQuestionIndex else currentIndex,
                        userAnswers = userAnswers,
                        markedForReview = markedForReview,
                        visitedQuestions = visitedQuestions,
                        isReviewMode = isReviewMode,
                        testTitle = testTitle,
                        onDismiss = { showQuestionPalette = false },
                        onSelectQuestion = { selectedIndex ->
                            if (isReviewMode) {
                                reviewQuestionIndex = selectedIndex
                            } else {
                                currentIndex = selectedIndex
                            }
                            showQuestionPalette = false
                        }
                    )
                }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        // Full screen dialog matching the clean canvas
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("mock_test_dialog"),
            color = ScreenBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                if (questions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "इस टेस्ट में अभी कोई प्रश्न उपलब्ध नहीं है।",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("बंद करें (Close)")
                            }
                        }
                    }
                } else if (!isFinished) {
                    val safeIndex = if (currentIndex in questions.indices) currentIndex else 0
                    val currentQ = questions[safeIndex]
                    val currentAnswer = userAnswers[safeIndex]
                    val isMarked = markedForReview[safeIndex] == true

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            // Dynamic section mapping based on official exam subject structure
                            val activeSubjectIndex = remember(currentIndex, examSections) {
                                val idx = examSections.indexOfFirst { currentIndex in it.range }
                                if (idx >= 0) idx else 0
                            }
                            val currentSection = examSections.getOrNull(activeSubjectIndex)
                            val subjectQuestionNumber = if (isMultiSubjectFull) {
                                currentSection?.let { (currentIndex - it.range.first) + 1 } ?: (currentIndex + 1)
                            } else {
                                currentIndex + 1
                            }
                            val subjectTotalQuestions = if (isMultiSubjectFull) {
                                currentSection?.questionCount ?: questions.size
                            } else {
                                questions.size
                            }

                            // Top Info Bar: Question pill, offline indicator, and control buttons (Timer, Grid, Submit)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Question Number Pill (Displays Q 1/20 for each subject)
                                    Box(
                                        modifier = Modifier
                                            .height(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(QuizPillBg)
                                            .padding(horizontal = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Q $subjectQuestionNumber/$subjectTotalQuestions",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = QuizPillText
                                        )
                                    }

                                    if (!isOnline) {
                                        Box(
                                            modifier = Modifier
                                                .height(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (ThemeManager.isDarkMode) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFECFDF5))
                                                .padding(horizontal = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "⚡ ऑफ़लाइन",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (ThemeManager.isDarkMode) Color(0xFF34D399) else Color(0xFF059669)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Countdown Timer / Clock Pill (to the left of Grid button) - isolated to prevent full screen recompositions
                                    QuizCountdownTimerPill(timeLeftSeconds = timeLeftSeconds)

                                    // Question Palette / Grid Icon Button
                                    Surface(
                                        onClick = { showQuestionPalette = true },
                                        shape = RoundedCornerShape(6.dp),
                                        color = QuizPillBg,
                                        shadowElevation = 1.dp,
                                        modifier = Modifier.height(28.dp).testTag("open_question_palette_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Apps,
                                                contentDescription = "Question Grid",
                                                tint = QuizPillText,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Grid",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = QuizPillText
                                            )
                                        }
                                    }

                                    // Top Submit Button (Clean, compact, fixed 28dp height, perfectly proportioned)
                                    Surface(
                                        onClick = { showSubmitConfirmDialog = true },
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF16A34A), // Vibrant emerald green submit button
                                        shadowElevation = 1.dp,
                                        modifier = Modifier.height(28.dp).testTag("top_submit_test_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Submit Test",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Submit",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Official Subjects Selector Tabs - Shown ONLY in Multi-Subject Full Tests (hidden in Chapter-Wise and Subject-Wise)
                            if (isMultiSubjectFull && quizSubjects.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("quiz_subjects_tabs"),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    quizSubjects.forEachIndexed { subIndex, subjectName ->
                                        val isSelected = activeSubjectIndex == subIndex
                                        Surface(
                                            onClick = {
                                                val targetIndex = examSections.getOrNull(subIndex)?.range?.first ?: 0
                                                if (targetIndex < questions.size) {
                                                    currentIndex = targetIndex
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) QuizPillBg else SurfaceCardSecondary,
                                            shadowElevation = if (isSelected) 2.dp else 0.dp,
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .testTag("quiz_subject_tab_$subIndex")
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = subjectName,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isSelected) QuizPillText else TextSecondary,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Scrollable Question & Options Content
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                // Question Text in Hindi: e.g. "Q 1: निम्नलिखित में से कौन सी पदार्थ की सबसे छोटी इकाई है?"
                                Text(
                                    text = parseSimpleMarkdown("Q $subjectQuestionNumber: ${currentQ.question}"),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = QuizQuestionTextColor,
                                    lineHeight = 26.sp
                                )

                                QuestionImageDisplay(imageUrl = currentQ.imageUrl)

                                // Exam, Date & Shift metadata card placed between Question and Options for Chapter-wise tests
                                val questionShiftInfo = remember(currentQ, testTitle, isMultiSubjectFull) {
                                    getQuestionExamShiftBadge(currentQ, testTitle, isMultiSubjectFull)
                                }

                                if (!isMultiSubjectFull && !questionShiftInfo.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        QuestionExamShiftBadgeCard(shiftInfo = questionShiftInfo)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                } else {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                // Options A, B, C, D with soft rounded pill cards and subtle bevel border
                                val optionPrefixes = listOf("A. ", "B. ", "C. ", "D. ")
                                currentQ.options.forEachIndexed { optIndex, optionText ->
                                    val isSelected = currentAnswer == optIndex
                                    val prefix = optionPrefixes.getOrElse(optIndex) { "${('A' + optIndex)}. " }

                                    val isDark = ThemeManager.isDarkMode
                                    val optionBg = when {
                                        isSelected && isDark -> Color(0xFF1E3A8A).copy(alpha = 0.45f)
                                        isSelected -> Color(0xFFD4E7D0)
                                        isDark -> SurfaceCard
                                        else -> Color(0xFFEBEFE7)
                                    }
                                    val optionBorder = when {
                                        isSelected && isDark -> Color(0xFF3B82F6)
                                        isSelected -> Color(0xFF98BC92)
                                        isDark -> CardBorderColor
                                        else -> Color(0xFFD8DFD4)
                                    }
                                    val optionTextColor = when {
                                        isSelected && isDark -> Color(0xFFBFDBFE)
                                        isSelected -> Color(0xFF1C2419)
                                        isDark -> TextPrimary
                                        else -> Color(0xFF1C2419)
                                    }
                                    val checkCircleBg = if (isDark) Color(0xFF2563EB) else Color(0xFF88A882)

                                    Surface(
                                        onClick = {
                                            if (userAnswers[currentIndex] == optIndex) {
                                                userAnswers.remove(currentIndex)
                                            } else {
                                                userAnswers[currentIndex] = optIndex
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = optionBg,
                                        shadowElevation = if (isSelected) 3.dp else 2.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .border(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = optionBorder,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .testTag("option_${currentIndex}_$optIndex")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            OptionContentDisplay(
                                                prefix = prefix,
                                                optionText = optionText,
                                                textColor = optionTextColor,
                                                isBold = isSelected,
                                                modifier = Modifier.weight(1f)
                                            )

                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(checkCircleBg),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                if (isMarked) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = null,
                                            tint = if (ThemeManager.isDarkMode) Color(0xFFFBBF24) else Color(0xFFE08E00),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "समीक्षा के लिए चिह्नित (Marked for Review)",
                                            fontSize = 12.sp,
                                            color = if (ThemeManager.isDarkMode) Color(0xFFFBBF24) else Color(0xFFE08E00),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // 4. Bottom Action Buttons: [ Previous ] [ Mark for Review ] [ Next / Submit ]
                            // Styled with clean responsive buttons that fit comfortably on mobile displays
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous Button
                                QuizBottomButton(
                                    text = "Previous",
                                    enabled = currentIndex > 0,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (currentIndex > 0) currentIndex--
                                    }
                                )

                                // Mark for Review Button
                                QuizBottomButton(
                                    text = "Mark for Review",
                                    enabled = true,
                                    modifier = Modifier.weight(1.3f),
                                    onClick = {
                                        markedForReview[currentIndex] = !isMarked
                                    }
                                )

                                // Next / Submit Button
                                val isLastQuestion = currentIndex == questions.size - 1
                                QuizBottomButton(
                                    text = if (isLastQuestion) "Submit" else "Next",
                                    enabled = true,
                                    isSubmitHighlight = isLastQuestion,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (isLastQuestion) {
                                            showSubmitConfirmDialog = true
                                        } else {
                                            currentIndex++
                                        }
                                    }
                                )
                            }
                        }
                    } else {
                        // Result, Scorecard, Accuracy & Test-identical Solutions View
                        val answeredCount = userAnswers.size
                        var correctCount = 0
                        var incorrectCount = 0
                        questions.forEachIndexed { qIdx, question ->
                            val userAns = userAnswers[qIdx]
                            if (userAns != null) {
                                if (userAns == question.correctIndex) {
                                    correctCount++
                                } else {
                                    incorrectCount++
                                }
                            }
                        }
                        val unattemptedCount = questions.size - answeredCount
                        val accuracyPercent = if (answeredCount > 0) (correctCount * 100) / answeredCount else 0
                        // Correct gets 1 mark each (Delhi Police / SSC standard), with 0.25 negative marking per wrong answer
                        val totalMaxMarks = questions.size * 1
                        val scoreMarks = (correctCount * 1.0) - (incorrectCount * 0.25)
                        val effectiveScore = if (scoreMarks < 0) 0.0 else scoreMarks
                        val formattedScore = String.format("%.2f", effectiveScore)

                        // --- All India Rank (AIR) & Competitive Analytics Engine ---
                        val (initialLeaderboard, initialUserEntry) = com.example.model.TestLeaderboardRepository.getDynamicLeaderboard(
                            context = context,
                            testTitle = testTitle,
                            userScore = effectiveScore,
                            totalMarks = totalMaxMarks.toDouble(),
                            accuracy = accuracyPercent,
                            airRank = 1
                        )
                        val totalRealCandidates = initialLeaderboard.size
                        val airRank = initialUserEntry.rank

                        val scoreRatio = (effectiveScore / totalMaxMarks.coerceAtLeast(1).toDouble()).coerceIn(0.0, 1.0)
                        val percentile = if (totalRealCandidates > 1) {
                            (((totalRealCandidates - airRank).toDouble() / (totalRealCandidates - 1).toDouble()) * 100.0).coerceIn(1.0, 99.9)
                        } else {
                            99.9
                        }
                        val formattedPercentile = String.format("%.1f", percentile)
                        val cutoffBenchmark = com.example.model.TestLeaderboardRepository.computeCutoff(
                            totalMarks = totalMaxMarks.toDouble(),
                            realScores = initialLeaderboard.map { it.score },
                            currentUserScore = effectiveScore
                        )
                        val expectedCutoffUR = cutoffBenchmark.cutoffUR
                        val isCutoffCleared = effectiveScore >= expectedCutoffUR
                        val isBorderline = !isCutoffCleared && effectiveScore >= (expectedCutoffUR - (totalMaxMarks * 0.08))

                        androidx.compose.runtime.LaunchedEffect(effectiveScore) {
                            withContext(Dispatchers.IO) {
                                try {
                                    com.example.model.UnlockedTestsRepository.unlockTest(context, testTitle)
                                    com.example.model.OfflineTestRepository.saveTestOffline(context, testTitle, questions)
                                    com.example.model.WeakAreaAnalyticsRepository.recordTestResults(
                                        context = context,
                                        testTitle = testTitle,
                                        questions = questions,
                                        userAnswers = userAnswers
                                    )
                                    com.example.model.TestHistoryRepository.recordAttempt(
                                        testKey = testTitle,
                                        score = effectiveScore,
                                        totalMarks = totalMaxMarks.toDouble(),
                                        accuracy = accuracyPercent,
                                        percentile = percentile,
                                        context = context
                                    )
                                    com.example.service.AuthManager.recordTestScoreToCloud(
                                        testTitle = testTitle,
                                        score = effectiveScore,
                                        totalMarks = totalMaxMarks.toDouble(),
                                        accuracy = accuracyPercent,
                                        percentile = percentile,
                                        rank = airRank,
                                        totalCandidates = totalRealCandidates
                                    )
                                    com.example.model.TestLeaderboardRepository.recordAndFetchLeaderboard(
                                        context = context,
                                        testTitle = testTitle,
                                        userScore = effectiveScore,
                                        totalMarks = totalMaxMarks.toDouble(),
                                        accuracy = accuracyPercent
                                    )
                                    com.example.model.RealtimeExamStatsManager.recordAspirantActivity(
                                        context = context,
                                        subExamId = testTitle,
                                        subExamName = testTitle
                                    )
                                } catch (_: Throwable) {}
                            }
                        }

                        if (!isReviewMode) {
                            // --- Comprehensive Clean Scoreboard Screen with All India Rank ---
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val isDark = ThemeManager.isDarkMode
                                // Timer Expired Auto-Submit Banner
                                if (showAutoSubmitBanner) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEF2F2),
                                        border = BorderStroke(1.dp, if (isDark) Color(0xFFDC2626) else Color(0xFFFECACA))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = "समय समाप्त! टेस्ट ऑटो-सबमिट हो गया",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                                )
                                                Text(
                                                    text = "निर्धारित समय पूरा होने पर आपका टेस्ट सुरक्षित रूप से सबमिट कर दिया गया है।",
                                                    fontSize = 11.5.sp,
                                                    color = if (isDark) Color(0xFFFECACA) else Color(0xFFB91C1C)
                                                )
                                            }
                                        }
                                    }
                                }
                                // 1. ALL INDIA RANK TOP COMPACT CARD (SurfaceCard Background with Highlighted Border, Click-to-Expand)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = SurfaceCard,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                                    shadowElevation = 1.5.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    ) {
                                        // Clickable Header Area: Always visible compact card
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showAllIndiaDetails = !showAllIndiaDetails }
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(38.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFEF3C7))
                                                            .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.EmojiEvents,
                                                            contentDescription = "All India Rank",
                                                            tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                    Column {
                                                        Text(
                                                            text = "ALL INDIA RANK",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextSecondary,
                                                            letterSpacing = 0.5.sp
                                                        )
                                                        Row(
                                                            verticalAlignment = Alignment.Bottom,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Text(
                                                                text = "#AIR $airRank",
                                                                fontSize = 20.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                                                            )
                                                            Text(
                                                                text = if (totalRealCandidates > 1) "/ $totalRealCandidates परीक्षार्थी" else "/ 1st Rank (लाइव)",
                                                                fontSize = 11.5.sp,
                                                                color = TextSecondary,
                                                                modifier = Modifier.padding(bottom = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Divider line
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(0.8.dp)
                                                    .background(CardBorderColor)
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Cutoff Status Banner & Target Info in a clean compact single row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = when {
                                                        isCutoffCleared -> if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFDCFCE7)
                                                        isBorderline -> if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFEF3C7)
                                                        else -> if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.5f) else Color(0xFFFEE2E2)
                                                    },
                                                    border = androidx.compose.foundation.BorderStroke(
                                                        1.dp,
                                                        when {
                                                            isCutoffCleared -> if (isDark) Color(0xFF059669) else Color(0xFF86EFAC)
                                                            isBorderline -> if (isDark) Color(0xFFD97706) else Color(0xFFFDE68A)
                                                            else -> if (isDark) Color(0xFFDC2626) else Color(0xFFFECACA)
                                                        }
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = when {
                                                                isCutoffCleared -> Icons.Default.CheckCircle
                                                                isBorderline -> Icons.AutoMirrored.Filled.TrendingUp
                                                                else -> Icons.Default.HighlightOff
                                                            },
                                                            contentDescription = null,
                                                            tint = when {
                                                                isCutoffCleared -> if (isDark) Color(0xFF34D399) else Color(0xFF16A34A)
                                                                isBorderline -> if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                                                                else -> if (isDark) Color(0xFFF87171) else Color(0xFFDC2626)
                                                            },
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Text(
                                                            text = when {
                                                                isCutoffCleared -> "कट-ऑफ क्लियर"
                                                                isBorderline -> "बॉर्डर लाइन"
                                                                else -> "सुधार अपेक्षित"
                                                            },
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = when {
                                                                isCutoffCleared -> if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                                                                isBorderline -> if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
                                                                else -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                                                            }
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = "Expected UR Cut-off: ${cutoffBenchmark.formattedCutoffUR} M",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextSecondary
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Clickable Strip: Tap to View Topper & Cutoff details
                                            Surface(
                                                shape = RoundedCornerShape(7.dp),
                                                color = SurfaceCardSecondary,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Leaderboard,
                                                            contentDescription = null,
                                                            tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6),
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                        Text(
                                                            text = if (showAllIndiaDetails) "कट-ऑफ, टॉपर्स लिस्ट छुपाएं ▲" else "🎯 कट-ऑफ व टॉपर्स लिस्ट देखें ▼",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimary
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = if (showAllIndiaDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        tint = TextPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // EXPANDABLE CONTENT: Shown only after clicking
                                        AnimatedVisibility(
                                            visible = showAllIndiaDetails,
                                            enter = fadeIn(),
                                            exit = fadeOut()
                                        ) {
                                            val (leaderboardEntries, currentUserLeaderboardEntry) = com.example.model.TestLeaderboardRepository.getDynamicLeaderboard(
                                                context = context,
                                                testTitle = testTitle,
                                                userScore = effectiveScore,
                                                totalMarks = totalMaxMarks.toDouble(),
                                                accuracy = accuracyPercent,
                                                airRank = airRank
                                            )
                                            val topRankers = leaderboardEntries.take(5)
                                            val topperScoreDisplay = String.format("%.2f", leaderboardEntries.firstOrNull()?.score ?: effectiveScore)

                                            Column(
                                                modifier = Modifier.padding(top = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                // 🎯 1. COMPARATIVE BENCHMARK STRIP (You vs Cut-off vs Average vs Topper)
                                                Surface(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = SurfaceCardSecondary,
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text(
                                                                text = "आपका स्कोर",
                                                                fontSize = 11.sp,
                                                                color = TextSecondary
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "$formattedScore M",
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = TextPrimary
                                                            )
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .width(1.dp)
                                                                .height(26.dp)
                                                                .background(CardBorderColor)
                                                        )
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text(
                                                                text = "कट-ऑफ (UR)",
                                                                fontSize = 11.sp,
                                                                color = TextSecondary
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "${cutoffBenchmark.formattedCutoffUR} M",
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = TextPrimary
                                                            )
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .width(1.dp)
                                                                .height(26.dp)
                                                                .background(CardBorderColor)
                                                        )
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text(
                                                                text = "औसत स्कोर",
                                                                fontSize = 11.sp,
                                                                color = TextSecondary
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "${cutoffBenchmark.formattedAverageScore} M",
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = TextPrimary
                                                            )
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .width(1.dp)
                                                                .height(26.dp)
                                                                .background(CardBorderColor)
                                                        )
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text(
                                                                text = "टॉपर स्कोर",
                                                                fontSize = 11.sp,
                                                                color = TextSecondary
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = "$topperScoreDisplay M",
                                                                fontSize = 15.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                                                            )
                                                        }
                                                    }
                                                }

                                                // 🥇 2. ALL INDIA LEADERBOARD (Top 5 Rankers & You)
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.weight(1f, fill = false),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Leaderboard,
                                                                contentDescription = "Leaderboard",
                                                                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                            Text(
                                                                text = "All India Leaderboard",
                                                                fontSize = 13.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = TextPrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF),
                                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2563EB) else Color(0xFFDBEAFE))
                                                        ) {
                                                            Text(
                                                                text = if (leaderboardEntries.size <= 1) "1 लाइव अभ्यर्थी" else "${leaderboardEntries.size} लाइव अभ्यर्थी",
                                                                fontSize = 10.5.sp,
                                                                color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                                                fontWeight = FontWeight.SemiBold,
                                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        }
                                                    }

                                                    // Table Header Row
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(SurfaceCardSecondary, RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "रैंक",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextSecondary,
                                                            modifier = Modifier.width(42.dp),
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                        Text(
                                                            text = "अभ्यर्थी व स्थिति",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextSecondary,
                                                            modifier = Modifier.weight(1f),
                                                            maxLines = 1
                                                        )
                                                        Text(
                                                            text = "मार्क्स",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextSecondary,
                                                            modifier = Modifier.width(58.dp),
                                                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                        Text(
                                                            text = "सटीकता",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextSecondary,
                                                            modifier = Modifier.width(46.dp),
                                                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }

                                                    // Top Candidates List
                                                    topRankers.forEachIndexed { idx, entry ->
                                                        val rankNum = entry.rank
                                                        val isUserInThisSpot = entry.isCurrentUser

                                                        Surface(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = if (isUserInThisSpot) {
                                                                if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFF0F9FF)
                                                            } else Color.Transparent
                                                         ) {
                                                             Row(
                                                                 modifier = Modifier
                                                                     .fillMaxWidth()
                                                                     .padding(horizontal = 8.dp, vertical = 5.dp),
                                                                 verticalAlignment = Alignment.CenterVertically
                                                             ) {
                                                                 Text(
                                                                     text = when (rankNum) {
                                                                         1 -> "🥇 1"
                                                                         2 -> "🥈 2"
                                                                         3 -> "🥉 3"
                                                                         else -> "#$rankNum"
                                                                     },
                                                                     fontSize = 12.sp,
                                                                     fontWeight = FontWeight.Bold,
                                                                     color = if (isUserInThisSpot) Color(0xFF60A5FA) else TextPrimary,
                                                                     modifier = Modifier.width(42.dp),
                                                                     maxLines = 1,
                                                                     softWrap = false
                                                                 )
                                                                 Column(modifier = Modifier.weight(1f)) {
                                                                     Text(
                                                                         text = if (isUserInThisSpot) "⭐ ${entry.userName}" else entry.userName,
                                                                         fontSize = 12.sp,
                                                                         fontWeight = if (isUserInThisSpot) FontWeight.Bold else FontWeight.Medium,
                                                                         color = if (isUserInThisSpot) Color(0xFF93C5FD) else TextPrimary,
                                                                         maxLines = 1,
                                                                         overflow = TextOverflow.Ellipsis
                                                                     )
                                                                     Text(
                                                                         text = entry.userLocation,
                                                                         fontSize = 10.sp,
                                                                         color = TextSecondary,
                                                                         maxLines = 1,
                                                                         overflow = TextOverflow.Ellipsis
                                                                     )
                                                                 }
                                                                 Text(
                                                                     text = "${String.format("%.2f", entry.score)} M",
                                                                     fontSize = 11.5.sp,
                                                                     fontWeight = FontWeight.Bold,
                                                                     color = if (isUserInThisSpot) Color(0xFF60A5FA) else TextPrimary,
                                                                     modifier = Modifier.width(58.dp),
                                                                     textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                                                     maxLines = 1,
                                                                     softWrap = false
                                                                 )
                                                                 Text(
                                                                     text = "${entry.accuracy}%",
                                                                     fontSize = 11.5.sp,
                                                                     fontWeight = FontWeight.Medium,
                                                                     color = if (isUserInThisSpot) Color(0xFF60A5FA) else TextSecondary,
                                                                     modifier = Modifier.width(46.dp),
                                                                     textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                                                     maxLines = 1,
                                                                     softWrap = false
                                                                 )
                                                             }
                                                         }
                                                         if (idx < topRankers.size - 1) {
                                                             Box(
                                                                 modifier = Modifier
                                                                     .fillMaxWidth()
                                                                     .height(0.5.dp)
                                                                     .background(CardBorderColor)
                                                             )
                                                         }
                                                     }

                                                     if (leaderboardEntries.size <= 1) {
                                                         Surface(
                                                             modifier = Modifier.fillMaxWidth(),
                                                             shape = RoundedCornerShape(6.dp),
                                                             color = SurfaceCardSecondary,
                                                             border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                                                         ) {
                                                             Text(
                                                                 text = "✨ इस टेस्ट में आप वर्तमान में शीर्ष रैंक पर हैं। अन्य अभ्यर्थियों के टेस्ट सबमिट करने पर ऑल इंडिया लाइव रैंकिंग स्वतः अपडेट होती रहेगी।",
                                                                 fontSize = 11.sp,
                                                                 color = TextSecondary,
                                                                 modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                             )
                                                         }
                                                     }

                                                     // Dedicated Aspirant Highlight Card (if user is beyond Top 5)
                                                     if (currentUserLeaderboardEntry.rank > 5) {
                                                         Box(
                                                             modifier = Modifier
                                                                 .fillMaxWidth()
                                                                 .padding(vertical = 1.dp),
                                                             contentAlignment = Alignment.Center
                                                         ) {
                                                             Text(
                                                                 text = "•  •  •",
                                                                 fontSize = 11.sp,
                                                                 fontWeight = FontWeight.Bold,
                                                                 color = TextSecondary
                                                             )
                                                         }

                                                         Surface(
                                                             modifier = Modifier.fillMaxWidth(),
                                                             shape = RoundedCornerShape(6.dp),
                                                             color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFF0F9FF),
                                                             border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF2563EB) else Color(0xFFBAE6FD))
                                                         ) {
                                                             Row(
                                                                 modifier = Modifier
                                                                     .fillMaxWidth()
                                                                     .padding(horizontal = 8.dp, vertical = 5.dp),
                                                                 verticalAlignment = Alignment.CenterVertically
                                                             ) {
                                                                 Text(
                                                                     text = "#AIR ${currentUserLeaderboardEntry.rank}",
                                                                     fontSize = 11.5.sp,
                                                                     fontWeight = FontWeight.ExtraBold,
                                                                     color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                                                     modifier = Modifier.width(42.dp),
                                                                     maxLines = 1,
                                                                     softWrap = false
                                                                 )
                                                                 Column(modifier = Modifier.weight(1f)) {
                                                                     Text(
                                                                         text = "⭐ ${currentUserLeaderboardEntry.userName}",
                                                                         fontSize = 12.sp,
                                                                         fontWeight = FontWeight.Bold,
                                                                         color = TextPrimary,
                                                                         maxLines = 1,
                                                                         overflow = TextOverflow.Ellipsis
                                                                     )
                                                                     Text(
                                                                         text = "All India Rank",
                                                                         fontSize = 10.sp,
                                                                         color = TextSecondary,
                                                                         maxLines = 1,
                                                                         overflow = TextOverflow.Ellipsis
                                                                     )
                                                                 }
                                                                 Text(
                                                                     text = "${String.format("%.2f", currentUserLeaderboardEntry.score)} M",
                                                                     fontSize = 11.5.sp,
                                                                     fontWeight = FontWeight.Bold,
                                                                     color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                                                     modifier = Modifier.width(58.dp),
                                                                     textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                                                     maxLines = 1,
                                                                     softWrap = false
                                                                 )
                                                                 Text(
                                                                     text = "${currentUserLeaderboardEntry.accuracy}%",
                                                                     fontSize = 11.5.sp,
                                                                     fontWeight = FontWeight.Bold,
                                                                     color = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                                                     modifier = Modifier.width(46.dp),
                                                                     textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                                                     maxLines = 1,
                                                                     softWrap = false
                                                                 )
                                                             }
                                                         }
                                                     }
                                                }

                                                // 📊 3. Sectional Breakdown (अनुभाग-वार अंक: परीक्षा के सभी आधिकारिक विषय)
                                                if (examSections.isNotEmpty()) {
                                                    val sections = examSections.map { it.name to it.range }

                                                    // Divider line
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(1.dp)
                                                            .background(CardBorderColor)
                                                    )

                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "📊 Sectional Score (विषय-वार अंक)",
                                                                fontSize = 13.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = TextPrimary
                                                            )
                                                            Text(
                                                                text = "${sections.size} विषय",
                                                                fontSize = 11.sp,
                                                                color = TextSecondary
                                                            )
                                                        }

                                                        sections.forEachIndexed { sIdx, (secTitle, secRange) ->
                                                            var sAttempted = 0
                                                            var sCorrect = 0
                                                            var sWrong = 0
                                                            secRange.forEach { qIdx ->
                                                                val ans = userAnswers[qIdx]
                                                                if (ans != null) {
                                                                    sAttempted++
                                                                    if (ans == questions[qIdx].correctIndex) sCorrect++ else sWrong++
                                                                }
                                                            }
                                                            val sScore = (sCorrect * 1.0) - (sWrong * 0.25)
                                                            val sScoreFormatted = String.format("%.2f", if (sScore < 0) 0.0 else sScore)

                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(vertical = 4.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column {
                                                                    Text(
                                                                        text = secTitle,
                                                                        fontSize = 12.sp,
                                                                        fontWeight = FontWeight.Medium,
                                                                        color = TextPrimary
                                                                    )
                                                                    Text(
                                                                        text = "प्रयास: $sAttempted/${secRange.count()} | सही: $sCorrect",
                                                                        fontSize = 10.5.sp,
                                                                        color = TextSecondary
                                                                    )
                                                                }
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = SurfaceCardSecondary,
                                                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, CardBorderColor)
                                                                ) {
                                                                    Text(
                                                                        text = "$sScoreFormatted Marks",
                                                                        fontSize = 11.5.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = TextPrimary,
                                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                                    )
                                                                }
                                                            }

                                                            if (sIdx < sections.size - 1) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .height(0.5.dp)
                                                                        .background(CardBorderColor)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                // 2. 4 CORE METRIC CARDS (Correct, Wrong, Unattempted, Accuracy)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PerformanceMetricCard(
                                        title = "Correct (सही)",
                                        count = "$correctCount",
                                        subtitle = "+${correctCount * 1} Marks",
                                        accentColor = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A),
                                        bgColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFF0FDF4),
                                        borderColor = if (isDark) Color(0xFF059669) else Color(0xFFBBF7D0),
                                        modifier = Modifier.weight(1f)
                                    )
                                    PerformanceMetricCard(
                                        title = "Wrong (गलत)",
                                        count = "$incorrectCount",
                                        subtitle = "-${String.format("%.2f", incorrectCount * 0.25)} Marks",
                                        accentColor = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                        bgColor = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEF2F2),
                                        borderColor = if (isDark) Color(0xFFDC2626) else Color(0xFFFECACA),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    PerformanceMetricCard(
                                        title = "Unattempted (छोड़ा)",
                                        count = "$unattemptedCount",
                                        subtitle = "Questions",
                                        accentColor = TextSecondary,
                                        bgColor = SurfaceCardSecondary,
                                        borderColor = CardBorderColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PerformanceMetricCard(
                                        title = "Accuracy (सटीकता)",
                                        count = "$accuracyPercent%",
                                        subtitle = if (accuracyPercent >= 80) "उत्कृष्ट (Very Good)" else if (accuracyPercent >= 60) "संतोषजनक (Good)" else "सुधार योग्य",
                                        accentColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF0284C7),
                                        bgColor = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFF0F9FF),
                                        borderColor = if (isDark) Color(0xFF2563EB) else Color(0xFFBAE6FD),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                // 6. Test Overview Breakdown Card
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = SurfaceCard,
                                    border = BorderStroke(1.dp, CardBorderColor),
                                    shadowElevation = 1.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Test Summary Breakdown",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )

                                        // Progress Breakdown Bar
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                                        ) {
                                            val total = questions.size.coerceAtLeast(1).toFloat()
                                            if (correctCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .weight(correctCount / total)
                                                        .background(Color(0xFF22C55E))
                                                )
                                            }
                                            if (incorrectCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .weight(incorrectCount / total)
                                                        .background(Color(0xFFEF4444))
                                                )
                                            }
                                            if (unattemptedCount > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .weight(unattemptedCount / total)
                                                        .background(if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "कुल प्रश्न (Total Questions)",
                                                fontSize = 12.5.sp,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = "${questions.size}",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "प्रयास किए गए (Attempted)",
                                                fontSize = 12.5.sp,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = "$answeredCount",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF0284C7)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // --- "View Solutions" Main Button ---
                                Surface(
                                    onClick = {
                                        isReviewMode = true
                                        reviewQuestionIndex = 0
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0284C7), // Bright action blue
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("view_solutions_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "View Solutions (उत्तर और व्याख्या देखें)",
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Secondary Action Buttons: Re-Attempt & Close
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    QuizBottomButton(
                                        text = "पुनः प्रयास करें (Re-Attempt)",
                                        enabled = true,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            userAnswers.clear()
                                            markedForReview.clear()
                                            visitedQuestions.clear()
                                            currentIndex = 0
                                            isFinished = false
                                            timeLeftSeconds = initialDurationMinutes * 60
                                            visitedQuestions[0] = true
                                            showAllIndiaDetails = false
                                        }
                                    )
                                    QuizBottomButton(
                                        text = "बंद करें (Close)",
                                        enabled = true,
                                        modifier = Modifier.weight(1f),
                                        onClick = onDismiss
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        } else {
                            // --- Test-Identical Solution View with 4 Subjects & 20 Questions each ---
                            val safeReviewIdx = if (reviewQuestionIndex in questions.indices) reviewQuestionIndex else 0
                            val reviewQ = questions[safeReviewIdx]
                            val userChoice = userAnswers[safeReviewIdx]
                            val isCorrect = userChoice == reviewQ.correctIndex
                            val isAttempted = userChoice != null

                            // Calculate subject-relative question number based on exam section
                            val reviewActiveSubjectIndex = remember(reviewQuestionIndex, examSections) {
                                val idx = examSections.indexOfFirst { reviewQuestionIndex in it.range }
                                if (idx >= 0) idx else 0
                            }
                            val reviewSection = examSections.getOrNull(reviewActiveSubjectIndex)
                            val reviewSubjectQuestionNumber = if (isMultiSubjectFull) {
                                reviewSection?.let { (reviewQuestionIndex - it.range.first) + 1 } ?: (reviewQuestionIndex + 1)
                            } else {
                                reviewQuestionIndex + 1
                            }
                            val reviewSubjectTotalQuestions = if (isMultiSubjectFull) {
                                reviewSection?.questionCount ?: questions.size
                            } else {
                                questions.size
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .navigationBarsPadding()
                                    .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 20.dp)
                            ) {
                                // Top Info Bar: Responsive Single Row with Question Pill & Back to Scorecard Button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Question Number Pill (Displays Q 1/20 for each subject)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(QuizPillBg)
                                            .padding(horizontal = 7.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Q $reviewSubjectQuestionNumber/$reviewSubjectTotalQuestions",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = QuizPillText
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Marked for Review Pill if marked during test
                                        if (markedForReview[reviewQuestionIndex] == true) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFF3E8FF))
                                                    .border(1.dp, Color(0xFFD8B4FE), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 5.dp, vertical = 4.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Bookmark,
                                                        contentDescription = null,
                                                        tint = Color(0xFF9333EA),
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Text(
                                                        text = "Marked",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF9333EA)
                                                    )
                                                }
                                            }
                                        }

                                        // Status Pill (Correct / Wrong / Left)
                                        val isDark = ThemeManager.isDarkMode
                                        val (statusLabel, statusBg, statusColor) = when {
                                            !isAttempted -> Triple("छोड़ा", SurfaceCardSecondary, TextSecondary)
                                            isCorrect -> Triple(
                                                "सही (+1)",
                                                if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFDCFCE7),
                                                if (isDark) Color(0xFF34D399) else Color(0xFF15803D)
                                            )
                                            else -> Triple(
                                                "गलत (-0.25)",
                                                if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.5f) else Color(0xFFFEE2E2),
                                                if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(statusBg)
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = statusLabel,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor
                                            )
                                        }

                                        // Solution Grid button to jump directly to any question
                                        Surface(
                                            onClick = { showQuestionPalette = true },
                                            shape = RoundedCornerShape(6.dp),
                                            color = QuizPillBg,
                                            shadowElevation = 0.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Apps,
                                                    contentDescription = "Question Grid",
                                                    tint = QuizPillText,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Grid",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = QuizPillText
                                                )
                                            }
                                        }

                                        // Back to Scorecard button (Compact & Highlighted)
                                        Surface(
                                            onClick = { isReviewMode = false },
                                            shape = RoundedCornerShape(6.dp),
                                            color = QuizPillBg,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                                            shadowElevation = 0.dp
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back to Scorecard",
                                                    tint = QuizPillText,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = "Scorecard",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = QuizPillText
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Official Subjects Selector Tabs - Shown ONLY in Multi-Subject Full Tests (hidden in Chapter-Wise and Subject-Wise)
                                if (isMultiSubjectFull && examSections.size > 1) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("solution_subject_tabs"),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        examSections.forEachIndexed { subIndex, section ->
                                            val isSelected = reviewActiveSubjectIndex == subIndex
                                            Surface(
                                                onClick = {
                                                    val targetIndex = section.range.first
                                                    if (targetIndex < questions.size) {
                                                        reviewQuestionIndex = targetIndex
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isSelected) QuizPillBg else SurfaceCardSecondary,
                                                shadowElevation = if (isSelected) 2.dp else 0.dp,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(34.dp)
                                                    .testTag("solution_subject_tab_$subIndex")
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = section.name,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (isSelected) QuizPillText else TextSecondary,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                } else {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }

                                // Scrollable Question, Options and Detailed Hindi Solution
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    // User Performance Result Card for this Question
                                    val isReviewDark = ThemeManager.isDarkMode
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = when {
                                            !isAttempted -> SurfaceCardSecondary
                                            isCorrect -> if (isReviewDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFF0FDF4)
                                            else -> if (isReviewDark) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEF2F2)
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.2.dp,
                                            when {
                                                !isAttempted -> CardBorderColor
                                                isCorrect -> if (isReviewDark) Color(0xFF059669) else Color(0xFF86EFAC)
                                                else -> if (isReviewDark) Color(0xFFDC2626) else Color(0xFFFCA5A5)
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = when {
                                                    !isAttempted -> Icons.AutoMirrored.Filled.HelpOutline
                                                    isCorrect -> Icons.Default.CheckCircle
                                                    else -> Icons.Default.Cancel
                                                },
                                                contentDescription = null,
                                                tint = when {
                                                    !isAttempted -> TextSecondary
                                                    isCorrect -> if (isReviewDark) Color(0xFF34D399) else Color(0xFF16A34A)
                                                    else -> if (isReviewDark) Color(0xFFF87171) else Color(0xFFDC2626)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = when {
                                                        !isAttempted -> "आपने यह प्रश्न छोड़ दिया (Unattempted)"
                                                        isCorrect -> "आपका उत्तर सही था! (+1 Mark)"
                                                        else -> "आपका उत्तर गलत था! (-0.25 Marks)"
                                                    },
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        !isAttempted -> TextPrimary
                                                        isCorrect -> if (isReviewDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                                                        else -> if (isReviewDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                                                    }
                                                )
                                                Text(
                                                    text = when {
                                                        !isAttempted -> "सही उत्तर: विकल्प ${'A' + reviewQ.correctIndex}"
                                                        isCorrect -> "आपने विकल्प ${'A' + userChoice} चुना था (सही उत्तर)"
                                                        else -> "आपने विकल्प ${'A' + userChoice} चुना था (गलत) • सही उत्तर: विकल्प ${'A' + reviewQ.correctIndex}"
                                                    },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = when {
                                                        !isAttempted -> TextSecondary
                                                        isCorrect -> if (isReviewDark) Color(0xFF86EFAC) else Color(0xFF16A34A)
                                                        else -> if (isReviewDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                                                    }
                                                )
                                            }
                                            if (markedForReview[reviewQuestionIndex] == true) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (isReviewDark) Color(0xFF581C87).copy(alpha = 0.5f) else Color(0xFFF3E8FF))
                                                        .border(1.dp, if (isReviewDark) Color(0xFF9333EA) else Color(0xFFD8B4FE), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Bookmark,
                                                            contentDescription = null,
                                                            tint = if (isReviewDark) Color(0xFFC084FC) else Color(0xFF9333EA),
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Text(
                                                            text = "Marked",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isReviewDark) Color(0xFFC084FC) else Color(0xFF9333EA)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Question Text in Hindi
                                    Text(
                                        text = parseSimpleMarkdown("Q $reviewSubjectQuestionNumber: ${reviewQ.question}"),
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = QuizQuestionTextColor,
                                        lineHeight = 25.sp
                                    )

                                    QuestionImageDisplay(imageUrl = reviewQ.imageUrl)

                                    // Exam, Date & Shift metadata card placed between Question and Options
                                    val reviewShiftInfo = remember(reviewQ, testTitle, isMultiSubjectFull) {
                                        getQuestionExamShiftBadge(reviewQ, testTitle, isMultiSubjectFull)
                                    }

                                    if (!isMultiSubjectFull && !reviewShiftInfo.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            QuestionExamShiftBadgeCard(shiftInfo = reviewShiftInfo)
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                    } else {
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }

                                    // Options A, B, C, D with clear Right / Wrong highlight and badges
                                    val optionPrefixes = listOf("A. ", "B. ", "C. ", "D. ")
                                    reviewQ.options.forEachIndexed { optIndex, optionText ->
                                        val prefix = optionPrefixes.getOrElse(optIndex) { "${('A' + optIndex)}. " }
                                        val isOptionCorrect = optIndex == reviewQ.correctIndex
                                        val isOptionChosen = userChoice == optIndex
                                        val isWrongChoice = isOptionChosen && !isOptionCorrect

                                        // Coloring: Sahi option is green; user ka galat option is red; baaki neutral
                                        val (cardBg, borderStroke, textColor) = when {
                                            isWrongChoice -> Triple(
                                                if (isReviewDark) Color(0xFF7F1D1D).copy(alpha = 0.35f) else Color(0xFFFEE2E2), // Soft red
                                                androidx.compose.foundation.BorderStroke(2.dp, if (isReviewDark) Color(0xFFDC2626) else Color(0xFFEF4444)),
                                                if (isReviewDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                            )
                                            isOptionCorrect -> Triple(
                                                if (isReviewDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFE8F8EE), // Soft green
                                                androidx.compose.foundation.BorderStroke(2.dp, if (isReviewDark) Color(0xFF059669) else Color(0xFF22C55E)),
                                                if (isReviewDark) Color(0xFF86EFAC) else Color(0xFF14532D)
                                            )
                                            else -> Triple(
                                                SurfaceCard,
                                                androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                                                TextPrimary
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = cardBg,
                                            border = borderStroke,
                                            shadowElevation = if (isWrongChoice || isOptionCorrect) 1.dp else 0.5.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.5.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OptionContentDisplay(
                                                    prefix = prefix,
                                                    optionText = optionText,
                                                    textColor = textColor,
                                                    isBold = (isOptionCorrect || isWrongChoice),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                // Clear badges for options: User's wrong choice vs correct answer
                                                when {
                                                    isWrongChoice -> {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color(0xFFDC2626))
                                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Cancel,
                                                                contentDescription = "Your Selected Wrong Answer",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Text(
                                                                text = "गलत (आपका उत्तर)",
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                    isOptionCorrect -> {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(Color(0xFF16A34A))
                                                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.CheckCircle,
                                                                contentDescription = "Correct Answer",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Text(
                                                                text = if (isOptionChosen) "सही (आपका उत्तर)" else "सही उत्तर",
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color.White
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    // Detailed Explanation Card (व्याख्या)
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isReviewDark) Color(0xFF064E3B).copy(alpha = 0.25f) else Color(0xFFF0FDF4),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isReviewDark) Color(0xFF059669) else Color(0xFFBBF7D0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoStories,
                                                    contentDescription = null,
                                                    tint = if (isReviewDark) Color(0xFF34D399) else Color(0xFF16A34A),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = "व्याख्या (Detailed Solution)",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isReviewDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = reviewQ.explanation.ifEmpty { "इस प्रश्न का सही उत्तर विकल्प (${('A' + reviewQ.correctIndex)}) है।" },
                                                fontSize = 13.5.sp,
                                                color = if (isReviewDark) Color(0xFFE2E8F0) else Color(0xFF1F2937),
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Bottom Navigation Buttons matching test UI with Save Question bookmark in the center
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 28.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    QuizBottomButton(
                                        text = "Previous",
                                        enabled = reviewQuestionIndex > 0,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            if (reviewQuestionIndex > 0) reviewQuestionIndex--
                                        }
                                    )

                                    val isCurrentQuesSaved = SavedQuestionsRepository.isSaved(reviewQ.question)
                                    val isSaveDark = ThemeManager.isDarkMode
                                    val saveBtnBg = when {
                                        isCurrentQuesSaved && isSaveDark -> Color(0xFF78350F).copy(alpha = 0.5f)
                                        isCurrentQuesSaved -> Color(0xFFFEF3C7)
                                        isSaveDark -> SurfaceCardSecondary
                                        else -> Color(0xFFF1F5F9)
                                    }
                                    val saveBtnBorder = when {
                                        isCurrentQuesSaved -> Color(0xFFF59E0B)
                                        isSaveDark -> CardBorderColor
                                        else -> Color(0xFFCBD5E1)
                                    }
                                    val saveBtnTint = when {
                                        isCurrentQuesSaved -> Color(0xFFF59E0B)
                                        isSaveDark -> TextSecondary
                                        else -> Color(0xFF475569)
                                    }
                                    val saveBtnText = when {
                                        isCurrentQuesSaved && isSaveDark -> Color(0xFFFDE68A)
                                        isCurrentQuesSaved -> Color(0xFFB45309)
                                        isSaveDark -> TextPrimary
                                        else -> Color(0xFF334155)
                                    }

                                    Surface(
                                        onClick = {
                                            if (isCurrentQuesSaved) {
                                                SavedQuestionsRepository.removeQuestion(context, reviewQ.question, scope)
                                                Toast.makeText(context, "प्रश्न Saved list से हटा दिया गया", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val subjectToSave = when {
                                                    reviewQ.subject.isNotBlank() -> reviewQ.subject
                                                    questions.size == 100 -> {
                                                        if (isDelhiPolice) {
                                                            when (reviewQuestionIndex) {
                                                                in 0..49 -> "GK / GS"
                                                                in 50..74 -> "तर्कशक्ति (Reasoning)"
                                                                in 75..89 -> "संख्यात्मक योग्यता (Maths)"
                                                                else -> "कंप्यूटर ज्ञान"
                                                            }
                                                        } else {
                                                            when (reviewQuestionIndex) {
                                                                in 0..24 -> "GK / GS"
                                                                in 25..49 -> "तर्कशक्ति (Reasoning)"
                                                                in 50..74 -> "संख्यात्मक योग्यता (Maths)"
                                                                else -> "English (अंग्रेजी)"
                                                            }
                                                        }
                                                    }
                                                    else -> "General"
                                                }
                                                SavedQuestionsRepository.saveQuestion(
                                                    context = context,
                                                    question = reviewQ,
                                                    subject = subjectToSave,
                                                    examTitle = testTitle,
                                                    scope = scope
                                                )
                                                Toast.makeText(context, "✅ प्रश्न 'Saved Questions' में सुरक्षित हो गया!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = saveBtnBg,
                                        border = BorderStroke(1.5.dp, saveBtnBorder),
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .height(44.dp)
                                            .testTag("save_question_review_btn")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isCurrentQuesSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = "Save Question",
                                                tint = saveBtnTint,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isCurrentQuesSaved) "Saved ✓" else "Save Question",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = saveBtnText
                                            )
                                        }
                                    }

                                    QuizBottomButton(
                                        text = "Next",
                                        enabled = reviewQuestionIndex < questions.size - 1,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            if (reviewQuestionIndex < questions.size - 1) reviewQuestionIndex++
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

// Helper data holder for quadruple values
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Metric Card for Scoreboard: Correct, Incorrect, Unattempted, Accuracy
 */
@Composable
private fun PerformanceMetricCard(
    title: String,
    count: String,
    subtitle: String,
    accentColor: Color,
    bgColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = accentColor.copy(alpha = 0.85f)
            )
            Text(
                text = count,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                color = TextSecondary
            )
        }
    }
}
/**
 * Bottom Button Component reproducing the pastel soft light blue pill buttons:
 * - "Previous"
 * - "Mark for Review"
 * - "Next"
 * from the user's uploaded reference screenshot.
 */
@Composable
fun QuizBottomButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isSubmitHighlight: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDark = ThemeManager.isDarkMode

    // Vibrant green for Submit button, soft pastel light-blue #9EC7E7 for navigation
    val bgColor = if (enabled) {
        if (isSubmitHighlight) {
            if (isPressed) Color(0xFF15803D) else Color(0xFF16A34A)
        } else {
            if (isDark) {
                if (isPressed) Color(0xFF1E3A8A) else Color(0xFF1E293B)
            } else {
                if (isPressed) Color(0xFF86B4D8) else Color(0xFF9EC7E7)
            }
        }
    } else {
        if (isDark) Color(0xFF0F172A) else Color(0xFFD4E3EF)
    }

    val textColor = if (enabled) {
        if (isSubmitHighlight) Color.White
        else if (isDark) Color(0xFFE2E8F0)
        else Color.White
    } else {
        if (isDark) Color(0xFF475569) else Color(0xFF8A9EAF)
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = if (isDark && !isSubmitHighlight) BorderStroke(1.dp, CardBorderColor) else null,
        shadowElevation = if (enabled && !isPressed) 1.dp else 0.dp,
        modifier = modifier.height(40.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isSubmitHighlight) FontWeight.Bold else FontWeight.SemiBold,
                color = textColor,
                letterSpacing = 0.2.sp
            )
        }
    }
}


/**
 * Standard Exam Question Palette (Navigation Panel / Question Grid):
 * - Direct Jump: Kisi bhi number par click karke sidha us question par switch karna.
 * - Color Coding Status:
 *   1. Green: Answered (Jawab de diya)
 *   2. Red: Not Answered (Dekha par jawab nahi diya)
 *   3. Grey/White: Not Visited (Abhi tak dekha hi nahi)
 *   4. Purple: Mark for Review (Baad me dubara check karne ke liye mark kiya)
 */
@Composable
fun QuestionPaletteDialog(
    questions: List<QuizQuestion>,
    currentIndex: Int,
    userAnswers: Map<Int, Int>,
    markedForReview: Map<Int, Boolean>,
    visitedQuestions: Map<Int, Boolean>,
    isReviewMode: Boolean = false,
    testTitle: String = "",
    onDismiss: () -> Unit,
    onSelectQuestion: (Int) -> Unit
) {
    val examSections = remember(testTitle, questions) {
        com.example.model.ExamSubjectManager.getExamSections(testTitle, questions)
    }
    val paletteSubjects = remember(examSections) {
        listOf("All") + examSections.map { it.name }
    }
    var selectedSubjectTab by remember {
        mutableIntStateOf(
            if (examSections.isNotEmpty()) {
                val sIdx = examSections.indexOfFirst { currentIndex in it.range }
                if (sIdx >= 0) sIdx + 1 else 0
            } else 0
        )
    }

    // Question stats calculation
    var answeredCount = 0
    var notAnsweredCount = 0
    var markedCount = 0
    var notVisitedCount = 0
    var correctCount = 0
    var wrongCount = 0
    var unattemptedCount = 0

    questions.indices.forEach { idx ->
        val isMarked = markedForReview[idx] == true
        val isAnswered = userAnswers.containsKey(idx)
        val isVisited = visitedQuestions[idx] == true
        val userAns = userAnswers[idx]

        if (isMarked) markedCount++
        if (isAnswered) answeredCount++ else notVisitedCount++
        if (isVisited && !isAnswered) notAnsweredCount++

        if (userAns != null) {
            if (userAns == questions[idx].correctIndex) {
                correctCount++
            } else {
                wrongCount++
            }
        } else {
            unattemptedCount++
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val isDark = ThemeManager.isDarkMode
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(20.dp)),
            color = QuizScreenBg,
            border = BorderStroke(1.dp, CardBorderColor),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header: Title and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isReviewMode) "Solutions Palette (उत्तर ग्रिड)" else "Question Palette (नेविगेशन ग्रिड)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SurfaceCardSecondary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status Legend Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CardBorderColor, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isReviewMode) {
                            // Solution Review Legend
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                PaletteLegendItem(
                                    color = Color(0xFF22C55E), // Green
                                    label = "Correct / सही ($correctCount)"
                                )
                                PaletteLegendItem(
                                    color = Color(0xFFEF4444), // Red
                                    label = "Wrong / गलत ($wrongCount)"
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                PaletteLegendItem(
                                    color = Color(0xFFA855F7), // Purple
                                    label = "Marked for Review ($markedCount)"
                                )
                                PaletteLegendItem(
                                    color = SurfaceCardSecondary,
                                    textColor = TextSecondary,
                                    hasBorder = true,
                                    label = "Unattempted / छोड़ा ($unattemptedCount)"
                                )
                            }
                        } else {
                            // Test Taking Mode Legend
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                PaletteLegendItem(
                                    color = QuizPillBg,
                                    textColor = QuizPillText,
                                    label = "Current (वर्तमान प्रश्न)"
                                )
                                PaletteLegendItem(
                                    color = Color(0xFF22C55E), // Green
                                    label = "Answered ($answeredCount)"
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                PaletteLegendItem(
                                    color = Color(0xFFA855F7), // Purple
                                    label = "Marked for Review ($markedCount)"
                                )
                                PaletteLegendItem(
                                    color = SurfaceCardSecondary,
                                    textColor = TextSecondary,
                                    hasBorder = true,
                                    label = "Not Answered (${notAnsweredCount + notVisitedCount})"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Subject Selector Tabs for the grid
                if (questions.size >= 80) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(paletteSubjects) { sIndex, sName ->
                            val isTabSelected = selectedSubjectTab == sIndex
                            Surface(
                                onClick = { selectedSubjectTab = sIndex },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isTabSelected) QuizPillBg else SurfaceCardSecondary,
                                shadowElevation = if (isTabSelected) 1.dp else 0.dp,
                                modifier = Modifier.height(32.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = sName,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isTabSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isTabSelected) QuizPillText else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Filtered questions based on selected subject tab
                val filteredIndices = remember(selectedSubjectTab, questions.size, examSections) {
                    if (selectedSubjectTab > 0 && selectedSubjectTab <= examSections.size) {
                        examSections[selectedSubjectTab - 1].range.toList()
                    } else {
                        questions.indices.toList()
                    }
                }

                // Question Grid (5 columns)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(filteredIndices) { _, questionIdx ->
                        val isCurrent = questionIdx == currentIndex
                        val isMarked = markedForReview[questionIdx] == true
                        val userAns = userAnswers[questionIdx]
                        val isAnswered = userAns != null
                        val isCorrect = isAnswered && userAns == questions[questionIdx].correctIndex
                        val isWrong = isAnswered && !isCorrect

                        val (bgColor, textColor, borderColor) = if (isReviewMode) {
                            when {
                                isWrong -> Triple(
                                    Color(0xFFEF4444),
                                    Color.White,
                                    if (isCurrent) Color(0xFF991B1B) else null
                                )
                                isCorrect -> Triple(
                                    Color(0xFF22C55E),
                                    Color.White,
                                    if (isCurrent) Color(0xFF14532D) else null
                                )
                                isMarked -> Triple(
                                    Color(0xFFA855F7),
                                    Color.White,
                                    if (isCurrent) Color(0xFF581C87) else null
                                )
                                else -> Triple(
                                    SurfaceCard,
                                    TextSecondary,
                                    if (isCurrent) Color(0xFF3B82F6) else CardBorderColor
                                )
                            }
                        } else {
                            when {
                                isCurrent -> Triple(QuizPillBg, QuizPillText, Color(0xFF3B82F6))
                                isMarked -> Triple(Color(0xFFA855F7), Color.White, null)
                                isAnswered -> Triple(Color(0xFF22C55E), Color.White, null)
                                else -> Triple(SurfaceCard, TextSecondary, CardBorderColor)
                            }
                        }

                        // Display number: subject-relative if specific subject tab, else global index+1
                        val displayNum = if (selectedSubjectTab > 0 && selectedSubjectTab <= examSections.size) {
                            (questionIdx - examSections[selectedSubjectTab - 1].range.first) + 1
                        } else {
                            questionIdx + 1
                        }

                        Surface(
                            onClick = { onSelectQuestion(questionIdx) },
                            shape = RoundedCornerShape(10.dp),
                            color = bgColor,
                            shadowElevation = if (isCurrent) 3.dp else 1.dp,
                            modifier = Modifier
                                .size(48.dp)
                                .then(
                                    if (isCurrent) {
                                        Modifier.border(2.5.dp, if (isReviewMode) Color(0xFF0284C7) else Color(0xFF3B82F6), RoundedCornerShape(10.dp))
                                    } else if (borderColor != null) {
                                        Modifier.border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                    } else {
                                        Modifier
                                    }
                                )
                                .testTag("palette_q_$questionIdx")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayNum.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )

                                // In Review/Solution Mode, show status icon at bottom-start
                                if (isReviewMode) {
                                    if (isCorrect) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Correct",
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(3.dp)
                                                .size(11.dp)
                                        )
                                    } else if (isWrong) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Wrong",
                                            tint = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(3.dp)
                                                .size(11.dp)
                                        )
                                    }
                                }

                                // Show Purple bookmark badge for Marked for Review questions
                                if (isMarked) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(2.dp)
                                            .size(13.dp)
                                            .clip(CircleShape)
                                            .background(if (isReviewMode && (isWrong || isCorrect)) Color(0xFF9333EA) else Color(0xFFA855F7))
                                            .border(1.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = "Marked",
                                            tint = Color.White,
                                            modifier = Modifier.size(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Close Action Button
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = QuizPillBg,
                    border = BorderStroke(1.dp, CardBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isReviewMode) "Close Palette & View Question" else "Close Palette & Continue Test",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = QuizPillText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteLegendItem(
    color: Color,
    label: String,
    textColor: Color = TextPrimary,
    hasBorder: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
                .then(
                    if (hasBorder) Modifier.border(1.dp, CardBorderColor, RoundedCornerShape(4.dp))
                    else Modifier
                )
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

/**
 * Modern, polished exam date & shift badge card placed cleanly between the question text and options.
 * Clear, legible, with a soft indigo/blue theme, calendar icon and crisp contrast.
 */
@Composable
fun QuestionExamShiftBadgeCard(
    shiftInfo: String,
    modifier: Modifier = Modifier
) {
    val isDark = ThemeManager.isDarkMode
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFF0F7FF),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF3B82F6) else Color(0xFFBFDBFE)),
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(8.dp)
                )
            }
            Text(
                text = shiftInfo,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) Color(0xFFBFDBFE) else Color(0xFF0F172A),
                letterSpacing = 0.1.sp
            )
        }
    }
}

/**
 * Returns the exam, date and shift badge string for chapter-wise questions.
 * e.g. "DP Constable 22/11/2025 Shift 1st"
 */
fun getQuestionExamShiftBadge(question: QuizQuestion, testTitle: String, isMultiSubjectFull: Boolean): String? {
    var shiftText = question.examShiftInfo?.trim()

    if (shiftText.isNullOrBlank()) {
        val extractedPair = GeminiQuestionParserService.extractShiftTagFromText(question.question)
        shiftText = extractedPair.second?.trim()
    }

    if (!shiftText.isNullOrBlank()) {
        return shiftText.replace("परीक्षा व शिफ्ट:", "", ignoreCase = true)
            .replace("परीक्षा व शिफ्ट", "", ignoreCase = true)
            .replace("Exam Date & Shift:", "", ignoreCase = true)
            .replace("Exam Shift:", "", ignoreCase = true)
            .replace("Shift Info:", "", ignoreCase = true)
            .trim()
    }
    if (!isMultiSubjectFull) {
        val title = testTitle.trim()
        val examPart = when {
            title.contains("Delhi Police", ignoreCase = true) || title.contains("दिल्ली पुलिस") -> "DP Constable"
            title.contains("SSC CGL", ignoreCase = true) -> "SSC CGL"
            title.contains("SSC GD", ignoreCase = true) -> "SSC GD"
            title.contains("SSC CHSL", ignoreCase = true) -> "SSC CHSL"
            title.contains("SSC MTS", ignoreCase = true) -> "SSC MTS"
            title.contains("SSC CPO", ignoreCase = true) -> "SSC CPO"
            title.contains("UP Police", ignoreCase = true) || title.contains("यूपी पुलिस") -> "UP Police"
            title.contains("RPF", ignoreCase = true) -> "RPF Constable"
            else -> title.split("•", "-").firstOrNull()?.trim()?.take(22) ?: "PYQ Special"
        }
        return "$examPart Official PYQ"
    }
    return null
}

fun parseSimpleMarkdown(text: String): AnnotatedString {
    return TextFormatHelper.parseMarkdown(text)
}
