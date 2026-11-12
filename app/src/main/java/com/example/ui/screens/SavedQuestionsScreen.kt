package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.QuizQuestion
import com.example.model.SavedQuestionEntity
import com.example.model.SavedQuestionsRepository
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.ScreenBackground
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThemeManager
import com.example.ui.util.TextFormatHelper
import kotlinx.coroutines.launch

@Composable
fun SavedQuestionsDialog(
    onDismiss: () -> Unit,
    onStartTest: (String, List<QuizQuestion>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = ThemeManager.isDarkMode

    LaunchedEffect(Unit) {
        SavedQuestionsRepository.initRepository(context, scope)
    }

    val savedQuestions = SavedQuestionsRepository.getReactiveSavedQuestions()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubjectFilter by remember { mutableStateOf("सभी विषय (All)") }
    val expandedItems = remember { mutableStateMapOf<Int, Boolean>() }

    // Distinct subjects from saved questions
    val subjectsList = remember(savedQuestions.size) {
        val unique = savedQuestions.map { it.subject }.distinct().filter { it.isNotBlank() }
        listOf("सभी विषय (All)") + unique
    }

    val filteredQuestions = remember(savedQuestions.size, searchQuery, selectedSubjectFilter) {
        savedQuestions.filter { item ->
            val matchesSubject = selectedSubjectFilter == "सभी विषय (All)" || item.subject.equals(selectedSubjectFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    item.questionText.contains(searchQuery, ignoreCase = true) ||
                    item.subject.contains(searchQuery, ignoreCase = true) ||
                    item.explanation.contains(searchQuery, ignoreCase = true)
            matchesSubject && matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground),
            color = ScreenBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Header Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SurfaceCard,
                    shadowElevation = if (isDark) 0.dp else 3.dp,
                    border = if (isDark) BorderStroke(1.dp, CardBorderColor) else null
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = null,
                                            tint = Color(0xFFEAB308),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Saved Questions",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = "${savedQuestions.size} प्रश्न सुरक्षित",
                                        fontSize = 11.5.sp,
                                        color = TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Start Practice Test Button (if saved questions available)
                            if (filteredQuestions.isNotEmpty()) {
                                Surface(
                                    onClick = {
                                        val quizList = filteredQuestions.map { SavedQuestionsRepository.entityToQuizQuestion(it) }
                                        val title = if (selectedSubjectFilter == "सभी विषय (All)") "Saved Questions Practice Test" else "$selectedSubjectFilter - Saved Practice"
                                        onStartTest(title, quizList)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF2563EB),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("start_saved_test_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "टेस्ट दें (${filteredQuestions.size})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("प्रश्न या विषय खोजें...", fontSize = 13.sp, color = TextSecondary) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = TextPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            ),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                unfocusedContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                cursorColor = Color(0xFF3B82F6),
                                focusedBorderColor = Color(0xFF3B82F6),
                                unfocusedBorderColor = CardBorderColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Subject Filter Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(subjectsList) { subject ->
                                val isSelected = selectedSubjectFilter == subject
                                Surface(
                                    onClick = { selectedSubjectFilter = subject },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Color(0xFF0284C7) else (if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF0284C7) else CardBorderColor),
                                    modifier = Modifier.testTag("filter_subject_$subject")
                                ) {
                                    Text(
                                        text = subject,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Content List
                if (filteredQuestions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                color = if (isDark) Color(0xFF78350F).copy(alpha = 0.4f) else Color(0xFFFEF3C7)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (savedQuestions.isEmpty()) "कोई सेव किया गया प्रश्न नहीं है" else "कोई प्रश्न नहीं मिला",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (savedQuestions.isEmpty())
                                    "टेस्ट पूरा करने के बाद 'View Solutions' में जाकर किसी भी प्रश्न को सेव करने के लिए 'Bookmark / Save' बटन दबाएं।"
                                else "कृपया खोज शब्द या विषय फ़िल्टर बदलें।",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(filteredQuestions, key = { _, item -> "${item.id}_${item.questionText}" }) { index, item ->
                            val isExpanded = expandedItems[item.id] ?: false

                            SavedQuestionCard(
                                questionNumber = index + 1,
                                item = item,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    expandedItems[item.id] = !isExpanded
                                },
                                onDelete = {
                                    SavedQuestionsRepository.deleteById(context, item.id, scope)
                                    Toast.makeText(context, "प्रश्न हटा दिया गया", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedQuestionCard(
    questionNumber: Int,
    item: SavedQuestionEntity,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = ThemeManager.isDarkMode
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_question_card_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.5.dp),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Subject Pill, Exam Tag & Delete
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
                            .background(if (isDark) Color(0xFF0369A1).copy(alpha = 0.35f) else Color(0xFFE0F2FE))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.subject,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
                        )
                    }

                    if (item.examTitle.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = item.examTitle,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Question Text
            Text(
                text = parseSimpleMarkdownSaved("Q$questionNumber. ${item.questionText}"),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Options List
            val options = listOf(
                "A" to item.optionA,
                "B" to item.optionB,
                "C" to item.optionC,
                "D" to item.optionD
            ).filter { it.second.isNotBlank() }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                options.forEachIndexed { optIdx, (label, optText) ->
                    val isCorrect = optIdx == item.correctIndex

                    val optBgColor = if (isCorrect) {
                        if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFDCFCE7)
                    } else {
                        if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                    }
                    val optBorderColor = if (isCorrect) {
                        if (isDark) Color(0xFF10B981) else Color(0xFF86EFAC)
                    } else {
                        CardBorderColor
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = optBgColor,
                        border = BorderStroke(1.dp, optBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$label.",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrect) (if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D)) else TextSecondary
                                )
                                Text(
                                    text = TextFormatHelper.parseMarkdown(optText),
                                    fontSize = 13.sp,
                                    fontWeight = if (isCorrect) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isCorrect) (if (isDark) Color(0xFFD1FAE5) else Color(0xFF14532D)) else TextPrimary
                                )
                            }

                            if (isCorrect) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Correct",
                                        tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "सही उत्तर",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Explanation section
            if (item.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF4ADE80) else Color(0xFF16A34A),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "व्याख्या (Solution Explanation):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.explanation,
                            fontSize = 12.5.sp,
                            color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF1F2937),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}


fun parseSimpleMarkdownSaved(text: String): AnnotatedString {
    return TextFormatHelper.parseMarkdown(text)
}
