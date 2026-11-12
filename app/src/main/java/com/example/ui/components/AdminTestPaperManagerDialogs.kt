package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import com.example.model.*

@Composable
fun AdminEditTestPaperDialog(
    testPaper: com.example.model.TestPaper,
    testCategory: String,
    subExamId: String,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var testTitle by remember { mutableStateOf(testPaper.title) }
    var testDesc by remember { mutableStateOf("") }
    var durationMinutes by remember { mutableIntStateOf(testPaper.durationMinutes) }
    var difficulty by remember { mutableStateOf(testPaper.difficulty) }
    var selectedYear by remember { mutableStateOf("2025") }

    val currentQuestions = remember { mutableStateListOf<QuizQuestion>().apply { addAll(testPaper.questions) } }

    var addQuestionMode by remember { mutableIntStateOf(0) } // 0: Manual Input, 1: AI Auto Generate
    var newQText by remember { mutableStateOf("") }
    var newOpt1 by remember { mutableStateOf("") }
    var newOpt2 by remember { mutableStateOf("") }
    var newOpt3 by remember { mutableStateOf("") }
    var newOpt4 by remember { mutableStateOf("") }
    var newCorrectIdx by remember { mutableIntStateOf(0) }
    var newExplanation by remember { mutableStateOf("") }
    var aiGenCount by remember { mutableIntStateOf(5) }
    var isAiGeneratingQuestions by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }

    var editingQuestionIndexInChapter by remember { mutableStateOf<Int?>(null) }
    var inlineEditingIndex by remember { mutableStateOf<Int?>(null) }

    editingQuestionIndexInChapter?.let { editIdx ->
        if (editIdx in currentQuestions.indices) {
            val qToEdit = currentQuestions[editIdx]
            com.example.ui.screens.QuestionEditDialog(
                question = qToEdit,
                index = editIdx,
                onDismiss = { editingQuestionIndexInChapter = null },
                onSave = { updatedQ ->
                    currentQuestions[editIdx] = updatedQ
                    editingQuestionIndexInChapter = null
                    Toast.makeText(context, "✅ प्रश्न ${editIdx + 1} सुरक्षित हो गया!", Toast.LENGTH_SHORT).show()
                },
                onDelete = {
                    currentQuestions.removeAt(editIdx)
                    editingQuestionIndexInChapter = null
                    Toast.makeText(context, "🗑️ प्रश्न ${editIdx + 1} हटा दिया गया", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF1F5F9)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(color = Color.White, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1E293B))
                            }
                            Text(
                                text = "टेस्ट संपादित करें",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE0E7FF),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = "${currentQuestions.size} प्रश्न",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Test Title & Desc
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = testTitle,
                                    onValueChange = { testTitle = it },
                                    label = { Text("टेस्ट का शीर्षक (Title)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("समय (मिनट):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf(15, 30, 60, 90).forEach { mins ->
                                                val isSel = durationMinutes == mins
                                                Surface(
                                                    onClick = { durationMinutes = mins },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSel) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                                    border = if (!isSel) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)) else null
                                                ) {
                                                    Text(
                                                        text = "$mins",
                                                        fontSize = 11.sp,
                                                        color = if (isSel) Color.White else Color(0xFF475569),
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("कठिनाई:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf("सरल", "मध्यम", "कठिन").forEach { diff ->
                                                val isSel = difficulty == diff
                                                Surface(
                                                    onClick = { difficulty = diff },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSel) Color(0xFF0F766E) else Color(0xFFF1F5F9),
                                                    border = if (!isSel) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)) else null
                                                ) {
                                                    Text(
                                                        text = diff,
                                                        fontSize = 11.sp,
                                                        color = if (isSel) Color.White else Color(0xFF475569),
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Existing Questions List
                    itemsIndexed(currentQuestions) { idx, q ->
                        val isEditingInline = inlineEditingIndex == idx
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "Q${idx + 1}. ${q.question}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF334155),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = { editingQuestionIndexInChapter = idx },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Full", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                currentQuestions.removeAt(idx)
                                                Toast.makeText(context, "🗑️ प्रश्न हटाया गया", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Save Action
                Surface(color = Color.White, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text("रद्द करें")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    isSaving = true
                                    com.example.model.DynamicTestRepository.deleteCustomTest(testPaper.id, context)
                                    
                                    val newTestPaper = testPaper.copy(
                                        title = testTitle.trim(),
                                        durationMinutes = durationMinutes,
                                        difficulty = difficulty,
                                        year = selectedYear,
                                        questions = currentQuestions.toList(),
                                        totalQuestions = currentQuestions.size,
                                        totalMarks = currentQuestions.size
                                    )
                                    
                                    com.example.model.DynamicTestRepository.addCustomTest(
                                        categoryId = subExamId,
                                        subExamId = subExamId,
                                        postType = testCategory,
                                        year = selectedYear,
                                        testType = "FULL_TEST",
                                        testPaper = newTestPaper,
                                        context = context
                                    )
                                    
                                    // Invalidate stale local cache and publish edited questions to GitHub
                                    try {
                                        com.example.service.OnlineTestSyncManager.onTestEdited(
                                            context = context,
                                            testId = testPaper.id,
                                            testTitle = newTestPaper.title,
                                            subExamId = subExamId,
                                            isChapterWise = false,
                                            updatedQuestions = newTestPaper.questions
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                    isSaving = false
                                    Toast.makeText(context, "✅ टेस्ट सफलतापूर्वक अपडेट और GitHub पर सिंक हो गया!", Toast.LENGTH_SHORT).show()
                                    onSaved()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                            enabled = !isSaving && testTitle.isNotBlank() && currentQuestions.isNotEmpty(),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(44.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("सेव हो रहा है...", fontSize = 14.sp)
                            } else {
                                Text("टेस्ट अपडेट करें", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
