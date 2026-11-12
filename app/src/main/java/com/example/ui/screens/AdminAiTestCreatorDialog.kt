package com.example.ui.screens

import android.widget.Toast
import com.example.model.RealtimeExamStatsManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.AddPhotoAlternate
import com.example.ui.components.AdminAiPromptRulesDialog
import com.example.ui.components.AdminDiagramCropDialog
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import android.app.Activity
import android.view.WindowManager
import android.os.PowerManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.testTag
import com.example.model.CustomChapterRepository
import com.example.model.ExamChapterManager
import com.example.model.ExamSubjectCategory
import com.example.model.ChapterItem
import com.example.model.DynamicTestRepository
import com.example.model.QuizQuestion
import com.example.model.TestPaper
import com.example.service.GeminiQuestionParserService
import com.example.ui.util.TextFormatHelper
import com.example.service.GitHubConfig
import com.example.service.GitHubSyncService
import com.example.service.GitHubUploadResult
import com.example.ui.components.ConfigInputField
import com.example.ui.components.GitHubConfigDialog
import kotlinx.coroutines.launch

@Composable
fun AdminAiTestCreatorDialog(
    onDismiss: () -> Unit,
    onLaunchTest: (TestPaper) -> Unit = {},
    initialMode: String = "FULL_TEST",
    initialSubExamId: String = "",
    initialSubjectKey: String = "MATHS",
    initialChapterTitle: String = "",
    initialPostType: String = ""
) {
    val isExplicitChapterWise = initialMode == "CHAPTER_WISE" || initialChapterTitle.isNotBlank()
    val initialDraft = com.example.service.AiBackgroundExecutionManager.draftSession.value
    val currentBgTask = com.example.service.AiBackgroundExecutionManager.taskState.value
    val hasActiveOrDoneTask = !isExplicitChapterWise && (currentBgTask.isRunning || (currentBgTask.isSuccess && currentBgTask.parsedQuestions.isNotEmpty()) || initialDraft.parsedQuestions.isNotEmpty() || initialDraft.rawAnswerKeyText.isNotBlank())

    var currentStep by remember { 
        mutableIntStateOf(if (hasActiveOrDoneTask) 5 else if (initialMode == "CHAPTER_WISE") 4 else 1) 
    }
    val effectiveSubExamId = if (initialSubExamId.isNotBlank()) initialSubExamId else initialDraft.subExamId.ifBlank { "ssc_gd" }
    val effectiveSubExamName = when (effectiveSubExamId) {
        "ssc_gd" -> "SSC GD Constable"
        "ssc_delhi_police", "delhi_police" -> "Delhi Police"
        "ssc_cgl" -> "SSC CGL"
        "ssc_chsl" -> "SSC CHSL"
        else -> initialDraft.subExam.ifBlank { "SSC GD Constable" }
    }
    var selectedCategory by remember { mutableStateOf(initialDraft.category.ifBlank { "SSC Exams" }) }
    var selectedSubExam by remember { mutableStateOf(effectiveSubExamName) }
    var selectedSubExamId by remember { mutableStateOf(effectiveSubExamId) }
    var selectedTestMode by remember { mutableStateOf(if (initialMode.isNotBlank()) initialMode else initialDraft.testMode) }
    var selectedPostType by remember { mutableStateOf(if (initialPostType.isNotBlank()) initialPostType else initialDraft.postType.ifBlank { "CONSTABLE" }) }
    var selectedYear by remember { mutableStateOf(initialDraft.year.ifBlank { "2025" }) }
    var selectedSubjectKey by remember { mutableStateOf(if (initialSubjectKey.isNotBlank()) initialSubjectKey else initialDraft.subjectKey.ifBlank { "MATHS" }) }
    var selectedSubjectTitle by remember { mutableStateOf(initialDraft.subjectTitle.ifBlank { "गणित / Math" }) }
    var selectedChapterTitle by remember { mutableStateOf(if (initialChapterTitle.isNotBlank()) initialChapterTitle else initialDraft.chapterTitle.ifBlank { "प्रतिशत (Percentage)" }) }

    // Step 5 State
    var rawAnswerKeyText by remember { mutableStateOf(initialDraft.rawAnswerKeyText) }
    val defaultGeneratedTitle = when (effectiveSubExamId) {
        "ssc_gd" -> "SSC GD Constable Mock Test 2025"
        "ssc_delhi_police", "delhi_police" -> "Delhi Police Constable Mock Test 2025"
        else -> "$effectiveSubExamName Mock Test 2025"
    }
    val isDraftTitleClean = initialDraft.testTitle.isNotBlank() && (!initialDraft.testTitle.contains("Delhi Police", ignoreCase = true) || effectiveSubExamId.contains("delhi", ignoreCase = true))
    var testTitle by remember { mutableStateOf(if (isDraftTitleClean) initialDraft.testTitle else defaultGeneratedTitle) }
    var durationMinutes by remember { mutableIntStateOf(initialDraft.durationMinutes) }
    var totalMarks by remember { mutableIntStateOf(initialDraft.totalMarks) }
    var isProcessing by remember { mutableStateOf(currentBgTask.isRunning) }
    var isUploadingToGithub by remember { mutableStateOf(false) }
    var processingMessage by remember { mutableStateOf(currentBgTask.statusMessage) }
    var parsedQuestions by remember { mutableStateOf<List<QuizQuestion>>(if (currentBgTask.parsedQuestions.isNotEmpty()) currentBgTask.parsedQuestions else initialDraft.parsedQuestions) }
    var addedToLiveApp by remember { mutableStateOf(false) }
    var uploadedFileName by remember { mutableStateOf<String?>(initialDraft.uploadedFileName) }
    var gitHubUploadResult by remember { mutableStateOf<GitHubUploadResult?>(null) }
    var showClearAllConfirmGlobal by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    if (showClearAllConfirmGlobal) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearAllConfirmGlobal = false },
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
                    text = "🗑️ सभी ${parsedQuestions.size} प्रश्न हटाएं?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "क्या आप वाकई तैयार किए गए सभी ${parsedQuestions.size} प्रश्नों को हटाना चाहते हैं? यह क्रिया ड्राफ्ट और प्रश्नों को पूरी तरह साफ़ कर देगी ताकि आप नया टेस्ट या PDF शुरू कर सकें।",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        parsedQuestions = emptyList()
                        rawAnswerKeyText = ""
                        uploadedFileName = null
                        com.example.service.AiBackgroundExecutionManager.clearAllQuestionsAndDraft(context)
                        showClearAllConfirmGlobal = false
                        Toast.makeText(context, "🧹 सभी प्रश्न व ड्राफ्ट सफलतापूर्वक साफ़ कर दिए गए!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("हाँ, सब साफ़ करें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearAllConfirmGlobal = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("रद्द करें")
                }
            }
        )
    }

    var processStartTime by remember { mutableStateOf(0L) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var processProgressPercent by remember { mutableIntStateOf(0) }
    var isScreenLocked by remember { mutableStateOf(false) }

    val bgTaskState by com.example.service.AiBackgroundExecutionManager.taskState.collectAsState()

    // Sync state with background execution manager
    androidx.compose.runtime.LaunchedEffect(bgTaskState) {
        if (bgTaskState.isRunning) {
            isProcessing = true
            processingMessage = bgTaskState.statusMessage
            processProgressPercent = bgTaskState.progressPercent
        } else if (bgTaskState.isSuccess && bgTaskState.parsedQuestions.isNotEmpty()) {
            if (parsedQuestions.isEmpty() || parsedQuestions.size != bgTaskState.parsedQuestions.size) {
                parsedQuestions = bgTaskState.parsedQuestions
                if (testTitle.isBlank() && bgTaskState.examTitle.isNotBlank()) {
                    testTitle = bgTaskState.examTitle
                }
            }
            if (!isUploadingToGithub) {
                isProcessing = false
            }
        } else if (bgTaskState.errorMessage != null && !bgTaskState.isRunning && !isUploadingToGithub) {
            isProcessing = false
        }
    }

    // Keep screen awake & acquire WakeLock during AI processing to prevent phone sleep & background termination
    val activity = (context as? Activity)
    androidx.compose.runtime.DisposableEffect(isProcessing, isUploadingToGithub) {
        val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ExamMitra:AiProcessingWakeLock"
        )
        if (isProcessing || isUploadingToGithub) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            try {
                wakeLock?.acquire(30 * 60 * 1000L /* 30 minutes max */)
            } catch (_: Exception) {}
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (_: Exception) {}
        }
    }

    androidx.compose.runtime.LaunchedEffect(isProcessing, isUploadingToGithub) {
        if (isProcessing || isUploadingToGithub) {
            processStartTime = System.currentTimeMillis()
            elapsedSeconds = 0
            while (isProcessing || isUploadingToGithub) {
                kotlinx.coroutines.delay(1000L)
                elapsedSeconds = ((System.currentTimeMillis() - processStartTime) / 1000L).toInt()
            }
        } else {
            processProgressPercent = 0
            isScreenLocked = false // Auto-unlock screen when processing finishes
        }
    }

    // GitHub Config State
    var gitHubConfig by remember { mutableStateOf(GitHubSyncService.getSavedConfig(context)) }
    var isPdfLoading by remember { mutableStateOf(false) }
    var pdfLoadingMessage by remember { mutableStateOf("PDF फाइल लोड हो रही है...") }
    var pdfTotalPages by remember { mutableStateOf(0) }
    var showAiRulesDialog by remember { mutableStateOf(false) }
    var showApiKeyManagerDialog by remember { mutableStateOf(false) }

    if (showAiRulesDialog) {
        AdminAiPromptRulesDialog(
            onDismiss = { showAiRulesDialog = false }
        )
    }

    if (showApiKeyManagerDialog) {
        com.example.ui.components.AdminApiKeyManagerDialog(
            onDismiss = { showApiKeyManagerDialog = false }
        )
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            uploadedFileName = uri.lastPathSegment ?: "answer_key.pdf"
            isPdfLoading = true
            pdfLoadingMessage = "⏳ PDF फाइल खोली जा रही है..."
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                var tempFile: java.io.File? = null
                var pfd: android.os.ParcelFileDescriptor? = null
                var renderer: android.graphics.pdf.PdfRenderer? = null
                try {
                    // Copy URI content to a local temporary cache file to ensure seekability and avoid permission issues
                    val cachePdf = java.io.File(context.cacheDir, "pdf_import_${System.currentTimeMillis()}.pdf")
                    tempFile = cachePdf
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        cachePdf.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: throw java.io.IOException("PDF फाइल पढ़ने की अनुमति नहीं मिल सकी")

                    pfd = android.os.ParcelFileDescriptor.open(cachePdf, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                    renderer = android.graphics.pdf.PdfRenderer(pfd)
                    val pageCount = renderer.pageCount
                    pdfTotalPages = pageCount
                    val jsonArray = org.json.JSONArray()
                    // Process all pages of the PDF without artificial capping so all 100 questions are fully parsed
                    val maxPagesToProcess = pageCount

                    for (i in 0 until maxPagesToProcess) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            pdfLoadingMessage = "📄 PDF पेज ${i + 1} / $maxPagesToProcess तैयार हो रहा है..."
                        }
                        var page: android.graphics.pdf.PdfRenderer.Page? = null
                        try {
                            page = renderer.openPage(i)
                            // 1200px width with 80% JPEG quality renders in <50ms per page while preserving full clarity for text, questions & diagrams
                            val width = 1200
                            val height = (width * page.height.toFloat() / page.width.toFloat()).toInt()
                            // PdfRenderer strictly requires ARGB_8888 bitmap config
                            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                            // IMPORTANT: Fill with white background before rendering PDF, because PDF often has a transparent background
                            // and JPEG compression turns transparency black, making black text invisible.
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            val stream = java.io.ByteArrayOutputStream(256 * 1024)
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
                            val byteArray = stream.toByteArray()
                            val base64Img = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
                            jsonArray.put(base64Img)
                            bitmap.recycle()
                        } catch (pageEx: Exception) {
                            // Skip problematic individual page
                        } finally {
                            try { page?.close() } catch (_: Exception) {}
                        }
                        kotlinx.coroutines.yield()
                    }

                    if (jsonArray.length() > 0) {
                        val pagesStr = jsonArray.toString()
                        rawAnswerKeyText = "data:image/jpeg-pages;base64,$pagesStr"
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            isPdfLoading = false
                            Toast.makeText(context, "✅ PDF के सभी ${jsonArray.length()} पेज सुरक्षित रूप से लोड हो गए! अब 'AI Parse' दबाएं।", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            isPdfLoading = false
                            Toast.makeText(context, "❌ PDF से पेज लोड नहीं हो सके। कृपया दोबारा चुनें।", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isPdfLoading = false
                        val errorMsg = when {
                            e is SecurityException -> "पासवर्ड सुरक्षित PDF समर्थित नहीं है या सुरक्षा त्रुटि हुई।"
                            e.message?.contains("seekable", ignoreCase = true) == true -> "PDF फ़ाइल पढ़ने योग्य नहीं है।"
                            else -> e.message ?: "अज्ञात त्रुटि"
                        }
                        Toast.makeText(context, "❌ PDF त्रुटि: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    try { renderer?.close() } catch (_: Exception) {}
                    try { pfd?.close() } catch (_: Exception) {}
                    try { tempFile?.delete() } catch (_: Exception) {}
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Surface(
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (currentStep > 1) {
                                    IconButton(
                                        onClick = { currentStep-- },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = Color(0xFF1E293B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFEF3C7)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Admin AI Test Creator",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFDCFCE7))
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "OWNER",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Text(
                                        text = "स्टेप $currentStep/5: ${
                                            when (currentStep) {
                                                1 -> "एग्जाम चुनें"
                                                2 -> "सिलेक्ट करें"
                                                3 -> "टेस्ट मोड"
                                                4 -> "पोस्ट/वर्ष"
                                                else -> "AI पार्सर"
                                            }
                                        }",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Multi-API Key Manager Button (Compact)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                                    modifier = Modifier.clickable { showApiKeyManagerDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Key,
                                            contentDescription = "Keys",
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Keys",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2563EB)
                                        )
                                    }
                                }

                                // AI Rules Button (Compact)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF3E8FF),
                                    border = BorderStroke(1.dp, Color(0xFFD8B4FE)),
                                    modifier = Modifier.clickable { showAiRulesDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Gavel,
                                            contentDescription = "Rules",
                                            tint = Color(0xFF7C3AED),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Rules",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF7C3AED)
                                        )
                                    }
                                }

                                // Clear Draft & Questions Button (Visible when on Step 5 and questions/draft exists)
                                if (currentStep == 5 && (parsedQuestions.isNotEmpty() || rawAnswerKeyText.isNotBlank() || uploadedFileName != null)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFEF2F2),
                                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                        modifier = Modifier.clickable { showClearAllConfirmGlobal = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Clear",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "साफ़ करें",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFDC2626)
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Progress Indicator Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (step in 1..5) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(
                                            if (step <= currentStep) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }

                // Step Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentStep) {
                        1 -> Step1CategorySelection(
                            selectedCategory = selectedCategory,
                            onSelect = { cat ->
                                selectedCategory = cat
                                currentStep = 2
                            }
                        )

                        2 -> Step2SubExamSelection(
                            category = selectedCategory,
                            selectedExam = selectedSubExam,
                            onSelect = { examName, examId ->
                                selectedSubExam = examName
                                selectedSubExamId = examId
                                currentStep = 3
                            }
                        )

                        3 -> Step3TestModeSelection(
                            selectedMode = selectedTestMode,
                            onSelect = { mode ->
                                selectedTestMode = mode
                                currentStep = 4
                            }
                        )

                        4 -> if (selectedTestMode == "CHAPTER_WISE") {
                            Step4ChapterWiseSelection(
                                category = selectedCategory,
                                subExam = selectedSubExam,
                                subExamId = selectedSubExamId,
                                selectedSubjectKey = selectedSubjectKey,
                                selectedChapter = selectedChapterTitle,
                                selectedYear = selectedYear,
                                onSelect = { subKey, subTitle, chapTitle, year ->
                                    val cleanChap = com.example.model.CustomChapterRepository.cleanBaseChapterTitle(chapTitle)
                                    selectedSubjectKey = subKey
                                    selectedSubjectTitle = subTitle
                                    selectedChapterTitle = cleanChap
                                    selectedYear = year
                                    testTitle = "$selectedSubExam • $subTitle • $cleanChap"
                                    durationMinutes = 10
                                    totalMarks = 10
                                    currentStep = 5
                                }
                            )
                        } else {
                            Step4PostAndYearSelection(
                                subExam = selectedSubExam,
                                selectedPost = selectedPostType,
                                selectedYear = selectedYear,
                                onSelect = { post, year ->
                                    selectedPostType = post
                                    selectedYear = year
                                    val existingTests = com.example.model.DynamicTestRepository.getCustomTests(selectedSubExamId, post, year)
                                    val nextNum = existingTests.size + 1
                                    val postLabel = when (post) {
                                        "TIER_1" -> "Tier-1"
                                        "TIER_2" -> "Tier-2"
                                        "CBT_1" -> "CBT-1"
                                        "CBT_2" -> "CBT-2"
                                        "CONSTABLE" -> "कांस्टेबल"
                                        "HEAD_CONSTABLE" -> "हेड कांस्टेबल"
                                        "SI" -> "SI"
                                        else -> post
                                    }
                                    testTitle = "$selectedSubExam $postLabel $year - Mock Test $nextNum"
                                    durationMinutes = 90
                                    totalMarks = 100
                                    currentStep = 5
                                }
                            )
                        }

                        5 -> Step5AiStudio(
                            category = selectedCategory,
                            subExam = selectedSubExam,
                            subExamId = selectedSubExamId,
                            testMode = selectedTestMode,
                            postType = selectedPostType,
                            year = selectedYear,
                            subjectKey = selectedSubjectKey,
                            subjectTitle = selectedSubjectTitle,
                            chapterTitle = selectedChapterTitle,
                            testTitle = testTitle,
                            onTitleChange = { testTitle = it },
                            rawText = rawAnswerKeyText,
                            onTextChange = { rawAnswerKeyText = it },
                            uploadedFileName = uploadedFileName,
                            pdfTotalPages = pdfTotalPages,
                            onPickPdf = { pdfPickerLauncher.launch("application/pdf") },
                            onLoadSample = {
                                rawAnswerKeyText = GeminiQuestionParserService.getSampleAnswerKeyText()
                                uploadedFileName = "Official_DP_AnswerKey_2025.pdf"
                                pdfTotalPages = 1
                                Toast.makeText(context, "सैंपल आंसर की लोड हो गई!", Toast.LENGTH_SHORT).show()
                            },
                            isProcessing = isProcessing,
                            isUploadingToGithub = isUploadingToGithub,
                            processingMessage = processingMessage,
                            parsedQuestions = parsedQuestions,
                            onQuestionsChange = { updated ->
                                parsedQuestions = updated
                                addedToLiveApp = false
                                com.example.service.AiBackgroundExecutionManager.updateDraftSession { s ->
                                    s.copy(parsedQuestions = updated, currentStep = 5)
                                }
                            },
                            onClearAll = {
                                parsedQuestions = emptyList()
                                rawAnswerKeyText = ""
                                uploadedFileName = null
                                addedToLiveApp = false
                                com.example.service.AiBackgroundExecutionManager.clearAllQuestionsAndDraft(context)
                            },
                            addedToLiveApp = addedToLiveApp,
                            gitHubConfig = gitHubConfig,
                            gitHubUploadResult = gitHubUploadResult,
                            onSaveGitHubConfig = { newConfig ->
                                gitHubConfig = newConfig
                                GitHubSyncService.saveConfig(context, newConfig)
                                Toast.makeText(context, "GitHub सेटिंग्स सुरक्षित हो गईं!", Toast.LENGTH_SHORT).show()
                            },
                            onProcessAi = {
                                if (rawAnswerKeyText.isBlank()) {
                                    Toast.makeText(context, "कृपया पहले आंसर की टेक्स्ट या PDF अपलोड करें", Toast.LENGTH_SHORT).show()
                                    return@Step5AiStudio
                                }
                                isProcessing = true
                                processProgressPercent = 5
                                processingMessage = "1/3: 🚀 Google Gemini AI मॉडल बैकग्राउंड में कनेक्ट हो रहा है..."
                                
                                com.example.service.AiBackgroundExecutionManager.startExamParsing(
                                    context = context,
                                    rawText = rawAnswerKeyText,
                                    examContext = "$selectedCategory - $selectedSubExam $selectedYear",
                                    testMode = selectedTestMode,
                                    chapterTitle = selectedChapterTitle,
                                    subjectTitle = selectedSubjectTitle,
                                    userGivenTitle = testTitle,
                                    onCompleted = { result ->
                                        isProcessing = false
                                        if (result.isSuccess) {
                                            val examRes = result.getOrNull()
                                            if (examRes != null && examRes.questions.isNotEmpty()) {
                                                val qList = examRes.questions
                                                val stableTitle = if (testTitle.isNotBlank()) testTitle else examRes.examTitle.ifBlank { "Mock Test ($selectedYear)" }
                                                testTitle = stableTitle

                                                val offlineImagesDir = java.io.File(context.filesDir, "offline_images")
                                                if (!offlineImagesDir.exists()) offlineImagesDir.mkdirs()

                                                val finalizedQuestions = qList.map { q ->
                                                    val img = q.imageUrl
                                                    if (!img.isNullOrBlank() && (img.startsWith("data:image", true) || (!img.startsWith("http") && !img.startsWith("/") && img.length > 100))) {
                                                        try {
                                                            val clean = if (img.contains(",")) img.substringAfter(",") else img
                                                            val bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
                                                            val file = java.io.File(offlineImagesDir, "diagram_${System.currentTimeMillis() % 100000}_q${q.id}.jpg")
                                                            file.writeBytes(bytes)
                                                            q.copy(imageUrl = file.absolutePath)
                                                        } catch (_: Exception) {
                                                            q
                                                        }
                                                    } else {
                                                        q
                                                    }
                                                }
                                                parsedQuestions = finalizedQuestions
                                                addedToLiveApp = false
                                                currentStep = 5

                                                // Persist draft on disk so all questions stay intact on Step 5/5 even if dialog is reopened
                                                com.example.service.AiBackgroundExecutionManager.updateDraftSession { s ->
                                                    s.copy(
                                                        subExamId = selectedSubExamId,
                                                        parsedQuestions = finalizedQuestions,
                                                        testTitle = stableTitle,
                                                        currentStep = 5,
                                                        isDialogOpen = true
                                                    )
                                                }

                                                processProgressPercent = 100
                                                Toast.makeText(context, "🎉 सभी ${finalizedQuestions.size} प्रश्न Step 5/5 पर तैयार हैं! कृपया नीचे प्रश्न चेक व एडिट कर लें, फिर 'ऐप में जोड़ें' या 'GitHub में पुश' दबाएं।", Toast.LENGTH_LONG).show()
                                            } else {
                                                processProgressPercent = 100
                                                Toast.makeText(context, "⚠️ कोई प्रश्न नहीं मिल सका", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            processProgressPercent = 0
                                            Toast.makeText(context, result.exceptionOrNull()?.message ?: "त्रुटि", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                )
                            },
                            onAutoParseAndPushToGitHub = {
                                if (rawAnswerKeyText.isBlank()) {
                                    Toast.makeText(context, "कृपया पहले आंसर की टेक्स्ट या PDF अपलोड करें", Toast.LENGTH_SHORT).show()
                                    return@Step5AiStudio
                                }
                                isProcessing = true
                                processProgressPercent = 5
                                processingMessage = "1/3: 🚀 Google Gemini AI आंसर की से सभी प्रश्न निकाल रहा है..."
                                com.example.service.AiBackgroundExecutionManager.startExamParsing(
                                    context = context,
                                    rawText = rawAnswerKeyText,
                                    examContext = "$selectedCategory - $selectedSubExam $selectedYear",
                                    testMode = selectedTestMode,
                                    chapterTitle = selectedChapterTitle,
                                    subjectTitle = selectedSubjectTitle,
                                    userGivenTitle = testTitle,
                                    onCompleted = { parseResult ->
                                        if (parseResult.isFailure) {
                                            isProcessing = false
                                            processProgressPercent = 0
                                            Toast.makeText(context, "AI पार्सिंग में त्रुटि: ${parseResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                            return@startExamParsing
                                        }
                                        coroutineScope.launch {
                                            val examRes = parseResult.getOrNull()
                                            val localFinalized = examRes?.questions ?: emptyList()
                                            if (testTitle.isBlank() && examRes != null && examRes.examTitle.isNotBlank()) {
                                                testTitle = examRes.examTitle
                                            }

                                    processProgressPercent = 94
                                    processingMessage = "2/3: प्रश्नों में मौजूद आकृतियों/इमेजेस की जांच हो रही है..."
                                    val questionsWithGithubImages = GeminiQuestionParserService.extractAndUploadImagesToGitHub(
                                        questions = localFinalized,
                                        config = gitHubConfig,
                                        context = context,
                                        onProgress = { msg -> processingMessage = msg }
                                    )
                                    parsedQuestions = questionsWithGithubImages

                                    // Auto add to live app & cache offline
                                    if (selectedTestMode == "CHAPTER_WISE") {
                                        val createdSets = CustomChapterRepository.addChapterWithSets(
                                            subExamId = selectedSubExamId,
                                            subjectKey = selectedSubjectKey,
                                            subjectTitle = selectedSubjectTitle,
                                            baseChapterTitle = selectedChapterTitle,
                                            allQuestions = questionsWithGithubImages,
                                            durationPerSet = 10,
                                            difficulty = "मध्यम",
                                            year = selectedYear,
                                            context = context
                                        )
                                        createdSets.forEach { chap ->
                                            val paper = TestPaper(
                                                id = chap.id,
                                                title = chap.title,
                                                totalQuestions = chap.questions.size,
                                                durationMinutes = chap.durationMinutes,
                                                totalMarks = chap.totalMarks,
                                                difficulty = chap.difficulty,
                                                year = selectedYear,
                                                questions = chap.questions
                                            )
                                            DynamicTestRepository.addCustomTest(
                                                categoryId = selectedCategory,
                                                subExamId = selectedSubExamId,
                                                postType = selectedPostType,
                                                year = selectedYear,
                                                testType = "CHAPTER_WISE",
                                                testPaper = paper,
                                                context = context,
                                                persistToDisk = true
                                            )
                                        }
                                        addedToLiveApp = true
                                    } else {
                                        val stableTestId = "custom_ai_${selectedSubExamId}_${selectedPostType.lowercase()}_${selectedYear}_${System.currentTimeMillis() % 10000000}_${Math.abs(testTitle.trim().hashCode()) % 1000}"
                                        val newPaper = TestPaper(
                                            id = stableTestId,
                                            title = testTitle,
                                            totalQuestions = questionsWithGithubImages.size,
                                            durationMinutes = durationMinutes,
                                            totalMarks = totalMarks,
                                            difficulty = "ऑफिशियल पैटर्न",
                                            year = selectedYear,
                                            questions = questionsWithGithubImages
                                        )
                                        DynamicTestRepository.addCustomTest(
                                            categoryId = selectedCategory,
                                            subExamId = selectedSubExamId,
                                            postType = selectedPostType,
                                            year = selectedYear,
                                            testType = selectedTestMode,
                                            testPaper = newPaper,
                                            context = context,
                                            persistToDisk = true
                                        )
                                        com.example.model.OfflineTestRepository.cacheTestQuestions(context, testTitle, questionsWithGithubImages)
                                        com.example.model.OfflineTestRepository.cacheTestQuestions(context, stableTestId, questionsWithGithubImages)
                                        addedToLiveApp = true
                                    }

                                    // Check GitHub config
                                    if (gitHubConfig.token.isBlank() || gitHubConfig.owner.isBlank() || gitHubConfig.repo.isBlank()) {
                                        processProgressPercent = 100
                                        isProcessing = false
                                        isUploadingToGithub = false
                                        Toast.makeText(context, "🎉 शानदार! सभी ${questionsWithGithubImages.size} प्रश्न ऐप में सफलतापूर्वक जुड़ गए! (GitHub कॉन्फिग सेट नहीं था)", Toast.LENGTH_LONG).show()
                                        return@launch
                                    }

                                    // Now push to GitHub automatically
                                    processProgressPercent = 97
                                    isUploadingToGithub = true
                                    processingMessage = "3/3: JSON फाइल बनाकर GitHub Repository में Commit हो रही है..."

                                    val sanitizedExamName = selectedSubExamId.replace(" ", "_").lowercase()
                                    val sanitizedPost = selectedPostType.lowercase()
                                    val isChapterWise = selectedTestMode == "CHAPTER_WISE"
                                    val sanitizedSubject = selectedSubjectKey.replace(" ", "_").lowercase()
                                    val cleanTopic = selectedChapterTitle.trim().replace(" ", "_").replace("/", "_").lowercase().ifBlank { "topic" }
                                    val cleanTitle = testTitle.trim().replace(" ", "_").replace("/", "_").lowercase().ifBlank { "test" }

                                    var allSuccess = true
                                    var lastMessage = ""
                                    var lastUploadPath = ""

                                    if (isChapterWise) {
                                        val sets = if (questionsWithGithubImages.size > 10) questionsWithGithubImages.chunked(10) else listOf(questionsWithGithubImages)
                                        sets.forEachIndexed { idx, chunk ->
                                            val setNum = idx + 1
                                            val setTitle = "$selectedChapterTitle - Set $setNum"
                                            val setFileName = "${cleanTopic}_${selectedYear}_set_${setNum}.json"
                                            val setFullPath = "tests/$sanitizedExamName/chapter_wise/$sanitizedSubject/$setFileName"
                                            lastUploadPath = setFullPath

                                            val setJsonContent = GeminiQuestionParserService.exportToGitHubJson(
                                                testTitle = setTitle,
                                                year = selectedYear,
                                                questions = chunk,
                                                subExamId = selectedSubExamId,
                                                subjectKey = selectedSubjectKey,
                                                testType = "CHAPTER_WISE",
                                                postType = selectedPostType
                                            )

                                            val uploadRes = GitHubSyncService.uploadJsonFile(
                                                config = gitHubConfig,
                                                filePath = setFullPath,
                                                jsonString = setJsonContent,
                                                commitMessage = "Add Set $setNum for $selectedChapterTitle ($selectedYear) to $setFullPath"
                                            )
                                            if (!uploadRes.isSuccess) {
                                                allSuccess = false
                                                lastMessage = uploadRes.message
                                            }
                                        }
                                    } else {
                                        val fileName = "${sanitizedExamName}_${sanitizedPost}_${selectedYear}_${cleanTitle}.json"
                                        val fullPath = "tests/$sanitizedExamName/full_tests/$fileName"
                                        lastUploadPath = fullPath

                                        val jsonContent = GeminiQuestionParserService.exportToGitHubJson(
                                            testTitle = testTitle,
                                            year = selectedYear,
                                            questions = questionsWithGithubImages,
                                            subExamId = selectedSubExamId,
                                            subjectKey = selectedSubjectKey,
                                            testType = selectedTestMode,
                                            postType = selectedPostType
                                        )

                                        val uploadRes = GitHubSyncService.uploadJsonFile(
                                            config = gitHubConfig,
                                            filePath = fullPath,
                                            jsonString = jsonContent,
                                            commitMessage = "Add Full Test: $testTitle ($selectedYear) to $fullPath"
                                        )
                                        if (!uploadRes.isSuccess) {
                                            allSuccess = false
                                            lastMessage = uploadRes.message
                                        }
                                    }

                                    processProgressPercent = 100
                                    isProcessing = false
                                    isUploadingToGithub = false

                                    if (allSuccess) {
                                        Toast.makeText(context, "🎉 बधाई! प्रत्येक सेट की अलग-अलग JSON फाइल GitHub ($lastUploadPath) में सफलतापूर्वक सेव हो गई!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "⚠️ GitHub अपलोड: $lastMessage", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        )
                    },
                    onPushOnlyToGitHub = {
                                if (parsedQuestions.isEmpty()) {
                                    Toast.makeText(context, "पहले प्रश्न तैयार करें", Toast.LENGTH_SHORT).show()
                                    return@Step5AiStudio
                                }
                                coroutineScope.launch {
                                    isUploadingToGithub = true
                                    
                                    // 1. First upload any base64 images to GitHub
                                    val questionsWithGithubImages = GeminiQuestionParserService.extractAndUploadImagesToGitHub(
                                        questions = parsedQuestions,
                                        config = gitHubConfig,
                                        context = context,
                                        onProgress = { msg -> /* optional toast or log */ }
                                    )
                                    parsedQuestions = questionsWithGithubImages
                                    
                                    val sanitizedExamName = selectedSubExamId.replace(" ", "_").lowercase()
                                    val sanitizedPost = selectedPostType.lowercase()
                                    val isChapterWise = selectedTestMode == "CHAPTER_WISE"
                                    val sanitizedSubject = selectedSubjectKey.replace(" ", "_").lowercase()
                                    val cleanTopic = selectedChapterTitle.trim().replace(" ", "_").replace("/", "_").lowercase().ifBlank { "topic" }
                                    val cleanTitle = testTitle.trim().replace(" ", "_").replace("/", "_").lowercase().ifBlank { "test" }

                                    var allSuccess = true
                                    var lastMessage = ""
                                    var lastUploadPath = ""

                                    if (isChapterWise) {
                                        val sets = if (questionsWithGithubImages.size > 10) questionsWithGithubImages.chunked(10) else listOf(questionsWithGithubImages)
                                        sets.forEachIndexed { idx, chunk ->
                                            val setNum = idx + 1
                                            val setTitle = "$selectedChapterTitle - Set $setNum"
                                            val setFileName = "${cleanTopic}_${selectedYear}_set_${setNum}.json"
                                            val setFullPath = "tests/$sanitizedExamName/chapter_wise/$sanitizedSubject/$setFileName"
                                            lastUploadPath = setFullPath

                                            val setJsonContent = GeminiQuestionParserService.exportToGitHubJson(
                                                testTitle = setTitle,
                                                year = selectedYear,
                                                questions = chunk,
                                                subExamId = selectedSubExamId,
                                                subjectKey = selectedSubjectKey,
                                                testType = "CHAPTER_WISE",
                                                postType = selectedPostType
                                            )

                                            val uploadRes = GitHubSyncService.uploadJsonFile(
                                                config = gitHubConfig,
                                                filePath = setFullPath,
                                                jsonString = setJsonContent,
                                                commitMessage = "Add Set $setNum for $selectedChapterTitle ($selectedYear) to $setFullPath"
                                            )
                                            if (!uploadRes.isSuccess) {
                                                allSuccess = false
                                                lastMessage = uploadRes.message
                                            }
                                        }
                                    } else {
                                        val fileName = "${sanitizedExamName}_${sanitizedPost}_${selectedYear}_${cleanTitle}.json"
                                        val fullPath = "tests/$sanitizedExamName/full_tests/$fileName"
                                        lastUploadPath = fullPath

                                        val jsonContent = GeminiQuestionParserService.exportToGitHubJson(
                                            testTitle = testTitle,
                                            year = selectedYear,
                                            questions = questionsWithGithubImages,
                                            subExamId = selectedSubExamId,
                                            subjectKey = selectedSubjectKey,
                                            testType = selectedTestMode,
                                            postType = selectedPostType
                                        )

                                        val uploadRes = GitHubSyncService.uploadJsonFile(
                                            config = gitHubConfig,
                                            filePath = fullPath,
                                            jsonString = jsonContent,
                                            commitMessage = "Add Full Test: $testTitle ($selectedYear) to $fullPath"
                                        )
                                        if (!uploadRes.isSuccess) {
                                            allSuccess = false
                                            lastMessage = uploadRes.message
                                        }
                                    }

                                    isUploadingToGithub = false

                                    if (allSuccess) {
                                        Toast.makeText(context, "🎉 बधाई! प्रत्येक सेट की अलग JSON फाइल GitHub ($lastUploadPath) में सेव हो गई!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "⚠️ GitHub अपलोड: $lastMessage", Toast.LENGTH_LONG).show()
                                    }

                                    if (allSuccess) {
                                        val stableTestId = "custom_ai_${selectedSubExamId}_${selectedPostType.lowercase()}_${selectedYear}_${System.currentTimeMillis() % 10000000}_${Math.abs(testTitle.trim().hashCode()) % 1000}"
                                        if (isChapterWise) {
                                            val createdSets = com.example.model.CustomChapterRepository.addChapterWithSets(
                                                subExamId = selectedSubExamId,
                                                subjectKey = selectedSubjectKey,
                                                subjectTitle = selectedSubjectTitle,
                                                baseChapterTitle = selectedChapterTitle,
                                                allQuestions = questionsWithGithubImages,
                                                durationPerSet = 10,
                                                difficulty = "मध्यम",
                                                year = selectedYear,
                                                context = context
                                            )
                                            createdSets.forEach { chap ->
                                                val paper = com.example.model.TestPaper(
                                                    id = chap.id,
                                                    title = chap.title,
                                                    totalQuestions = chap.questions.size,
                                                    durationMinutes = chap.durationMinutes,
                                                    totalMarks = chap.totalMarks,
                                                    difficulty = chap.difficulty,
                                                    year = selectedYear,
                                                    questions = chap.questions
                                                )
                                                com.example.model.DynamicTestRepository.addCustomTest(
                                                    categoryId = selectedCategory,
                                                    subExamId = selectedSubExamId,
                                                    postType = selectedPostType,
                                                    year = selectedYear,
                                                    testType = "CHAPTER_WISE",
                                                    testPaper = paper,
                                                    context = context,
                                                    persistToDisk = true
                                                )
                                                com.example.model.OfflineTestRepository.cacheTestQuestions(context, chap.title, chap.questions)
                                                com.example.model.OfflineTestRepository.cacheTestQuestions(context, chap.id, chap.questions)
                                            }
                                            addedToLiveApp = true
                                        } else {
                                            val newPaper = com.example.model.TestPaper(
                                                id = stableTestId,
                                                title = testTitle,
                                                totalQuestions = questionsWithGithubImages.size,
                                                durationMinutes = durationMinutes,
                                                totalMarks = totalMarks,
                                                difficulty = "ऑफिशियल पैटर्न",
                                                year = selectedYear,
                                                questions = questionsWithGithubImages
                                            )
                                            com.example.model.DynamicTestRepository.addCustomTest(
                                                categoryId = selectedCategory,
                                                subExamId = selectedSubExamId,
                                                postType = selectedPostType,
                                                year = selectedYear,
                                                testType = selectedTestMode,
                                                testPaper = newPaper,
                                                context = context,
                                                persistToDisk = true
                                            )
                                            com.example.model.OfflineTestRepository.cacheTestQuestions(context, testTitle, questionsWithGithubImages)
                                            com.example.model.OfflineTestRepository.cacheTestQuestions(context, stableTestId, questionsWithGithubImages)
                                            addedToLiveApp = true
                                        }
                                        com.example.model.CustomChapterRepository.invalidateFullSubjectsCache()
                                        com.example.model.RealtimeExamStatsManager.invalidateCache()

                                        Toast.makeText(context, "🎉 बधाई! प्रत्येक सेट की अलग JSON फाइल GitHub फोल्डर में सेव हो गई और ऐप में लाइव हो गई!", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "GitHub एरर: $lastMessage", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onAddToLiveApp = {
                                if (parsedQuestions.isNotEmpty()) {
                                    val stableTestId = "custom_ai_${selectedSubExamId}_${selectedPostType.lowercase()}_${selectedYear}_${System.currentTimeMillis() % 10000000}_${Math.abs(testTitle.trim().hashCode()) % 1000}"
                                    
                                    // Save any base64 diagrams to persistent offline files on device storage
                                    val offlineImagesDir = java.io.File(context.filesDir, "offline_images")
                                    if (!offlineImagesDir.exists()) offlineImagesDir.mkdirs()

                                    val finalizedQuestions = parsedQuestions.map { q ->
                                        val img = q.imageUrl
                                        if (!img.isNullOrBlank() && (img.startsWith("data:image", true) || (!img.startsWith("http") && !img.startsWith("/") && img.length > 100))) {
                                            try {
                                                val clean = if (img.contains(",")) img.substringAfter(",") else img
                                                val bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
                                                val file = java.io.File(offlineImagesDir, "diagram_${stableTestId}_q${q.id}.jpg")
                                                file.writeBytes(bytes)
                                                q.copy(imageUrl = file.absolutePath)
                                            } catch (_: Exception) {
                                                q
                                            }
                                        } else {
                                            q
                                        }
                                    }

                                     if (selectedTestMode == "CHAPTER_WISE") {
                                        val createdSets = CustomChapterRepository.addChapterWithSets(
                                            subExamId = selectedSubExamId,
                                            subjectKey = selectedSubjectKey,
                                            subjectTitle = selectedSubjectTitle,
                                            baseChapterTitle = selectedChapterTitle,
                                            allQuestions = finalizedQuestions,
                                            durationPerSet = 10,
                                            difficulty = "मध्यम",
                                            year = selectedYear,
                                            context = context
                                        )
                                        createdSets.forEach { chap ->
                                            val paper = TestPaper(
                                                id = chap.id,
                                                title = chap.title,
                                                totalQuestions = chap.questions.size,
                                                durationMinutes = chap.durationMinutes,
                                                totalMarks = chap.totalMarks,
                                                difficulty = chap.difficulty,
                                                year = selectedYear,
                                                questions = chap.questions
                                            )
                                            DynamicTestRepository.addCustomTest(
                                                categoryId = selectedCategory,
                                                subExamId = selectedSubExamId,
                                                postType = selectedPostType,
                                                year = selectedYear,
                                                testType = "CHAPTER_WISE",
                                                testPaper = paper,
                                                context = context,
                                                persistToDisk = true
                                            )
                                            com.example.model.OfflineTestRepository.cacheTestQuestions(context, chap.title, chap.questions)
                                            com.example.model.OfflineTestRepository.cacheTestQuestions(context, chap.id, chap.questions)
                                        }
                                        addedToLiveApp = true
                                        val numSets = if (finalizedQuestions.isEmpty()) 1 else (finalizedQuestions.size + 9) / 10
                                        Toast.makeText(context, "🎉 शानदार! अध्याय '$selectedChapterTitle' में 10-10 प्रश्नों के सभी $numSets सेट सफलतापूर्वक जुड़ गए!", Toast.LENGTH_LONG).show()
                                    } else {
                                        val newPaper = TestPaper(
                                            id = stableTestId,
                                            title = testTitle,
                                            totalQuestions = finalizedQuestions.size,
                                            durationMinutes = durationMinutes,
                                            totalMarks = totalMarks,
                                            difficulty = "ऑफिशियल पैटर्न",
                                            year = selectedYear,
                                            questions = finalizedQuestions
                                        )
                                        DynamicTestRepository.addCustomTest(
                                            categoryId = selectedCategory,
                                            subExamId = selectedSubExamId,
                                            postType = selectedPostType,
                                            year = selectedYear,
                                            testType = selectedTestMode,
                                            testPaper = newPaper,
                                            context = context,
                                            persistToDisk = true
                                        )
                                        com.example.model.OfflineTestRepository.cacheTestQuestions(context, testTitle, finalizedQuestions)
                                        com.example.model.OfflineTestRepository.cacheTestQuestions(context, stableTestId, finalizedQuestions)
                                        addedToLiveApp = true
                                        Toast.makeText(context, "🎉 टेस्ट ऐप में जोड़ दिया गया है! अब आप इसे सीधे दे सकते हैं।", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onCopyGitHubCode = {
                                val code = GeminiQuestionParserService.exportToGitHubKotlinCode(testTitle, selectedYear, parsedQuestions)
                                clipboardManager.setText(AnnotatedString(code))
                                Toast.makeText(context, "📋 GitHub Kotlin कोड क्लिपबोर्ड पर कॉपी हो गया!", Toast.LENGTH_SHORT).show()
                            },
                            onCopyJson = {
                                val json = GeminiQuestionParserService.exportToGitHubJson(
                                    testTitle = testTitle,
                                    year = selectedYear,
                                    questions = parsedQuestions,
                                    subExamId = selectedSubExamId,
                                    subjectKey = selectedSubjectKey,
                                    testType = selectedTestMode,
                                    postType = selectedPostType
                                )
                                clipboardManager.setText(AnnotatedString(json))
                                Toast.makeText(context, "📋 GitHub JSON क्लिपबोर्ड पर कॉपी हो गया!", Toast.LENGTH_SHORT).show()
                            },
                            onLaunchTest = {
                                val offlineImagesDir = java.io.File(context.filesDir, "offline_images")
                                if (!offlineImagesDir.exists()) offlineImagesDir.mkdirs()
                                val stableTestId = "preview_${System.currentTimeMillis()}"

                                val finalized = parsedQuestions.map { q ->
                                    val img = q.imageUrl
                                    if (!img.isNullOrBlank() && (img.startsWith("data:image", true) || (!img.startsWith("http") && !img.startsWith("/") && img.length > 100))) {
                                        try {
                                            val clean = if (img.contains(",")) img.substringAfter(",") else img
                                            val bytes = android.util.Base64.decode(clean.trim(), android.util.Base64.DEFAULT)
                                            val file = java.io.File(offlineImagesDir, "diagram_${stableTestId}_q${q.id}.jpg")
                                            file.writeBytes(bytes)
                                            q.copy(imageUrl = file.absolutePath)
                                        } catch (_: Throwable) {
                                            q
                                        }
                                    } else {
                                        q
                                    }
                                }

                                com.example.model.OfflineTestRepository.cacheTestQuestions(context, testTitle, finalized)
                                val test = TestPaper(
                                    id = stableTestId,
                                    title = testTitle,
                                    totalQuestions = finalized.size,
                                    durationMinutes = durationMinutes,
                                    totalMarks = totalMarks,
                                    difficulty = "ऑफिशियल पैटर्न",
                                    year = selectedYear,
                                    questions = finalized
                                )
                                com.example.model.OfflineTestRepository.cacheTestQuestions(context, test.id, finalized)
                                onLaunchTest(test)
                            }
                        )
                    }
                }
            }

            // PDF Loading Overlay Dialog
            if (isPdfLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF2563EB),
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Text(
                                text = "📄 PDF फाइल लोड हो रही है...",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = pdfLoadingMessage,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            androidx.compose.material3.LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF2563EB),
                                trackColor = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }

            // Live Processing Overlay Dialog / Banner
            if (isProcessing || isUploadingToGithub) {
                val currentPct = processProgressPercent.coerceIn(5, 100)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 400.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Circular Progress with bold percentage
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { currentPct / 100f },
                                    color = Color(0xFF2563EB),
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(64.dp),
                                    trackColor = Color(0xFFDBEAFE)
                                )
                                Text(
                                    text = "$currentPct%",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8)
                                )
                            }

                            Text(
                                text = if (isUploadingToGithub) "🚀 GitHub अपलोड प्रगति पर है" else "🤖 AI प्रश्न एक्सट्रैक्शन जारी है",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )

                            // Current step / message with clean styling and wrapping
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = processingMessage.ifBlank { "कृपया प्रतीक्षा करें..." },
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155),
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            // Linear progress bar and clean info labels
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { currentPct / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFF2563EB),
                                    trackColor = Color(0xFFE2E8F0)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "पूर्ण: $currentPct%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB)
                                    )
                                    Text(
                                        text = "⏱️ समय: ${elapsedSeconds}s",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // 🌐 Background Processing Mode Badge
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "📱 बैकग्राउंड सेवा चालू: आप ऐप मिनिमाइज़ कर सकते हैं, नोटिफिकेशन बार में लाइव प्रोग्रेस दिखेगी!",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF15803D),
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 🔒 Lock Screen Button
                                Surface(
                                    onClick = { isScreenLocked = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0F172A),
                                    shadowElevation = 3.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("lock_screen_processing_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Lock Screen",
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "स्क्रीन लॉक",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // ⏹️ Cancel Button
                                Surface(
                                    onClick = {
                                        com.example.service.AiBackgroundExecutionManager.cancelTask(context)
                                        isProcessing = false
                                        isUploadingToGithub = false
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                    modifier = Modifier.testTag("cancel_ai_task_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cancel Task",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "रोकें",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 🛡️ FULL-SCREEN TOUCH-SHIELD OVERLAY (When Screen is Locked)
            if (isScreenLocked && (isProcessing || isUploadingToGithub)) {
                val currentPct = processProgressPercent.coerceIn(5, 100)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.94f))
                        .pointerInput(Unit) {
                            // Block all touch events from passing through to underlying UI
                            awaitPointerEventScope {
                                while (true) {
                                    awaitPointerEvent()
                                }
                            }
                        }
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 380.dp)
                    ) {
                        // Glowing animated Lock badge
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.18f))
                                .border(2.dp, Color(0xFFF59E0B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Text(
                            text = "🔒 स्क्रीन सुरक्षित लॉक है",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "AI बैकग्राउंड में PDF प्रोसेस कर रहा है। गलती से स्क्रीन पर हाथ लगने से प्रोसेस नहीं रुकेगा।",
                            fontSize = 13.sp,
                            color = Color(0xFFCBD5E1),
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )

                        // Progress card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚡ AI प्रगति:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF93C5FD)
                                    )
                                    Text(
                                        text = "$currentPct% (${elapsedSeconds}s)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF60A5FA)
                                    )
                                }

                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { currentPct / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFF3B82F6),
                                    trackColor = Color(0xFF334155)
                                )

                                Text(
                                    text = processingMessage.ifBlank { "कृपया प्रतीक्षा करें..." },
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 16.sp,
                                    maxLines = 3
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Manual Unlock Button (Extra small phone UI size button)
                        Surface(
                            onClick = { isScreenLocked = false },
                            shape = CircleShape,
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            modifier = Modifier.testTag("manual_unlock_screen_btn")
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = "Unlock",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "अनलॉक",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }

                        Text(
                            text = "💡 प्रोसेस पूरा होते ही स्क्रीन ऑटोमैटिक अनलॉक हो जाएगी।",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// STEP 1: CATEGORY SELECTION
// ==========================================
@Composable
fun Step1CategorySelection(
    selectedCategory: String,
    onSelect: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "1. मुख्य परीक्षा श्रेणी (Category) चुनें",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "जिस परीक्षा में नया टेस्ट जोड़ना है, उस पर क्लिक करें:",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        val categories = listOf(
            Triple("SSC Exams", "CGL, CHSL, Delhi Police, GD, MTS व अन्य", Color(0xFF2563EB)),
            Triple("Railway Exams", "RRB NTPC, Group D, ALP, RPF Constable", Color(0xFFDC2626)),
            Triple("State Exams", "UP Police, Rajasthan CET, Bihar SI, MP Police", Color(0xFF059669))
        )

        items(categories) { (cat, desc, color) ->
            Card(
                onClick = { onSelect(cat) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(
                    if (selectedCategory == cat) 2.dp else 1.dp,
                    if (selectedCategory == cat) color else Color(0xFFE2E8F0)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(color.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (cat) {
                                    "SSC Exams" -> Icons.Default.School
                                    "Railway Exams" -> Icons.Default.DirectionsRailway
                                    else -> Icons.Default.LocationCity
                                },
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column {
                            Text(
                                text = cat,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = desc,
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// STEP 2: SUB-EXAM SELECTION
// ==========================================
@Composable
fun Step2SubExamSelection(
    category: String,
    selectedExam: String,
    onSelect: (String, String) -> Unit
) {
    val examList = when (category) {
        "Railway Exams" -> listOf(
            Pair("RRB NTPC (Graduate & Under-Graduate)", "rrb_ntpc"),
            Pair("RRB Group D", "rrb_group_d"),
            Pair("RRB ALP & Technician", "rrb_alp"),
            Pair("RPF Constable & SI", "rpf_constable")
        )
        "State Exams" -> listOf(
            Pair("UP Police Constable & SI", "up_police"),
            Pair("Rajasthan CET & Police", "raj_cet"),
            Pair("Bihar Police SI & Constable", "bihar_police"),
            Pair("MP Police Constable", "mp_police")
        )
        else -> listOf(
            Pair("Delhi Police (कांस्टेबल व हेड कांस्टेबल)", "delhi_police"),
            Pair("SSC CGL (कंबाइंड ग्रेजुएट लेवल)", "ssc_cgl"),
            Pair("SSC CHSL (10+2 लेवल)", "ssc_chsl"),
            Pair("SSC GD Constable", "ssc_gd"),
            Pair("SSC MTS & Havaldar", "ssc_mts"),
            Pair("SSC CPO SI", "ssc_cpo")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "2. $category के अंतर्गत परीक्षा चुनें",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "जिस परीक्षा में आप आंसर की का टेस्ट जोड़ना चाहते हैं:",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(examList) { (examName, examId) ->
            Card(
                onClick = { onSelect(examName, examId) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(
                    if (selectedExam == examName) 2.dp else 1.dp,
                    if (selectedExam == examName) Color(0xFF2563EB) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Text(
                            text = examName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// STEP 3: TEST MODE SELECTION
// ==========================================
@Composable
fun Step3TestModeSelection(
    selectedMode: String,
    onSelect: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "3. टेस्ट का प्रकार (Test Mode) चुनें",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "आप फुल टेस्ट पेपर जोड़ना चाहते हैं या चैप्टर वाइज?",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        item {
            Card(
                onClick = { onSelect("FULL_TEST") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(2.dp, Color(0xFF2563EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFDBEAFE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Full Test (फुल मॉक टेस्ट)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "100 प्रश्न, 90 मिनट, नेगेटिव मार्किंग व संपूर्ण ऑल इंडिया परीक्षा पैटर्न",
                            fontSize = 12.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF2563EB)
                    )
                }
            }
        }

        item {
            Card(
                onClick = { onSelect("CHAPTER_WISE") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chapter Wise (चैप्टर वाइज टेस्ट)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "GK, रीजनिंग, गणित, इंग्लिश या कंप्यूटर के विषयवार प्रश्न सेट",
                            fontSize = 12.5.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF059669)
                    )
                }
            }
        }
    }
}

// ==========================================
// STEP 4: POST & YEAR SELECTION
// ==========================================
@Composable
fun Step4PostAndYearSelection(
    subExam: String,
    selectedPost: String,
    selectedYear: String,
    onSelect: (String, String) -> Unit
) {
    var postState by remember { mutableStateOf(selectedPost) }
    var yearState by remember { mutableStateOf(selectedYear) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val normSub = subExam.lowercase()
            val isTierExam = normSub.contains("cgl") || normSub.contains("chsl") || normSub.contains("cpo") || normSub.contains("ntpc") || normSub.contains("alp") || normSub.contains("je")
            Text(
                text = if (isTierExam) "4. टियर / स्टेज (Stage / Tier) व परीक्षा वर्ष चुनें" else "4. पद (Post) व परीक्षा वर्ष (Year) चुनें",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = if (isTierExam) "परीक्षा का टियर/स्टेज चुनें:" else "जैसे टेस्ट में कांस्टेबल और हेड कांस्टेबल के बटन थे:",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Post / Tier Selection
        item {
            val normSub = subExam.lowercase()
            val isTierExam = normSub.contains("cgl") || normSub.contains("chsl") || normSub.contains("cpo") || normSub.contains("ntpc") || normSub.contains("alp") || normSub.contains("je")
            val isRailway = normSub.contains("ntpc") || normSub.contains("alp") || normSub.contains("je")
            val isCpo = normSub.contains("cpo")

            Text(
                text = if (isTierExam) "स्टेज / टियर (Stage / Tier)" else "पद (Post Type)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (isTierExam) {
                val stage1Key = if (isRailway) "CBT_1" else "TIER_1"
                val stage2Key = if (isRailway) "CBT_2" else "TIER_2"
                val stage1Label = when {
                    isRailway -> "CBT 1 (Tier 1)"
                    isCpo -> "Paper 1 (Tier 1)"
                    else -> "Tier 1 (टियर-1)"
                }
                val stage2Label = when {
                    isRailway -> "CBT 2 (Tier 2)"
                    isCpo -> "Paper 2 (Tier 2)"
                    else -> "Tier 2 (टियर-2)"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        onClick = { postState = stage1Key },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (postState == stage1Key || postState == "TIER_1" || postState == "CBT_1") Color(0xFFEFF6FF) else Color.White
                        ),
                        border = BorderStroke(
                            if (postState == stage1Key || postState == "TIER_1" || postState == "CBT_1") 2.dp else 1.dp,
                            if (postState == stage1Key || postState == "TIER_1" || postState == "CBT_1") Color(0xFF2563EB) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stage1Label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (postState == stage1Key || postState == "TIER_1" || postState == "CBT_1") Color(0xFF1D4ED8) else Color(0xFF1E293B)
                            )
                        }
                    }

                    Card(
                        onClick = { postState = stage2Key },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (postState == stage2Key || postState == "TIER_2" || postState == "CBT_2") Color(0xFFEFF6FF) else Color.White
                        ),
                        border = BorderStroke(
                            if (postState == stage2Key || postState == "TIER_2" || postState == "CBT_2") 2.dp else 1.dp,
                            if (postState == stage2Key || postState == "TIER_2" || postState == "CBT_2") Color(0xFF2563EB) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stage2Label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (postState == stage2Key || postState == "TIER_2" || postState == "CBT_2") Color(0xFF1D4ED8) else Color(0xFF1E293B)
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        onClick = { postState = "CONSTABLE" },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (postState == "CONSTABLE") Color(0xFFEFF6FF) else Color.White
                        ),
                        border = BorderStroke(
                            if (postState == "CONSTABLE") 2.dp else 1.dp,
                            if (postState == "CONSTABLE") Color(0xFF2563EB) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Constable",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (postState == "CONSTABLE") Color(0xFF1D4ED8) else Color(0xFF1E293B)
                            )
                        }
                    }

                    Card(
                        onClick = { postState = "HEAD_CONSTABLE" },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (postState == "HEAD_CONSTABLE") Color(0xFFEFF6FF) else Color.White
                        ),
                        border = BorderStroke(
                            if (postState == "HEAD_CONSTABLE") 2.dp else 1.dp,
                            if (postState == "HEAD_CONSTABLE") Color(0xFF2563EB) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Head Const.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (postState == "HEAD_CONSTABLE") Color(0xFF1D4ED8) else Color(0xFF1E293B)
                            )
                        }
                    }

                    Card(
                        onClick = { postState = "SI" },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (postState == "SI") Color(0xFFF3E8FF) else Color.White
                        ),
                        border = BorderStroke(
                            if (postState == "SI") 2.dp else 1.dp,
                            if (postState == "SI") Color(0xFF7C3AED) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "SI (उप निरीक्षक)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (postState == "SI") Color(0xFF7C3AED) else Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }
        }

        // Year Selection
        item {
            Text(
                text = "परीक्षा वर्ष (Year)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020").forEach { year ->
                    Surface(
                        onClick = { yearState = year },
                        shape = RoundedCornerShape(10.dp),
                        color = if (yearState == year) Color(0xFF2563EB) else Color.White,
                        border = BorderStroke(
                            if (yearState == year) 2.dp else 1.dp,
                            if (yearState == year) Color(0xFF1D4ED8) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = year,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (yearState == year) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { onSelect(postState, yearState) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "आगे बढ़ें (Next to AI Studio) ➔",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ==========================================
// STEP 4 (CHAPTER WISE): SUBJECT & CHAPTER SELECTION
// ==========================================
@Composable
fun Step4ChapterWiseSelection(
    category: String,
    subExam: String,
    subExamId: String,
    selectedSubjectKey: String,
    selectedChapter: String,
    selectedYear: String,
    onSelect: (subjectKey: String, subjectTitle: String, chapterTitle: String, year: String) -> Unit
) {
    val context = LocalContext.current
    var subjectKeyState by remember(selectedSubjectKey) { mutableStateOf(selectedSubjectKey) }
    var chapterTitleState by remember(selectedChapter) { mutableStateOf(selectedChapter) }
    var yearState by remember(selectedYear) { mutableStateOf(selectedYear) }
    var newSubjectCustomName by remember { mutableStateOf("") }
    var showAddSubjectInput by remember { mutableStateOf(false) }

    // Retrieve subjects for this subExam
    val subjects = remember(subExamId, CustomChapterRepository.revision) {
        val list = CustomChapterRepository.getFullSubjectsForExam(subExamId, context)
        if (list.isEmpty()) {
            listOf(
                ExamSubjectCategory("MATHS", "गणित / Maths", "Maths", CustomChapterRepository.getIconVector("MATHS"), listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)), emptyList()),
                ExamSubjectCategory("REASONING", "रीजनिंग / Reasoning", "Reasoning", CustomChapterRepository.getIconVector("REASONING"), listOf(Color(0xFF7C3AED), Color(0xFF6D28D9)), emptyList()),
                ExamSubjectCategory("GK", "सामान्य ज्ञान / GK", "GK", CustomChapterRepository.getIconVector("GK"), listOf(Color(0xFF059669), Color(0xFF047857)), emptyList()),
                ExamSubjectCategory("COMPUTER", "कंप्यूटर / Computer", "Computer", CustomChapterRepository.getIconVector("COMPUTER"), listOf(Color(0xFFEA580C), Color(0xFFC2410C)), emptyList()),
                ExamSubjectCategory("HINDI", "हिंदी / Hindi", "Hindi", CustomChapterRepository.getIconVector("HINDI"), listOf(Color(0xFFD97706), Color(0xFFB45309)), emptyList()),
                ExamSubjectCategory("ENGLISH", "अंग्रेजी / English", "English", CustomChapterRepository.getIconVector("ENGLISH"), listOf(Color(0xFF0284C7), Color(0xFF0369A1)), emptyList())
            )
        } else list
    }

    val activeSubject = subjects.find { it.key.equals(subjectKeyState, ignoreCase = true) }
        ?: subjects.firstOrNull()
        ?: ExamSubjectCategory("MATHS", "गणित / Maths", "Maths", CustomChapterRepository.getIconVector("MATHS"), listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)), emptyList())

    // Quick suggestions for chapter names for this subject
    val existingChapters = remember(activeSubject, CustomChapterRepository.revision) {
        activeSubject.chapters.map { it.title }.distinct().take(12)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "4. विषय व अध्याय (Subject & Chapter) चुनें",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "$category • $subExam के लिए चैप्टर वाइज टेस्ट:",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
        }

        // 1. Subject Selection
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "1. विषय चुनें (Select Subject):",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                TextButton(
                    onClick = { showAddSubjectInput = !showAddSubjectInput },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (showAddSubjectInput) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Color(0xFF2563EB)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (showAddSubjectInput) "रद्द करें" else "नया विषय जोड़ें",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB)
                    )
                }
            }

            if (showAddSubjectInput) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newSubjectCustomName,
                            onValueChange = { newSubjectCustomName = it },
                            placeholder = { Text("नया विषय नाम (उदा. सामान्य विज्ञान)", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val trimmed = newSubjectCustomName.trim()
                                if (trimmed.isNotBlank()) {
                                    val newSub = CustomChapterRepository.addSubject(
                                        subExamId = subExamId,
                                        name = trimmed,
                                        englishName = trimmed,
                                        iconType = "BOOK",
                                        context = context
                                    )
                                    subjectKeyState = newSub.key
                                    newSubjectCustomName = ""
                                    showAddSubjectInput = false
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("जोड़ें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subjects.size) { idx ->
                    val sub = subjects[idx]
                    val isSel = subjectKeyState.equals(sub.key, ignoreCase = true)
                    Surface(
                        onClick = { subjectKeyState = sub.key },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) Color(0xFF0F172A) else Color.White,
                        border = BorderStroke(
                            if (isSel) 2.dp else 1.dp,
                            if (isSel) Color(0xFF2563EB) else Color(0xFFCBD5E1)
                        ),
                        shadowElevation = if (isSel) 3.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = sub.icon,
                                contentDescription = null,
                                tint = if (isSel) Color.White else Color(0xFF334155),
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = sub.name,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else Color(0xFF334155)
                            )
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }

        // 2. Chapter / Topic Name
        item {
            Text(
                text = "2. अध्याय / टॉपिक का नाम (Chapter Name):",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = chapterTitleState,
                onValueChange = { chapterTitleState = it },
                placeholder = { Text("उदा. प्रतिशत (Percentage) या कोडिंग-डिकोडिंग") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.5.sp, fontWeight = FontWeight.Medium),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_chapter_name_input")
            )

            if (existingChapters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 ${activeSubject.name} के सुझाव (Click to choose):",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(existingChapters.size) { cIdx ->
                        val chap = existingChapters[cIdx]
                        Surface(
                            onClick = { chapterTitleState = chap },
                            shape = RoundedCornerShape(8.dp),
                            color = if (chapterTitleState == chap) Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (chapterTitleState == chap) Color(0xFF3B82F6) else Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = chap,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (chapterTitleState == chap) Color(0xFF1D4ED8) else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. AI 10-Question Sets Rule Explanatory Card
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.5.dp, Color(0xFF86EFAC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF16A34A),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "🎯 10-10 प्रश्नों के सेट्स का AI नियम",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14532D)
                        )
                    }

                    Text(
                        text = "• जब आप अगले स्टेप में इस अध्याय के लिए कोई PDF (जैसे 60 प्रश्नों की PDF) देंगे, तो AI अपने आप हर 10 प्रश्नों का अलग-अलग सेट (Set 1, Set 2, Set 3, Set 4, Set 5, Set 6...) बनाकर इस चैप्टर में व्यवस्थित कर देगा।\n• प्रत्येक सेट 10 प्रश्न, 10 मिनट व विस्तृत हल सहित तैयार होगा।",
                        fontSize = 12.sp,
                        color = Color(0xFF15803D),
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // 3. Year Selection
        item {
            Text(
                text = "परीक्षा वर्ष (Year)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020").forEach { year ->
                    Surface(
                        onClick = { yearState = year },
                        shape = RoundedCornerShape(10.dp),
                        color = if (yearState == year) Color(0xFF2563EB) else Color.White,
                        border = BorderStroke(
                            if (yearState == year) 2.dp else 1.dp,
                            if (yearState == year) Color(0xFF1D4ED8) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = year,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (yearState == year) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }

        // Next Button
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = {
                    val finalChapter = chapterTitleState.trim().ifBlank { "अध्याय 1" }
                    onSelect(activeSubject.key, activeSubject.name, finalChapter, yearState)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = "आगे बढ़ें (Next to AI PDF Studio) ➔",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ==========================================
// STEP 5: AI STUDIO (PDF & ANSWER KEY PARSER + GITHUB SYNC)
// ==========================================
@Composable
fun Step5AiStudio(
    category: String,
    subExam: String,
    subExamId: String,
    testMode: String,
    postType: String,
    year: String,
    subjectKey: String = "MATHS",
    subjectTitle: String = "गणित / Math",
    chapterTitle: String = "प्रतिशत (Percentage)",
    testTitle: String,
    onTitleChange: (String) -> Unit,
    rawText: String,
    onTextChange: (String) -> Unit,
    uploadedFileName: String?,
    pdfTotalPages: Int = 0,
    onPickPdf: () -> Unit,
    onLoadSample: () -> Unit,
    isProcessing: Boolean,
    isUploadingToGithub: Boolean,
    processingMessage: String,
    parsedQuestions: List<QuizQuestion>,
    onQuestionsChange: ((List<QuizQuestion>) -> Unit)? = null,
    onClearAll: (() -> Unit)? = null,
    addedToLiveApp: Boolean,
    gitHubConfig: GitHubConfig,
    gitHubUploadResult: GitHubUploadResult?,
    onSaveGitHubConfig: (GitHubConfig) -> Unit,
    onProcessAi: () -> Unit,
    onAutoParseAndPushToGitHub: () -> Unit,
    onPushOnlyToGitHub: () -> Unit,
    onAddToLiveApp: () -> Unit,
    onCopyGitHubCode: () -> Unit,
    onCopyJson: () -> Unit,
    onLaunchTest: () -> Unit
) {
    var showGithubDialog by remember { mutableStateOf(false) }
    var showGithubSettings by remember { mutableStateOf(false) }
    var showAiRulesDialog by remember { mutableStateOf(false) }
    var showApiKeyManagerDialog by remember { mutableStateOf(false) }
    var editingQuestionIndex by remember { mutableStateOf<Int?>(null) }
    val isChapterWise = testMode == "CHAPTER_WISE"
    val questionSets = remember(parsedQuestions, isChapterWise) {
        if (parsedQuestions.isEmpty()) emptyList()
        else if (isChapterWise) parsedQuestions.chunked(10)
        else listOf(parsedQuestions)
    }
    var selectedSetIndex by remember { mutableIntStateOf(-1) }
    var tempOwner by remember(gitHubConfig) { mutableStateOf(gitHubConfig.owner) }
    var tempRepo by remember(gitHubConfig) { mutableStateOf(gitHubConfig.repo) }
    var tempBranch by remember(gitHubConfig) { mutableStateOf(gitHubConfig.branch.ifBlank { "main" }) }
    var tempToken by remember(gitHubConfig) { mutableStateOf(gitHubConfig.token) }
    var tempFolder by remember(gitHubConfig) { mutableStateOf(gitHubConfig.folderPath.ifBlank { "tests" }) }
    var previewImageForDialog by remember { mutableStateOf<String?>(null) }
    var cropTargetQuestionIndex by remember { mutableStateOf<Int?>(null) }
    var cropTargetImageSource by remember { mutableStateOf<String?>(null) }
    var addDiagramTargetQuestionIndex by remember { mutableStateOf<Int?>(null) }
    var deleteDiagramConfirmQuestionIndex by remember { mutableStateOf<Int?>(null) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    val addDiagramPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && addDiagramTargetQuestionIndex != null) {
            val qIdx = addDiagramTargetQuestionIndex!!
            if (qIdx in parsedQuestions.indices) {
                cropTargetQuestionIndex = qIdx
                cropTargetImageSource = uri.toString()
            }
        }
        addDiagramTargetQuestionIndex = null
    }

    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    previewImageForDialog?.let { fullImgUrl ->
        Dialog(
            onDismissRequest = { previewImageForDialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(16.dp)),
                color = Color.White,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🔍 संपूर्ण प्रश्न आकृति (Full HD View)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "पूरी आकृति और सभी विकल्प स्पष्ट देखें",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        IconButton(onClick = { previewImageForDialog = null }) {
                            Icon(Icons.Default.Close, contentDescription = "बंद करें", tint = Color(0xFF475569))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC)),
                        contentAlignment = Alignment.Center
                    ) {
                        val fullBitmap = remember(fullImgUrl) {
                            when {
                                fullImgUrl.startsWith("data:image", true) || (!fullImgUrl.startsWith("http") && !fullImgUrl.startsWith("/") && fullImgUrl.length > 50) -> {
                                    try {
                                        val clean = if (fullImgUrl.contains(",")) fullImgUrl.substringAfter(",") else fullImgUrl
                                        val bytes = android.util.Base64.decode(clean.trim(), android.util.Base64.DEFAULT)
                                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    } catch (_: Exception) { null }
                                }
                                fullImgUrl.startsWith("/") && java.io.File(fullImgUrl).exists() -> {
                                    try {
                                        android.graphics.BitmapFactory.decodeFile(fullImgUrl)
                                    } catch (_: Exception) { null }
                                }
                                else -> {
                                    try {
                                        val cached = com.example.model.OfflineTestRepository.getOfflineImageFile(context, fullImgUrl)
                                        if (cached != null) android.graphics.BitmapFactory.decodeFile(cached.absolutePath) else null
                                    } catch (_: Exception) { null }
                                }
                            }
                        }
                        if (fullBitmap != null) {
                            Image(
                                bitmap = fullBitmap.asImageBitmap(),
                                contentDescription = "Full Question Diagram",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        } else {
                            coil.compose.AsyncImage(
                                model = fullImgUrl,
                                contentDescription = "Full Question Diagram",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { previewImageForDialog = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ठीक है (Done)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showGithubDialog) {
        GitHubConfigDialog(
            initialConfig = gitHubConfig,
            onDismiss = { showGithubDialog = false },
            onSaveConfig = {
                onSaveGitHubConfig(it)
                tempOwner = it.owner
                tempRepo = it.repo
                tempBranch = it.branch
                tempToken = it.token
                tempFolder = it.folderPath
            }
        )
    }

    if (showAiRulesDialog) {
        com.example.ui.components.AdminAiPromptRulesDialog(
            initialMode = testMode,
            onDismiss = { showAiRulesDialog = false },
            onRulesSaved = {
                Toast.makeText(context, "AI के नियम सुरक्षित हो गए!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showApiKeyManagerDialog) {
        com.example.ui.components.AdminApiKeyManagerDialog(
            onDismiss = { showApiKeyManagerDialog = false }
        )
    }

    editingQuestionIndex?.let { editIdx ->
        if (editIdx in parsedQuestions.indices) {
            val qToEdit = parsedQuestions[editIdx]
            QuestionEditDialog(
                question = qToEdit,
                index = editIdx,
                onDismiss = { editingQuestionIndex = null },
                onSave = { updatedQ ->
                    val updatedList = parsedQuestions.toMutableList().also { list ->
                        list[editIdx] = updatedQ
                    }
                    onQuestionsChange?.invoke(updatedList)
                    editingQuestionIndex = null
                    android.widget.Toast.makeText(context, "✅ प्रश्न ${editIdx + 1} सुरक्षित हो गया!", android.widget.Toast.LENGTH_SHORT).show()
                },
                onDelete = {
                    val updatedList = parsedQuestions.toMutableList().also { list ->
                        list.removeAt(editIdx)
                    }
                    onQuestionsChange?.invoke(updatedList)
                    editingQuestionIndex = null
                    android.widget.Toast.makeText(context, "🗑️ प्रश्न ${editIdx + 1} हटा दिया गया", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    cropTargetImageSource?.let { src ->
        val qIdx = cropTargetQuestionIndex ?: 0
        AdminDiagramCropDialog(
            imageSource = src,
            questionNumber = qIdx + 1,
            onDismiss = {
                cropTargetImageSource = null
                cropTargetQuestionIndex = null
            },
            onDeleteDiagram = {
                if (qIdx in parsedQuestions.indices) {
                    val updatedList = parsedQuestions.toMutableList().also { list ->
                        list[qIdx] = list[qIdx].copy(imageUrl = null)
                    }
                    onQuestionsChange?.invoke(updatedList)
                }
                cropTargetImageSource = null
                cropTargetQuestionIndex = null
            },
            onCropped = { newImagePath ->
                if (qIdx in parsedQuestions.indices) {
                    val updatedList = parsedQuestions.toMutableList().also { list ->
                        list[qIdx] = list[qIdx].copy(imageUrl = newImagePath)
                    }
                    onQuestionsChange?.invoke(updatedList)
                }
                cropTargetImageSource = null
                cropTargetQuestionIndex = null
            }
        )
    }

    val delIdx = deleteDiagramConfirmQuestionIndex
    if (delIdx != null && delIdx in parsedQuestions.indices) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteDiagramConfirmQuestionIndex = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "प्रश्न ${delIdx + 1} का डायग्राम हटाएं?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "क्या आप इस प्रश्न से डायग्राम हटाना चाहते हैं? डायग्राम हटने के बाद यह सामान्य टेक्स्ट प्रश्न रहेगा।",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = parsedQuestions.toMutableList().also { list ->
                            list[delIdx] = list[delIdx].copy(imageUrl = null)
                        }
                        onQuestionsChange?.invoke(updated)
                        deleteDiagramConfirmQuestionIndex = null
                        Toast.makeText(context, "🗑️ प्रश्न ${delIdx + 1} का डायग्राम हटा दिया गया", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("हाँ, डायग्राम हटाएं", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteDiagramConfirmQuestionIndex = null }) {
                    Text("रद्द करें")
                }
            }
        )
    }

    if (showClearAllConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
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
                    text = "🗑️ सभी ${parsedQuestions.size} प्रश्न हटाएं?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "क्या आप वाकई तैयार किए गए सभी ${parsedQuestions.size} प्रश्नों को हटाना चाहते हैं? यह क्रिया ड्राफ्ट को पूरी तरह साफ़ कर देगी ताकि आप नया टेस्ट या PDF शुरू कर सकें।",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onQuestionsChange?.invoke(emptyList())
                        onTextChange("")
                        onClearAll?.invoke()
                        com.example.service.AiBackgroundExecutionManager.clearAllQuestionsAndDraft(context)
                        selectedSetIndex = -1
                        showClearAllConfirmDialog = false
                        Toast.makeText(context, "🧹 सभी प्रश्न व ड्राफ्ट सफलतापूर्वक साफ़ कर दिए गए!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("हाँ, सब साफ़ करें", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearAllConfirmDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("रद्द करें")
                }
            }
        )
    }

    // Extracted Questions Preview list (filtered by selectedSetIndex if in chapter-wise mode)
    val displayedQuestions = remember(parsedQuestions, selectedSetIndex, isChapterWise) {
        if (!isChapterWise || selectedSetIndex == -1 || selectedSetIndex >= questionSets.size) {
            parsedQuestions.mapIndexed { idx, q -> idx to q }
        } else {
            val start = selectedSetIndex * 10
            questionSets[selectedSetIndex].mapIndexed { lIdx, q -> (start + lIdx) to q }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Destination Badge
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Target Destination:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1D4ED8)
                        )
                        Text(
                            text = if (isChapterWise) {
                                "$category ➔ $subExam ➔ $subjectTitle ➔ $chapterTitle (10-10 Q Sets)"
                            } else {
                                "$category ➔ $subExam ➔ ${if (postType == "CONSTABLE") "कांस्टेबल" else "हेड कांस्टेबल"} ➔ $year"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }
        }

        // GitHub Sync Banner & Config Header
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (gitHubConfig.token.isNotBlank()) Color(0xFFF8FAFC) else Color(0xFFFFFBEB)
                ),
                border = BorderStroke(
                    1.2.dp,
                    if (gitHubConfig.token.isNotBlank()) Color(0xFFCBD5E1) else Color(0xFFFDE68A)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = if (gitHubConfig.token.isNotBlank()) Color(0xFF0F172A) else Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "GitHub Auto-Sync Repo",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (gitHubConfig.token.isNotBlank())
                                        "${gitHubConfig.owner}/${gitHubConfig.repo} (${gitHubConfig.branch})"
                                    else
                                        "डिफ़ॉल्ट या कस्टम GitHub टोकन सेट करें",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showGithubSettings = !showGithubSettings },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = if (showGithubSettings) Icons.Default.KeyboardArrowUp else Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showGithubSettings) "बंद करें" else "GitHub सेटिंग्स", fontSize = 11.sp)
                        }
                    }

                    // Collapsible GitHub Settings Section
                    AnimatedVisibility(visible = showGithubSettings) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "📝 5 जरूरी डिब्बे (Fields) - ध्यान से भरें:",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            // 1. TOKEN BOX
                            ConfigInputField(
                                boxNumber = "1",
                                title = "GitHub Personal Access Token",
                                description = "GitHub API टोकन (ghp_... / github_pat_...)",
                                exampleHint = "उदा. ghp_xxxxxxxxxxxxxxxxxxxx",
                                value = tempToken,
                                onValueChange = { tempToken = it },
                                icon = Icons.Default.Key,
                                isPassword = true
                            )

                            // 2. OWNER BOX
                            ConfigInputField(
                                boxNumber = "2",
                                title = "Owner / Username (यूजरनेम)",
                                description = "आपका GitHub यूज़रनेम या Organization",
                                exampleHint = "उदा. prahlad-singh या exam-portal-admin",
                                value = tempOwner,
                                onValueChange = { tempOwner = it },
                                icon = Icons.Default.Person
                            )

                            // 3. REPOSITORY BOX
                            ConfigInputField(
                                boxNumber = "3",
                                title = "Repository Name (रेपो का नाम)",
                                description = "जहाँ टेस्ट की JSON फ़ाइलें सेव होंगी",
                                exampleHint = "उदा. exam-question-bank",
                                value = tempRepo,
                                onValueChange = { tempRepo = it },
                                icon = Icons.Default.Code
                            )

                            // 4. BRANCH BOX
                            ConfigInputField(
                                boxNumber = "4",
                                title = "Git Branch (ब्रांच)",
                                description = "डिफ़ॉल्ट कमिट ब्रांच (main/master)",
                                exampleHint = "उदा. main",
                                value = tempBranch,
                                onValueChange = { tempBranch = it },
                                icon = Icons.Default.Code
                            )

                            // 5. FOLDER BOX
                            ConfigInputField(
                                boxNumber = "5",
                                title = "Folder Path (फ़ोल्डर का रास्ता)",
                                description = "रिपो में किस फ़ोल्डर में टेस्ट सेव होगा",
                                exampleHint = "उदा. tests या data/exams",
                                value = tempFolder,
                                onValueChange = { tempFolder = it },
                                icon = Icons.Default.Folder
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val newCfg = GitHubConfig(
                                            owner = tempOwner.trim(),
                                            repo = tempRepo.trim(),
                                            branch = tempBranch.trim().ifBlank { "main" },
                                            token = tempToken.trim(),
                                            folderPath = tempFolder.trim().ifBlank { "tests" }
                                        )
                                        onSaveGitHubConfig(newCfg)
                                        showGithubSettings = false
                                        Toast.makeText(context, "GitHub सेटिंग्स सुरक्षित हो गईं!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("सेटिंग्स सेव करें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (tempToken.isBlank()) {
                                            val savedToken = GitHubSyncService.getSavedConfig(context).token
                                            if (savedToken.isNotBlank()) tempToken = savedToken
                                        }
                                        tempOwner = GitHubSyncService.DEFAULT_OWNER
                                        tempRepo = GitHubSyncService.DEFAULT_REPO
                                        tempBranch = GitHubSyncService.DEFAULT_BRANCH
                                        tempFolder = GitHubSyncService.DEFAULT_FOLDER
                                        Toast.makeText(context, "डिफ़ॉल्ट वैल्यू भर दी गई! (टोकन सुरक्षित है)", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(0.9f)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("डिफ़ॉल्ट भरें", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Test Title & Settings
        item {
            OutlinedTextField(
                value = testTitle,
                onValueChange = onTitleChange,
                label = { Text("टेस्ट का नाम (Test Title)", color = Color(0xFF1E293B)) },
                textStyle = TextStyle(
                    color = Color(0xFF0F172A),
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = Color(0xFF2563EB),
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedLabelColor = Color(0xFF2563EB),
                    unfocusedLabelColor = Color(0xFF64748B)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Mode-Specific AI Rules Banner / Settings Button
        item {
            Card(
                onClick = { showAiRulesDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isChapterWise) Color(0xFFFAF5FF) else Color(0xFFEFF6FF)
                ),
                border = BorderStroke(
                    1.2.dp,
                    if (isChapterWise) Color(0xFFD8B4FE) else Color(0xFFBFDBFE)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isChapterWise) Color(0xFFEDE9FE) else Color(0xFFDBEAFE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = if (isChapterWise) Color(0xFF7C3AED) else Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isChapterWise) "चैप्टर-वाइज़ AI नियम (Separate Rules)" else "फुल टेस्ट AI नियम (Separate Rules)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isChapterWise) Color(0xFF7C3AED) else Color(0xFF2563EB)
                            ) {
                                Text(
                                    text = if (isChapterWise) "10-Q SETS" else "FULL PAPER",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = if (isChapterWise)
                                "चैप्टर वाइज 10-10 प्रश्न सेट्स के विशेष नियम सेट करें (फुल टेस्ट से अलग)"
                            else
                                "फुल टेस्ट आंसर की पार्सिंग के विशेष नियम सेट करें (चैप्टर वाइज से अलग)",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isChapterWise) Color(0xFF7C3AED) else Color(0xFF2563EB)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "नियम बदलें",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Multi-API Key Pool & Rotation Status Card
        item {
            Card(
                onClick = { showApiKeyManagerDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.2.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val activeKeysCount = remember { com.example.service.GeminiApiKeyPoolManager.getSavedKeys(context).size }
                            Text(
                                text = "$activeKeysCount Gemini API Keys सक्रिय (Sequential Rotation)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF16A34A)
                            ) {
                                Text(
                                    text = "0% LIMIT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "पेज दर पेज ऑटो-रोटेशन + मेमोरी शेयरिंग ऑन है। कोई कोटा ब्लॉक (429) नहीं आएगा!",
                            fontSize = 11.sp,
                            color = Color(0xFF166534)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF16A34A)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Keys देखें",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Upload Options & Input Section
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "आंसर की / PDF अपलोड करें",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        OutlinedButton(
                            onClick = onLoadSample,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("1-टैप सैंपल लोड करें", fontSize = 11.sp, color = Color(0xFFB45309))
                        }
                    }

                    // PDF Pick Button
                    Surface(
                        onClick = onPickPdf,
                        shape = RoundedCornerShape(12.dp),
                        color = if (uploadedFileName != null) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.2.dp, if (uploadedFileName != null) Color(0xFF86EFAC) else Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (uploadedFileName != null) Color(0xFFDCFCE7) else Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uploadedFileName != null) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = if (uploadedFileName != null) Color(0xFF16A34A) else Color(0xFF475569),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uploadedFileName ?: "आंसर की PDF फाइल चुनें (Pick PDF)",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (uploadedFileName != null) Color(0xFF15803D) else Color(0xFF1E293B)
                                )
                                Text(
                                    text = if (uploadedFileName != null) "PDF सफलतापूर्वक अटैच हो गई है" else "ऑफिशियल परीक्षा आंसर की या पेपर PDF",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // Raw Text Field or Attached PDF Status
                    val isPdfData = rawText.startsWith("data:")
                    if (isPdfData) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.2.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDBEAFE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "📄 ${uploadedFileName ?: "Answer_Key.pdf"}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E3A8A)
                                    )
                                    Text(
                                        text = if (pdfTotalPages > 0) {
                                            "PDF के सभी $pdfTotalPages पेज सुरक्षित रूप से लोड हैं। अब नीचे हरा बटन 'AI Parse' दबाएं।"
                                        } else {
                                            "PDF सफलतापूर्वक लोड हो चुकी है। अब नीचे 'AI Parse' बटन दबाएं।"
                                        },
                                        fontSize = 12.sp,
                                        color = Color(0xFF2563EB)
                                    )
                                }
                                IconButton(
                                    onClick = { onTextChange("") }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove PDF",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = rawText,
                            onValueChange = onTextChange,
                            label = { Text("या आंसर की / प्रश्न टेक्स्ट यहाँ पेस्ट करें (Paste Raw Text)", color = Color(0xFF1E293B)) },
                            placeholder = { Text("Q1. प्रश्न विवरण...\nA) विकल्प 1\nB) विकल्प 2\nAns: A\nExp: व्याख्या...", color = Color(0xFF94A3B8)) },
                            textStyle = TextStyle(
                                color = Color(0xFF0F172A),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 4,
                            maxLines = 8,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                cursorColor = Color(0xFF2563EB),
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedLabelColor = Color(0xFF2563EB),
                                unfocusedLabelColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Primary action: AI Parse & Review Questions First (Manual Review Flow)
                    Button(
                        onClick = onProcessAi,
                        enabled = !isProcessing && !isUploadingToGithub,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(processingMessage, fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("✨ AI से प्रश्न पार्स व प्रीव्यू करें (Review & Edit)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                                Text("पहले सभी प्रश्न, विकल्प व आकृतियां चेक करें, फिर ऐप या GitHub में जोड़ें", fontSize = 10.sp, color = Color(0xFFDBEAFE))
                            }
                        }
                    }

                    // Direct 1-Click Auto Push (Optional shortcut)
                    OutlinedButton(
                        onClick = onAutoParseAndPushToGitHub,
                        enabled = !isProcessing && !isUploadingToGithub,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        if (isUploadingToGithub) {
                            CircularProgressIndicator(
                                color = Color(0xFF047857),
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(processingMessage, fontSize = 11.5.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⚡ डायरेक्ट ऑटो-पार्स + GitHub कमिट (Direct Push)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF047857))
                        }
                    }
                }
            }
        }

        // GitHub Upload Success / Error Card (if uploaded)
        if (gitHubUploadResult != null) {
            item {
                val isBadCred = !gitHubUploadResult.isSuccess && (gitHubUploadResult.message.contains("credential", ignoreCase = true) || gitHubUploadResult.message.contains("401") || gitHubUploadResult.message.contains("token", ignoreCase = true))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (gitHubUploadResult.isSuccess) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                    border = BorderStroke(
                        1.2.dp,
                        if (gitHubUploadResult.isSuccess) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (gitHubUploadResult.isSuccess) Icons.Default.CheckCircle else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (gitHubUploadResult.isSuccess) Color(0xFF059669) else Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (gitHubUploadResult.isSuccess) {
                                        "✅ GitHub पर JSON फाइल सफलतापूर्वक सेव हो गई!"
                                    } else if (isBadCred) {
                                        "⚠️ GitHub टोकन एक्सपायर/अमान्य है (Bad Credentials)"
                                    } else {
                                        "GitHub अपलोड स्टेटस (Upload Failed):"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (gitHubUploadResult.isSuccess) Color(0xFF065F46) else Color(0xFF991B1B)
                                )
                                Text(
                                    text = if (isBadCred) {
                                        "आपका GitHub पर्सनल एक्सेस टोकन (PAT) एक्सपायर हो गया है या बदल गया है। कृपया नीचे बटन दबाकर नया टोकन पेस्ट करें।"
                                    } else {
                                        gitHubUploadResult.message
                                    },
                                    fontSize = 11.5.sp,
                                    color = if (gitHubUploadResult.isSuccess) Color(0xFF047857) else Color(0xFFB91C1C)
                                )
                            }
                        }

                        if (isBadCred) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showGithubDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("⚙️ नया GitHub Token दर्ज करें", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        if (gitHubUploadResult.commitSha != null) {
                            Text(
                                text = "Commit SHA: ${gitHubUploadResult.commitSha}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF475569)
                            )
                        }

                        if (gitHubUploadResult.fileUrl != null) {
                            Button(
                                onClick = { uriHandler.openUri(gitHubUploadResult.fileUrl) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GitHub पर JSON फाइल देखें ➔", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Parsed Results Section
        if (parsedQuestions.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.2.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isChapterWise) {
                                            "AI द्वारा ${parsedQuestions.size} प्रश्न = ${questionSets.size} सेट्स तैयार!"
                                        } else {
                                            "AI द्वारा ${parsedQuestions.size} प्रश्न तैयार कर दिए गए!"
                                        },
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534)
                                    )
                                    Text(
                                        text = if (isChapterWise) {
                                            "प्रत्येक सेट 10 प्रश्न, 10 मिनट व विस्तृत हल सहित"
                                        } else {
                                            "4 विकल्प, सही उत्तर व हिंदी व्याख्या सहित"
                                        },
                                        fontSize = 11.sp,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { showClearAllConfirmDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFFFEF2F2),
                                    contentColor = Color(0xFFDC2626)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFDC2626))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("सब साफ़ करें", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                        }

                        // If Chapter Wise, Show 10-Question Sets Browser
                        if (isChapterWise && questionSets.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "📦 10-10 प्रश्नों के तैयार सेट्स (कुल ${questionSets.size} सेट):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534)
                                    )
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        item {
                                            val isAll = selectedSetIndex == -1
                                            Surface(
                                                onClick = { selectedSetIndex = -1 },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isAll) Color(0xFF166534) else Color.White,
                                                border = BorderStroke(1.dp, if (isAll) Color(0xFF166534) else Color(0xFFCBD5E1))
                                            ) {
                                                Text(
                                                    text = "सभी प्रश्न (${parsedQuestions.size})",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAll) Color.White else Color(0xFF1E293B),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                        items(questionSets.size) { sIdx ->
                                            val isCur = selectedSetIndex == sIdx
                                            val setQCount = questionSets[sIdx].size
                                            Surface(
                                                onClick = { selectedSetIndex = sIdx },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isCur) Color(0xFF2563EB) else Color.White,
                                                border = BorderStroke(1.dp, if (isCur) Color(0xFF1D4ED8) else Color(0xFFCBD5E1))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Set ${sIdx + 1}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isCur) Color.White else Color(0xFF1E293B)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(if (isCur) Color.White.copy(alpha = 0.25f) else Color(0xFFEFF6FF))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "$setQCount Q",
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isCur) Color.White else Color(0xFF2563EB)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Dynamic Detected Subjects Breakdown (for Full Test mode)
                        val subjectGroups = remember(parsedQuestions) {
                            parsedQuestions.groupBy { it.subject }.mapValues { it.value.size }
                        }
                        if (!isChapterWise && subjectGroups.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "📚 इस परीक्षा में पहचाने गए विषय (${subjectGroups.size}):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF334155)
                                    )
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        items(subjectGroups.entries.toList()) { (sub, count) ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.White,
                                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = sub,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF1E293B)
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFF2563EB))
                                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                                    ) {
                                                        Text(
                                                            text = "$count प्रश्न",
                                                            fontSize = 10.sp,
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
                        }

                        // Primary Action: Add to App
                        Button(
                            onClick = onAddToLiveApp,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (addedToLiveApp) Color(0xFF059669) else Color(0xFF16A34A)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (addedToLiveApp) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (addedToLiveApp) {
                                    if (isChapterWise) "✅ सभी ${questionSets.size} सेट ऐप में जुड़ गए!" else "✅ टेस्ट ऐप में लाइव जुड़ गया है!"
                                } else {
                                    if (isChapterWise) "🚀 अध्याय '$chapterTitle' में सभी ${questionSets.size} सेट जोड़ें" else "🚀 ऐप टेस्ट सीरीज में जोड़ें (Add to App)"
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Push directly to GitHub Action Button
                        Button(
                            onClick = onPushOnlyToGitHub,
                            enabled = !isUploadingToGithub,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isUploadingToGithub) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GitHub में Push हो रहा है...", fontSize = 13.sp)
                            } else {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GitHub Repository में JSON पुश करें (Commit to GitHub)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // GitHub Export & Test Launch Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCopyGitHubCode,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Kotlin Code", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = onCopyJson,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("GitHub JSON", fontSize = 11.sp)
                            }

                            Button(
                                onClick = onLaunchTest,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                modifier = Modifier.weight(1.1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("टेस्ट दें (Play)", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Extracted Questions Preview list header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isChapterWise) "📝 प्रश्न सूची (कुल: ${parsedQuestions.size})" else "📝 सभी प्रश्न (${parsedQuestions.size})",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showClearAllConfirmDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFFEF2F2),
                                contentColor = Color(0xFFDC2626)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("सब साफ़ करें", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }

                        OutlinedButton(
                            onClick = {
                                val newQ = QuizQuestion(
                                    id = parsedQuestions.size + 1,
                                    question = "नया प्रश्न दर्ज करें",
                                    options = listOf("विकल्प A", "विकल्प B", "विकल्प C", "विकल्प D"),
                                    correctIndex = 0,
                                    explanation = "",
                                    subject = if (isChapterWise) (chapterTitle.ifBlank { "सामान्य ज्ञान" }) else "सामान्य ज्ञान",
                                    examShiftInfo = if (year.isNotBlank()) "$subExam $year" else null
                                )
                                val updated = parsedQuestions.toMutableList().apply { add(newQ) }
                                onQuestionsChange?.invoke(updated)
                                editingQuestionIndex = updated.size - 1
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                            border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("+ नया प्रश्न", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Extracted Questions Preview list (filtered by selectedSetIndex if in chapter-wise mode)
            items(displayedQuestions.size) { i ->
                val (idx, q) = displayedQuestions[i]
                Card(
                    onClick = { editingQuestionIndex = idx },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
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
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEFF6FF))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "प्रश्न ${idx + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = q.subject,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF92400E)
                                )
                            }
                            if (!q.examShiftInfo.isNullOrBlank()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = q.examShiftInfo,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { editingQuestionIndex = idx },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "प्रश्न एडिट करें",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val updated = parsedQuestions.toMutableList().also { it.removeAt(idx) }
                                    onQuestionsChange?.invoke(updated)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "प्रश्न हटाएं",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = TextFormatHelper.parseMarkdown(q.question),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )

                        if (!q.imageUrl.isNullOrBlank()) {
                            val previewBitmap = remember(q.imageUrl) {
                                val img = q.imageUrl
                                when {
                                    img.startsWith("data:image", true) || (!img.startsWith("http") && !img.startsWith("/") && img.length > 50) -> {
                                        try {
                                            val clean = if (img.contains(",")) img.substringAfter(",") else img
                                            val bytes = android.util.Base64.decode(clean.trim(), android.util.Base64.DEFAULT)
                                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        } catch (_: Exception) { null }
                                    }
                                    img.startsWith("/") && java.io.File(img).exists() -> {
                                        try {
                                            android.graphics.BitmapFactory.decodeFile(img)
                                        } catch (_: Exception) { null }
                                    }
                                    else -> {
                                        try {
                                            val cached = com.example.model.OfflineTestRepository.getOfflineImageFile(context, img)
                                            if (cached != null) android.graphics.BitmapFactory.decodeFile(cached.absolutePath) else null
                                        } catch (_: Exception) { null }
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "📷 प्रश्न आकृति (Diagram)",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = { previewImageForDialog = q.imageUrl },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ZoomIn,
                                                    contentDescription = "बड़ा देखें",
                                                    tint = Color(0xFF2563EB),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { deleteDiagramConfirmQuestionIndex = idx },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "डायग्राम हटाएं",
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                            .clickable { previewImageForDialog = q.imageUrl },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (previewBitmap != null) {
                                            Image(
                                                bitmap = previewBitmap.asImageBitmap(),
                                                contentDescription = "Question Image",
                                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                            )
                                        } else {
                                            coil.compose.AsyncImage(
                                                model = q.imageUrl,
                                                contentDescription = "Question Image",
                                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                cropTargetQuestionIndex = idx
                                                cropTargetImageSource = q.imageUrl
                                            },
                                            modifier = Modifier.weight(1.2f).height(30.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                                            border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("✂️ क्रॉप व एडिट", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                addDiagramTargetQuestionIndex = idx
                                                addDiagramPickerLauncher.launch("image/*")
                                            },
                                            modifier = Modifier.weight(0.9f).height(30.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("बदलें", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                deleteDiagramConfirmQuestionIndex = idx
                                            },
                                            modifier = Modifier.weight(0.8f).height(30.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFFDC2626))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("हटाएं", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                        }
                                    }
                                }
                            }
                        }

                        if (q.imageUrl.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        addDiagramTargetQuestionIndex = idx
                                        addDiagramPickerLauncher.launch("image/*")
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "+ इस प्रश्न में डायग्राम/आकृति जोड़ें व क्रॉप करें",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1D4ED8)
                                    )
                                }
                            }
                        }

                        // Options
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            q.options.forEachIndexed { optIdx, optText ->
                                val isCorrect = optIdx == q.correctIndex
                                val cleanedOpt = GeminiQuestionParserService.cleanOptionText(optText)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isCorrect) Color(0xFFDCFCE7) else Color(0xFFF8FAFC))
                                        .border(
                                            1.dp,
                                            if (isCorrect) Color(0xFF86EFAC) else Color(0xFFE2E8F0),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val optTrimmed = cleanedOpt.trim()
                                    val isOptImage = optTrimmed.startsWith("data:image", true) || optTrimmed.startsWith("http://") || optTrimmed.startsWith("https://")
                                    if (isOptImage) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${('A' + optIdx)}. [आकृति / चित्र]",
                                                fontSize = 12.sp,
                                                fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isCorrect) Color(0xFF166534) else Color(0xFF334155),
                                            )
                                            coil.compose.AsyncImage(
                                                model = optTrimmed,
                                                contentDescription = "Option Image",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(80.dp)
                                                    .clip(RoundedCornerShape(4.dp)),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = TextFormatHelper.parseMarkdown("${('A' + optIdx)}. $cleanedOpt"),
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCorrect) Color(0xFF166534) else Color(0xFF334155),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (isCorrect) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Explanation
                        if (q.explanation.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "💡 व्याख्या: ${q.explanation}",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        // Bottom edit button for clear affordance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { editingQuestionIndex = idx },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                                border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("✏️ प्रश्न व विकल्प एडिट करें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (parsedQuestions.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🗑️ सभी ${parsedQuestions.size} प्रश्न साफ़ करना चाहते हैं?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                text = "ड्राफ्ट रीसेट करके नया पेपर या PDF अपलोड करने के लिए 'सब साफ़ करें' दबाएं।",
                                fontSize = 11.sp,
                                color = Color(0xFFB91C1C)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { showClearAllConfirmDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("सब साफ़ करें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun QuestionEditDialog(
    question: QuizQuestion,
    index: Int,
    onDismiss: () -> Unit,
    onSave: (QuizQuestion) -> Unit,
    onDelete: () -> Unit
) {
    var questionText by remember(question) { mutableStateOf(question.question) }
    var subjectText by remember(question) { mutableStateOf(question.subject) }
    var examShiftInfoText by remember(question) { mutableStateOf(question.examShiftInfo ?: "") }
    val initialOptions = remember(question) {
        val list = question.options.map { GeminiQuestionParserService.cleanOptionText(it) }.toMutableList()
        while (list.size < 4) {
            list.add("विकल्प ${list.size + 1}")
        }
        list
    }
    var opt0 by remember(question) { mutableStateOf(initialOptions.getOrElse(0) { "" }) }
    var opt1 by remember(question) { mutableStateOf(initialOptions.getOrElse(1) { "" }) }
    var opt2 by remember(question) { mutableStateOf(initialOptions.getOrElse(2) { "" }) }
    var opt3 by remember(question) { mutableStateOf(initialOptions.getOrElse(3) { "" }) }
    var correctIndex by remember(question) { mutableIntStateOf(question.correctIndex.coerceIn(0, 3)) }
    var explanationText by remember(question) { mutableStateOf(question.explanation) }
    var imageUrlText by remember(question) { mutableStateOf(question.imageUrl ?: "") }
    var previewImageFull by remember { mutableStateOf(false) }
    var showCropInEdit by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val editImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUrlText = uri.toString()
            showCropInEdit = true
        }
    }

    if (showCropInEdit && imageUrlText.isNotBlank()) {
        AdminDiagramCropDialog(
            imageSource = imageUrlText,
            questionNumber = index + 1,
            onDismiss = { showCropInEdit = false },
            onDeleteDiagram = {
                imageUrlText = ""
                showCropInEdit = false
            },
            onCropped = { newCroppedPath ->
                imageUrlText = newCroppedPath
                showCropInEdit = false
            }
        )
    }

    if (previewImageFull && imageUrlText.isNotBlank()) {
        Dialog(
            onDismissRequest = { previewImageFull = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(16.dp)),
                color = Color.White,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔍 संपूर्ण प्रश्न आकृति (Full View)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        IconButton(onClick = { previewImageFull = false }) {
                            Icon(Icons.Default.Close, contentDescription = "बंद करें", tint = Color(0xFF475569))
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF8FAFC)),
                        contentAlignment = Alignment.Center
                    ) {
                        val fullBitmap = remember(imageUrlText) {
                            when {
                                imageUrlText.startsWith("data:image", true) || (!imageUrlText.startsWith("http") && !imageUrlText.startsWith("/") && imageUrlText.length > 50) -> {
                                    try {
                                        val clean = if (imageUrlText.contains(",")) imageUrlText.substringAfter(",") else imageUrlText
                                        val bytes = android.util.Base64.decode(clean.trim(), android.util.Base64.DEFAULT)
                                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    } catch (_: Exception) { null }
                                }
                                imageUrlText.startsWith("/") && java.io.File(imageUrlText).exists() -> {
                                    try {
                                        android.graphics.BitmapFactory.decodeFile(imageUrlText)
                                    } catch (_: Exception) { null }
                                }
                                else -> {
                                    try {
                                        val cached = com.example.model.OfflineTestRepository.getOfflineImageFile(context, imageUrlText)
                                        if (cached != null) android.graphics.BitmapFactory.decodeFile(cached.absolutePath) else null
                                    } catch (_: Exception) { null }
                                }
                            }
                        }
                        if (fullBitmap != null) {
                            Image(
                                bitmap = fullBitmap.asImageBitmap(),
                                contentDescription = "Full Question Diagram",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        } else {
                            coil.compose.AsyncImage(
                                model = imageUrlText,
                                contentDescription = "Full Question Diagram",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { previewImageFull = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("ठीक है (Done)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                var showLivePreviewCard by remember { mutableStateOf(true) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2563EB))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "प्रश्न ${index + 1} एडिट करें",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Real-time preview toggle button
                        Surface(
                            onClick = { showLivePreviewCard = !showLivePreviewCard },
                            shape = RoundedCornerShape(6.dp),
                            color = if (showLivePreviewCard) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (showLivePreviewCard) Color(0xFF93C5FD) else Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (showLivePreviewCard) Color(0xFF16A34A) else Color(0xFF94A3B8))
                                )
                                Text(
                                    text = if (showLivePreviewCard) "👁️ रियल-टाइम प्रीव्यू" else "👁️ प्रीव्यू दिखाएं",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (showLivePreviewCard) Color(0xFF1D4ED8) else Color(0xFF475569)
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "बंद करें")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Reduced from 10dp
                ) {
                    // Full Real-Time Live Preview Card (Always updates instantly as user types)
                    if (showLivePreviewCard) {
                        Surface(
                            shape = RoundedCornerShape(10.dp), // Reduced from 12dp
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.5.dp, Color(0xFF93C5FD)),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) { // Reduced from 12dp
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEF4444))
                                        )
                                        Text(
                                            text = "लाइव रियल-टाइम प्रीव्यू",
                                            fontSize = 11.sp, // Reduced from 11.5sp
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1D4ED8)
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (subjectText.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFEFF6FF)
                                            ) {
                                                Text(
                                                    text = subjectText,
                                                    fontSize = 9.sp, // Reduced from 10sp
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2563EB),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        if (examShiftInfoText.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFF1F5F9)
                                            ) {
                                                Text(
                                                    text = examShiftInfoText,
                                                    fontSize = 9.sp, // Reduced from 10sp
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF475569),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp)) // Reduced from 8dp

                                // Question text rendered live
                                Text(
                                    text = TextFormatHelper.parseMarkdown("Q${index + 1}. ${if (questionText.isNotBlank()) questionText else "प्रश्न का टेक्स्ट यहाँ टाइप करें..."}"),
                                    fontSize = 12.5.sp, // Reduced from 13.5sp
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (questionText.isNotBlank()) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                    lineHeight = 17.sp // Reduced from 19sp
                                )

                                // Image diagram preview if present
                                if (imageUrlText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val previewBitmap = remember(imageUrlText) {
                                        when {
                                            imageUrlText.startsWith("data:image", true) || (!imageUrlText.startsWith("http") && !imageUrlText.startsWith("/") && imageUrlText.length > 50) -> {
                                                try {
                                                    val clean = if (imageUrlText.contains(",")) imageUrlText.substringAfter(",") else imageUrlText
                                                    val bytes = android.util.Base64.decode(clean.trim(), android.util.Base64.DEFAULT)
                                                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                                } catch (_: Exception) { null }
                                            }
                                            imageUrlText.startsWith("/") && java.io.File(imageUrlText).exists() -> {
                                                android.graphics.BitmapFactory.decodeFile(imageUrlText)
                                            }
                                            else -> null
                                        }
                                    }
                                    if (previewBitmap != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White)
                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                bitmap = previewBitmap.asImageBitmap(),
                                                contentDescription = "डायग्राम",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // 4 Live Options
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    listOf(
                                        Pair(0, opt0),
                                        Pair(1, opt1),
                                        Pair(2, opt2),
                                        Pair(3, opt3)
                                    ).forEach { (oIdx, oVal) ->
                                        val isCorr = oIdx == correctIndex
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isCorr) Color(0xFFDCFCE7) else Color.White,
                                            border = BorderStroke(1.dp, if (isCorr) Color(0xFF16A34A) else Color(0xFFE2E8F0)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isCorr) Color(0xFF16A34A) else Color(0xFFF1F5F9)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${('A' + oIdx)}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isCorr) Color.White else Color(0xFF475569)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = TextFormatHelper.parseMarkdown(if (oVal.isNotBlank()) oVal else "विकल्प ${('A' + oIdx)}..."),
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isCorr) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (oVal.isNotBlank()) {
                                                        if (isCorr) Color(0xFF166534) else Color(0xFF1E293B)
                                                    } else Color(0xFF94A3B8),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isCorr) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "✓ सही उत्तर",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF16A34A)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Live explanation if present
                                if (explanationText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFEFCE8),
                                        border = BorderStroke(1.dp, Color(0xFFFEF08A)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                text = "💡 व्याख्या (Explanation):",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF854D0E)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = TextFormatHelper.parseMarkdown(explanationText),
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF713F12)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Question Text Input
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = questionText,
                                onValueChange = { questionText = it },
                                label = { Text("प्रश्न (Question Text)", fontSize = 12.sp, color = Color(0xFF475569)) },
                                placeholder = { Text("प्रश्न दर्ज करें... (अंडरलाइन हेतु _शब्द_ लिखें)", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 6,
                                trailingIcon = {
                                    if (questionText.isNotEmpty()) {
                                        IconButton(
                                            onClick = { questionText = "" },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "प्रश्न हटाएं",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                textStyle = TextStyle(fontSize = 13.5.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = Color(0xFF2563EB),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )

                            // Underline & Bold Formatting Helper Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("फॉर्मेटिंग:", fontSize = 10.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                Surface(
                                    onClick = {
                                        questionText = if (questionText.isBlank()) "_रेखांकित शब्द_" else "$questionText _रेखांकित शब्द_"
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, Color(0xFF93C5FD))
                                ) {
                                    Text(
                                        text = "+ _अंडरलाइन_",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1D4ED8),
                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    onClick = {
                                        questionText = if (questionText.isBlank()) "**बोल्ड शब्द**" else "$questionText **बोल्ड शब्द**"
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, Color(0xFF93C5FD))
                                ) {
                                    Text(
                                        text = "+ **बोल्ड**",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Live formatted preview shown immediately below question input as user types
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("👁️ टाइप करते समय लाइव टेक्स्ट:", fontSize = 10.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                                        if (questionText.contains("_") || questionText.contains("*")) {
                                            Text("✨ फॉर्मेटिंग सक्रिय", fontSize = 9.5.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (questionText.isNotBlank()) TextFormatHelper.parseMarkdown(questionText) else androidx.compose.ui.text.AnnotatedString("यहाँ लाइव प्रश्न दिखेगा..."),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (questionText.isNotBlank()) Color(0xFF0F172A) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                    // Subject Input
                    OutlinedTextField(
                            value = subjectText,
                            onValueChange = { subjectText = it },
                            label = { Text("विषय / सेक्शन (Subject)", fontSize = 12.sp, color = Color(0xFF475569)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                if (subjectText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { subjectText = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "विषय हटाएं",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                    // Exam, Date & Shift Info (Optional / Automatic)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = examShiftInfoText,
                            onValueChange = { examShiftInfoText = it },
                            label = { Text("परीक्षा, दिनांक एवं शिफ्ट (Exam/Date/Shift Badge)", fontSize = 12.sp, color = Color(0xFF475569)) },
                            placeholder = { Text("उदा. DP Constable 22/11/2025 Shift 1st", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                if (examShiftInfoText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { examShiftInfoText = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "शिफ्ट विवरण हटाएं",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.SemiBold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF1D4ED8),
                                unfocusedTextColor = Color(0xFF1D4ED8),
                                focusedContainerColor = Color(0xFFEFF6FF),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        // Quick Year selection chips for easy year assignment
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("वर्ष (Year):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020").forEach { yr ->
                                val isContained = examShiftInfoText.contains(yr)
                                Surface(
                                    onClick = {
                                        if (isContained) {
                                            examShiftInfoText = examShiftInfoText.replace(yr, "").replace("  ", " ").trim()
                                        } else {
                                            var newText = examShiftInfoText
                                            listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020").forEach { existingYr ->
                                                newText = newText.replace(existingYr, "")
                                            }
                                            examShiftInfoText = "$newText $yr".replace("  ", " ").trim()
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isContained) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isContained) Color(0xFF1D4ED8) else Color(0xFFCBD5E1))
                                ) {
                                    Text(
                                        text = yr,
                                        fontSize = 11.sp,
                                        fontWeight = if (isContained) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isContained) Color.White else Color(0xFF334155),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Image / Diagram Status
                    val editDiagramBitmap = remember(imageUrlText) {
                            if (imageUrlText.isBlank()) null
                            else {
                                when {
                                    imageUrlText.startsWith("data:image", true) || (!imageUrlText.startsWith("http") && !imageUrlText.startsWith("/") && imageUrlText.length > 50) -> {
                                        try {
                                            val clean = if (imageUrlText.contains(",")) imageUrlText.substringAfter(",") else imageUrlText
                                            val bytes = android.util.Base64.decode(clean.trim(), android.util.Base64.DEFAULT)
                                            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        } catch (_: Exception) { null }
                                    }
                                    imageUrlText.startsWith("/") && java.io.File(imageUrlText).exists() -> {
                                        try {
                                            android.graphics.BitmapFactory.decodeFile(imageUrlText)
                                        } catch (_: Exception) { null }
                                    }
                                    else -> {
                                        try {
                                            val cached = com.example.model.OfflineTestRepository.getOfflineImageFile(context, imageUrlText)
                                            if (cached != null) android.graphics.BitmapFactory.decodeFile(cached.absolutePath) else null
                                        } catch (_: Exception) { null }
                                    }
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📷 प्रश्न आकृति / डायग्राम",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                    if (imageUrlText.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFEFF6FF)
                                        ) {
                                            Text(
                                                text = "आकृति संलग्न है",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF2563EB),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (imageUrlText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                                            .clickable { previewImageFull = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (editDiagramBitmap != null) {
                                            Image(
                                                bitmap = editDiagramBitmap.asImageBitmap(),
                                                contentDescription = "Question Diagram Preview",
                                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                            )
                                        } else {
                                            coil.compose.AsyncImage(
                                                model = imageUrlText,
                                                contentDescription = "Question Diagram Preview",
                                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                            )
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.Black.copy(alpha = 0.65f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                Text("टैप करके बड़ा देखें", fontSize = 10.sp, color = Color.White)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { showCropInEdit = true },
                                            modifier = Modifier.weight(1.2f).height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("✂️ क्रॉप/एडिट", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { previewImageFull = true },
                                            modifier = Modifier.weight(0.9f).height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                                            border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("बड़ा देखें", fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                                        }
                                        OutlinedButton(
                                            onClick = { editImagePickerLauncher.launch("image/*") },
                                            modifier = Modifier.weight(0.8f).height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("बदलें", fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                imageUrlText = ""
                                                Toast.makeText(context, "🗑️ डायग्राम हटा दिया गया", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(0.8f).height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFDC2626))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("हटाएं", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "इस प्रश्न में कोई आकृति नहीं है।",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        OutlinedButton(
                                            onClick = { editImagePickerLauncher.launch("image/*") },
                                            modifier = Modifier.height(30.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                                            border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ आकृति जोड़ें", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                    // Options Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "विकल्प (Options) एवं सही उत्तर चुनें:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        TextButton(
                            onClick = {
                                opt0 = ""
                                opt1 = ""
                                opt2 = ""
                                opt3 = ""
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("ऑप्शन खाली करें", fontSize = 11.5.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Options list
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val optionEntries = listOf(
                            Triple(0, opt0, { s: String -> opt0 = s }),
                            Triple(1, opt1, { s: String -> opt1 = s }),
                            Triple(2, opt2, { s: String -> opt2 = s }),
                            Triple(3, opt3, { s: String -> opt3 = s })
                        )
                        optionEntries.forEach { (optIdx, optValue, onValChange) ->
                            val isCorrect = optIdx == correctIndex
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isCorrect) Color(0xFFDCFCE7) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isCorrect) Color(0xFF16A34A) else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { correctIndex = optIdx },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isCorrect) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = "सही उत्तर चुनें",
                                                tint = if (isCorrect) Color(0xFF16A34A) else Color(0xFF64748B)
                                            )
                                        }

                                        Text(
                                            text = "${('A' + optIdx)}. ",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCorrect) Color(0xFF166534) else Color(0xFF1E293B)
                                        )

                                        OutlinedTextField(
                                            value = optValue,
                                            onValueChange = onValChange,
                                            placeholder = { Text("विकल्प ${('A' + optIdx)} दर्ज करें (अंडरलाइन हेतु _शब्द_)", fontSize = 11.5.sp, color = Color(0xFF94A3B8)) },
                                            modifier = Modifier.weight(1f),
                                            maxLines = 3,
                                            trailingIcon = {
                                                if (optValue.isNotEmpty()) {
                                                    IconButton(
                                                        onClick = { onValChange("") },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Clear,
                                                            contentDescription = "विकल्प हटाएं",
                                                            tint = Color(0xFF94A3B8),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            textStyle = TextStyle(fontSize = 12.5.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color(0xFF0F172A),
                                                unfocusedTextColor = Color(0xFF0F172A),
                                                focusedContainerColor = Color.White,
                                                unfocusedContainerColor = Color.White,
                                                focusedBorderColor = if (isCorrect) Color(0xFF16A34A) else Color(0xFF2563EB),
                                                unfocusedBorderColor = Color(0xFFCBD5E1)
                                            )
                                        )
                                    }

                                    // Real-time live formatted preview for option as user types
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isCorrect) Color(0xFFDCFCE7).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 36.dp, top = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("👁️ लाइव प्रीव्यू: ", fontSize = 10.sp, color = if (isCorrect) Color(0xFF166534) else Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                            Text(
                                                text = if (optValue.isNotBlank()) TextFormatHelper.parseMarkdown(optValue) else androidx.compose.ui.text.AnnotatedString("विकल्प ${('A' + optIdx)} टाइप करें..."),
                                                fontSize = 11.5.sp,
                                                color = if (isCorrect) Color(0xFF166534) else if (optValue.isNotBlank()) Color(0xFF1E293B) else Color(0xFF94A3B8),
                                                fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Explanation Field
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = explanationText,
                            onValueChange = { explanationText = it },
                            label = { Text("व्याख्या / हल (Explanation - Optional)", fontSize = 12.sp, color = Color(0xFF475569)) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            trailingIcon = {
                                if (explanationText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { explanationText = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "व्याख्या हटाएं",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            textStyle = TextStyle(fontSize = 12.5.sp, color = Color(0xFF0F172A)),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        // Live explanation preview as user types
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEFCE8),
                            border = BorderStroke(1.dp, Color(0xFFFEF08A)),
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "💡 लाइव व्याख्या प्रीव्यू:",
                                    fontSize = 10.sp,
                                    color = Color(0xFF854D0E),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (explanationText.isNotBlank()) TextFormatHelper.parseMarkdown(explanationText) else androidx.compose.ui.text.AnnotatedString("व्याख्या का प्रीव्यू यहाँ दिखेगा..."),
                                    fontSize = 11.5.sp,
                                    color = if (explanationText.isNotBlank()) Color(0xFF713F12) else Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("प्रश्न हटाएं", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val rawList = listOf(opt0, opt1, opt2, opt3)
                            val sanitizedOptions = rawList.mapIndexed { i, opt ->
                                val clean = GeminiQuestionParserService.cleanOptionText(opt)
                                if (clean.isBlank()) "विकल्प ${('A' + i)}" else clean
                            }
                            val updatedQ = question.copy(
                                question = questionText.trim(),
                                subject = subjectText.trim(),
                                options = sanitizedOptions,
                                correctIndex = correctIndex.coerceIn(0, (sanitizedOptions.size - 1).coerceAtLeast(0)),
                                explanation = explanationText.trim(),
                                imageUrl = imageUrlText.ifBlank { null },
                                examShiftInfo = examShiftInfoText.trim().ifBlank { null }
                            )
                            onSave(updatedQ)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("सेव करें (Save)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
