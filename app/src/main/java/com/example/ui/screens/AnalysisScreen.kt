package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExamSubjectManager
import com.example.model.MasteryLevel
import com.example.model.SubjectWeaknessSummary
import com.example.model.TestPaper
import com.example.model.TopicWeaknessStat
import com.example.model.WeakAreaAnalyticsRepository
import com.example.ui.components.MistakesReviewDialog
import com.example.ui.theme.ScreenBackground
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextMuted

@Composable
fun AnalysisScreen(
    modifier: Modifier = Modifier,
    onNavigateToHome: (() -> Unit)? = null
) {
    var activeTestToLaunch by remember { mutableStateOf<TestPaper?>(null) }
    var showMistakesReviewDialog by remember { mutableStateOf(false) }

    val completedTestsCount = WeakAreaAnalyticsRepository.getCompletedTestsCount()
    val isAnalysisUnlocked = WeakAreaAnalyticsRepository.isAnalysisUnlocked()

    val topicStats = WeakAreaAnalyticsRepository.topicStatsMap.values.toList()
    val mistakesList = WeakAreaAnalyticsRepository.mistakeQuestionsList
    val subjectSummaries = WeakAreaAnalyticsRepository.getSubjectSummaries()
    val topWeakTopics = WeakAreaAnalyticsRepository.getTopWeakTopics(limit = 4)

    var selectedSubjectTab by remember { mutableStateOf("ALL") }
    var selectedMasteryFilter by remember { mutableStateOf("ALL") } // ALL, WEAK, MODERATE, STRONG

    val distinctSubjects = remember(topicStats) {
        listOf("ALL") + topicStats.map { it.subjectName }.distinct()
    }

    val displayedTopics = remember(selectedSubjectTab, selectedMasteryFilter, topicStats) {
        topicStats.filter { topic ->
            val matchSubject = if (selectedSubjectTab == "ALL") true else topic.subjectName == selectedSubjectTab
            val matchMastery = when (selectedMasteryFilter) {
                "WEAK" -> topic.masteryLevel == MasteryLevel.WEAK
                "MODERATE" -> topic.masteryLevel == MasteryLevel.MODERATE
                "STRONG" -> topic.masteryLevel == MasteryLevel.STRONG
                else -> true
            }
            matchSubject && matchMastery
        }.sortedWith(
            compareBy<TopicWeaknessStat> { it.accuracy }
                .thenByDescending { it.wrongCount + it.skippedCount }
        )
    }

    val weakCount = topicStats.count { it.masteryLevel == MasteryLevel.WEAK }
    val moderateCount = topicStats.count { it.masteryLevel == MasteryLevel.MODERATE }
    val strongCount = topicStats.count { it.masteryLevel == MasteryLevel.STRONG }
    val totalAttempts = topicStats.sumOf { it.totalCount }
    val overallAccuracy = if (totalAttempts > 0) {
        val totalCorrect = topicStats.sumOf { it.correctCount }
        (totalCorrect * 100) / totalAttempts
    } else 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        if (!isAnalysisUnlocked) {
            // Milestone locked state: User must complete 5 tests first
            AnalysisMilestoneLockedView(
                completedTestsCount = completedTestsCount,
                requiredTests = WeakAreaAnalyticsRepository.REQUIRED_TESTS_FOR_ANALYSIS,
                onStartTest = { testNumber ->
                    val testTitle = "ऑल इंडिया डायग्नोस्टिक टेस्ट #$testNumber"
                    val questions = ExamSubjectManager.getQuestionsForExam(testTitle)
                    activeTestToLaunch = TestPaper(
                        id = "diagnostic_test_$testNumber",
                        title = testTitle,
                        totalQuestions = questions.size,
                        durationMinutes = maxOf((questions.size * 1.2).toInt(), 15),
                        totalMarks = questions.size,
                        difficulty = "Exam Level",
                        questions = questions
                    )
                },
                onNavigateToHome = onNavigateToHome
            )
        } else {
            // MAIN SCROLLABLE UNLOCKED CONTENT
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // 1. TOP HEADER ITEM
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "प्रदर्शन व कमज़ोर विषय",
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "टेस्ट प्रदर्शन से ऑटो-अपडेटेड लाइव विश्लेषण",
                                fontSize = 11.5.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    if (weakCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Text(
                                text = "$weakCount कमज़ोर",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // 2. HERO ACTION CARDS (Weak Area Booster & Review Mistakes)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start Weak Topics Practice Booster Test Card
                    Card(
                        onClick = {
                            val practiceQuestions = WeakAreaAnalyticsRepository.generateWeakTopicsPracticeTest()
                            activeTestToLaunch = TestPaper(
                                id = "weak_area_booster_${System.currentTimeMillis()}",
                                title = "🎯 कमज़ोर टॉपिक सुधार टेस्ट (Weak Booster)",
                                totalQuestions = practiceQuestions.size,
                                durationMinutes = maxOf((practiceQuestions.size * 1.2).toInt(), 15),
                                totalMarks = practiceQuestions.size,
                                difficulty = "Exam Level",
                                questions = practiceQuestions
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("launch_weak_booster_test_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFEA580C), Color(0xFFF97316))
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "BOOSTER",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = "कमज़ोर टॉपिक टेस्ट",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "सुधार हेतु स्पेशल टेस्ट दें",
                                        fontSize = 11.sp,
                                        color = Color(0xFFFFEDD5)
                                    )
                                }
                            }
                        }
                    }

                    // Review Mistakes Card
                    Card(
                        onClick = { showMistakesReviewDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_mistakes_review_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1E40AF), Color(0xFF3B82F6))
                                    )
                                )
                                .padding(14.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "${mistakesList.size} प्रश्न",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = "गलतियाँ समीक्षा",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "गलत व छूटे प्रश्न रिवाइज करें",
                                        fontSize = 11.sp,
                                        color = Color(0xFFDBEAFE)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. OVERALL PERFORMANCE METRICS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.2f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "📊",
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "समग्र सटीकता व स्थिति",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = "कुल प्रश्न: $totalAttempts",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Progress bar with percentage
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "औसत सटीकता (Overall Accuracy)",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$overallAccuracy%",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (overallAccuracy >= 70) Color(0xFF16A34A) else if (overallAccuracy >= 45) Color(0xFFD97706) else Color(0xFFDC2626)
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (overallAccuracy / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (overallAccuracy >= 70) Color(0xFF16A34A) else if (overallAccuracy >= 45) Color(0xFFD97706) else Color(0xFFDC2626),
                                trackColor = Color(0xFFF1F5F9)
                            )
                        }

                        // Status metric 3-box row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatusMetricBox(
                                label = "कमज़ोर टॉपिक",
                                count = weakCount,
                                bgColor = Color(0xFFFEF2F2),
                                borderColor = Color(0xFFFECACA),
                                textColor = Color(0xFFDC2626),
                                modifier = Modifier.weight(1f)
                            )
                            StatusMetricBox(
                                label = "औसत टॉपिक",
                                count = moderateCount,
                                bgColor = Color(0xFFFFFBEB),
                                borderColor = Color(0xFFFDE68A),
                                textColor = Color(0xFFD97706),
                                modifier = Modifier.weight(1f)
                            )
                            StatusMetricBox(
                                label = "मजबूत पकड़",
                                count = strongCount,
                                bgColor = Color(0xFFF0FDF4),
                                borderColor = Color(0xFFBBF7D0),
                                textColor = Color(0xFF16A34A),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. CRITICAL WEAK TOPIC HIGHLIGHT (If weak areas exist)
            if (topWeakTopics.isNotEmpty()) {
                val primaryWeak = topWeakTopics.first()
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                        border = BorderStroke(1.2.dp, Color(0xFFFED7AA))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEA580C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "शीर्ष सुधार क्षेत्र: ${primaryWeak.topicName}",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF9A3412)
                                    )
                                    Text(
                                        text = "${primaryWeak.subjectName} • सटीकता: ${primaryWeak.accuracy}% (${primaryWeak.wrongCount} गलत, ${primaryWeak.skippedCount} छूटे)",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFFC2410C),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFEDD5).copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, Color(0xFFFED7AA))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = primaryWeak.recommendation,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF7C2D12),
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. SUBJECT PERFORMANCE SUMMARY
            if (subjectSummaries.isNotEmpty()) {
                item {
                    Text(
                        text = "विषयवार प्रदर्शन (Subject-wise Breakdown)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(subjectSummaries) { subject ->
                            SubjectPerformanceMiniCard(
                                summary = subject,
                                onClick = {
                                    selectedSubjectTab = subject.subjectName
                                }
                            )
                        }
                    }
                }
            }

            // 6. TOPIC-LEVEL DIAGNOSTICS & PRACTICE HEADER
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "टॉपिकवार डायग्नोस्टिक्स व अभ्यास",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    // Subject filter pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(distinctSubjects) { sub ->
                            val isSelected = selectedSubjectTab == sub
                            Surface(
                                onClick = { selectedSubjectTab = sub },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color(0xFF2563EB) else Color.White,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1)
                                ),
                                shadowElevation = if (isSelected) 1.dp else 0.dp
                            ) {
                                Text(
                                    text = if (sub == "ALL") "सभी विषय" else sub,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Mastery filter chips (Scrollable row to avoid cramping)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf(
                            FilterOption("ALL", "सभी (${topicStats.size})", Color(0xFF0F172A), Color(0xFFF1F5F9)),
                            FilterOption("WEAK", "कमज़ोर ($weakCount)", Color(0xFFDC2626), Color(0xFFFEF2F2)),
                            FilterOption("MODERATE", "औसत ($moderateCount)", Color(0xFFD97706), Color(0xFFFFFBEB)),
                            FilterOption("STRONG", "मजबूत ($strongCount)", Color(0xFF16A34A), Color(0xFFF0FDF4))
                        )

                        items(filters) { item ->
                            val isSelected = selectedMasteryFilter == item.key
                            Surface(
                                onClick = { selectedMasteryFilter = item.key },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) item.accentColor else item.bgColor,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) item.accentColor else item.accentColor.copy(alpha = 0.35f)
                                ),
                                shadowElevation = if (isSelected) 1.dp else 0.dp
                            ) {
                                Text(
                                    text = item.label,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else item.accentColor,
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.5.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 7. TOPIC ITEMS LIST
            if (displayedTopics.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (topicStats.isEmpty()) Icons.Default.Analytics else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (topicStats.isEmpty()) Color(0xFF2563EB) else Color(0xFF16A34A),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = if (topicStats.isEmpty()) "🎯 अभी तक कोई टेस्ट पूरा नहीं हुआ" else "इस फ़िल्टर में कोई टॉपिक नहीं है",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = if (topicStats.isEmpty())
                                    "जैसे ही आप कोई भी मॉक टेस्ट सबमिट करेंगे, आपके सही, गलत व छूटे हुए प्रश्नों के आधार पर कमज़ोर और मजबूत टॉपिक्स का 100% लाइव एनालिसिस यहाँ दिखने लगेगा।"
                                else
                                    "चयनित फ़िल्टर के अनुसार कोई टॉपिक उपलब्ध नहीं है।",
                                fontSize = 12.5.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            } else {
                items(
                    items = displayedTopics,
                    key = { "${it.subjectName}_${it.topicName}" }
                ) { topic ->
                    TopicAnalysisCard(
                        topic = topic,
                        onPracticeTopic = { topicName ->
                            val practiceQuestions = WeakAreaAnalyticsRepository.generateWeakTopicsPracticeTest(
                                title = "🎯 $topicName अभ्यास टेस्ट"
                            )
                            activeTestToLaunch = TestPaper(
                                id = "topic_practice_${System.currentTimeMillis()}",
                                title = "🎯 $topicName अभ्यास टेस्ट",
                                totalQuestions = practiceQuestions.size,
                                durationMinutes = maxOf((practiceQuestions.size * 1.2).toInt(), 15),
                                totalMarks = practiceQuestions.size,
                                difficulty = "Exam Level",
                                questions = practiceQuestions
                            )
                        }
                    )
                }
            }
        }
    }
}

    // DIALOGS
    if (showMistakesReviewDialog) {
        MistakesReviewDialog(
            onDismiss = { showMistakesReviewDialog = false },
            onStartTopicPractice = { topicName ->
                showMistakesReviewDialog = false
                val practiceQuestions = WeakAreaAnalyticsRepository.generateWeakTopicsPracticeTest(
                    title = "🎯 $topicName अभ्यास टेस्ट"
                )
                activeTestToLaunch = TestPaper(
                    id = "topic_practice_${System.currentTimeMillis()}",
                    title = "🎯 $topicName अभ्यास टेस्ट",
                    totalQuestions = practiceQuestions.size,
                    durationMinutes = maxOf((practiceQuestions.size * 1.2).toInt(), 15),
                    totalMarks = practiceQuestions.size,
                    difficulty = "Exam Level",
                    questions = practiceQuestions
                )
            }
        )
    }

    if (activeTestToLaunch != null) {
        MockTestDialog(
            testTitle = activeTestToLaunch!!.title,
            customQuestions = activeTestToLaunch!!.questions,
            onDismiss = { activeTestToLaunch = null }
        )
    }
}

@Composable
private fun AnalysisMilestoneLockedView(
    completedTestsCount: Int,
    requiredTests: Int,
    onStartTest: (Int) -> Unit,
    onNavigateToHome: (() -> Unit)?
) {
    val remaining = (requiredTests - completedTestsCount).coerceAtLeast(0)
    val progress = (completedTestsCount.toFloat() / requiredTests.toFloat()).coerceIn(0f, 1f)
    val nextTestNumber = (completedTestsCount + 1).coerceAtMost(requiredTests)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP HEADER ITEM
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "प्रदर्शन व कमज़ोर विषय",
                            fontSize = 17.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "सटीक AI विश्लेषण हेतु 10 फुल टेस्ट्स आवश्यक हैं",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "$completedTestsCount/$requiredTests पूर्ण",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }
        }

        // 2. HERO PROGRESS CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF3B82F6))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Insights,
                                        contentDescription = null,
                                        tint = Color(0xFFFDE047),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "AI डायग्नोस्टिक माइलस्टोन",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Text(
                                text = "प्रगति: ${(progress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBAE6FD)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "कम से कम 10 फुल टेस्ट्स का डेटा आवश्यक है",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                lineHeight = 23.sp
                            )
                            Text(
                                text = if (remaining > 0)
                                    "आपके अभी $completedTestsCount फुल टेस्ट्स दर्ज हुए हैं। पूर्ण कमज़ोर विषय रिपोर्ट और बूस्टर टेस्ट अनलॉक करने के लिए $remaining और फुल टेस्ट्स दें।"
                                else
                                    "सभी 10 फुल टेस्ट्स पूर्ण! आपकी रिपोर्ट तैयार हो रही है...",
                                fontSize = 12.5.sp,
                                color = Color(0xFFE0F2FE),
                                lineHeight = 17.sp
                            )
                        }

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(9.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = Color(0xFF22C55E),
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )

                        // 5 Step Indicator Dots
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (step in 1..requiredTests) {
                                val isDone = step <= completedTestsCount
                                val isCurrent = step == nextTestNumber && !isDone
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isDone -> Color(0xFF22C55E)
                                                    isCurrent -> Color(0xFFFBBF24)
                                                    else -> Color.White.copy(alpha = 0.25f)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color(0xFF78350F),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.7f),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "T$step",
                                        fontSize = 10.sp,
                                        fontWeight = if (isDone || isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isDone || isCurrent) Color.White else Color(0xFFBAE6FD).copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. QUICK ACTION CTA BUTTONS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.2f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "अगला डायग्नोस्टिक टेस्ट #$nextTestNumber दें",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "सभी विषयों के रियल परीक्षा स्तर के प्रश्न",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Button(
                        onClick = { onStartTest(nextTestNumber) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("start_milestone_test_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "टेस्ट #$nextTestNumber शुरू करें",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (onNavigateToHome != null) {
                        OutlinedButton(
                            onClick = onNavigateToHome,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("go_to_home_tests_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "होम स्क्रीन से पसंदीदा टेस्ट चुनें",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }

        // 4. WHY 5 TESTS ARE REQUIRED EXPLANATION CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.2f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "💡", fontSize = 16.sp)
                        Text(
                            text = "कम से कम 10 फुल टेस्ट्स का डेटा क्यों आवश्यक है?",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    MilestoneReasonRow(
                        iconEmoji = "📊",
                        title = "100% सटीक सांख्यिकीय विश्लेषण (Statistical Accuracy)",
                        desc = "1-2 टेस्ट में तुक्के या कम प्रश्नों से सही कमज़ोरी पता नहीं चलती। 10 फुल टेस्ट्स पूरे होने पर सभी विषयों (Maths, Reasoning, GK, Science, Hindi) का वास्तविक स्तर सटीक निकलता है।"
                    )

                    MilestoneReasonRow(
                        iconEmoji = "🎯",
                        title = "कमज़ोर टॉपिक ऑटो-डिटेक्शन (Weak Areas Detection)",
                        desc = "जिन टॉपिक्स में लगातार 3+ गलतियां या प्रश्न छूट रहे हैं, सिस्टम उन्हें ऑटो-डिटेक्ट करके आपकी पर्सनलाइज्ड लिस्ट तैयार करता है।"
                    )

                    MilestoneReasonRow(
                        iconEmoji = "⚡",
                        title = "कमज़ोर टॉपिक सुधार टेस्ट (Weak Area Booster)",
                        desc = "केवल आपके कमज़ोर क्षेत्रों के प्रश्नों से बना कस्टमाइज़्ड बूस्टर टेस्ट जेनरेट होता है ताकि आप तुरंत सुधार कर सकें।"
                    )

                    MilestoneReasonRow(
                        iconEmoji = "📑",
                        title = "गलत प्रश्नों की लाइव समीक्षा (Mistakes Review Bank)",
                        desc = "सभी 10 फुल टेस्ट्स में हुई गलतियों का विस्तृत व्याख्या सहित सॉल्यूशन बैंक तैयार होता है।"
                    )
                }
            }
        }

        // 5. WHAT WILL BE UNLOCKED TEASER
        item {
            Text(
                text = "10 फुल टेस्ट्स बाद क्या अनलॉक होगा? (Features Preview)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LockedPreviewItem(
                    title = "विषयवार व टॉपिक-वाइज़ सटीकता डायग्नोस्टिक्स",
                    subtitle = "गणित, रीजनिंग, GK के हर चैप्टर का स्तर (कमज़ोर / मध्यम / मजबूत)"
                )
                LockedPreviewItem(
                    title = "AI सिफारिशें व तैयारी के टिप्स",
                    subtitle = "हर कमज़ोर टॉपिक पर सुधार के लिए सटीक एक्शन प्लान"
                )
                LockedPreviewItem(
                    title = "गलत व छूटे प्रश्नों की 1-क्लिक समीक्षा",
                    subtitle = "टेस्ट के बाद बिना समय गंवाए डायरेक्ट मिस्टेक प्रैक्टिस"
                )
            }
        }
    }
}

@Composable
private fun MilestoneReasonRow(
    iconEmoji: String,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = iconEmoji, fontSize = 16.sp, modifier = Modifier.padding(top = 2.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text(
                text = desc,
                fontSize = 11.5.sp,
                color = Color(0xFF64748B),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun LockedPreviewItem(
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF3C7)
            ) {
                Text(
                    text = "🔒 लॉक्ड",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB45309),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

private data class FilterOption(
    val key: String,
    val label: String,
    val accentColor: Color,
    val bgColor: Color
)

@Composable
private fun StatusMetricBox(
    label: String,
    count: Int,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "$count",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SubjectPerformanceMiniCard(
    summary: SubjectWeaknessSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(190.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = summary.subjectName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (summary.accuracy >= 70) Color(0xFFDCFCE7) else if (summary.accuracy >= 45) Color(0xFFFEF3C7) else Color(0xFFFEE2E2)
                ) {
                    Text(
                        text = "${summary.accuracy}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.accuracy >= 70) Color(0xFF166534) else if (summary.accuracy >= 45) Color(0xFF92400E) else Color(0xFF991B1B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (summary.accuracy / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (summary.accuracy >= 70) Color(0xFF16A34A) else if (summary.accuracy >= 45) Color(0xFFD97706) else Color(0xFFDC2626),
                trackColor = Color(0xFFF1F5F9)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${summary.correctQuestions} सही • ${summary.wrongQuestions} गलत",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun TopicAnalysisCard(
    topic: TopicWeaknessStat,
    onPracticeTopic: (String) -> Unit
) {
    val masteryColor = when (topic.masteryLevel) {
        MasteryLevel.WEAK -> Color(0xFFDC2626)
        MasteryLevel.MODERATE -> Color(0xFFD97706)
        MasteryLevel.STRONG -> Color(0xFF16A34A)
    }

    val badgeBg = when (topic.masteryLevel) {
        MasteryLevel.WEAK -> Color(0xFFFEF2F2)
        MasteryLevel.MODERATE -> Color(0xFFFFFBEB)
        MasteryLevel.STRONG -> Color(0xFFF0FDF4)
    }

    val badgeBorder = when (topic.masteryLevel) {
        MasteryLevel.WEAK -> Color(0xFFFECACA)
        MasteryLevel.MODERATE -> Color(0xFFFDE68A)
        MasteryLevel.STRONG -> Color(0xFFBBF7D0)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Topic Name & Mastery Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic.topicName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = topic.subjectName,
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeBorder)
                ) {
                    Text(
                        text = topic.masteryLevel.hindiLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = masteryColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp)
                    )
                }
            }

            // Stats breakdown & Progress bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${topic.totalCount} प्रश्न: ${topic.correctCount} सही, ${topic.wrongCount} गलत, ${topic.skippedCount} छूटे",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${topic.accuracy}%",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = masteryColor
                )
            }

            LinearProgressIndicator(
                progress = { (topic.accuracy / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = masteryColor,
                trackColor = Color(0xFFF1F5F9)
            )

            // Recommendation tip & Practice Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = topic.recommendation,
                        fontSize = 11.5.sp,
                        color = Color(0xFF78350F),
                        maxLines = 2,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onPracticeTopic(topic.topicName) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "अभ्यास करें",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
