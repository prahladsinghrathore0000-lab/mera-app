package com.example.service

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.model.ExamSubjectManager
import com.example.model.QuizQuestion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Global App-wide Background AI Execution Manager.
 * Ensures that long-running PDF OCR scanning and Chapter question generation
 * run on an unkillable Application Scope with an Android Foreground Service and WakeLock,
 * allowing users to freely minimize the app, switch to other apps, or lock their phone.
 * Also persists the active Admin Step 5 draft state to disk so even if the app was closed,
 * reopening the app directly restores Step 5 with all 100 completed questions intact!
 */
object AiBackgroundExecutionManager {
    private const val TAG = "AiBackgroundManager"
    private const val DRAFT_FILE_NAME = "ai_completed_draft_session.json"

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentJob: Job? = null

    data class AiDraftSession(
        val category: String = "SSC Exams",
        val subExam: String = "SSC GD Constable",
        val subExamId: String = "ssc_gd",
        val testMode: String = "FULL_TEST",
        val postType: String = "CONSTABLE",
        val year: String = "2025",
        val subjectKey: String = "MATHS",
        val subjectTitle: String = "गणित / Math",
        val chapterTitle: String = "प्रतिशत (Percentage)",
        val testTitle: String = "SSC GD Constable Mock Test 2025",
        val rawAnswerKeyText: String = "",
        val uploadedFileName: String? = null,
        val pdfTotalPages: Int = 0,
        val durationMinutes: Int = 60,
        val totalMarks: Int = 160,
        val parsedQuestions: List<QuizQuestion> = emptyList(),
        val currentStep: Int = 5,
        val isDialogOpen: Boolean = false
    )

    data class AiTaskState(
        val taskId: String = "",
        val isRunning: Boolean = false,
        val taskType: String = "", // "EXAM_PARSE", "CHAPTER_GEN"
        val progressPercent: Int = 0,
        val statusMessage: String = "",
        val examTitle: String = "",
        val parsedQuestions: List<QuizQuestion> = emptyList(),
        val isSuccess: Boolean = false,
        val errorMessage: String? = null,
        val lastFinishedTimestamp: Long = 0L
    )

    private val _taskState = MutableStateFlow(AiTaskState())
    val taskState: StateFlow<AiTaskState> = _taskState.asStateFlow()

    private val _draftSession = MutableStateFlow(AiDraftSession())
    val draftSession: StateFlow<AiDraftSession> = _draftSession.asStateFlow()

    private val _isDialogOpen = MutableStateFlow(false)
    val isDialogOpen: StateFlow<Boolean> = _isDialogOpen.asStateFlow()

    fun init(context: Context) {
        try {
            val file = File(context.filesDir, DRAFT_FILE_NAME)
            if (file.exists() && file.length() > 0) {
                val jsonStr = file.readText()
                val json = JSONObject(jsonStr)
                val questionsArr = json.optJSONArray("questions") ?: JSONArray()
                val loadedQuestions = mutableListOf<QuizQuestion>()
                for (i in 0 until questionsArr.length()) {
                    val qObj = questionsArr.getJSONObject(i)
                    val optsArr = qObj.optJSONArray("options") ?: JSONArray()
                    val opts = mutableListOf<String>()
                    for (j in 0 until optsArr.length()) {
                        opts.add(optsArr.optString(j, ""))
                    }
                    loadedQuestions.add(
                        QuizQuestion(
                            id = qObj.optInt("id", i + 1),
                            question = qObj.optString("question", ""),
                            options = opts,
                            correctIndex = qObj.optInt("correctIndex", 0),
                            explanation = qObj.optString("explanation", ""),
                            subject = qObj.optString("subject", "General"),
                            examShiftInfo = qObj.optString("examShiftInfo").takeIf { it.isNotBlank() },
                            imageUrl = qObj.optString("imageUrl").takeIf { it.isNotBlank() }
                        )
                    )
                }

                if (loadedQuestions.isNotEmpty()) {
                    val session = AiDraftSession(
                        category = json.optString("category", "SSC Exams"),
                        subExam = json.optString("subExam", "Delhi Police"),
                        subExamId = json.optString("subExamId", "delhi_police"),
                        testMode = json.optString("testMode", "FULL_TEST"),
                        postType = json.optString("postType", "CONSTABLE"),
                        year = json.optString("year", "2025"),
                        subjectKey = json.optString("subjectKey", "MATHS"),
                        subjectTitle = json.optString("subjectTitle", "गणित / Math"),
                        chapterTitle = json.optString("chapterTitle", "प्रतिशत (Percentage)"),
                        testTitle = json.optString("testTitle", "Delhi Police Constable 22/11/2025 Shift 1st"),
                        rawAnswerKeyText = json.optString("rawAnswerKeyText", ""),
                        uploadedFileName = json.optString("uploadedFileName").takeIf { it.isNotBlank() },
                        pdfTotalPages = json.optInt("pdfTotalPages", 0),
                        parsedQuestions = loadedQuestions,
                        currentStep = 5,
                        isDialogOpen = true
                    )
                    _draftSession.value = session
                    _taskState.value = AiTaskState(
                        taskId = "persisted_${System.currentTimeMillis()}",
                        isRunning = false,
                        progressPercent = 100,
                        statusMessage = "🎉 पिछले सेशन से ${loadedQuestions.size} प्रश्न रीस्टोर किए गए",
                        examTitle = session.testTitle,
                        parsedQuestions = loadedQuestions,
                        isSuccess = true
                    )
                    _isDialogOpen.value = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore draft session from disk", e)
        }
    }

    private fun persistDraftToDisk(context: Context, session: AiDraftSession) {
        try {
            val json = JSONObject().apply {
                put("category", session.category)
                put("subExam", session.subExam)
                put("subExamId", session.subExamId)
                put("testMode", session.testMode)
                put("postType", session.postType)
                put("year", session.year)
                put("subjectKey", session.subjectKey)
                put("subjectTitle", session.subjectTitle)
                put("chapterTitle", session.chapterTitle)
                put("testTitle", session.testTitle)
                put("rawAnswerKeyText", session.rawAnswerKeyText)
                put("uploadedFileName", session.uploadedFileName ?: "")
                put("pdfTotalPages", session.pdfTotalPages)

                val questionsArr = JSONArray()
                for (q in session.parsedQuestions) {
                    val qObj = JSONObject().apply {
                        put("id", q.id)
                        put("question", q.question)
                        put("options", JSONArray(q.options))
                        put("correctIndex", q.correctIndex)
                        put("explanation", q.explanation)
                        put("subject", q.subject)
                        put("examShiftInfo", q.examShiftInfo ?: "")
                        put("imageUrl", q.imageUrl ?: "")
                    }
                    questionsArr.put(qObj)
                }
                put("questions", questionsArr)
            }
            val file = File(context.filesDir, DRAFT_FILE_NAME)
            file.writeText(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write draft session to disk", e)
        }
    }

    private fun clearDraftFromDisk(context: Context) {
        try {
            val file = File(context.filesDir, DRAFT_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {}
    }

    fun openDialog(session: AiDraftSession? = null) {
        if (session != null) {
            _draftSession.value = session.copy(isDialogOpen = true)
        } else {
            _draftSession.value = _draftSession.value.copy(isDialogOpen = true)
        }
        _isDialogOpen.value = true
    }

    fun closeDialog(context: Context? = null) {
        _isDialogOpen.value = false
        _draftSession.value = _draftSession.value.copy(isDialogOpen = false)
        if (context != null) {
            clearDraftFromDisk(context)
        }
    }

    fun clearAllQuestionsAndDraft(context: Context) {
        clearDraftFromDisk(context)
        _draftSession.value = AiDraftSession()
        _taskState.value = AiTaskState()
    }

    fun updateDraftSession(transform: (AiDraftSession) -> AiDraftSession) {
        _draftSession.value = transform(_draftSession.value)
    }

    fun startExamParsing(
        context: Context,
        rawText: String,
        examContext: String,
        testMode: String = "FULL_TEST",
        chapterTitle: String? = null,
        subjectTitle: String? = null,
        userGivenTitle: String = "",
        onCompleted: ((Result<GeminiQuestionParserService.ParsedExamResult>) -> Unit)? = null
    ) {
        val appContext = context.applicationContext
        currentJob?.cancel()

        val taskId = "parse_${System.currentTimeMillis()}"
        _taskState.value = AiTaskState(
            taskId = taskId,
            isRunning = true,
            taskType = "EXAM_PARSE",
            progressPercent = 5,
            statusMessage = "🚀 Google Gemini AI मॉडल बैकग्राउंड में कनेक्ट हो रहा है..."
        )

        _isDialogOpen.value = true
        _draftSession.value = _draftSession.value.copy(
            testMode = testMode,
            chapterTitle = chapterTitle ?: _draftSession.value.chapterTitle,
            subjectTitle = subjectTitle ?: _draftSession.value.subjectTitle,
            testTitle = if (userGivenTitle.isNotBlank()) userGivenTitle else _draftSession.value.testTitle,
            rawAnswerKeyText = rawText,
            currentStep = 5,
            isDialogOpen = true
        )

        // Start Foreground Service with WakeLock & Notification
        AiProcessingForegroundService.start(
            appContext,
            "🤖 AI PDF स्कैनिंग जारी है...",
            "Google Gemini AI से प्रश्न निकाले जा रहे हैं..."
        )

        currentJob = applicationScope.launch {
            try {
                val result = GeminiQuestionParserService.parseAnswerKeyToExamResult(
                    rawText = rawText,
                    examContext = examContext,
                    subjectOverride = null,
                    testMode = testMode,
                    chapterTitle = chapterTitle,
                    subjectTitle = subjectTitle,
                    context = appContext,
                    onProgress = { msg, pct ->
                        _taskState.value = _taskState.value.copy(
                            progressPercent = pct,
                            statusMessage = msg
                        )
                        AiProcessingForegroundService.updateProgress(
                            appContext,
                            "🤖 AI PDF स्कैनिंग जारी है...",
                            msg,
                            pct
                        )
                    }
                )

                if (result.isSuccess) {
                    val examRes = result.getOrNull()
                    val questions = examRes?.questions ?: emptyList()
                    val resolvedTitle = if (userGivenTitle.isNotBlank()) userGivenTitle else (examRes?.examTitle ?: examContext)

                    // Persist base64 images to local storage files
                    val offlineImagesDir = File(appContext.filesDir, "offline_images")
                    if (!offlineImagesDir.exists()) offlineImagesDir.mkdirs()

                    val finalized = questions.map { q ->
                        val normSubject = ExamSubjectManager.normalizeToOfficialSubject(q.subject, resolvedTitle)
                        val img = q.imageUrl
                        val processedImg = if (!img.isNullOrBlank() && (img.startsWith("data:image", true) || (!img.startsWith("http") && !img.startsWith("/") && img.length > 100))) {
                            try {
                                val clean = if (img.contains(",")) img.substringAfter(",") else img
                                val bytes = Base64.decode(clean.trim(), Base64.DEFAULT)
                                val file = File(offlineImagesDir, "diagram_${System.currentTimeMillis() % 100000}_q${q.id}.jpg")
                                file.writeBytes(bytes)
                                file.absolutePath
                            } catch (_: Throwable) {
                                img
                            }
                        } else {
                            img
                        }
                        q.copy(subject = normSubject, imageUrl = processedImg)
                    }

                    _taskState.value = _taskState.value.copy(
                        isRunning = false,
                        progressPercent = 100,
                        statusMessage = "🎉 शानदार! सभी ${finalized.size} प्रश्न सफलतापूर्वक तैयार हो गए",
                        examTitle = examRes?.examTitle ?: "",
                        parsedQuestions = finalized,
                        isSuccess = true,
                        errorMessage = null,
                        lastFinishedTimestamp = System.currentTimeMillis()
                    )

                    // Keep draft session up-to-date with finished questions and step 5
                    val finalTitle = if (userGivenTitle.isNotBlank()) userGivenTitle else (examRes?.examTitle ?: _draftSession.value.testTitle)
                    val resolvedExamId = com.example.model.DynamicTestRepository.resolveAccurateSubExamId(
                        _draftSession.value.subExamId,
                        finalTitle,
                        finalized
                    )
                    val updatedSession = _draftSession.value.copy(
                        subExamId = resolvedExamId,
                        parsedQuestions = finalized,
                        testTitle = finalTitle,
                        currentStep = 5,
                        isDialogOpen = true
                    )
                    _draftSession.value = updatedSession
                    _isDialogOpen.value = true

                    // Persist completed draft to disk so it stays 100% safe on Step 5/5 even if OS kills app!
                    persistDraftToDisk(appContext, updatedSession)

                    AiProcessingForegroundService.updateProgress(
                        appContext,
                        "✅ AI स्कैनिंग पूरी हुई!",
                        "सभी ${finalized.size} प्रश्न Step 5/5 पर तैयार हैं।",
                        100
                    )
                    AiProcessingForegroundService.stop(appContext)

                    withContext(Dispatchers.Main) {
                        onCompleted?.invoke(Result.success(GeminiQuestionParserService.ParsedExamResult(examRes?.examTitle ?: "", finalized)))
                    }
                } else {
                    val err = result.exceptionOrNull()?.message ?: "AI Parsing failed"
                    _taskState.value = _taskState.value.copy(
                        isRunning = false,
                        progressPercent = 0,
                        statusMessage = "⚠️ AI त्रुटि: $err",
                        isSuccess = false,
                        errorMessage = err,
                        lastFinishedTimestamp = System.currentTimeMillis()
                    )
                    AiProcessingForegroundService.stop(appContext)
                    withContext(Dispatchers.Main) {
                        onCompleted?.invoke(Result.failure(Exception(err)))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Background AI task failed", e)
                val err = e.message ?: "Unknown error"
                _taskState.value = _taskState.value.copy(
                    isRunning = false,
                    progressPercent = 0,
                    statusMessage = "⚠️ AI त्रुटि: $err",
                    isSuccess = false,
                    errorMessage = err,
                    lastFinishedTimestamp = System.currentTimeMillis()
                )
                AiProcessingForegroundService.stop(appContext)
                withContext(Dispatchers.Main) {
                    onCompleted?.invoke(Result.failure(e))
                }
            }
        }
    }

    fun startChapterQuestionsGeneration(
        context: Context,
        chapterTitle: String,
        subjectTitle: String,
        count: Int = 15,
        onCompleted: ((Result<List<QuizQuestion>>) -> Unit)? = null
    ) {
        val appContext = context.applicationContext
        currentJob?.cancel()

        val taskId = "chap_${System.currentTimeMillis()}"
        _taskState.value = AiTaskState(
            taskId = taskId,
            isRunning = true,
            taskType = "CHAPTER_GEN",
            progressPercent = 10,
            statusMessage = "🚀 Google AI $chapterTitle के लिए $count प्रश्न तैयार कर रहा है..."
        )

        AiProcessingForegroundService.start(
            appContext,
            "🤖 AI प्रश्न जनरेशन जारी है...",
            "$chapterTitle ($subjectTitle) के प्रश्न तैयार हो रहे हैं..."
        )

        currentJob = applicationScope.launch {
            try {
                val result = GeminiQuestionParserService.generateChapterQuestionsWithGemini(
                    chapterTitle = chapterTitle,
                    subjectTitle = subjectTitle,
                    count = count,
                    context = appContext,
                    onProgress = { msg, pct ->
                        _taskState.value = _taskState.value.copy(
                            progressPercent = pct,
                            statusMessage = msg
                        )
                        AiProcessingForegroundService.updateProgress(
                            appContext,
                            "🤖 AI प्रश्न जनरेशन जारी है...",
                            msg,
                            pct
                        )
                    }
                )

                if (result.isSuccess && !result.getOrNull().isNullOrEmpty()) {
                    val questions = result.getOrNull()!!
                    _taskState.value = _taskState.value.copy(
                        isRunning = false,
                        progressPercent = 100,
                        statusMessage = "✅ ${questions.size} नए प्रश्न तैयार हो गए!",
                        parsedQuestions = questions,
                        isSuccess = true,
                        errorMessage = null,
                        lastFinishedTimestamp = System.currentTimeMillis()
                    )
                    AiProcessingForegroundService.updateProgress(
                        appContext,
                        "✅ AI प्रश्न तैयार हुए!",
                        "${questions.size} नए प्रश्न तैयार हो गए हैं।",
                        100
                    )
                    AiProcessingForegroundService.stop(appContext)
                    withContext(Dispatchers.Main) {
                        onCompleted?.invoke(Result.success(questions))
                    }
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Chapter generation failed"
                    _taskState.value = _taskState.value.copy(
                        isRunning = false,
                        progressPercent = 0,
                        statusMessage = "⚠️ AI त्रुटि: $err",
                        isSuccess = false,
                        errorMessage = err,
                        lastFinishedTimestamp = System.currentTimeMillis()
                    )
                    AiProcessingForegroundService.stop(appContext)
                    withContext(Dispatchers.Main) {
                        onCompleted?.invoke(Result.failure(Exception(err)))
                    }
                }
            } catch (e: Exception) {
                val err = e.message ?: "Unknown error"
                _taskState.value = _taskState.value.copy(
                    isRunning = false,
                    progressPercent = 0,
                    statusMessage = "⚠️ AI त्रुटि: $err",
                    isSuccess = false,
                    errorMessage = err,
                    lastFinishedTimestamp = System.currentTimeMillis()
                )
                AiProcessingForegroundService.stop(appContext)
                withContext(Dispatchers.Main) {
                    onCompleted?.invoke(Result.failure(e))
                }
            }
        }
    }

    fun cancelTask(context: Context) {
        currentJob?.cancel()
        currentJob = null
        _taskState.value = _taskState.value.copy(
            isRunning = false,
            statusMessage = "⏹️ AI प्रक्रिया रोकी गई",
            errorMessage = "रद्द किया गया"
        )
        clearDraftFromDisk(context)
        AiProcessingForegroundService.stop(context.applicationContext)
    }

    fun clearState(context: Context? = null) {
        _taskState.value = AiTaskState()
        if (context != null) {
            clearDraftFromDisk(context)
        }
    }
}
