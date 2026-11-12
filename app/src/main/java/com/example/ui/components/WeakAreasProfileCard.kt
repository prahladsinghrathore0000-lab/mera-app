package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MasteryLevel
import com.example.model.SubjectWeaknessSummary
import com.example.model.TopicWeaknessStat
import com.example.model.WeakAreaAnalyticsRepository

@Composable
fun WeakAreasProfileCard(
    onLaunchWeakPractice: () -> Unit,
    onOpenMistakesReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topicStats = WeakAreaAnalyticsRepository.topicStatsMap.values.toList()
    val mistakesList = WeakAreaAnalyticsRepository.mistakeQuestionsList
    val subjectSummaries = WeakAreaAnalyticsRepository.getSubjectSummaries()
    val topWeakTopics = WeakAreaAnalyticsRepository.getTopWeakTopics(limit = 4)

    var selectedSubjectTab by remember { mutableStateOf("ALL") }
    var isExpandedTopicsView by remember { mutableStateOf(false) }

    val distinctSubjects = remember(topicStats.size) {
        listOf("ALL") + topicStats.map { it.subjectName }.distinct()
    }

    val displayedTopics = remember(selectedSubjectTab, topicStats.size) {
        if (selectedSubjectTab == "ALL") {
            topicStats.sortedWith(
                compareBy<TopicWeaknessStat> { it.accuracy }
                    .thenByDescending { it.wrongCount + it.skippedCount }
            )
        } else {
            topicStats.filter { it.subjectName == selectedSubjectTab }
                .sortedBy { it.accuracy }
        }
    }

    val weakCount = topicStats.count { it.masteryLevel == MasteryLevel.WEAK }
    val moderateCount = topicStats.count { it.masteryLevel == MasteryLevel.MODERATE }
    val strongCount = topicStats.count { it.masteryLevel == MasteryLevel.STRONG }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFFDE68A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFFBEB), Color(0xFFFFFFFF))
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEA580C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "कमज़ोर टॉपिक व विषय विश्लेषण",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Text(
                            text = "टेस्ट में गलत/छूटे प्रश्नों से ऑटो-अपडेटेड",
                            fontSize = 11.sp,
                            color = Color(0xFFEA580C),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFEE2E2))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "$weakCount कमज़ोर टॉपिक",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB91C1C)
                        )
                    }
                }
            }

            // Summary Pills Grid (Weak, Moderate, Strong, Mistakes)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusCounterBadge(
                    label = "कमज़ोर",
                    count = weakCount,
                    bgColor = Color(0xFFFEF2F2),
                    textColor = Color(0xFFDC2626),
                    modifier = Modifier.weight(1f)
                )
                StatusCounterBadge(
                    label = "औसत",
                    count = moderateCount,
                    bgColor = Color(0xFFFFFBEB),
                    textColor = Color(0xFFD97706),
                    modifier = Modifier.weight(1f)
                )
                StatusCounterBadge(
                    label = "मजबूत",
                    count = strongCount,
                    bgColor = Color(0xFFF0FDF4),
                    textColor = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
                StatusCounterBadge(
                    label = "गलत/छूटे",
                    count = mistakesList.size,
                    bgColor = Color(0xFFEFF6FF),
                    textColor = Color(0xFF2563EB),
                    modifier = Modifier.weight(1.1f)
                )
            }

            // High Priority Alert Banner (Top critical weak area callout)
            if (topWeakTopics.isNotEmpty()) {
                val primaryWeak = topWeakTopics.first()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "सर्वोच्च सुधार आवश्यकता (Priority Focus):",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            }
                            Text(
                                text = "${primaryWeak.accuracy}% सटीकता",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }

                        Text(
                            text = "📌 [${primaryWeak.subjectName}] ${primaryWeak.topicName}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7F1D1D)
                        )

                        Text(
                            text = "💡 ${primaryWeak.recommendation}",
                            fontSize = 11.sp,
                            color = Color(0xFF991B1B),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Subject Filter Tabs
            if (distinctSubjects.size > 1) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(distinctSubjects) { subj ->
                        val isSel = selectedSubjectTab == subj
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedSubjectTab = subj },
                            label = {
                                Text(
                                    text = if (subj == "ALL") "सभी विषय" else subj,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0F172A),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFF1F5F9),
                                labelColor = Color(0xFF334155)
                            )
                        )
                    }
                }
            }

            // List of Topics (Showing top 3 by default, expandable to full list)
            val topicsToShow = if (isExpandedTopicsView) displayedTopics else displayedTopics.take(3)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (displayedTopics.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "मॉक टेस्ट पूरा करने के बाद टॉपिकवार विश्लेषण यहाँ दिखेगा।",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    topicsToShow.forEach { topic ->
                        TopicDiagnosticRow(topic = topic)
                    }
                }
            }

            if (displayedTopics.size > 3) {
                Surface(
                    onClick = { isExpandedTopicsView = !isExpandedTopicsView },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpandedTopicsView) "कम दिखाएं ▲" else "सभी ${displayedTopics.size} टॉपिक्स की लिस्ट देखें ▼",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }

            // Action Buttons: Practice Weak Topics & Review Mistakes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onLaunchWeakPractice,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    modifier = Modifier.weight(1.1f).height(42.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "कमज़ोर टॉपिक टेस्ट",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onOpenMistakesReview,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF93C5FD)),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Icon(Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "गलतियां देखें (${mistakesList.size})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCounterBadge(
    label: String,
    count: Int,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun TopicDiagnosticRow(
    topic: TopicWeaknessStat
) {
    val barColor = when (topic.masteryLevel) {
        MasteryLevel.WEAK -> Color(0xFFDC2626)
        MasteryLevel.MODERATE -> Color(0xFFD97706)
        MasteryLevel.STRONG -> Color(0xFF16A34A)
    }

    val badgeBg = when (topic.masteryLevel) {
        MasteryLevel.WEAK -> Color(0xFFFEE2E2)
        MasteryLevel.MODERATE -> Color(0xFFFEF3C7)
        MasteryLevel.STRONG -> Color(0xFFDCFCE7)
    }

    val badgeText = when (topic.masteryLevel) {
        MasteryLevel.WEAK -> Color(0xFFB91C1C)
        MasteryLevel.MODERATE -> Color(0xFFB45309)
        MasteryLevel.STRONG -> Color(0xFF15803D)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Topic Title & Subject Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE2E8F0))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = topic.subjectName,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                    }
                    Text(
                        text = topic.topicName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(badgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${topic.accuracy}% सटीकता",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText
                    )
                }
            }

            // Accuracy Bar
            LinearProgressIndicator(
                progress = { (topic.accuracy / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = barColor,
                trackColor = Color(0xFFE2E8F0)
            )

            // Counts Row (Right, Wrong, Skipped)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "✅ ${topic.correctCount} सही",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF16A34A)
                    )
                    Text(
                        text = "❌ ${topic.wrongCount} गलत",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFDC2626)
                    )
                    Text(
                        text = "⏭️ ${topic.skippedCount} छूटे",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD97706)
                    )
                }

                Text(
                    text = topic.masteryLevel.hindiLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeText
                )
            }
        }
    }
}
