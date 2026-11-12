package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OfflineTestRepository
import com.example.model.TestHistoryRepository
import com.example.model.UnlockedTestsRepository
import com.example.ui.components.RewardAdUnlockDialog
import androidx.compose.material3.AlertDialog
import com.example.ui.theme.Primary40
import com.example.ui.theme.ScreenBackground
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThemeManager

data class PracticeSubject(
    val title: String,
    val icon: ImageVector,
    val totalQuestions: String,
    val bgColor: Color,
    val darkBgColor: Color,
    val iconColor: Color,
    val description: String
)

@Composable
fun PracticeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = ThemeManager.isDarkMode
    var activeQuizTitle by remember { mutableStateOf<String?>(null) }
    var pendingQuizToUnlock by remember { mutableStateOf<String?>(null) }
    var showOfflineUnlockNotice by remember { mutableStateOf(false) }

    val subjects = remember {
        listOf(
            PracticeSubject(
                "Quantitative Aptitude",
                Icons.Default.Calculate,
                "2,400+ Questions",
                Color(0xFFEFF6FF),
                Color(0xFF1E3A8A).copy(alpha = 0.4f),
                Color(0xFF3B82F6),
                "Arithmetic, Algebra, Geometry, Mensuration & Trigonometry"
            ),
            PracticeSubject(
                "Logical & Analytical Reasoning",
                Icons.Default.Psychology,
                "1,950+ Questions",
                Color(0xFFFAF5FF),
                Color(0xFF581C87).copy(alpha = 0.4f),
                Color(0xFFA855F7),
                "Analogies, Series, Coding-Decoding, Syllogism & Puzzles"
            ),
            PracticeSubject(
                "General Awareness & GK",
                Icons.Default.Public,
                "3,200+ Questions",
                Color(0xFFECFDF5),
                Color(0xFF064E3B).copy(alpha = 0.4f),
                Color(0xFF10B981),
                "Indian Polity, History, Geography, Economy & Static GK"
            ),
            PracticeSubject(
                "General Science (Physics, Chem, Bio)",
                Icons.Default.Science,
                "1,600+ Questions",
                Color(0xFFFFFBEB),
                Color(0xFF78350F).copy(alpha = 0.4f),
                Color(0xFFF59E0B),
                "High yield science for Railway RRB & SSC exams"
            ),
            PracticeSubject(
                "English Comprehension",
                Icons.AutoMirrored.Filled.MenuBook,
                "1,800+ Questions",
                Color(0xFFFEF2F2),
                Color(0xFF7F1D1D).copy(alpha = 0.4f),
                Color(0xFFEF4444),
                "Grammar, Vocabulary, Idioms, Cloze Test & Reading"
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("practice_screen_content"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        ) {
            item(key = "practice_header") {
                Text(
                    text = "Practice Section",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Solve subject-wise topic tests and previous year questions",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            items(
                items = subjects,
                key = { it.title }
            ) { subject ->
                val practiceKey = "${subject.title} Practice Test"
                val stats = TestHistoryRepository.getStats(practiceKey, subject.title)
                val isAttempted = stats != null && stats.attemptsCount > 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("practice_card_${subject.title}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isDark) subject.darkBgColor else subject.bgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = subject.icon,
                                        contentDescription = subject.title,
                                        tint = subject.iconColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = subject.title,
                                            fontSize = 15.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        if (stats != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFEF3C7))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Attempt #${stats.attemptsCount}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = subject.totalQuestions,
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFF60A5FA) else Primary40,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = subject.description,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }

                            val isUnlocked = UnlockedTestsRepository.isTestUnlocked(practiceKey) || isAttempted

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (isUnlocked) {
                                        // Already unlocked for lifetime -> directly start (offline or online)
                                        activeQuizTitle = practiceKey
                                    } else {
                                        // 1-time ad watch to unlock for lifetime
                                        if (OfflineTestRepository.isNetworkAvailable(context)) {
                                            pendingQuizToUnlock = practiceKey
                                        } else {
                                            showOfflineUnlockNotice = true
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAttempted) Color(0xFF059669) else if (isUnlocked) Color(0xFF0284C7) else Color(0xFF2563EB)
                                ),
                                contentPadding = ButtonDefaults.TextButtonContentPadding,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAttempted) Icons.Default.Refresh else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isAttempted) "Re-Attempt" else "Start", fontSize = 12.sp)
                            }
                        }

                        if (stats != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFF0FDF4))
                                    .border(1.dp, if (isDark) Color(0xFF059669).copy(alpha = 0.4f) else Color(0xFFBBF7D0), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color(0xFFEAB308),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "High Score: ${String.format("%.2f", stats.highestScore)} Marks",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534)
                                    )
                                }
                                Text(
                                    text = "Attempt #${stats.attemptsCount} (Lifetime Free)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF059669)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(70.dp))
            }
        }

        // Rewarded Video Ad Unlock Dialog (1 Ad = Lifetime Free)
        pendingQuizToUnlock?.let { title ->
            RewardAdUnlockDialog(
                testTitle = title,
                onDismiss = { pendingQuizToUnlock = null },
                onTestUnlocked = {
                    pendingQuizToUnlock = null
                    activeQuizTitle = title
                    OfflineTestRepository.saveTestOffline(context, title, null)
                }
            )
        }

        if (showOfflineUnlockNotice) {
            AlertDialog(
                onDismissRequest = { showOfflineUnlockNotice = false },
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = "⚠️ ऑफ़लाइन मोड (Offline Mode)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = "यह नया टेस्ट पहली बार अनलॉक करने के लिए सिर्फ 1 बार ऑनलाइन Ad देखने की आवश्यकता है।\n\nएक बार ऑनलाइन देखने के बाद यह टेस्ट आपके फोन में सेव हो जाएगा और आप इसे हमेशा बिना इंटरनेट (Offline) भी दे सकते हैं!",
                        fontSize = 13.5.sp,
                        color = TextSecondary,
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

        activeQuizTitle?.let { title ->
            MockTestDialog(
                testTitle = title,
                onDismiss = { activeQuizTitle = null }
            )
        }
    }
}
