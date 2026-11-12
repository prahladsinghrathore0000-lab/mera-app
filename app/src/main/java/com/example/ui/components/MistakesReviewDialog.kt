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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MistakeQuestionRecord
import com.example.model.WeakAreaAnalyticsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakesReviewDialog(
    onDismiss: () -> Unit,
    onStartTopicPractice: ((String) -> Unit)? = null
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val allMistakes = WeakAreaAnalyticsRepository.mistakeQuestionsList

    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, WRONG, SKIPPED
    var selectedSubjectFilter by remember { mutableStateOf("ALL") }

    val distinctSubjects = remember(allMistakes.size) {
        listOf("ALL") + allMistakes.map { it.subject }.distinct()
    }

    val filteredList = remember(selectedTypeFilter, selectedSubjectFilter, allMistakes.size) {
        allMistakes.filter { item ->
            val matchType = when (selectedTypeFilter) {
                "WRONG" -> item.userSelectedOption != null
                "SKIPPED" -> item.userSelectedOption == null
                else -> true
            }
            val matchSubject = if (selectedSubjectFilter == "ALL") true else item.subject == selectedSubjectFilter
            matchType && matchSubject
        }
    }

    ModalBottomSheet(
        onDismissRequest = { onDismiss() },
        sheetState = bottomSheetState,
        containerColor = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📝 रिवीज़न: गलत व छूटे हुए प्रश्न",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFEF2F2))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${allMistakes.size} प्रश्न",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                    Text(
                        text = "कमज़ोर टॉपिक सुधारने के लिए सही उत्तर व व्याख्या समझें",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                IconButton(
                    onClick = { onDismiss() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                }
            }

            // Type Filter Chips (All, Wrong, Skipped)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val wrongCount = allMistakes.count { it.userSelectedOption != null }
                val skippedCount = allMistakes.count { it.userSelectedOption == null }

                FilterChip(
                    selected = selectedTypeFilter == "ALL",
                    onClick = { selectedTypeFilter = "ALL" },
                    label = { Text("सभी (${allMistakes.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2563EB),
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedTypeFilter == "WRONG",
                    onClick = { selectedTypeFilter = "WRONG" },
                    label = { Text("❌ गलतियां ($wrongCount)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFDC2626),
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = selectedTypeFilter == "SKIPPED",
                    onClick = { selectedTypeFilter = "SKIPPED" },
                    label = { Text("⏭️ छूटे हुए ($skippedCount)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFD97706),
                        selectedLabelColor = Color.White
                    )
                )
            }

            // Subject Filter Chips
            if (distinctSubjects.size > 2) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(distinctSubjects) { subj ->
                        val isSel = selectedSubjectFilter == subj
                        Surface(
                            onClick = { selectedSubjectFilter = subj },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0xFF1E293B) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF1E293B) else Color(0xFFE2E8F0)),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = if (subj == "ALL") "सभी विषय" else subj,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Questions List
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "शाबाश! इस फ़िल्टर में कोई गलत या छूटा प्रश्न नहीं है।",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "नया टेस्ट दें और अपनी पकड़ मजबूत बनाएं।",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(filteredList) { index, item ->
                        MistakeQuestionCard(
                            index = index + 1,
                            item = item
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MistakeQuestionCard(
    index: Int,
    item: MistakeQuestionRecord
) {
    val isWrong = item.userSelectedOption != null
    var showSolution by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isWrong) Color(0xFFFECACA) else Color(0xFFFED7AA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Badges Row (Subject, Topic & Status)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEFF6FF))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.subject,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.topic,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                    }
                }

                // Outcome Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isWrong) Color(0xFFFEE2E2) else Color(0xFFFEF3C7))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isWrong) "❌ गलत उत्तर" else "⏭️ छोड़ा गया",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWrong) Color(0xFFB91C1C) else Color(0xFFB45309)
                    )
                }
            }

            // Question Text
            Text(
                text = "Q$index. ${item.questionText}",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B),
                lineHeight = 19.sp
            )

            // Options List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item.options.forEachIndexed { optIdx, optText ->
                    val isCorrectOpt = optIdx == item.correctIndex
                    val isUserPicked = optIdx == item.userSelectedOption

                    val optBgColor = when {
                        isCorrectOpt -> Color(0xFFDCFCE7)
                        isUserPicked -> Color(0xFFFEE2E2)
                        else -> Color(0xFFF8FAFC)
                    }
                    val optBorderColor = when {
                        isCorrectOpt -> Color(0xFF86EFAC)
                        isUserPicked -> Color(0xFFFCA5A5)
                        else -> Color(0xFFE2E8F0)
                    }
                    val optTextColor = when {
                        isCorrectOpt -> Color(0xFF15803D)
                        isUserPicked -> Color(0xFFB91C1C)
                        else -> Color(0xFF334155)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(optBgColor)
                            .border(1.dp, optBorderColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${('A' + optIdx)}. $optText",
                            fontSize = 12.sp,
                            fontWeight = if (isCorrectOpt || isUserPicked) FontWeight.Bold else FontWeight.Normal,
                            color = optTextColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (isCorrectOpt) {
                            Text("✅ सही उत्तर", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        } else if (isUserPicked) {
                            Text("❌ आपका चयन", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                }
            }

            // Toggle Explanation Strip
            Surface(
                onClick = { showSolution = !showSolution },
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (showSolution) "व्याख्या व हल छुपाएं ▲" else "💡 सही व्याख्या व हल देखें ▼",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = showSolution) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFFBEB))
                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = if (item.explanation.isNotBlank()) item.explanation else "सही उत्तर विकल्प ${('A' + item.correctIndex)} है।",
                        fontSize = 11.5.sp,
                        color = Color(0xFF92400E),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
