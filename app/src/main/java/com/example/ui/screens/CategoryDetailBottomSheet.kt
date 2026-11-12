package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.model.CustomChapterRepository
import com.example.service.AuthManager
import com.example.ui.screens.AdminAiTestCreatorDialog
import com.example.ui.components.AdminAddSubjectDialog
import com.example.ui.components.AdminEditChapterQuestionsDialog
import com.example.ui.components.AdminEditTestPaperDialog
import com.example.model.ChapterItem
import com.example.model.DynamicTestRepository
import com.example.model.ExamCategory
import com.example.model.ExamChapterManager
import com.example.model.ExamSubjectCategory
import com.example.model.ExamSubjectManager
import com.example.model.QuizQuestion
import com.example.model.SubExam
import com.example.model.TestPaper
import com.example.model.TestHistoryRepository
import com.example.model.UnlockedTestsRepository
import com.example.model.availableExamYears
import com.example.model.sampleQuizQuestions
import com.example.service.OnlineTestSyncManager
import com.example.service.GitHubSyncService
import com.example.ui.components.GitHubConfigDialog
import com.example.ui.theme.CardBorderColor
import com.example.ui.theme.ScreenBackground
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ThemeManager
import kotlinx.coroutines.launch

enum class PolicePostType {
    CONSTABLE,
    HEAD_CONSTABLE,
    SI
}

fun isTierExam(subExamId: String): Boolean {
    val id = subExamId.lowercase().trim()
    return id in setOf("ssc_cgl", "ssc_chsl", "ssc_cpo", "rrb_ntpc", "rrb_alp", "rrb_je")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailBottomSheet(
    category: ExamCategory,
    onDismiss: () -> Unit,
    onStartTest: (title: String, questions: List<QuizQuestion>?, durationMinutes: Int?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isAdmin = AuthManager.isAdmin()
    val isDark = ThemeManager.isDarkMode
    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }
    var testPaperToDelete by remember { mutableStateOf<com.example.model.TestPaper?>(null) }

    // Tracks which exam button is currently clicked to show its options
    var expandedExamId by remember { mutableStateOf<String?>(null) }
    // When "Full Test" is clicked under Delhi Police or any sub-exam, show test selection papers
    var selectedSubExamForTests by remember { mutableStateOf<SubExam?>(null) }
    var selectedTestType by remember { mutableStateOf("FULL_TEST") } // "FULL_TEST" or "CHAPTER_WISE"
    // Selected post type: Constable (default) or Head Constable or SI
    var selectedPostType by remember { mutableStateOf(PolicePostType.CONSTABLE) }
    // Selected Tier Stage for exams with Tier 1 and Tier 2: TIER_1 (default) or TIER_2
    var selectedTierStage by remember { mutableStateOf("TIER_1") }
    // Selected year: सभी (All, default), 2025, 2024, 2023, 2022, 2021, 2020
    var selectedExamYear by remember { mutableStateOf("सभी") }
    var showAddFullTestDialog by remember { mutableStateOf(false) }
    var isManualSyncing by remember { mutableStateOf(false) }

    // Reset year filter to 'सभी' (All) when user selects or changes exam so all tests are immediately visible
    LaunchedEffect(selectedSubExamForTests?.id) {
        if (selectedSubExamForTests != null) {
            selectedExamYear = "सभी"
            if (com.example.model.DynamicTestRepository.customTests.isEmpty()) {
                com.example.service.OnlineTestSyncManager.autoSyncIfOnline(context)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
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
                // Top Header Bar - Clean, Beautiful, Easy to Touch & Close
                Surface(
                    color = SurfaceCard,
                    shadowElevation = if (isDark) 0.dp else 1.dp,
                    border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor) else null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (selectedSubExamForTests != null) {
                            IconButton(
                                onClick = { selectedSubExamForTests = null },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "पीछे जाएं",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column {
                            Text(
                                text = selectedSubExamForTests?.name ?: category.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                letterSpacing = 0.2.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (selectedSubExamForTests != null) {
                                    if (selectedTestType == "FULL_TEST") {
                                        if (isTierExam(selectedSubExamForTests!!.id)) {
                                            val isRailway = selectedSubExamForTests!!.id.startsWith("rrb")
                                            val isCpo = selectedSubExamForTests!!.id == "ssc_cpo"
                                            val tierLabel = when {
                                                isRailway -> if (selectedTierStage == "TIER_1") "CBT 1 (Tier-1)" else "CBT 2 (Tier-2)"
                                                isCpo -> if (selectedTierStage == "TIER_1") "Paper 1 (Tier-1)" else "Paper 2 (Tier-2)"
                                                else -> if (selectedTierStage == "TIER_1") "Tier 1 (टियर-1)" else "Tier 2 (टियर-2)"
                                            }
                                            "फुल मॉक टेस्ट्स • $tierLabel"
                                        } else {
                                            "फुल मॉक टेस्ट्स"
                                        }
                                    } else "अध्याय-वार टेस्ट्स"
                                } else {
                                    "मॉक टेस्ट या अध्याय-वार टेस्ट चुनें"
                                },
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        // Prominent Close Button (Comfortable 44dp Touch Target)
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9))
                                .testTag("close_category_sheet_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "बंद करें",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            val currentSubExam = selectedSubExamForTests
            if (currentSubExam != null) {
                if (selectedTestType == "CHAPTER_WISE") {
                    ChapterWiseSelectionScreen(
                        subExam = currentSubExam,
                        onStartTest = onStartTest
                    )
                } else {
                    val isPoliceExam = currentSubExam.id == "ssc_delhi_police"

                    if (isPoliceExam) {
                        selectedPostType = PolicePostType.CONSTABLE
                        // Years Selector for Delhi Police Constable
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Combined available years including dynamic ones
                            val dynamicYears = DynamicTestRepository.getDynamicYearsForSubExam(currentSubExam.id, selectedPostType.name)
                            val combinedYears = (listOf("सभी") + dynamicYears + availableExamYears).distinct()

                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("police_year_selector"),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                            ) {
                                items(combinedYears) { year ->
                                    val isYearSelected = selectedExamYear == year
                                    Surface(
                                        onClick = { selectedExamYear = year },
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isYearSelected) (if (isDark) Color(0xFF3B82F6) else Color(0xFF0F172A)) else (if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)),
                                        shadowElevation = if (isYearSelected) 2.dp else 0.dp,
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("year_button_$year")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = year,
                                                fontSize = 13.sp,
                                                fontWeight = if (isYearSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (isYearSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val repoRev = DynamicTestRepository.repositoryRevision
                        val customTestsCount = DynamicTestRepository.customTests.size
                        val currentTestsList = remember(currentSubExam.id, selectedPostType, selectedExamYear, customTestsCount, repoRev) {
                            DynamicTestRepository.getCustomTests(currentSubExam.id, selectedPostType.name, selectedExamYear)
                                .filter { it.questions.isNotEmpty() }
                                .distinctBy { it.id.toString() }
                        }

                        val dynamicTestIds = remember(currentTestsList) {
                            currentTestsList.map { it.id }.toSet()
                        }

                        if (currentTestsList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = null,
                                                tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "कोई टेस्ट उपलब्ध नहीं है",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF1E293B)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$selectedExamYear के लिए अभी कोई टेस्ट उपलब्ध नहीं है।",
                                        fontSize = 13.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            if (!isManualSyncing) {
                                                isManualSyncing = true
                                                com.example.service.OnlineTestSyncManager.forceSyncNow(context) { _, msg ->
                                                    isManualSyncing = false
                                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2563EB)
                                        )
                                    ) {
                                        if (isManualSyncing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("सिंक हो रहा है...", color = Color.White, fontSize = 13.sp)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("🔄 टेस्ट सिंक करें", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    items = currentTestsList,
                                    key = { _, testPaper -> testPaper.id }
                                ) { index, testPaper ->
                                    val isDynamic = dynamicTestIds.contains(testPaper.id)
                                    DelhiPoliceTestCard(
                                        testPaper = testPaper,
                                        index = index,
                                        isDynamic = isDynamic,
                                        isAdmin = isAdmin,
                                        onDeleteCustom = {
                                            testPaperToDelete = testPaper
                                        },
                                        onEditTest = {
                                            testPaperToEdit = testPaper
                                        },
                                        onClick = {
                                            onStartTest(testPaper.title, testPaper.questions, testPaper.durationMinutes)
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // For other sub-exams (SSC CGL, RRB NTPC, SSC CHSL, RRB ALP, RRB JE, SSC GD, etc.)
                        val isTierExam = isTierExam(currentSubExam.id)

                        // 1. If exam has Tier 1 / Tier 2, show Tier selector tabs at the very top (sabse upar)
                        if (isTierExam) {
                            val isRailway = currentSubExam.id.startsWith("rrb")
                            val isCpo = currentSubExam.id == "ssc_cpo"
                            val stage1Title = when {
                                isRailway -> "CBT 1 (Tier 1)"
                                isCpo -> "Paper 1 (Tier 1)"
                                else -> "Tier 1 (टियर-1)"
                            }
                            val stage2Title = when {
                                isRailway -> "CBT 2 (Tier 2)"
                                isCpo -> "Paper 2 (Tier 2)"
                                else -> "Tier 2 (टियर-2)"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(
                                    "TIER_1" to stage1Title,
                                    "TIER_2" to stage2Title
                                ).forEach { (stageKey, title) ->
                                    val isSelected = selectedTierStage == stageKey
                                    Surface(
                                        onClick = { selectedTierStage = stageKey },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) (if (isDark) Color(0xFF2563EB) else Color(0xFF0F172A)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) (if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)) else (if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                                        ),
                                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("tier_tab_$stageKey")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color.White.copy(alpha = 0.22f) else (if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (stageKey == "TIER_1") "1" else "2",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isSelected) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = title,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (isSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Year Selector
                        val dynamicYears = DynamicTestRepository.getDynamicYearsForSubExam(currentSubExam.id, if (isTierExam) selectedTierStage else "")
                        val combinedYears = (listOf("सभी") + dynamicYears + availableExamYears).distinct()

                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .testTag("subexam_year_selector"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            items(combinedYears) { year ->
                                val isYearSelected = selectedExamYear == year
                                Surface(
                                    onClick = { selectedExamYear = year },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isYearSelected) (if (isDark) Color(0xFF3B82F6) else Color(0xFF0F172A)) else (if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)),
                                    shadowElevation = if (isYearSelected) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("year_button_$year")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = year,
                                            fontSize = 13.sp,
                                            fontWeight = if (isYearSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (isYearSelected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Tests filtering by Tier stage and Year
                        val repoRevTier = DynamicTestRepository.repositoryRevision
                        val customTestsCount = DynamicTestRepository.customTests.size
                        val (allPapers, dynamicPaperIds) = remember(currentSubExam.id, selectedTierStage, selectedExamYear, customTestsCount, repoRevTier) {
                            val dynamicExamTests = DynamicTestRepository.getAllTestsForSubExam(currentSubExam.id, "FULL_TEST")
                            val filtered = dynamicExamTests.filter { entry ->
                                val paper = entry.testPaper
                                if (paper.questions.isEmpty()) return@filter false

                                val matchesTier = if (isTierExam) {
                                    val post = entry.postType.uppercase().trim()
                                    val title = paper.title.lowercase()
                                    val id = paper.id.lowercase()
                                    val isStage2 = post == "TIER_2" || post == "TIER2" || post == "CBT_2" || post == "CBT2" || post == "PAPER_2" ||
                                            title.contains("tier 2") || title.contains("tier-2") || title.contains("tier2") ||
                                            title.contains("टियर 2") || title.contains("टियर-2") || title.contains("टियर2") ||
                                            title.contains("cbt 2") || title.contains("cbt-2") || title.contains("cbt2") ||
                                            title.contains("paper 2") || title.contains("paper-2") || title.contains("पेपर 2") ||
                                            id.contains("tier_2") || id.contains("tier2") || id.contains("cbt_2") || id.contains("cbt2")
                                    if (selectedTierStage == "TIER_2") isStage2 else !isStage2
                                } else true

                                val matchesYear = if (selectedExamYear == "सभी") true else {
                                    entry.year.isBlank() || entry.year.equals("ALL", true) || entry.year == selectedExamYear || paper.year == selectedExamYear || paper.title.contains(selectedExamYear)
                                }

                                matchesTier && matchesYear
                            }
                            val papers = filtered.map { it.testPaper }.distinctBy { it.id.toString() }
                            val dynIds = papers.map { it.id }.toSet()
                            Pair(papers, dynIds)
                        }

                        if (allPapers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                                contentDescription = null,
                                                tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "कोई टेस्ट उपलब्ध नहीं है",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color.White else Color(0xFF1E293B)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val emptyTierMessage = if (isTierExam) {
                                        val tierLabel = if (selectedTierStage == "TIER_1") "Tier-1" else "Tier-2"
                                        "इस परीक्षा के $tierLabel ($selectedExamYear) के लिए अभी कोई फुल टेस्ट नहीं है।"
                                    } else {
                                        "इस परीक्षा ($selectedExamYear) के लिए अभी कोई फुल टेस्ट नहीं है।"
                                    }
                                    Text(
                                        text = emptyTierMessage,
                                        fontSize = 13.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            if (!isManualSyncing) {
                                                isManualSyncing = true
                                                com.example.service.OnlineTestSyncManager.forceSyncNow(context) { _, msg ->
                                                    isManualSyncing = false
                                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF2563EB)
                                        )
                                    ) {
                                        if (isManualSyncing) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("सिंक हो रहा है...", color = Color.White, fontSize = 13.sp)
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("🔄 टेस्ट सिंक करें", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                contentPadding = PaddingValues(top = 14.dp, bottom = 40.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    items = allPapers,
                                    key = { _, testPaper -> testPaper.id }
                                ) { index, testPaper ->
                                    val isDynamic = dynamicPaperIds.contains(testPaper.id)
                                    DelhiPoliceTestCard(
                                        testPaper = testPaper,
                                        index = index,
                                        isDynamic = isDynamic,
                                        isAdmin = isAdmin,
                                        onDeleteCustom = {
                                            testPaperToDelete = testPaper
                                        },
                                        onEditTest = {
                                            testPaperToEdit = testPaper
                                        },
                                        onClick = {
                                            onStartTest(testPaper.title, testPaper.questions, testPaper.durationMinutes)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Sub-exams list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(
                        items = category.subExams,
                        key = { _, subExam -> subExam.id }
                    ) { index, subExam ->
                        val isExpanded = expandedExamId == subExam.id

                        ExamItemCard(
                            subExam = subExam,
                            index = index,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedExamId = if (isExpanded) null else subExam.id
                                com.example.model.RealtimeExamStatsManager.recordAspirantActivity(context, subExam.id, subExam.name)
                            },
                            onFullTestClick = {
                                com.example.model.RealtimeExamStatsManager.recordAspirantActivity(context, subExam.id, subExam.name)
                                selectedSubExamForTests = subExam
                                selectedTestType = "FULL_TEST"
                                selectedTierStage = "TIER_1"
                                selectedExamYear = "सभी"
                                val isPoliceExam = subExam.id == "ssc_delhi_police" || subExam.id == "up_police" || subExam.id == "rajasthan_police" || subExam.id == "raj_cet"
                                if (isPoliceExam) {
                                    selectedPostType = PolicePostType.CONSTABLE
                                }
                            },
                            onChapterWiseClick = {
                                com.example.model.RealtimeExamStatsManager.recordAspirantActivity(context, subExam.id, subExam.name)
                                selectedSubExamForTests = subExam
                                selectedTestType = "CHAPTER_WISE"
                            }
                        )
                    }
                }
            }
        }
    }

    if (testPaperToEdit != null) {
        val entry = com.example.model.DynamicTestRepository.customTests.find { it.testPaper.id == testPaperToEdit!!.id }
        if (entry != null) {
            com.example.ui.components.AdminEditTestPaperDialog(
                testPaper = testPaperToEdit!!,
                testCategory = entry.postType,
                subExamId = entry.subExamId,
                onDismiss = { testPaperToEdit = null },
                onSaved = { testPaperToEdit = null }
            )
        } else {
            testPaperToEdit = null
        }
    }

    // Confirmation dialog before deleting full test
    if (testPaperToDelete != null) {
        val targetTest = testPaperToDelete!!
        AlertDialog(
            onDismissRequest = { testPaperToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "टेस्ट हटाएं? (Delete Test)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "क्या आप वाकई टेस्ट '${targetTest.title}' को हटाना चाहते हैं?\n\nयह टेस्ट ऐप और ऑनलाइन क्लाउड दोनों से हटा दिया जाएगा। यह क्रिया वापस नहीं ली जा सकती।",
                    fontSize = 13.5.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = targetTest
                        testPaperToDelete = null
                        val questionsToDelete = toDel.questions
                        com.example.model.DynamicTestRepository.deleteCustomTest(toDel.id.toString(), context)
                        android.widget.Toast.makeText(context, "टेस्ट '${toDel.title}' ऐप से हटाया गया। GitHub सिंक हो रहा है...", android.widget.Toast.LENGTH_LONG).show()
                        kotlinx.coroutines.GlobalScope.launch {
                            com.example.service.OnlineTestSyncManager.deleteTestAssetsFromCloud(context, questionsToDelete)
                            kotlinx.coroutines.Dispatchers.Main.dispatch(kotlin.coroutines.EmptyCoroutineContext) {
                                android.widget.Toast.makeText(context, "✅ '${toDel.title}' ऐप और GitHub (Images + JSON) दोनों से डिलीट हो गया!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("हाँ, हटाएं (OK)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { testPaperToDelete = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("रद्द करें")
                }
            }
        )
    }

    if (showAddFullTestDialog && selectedSubExamForTests != null) {
        val initialPost = if (isTierExam(selectedSubExamForTests!!.id)) selectedTierStage else "CONSTABLE"
        AdminAiTestCreatorDialog(
            onDismiss = { showAddFullTestDialog = false },
            initialMode = "FULL_TEST",
            initialSubExamId = selectedSubExamForTests!!.id,
            initialSubjectKey = "",
            initialChapterTitle = "",
            initialPostType = initialPost
        )
    }
}
}

/**
 * Individual test card with live cloud sync dynamic badge support
 */
@Composable
fun DelhiPoliceTestCard(
    testPaper: TestPaper,
    index: Int,
    isDynamic: Boolean = false,
    isAdmin: Boolean = false,
    onDeleteCustom: (() -> Unit)? = null,
    onEditTest: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val isDark = ThemeManager.isDarkMode

    val stats = remember(testPaper.id, testPaper.title) {
        TestHistoryRepository.getStats(testPaper.id, testPaper.title)
    }
    val isAttempted = stats != null && stats.attemptsCount > 0
    val isUnlocked = remember(testPaper.id, testPaper.title, isAttempted) {
        UnlockedTestsRepository.isTestUnlocked(testPaper.id, testPaper.title) || isAttempted
    }

    val gradientColors = remember(index, isAttempted, isUnlocked) {
        if (isAttempted) {
            listOf(Color(0xFF059669), Color(0xFF34D399))
        } else if (isUnlocked) {
            listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
        } else {
            when (index % 3) {
                0 -> listOf(Color(0xFF2563EB), Color(0xFF3B82F6))
                1 -> listOf(Color(0xFF0891B2), Color(0xFF06B6D4))
                else -> listOf(Color(0xFF4F46E5), Color(0xFF6366F1))
            }
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = if (isDark) 0.dp else 2.dp,
        color = if (isAttempted) (if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFF0FDF4)) else SurfaceCard,
        border = if (isAttempted) androidx.compose.foundation.BorderStroke(1.2.dp, if (isDark) Color(0xFF10B981) else Color(0xFF86EFAC)) else if (isUnlocked) androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF0284C7) else Color(0xFFBAE6FD)) else androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dp_test_${testPaper.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Test Badge + Offline / Attempt Badge + Difficulty
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // Test badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isAttempted) (if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)) else (if (isDark) Color(0xFF0369A1).copy(alpha = 0.35f) else Color(0xFFE0F2FE)))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "TEST ${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAttempted) (if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)) else (if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1))
                        )
                    }

                    if (isUnlocked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "⚡ ऑफ़लाइन",
                                fontSize = 10.sp,
                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF059669),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                val cleanDifficulty = remember(testPaper.difficulty) {
                    if (testPaper.difficulty.contains("AI", ignoreCase = true) ||
                        testPaper.difficulty.contains("जेनरेटेड") ||
                        testPaper.difficulty.contains("2025") ||
                        testPaper.difficulty.contains("ऑनलाइन लाइव")
                    ) {
                        "ऑफिशियल पैटर्न"
                    } else {
                        testPaper.difficulty.ifBlank { "ऑफिशियल पैटर्न" }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cleanDifficulty,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    if (isAdmin && isDynamic) {
                        if (onEditTest != null) {
                            IconButton(onClick = onEditTest, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                                Icon(Icons.Default.Edit, "Edit Test", tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                            }
                        }
                        if (onDeleteCustom != null) {
                            IconButton(onClick = onDeleteCustom, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                                Icon(Icons.Default.DeleteOutline, "Delete Test", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = testPaper.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 20.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Info items (Questions, Duration, Marks)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${testPaper.questions.size} Qs (${testPaper.totalMarks} Marks)",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${testPaper.durationMinutes} Mins",
                        fontSize = 11.5.sp,
                        color = TextSecondary
                    )
                }
            }

            // Attempted Status & High Score Box (Compact, Adaptive & Clean)
            if (stats != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFFBBF7D0)),
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFEAB308),
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "Best Score:",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${String.format("%.2f", stats.highestScore)} / ${testPaper.totalMarks}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF166534),
                                maxLines = 1
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF78350F).copy(alpha = 0.4f) else Color(0xFFFEF3C7))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Attempt #${stats.attemptsCount}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Button: Clean, perfectly sized 40dp button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isAttempted) Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAttempted) "Re-Attempt Test (पुनः दें)" else "Start Test Now (टेस्ट शुरू करें)",
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Exam item card:
 * Tapping the exam button toggles open "Full Test" and "Chapter Wise" buttons.
 */
@Composable
fun ExamItemCard(
    subExam: SubExam,
    index: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onFullTestClick: () -> Unit,
    onChapterWiseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sub_exam_card_${subExam.id}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SimpleExamButton(
            subExam = subExam,
            index = index,
            isExpanded = isExpanded,
            onClick = onToggleExpand
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isTier = isTierExam(subExam.id)
                val fullTestTitle = when {
                    subExam.id == "ssc_delhi_police" -> "Full Tests (Constable)"
                    isTier && subExam.id.startsWith("rrb") -> "Full Tests (CBT 1 / CBT 2)"
                    isTier -> "Full Tests (Tier 1 / Tier 2)"
                    else -> "Full Test (मॉक टेस्ट)"
                }
                TestTypeButton(
                    title = fullTestTitle,
                    icon = Icons.Default.PlayArrow,
                    gradient = listOf(Color(0xFF0284C7), Color(0xFF38BDF8)),
                    testTag = "full_test_${subExam.id}",
                    modifier = Modifier.weight(1f),
                    onClick = onFullTestClick
                )

                TestTypeButton(
                    title = "Chapter Wise (अध्याय)",
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    gradient = listOf(Color(0xFF059669), Color(0xFF34D399)),
                    testTag = "chapter_wise_${subExam.id}",
                    modifier = Modifier.weight(1f),
                    onClick = onChapterWiseClick
                )
            }
        }
    }
}

@Composable
fun SimpleExamButton(
    subExam: SubExam,
    index: Int,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val isDark = ThemeManager.isDarkMode
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        label = "simple_btn_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (isDark) 0.dp else (if (isExpanded) 4.dp else 2.dp),
        color = SurfaceCard,
        border = if (isExpanded) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF3B82F6)) else androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .testTag("exam_item_${subExam.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = subExam.name,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val currentCtx = LocalContext.current
                    val rev = CustomChapterRepository.revision
                    val dynRev = DynamicTestRepository.repositoryRevision
                    val customTestsSize = DynamicTestRepository.customTests.size
                    val customChaptersSize = CustomChapterRepository.customChapters.size
                    val realTestsCount = remember(subExam.id, subExam.name, rev, dynRev, customTestsSize, customChaptersSize) {
                        com.example.model.RealtimeExamStatsManager.getRealTestCount(subExam.id, subExam.name, currentCtx)
                    }
                    val testsText = com.example.model.RealtimeExamStatsManager.formatTests(realTestsCount)
                    Text(
                        text = testsText,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = if (isExpanded) (if (isDark) Color(0xFF1E3A8A) else Color(0xFFEFF6FF)) else (if (isDark) Color(0xFF334155) else Color(0xFFF8FAFC))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = if (isExpanded) Color(0xFF3B82F6) else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TestTypeButton(
    title: String,
    icon: ImageVector,
    gradient: List<Color>,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        label = "test_type_btn_scale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        modifier = modifier
            .height(46.dp)
            .scale(scale)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.horizontalGradient(gradient))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

private fun getCompactSubjectLabel(subject: ExamSubjectCategory): String {
    return when (subject.key.uppercase().trim()) {
        "GK", "GK_GS", "GS", "GENERAL_KNOWLEDGE" -> "GK/GS"
        "REASONING", "REASON" -> "रीजनिंग"
        "MATHS", "MATH", "QUANT" -> "गणित"
        "COMPUTER", "COMP" -> "कंप्यूटर"
        "HINDI" -> "हिंदी"
        "ENGLISH", "ENG" -> "English"
        "SCIENCE", "SCI" -> "विज्ञान"
        else -> {
            val clean = subject.name
                .split("(", "/", "•", "–", "-")
                .firstOrNull()?.trim() ?: subject.name
            if (clean.length > 7) clean.take(7) else clean
        }
    }
}

@Composable
fun ChapterWiseSelectionScreen(
    subExam: SubExam,
    onStartTest: (title: String, questions: List<QuizQuestion>?, durationMinutes: Int?) -> Unit
) {
    val context = LocalContext.current
    val isAdmin = AuthManager.isAdmin()

    LaunchedEffect(subExam.id) {
        CustomChapterRepository.init(context)
    }

    val repoRevision = CustomChapterRepository.revision

    val subjects = remember(subExam.id, repoRevision) {
        CustomChapterRepository.getFullSubjectsForExam(subExam.id, context)
    }

    var selectedSubjectKey by remember(subExam.id) {
        mutableStateOf(subjects.firstOrNull()?.key ?: "")
    }

    // Ensure selectedSubjectKey is valid and synchronized
    LaunchedEffect(subjects) {
        if (subjects.isNotEmpty() && subjects.none { it.key.equals(selectedSubjectKey, ignoreCase = true) }) {
            selectedSubjectKey = subjects.first().key
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedPyqMode by remember { mutableStateOf("CHAPTER_WISE") } // Default to Chapter-Wise so user sees their chapters & sets
    var selectedSubjectYear by remember { mutableStateOf("सभी") }
    var selectedChapterYear by remember { mutableStateOf("सभी") }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var initialTopicTitleForAdd by remember { mutableStateOf("") }
    var subjectToDelete by remember { mutableStateOf<ExamSubjectCategory?>(null) }
    var chapterToEdit by remember { mutableStateOf<ChapterItem?>(null) }
    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }
    var testPaperToDelete by remember { mutableStateOf<com.example.model.TestPaper?>(null) }
    var chapterToDelete by remember { mutableStateOf<ChapterItem?>(null) }
    var chapterGroupToDelete by remember { mutableStateOf<Pair<String, List<ChapterItem>>?>(null) }

    val currentSubject = subjects.find { it.key.equals(selectedSubjectKey, ignoreCase = true) } ?: subjects.firstOrNull()

    // Dynamic tests created specifically for chapter-wise practice
    val dynamicChapterTests = remember(subExam.id) {
        DynamicTestRepository.getAllTestsForSubExam(subExam.id, "CHAPTER_WISE")
    }

    // Automatically extracted Subject-wise tests from all Full Tests (Official & Dynamic & Answer Keys)
    val subjectWisePyqTests = remember(subExam.id, currentSubject?.key, repoRevision) {
        if (currentSubject != null) {
            CustomChapterRepository.getSubjectWisePyqTests(subExam, currentSubject, context)
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 4.dp)
    ) {
        val isDark = ThemeManager.isDarkMode

        // Top 2-Button Toggle: PYQ Subject-Wise vs PYQ Chapter-Wise
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. PYQ Subject Wise Button
                Surface(
                    onClick = { selectedPyqMode = "SUBJECT_WISE" },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedPyqMode == "SUBJECT_WISE") Color(0xFF2563EB) else Color.Transparent,
                    shadowElevation = if (selectedPyqMode == "SUBJECT_WISE") 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("pyq_subject_wise_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = if (selectedPyqMode == "SUBJECT_WISE") Color.White else TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PYQ विषय-वार",
                            fontSize = 13.sp,
                            fontWeight = if (selectedPyqMode == "SUBJECT_WISE") FontWeight.Bold else FontWeight.SemiBold,
                            color = if (selectedPyqMode == "SUBJECT_WISE") Color.White else TextSecondary
                        )
                    }
                }

                // 2. PYQ Chapter Wise Button
                Surface(
                    onClick = { selectedPyqMode = "CHAPTER_WISE" },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedPyqMode == "CHAPTER_WISE") Color(0xFF2563EB) else Color.Transparent,
                    shadowElevation = if (selectedPyqMode == "CHAPTER_WISE") 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("pyq_chapter_wise_btn")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = if (selectedPyqMode == "CHAPTER_WISE") Color.White else TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PYQ अध्याय-वार",
                            fontSize = 13.sp,
                            fontWeight = if (selectedPyqMode == "CHAPTER_WISE") FontWeight.Bold else FontWeight.SemiBold,
                            color = if (selectedPyqMode == "CHAPTER_WISE") Color.White else TextSecondary
                        )
                    }
                }
            }
        }

        // Admin Top Action Header (in Chapter Wise mode if user is Admin)
        if (isAdmin && selectedPyqMode == "CHAPTER_WISE") {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFBFDBFE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "एडमिन कंट्रोल (Admin Studio)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1E3A8A)
                                )
                                Text(
                                    text = "नया विषय या अध्याय तुरंत जोड़ें",
                                    fontSize = 10.5.sp,
                                    color = if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Delete current custom subject button (if selected subject is custom)
                            val isCurrentSubjectCustom = currentSubject != null && currentSubject.key.startsWith("custom_sub_")
                            if (isCurrentSubjectCustom) {
                                Surface(
                                    onClick = { subjectToDelete = currentSubject },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFFCA5A5)),
                                    modifier = Modifier.testTag("admin_delete_subject_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "विषय हटाएं", tint = Color(0xFFDC2626), modifier = Modifier.size(13.dp))
                                        Text("विषय हटाएं", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626))
                                    }
                                }
                            }
                        }
                    }

                    // Admin Action Buttons (Large, high-contrast, fully visible)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // + Add Subject Button
                        Surface(
                            onClick = { showAddSubjectDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E40AF),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("admin_open_add_subject_btn")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ नया विषय (Subject)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // + Add Chapter Button
                        Surface(
                            onClick = { showAddChapterDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF16A34A),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .testTag("admin_open_add_chapter_btn")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ नया अध्याय (Chapter)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Subject Selector Tabs (All subjects fitted compactly on a single screen without scrolling)
        if (subjects.isNotEmpty()) {
            if (subjects.size <= 5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("chapter_subject_tabs"),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    subjects.forEach { subject ->
                        val isSelected = selectedSubjectKey.equals(subject.key, ignoreCase = true)
                        val isCustom = subject.key.startsWith("custom_sub_")
                        val compactTitle = getCompactSubjectLabel(subject)

                        Surface(
                            onClick = {
                                selectedSubjectKey = subject.key
                                searchQuery = "" // reset search on subject change
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                            shadowElevation = if (isSelected) 1.5.dp else 0.dp,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else if (isCustom) Color(0xFFBFDBFE) else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .testTag("subject_tab_${subject.key}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = subject.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = compactTitle,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isCustom && isAdmin) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Surface(
                                        onClick = { subjectToDelete = subject },
                                        shape = CircleShape,
                                        color = if (isSelected) Color.White.copy(alpha = 0.3f) else (if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)),
                                        modifier = Modifier
                                            .size(15.dp)
                                            .testTag("delete_subject_btn_${subject.key}")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "विषय हटाएं",
                                                tint = if (isSelected) Color.White else Color(0xFFDC2626),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Fallback for > 5 subjects: Compact LazyRow
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("chapter_subject_tabs"),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    contentPadding = PaddingValues(horizontal = 1.dp, vertical = 1.dp)
                ) {
                    items(subjects) { subject ->
                        val isSelected = selectedSubjectKey.equals(subject.key, ignoreCase = true)
                        val isCustom = subject.key.startsWith("custom_sub_")
                        val compactTitle = getCompactSubjectLabel(subject)

                        Surface(
                            onClick = {
                                selectedSubjectKey = subject.key
                                searchQuery = ""
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF2563EB) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                            shadowElevation = if (isSelected) 1.5.dp else 0.dp,
                            border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else if (isCustom) Color(0xFFBFDBFE) else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("subject_tab_${subject.key}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = subject.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else TextSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = compactTitle,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isCustom && isAdmin) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Surface(
                                        onClick = { subjectToDelete = subject },
                                        shape = CircleShape,
                                        color = if (isSelected) Color.White.copy(alpha = 0.3f) else (if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2)),
                                        modifier = Modifier
                                            .size(15.dp)
                                            .testTag("delete_subject_btn_${subject.key}")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "विषय हटाएं",
                                                tint = if (isSelected) Color.White else Color(0xFFDC2626),
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedPyqMode == "SUBJECT_WISE") {
            // =========================================================================
            // MODE 1: PYQ SUBJECT-WISE SCREEN (Auto-Split from Full Tests)
            // =========================================================================
            // Available Years for Subject-wise filter (directly 2025, 2024, 2023, 2022, 2021, 2020)
            val availableYearsForSubject = remember(subjectWisePyqTests) {
                val foundYears = subjectWisePyqTests.map { it.year }.filter { it.isNotBlank() && it != "सभी" }
                (listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020") + foundYears).distinct()
            }

            val filteredSubjectTests = remember(subjectWisePyqTests, searchQuery, selectedSubjectYear) {
                subjectWisePyqTests.filter { testPaper ->
                    val matchesYear = selectedSubjectYear.isBlank() || selectedSubjectYear == "सभी" ||
                            testPaper.year.isBlank() || testPaper.year == selectedSubjectYear || testPaper.title.contains(selectedSubjectYear)
                    val matchesSearch = if (searchQuery.isBlank()) true else {
                        val query = searchQuery.trim().lowercase()
                        testPaper.title.lowercase().contains(query) ||
                                testPaper.year.lowercase().contains(query) ||
                                testPaper.difficulty.lowercase().contains(query)
                    }
                    matchesYear && matchesSearch
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Bar and Year Selector - scroll together with the tests
                item(key = "subject_search_and_year_header") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Search Bar
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceCard,
                            shadowElevation = if (isDark) 0.dp else 1.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 8.dp)
                                        .testTag("chapter_search_input"),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 13.5.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "फुल टेस्ट खोजें (${currentSubject?.name ?: "विषय"})...",
                                                fontSize = 13.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "साफ करें",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Year selector row
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pyq_subject_year_selector"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(availableYearsForSubject) { year ->
                                val isYearSelected = selectedSubjectYear == year
                                Surface(
                                    onClick = { selectedSubjectYear = year },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isYearSelected) (if (isDark) Color(0xFF2563EB) else Color(0xFF0F172A)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                                    shadowElevation = if (isYearSelected) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("subject_year_btn_$year")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = year,
                                            fontSize = 13.sp,
                                            fontWeight = if (isYearSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (isYearSelected) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (filteredSubjectTests.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "इस विषय में कोई टेस्ट नहीं मिला",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "फुल टेस्ट में टेस्ट जोड़ते ही इस विषय के प्रश्न ऑटोमैटिक यहाँ प्रदर्शित हो जाएंगे।",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(
                        items = filteredSubjectTests,
                        key = { _, testPaper -> testPaper.id }
                    ) { index, testPaper ->
                        DelhiPoliceTestCard(
                            testPaper = testPaper,
                            index = index,
                            isDynamic = true,
                            isAdmin = isAdmin,
                            onDeleteCustom = {
                                testPaperToDelete = testPaper
                            },
                            onEditTest = {
                                testPaperToEdit = testPaper
                            },
                            onClick = {
                                onStartTest(testPaper.title, testPaper.questions, testPaper.durationMinutes)
                            }
                        )
                    }
                }
            }

        } else {
            // =========================================================================
            // MODE 2: PYQ CHAPTER-WISE SCREEN
            // =========================================================================
            val activeChapters = currentSubject?.chapters ?: emptyList()
            val availableYearsForChapter = remember(dynamicChapterTests, activeChapters) {
                val foundYears1 = dynamicChapterTests.map { it.testPaper.year }.filter { it.isNotBlank() && it != "सभी" }
                val foundYears2 = activeChapters.map { if (it.year.isNotBlank() && it.year != "सभी") it.year else CustomChapterRepository.extractYear(it.title, it.description) }.filter { it.isNotBlank() && it != "सभी" }
                (listOf("सभी", "2026", "2025", "2024", "2023", "2022", "2021", "2020") + foundYears1 + foundYears2).distinct()
            }

            val filteredChapters = remember(activeChapters, searchQuery, selectedChapterYear) {
                activeChapters.filter { chapter ->
                    val isTopicPlaceholder = chapter.id.startsWith("topic_") || (chapter.questions.isEmpty() && chapter.totalQuestions == 0)
                    val chapterYear = if (chapter.year.isNotBlank() && chapter.year != "सभी") chapter.year else CustomChapterRepository.extractYear(chapter.title, chapter.description)
                    val matchesYear = if (isTopicPlaceholder || selectedChapterYear == "सभी") true else {
                        chapterYear == selectedChapterYear || chapter.title.contains(selectedChapterYear) || chapter.description.contains(selectedChapterYear)
                    }
                    val matchesSearch = if (searchQuery.isBlank()) true else {
                        val query = searchQuery.trim().lowercase()
                        chapter.title.lowercase().contains(query) ||
                                chapter.description.lowercase().contains(query) ||
                                chapter.id.lowercase().contains(query)
                    }
                    matchesYear && matchesSearch
                }
            }

            val groupedChapters = remember(filteredChapters) {
                val groups = mutableMapOf<String, MutableList<ChapterItem>>()
                filteredChapters.forEach { chapter ->
                    val baseTitle = CustomChapterRepository.cleanBaseChapterTitle(chapter.title)
                    if (!groups.containsKey(baseTitle)) {
                        groups[baseTitle] = mutableListOf()
                    }
                    groups[baseTitle]?.add(chapter)
                }
                groups
            }
            val expandedGroups = remember {
                androidx.compose.runtime.mutableStateListOf<String>()
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Bar and Year Selector - scroll together with the tests
                item(key = "chapter_search_and_year_header") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Search Bar
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceCard,
                            shadowElevation = if (isDark) 0.dp else 1.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                androidx.compose.foundation.text.BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 8.dp)
                                        .testTag("chapter_search_input"),
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 13.5.sp,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text(
                                                text = "अध्याय खोजें (${currentSubject?.name ?: "सभी"}) ...",
                                                fontSize = 13.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "साफ करें",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Year selector row
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pyq_chapter_year_selector"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(availableYearsForChapter) { year ->
                                val isYearSelected = selectedChapterYear == year
                                Surface(
                                    onClick = { selectedChapterYear = year },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isYearSelected) (if (isDark) Color(0xFF2563EB) else Color(0xFF0F172A)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                                    shadowElevation = if (isYearSelected) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("chapter_year_btn_$year")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = year,
                                            fontSize = 13.sp,
                                            fontWeight = if (isYearSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (isYearSelected) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                // Admin Action: Sleek & Compact Button to Create a new Chapter / Button with text typing & tests
                if (isAdmin && searchQuery.isBlank()) {
                    item {
                        Surface(
                            onClick = { showAddChapterDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFF86EFAC)),
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_add_chapter_banner_btn")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "+ ${currentSubject?.name ?: "इस विषय"} में नया अध्याय / टॉपिक बटन जोड़ें",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF14532D)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF16A34A)
                                ) {
                                    Text(
                                        text = "+ जोड़ें",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredChapters.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "कोई अध्याय नहीं मिला",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isAdmin) "ऊपर '+ अध्याय' बटन पर क्लिक करके नया अध्याय जोड़ें" else "कृपया कोई अन्य विषय या शब्द खोजें",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    val baseTitles = groupedChapters.keys.toList()
                    items(
                        count = baseTitles.size,
                        key = { groupIndex -> baseTitles[groupIndex] }
                    ) { groupIndex ->
                        val baseTitle = baseTitles[groupIndex]
                        val chaptersInGroup = groupedChapters[baseTitle] ?: emptyList()
                        
                        // Expandable Group
                        val isExpanded = expandedGroups.contains(baseTitle)
                        
                        ChapterGroupHeaderCard(
                            baseTitle = baseTitle,
                            isExpanded = isExpanded,
                            chaptersInGroup = chaptersInGroup,
                            isAdmin = isAdmin,
                            onDeleteGroup = {
                                chapterGroupToDelete = Pair(baseTitle, chaptersInGroup)
                            },
                            onClick = {
                                if (isExpanded) expandedGroups.remove(baseTitle) else expandedGroups.add(baseTitle)
                            }
                        )
                        
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val rawActualTests = chaptersInGroup.filter { !it.id.startsWith("topic_") && (it.questions.isNotEmpty() || it.totalQuestions > 0) }
                                val actualTests = if (selectedChapterYear == "सभी") {
                                    rawActualTests.sortedWith(
                                        compareByDescending<ChapterItem> { ch ->
                                            val y = if (ch.year.isNotBlank() && ch.year != "सभी") ch.year else CustomChapterRepository.extractYear(ch.title, ch.description)
                                            y.toIntOrNull() ?: 2025
                                        }.thenBy { ch ->
                                            CustomChapterRepository.extractSetNumber(ch.title)
                                        }
                                    )
                                } else {
                                    rawActualTests.sortedBy { CustomChapterRepository.extractSetNumber(it.title) }
                                }
                                if (actualTests.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "इस अध्याय में अभी कोई टेस्ट उपलब्ध नहीं है",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (isAdmin) "टॉपिक सुरक्षित है। आप इसमें नया टेस्ट जोड़ सकते हैं।" else "जल्द ही नए टेस्ट जोड़े जाएंगे।",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                            if (isAdmin) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Button(
                                                    onClick = {
                                                        initialTopicTitleForAdd = baseTitle
                                                        showAddChapterDialog = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("➕ इस टॉपिक में टेस्ट जोड़ें", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    actualTests.forEachIndexed { setIndex, chapter ->
                                        val isCustomChapter = chapter.id.startsWith("custom_chap_")
                                        val origSet = CustomChapterRepository.extractSetNumber(chapter.title)
                                        val setTag = if (origSet != 999) "Set $origSet" else if (selectedChapterYear == "सभी") "Set ${setIndex + 1}" else CustomChapterRepository.extractSetDisplayTitle(chapter.title, setIndex)
                                        val yearTag = if (chapter.year.isNotBlank() && chapter.year != "सभी") chapter.year else CustomChapterRepository.extractYear(chapter.title, chapter.description)
                                        val baseChap = CustomChapterRepository.cleanBaseChapterTitle(chapter.title)
                                        val computedTestTitle = if (selectedChapterYear == "सभी") {
                                            "${subExam.name} - $baseChap - $setTag ($yearTag PYQ)"
                                        } else if (!chapter.title.contains(selectedChapterYear)) {
                                            "${subExam.name} - $baseChap - $setTag ($selectedChapterYear PYQ)"
                                        } else {
                                            "${subExam.name} - ${chapter.title}"
                                        }
                                        SetItemCard(
                                            testTitle = computedTestTitle,
                                            chapter = chapter,
                                            index = setIndex,
                                            subExamName = subExam.name,
                                            subjectName = currentSubject?.name ?: "विषय",
                                            selectedYear = selectedChapterYear,
                                            isCustom = isCustomChapter,
                                            isAdmin = isAdmin,
                                            onDeleteCustom = {
                                                chapterToDelete = chapter
                                            },
                                            onEditTest = {
                                                chapterToEdit = chapter
                                            },
                                            onStartTest = {
                                                val isMathOrReasoning = CustomChapterRepository.isMathOrReasoningSubject(chapter.subjectKey, chapter.subjectTitle)
                                                val qCount = if (chapter.questions.isNotEmpty()) chapter.questions.size else chapter.totalQuestions
                                                val chapterDur = if (isMathOrReasoning) qCount * 2 else chapter.durationMinutes
                                                onStartTest(computedTestTitle, chapter.questions, chapterDur)
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
    }

    // Admin Dialogs
    if (showAddSubjectDialog) {
        AdminAddSubjectDialog(
            subExam = subExam,
            onDismiss = { showAddSubjectDialog = false },
            onSubjectAdded = { newKey ->
                selectedSubjectKey = newKey
                showAddSubjectDialog = false
            }
        )
    }

    if (showAddChapterDialog) {
        AdminAiTestCreatorDialog(
            onDismiss = {
                showAddChapterDialog = false
                initialTopicTitleForAdd = ""
            },
            initialMode = "CHAPTER_WISE",
            initialSubExamId = subExam.id,
            initialSubjectKey = selectedSubjectKey,
            initialChapterTitle = initialTopicTitleForAdd
        )
    }

    
    if (testPaperToEdit != null) {
        val entry = com.example.model.DynamicTestRepository.customTests.find { it.testPaper.id == testPaperToEdit!!.id }
        if (entry != null) {
            com.example.ui.components.AdminEditTestPaperDialog(
                testPaper = testPaperToEdit!!,
                testCategory = entry.postType,
                subExamId = entry.subExamId,
                onDismiss = { testPaperToEdit = null },
                onSaved = { testPaperToEdit = null }
            )
        } else {
            testPaperToEdit = null
        }
    }

    if (chapterToEdit != null) {
        AdminEditChapterQuestionsDialog(
            subExam = subExam,
            chapter = chapterToEdit!!,
            onDismiss = { chapterToEdit = null },
            onSaved = { chapterToEdit = null }
        )
    }

    // Confirmation dialog for deleting a subject
    if (subjectToDelete != null) {
        val targetSubject = subjectToDelete!!
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "विषय हटाएं? (Delete Subject)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "क्या आप वास्तव में विषय '${targetSubject.name}' और इसके तहत बनाए गए सभी अध्यायों/टेस्ट को हटाना चाहते हैं? यह क्रिया वापस नहीं ली जा सकती।",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = targetSubject
                        subjectToDelete = null
                        CustomChapterRepository.deleteSubject(subExam.id, toDel.key, context)
                        Toast.makeText(context, "विषय '${toDel.name}' सफलतापूर्वक हटा दिया गया", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("हां, हटाएं (Delete)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { subjectToDelete = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Confirmation dialog before deleting subject-wise test
    if (testPaperToDelete != null) {
        val targetTest = testPaperToDelete!!
        AlertDialog(
            onDismissRequest = { testPaperToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "टेस्ट हटाएं? (Delete Test)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "क्या आप वाकई टेस्ट '${targetTest.title}' को हटाना चाहते हैं?\n\nयह टेस्ट ऐप और GitHub क्लाउड दोनों से हमेशा के लिए डिलीट हो जाएगा। यह क्रिया वापस नहीं ली जा सकती।",
                    fontSize = 13.5.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = targetTest
                        testPaperToDelete = null
                        val questionsToDelete = toDel.questions
                        com.example.model.DynamicTestRepository.deleteCustomTest(toDel.id.toString(), context)
                        android.widget.Toast.makeText(context, "टेस्ट '${toDel.title}' हटाया जा रहा है...", android.widget.Toast.LENGTH_SHORT).show()
                        kotlinx.coroutines.GlobalScope.launch {
                            com.example.service.OnlineTestSyncManager.deleteTestAssetsFromCloud(context, questionsToDelete)
                            kotlinx.coroutines.Dispatchers.Main.dispatch(kotlin.coroutines.EmptyCoroutineContext) {
                                android.widget.Toast.makeText(context, "✅ '${toDel.title}' ऐप और GitHub (Images + JSON) दोनों से डिलीट हो गया!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("हाँ, हटाएं (OK)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { testPaperToDelete = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Confirmation dialog before deleting chapter-wise test
    if (chapterToDelete != null) {
        val targetChapter = chapterToDelete!!
        AlertDialog(
            onDismissRequest = { chapterToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "टेस्ट हटाएं? (Delete Test)",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "क्या आप वाकई टेस्ट '${targetChapter.title}' को हटाना चाहते हैं?\n\nयह टेस्ट डिलीट हो जाएगा, लेकिन टॉपिक बटन सुरक्षित रहेगा ताकि आप इसमें और टेस्ट जोड़ सकें।",
                    fontSize = 13.5.sp,
                    color = TextSecondary
                )
            },
                    confirmButton = {
                Button(
                    onClick = {
                        val toDel = targetChapter
                        chapterToDelete = null
                        val questionsToDelete = toDel.questions
                        CustomChapterRepository.deleteChapter(
                            subExamId = subExam.id,
                            chapterId = toDel.id,
                            subjectKey = toDel.subjectKey,
                            chapterTitle = toDel.title,
                            context = context
                        )
                        Toast.makeText(context, "टेस्ट '${toDel.title}' हटाया जा रहा है...", Toast.LENGTH_SHORT).show()
                        kotlinx.coroutines.GlobalScope.launch {
                            com.example.service.OnlineTestSyncManager.deleteTestAssetsFromCloud(context, questionsToDelete)
                            kotlinx.coroutines.Dispatchers.Main.dispatch(kotlin.coroutines.EmptyCoroutineContext) {
                                Toast.makeText(context, "✅ '${toDel.title}' हटाया गया! (टॉपिक बटन सुरक्षित है)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("हाँ, हटाएं (OK)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { chapterToDelete = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("रद्द करें")
                }
            }
        )
    }

    if (chapterGroupToDelete != null) {
        val targetGroup = chapterGroupToDelete!!
        val isAllYears = selectedChapterYear == "सभी"
        val dialogTitle = if (isAllYears) "पूरा अध्याय (सभी वर्ष) हटाएं?" else "वर्ष $selectedChapterYear के टेस्ट हटाएं?"
        val dialogMsg = if (isAllYears) {
            "क्या आप वाकई अध्याय '${targetGroup.first}' और इसके सभी वर्षों के टेस्ट सेट्स को हटाना चाहते हैं?\n\nइसके सभी प्रश्न ऐप और GitHub क्लाउड दोनों से हमेशा के लिए डिलीट हो जाएंगे।"
        } else {
            "क्या आप वाकई अध्याय '${targetGroup.first}' के वर्ष $selectedChapterYear के टेस्ट (${targetGroup.second.size} टेस्ट) हटाना चाहते हैं?\n\n(नोट: 2024, 2025 और अन्य वर्षों के टेस्ट बिल्कुल सुरक्षित रहेंगे।)"
        }
        AlertDialog(
            onDismissRequest = { chapterGroupToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = dialogTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = dialogMsg,
                    fontSize = 13.5.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = targetGroup
                        val yearToDelete = selectedChapterYear
                        chapterGroupToDelete = null
                        val allQuestionsToDelete = toDel.second.flatMap { it.questions }
                        CustomChapterRepository.deleteChapterGroupForYear(
                            subExamId = subExam.id,
                            subjectKey = currentSubject?.key ?: "",
                            baseTitle = toDel.first,
                            year = yearToDelete,
                            context = context
                        )
                        val successText = if (yearToDelete == "सभी") "अध्याय '${toDel.first}' डिलीट हो गया!" else "'${toDel.first}' के वर्ष $yearToDelete के टेस्ट हटा दिए गए (अन्य वर्ष सुरक्षित हैं)!"
                        Toast.makeText(context, successText, Toast.LENGTH_SHORT).show()
                        kotlinx.coroutines.GlobalScope.launch {
                            com.example.service.OnlineTestSyncManager.deleteTestAssetsFromCloud(context, allQuestionsToDelete)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("हाँ, हटाएं (OK)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { chapterGroupToDelete = null },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("रद्द करें")
                }
            }
        )
    }
}

@Composable
fun ChapterGroupHeaderCard(
    baseTitle: String,
    isExpanded: Boolean,
    chaptersInGroup: List<ChapterItem> = emptyList(),
    isAdmin: Boolean = false,
    onDeleteGroup: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val isDark = ThemeManager.isDarkMode
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor),
        modifier = Modifier.fillMaxWidth().testTag("chapter_group_card_${baseTitle.hashCode()}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (isDark) Color(0xFF1E3A8A) else Color(0xFFEFF6FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = baseTitle,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val actualTests = chaptersInGroup.filter { !it.id.startsWith("topic_") && (it.questions.isNotEmpty() || it.totalQuestions > 0) }
                    if (actualTests.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFEF3C7),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, if (isDark) Color(0xFFD97706) else Color(0xFFF59E0B))
                        ) {
                            Text(
                                text = if (isAdmin) "0 टेस्ट उपलब्ध • नया टेस्ट जोड़ें" else "0 टेस्ट उपलब्ध",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        val totalQ = actualTests.sumOf { if (it.questions.isNotEmpty()) it.questions.size else it.totalQuestions }
                        val distinctYears = actualTests.map { CustomChapterRepository.extractYear(it.title, it.description) }.distinct()
                        val statusText = if (distinctYears.size > 1) {
                            "${actualTests.size} टेस्ट (${distinctYears.size} वर्ष) • $totalQ प्रश्न"
                        } else if (actualTests.size == 1) {
                            "1 टेस्ट • $totalQ प्रश्न"
                        } else {
                            "${actualTests.size} टेस्ट सेट्स • $totalQ प्रश्न"
                        }
                        Text(
                            text = statusText,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isAdmin && onDeleteGroup != null) {
                    IconButton(
                        onClick = onDeleteGroup,
                        modifier = Modifier.size(32.dp).testTag("delete_chapter_group_btn_${baseTitle.hashCode()}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "अध्याय हटाएं (Delete Chapter)",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = TextSecondary
                )
            }
        }
    }
}

@Composable
fun SetItemCard(
    testTitle: String,
    chapter: ChapterItem,
    index: Int,
    subExamName: String,
    subjectName: String,
    selectedYear: String = "2025",
    isCustom: Boolean = false,
    isAdmin: Boolean = false,
    onDeleteCustom: (() -> Unit)? = null,
    onEditTest: (() -> Unit)? = null,
    onStartTest: () -> Unit
) {
    val isDark = ThemeManager.isDarkMode
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        label = "set_card_scale"
    )

    val stats = com.example.model.TestHistoryRepository.attemptRecords[testTitle]
        ?: com.example.model.TestHistoryRepository.attemptRecords[chapter.id]
        ?: com.example.model.TestHistoryRepository.attemptRecords[chapter.title]
    val hasAttempted = stats != null

    val setNumberStr = if (selectedYear == "सभी") "Set ${index + 1}" else CustomChapterRepository.extractSetDisplayTitle(chapter.title, index)
    val yearTag = CustomChapterRepository.extractYear(chapter.title, chapter.description)
    val displayTitle = if (selectedYear == "सभी") {
        "Set ${index + 1} ($yearTag PYQ)"
    } else {
        setNumberStr
    }

    val cardBackgroundColor = if (hasAttempted) (if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFF0FDF4)) else SurfaceCard
    val cardBorderColor = if (hasAttempted) (if (isDark) Color(0xFF10B981) else Color(0xFF86EFAC)) else if (isCustom) (if (isDark) Color(0xFF2563EB) else Color(0xFF93C5FD)) else CardBorderColor
    val iconColor = if (hasAttempted) (if (isDark) Color(0xFF34D399) else Color(0xFF10B981)) else (if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6))

    Surface(
        onClick = onStartTest,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = cardBackgroundColor,
        shadowElevation = if (isPressed || isDark) 0.dp else 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .testTag("set_card_${chapter.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (hasAttempted) Icons.Default.CheckCircle else Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = displayTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (hasAttempted) {
                        // Display score and attempts
                        val scoreFormatted = if (stats!!.highestScore % 1.0 == 0.0) stats.highestScore.toInt().toString() else stats.highestScore.toString()
                        val totalFormatted = if (stats.totalMarks % 1.0 == 0.0) stats.totalMarks.toInt().toString() else stats.totalMarks.toString()
                        Text(
                            text = "स्कोर: $scoreFormatted/$totalFormatted • ${stats.attemptsCount} बार दिया",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFF4ADE80) else Color(0xFF059669)
                        )
                    } else {
                        // Display default test stats
                        val isMathOrReasoning = CustomChapterRepository.isMathOrReasoningSubject(chapter.subjectKey, chapter.subjectTitle)
                        val qCount = if (chapter.questions.isNotEmpty()) chapter.questions.size else chapter.totalQuestions
                        val displayDuration = if (isMathOrReasoning) qCount * 2 else chapter.durationMinutes
                        val yearInfo = if (selectedYear == "सभी") {
                            val y = CustomChapterRepository.extractYear(chapter.title, chapter.description)
                            " • $y PYQ"
                        } else ""

                        Text(
                            text = "${chapter.totalQuestions} प्रश्न • $displayDuration मिनट$yearInfo",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isAdmin && onEditTest != null) {
                    IconButton(
                        onClick = onEditTest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (isAdmin && onDeleteCustom != null) {
                    IconButton(
                        onClick = onDeleteCustom,
                        modifier = Modifier.size(32.dp).testTag("delete_set_${chapter.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
