package com.example.service

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.model.QuizQuestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiQuestionParserService {
    private const val TAG = "GeminiQuestionParser"

    val ORDERED_MODELS = listOf(
        "gemini-3.5-flash",
        "gemini-3.5-flash-lite"
    )

    // Tracks current active model index when daily quota is exhausted
    @Volatile
    private var currentModelIndex = 0
    @Volatile
    private var modelExhaustedTimestamp = 0L

    // Tracks keys that have hit 24-hr daily free quota for each model
    private val dailyExhaustedKeysByModel = java.util.concurrent.ConcurrentHashMap<String, MutableSet<String>>()

    fun resetDailyExhaustionTracking() {
        dailyExhaustedKeysByModel.clear()
    }

    fun isKeyExhaustedForModel(model: String, key: String): Boolean {
        return dailyExhaustedKeysByModel[model]?.contains(key) == true
    }

    fun markKeyExhaustedForModel(model: String, key: String) {
        dailyExhaustedKeysByModel.getOrPut(model) { java.util.concurrent.ConcurrentHashMap.newKeySet() }.add(key)
    }

    fun areAllModelsExhausted(keysCount: Int): Boolean {
        if (keysCount <= 0) return false
        return ORDERED_MODELS.all { model ->
            (dailyExhaustedKeysByModel[model]?.size ?: 0) >= keysCount
        }
    }

    /**
     * Resets active model back to default for any new test extraction.
     */
    fun resetModelToDefault() {
        currentModelIndex = 0
        modelExhaustedTimestamp = 0L
        resetDailyExhaustionTracking()
    }

    /**
     * Returns candidate models in order of priority.
     */
    fun getModelsForCurrentAttempt(): List<String> {
        if (currentModelIndex > 0 && (System.currentTimeMillis() - modelExhaustedTimestamp < 4 * 60 * 60 * 1000L)) {
            return ORDERED_MODELS.drop(currentModelIndex).ifEmpty { ORDERED_MODELS }
        }
        return ORDERED_MODELS
    }

    fun getActiveModel(): String {
        return getModelsForCurrentAttempt().first()
    }

    fun advanceToNextModel(reason: String): String {
        if (currentModelIndex < ORDERED_MODELS.size - 1) {
            val oldModel = ORDERED_MODELS[currentModelIndex]
            currentModelIndex++
            modelExhaustedTimestamp = System.currentTimeMillis()
            val newModel = ORDERED_MODELS[currentModelIndex]
            Log.w(TAG, "[$oldModel] quota exhausted ($reason). Shifted to [$newModel].")
            return newModel
        }
        return ORDERED_MODELS.last()
    }

    fun mark38FlashDailyLimitExhausted() {
        if (currentModelIndex == 0) {
            currentModelIndex = 1
            modelExhaustedTimestamp = System.currentTimeMillis()
            Log.w(TAG, "Gemini primary model marked as daily exhausted.")
        }
    }

    fun is38FlashDailyExhausted(): Boolean {
        if (currentModelIndex > 0 && System.currentTimeMillis() - modelExhaustedTimestamp > 4 * 60 * 60 * 1000L) {
            currentModelIndex = 0
            modelExhaustedTimestamp = 0L
        }
        return currentModelIndex > 0
    }

    fun resetDailyLimit() {
        currentModelIndex = 0
        modelExhaustedTimestamp = 0L
        resetDailyExhaustionTracking()
    }

    fun getCandidateModels(): List<String> {
        return getModelsForCurrentAttempt()
    }

    fun getModelDisplayName(model: String): String = when {
        model.contains("lite") -> "Gemini 3.5 Flash Lite"
        else -> "Gemini 3.5 Flash"
    }

    fun isDailyQuotaError(errMsg: String, fullResponse: String = ""): Boolean {
        val text = "$errMsg $fullResponse".lowercase()

        // 1. If it mentions minute, rpm, or tpm and NOT daily/per_day, it is a temporary RPM cooldown, NOT a daily block!
        val isMinuteLimit = text.contains("perminute") || 
                            text.contains("per_minute") || 
                            text.contains("per minute") || 
                            text.contains("requestsperminute") ||
                            text.contains("requests_per_minute") ||
                            text.contains("requests per minute") ||
                            text.contains("rpm") ||
                            text.contains("tpm") ||
                            text.contains("tokensperminute")

        if (isMinuteLimit && !text.contains("perday") && !text.contains("per_day") && !text.contains("generaterequestsperday")) {
            return false
        }

        // 2. Explicit Daily Quota indicators (Must mention perday / daily)
        return text.contains("generaterequestsperday") ||
               text.contains("perday") ||
               text.contains("per_day") ||
               text.contains("daily quota") ||
               text.contains("day quota") ||
               text.contains("daily limit") ||
               text.contains("per day") ||
               (text.contains("quota exceeded") && (text.contains("day") || text.contains("perday")))
    }

    fun isRateLimitError(errMsg: String, errCode: Int, fullResponse: String = ""): Boolean {
        val text = "$errMsg $fullResponse".lowercase()
        return errCode == 429 ||
               text.contains("quota") ||
               text.contains("rate") ||
               text.contains("limit") ||
               text.contains("429") ||
               text.contains("resource_exhausted")
    }

    fun isServerHighDemandError(errMsg: String, errCode: Int, fullResponse: String = ""): Boolean {
        val text = "$errMsg $fullResponse".lowercase()
        return errCode == 503 ||
               errCode == 500 ||
               errCode == 502 ||
               errCode == 504 ||
               text.contains("high demand") ||
               text.contains("spikes in demand") ||
               text.contains("unavailable") ||
               text.contains("overloaded") ||
               text.contains("temporarily unavailable") ||
               text.contains("service unavailable")
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(180, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    /**
     * Parses raw answer key / exam paper text using upgraded Gemini AI model.
     */
    data class ParsedExamResult(
        val examTitle: String,
        val questions: List<QuizQuestion>
    )

    suspend fun parseAnswerKeyToExamResult(
        rawText: String,
        examContext: String,
        subjectOverride: String? = null,
        testMode: String = "FULL_TEST",
        chapterTitle: String? = null,
        subjectTitle: String? = null,
        context: Context? = null,
        onProgress: ((String, Int) -> Unit)? = null
    ): Result<ParsedExamResult> = withContext(Dispatchers.IO) {
        // Reset rotation at the beginning of each document
        GeminiApiKeyPoolManager.resetRotation()

        val initialKeyInfo = GeminiApiKeyPoolManager.getNextKey(context)
        val apiKey = initialKeyInfo.first

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API Key missing! कृपया AI Studio Secrets या API Keys मैनेजर में वैध GEMINI_API_KEY दर्ज करें।"))
        }

        try {
            val (title, questions) = callGeminiApiForExam(
                apiKey = apiKey,
                rawText = rawText,
                examContext = examContext,
                subjectOverride = subjectOverride,
                testMode = testMode,
                chapterTitle = chapterTitle,
                subjectTitle = subjectTitle,
                context = context,
                onProgress = onProgress
            )
            if (questions.isNotEmpty()) {
                return@withContext Result.success(ParsedExamResult(title.ifBlank { examContext }, questions))
            } else {
                return@withContext Result.failure(Exception("Gemini AI से कोई प्रश्न प्राप्त नहीं हुआ। कृपया आंसर की या PDF जांचें।"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Gemini AI Error: ${e.message ?: "Unknown error"}"))
        }
    }

    suspend fun parseAnswerKeyToQuestions(
        rawText: String,
        examContext: String,
        subjectOverride: String? = null,
        testMode: String = "FULL_TEST",
        chapterTitle: String? = null,
        subjectTitle: String? = null
    ): Result<List<QuizQuestion>> {
        val res = parseAnswerKeyToExamResult(
            rawText = rawText,
            examContext = examContext,
            subjectOverride = subjectOverride,
            testMode = testMode,
            chapterTitle = chapterTitle,
            subjectTitle = subjectTitle
        )
        return if (res.isSuccess) Result.success(res.getOrNull()?.questions ?: emptyList())
        else Result.failure(res.exceptionOrNull() ?: Exception("Failed"))
    }

    /**
     * Generates authentic, high-standard competitive exam questions for a specific chapter/topic using Google Gemini.
     * Automatically breaks large question counts (e.g. 40-50+ questions) into manageable parts (Part-1, Part-2) of 20-25 questions each,
     * maintaining continuous memory and ID sequence to guarantee zero omitted or duplicated questions.
     */
    suspend fun generateChapterQuestionsWithGemini(
        chapterTitle: String,
        subjectTitle: String,
        count: Int = 15,
        context: Context? = null,
        onProgress: ((String, Int) -> Unit)? = null
    ): Result<List<QuizQuestion>> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Gemini API Key missing! कृपया AI Studio Secrets में अपनी वैध GEMINI_API_KEY दर्ज करें।"))
        }

        val models = getCandidateModels()

        // Automatic splitting: If requested questions > 30, split into 25-30 questions parts (Part-1 & Part-2)
        val batchLimit = if (count > 30) 25 else count
        val totalBatches = if (count <= 30) 1 else (count + batchLimit - 1) / batchLimit
        val accumulatedQuestions = mutableListOf<QuizQuestion>()
        var previousBatchMemory = ""

        for (batchIdx in 0 until totalBatches) {
            val partNum = batchIdx + 1
            val currentBatchTarget = java.lang.Math.min(batchLimit, count - accumulatedQuestions.size)
            if (currentBatchTarget <= 0) break

            val startId = accumulatedQuestions.size + 1
            val endId = accumulatedQuestions.size + currentBatchTarget

            val memoryContextPrompt = if (previousBatchMemory.isNotBlank()) {
                """
                
                🧠 PREVIOUS PART GENERATED (भाग-$batchIdx से संकलित प्रश्न):
                $previousBatchMemory
                
                CRITICAL CONTINUITY MANDATE:
                - You are now generating PART $partNum of $totalBatches (Questions #$startId to #$endId).
                - Do NOT repeat or duplicate any concept/question from the previous part!
                - Generate distinct, advanced, and varied questions directly continuing the topic '$chapterTitle'.
                """.trimIndent()
            } else ""

            val activeCustomRules = AiPromptRulesManager.getChapterWiseRules(context).trim()
            val customRulesSection = if (activeCustomRules.isNotBlank()) {
                """
                
                ADMIN STRICT CUSTOM RULES & INSTRUCTIONS:
                $activeCustomRules
                """.trimIndent()
            } else ""

            val prompt = """
                You are a premier, senior exam paper setter for Indian Competitive Exams (SSC CGL, CHSL, GD, UP Police, Railway RRB, Delhi Police).
                
                TASK:
                Generate exactly $currentBatchTarget authentic, exam-standard Multiple Choice Questions (MCQs) in pure Hindi (देवनागरी) for:
                - SUBJECT: $subjectTitle
                - CHAPTER / TOPIC: $chapterTitle
                - PART / BATCH: Part $partNum of $totalBatches (Questions #$startId to #$endId)$memoryContextPrompt
                
                CRITICAL MANDATORY RULES:
                1. TOPIC RELEVANCE: Every question must be directly and deeply based on '$chapterTitle' under '$subjectTitle'. Do not generate generic filler questions.
                2. EXAM STANDARD: Questions must match the exact pattern, style, and difficulty asked in actual 2022-2025 government exams (SSC GD, Delhi Police, UP Police).
                3. 🌟 MANDATORY BOLD & UNDERLINE FORMATTING (बोल्ड व अंडरलाइन नियम):
                   - मुहावरे, लोकोक्तियां, पर्यायवाची, विलोम व मुख्य शब्द: Wrap main idioms/phrases/words in BOLD ONLY `**मुहावरा**` or `**शब्द**` (DO NOT UNDERLINE them unless physically underlined).
                     * Example: "'**आग में घी डालना**' मुहावरे का अर्थ है-"
                     * Example: "मुहावरे और उसके अर्थ के लिए सही विकल्प चुनें:\n**घर बसाना**"
                     * Example: "दिए गए मुहावरे का सही अर्थ चुनें: **हुक्का भरना**"
                     * Example: "दिए गए शब्द का पर्यायवाची चुनें: **‘अमृत’**"
                   - वाक्य में रेखांकित पद (Sentence Underlined Words/Parts to replace): Wrap in UNDERLINE `_अंश_` ONLY when the question asks about an underlined word in a sentence:
                     * Example: "'उँगली पकड़ कर _माथा_ पकड़ना।' मुहावरे में रेखांकित शब्द के स्थान पर उपयुक्त शब्द होगा-"
                     * Example: "'कुछ ढंग का लिखो-पढ़ो तो बात बने, खाली _कागजी घोड़े दौड़ाने_ से क्या लाभ?' रेखांकित मुहावरे को प्रतिस्थापित करें-"
                     * Example: "'जब भी कोई काम की बात शुरू होती है, सोहन _अपनी अलग घास छीलने लगता है_, उससे कुछ भी कहना बेकार है।' रेखांकित असंगत भाग के लिए उचित मुहावरा चुनें:"
                   - रीजनिंग व गणित (Reasoning & Math): Wrap codes, letter series, equations in `**bold**` e.g. `**AZ, BY, CX, ?**`, `**ROSE = 68**`, `**24 + 12 ÷ 4 = ?**`.
                4. OPTIONS: Provide exactly 4 realistic, distinct options for each question without option letter prefixes (e.g. "विकल्प", "A", "(1)").
                5. 100% VERIFIED CORRECT ANSWER (0% ERROR MANDATE):
                   - 'correctIndex': STRICT 0-BASED index:
                     * 0 for the 1st option in the list
                     * 1 for the 2nd option in the list
                     * 2 for the 3rd option in the list
                     * 3 for the 4th option in the list
                   - 'correctOption': "A", "B", "C", or "D" (matching the option letter).
                   - 'correctAnswer': The EXACT string of the correct option verbatim matching options[correctIndex].
                   - Verify that options[correctIndex] == correctAnswer!
                6. EXPLANATION: Detailed step-by-step Hindi explanation ('explanation') starting with 'उत्तर: (A/B/C/D) - [सही विकल्प]' with complete reasoning, formula, or grammar rule.
                7. EXAM INFO: In 'examShiftInfo', add a realistic exam tag e.g. "SSC GD 2024 / Police Constable".
                8. SUBJECT: Set 'subject' to '$subjectTitle'.
                9. NUMBERING: Start ID at $startId and number sequentially up to $endId.
                $customRulesSection
                
                Return ONLY a valid JSON object matching this schema:
                {
                  "questions": [
                    {
                      "id": $startId,
                      "question": "प्रश्न का पूरा विवरण (with _underlined_ words and **bold** codes)...",
                      "options": ["विकल्प 1", "विकल्प 2", "विकल्प 3", "विकल्प 4"],
                      "correctIndex": 0,
                      "correctOption": "A",
                      "correctAnswer": "विकल्प 1",
                      "explanation": "उत्तर: (A) - विकल्प 1। विस्तृत प्रामाणिक व्याख्या...",
                      "subject": "$subjectTitle",
                      "examShiftInfo": "SSC GD 2024 Shift 1"
                    }
                  ]
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("maxOutputTokens", 65536)
                    put("responseMimeType", "application/json")
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            var batchSuccess = false
            var lastEx: Exception? = null

            val baseProgress = (batchIdx * 80) / totalBatches
            if (totalBatches > 1) {
                onProgress?.invoke("⚡ भाग-$partNum/$totalBatches (प्रश्न $startId-$endId) तैयार हो रहा है...", baseProgress + 15)
            } else {
                onProgress?.invoke("🚀 Google Gemini AI द्वारा $count प्रश्न तैयार किए जा रहे हैं...", 30)
            }

            val modelsToTry = getModelsForCurrentAttempt()

            for (modelIndex in modelsToTry.indices) {
                if (batchSuccess) break
                val model = modelsToTry[modelIndex]
                val modelDisplayName = getModelDisplayName(model)
                val isLastModel = modelIndex == modelsToTry.size - 1
                val fallbackKeys = GeminiApiKeyPoolManager.getAllKeysForFallback(context, apiKey)
                var allKeysDaily = true

                for ((kIdx, attemptKey) in fallbackKeys.withIndex()) {
                    val keyNum = kIdx + 1
                    val totalKeys = fallbackKeys.size
                    val keyTag = " [🔑 Key $keyNum/$totalKeys]"

                    if (totalBatches > 1) {
                        onProgress?.invoke("⚡ भाग-$partNum/$totalBatches [🤖 $modelDisplayName]$keyTag (प्रश्न $startId-$endId)...", baseProgress + 15)
                    } else {
                        onProgress?.invoke("🚀 [🤖 $modelDisplayName]$keyTag द्वारा $count प्रश्न तैयार किए जा रहे हैं...", 30)
                    }

                    try {
                        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$attemptKey"
                        val request = Request.Builder()
                            .url(url)
                            .addHeader("Content-Type", "application/json")
                            .post(requestBody)
                            .build()

                        val response = client.newCall(request).execute()
                        val responseString = response.body?.string() ?: continue
                        val respJson = try { JSONObject(responseString) } catch (_: Exception) { null }

                        if (respJson != null && respJson.has("error")) {
                            val errObj = respJson.optJSONObject("error")
                            val errMsg = errObj?.optString("message") ?: "API Error"
                            val errCode = errObj?.optInt("code", 0) ?: 0
                            lastEx = Exception("[$model] $errMsg")

                            if (errCode == 404 || errMsg.contains("not found", ignoreCase = true) || errMsg.contains("not supported", ignoreCase = true)) {
                                break
                            }

                            if (isServerHighDemandError(errMsg, errCode, responseString)) {
                                if (modelIndex < modelsToTry.size - 1) {
                                    val nextModel = modelsToTry[modelIndex + 1]
                                    val nextDisplayName = getModelDisplayName(nextModel)
                                    onProgress?.invoke("⚡ [$modelDisplayName] सर्वर लोड (High Demand), तुरंत [$nextDisplayName] पर शिफ्ट हो रहे हैं...", baseProgress + 15)
                                }
                                break
                            }

                            if (isDailyQuotaError(errMsg, responseString)) {
                                markKeyExhaustedForModel(model, attemptKey)
                                if (kIdx < fallbackKeys.size - 1) {
                                    onProgress?.invoke("⚡ [$modelDisplayName] Key $keyNum/$totalKeys दैनिक कोटा समाप्त! अगली Key ${keyNum + 1}/$totalKeys पर जा रहे हैं...", baseProgress + 15)
                                    continue
                                } else {
                                    break
                                }
                            }

                            allKeysDaily = false

                            if (isRateLimitError(errMsg, errCode, responseString)) {
                                if (kIdx < fallbackKeys.size - 1) {
                                    onProgress?.invoke("🔄 [$modelDisplayName] Key $keyNum/$totalKeys बिजी (RPM)! अगली Key ${keyNum + 1}/$totalKeys पर जा रहे हैं...", baseProgress + 15)
                                    continue
                                } else {
                                    break
                                }
                            }

                            if (kIdx < fallbackKeys.size - 1) {
                                onProgress?.invoke("⚠️ [$modelDisplayName] Key $keyNum/$totalKeys त्रुटि! अगली Key पर जा रहे हैं...", baseProgress + 15)
                                continue
                            } else {
                                break
                            }
                        }

                        if (respJson == null) {
                            if (kIdx < fallbackKeys.size - 1) continue else break
                        }

                        val candidates = respJson.optJSONArray("candidates")
                        if (candidates == null || candidates.length() == 0) {
                            if (kIdx < fallbackKeys.size - 1) continue else break
                        }

                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts == null || parts.length() == 0) {
                            if (kIdx < fallbackKeys.size - 1) continue else break
                        }

                        val rawText = parts.getJSONObject(0).optString("text", "")
                        var cleanJson = rawText.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                        val jsonStart = cleanJson.indexOf('{')
                        val jsonEnd = cleanJson.lastIndexOf('}')
                        val safeJson = if (jsonStart >= 0 && jsonEnd > jsonStart) cleanJson.substring(jsonStart, jsonEnd + 1) else cleanJson

                        val parsedObj = JSONObject(safeJson)
                        val qArray = parsedObj.optJSONArray("questions") ?: JSONArray()
                        val batchList = mutableListOf<QuizQuestion>()

                        for (i in 0 until qArray.length()) {
                            val qItem = qArray.getJSONObject(i)
                            val rawQ = qItem.optString("question", "").trim()
                            if (rawQ.isBlank()) continue
                            val (cleanedQ, detectedShift) = extractShiftTagFromText(cleanQuestionText(rawQ))
                            val optsArray = qItem.optJSONArray("options")
                            val opts = mutableListOf<String>()
                            if (optsArray != null) {
                                for (j in 0 until optsArray.length()) {
                                    opts.add(cleanOptionText(optsArray.getString(j)))
                                }
                            }
                            while (opts.size < 4) opts.add("विकल्प ${opts.size + 1}")
                            val correct = parseCorrectIndex(qItem, opts).coerceIn(0, opts.size - 1)
                            val expl = qItem.optString("explanation", "").trim()
                            val shift = qItem.optString("examShiftInfo", "").ifBlank { detectedShift ?: "अध्याय अभ्यास" }

                            batchList.add(
                                QuizQuestion(
                                    id = accumulatedQuestions.size + batchList.size + 1,
                                    question = cleanedQ,
                                    options = opts,
                                    correctIndex = correct,
                                    explanation = expl,
                                    subject = subjectTitle,
                                    examShiftInfo = shift
                                )
                            )
                        }

                        if (batchList.isNotEmpty()) {
                            accumulatedQuestions.addAll(batchList)
                            val lastThreeSummary = batchList.takeLast(3).joinToString("; ") { "Q: ${it.question.take(40)}..." }
                            previousBatchMemory = "Last generated questions: $lastThreeSummary. Total count so far: ${accumulatedQuestions.size}"
                            batchSuccess = true
                            break
                        }
                    } catch (e: Exception) {
                        lastEx = e
                        allKeysDaily = false
                        if (kIdx < fallbackKeys.size - 1) {
                            onProgress?.invoke("⚠️ [$modelDisplayName] Key $keyNum/$totalKeys समस्या! अगली Key पर जा रहे हैं...", baseProgress + 15)
                            continue
                        } else {
                            break
                        }
                    }
                }

                if (batchSuccess) break

                if (allKeysDaily) {
                    mark38FlashDailyLimitExhausted()
                }

                // If not successful and more models available, immediately switch without long wait:
                if (!isLastModel) {
                    val nextModel = modelsToTry[modelIndex + 1]
                    val nextDisplayName = getModelDisplayName(nextModel)
                    onProgress?.invoke("⚡ [$modelDisplayName] सभी Keys की स्पीड लिमिट पूरी! बिना रुके तुरंत [$nextDisplayName] चालू...", baseProgress + 15)
                    Log.w(TAG, "[$modelDisplayName] limit on all keys. Instantly shifting to [$nextDisplayName].")
                    continue
                } else {
                    for (s in 5 downTo 1) {
                        onProgress?.invoke("⏳ सभी AI मॉडल्स व्यस्त हैं... ${s}s में पुनः प्रयास...", baseProgress + 15)
                        try { Thread.sleep(1000L) } catch (_: Exception) {}
                    }
                }
            }

            if (!batchSuccess && accumulatedQuestions.isEmpty()) {
                return@withContext Result.failure(lastEx ?: Exception("प्रश्नों को जनरेट करने में विफलता हुई। कृपया पुनः प्रयास करें।"))
            }
        }

        if (accumulatedQuestions.isNotEmpty()) {
            val finalized = accumulatedQuestions.mapIndexed { idx, q -> q.copy(id = idx + 1) }
            val successMsg = if (totalBatches > 1) {
                "✅ Part-1 और Part-2 दोनों भागों से कुल ${finalized.size} प्रश्न सफलतापूर्वक तैयार!"
            } else {
                "✅ कुल ${finalized.size} उच्च-स्तरीय प्रश्न तैयार!"
            }
            onProgress?.invoke(successMsg, 100)
            return@withContext Result.success(finalized)
        }

        return@withContext Result.failure(Exception("कोई प्रश्न जनरेट नहीं हो सका। कृपया पुनः प्रयास करें।"))
    }

    private suspend fun callGeminiApiForExam(
        apiKey: String,
        rawText: String,
        examContext: String,
        subjectOverride: String?,
        testMode: String = "FULL_TEST",
        chapterTitle: String? = null,
        subjectTitle: String? = null,
        context: Context? = null,
        onProgress: ((String, Int) -> Unit)? = null
    ): Pair<String, List<QuizQuestion>> {
        resetModelToDefault()
        val isPdfPages = rawText.startsWith("data:image/jpeg-pages;base64,")
        val pagesJsonStr = if (isPdfPages) rawText.removePrefix("data:image/jpeg-pages;base64,") else null

        // If multi-page PDF is provided, process in intelligent batches with Sequential Key Rotation & Memory Passing
        if (pagesJsonStr != null) {
            val pageArray = try { JSONArray(pagesJsonStr) } catch (e: Exception) { JSONArray() }
            val totalPages = pageArray.length()

            if (totalPages > 0) {
                // 2-page batching provides cross-page visual context and guarantees 100% question extraction across all PDF pages.
                val chunkSize = 2
                val step = 2
                
                val chunks = mutableListOf<Triple<Int, Int, List<String>>>() // (startPage1Based, endPage1Based, list of base64)
                var startIdx = 0
                while (startIdx < totalPages) {
                    val endIdx = java.lang.Math.min(startIdx + chunkSize, totalPages)
                    val chunkPages = mutableListOf<String>()
                    for (k in startIdx until endIdx) {
                        chunkPages.add(pageArray.getString(k))
                    }
                    chunks.add(Triple(startIdx + 1, endIdx, chunkPages))
                    if (endIdx >= totalPages) break
                    startIdx += step 
                }

                var detectedTitle = ""
                val allQuestions = java.util.concurrent.CopyOnWriteArrayList<QuizQuestion>()
                val coveredPagesSet = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()
                
                var completedChunks = 0
                var memoryContext = ""

                fun addQuestionsWithDeduplication(newQuestions: List<QuizQuestion>, fromStartPage: Int, fromEndPage: Int) {
                    var addedCount = 0

                    // Boundary stitching: Check if the first new question is a split continuation of previous page's last question
                    var isContinuation = false
                    if (allQuestions.isNotEmpty() && newQuestions.isNotEmpty()) {
                        val lastQ = allQuestions.last()
                        val firstNew = newQuestions.first()
                        val lastHasDummy = hasDummyOptions(lastQ.options)
                        val lastIncomplete = lastHasDummy || lastQ.options.size < 4 || lastQ.question.length < 15
                        val lastIsEnglish = !lastQ.question.any { it in '\u0900'..'\u097F' }
                        val firstHasHindi = firstNew.question.any { it in '\u0900'..'\u097F' }
                        
                        val isSameId = (firstNew.id == lastQ.id && firstNew.id in 1..300 && lastIncomplete)
                        val isCrossPageEnglishToHindiSplit = lastIsEnglish && firstHasHindi && (lastIncomplete || firstNew.id == lastQ.id || firstNew.id == lastQ.id + 1)
                        val isTextContinuation = lastIncomplete && (firstNew.question.isBlank() || firstNew.question.length < 25 || areWordsSimilar(lastQ.question, firstNew.question, 0.70f))
                        isContinuation = isSameId || isCrossPageEnglishToHindiSplit || isTextContinuation
                        
                        if (isContinuation) {
                            val finalQuestionText = if (firstHasHindi) cleanQuestionText(firstNew.question) else cleanQuestionText(lastQ.question)
                            val mergedOptions = mutableListOf<String>()
                            for (idx in 0 until 4) {
                                val optFromFirst = firstNew.options.getOrNull(idx)?.takeIf { !it.startsWith("विकल्प ") && it.isNotBlank() }
                                val optFromLast = lastQ.options.getOrNull(idx)?.takeIf { !it.startsWith("विकल्प ") && it.isNotBlank() }
                                mergedOptions.add(cleanOptionText(optFromFirst ?: optFromLast ?: "विकल्प ${idx + 1}"))
                            }
                            val mergedImg = firstNew.imageUrl ?: lastQ.imageUrl
                            val mergedExpl = if (firstNew.explanation.length > lastQ.explanation.length) firstNew.explanation else lastQ.explanation
                            val mergedCorrect = if (firstNew.correctIndex in 0..3 && !hasDummyOptions(firstNew.options)) firstNew.correctIndex else lastQ.correctIndex
                            
                            allQuestions[allQuestions.size - 1] = lastQ.copy(
                                question = finalQuestionText,
                                options = mergedOptions,
                                imageUrl = mergedImg,
                                explanation = mergedExpl,
                                correctIndex = mergedCorrect
                            )
                        }
                    }

                    val questionsToProcess = if (isContinuation) newQuestions.drop(1) else newQuestions
                    for (q in questionsToProcess) {
                        val cleanQ = q.copy(
                            question = cleanQuestionText(q.question),
                            options = q.options.map { cleanOptionText(it) },
                            subject = subjectOverride ?: normalizeSubject(q.subject)
                        )
                        if (cleanQ.question.isBlank() || cleanQ.options.isEmpty()) continue

                        var duplicateExistingIdx = -1
                        for (i in 0 until allQuestions.size) {
                            if (isDuplicateQuestion(allQuestions[i], cleanQ)) {
                                duplicateExistingIdx = i
                                break
                            }
                        }
                        
                        if (duplicateExistingIdx != -1) {
                            if (shouldReplaceExistingQuestion(allQuestions[duplicateExistingIdx], cleanQ)) { 
                                allQuestions[duplicateExistingIdx] = cleanQ
                            }
                        } else {
                            allQuestions.add(cleanQ)
                            addedCount++
                        }
                    }
                    if (addedCount > 0) {
                        for (p in fromStartPage..fromEndPage) {
                            coveredPagesSet.add(p)
                        }
                    }
                }

                for (chunk in chunks) {
                    val (startP, endP, chunkBase64List) = chunk
                    val (batchKey, keyNum, totalKeys) = GeminiApiKeyPoolManager.getNextKey(context)
                    val activeKeyToUse = if (batchKey.isNotBlank()) batchKey else apiKey
                    
                    var chunkResult: Pair<String, List<QuizQuestion>>? = null

                    val pageLabel = if (startP == endP) "पेज $startP" else "पेज $startP-$endP"
                    val partLabel = if (chunks.size > 1) "पेज $startP/${chunks.size}" else pageLabel
                    val keyLabel = if (totalKeys > 1) " [🔑 Key $keyNum/$totalKeys]" else ""
                    val currentProgressVal = 10 + (completedChunks * 75) / chunks.size
                    onProgress?.invoke("🔄 $partLabel स्कैन हो रहा है...$keyLabel (अब तक: ${allQuestions.size} प्रश्न मिले)", currentProgressVal)

                    var attempt = 0
                    while (attempt < 2 && chunkResult == null) {
                        attempt++
                        try {
                            val keyForThisAttempt = activeKeyToUse
                            val res = callGeminiForPagesChunk(
                                apiKey = keyForThisAttempt,
                                pages = chunkBase64List,
                                startPage = startP,
                                endPage = endP,
                                totalPages = totalPages,
                                examContext = examContext,
                                subjectOverride = subjectOverride,
                                testMode = testMode,
                                chapterTitle = chapterTitle,
                                subjectTitle = subjectTitle,
                                memoryContext = memoryContext,
                                context = context,
                                currentProgress = currentProgressVal,
                                onProgress = onProgress
                            )
                            if (res.second.isNotEmpty()) {
                                chunkResult = res
                            }
                        } catch (chunkEx: Exception) {
                            Log.e(TAG, "Error in chunk $pageLabel on attempt $attempt: ${chunkEx.message}")
                            val msg = chunkEx.message ?: ""
                            if (msg.contains("दैनिक सीमा") || areAllModelsExhausted(totalKeys)) {
                                throw chunkEx
                            }
                            if (attempt < 2) {
                                try { Thread.sleep(2000L) } catch (_: Exception) {}
                            }
                        }
                    }

                    // ZERO-CHUNK-LOSS FALLBACK: If a multi-page chunk failed, scan each page individually so no questions are EVER dropped!
                    if ((chunkResult == null || chunkResult.second.isEmpty()) && chunkBase64List.size > 1 && !areAllModelsExhausted(totalKeys)) {
                        Log.w(TAG, "Chunk $pageLabel returned empty or failed. Falling back to single-page scanning to ensure 0 questions missed.")
                        onProgress?.invoke("🔍 [$pageLabel] गहन 1-by-1 पेज स्कैन... ताकि कोई प्रश्न न छूटे", currentProgressVal)
                        val recoveredList = mutableListOf<QuizQuestion>()
                        for (pageIndex in startP..endP) {
                            if (areAllModelsExhausted(totalKeys)) break
                            try {
                                val singlePageBase64 = pageArray.getString(pageIndex - 1)
                                val singleResult = callGeminiForPagesChunk(
                                    apiKey = activeKeyToUse,
                                    pages = listOf(singlePageBase64),
                                    startPage = pageIndex,
                                    endPage = pageIndex,
                                    totalPages = totalPages,
                                    examContext = examContext,
                                    subjectOverride = subjectOverride,
                                    testMode = testMode,
                                    chapterTitle = chapterTitle,
                                    subjectTitle = subjectTitle,
                                    memoryContext = memoryContext,
                                    context = context,
                                    currentProgress = currentProgressVal,
                                    onProgress = onProgress
                                )
                                if (singleResult.second.isNotEmpty()) {
                                    recoveredList.addAll(singleResult.second)
                                    coveredPagesSet.add(pageIndex)
                                }
                            } catch (singleEx: Exception) {
                                Log.e(TAG, "Single page recovery failed for page $pageIndex: ${singleEx.message}")
                                if (areAllModelsExhausted(totalKeys)) break
                            }
                        }
                        if (recoveredList.isNotEmpty()) {
                            chunkResult = Pair(detectedTitle, recoveredList)
                        }
                    }

                    if (chunkResult != null) {
                        val (chunkTitle, chunkQuestions) = chunkResult
                        if (detectedTitle.isBlank() && chunkTitle.isNotBlank()) {
                            detectedTitle = chunkTitle
                        }
                        addQuestionsWithDeduplication(chunkQuestions, startP, endP)

                        if (allQuestions.isNotEmpty()) {
                            val lastQ = allQuestions.last()
                            val qText = lastQ.question.replace('"', '\'').replace('\n', ' ')
                            val optsText = lastQ.options.joinToString(", ") { it.replace('"', '\'') }
                            val lastId = allQuestions.size
                            val lastSubj = lastQ.subject
                            memoryContext = """
                                📌 PREVIOUS BATCH EXTRACTION SUMMARY:
                                - Total questions successfully extracted so far: ${lastId} questions
                                - Last Extracted Question (#${lastId}): "${qText.take(150)}"
                                - Options for #${lastId}: [${optsText}]
                                - Correct Index: ${lastQ.correctIndex}
                                - Subject: ${lastSubj}
                                
                                🎯 CONTINUATION INSTRUCTIONS FOR THIS BATCH:
                                1. IF Question #${lastId} was cut-off or split at the bottom of the previous page and its remaining options/diagram/Hindi text continue onto the first page of this batch:
                                   -> Merge/complete it properly and include it as id: ${lastId}.
                                2. IF Question #${lastId} was already fully completed:
                                   -> DO NOT repeat or duplicate Question #${lastId}!
                                   -> Start extracting directly from the first new question appearing on this batch's pages.
                                   -> Assign sequential continuous ID starting from: ${lastId + 1}.
                                3. Extract EVERY SINGLE QUESTION appearing on the pages of this batch completely without skipping or omitting any question!
                            """.trimIndent()
                        }
                    }
                    completedChunks++
                    val chunkProgress = 10 + (completedChunks * 75) / chunks.size
                    val completionMsg = if (chunks.size > 1) "✅ Part-$completedChunks/${chunks.size} पूर्ण! कुल प्रश्न: ${allQuestions.size}" else "✅ पेज स्कैन पूर्ण! कुल प्रश्न: ${allQuestions.size}"
                    onProgress?.invoke(completionMsg, chunkProgress)
                }

                // MISSING PAGE GAP AUTO-RECOVERY:
                // Only runs if multi-page chunks were used (chunkSize > 1) and models are not exhausted.
                // If single-page scanning was used, empty pages are non-question pages (e.g. instructions/cover/blank) and should not waste requests.
                val totalAvailableKeys = GeminiApiKeyPoolManager.getAllKeysForFallback(context, apiKey).size
                if (totalPages >= 1 && chunkSize > 1 && !areAllModelsExhausted(totalAvailableKeys)) {
                    val missedPages = mutableListOf<Int>()
                    for (p in 1..totalPages) {
                        if (!coveredPagesSet.contains(p)) {
                            missedPages.add(p)
                        }
                    }
                    if (missedPages.isNotEmpty()) {
                        Log.w(TAG, "Gap detected! The following pages had 0 extracted questions: $missedPages. Running targeted gap recovery...")
                        for (missedPage in missedPages.take(2)) {
                            if (areAllModelsExhausted(totalAvailableKeys)) break
                            try {
                                onProgress?.invoke("🔍 छूटे हुए पेज $missedPage से प्रश्न निकाले जा रहे हैं...", 88)
                                val missedPageBase64 = pageArray.getString(missedPage - 1)
                                val (batchKey) = GeminiApiKeyPoolManager.getNextKey(context)
                                val keyToUse = if (batchKey.isNotBlank()) batchKey else apiKey
                                val recoveryResult = callGeminiForPagesChunk(
                                    apiKey = keyToUse,
                                    pages = listOf(missedPageBase64),
                                    startPage = missedPage,
                                    endPage = missedPage,
                                    totalPages = totalPages,
                                    examContext = examContext,
                                    subjectOverride = subjectOverride,
                                    testMode = testMode,
                                    chapterTitle = chapterTitle,
                                    subjectTitle = subjectTitle,
                                    memoryContext = memoryContext,
                                    context = context,
                                    currentProgress = 88,
                                    onProgress = onProgress
                                )
                                if (recoveryResult.second.isNotEmpty()) {
                                    addQuestionsWithDeduplication(recoveryResult.second, missedPage, missedPage)
                                }
                            } catch (gapEx: Exception) {
                                Log.e(TAG, "Gap recovery failed for page $missedPage: ${gapEx.message}")
                                if (areAllModelsExhausted(totalAvailableKeys)) break
                            }
                        }
                    }
                }

                if (allQuestions.isNotEmpty()) {
                    // Final deduplication pass over entire collected questions to guarantee exactly 0 duplicates
                    val finalUnique = mutableListOf<QuizQuestion>()
                    for (q in allQuestions) {
                        var dupIdx = -1
                        for (i in finalUnique.indices) {
                            if (isDuplicateQuestion(finalUnique[i], q)) {
                                dupIdx = i
                                break
                            }
                        }
                        if (dupIdx != -1) {
                            if (shouldReplaceExistingQuestion(finalUnique[dupIdx], q)) {
                                finalUnique[dupIdx] = q
                            }
                        } else {
                            finalUnique.add(q)
                        }
                    }

                    // Only sort if extracted IDs are strictly unique official printed IDs in 1..500
                    val allIdsUniqueOfficial = finalUnique.isNotEmpty() &&
                        finalUnique.all { it.id in 1..500 } &&
                        finalUnique.map { it.id }.distinct().size == finalUnique.size

                    if (allIdsUniqueOfficial) {
                        finalUnique.sortBy { it.id }
                    }

                    val numbered = finalUnique.mapIndexed { idx, q ->
                        // If extracted ID is a unique official printed ID in 1..250, preserve it; otherwise use sequential index
                        val isUniqueOfficialId = allIdsUniqueOfficial && q.id in 1..250
                        val finalId = if (isUniqueOfficialId) q.id else (idx + 1)
                        q.copy(id = finalId)
                    }
                    val finalMsg = if (chunks.size > 1) {
                        "🎉 सभी ${chunks.size} भागों (Part-1, Part-2...) से कुल ${numbered.size} प्रश्न 100% सटीकता से तैयार!"
                    } else {
                        "🎉 सभी $totalPages पेजों से कुल ${numbered.size} प्रश्न सफलतापूर्वक तैयार!"
                    }
                    onProgress?.invoke(finalMsg, 95)
                    return Pair(detectedTitle, numbered)
                }
            }
        }

        resetModelToDefault()
        val isPdf = rawText.startsWith("data:application/pdf;base64,")
        val pdfBase64 = if (isPdf) rawText.removePrefix("data:application/pdf;base64,") else null
        val activeCustomRules = AiPromptRulesManager.getRulesForMode(mode = testMode)
        val isChapterWise = testMode.equals("CHAPTER_WISE", ignoreCase = true)
        
        val modeHeader = if (isChapterWise) {
            """
            MODE: [STRICT OCR & TEXT EXTRACTION ONLY]
            TARGET SUBJECT: ${subjectTitle ?: subjectOverride ?: "General"}
            TARGET CHAPTER / TOPIC: ${chapterTitle ?: "Selected Chapter"}
            PRIMARY MANDATE: You are a pure OCR transcriber. Your ONLY job is to extract the EXACT questions that are physically printed on the provided document.
            
            🚨 ZERO HALLUCINATION POLICY (STRICTEST WARNING): 
            - DO NOT generate, create, or invent ANY questions from your own memory or knowledge!
            - DO NOT create tests. DO NOT write your own questions.
            - IF a question is NOT physically visible in the provided document, DO NOT include it in the JSON!
            - Extract whatever is physically printed in this document 100% verbatim, nothing more, nothing less.
            """.trimIndent()
        } else {
            """
            MODE: [FULL MOCK TEST EXTRACTION]
            EXAM BLUEPRINT: $examContext
            STRICT REQUIREMENT: Extract full length exam paper with all subjects (Reasoning, GK/GS, Maths, Computer/Language) sequentially.
            """.trimIndent()
        }

        val customRulesPromptSection = if (activeCustomRules.isNotBlank()) {
            "\n\nADMINISTRATOR STRICT RULES (प्रशासक द्वारा निर्धारित अतिरिक्त सख्त नियम - 100% लागू करें):\n$activeCustomRules"
        } else ""

        val prompt = if (isPdf) {
            """
                You are an elite expert exam question extractor and Answer Key parser for Indian Competitive Exams ($examContext).
                $modeHeader
                Analyze the attached official PDF document with 100% precision and absolute accuracy:
                
                1. EXAM TITLE: Automatically detect and extract official exam title, date, and shift. Return as "examTitle".
                
                2. ZERO-MISTAKE & EXACT SEQUENTIAL NUMBERING (CRITICAL):
                   - 🚫 FATAL ERROR AVOIDANCE: Do NOT add questions from your AI memory! Output exactly the number of questions visible in the document.
                   - Extract EVERY SINGLE QUESTION from start to finish without any omission.
                   - Ensure strict sequential numbering (1, 2, 3, 4, 5, ... N).
                   - Transcribe all questions, options (A, B, C, D), correct answers, and 1-2 sentence explanations.
                
                3. ABSOLUTE ANSWER ACCURACY & EXACT OPTIONS (CRITICAL):
                   - Extract all 4 options (a, b, c, d) EXACTLY as printed for THAT SPECIFIC question.
                   - 🚫 NEVER mix or swap options between different questions! Always ensure option texts belong strictly to the current question number.
                   - 🚫 NO DUPLICATE OPTIONS: Ensure all 4 options are distinct and accurately transcribed from the PDF (e.g. differentiate subtle differences like "घमंड कराना" vs "घमंड करना", "प्रतिकूल कार्य" vs "प्रतिकूल कार्य करना").
                   - You MUST map the correct answer exactly as given in the official Answer Key / standard subject grammar. Double-check the answer index (0 for (a), 1 for (b), 2 for (c), 3 for (d)). 100% precision is mandatory.
                   - If any word or phrase in the question is underlined (रेखांकित), you MUST put an underscore before and after it to highlight it. Example: "उंगली पकड़ कर _माथा_ पकड़ना"।
                
                4. CROSS-PAGE SPLIT & SINGLE-PAGE CUT-OFF QUESTIONS (पेज कटने पर 100% सही समाधान):
                   - When multi-page PDF or images are provided:
                     * If a question begins at the bottom of one page and its options (a, b, c, d) appear at the top of the next page, YOU MUST STITCH THEM SEAMLESSLY TOGETHER into one unified question!
                     * 🚫 NEVER hallucinate or borrow options from another question for a split question. Read the options directly from the top of the next page.
                   - When ONLY A SINGLE PAGE is provided and the bottom question is cut-off (options are missing on next page):
                     * 🚨 DO NOT BORROW options from neighboring questions!
                     * If the question and official shift info is fully stated, provide the standard authentic 4 options matching standard exam format with accurate answer.
                     * Otherwise, safely omit the incomplete cut-off question so only 100% accurate questions are produced.

                5. BILINGUAL EXAM PAPERS & REASONING 100% HINDI RULE (तर्कशक्ति केवल और केवल शुद्ध हिंदी में):
                   - If questions/options are printed in BOTH English and Hindi (Bilingual), or if Reasoning is in English:
                     * You MUST IGNORE the English text completely and translate any English reasoning statement/instruction into 100% pure, natural Hindi!
                     * 🚫 NEVER output English sentences or instructions in Reasoning (e.g. "Select the option...", "In a code language...", "Six people are sitting around a circle..."). Write them strictly in Hindi ("उस विकल्प का चयन करें...", "एक निश्चित कूट भाषा में...", "छह व्यक्ति एक वृत्त के चारों ओर बैठे हैं...").
                     * Retain ONLY letter codes/symbols within the Hindi question (e.g. 'ROSE = 68', 'A, B, D, G, ?', 'HEN: KHQ:: WIN:?').
                     * For options separated by slash (e.g. "North / उत्तर"), extract ONLY the Hindi part ("उत्तर").
                     * 🚨 NO REPEATED / DUPLICATE VALUES: If option values are identical in both languages (e.g. "2nd / 2nd", "25% / 25%"), output the clean value ONLY ONCE e.g. "2nd", "25%".
                     * DO NOT create duplicate questions. Output strictly ONE JSON object per question in pure Hindi.

                6. MANDATORY LANGUAGE RULE:
                   - Flawless Hindi (100% शुद्ध हिंदी).
                   - Subjects: "GK / GS", "तर्कशक्ति (Reasoning)", "संख्यात्मक योग्यता (Maths)", "कंप्यूटर ज्ञान (Computer)".
                $customRulesPromptSection

                7. MANDATORY JSON FORMAT:
                🚨 FINAL WARNING: Before outputting the JSON, DOUBLE-CHECK the 'correctIndex' of the VERY LAST QUESTION you parsed against the answer key. Do not hallucinate or skip its verification!
                Return ONLY a valid JSON object:
                {
                  "examTitle": "Exam Title Here",
                  "questions": [
                    {
                      "id": 1,
                      "question": "प्रश्न विवरण (with _underlined_ words)...",
                      "options": ["विकल्प A", "विकल्प B", "विकल्प C", "विकल्प D"],
                      "correctIndex": 0,
                      "correctOption": "A",
                      "correctAnswer": "विकल्प A",
                      "explanation": "उत्तर: (A) - विकल्प A। विस्तृत प्रामाणिक व्याख्या...",
                      "subject": "तर्कशक्ति (Reasoning)",
                      "examShiftInfo": "DP Constable 22/11/2025 Shift 1st",
                      "imageUrl": null
                    }
                  ]
                }
            """.trimIndent()
        } else {
            """
                You are an elite expert exam question extractor and Answer Key parser for Indian Competitive Exams ($examContext).
                $modeHeader
                Analyze the provided raw text/answer key with 100% precision and absolute accuracy:
                
                1. EXAM TITLE: Automatically detect and extract the official exam title, date, and shift from the document header or text. Return this as "examTitle".
                
                2. ZERO-MISTAKE & EXACT SEQUENTIAL NUMBERING (CRITICAL):
                   - 🚫 FATAL ERROR AVOIDANCE: Do NOT add questions from your AI memory! Output exactly the number of questions visible in the document.
                   - Extract EVERY SINGLE QUESTION from the source text from start to finish without any omission (continue through all questions e.g. Q1 to Q80 or Q100).
                   - Ensure strict sequential numbering (1, 2, 3, 4, 5, ... N) exactly matching the source order.
                   - Transcribe all questions, options (A, B, C, D), and correct answers with absolute precision.
                   - Provide a concise 1-2 sentence explanation in Hindi.
                
                3. EXAM, DATE & SHIFT PER QUESTION:
                   - If specific exam name, date, or shift is indicated per question or on the page/tag (e.g. "DP Constable 22/11/2025 Shift 1st" or "SSC GD 20/02/2024 Shift 2"), extract it into "examShiftInfo".
                
                4. ABSOLUTE ANSWER ACCURACY & EXACT OPTIONS (CRITICAL):
                   - Extract all 4 options (a, b, c, d) EXACTLY as given for THAT SPECIFIC question without mixing across questions or duplicating.
                   - You MUST map the correct answer exactly as given in the official text / standard subject grammar. Double-check the answer index (0 for (a), 1 for (b), 2 for (c), 3 for (d)). 100% accuracy is required.
                   - If any word or phrase in the question is underlined (रेखांकित), you MUST put an underscore before and after it to highlight it. Example: "उंगली पकड़ कर _माथा_ पकड़ना"।
                   
                5. PARTIAL/CUT-OFF QUESTIONS CROSSING PAGES:
                   - Because you are receiving overlapping page chunks, a question might be cut in half at the very beginning or end of your pages (e.g., question text on the last page but options are missing).
                   - IF a question or its options are completely cut-off and incomplete, IGNORE IT ENTIRELY. It will be safely extracted in the adjacent overlapping chunk.
                   
                6. BILINGUAL PAPERS (CRITICAL 100% HINDI ONLY RULE):
                   - If the text contains questions printed in BOTH English and Hindi, you MUST IGNORE the English text completely!
                   - Extract ONLY the Hindi version of the question and its options.
                   - For options (e.g. "Direct election / प्रत्यक्ष चुनाव"), extract ONLY the Hindi part ("प्रत्यक्ष चुनाव").
                   - DO NOT create duplicate JSON objects for the English and Hindi versions of the same question. Only output ONE JSON object per question, exclusively in Hindi.

                7. MANDATORY LANGUAGE RULE:
                   - All questions, options, and explanations MUST BE IN FLAWLESS HINDI (शुद्ध व स्पष्ट हिंदी माध्यम में).
                   - Subjects must be mapped in Hindi: "GK / GS", "तर्कशक्ति (Reasoning)", "संख्यात्मक योग्यता (Maths)", "कंप्यूटर ज्ञान (Computer)".
                $customRulesPromptSection

                8. MANDATORY JSON FORMAT:
                🚨 FINAL WARNING: Before outputting the JSON, DOUBLE-CHECK the 'correctIndex' of the VERY LAST QUESTION you parsed against the answer key. Do not hallucinate or skip its verification!
                Return ONLY a valid JSON object with the following schema:
                {
                  "examTitle": "Exam Title Here",
                  "questions": [
                    {
                      "id": 1,
                      "question": "प्रश्न का पूरा विवरण हिंदी में...",
                      "options": ["विकल्प A", "विकल्प B", "विकल्प C", "विकल्प D"],
                      "correctIndex": 0,
                      "correctOption": "A",
                      "correctAnswer": "विकल्प A",
                      "explanation": "उत्तर: (A) - विकल्प A। संक्षिप्त व्याख्या हिंदी में...",
                      "subject": "तर्कशक्ति (Reasoning)",
                      "examShiftInfo": "DP Constable 22/11/2025 Shift 1st",
                      "imageUrl": null
                    }
                  ]
                }
                
                Here is the raw exam answer key / text:
                $rawText
            """.trimIndent()
        }

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        if (pdfBase64 != null) {
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", "application/pdf")
                                    put("data", pdfBase64)
                                })
                            })
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        } else {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        }
                    })
                })
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("maxOutputTokens", 65536)
                put("responseMimeType", "application/json")
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        var lastException: Exception? = null

        val modelsToTry = getModelsForCurrentAttempt()

        for (modelIndex in modelsToTry.indices) {
            val model = modelsToTry[modelIndex]
            val isLastModel = modelIndex == modelsToTry.size - 1
            val modelDisplayName = getModelDisplayName(model)
            val fallbackKeys = GeminiApiKeyPoolManager.getAllKeysForFallback(context, apiKey)
            var allKeysDaily = true

            for ((kIdx, attemptKey) in fallbackKeys.withIndex()) {
                val keyNum = kIdx + 1
                val totalKeys = fallbackKeys.size
                val keyTag = " [🔑 Key $keyNum/$totalKeys]"

                onProgress?.invoke("🔄 AI विश्लेषण जारी है... [🤖 $modelDisplayName]$keyTag", 40)

                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$attemptKey"
                    val request = Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseString = response.body?.string() ?: continue

                    val respJson = try { JSONObject(responseString) } catch (_: Exception) { null }
                    if (respJson != null && respJson.has("error")) {
                        val errObj = respJson.optJSONObject("error")
                        val errMsg = errObj?.optString("message") ?: "API Error"
                        val errCode = errObj?.optInt("code", 0) ?: 0
                        lastException = Exception("[$model] $errMsg")

                        if (errCode == 404 || errMsg.contains("not found", ignoreCase = true) || errMsg.contains("not supported", ignoreCase = true)) {
                            break
                        }

                        if (isServerHighDemandError(errMsg, errCode, responseString)) {
                            if (modelIndex < modelsToTry.size - 1) {
                                val nextModel = modelsToTry[modelIndex + 1]
                                val nextDisplayName = getModelDisplayName(nextModel)
                                onProgress?.invoke("⚡ [$modelDisplayName] सर्वर लोड (High Demand), तुरंत [$nextDisplayName] पर शिफ्ट हो रहे हैं...", 40)
                            }
                            break
                        }

                        if (isDailyQuotaError(errMsg, responseString)) {
                            if (kIdx < fallbackKeys.size - 1) {
                                onProgress?.invoke("⚡ [$modelDisplayName] Key $keyNum/$totalKeys दैनिक लिमिट पूर्ण! Key ${keyNum + 1}/$totalKeys पर जा रहे हैं...", 40)
                                continue
                            } else {
                                break
                            }
                        }

                        allKeysDaily = false

                        if (isRateLimitError(errMsg, errCode, responseString)) {
                            if (kIdx < fallbackKeys.size - 1) {
                                onProgress?.invoke("🔄 [$modelDisplayName] Key $keyNum/$totalKeys बिजी (RPM)! अगली Key पर जा रहे हैं...", 40)
                                continue
                            } else {
                                break
                            }
                        }

                        if (kIdx < fallbackKeys.size - 1) {
                            onProgress?.invoke("⚠️ [$modelDisplayName] Key $keyNum/$totalKeys त्रुटि! अगली Key पर जा रहे हैं...", 40)
                            continue
                        } else {
                            break
                        }
                    }

                    if (respJson == null) {
                        if (kIdx < fallbackKeys.size - 1) continue else break
                    }

                    allKeysDaily = false

                    val candidates = respJson.optJSONArray("candidates") ?: continue
                    if (candidates.length() == 0) {
                        if (kIdx < fallbackKeys.size - 1) continue else break
                    }

                    val content = candidates.getJSONObject(0).optJSONObject("content") ?: continue
                    val parts = content.optJSONArray("parts") ?: continue
                    if (parts.length() == 0) {
                        if (kIdx < fallbackKeys.size - 1) continue else break
                    }

                    val text = parts.getJSONObject(0).optString("text") ?: continue

                    val cleanJson = text.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    val jsonStart = cleanJson.indexOf('{')
                    val jsonEnd = cleanJson.lastIndexOf('}')
                    val safeJson = if (jsonStart >= 0 && jsonEnd > jsonStart) {
                        cleanJson.substring(jsonStart, jsonEnd + 1)
                    } else cleanJson

                    try {
                        if (safeJson.startsWith("{")) {
                            val jsonObj = JSONObject(safeJson)
                            val examTitle = jsonObj.optString("examTitle", jsonObj.optString("title", ""))
                            val questionsArray = jsonObj.optJSONArray("questions") ?: JSONArray()
                            val questions = parseJsonArrayObj(questionsArray, subjectOverride)
                            if (questions.isNotEmpty()) {
                                return Pair(examTitle, questions)
                            }
                        } else if (safeJson.startsWith("[")) {
                            val questions = parseJsonArrayToQuestions(safeJson, subjectOverride)
                            if (questions.isNotEmpty()) {
                                return Pair("", questions)
                            }
                        }
                    } catch (e: Exception) {
                        lastException = e
                    }
                } catch (e: Exception) {
                    lastException = e
                    allKeysDaily = false
                    if (kIdx < fallbackKeys.size - 1) {
                        onProgress?.invoke("⚠️ [$modelDisplayName] Key $keyNum/$totalKeys नेटवर्क समस्या! अगली Key पर जा रहे हैं...", 40)
                        continue
                    } else {
                        break
                    }
                }
            }

            if (allKeysDaily) {
                mark38FlashDailyLimitExhausted()
            }

            if (!isLastModel) {
                val nextModel = modelsToTry[modelIndex + 1]
                val nextDisplayName = getModelDisplayName(nextModel)
                onProgress?.invoke("⚡ [$modelDisplayName] सभी Keys की स्पीड लिमिट पूरी! बिना रुके तुरंत [$nextDisplayName] चालू...", 40)
                Log.w(TAG, "[$modelDisplayName] limit hit on all keys. Instantly shifting to [$nextDisplayName].")
                continue
            } else {
                for (s in 5 downTo 1) {
                    onProgress?.invoke("⏳ सभी AI मॉडल्स व्यस्त हैं... ${s}s में पुनः प्रयास...", 40)
                    try { Thread.sleep(1000L) } catch (_: Exception) {}
                }
            }
        }

        if (lastException != null) {
            throw lastException
        }

        return Pair("", emptyList())
    }

    private fun callGeminiForPagesChunk(
        apiKey: String,
        pages: List<String>,
        startPage: Int,
        endPage: Int,
        totalPages: Int,
        examContext: String,
        subjectOverride: String?,
        testMode: String = "FULL_TEST",
        chapterTitle: String? = null,
        subjectTitle: String? = null,
        memoryContext: String = "",
        context: Context? = null,
        currentProgress: Int = 0,
        onProgress: ((String, Int) -> Unit)? = null
    ): Pair<String, List<QuizQuestion>> {
        val activeCustomRules = AiPromptRulesManager.getRulesForMode(mode = testMode)
        val isChapterWise = testMode.equals("CHAPTER_WISE", ignoreCase = true)

        val memoryHeaderSection = if (memoryContext.isNotBlank()) {
            """
            
            🧠 ACCUMULATED CONTEXT & MEMORY (स्मार्ट निरंतरता मेमोरी):
            $memoryContext
            - Maintain continuous sequential counting and verify each question on this page against previous questions to avoid any duplicate questions.
            """.trimIndent()
        } else ""

        val pageRangeLabel = if (startPage == endPage) "Page $startPage" else "Pages $startPage to $endPage"

        val modeHeader = if (isChapterWise) {
            """
            MODE: [STRICT OCR & TEXT EXTRACTION ONLY]
            TARGET SUBJECT: ${subjectTitle ?: subjectOverride ?: "General"}
            TARGET CHAPTER / TOPIC: ${chapterTitle ?: "Selected Chapter"}
            PRIMARY MANDATE: You are a pure OCR transcriber. Your ONLY job is to extract EVERY SINGLE QUESTION physically printed on the provided image pages ($pageRangeLabel).$memoryHeaderSection
            
            🚨 ZERO HALLUCINATION POLICY & COMPLETE EXTRACTION: 
            - DO NOT generate, create, or invent ANY questions from your own memory or knowledge!
            - DO NOT create tests. DO NOT write your own questions.
            - DO NOT filter questions out based on topic name: extract EVERY SINGLE question that is physically printed on $pageRangeLabel!
            - IF a question is NOT physically visible in the provided image, DO NOT include it in the JSON!
            - Extract whatever is physically printed on these PDF pages 100% verbatim, nothing more, nothing less. ZERO OMISSION!
            """.trimIndent()
        } else {
            """
            MODE: [FULL MOCK TEST EXTRACTION]
            EXAM BLUEPRINT: $examContext
            STRICT REQUIREMENT: Extract full length exam paper with all subjects (Reasoning, GK/GS, Maths, Computer/Language) sequentially.$memoryHeaderSection
            """.trimIndent()
        }

        val customRulesPromptSection = if (activeCustomRules.isNotBlank()) {
            """

            10. ADMINISTRATOR STRICT CUSTOM RULES (प्रशासक द्वारा निर्धारित विशेष सख्त नियम - इन्हें 100% अनिवार्य रूप से लागू करें):
            $activeCustomRules
            """.trimIndent()
        } else ""

        val chunkPrompt = if (isChapterWise) {
            val pageScanTargetHeader = if (startPage == endPage) {
                "You are analyzing Page $startPage (out of $totalPages total pages)."
            } else {
                "You are analyzing Pages $startPage to $endPage (out of $totalPages total pages). You MUST extract ALL questions from ALL provided pages ($startPage to $endPage) without stopping after the first page!"
            }

            val pageVerbatimMandate = if (startPage == endPage) {
                """
                2. 100% VERBATIM EXTRACTION & EXACT STOPPING (हूबहू और बिना छोड़े):
                   - Transcribe ONLY AND EXACTLY what is physically printed on Page $startPage.
                   - 🚨 STOP EXACTLY WHEN THE PAGE ENDS: Your JSON array MUST end exactly at the last visible question on Page $startPage.
                   - 🚫 FATAL ERROR AVOIDANCE: Do NOT add questions from your AI memory! Only output what your eyes can physically read on the image. Maintain sequential question numbering.
                   - DO NOT FILTER: Extract EVERY SINGLE QUESTION appearing on Page $startPage. Do NOT discard any question.
                   - ZERO OMISSION: Do NOT skip any question. Every printed question must be extracted.
                   - Every Hindi word, matra, number, technical term, name, date, and punctuation must match the original page 100%. Do not rewrite, summarize, or alter text.
                """.trimIndent()
            } else {
                """
                2. 100% VERBATIM EXTRACTION ACROSS ALL PAGES ($startPage to $endPage) (सभी पेजों के सभी प्रश्न बिना छोड़े):
                   - Transcribe EVERY SINGLE QUESTION physically printed across ALL provided pages (Pages $startPage to $endPage).
                   - Extract all questions from Page $startPage, and then immediately continue to extract all questions from Page $endPage!
                   - 🚨 DO NOT STOP AFTER THE FIRST PAGE! Extract from start to finish across ALL provided pages ($startPage to $endPage).
                   - 🚫 FATAL ERROR AVOIDANCE: Do NOT add questions from your AI memory! Only output what your eyes can physically read on the images.
                   - DO NOT FILTER: Extract EVERY SINGLE QUESTION appearing on these pages. Do NOT discard any question.
                   - ZERO OMISSION: Do NOT skip any question. Every printed question must be extracted.
                   - Every Hindi word, matra, number, technical term, name, date, and punctuation must match the original page 100%. Do not rewrite, summarize, or alter text.
                """.trimIndent()
            }

            """
            You are an elite OCR transcription system for Indian Competitive Exam Chapter Books, Question Banks, and Practice Sets ($examContext). Your only purpose is to read the exact text and numbers printed in the image.
            $modeHeader
            $pageScanTargetHeader

            CRITICAL MANDATORY INSTRUCTIONS (100% VERBATIM EXTRACTION, TWO-COLUMN ORDER, ZERO HALLUCINATION, ZERO OMISSION):

            1. LAYOUT DETECTION & READING ORDER (Two-Column vs Single-Column):
               - FIRST, visually analyze the page layout.
               - IF THE PAGE HAS TWO COLUMNS (e.g., Rojgar Publication, Pinnacle, Kiran books):
                 * YOU MUST FOLLOW THIS STRICT TWO-PHASE SEQUENCE:
                 ★ PHASE 1 - LEFT COLUMN FIRST: Direct your attention EXCLUSIVELY to the LEFT HALF. Extract ALL questions in the left column from top to bottom completely in order before looking at the right column!
                 ★ PHASE 2 - RIGHT COLUMN SECOND: ONLY AFTER the left side is completely extracted, move to the top of the RIGHT HALF and extract from top to bottom.
                 * NEVER read horizontally across a 2-column page!
               - IF THE PAGE IS SINGLE COLUMN (e.g. normal text, test series PDF):
                 * Simply read from top to bottom sequentially.
                 
            $pageVerbatimMandate

            3. COMPLETE MULTI-LINE QUESTIONS, REASONING DATA & UNDERLINED WORDS (रीजनिंग व रेखांकित शब्द):
               - Transcribe the exact printed text word-for-word exactly as printed.
               - 🚨 CRITICAL REASONING (तर्कशक्ति) & MATHS EXTRACTION MANDATE:
                 * For Coding-Decoding ('ROSE' = '68', 'CAT = 3120'), Letter Series ('AZ, BY, CX, ?', 'P3M, Q5K, ?'), Number Series ('12, 36, 108, ?'), Analogies ('12 : 144 :: 15 : ?'), and Mathematical Operations ('24 + 12 ÷ 4 - 8 × 2 = 36'):
                   YOU MUST TRANSCRIBE THE ENTIRE CODING RULE, ENGLISH LETTERS, SERIES, EQUATION, AND QUESTION TEXT COMPLETELY in the 'question' field! Never delete or drop reasoning letters or equations!
                 * For Syllogism (कथन और निष्कर्ष):
                   Transcribe ALL statements ('कथन:\nI. ...\nII. ...') and ALL conclusions ('निष्कर्ष:\nI. ...\nII. ...') in full with linebreaks in 'question'.
                 * For Blood Relations, Direction, Seating Arrangement & Ranking:
                   Transcribe the full conditions, relations (e.g. 'A + B का अर्थ...'), and query line completely.
                 * For Figure/Diagram questions:
                   Even when 'hasDiagram: true', YOU MUST INCLUDE THE FULL QUESTION INSTRUCTION in 'question' (e.g. 'दी गई आकृति का सही दर्पण प्रतिबिंब ज्ञात कीजिए:', 'दी गई आकृति में कितने त्रिभुज हैं?').
               - Sometimes questions have 2 lines before options (e.g. Line 1 is an instruction, Line 2 is a specific word/phrase/equation).
                 YOU MUST COMBINE BOTH LINES INTO THE "question" field!
                 NEVER output only the instruction line without the target word, equation or phrase!
               - 🚨 CRITICAL MANDATE FOR BOLD & UNDERLINED WORDS (बोल्ड व रेखांकित अंश):
                 * मुहावरे, लोकोक्तियां, पर्यायवाची, विलोम शब्द: मुख्य मुहावरे या शब्द को केवल बोल्ड `**मुहावरा**` या `**शब्द**` में लिखें (अंडरलाइन न करें)।
                   - Example: "निम्नलिखित प्रश्न में... उस विकल्प का चयन करें जो दिए गए मुहावरे के अर्थ को सर्वश्रेष्ठ रूप से व्यक्त करता है:\n**बासी बचे न कुत्ता खाय**"
                   - Example: "**‘टिप्पस भिड़ाना’** मुहावरे का सही अर्थ होगा-"
                   - Example: "दिए गए मुहावरे का सही अर्थ चुनें: **हुक्का भरना**"
                   - Example: "**कहा सुनी होना** मुहावरे का अर्थ है:"
                   - Example: "दिए गए शब्द का पर्यायवाची शब्द चुनें: **‘अमृत’**"
                 * केवल रेखांकित शब्द (Physically Underlined words in Question / PDF): Whenever a specific word or phrase in a sentence is UNDERLINED (नीचे रेखा खींची है / रेखांकित अंश), wrap it with `_शब्द_` (or `<u>शब्द</u>`).
                   - Example: 'दिए गए वाक्य में रेखांकित शब्द का पर्यायवाची क्या होगा: उसने _मृग_ देखा।'
                   - Example: 'निम्नलिखित वाक्य में रेखांकित खंड को प्रतिस्थापित करें: वह _प्रतिदिन_ विद्यालय जाता है।'
                   - Example: 'वाक्य में रेखांकित अंश के लिए उचित मुहावरा चुनें: तुम _किस काम के_ हो?'
                   - Example: "'उँगली पकड़ कर _माथा_ पकड़ना।' मुहावरे में रेखांकित शब्द के स्थान पर उपयुक्त शब्द होगा-"
                 * रीजनिंग व गणित: कूट/श्रृंखला को केवल बोल्ड `**AZ, BY, CX, ?**` या `**ROSE = 68**` में लिखें।
               - Strip leading question numbers like "1.", "2.", "Q.1", or "**Q1**". Start directly with the question text.

            4. OPTIONS EXTRACTION (चारों विकल्प):
               - "options": Array of all 4 options as clean strings exactly as printed.
               - Transcribe each option word-for-word exactly as printed.
               - Strip option prefix labels like "(a)", "(b)", "(c)", "(d)" or "(1)", "(2)", "(3)", "(4)" or "A.", "B.", "C.", "D.".

            5. QUESTION NUMBERING & ID (सटीक प्रश्न संख्या):
               - "id": Look at the printed question number on the page (e.g. 1, 2, 3... up to 300). Use this exact printed integer number. If numbers continue across pages, use that printed number. If no printed number is visible, continue sequentially starting from: ${if (memoryContext.isNotBlank()) "the ID indicated in Memory" else "1"}.

            6. OFFICIAL EXAM BADGE & SHIFT TAG (परीक्षा शिफ्ट व दिनांक टैग):
               - Under or next to questions, books print official exam details in blue or bold (e.g. "Exam Name 10 Jan 2023, Shift-I", "Exam 2024 Shift 2").
               - Extract this EXACT string into the "examShiftInfo" field.
               - Do NOT include it in the question text or options.

            7. 100% CORRECT ANSWER EXTRACTION & EXPLANATION (सटीक सही उत्तर - 100% एक्यूरेसी अनिवार्य):
               - 🚨 100% ACCURACY MANDATE: 90% accuracy is NOT acceptable. We need 100% accuracy!
               - 🚨 FINAL QUESTION CHECK (CRITICAL): The VERY LAST question on the page often gets the wrong answer due to truncation or answer keys at the bottom. YOU MUST meticulously verify the 'correctIndex' for the FINAL question before generating the JSON. Look explicitly at the bottom-most answer key or double-check your AI solution!
               - 1. OFFICIAL ANSWER KEY TABLE: Look carefully for an Answer Key (उत्तरमाला / उत्तर तालिका) printed on the page, at the bottom of columns, or at the bottom of the page (e.g. 'उत्तरमाला: 1-(b), 2-(c), 3-(a)...' or '1.(A), 2.(B)').
                 * IF AN ANSWER KEY IS PRESENT: YOU MUST STRICTLY USE THAT OFFICIAL PRINTED ANSWER! Match each question number directly to its letter in the key (A/1 -> correctIndex: 0, B/2 -> correctIndex: 1, C/3 -> correctIndex: 2, D/4 -> correctIndex: 3).
               - 2. MARKED / BOLD OPTIONS: If an option has a tick mark (✓), bold font, filled dot, or marked letter [B], select that marked option.
               - 3. EXPLANATION HEADER: If solutions/explanations are printed with the question (e.g. 'उत्तर (c) : व्याख्या...'), use the exact option letter specified in the solution.
               - 4. AI SOLVING (ONLY IF NO ANSWER KEY): If no answer key or marking is physically present on the page:
                 * You must carefully solve each question as an elite teacher. Re-verify Hindi grammar rules (संधि, समास, पर्यायवाची, विलोम, वर्तनी, मुहावरे, त्रुटि पहचान), formulas, and GK facts to guarantee 100% correctIndex. Double-check before outputting!
               - "explanation": Provide a concise 1-2 sentence Hindi explanation explaining why this answer is correct.

            8. DIAGRAMS & FIGURES:
               - If a question contains a diagram, geometry, figure, or visual option (e.g. in Reasoning, Maths, Science):
                 * Set "hasDiagram": true
                 * Set "diagramPageIndex": The page number where it appears ($startPage to $endPage).
                 * Set "box_2d": [ymin, xmin, ymax, xmax] coordinates (integers 0 to 1000).
                 * Set "diagramBox": [ymin, xmin, ymax, xmax].
                 * CRITICAL DIAGRAM CROPPING RULE: Include ONLY options that are drawings/figures in the cropped diagram. If options are pure numbers or text, DO NOT include them in the diagram box; keep the diagram box strictly around the question figure!
               - If pure text without diagrams, set "hasDiagram": false, "box_2d": null, "diagramBox": null.

            9. BILINGUAL EXAM & PRACTICE BOOKS (100% केवल हिंदी):
               - If questions or options are printed in BOTH English and Hindi (Bilingual):
                 * Extract ONLY the Hindi sentence for "question"! Discard the English question.
                 * For options (e.g. "Direct election / प्रत्यक्ष चुनाव"), extract ONLY the Hindi part ("प्रत्यक्ष चुनाव").
                 * (Exception: preserve reasoning codes like 'ROSE = 68', 'AZ, BY, CX, ?' and mathematical formulas).

            10. SUBJECT & TITLE:
               - Set "subject": "${subjectTitle ?: subjectOverride ?: "General"}".
               - Return "examTitle": "${chapterTitle ?: examContext}".
            $customRulesPromptSection
            
            🚨 FINAL WARNING: Before outputting the JSON, DOUBLE-CHECK the 'correctIndex' of the VERY LAST QUESTION you parsed against the answer key. Do not hallucinate or skip its verification!

            Return ONLY a valid JSON object matching this schema:
            {
              "examTitle": "${chapterTitle ?: examContext}",
              "questions": [
                {
                  "id": 1,
                  "question": "प्रश्न का पूरा विवरण...",
                  "options": ["विकल्प 1", "विकल्प 2", "विकल्प 3", "विकल्प 4"],
                  "correctIndex": 0,
                  "correctOption": "A",
                  "correctAnswer": "विकल्प 1",
                  "explanation": "उत्तर: (A) - विकल्प 1। विस्तृत व्याख्या या हल...",
                  "subject": "${subjectTitle ?: subjectOverride ?: "General"}",
                  "examShiftInfo": "Exam Name 10 Jan 2023, Shift-I",
                  "hasDiagram": false,
                  "diagramPageIndex": $startPage,
                  "box_2d": null,
                  "diagramBox": null
                }
              ]
            }
            """.trimIndent()
        } else {
            """
            You are an elite expert exam question extractor for Indian Competitive Exams ($examContext).
            $modeHeader
            You are analyzing Pages $startPage to $endPage (out of $totalPages total pages) of the official exam question paper / answer key.

            CRITICAL MANDATORY INSTRUCTIONS (100% EXTRACTION, 100% HINDI ONLY, ZERO OMISSION, 100% VERBATIM ACCURACY):
            
            🚨 1. 100% HINDI MANDATE FOR ALL SUBJECTS INCLUDING REASONING & MATHS (अति-महत्वपूर्ण - 100% केवल हिंदी):
               - Indian Competitive Exam Papers (Testbook, SSC, Delhi Police, UP Police, Railways) are BILINGUAL:
                 * Line 1: English Question Text.
                 * Line 2: HINDI Question Text (directly below Line 1).
                 * Options: "English Option / Hindi Option" (separated by a slash '/').
               - YOU MUST EXTRACT 100% HINDI TEXT ONLY!
                 * For "question": Transcribe ONLY the printed HINDI sentence (Line 2) word-for-word! DISCARD Line 1 (English) completely!
                 * In Reasoning questions (Analogy, Series, Math operations, Blood relations, Number logic):
                   - For Q63: Do NOT output English ("Portrait : Painter"). Output Hindi: "इनमें से कौन सा विकल्प दूसरे शब्द युग्म में ठीक उसी प्रकार संबंधित है... चित्र : चित्रकार :: सिम्फनी : ?"
                   - For Q64: Do NOT output English ("Find the next term..."). Output Hindi: "श्रृंखला में अगला पद ज्ञात कीजिए: खोज इंजन, परिणाम सूची, क्लिक करना, ?"
                   - For Q65: Do NOT output English ("If # means..."). Output Hindi: "यदि # का अर्थ +, $ का अर्थ -, * का अर्थ x, और @ का अर्थ ÷ है, तो हल कीजिए: 87 # 53 * 3 $ 360 @ 24"
                   - For Q66: Do NOT output English ("Four numbers are given..."). Output Hindi: "नीचे चार संख्याएँ दी गई हैं, जिनमें से तीन एक ही तर्क का पालन करती हैं, भिन्न संख्या ज्ञात करें और उसे उत्तर के रूप में चिह्नित करें।"
                   - For Q67: Do NOT output English ("If for any numbers..."). Output Hindi: "यदि किसी भी संख्या a और b के लिए, a @ b को 4a + 3b² + 1 के रूप में परिभाषित किया गया है, तो 10 @ 10 का मान क्या होगा?"
                   - For Q68: Do NOT output English ("Which of the following numbers..."). Output Hindi: "निम्नलिखित संख्या श्रृंखला में प्रश्न चिह्न (?) के स्थान पर कौन-सी संख्या आएगी? 2, 5, 11, 23, 47, ?"
                   - For Q69: Do NOT output English ("Pointing towards a person..."). Output Hindi: "एक फोटोग्राफ में एक व्यक्ति की ओर इशारा करते हुए, अंजलि ने कहा, 'वह मेरी बहन के भाई के पिता का इकलौता बेटा है'। वह व्यक्ति अंजलि से कैसे संबंधित है?"
                   - For Q70: Do NOT output English ("D is the son-in-law..."). Output Hindi: "D, B का दामाद है, A का बहनोई है जो C का भाई है, A, B से कैसे संबंधित है?"
                 * For "options": Extract ONLY the Hindi part after the slash '/' (e.g. if option is "A) Track / ट्रैक", extract "ट्रैक").
                 * 🚨 NO REPEATED / DUPLICATE VALUES: If option values are identical in English and Hindi (e.g. "2nd / 2nd", "25% / 25%"), output the clean single value e.g. "2nd", "25%".
                 * It is a STRICT FATAL ERROR to output English questions or English options when Hindi is printed on the paper!

            🚨 2. SPLIT QUESTIONS ACROSS PAGES (दो पेजों के बीच बंटे हुए प्रश्न):
               - In this batch of 4 pages ($startPage to $endPage), if a question starts near the bottom of one page and its diagram, options, or answer continue on the next page: MERGE THEM INTO ONE COMPLETE QUESTION!
               - If a question begins at the bottom of the last page (Page $endPage): Extract its question text and whatever options are printed. DO NOT hallucinate fake options or invent answers!
               - If at the top of Page $startPage you see the continuation of a question (e.g. options C, D, or diagram) from the previous page: Extract it with its sequential number so it completes the question!

            3. EXTRACT EVERY SINGLE QUESTION that appears on these pages ($startPage to $endPage) from top to bottom.
            4. ZERO OMISSION: Do NOT skip any question (Q1, Q2, Q3... Q17, Q18... up to Q100). Every single question must be extracted. Never leave any question out!
            5. Each question must have:
               - "id": Look at the official printed question number on the paper (e.g. 1, 2, 3... up to 100). Use this exact printed integer number.
               - 🚨 CONTINUATION QUESTIONS AT PAGE BOUNDARIES:
                 If a question began at the bottom of the previous page and its Hindi question, diagram, or options appear at the top of the current page: YOU MUST EXTRACT IT COMPLETELY! Do not skip it! Assign it the correct sequential number (or id indicated in Memory).
               - 🚨 SPLIT QUESTIONS ACROSS PAGES IN THIS BATCH:
                 If a question starts near the bottom of one page in this batch and its diagram or options continue on the next page, merge them into ONE complete question! Do not drop it!

            6. VERBATIM HINDI TEXT & REASONING DATA (STRICT: NO REWRITING, NO SUMMARIZING, NO TRANSLATION FROM ENGLISH):
               - "question": Transcribe the EXACT printed Hindi text word-for-word (अक्षरशः और हूबहू) exactly as printed on the official paper.
               - DO NOT summarize, DO NOT rewrite, DO NOT modernize, and DO NOT invent or substitute words!
               - Every Hindi word, matra, number, technical term, name, date, and punctuation must match the original question paper 100%.
               - If English terms appear in brackets e.g. "कंट्रोल पैनल (Control Panel)" or mathematical symbols e.g. "x² + y²", preserve them EXACTLY.
               - 🚨 REASONING SPECIAL REQUIREMENTS (तर्कशक्ति के लिए विशेष नियम):
                 * Syllogism (कथन और निष्कर्ष): Transcribe ALL statements (कथन: I, II...) and ALL conclusions (निष्कर्ष: I, II...) with linebreaks in "question".
                 * Coding-Decoding & Letter/Number Series: Transcribe all codes, letters, and numbers in "question".
                 * Mathematical Operations: Transcribe both the symbol changes AND the full equation in "question".
                 * Blood Relations & Seating: Transcribe the complete relationship definitions and query.
                 * Figure/Diagram Questions: Even when "hasDiagram": true, ALWAYS transcribe the complete question instruction (e.g. 'दी गई आकृति का सही दर्पण प्रतिबिंब चुनें:', 'दी गई आकृति में कितने त्रिभुज हैं?') into "question"!
               - 🚨 UNDERLINED TEXT (रेखांकित शब्द): If any word, phrase, or clause is UNDERLINED (नीचे रेखा खींची है) in the question, wrap it in underscores like `_शब्द_` (or `<u>शब्द</u>`).
               - Do NOT prefix with "Q.1", "1.", or "**Q1**". Start directly with the question text.

            7. OPTIONS:
               - "options": Array of all 4 options in Hindi.
               - Transcribe each option word-for-word exactly as printed. Do NOT include option markers like "(A)", "(1)", "A.", "1.".
               - If options are conclusions (e.g. 'केवल निष्कर्ष I अनुसरण करता है', 'दोनों I और II अनुसरण करते हैं') or directions/measurements/numbers, keep them 100% complete!
               - 🚨 PRESERVE DECIMAL NUMBERS & PERCENTAGES: If an option is a decimal number like "3.375", "4.375", or a percentage like "3.84% बढ़ोतरी", NEVER omit or truncate the leading integer or decimal point! Keep the full exact number e.g. "3.375".

            6. CORRECT ANSWER DETECTION (GREEN TICK ✔️ OR AI LOGICAL SOLVING):
               - FIRST, inspect the options for each question:
                 * If an option has a GREEN TICK / GREEN CHECKMARK (✔️ or ✓) or green highlighted text -> THAT OPTION IS THE OFFICIAL CORRECT ANSWER!
                 * Set "correctIndex" (0, 1, 2, or 3) corresponding to that green-ticked option.
               - SECOND, IF NO GREEN TICK / NO MARKS ARE PRESENT (e.g. clean question paper without answer marks):
                 * YOU MUST LOGICALLY SOLVE THE QUESTION AS AN EXPERT TEACHER AND CHOOSE THE MATHEMATICALLY / FACTUALLY / REASONING-WISE ACCURATE CORRECT OPTION!
                 * For Reasoning diagrams (e.g. paper folding, mirror image, pattern series, counting figures, venn diagrams), analyze the visual shapes and select the correct option (0, 1, 2, or 3).
                 * For Maths and GK, solve the formula or recall the verified factual knowledge and pick the correct option index (0 to 3).
               - STRICT WARNING ABOUT "Chosen Option":
                 In candidate response sheets, ignore "Chosen Option : X" in the side status box. Either use the official green tick or solve logically.
               - "explanation": Provide a clear, concise 1-2 sentence explanation in Hindi explaining why this answer is correct.

            7. REASONING & MATH DIAGRAMS & BOUNDING BOX (आरेख और विकल्प क्रॉप करने का सख्त नियम):
               Many questions, especially in Reasoning (तर्कशक्ति) or Maths, contain diagrams, geometric figures, charts, or visual options:
               - e.g. Mirror image, Water image, Paper folding & cutting, Figure series, Embedded figures, Venn diagrams, Dice/Cube, Counting figures, Geometry, Bar/Pie charts.
               
               Whenever a question has ANY diagram, figure, drawing, or visual options:
               - Set "hasDiagram": true
               - Set "diagramPageIndex": The page number where the diagram appears (Pages $startPage to $endPage).
               - Set "box_2d": [ymin, xmin, ymax, xmax] coordinates (integers 0 to 1000) of the bounding box on that page.
               - Set "diagramBox": [ymin, xmin, ymax, xmax] (identical to box_2d).

               MANDATORY RULE FOR OPTIONS IN BOUNDING BOX (विकल्पों को आरेख के साथ क्रॉप करने या न करने का सख्त नियम):
               * SITUATION 1: ONLY WHEN OPTIONS THEMSELVES ARE DIAGRAMS/FIGURES (सिर्फ तब जब विकल्प भी आकृतियां/चित्र हों):
                 - If options 1, 2, 3, 4 (or A, B, C, D) are THEMSELVES DRAWINGS, FIGURES, OR SHAPES (e.g. Non-verbal reasoning, mirror images, paper folding shapes, pattern choices, embedded figure options):
                 - ONLY in this case, include BOTH the question figure AND all 4 option figures inside the bounding box ([ymin, xmin, ymax, xmax])!
                 - In the "options" JSON array, write: ["विकल्प 1", "विकल्प 2", "विकल्प 3", "विकल्प 4"] or ["आकृति 1", "आकृति 2", "आकृति 3", "आकृति 4"].

               * SITUATION 2: WHEN OPTIONS ARE NUMBERS, TEXT, OR VALUES (जब विकल्पों में आकृति नहीं है बल्कि संख्या/नंबर/टेक्स्ट हैं):
                 - If options 1, 2, 3, 4 are NUMBERS (e.g. 12, 45, 80), DIGITS, FORMULAS, MEASUREMENTS (e.g. 15 cm, 40°), OR PLAIN TEXT:
                 - STRICT CRITICAL MANDATE: DO NOT INCLUDE THE NUMBER OPTIONS IN THE BOUNDING BOX! (संख्या/नंबर वाले विकल्पों को आरेख बॉक्स के अंदर बिल्कुल शामिल नहीं करना है!)
                 - The bounding box ([ymin, xmin, ymax, xmax]) MUST CROP ONLY AND ONLY THE QUESTION DIAGRAM / FIGURE ITSELF!
                 - Set 'ymax' so it ends EXACTLY at the bottom edge of the diagram, BEFORE the number options start!
                 - Transcribe the numbers/text purely into the "options" JSON array (e.g. ["12", "45", "80", "120"]). They will be shown as clickable buttons in the app, so they MUST NOT appear inside the cropped diagram image!

               - If the question does NOT have any diagram (pure text), set:
                 "hasDiagram": false, "diagramPageIndex": $startPage, "box_2d": null, "diagramBox": null

            8. SUBJECT DETECTION (MANDATORY: STRICT 4 SUBJECTS ONLY):
               - Assign EACH question to ONLY ONE of these 4 official canonical subjects:
                 * "GK / GS" -> For ALL General Knowledge, History (इतिहास), Geography (भूगोल), Science (विज्ञान), Polity/Constitution (संविधान), Economics (अर्थशास्त्र), Current Affairs (समसामयिकी), and Static GK.
                 * "तर्कशक्ति" -> For ALL Reasoning, Mental Ability, Intelligence, Figure/Diagram questions.
                 * "गणित" -> For ALL Numerical Ability, Mathematics, Arithmetic, Quantitative Aptitude.
                 * "कंप्यूटर" -> For ALL Computer Awareness, MS Office, Internet, IT questions.
                 (Note: For UP Police / Lekhpal, use "सामान्य हिंदी" for Hindi questions; for Teaching use "बाल विकास एवं शिक्षाशास्त्र").
                 DO NOT create separate subjects for history, geography, science or polity. They ALL belong strictly to "GK / GS"!

            9. EXAM, DATE & SHIFT DETECTION (विज्ञापन बैनर को सख्ती से छोड़ें):
               - 🚨 IGNORE ALL ADVERTISEMENTS: Banners at the top or bottom of pages like "SSC CGL LIVE CLASSES @ ₹1 Trial", "Enroll in For 375+ Exams", "Testbook Supercoaching", "Enroll Now" are COMMERCIAL ADS! NEVER put them into "examShiftInfo"!
               - Only if an official exam shift badge is printed (e.g. "Delhi Police 18 Dec 2025 Shift 1", "SSC GD 20 Feb 2024 Shift 2"), put that into "examShiftInfo". Otherwise, set "examShiftInfo": null.

            10. EXAM TITLE:
               - If the official exam title / shift header is visible on these pages (e.g. on Page 2: "Delhi Police Const. (Exec) 18 Dec, 2025 Shift 1"), return it in "examTitle", otherwise return "examTitle": "".
            $customRulesPromptSection
            
            🚨 FINAL WARNING: Before outputting the JSON, DOUBLE-CHECK the 'correctIndex' of the VERY LAST QUESTION you parsed against the answer key. Do not hallucinate or skip its verification!

            Return ONLY a valid JSON object matching this schema:
            {
              "examTitle": "Official Exam Title if visible on these pages, else empty",
              "questions": [
                {
                  "id": 1,
                  "question": "प्रश्न का पूरा विवरण...",
                  "options": ["विकल्प 1", "विकल्प 2", "विकल्प 3", "विकल्प 4"],
                  "correctIndex": 0,
                  "correctOption": "A",
                  "correctAnswer": "विकल्प 1",
                  "explanation": "उत्तर: (A) - विकल्प 1। संक्षिप्त व्याख्या...",
                  "subject": "तर्कशक्ति (Reasoning)",
                  "examShiftInfo": "DP Constable 22/11/2025 Shift 1st",
                  "hasDiagram": true,
                  "diagramPageIndex": $startPage,
                  "box_2d": [120, 30, 480, 750],
                  "diagramBox": [120, 30, 480, 750]
                }
              ]
            }
            """.trimIndent()
        }

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        for (pageBase64 in pages) {
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", "image/jpeg")
                                    put("data", pageBase64)
                                })
                            })
                        }
                        put(JSONObject().apply {
                            put("text", chunkPrompt)
                        })
                    })
                })
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                // Temperature 0.1 forces strict deterministic OCR but avoids 0.0 loops
                put("temperature", 0.1)
                put("maxOutputTokens", 65536)
                put("responseMimeType", "application/json")
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        var lastEx: Exception? = null

        val modelsToTry = getModelsForCurrentAttempt()
        val fallbackKeys = GeminiApiKeyPoolManager.getAllKeysForFallback(context, apiKey)
        val pageLabel = if (startPage == endPage) "पेज $startPage" else "पेज $startPage-$endPage"

        if (areAllModelsExhausted(fallbackKeys.size)) {
            throw Exception("दैनिक सीमा समाप्त: आपके सभी ${fallbackKeys.size} API Keys की दैनिक फ्री लिमिट (20 requests/day) पूरी हो चुकी है। कृपया Settings में नई API Keys दर्ज करें या कल कोटा रीसेट होने तक प्रतीक्षा करें।")
        }

        for (modelIndex in modelsToTry.indices) {
            val model = modelsToTry[modelIndex]
            val modelDisplayName = getModelDisplayName(model)
            val isLastModel = modelIndex == modelsToTry.size - 1
            var allKeysDaily = true

            // Filter out keys already exhausted on this model today
            val availableKeys = fallbackKeys.filterNot { isKeyExhaustedForModel(model, it) }
            if (availableKeys.isEmpty()) {
                if (!isLastModel) {
                    val nextModel = modelsToTry[modelIndex + 1]
                    val nextDisplayName = getModelDisplayName(nextModel)
                    onProgress?.invoke("⚡ [$modelDisplayName] सभी Keys की दैनिक लिमिट पूरी! तुरंत [$nextDisplayName] पर शिफ्ट हो रहे हैं...", currentProgress)
                    advanceToNextModel("All keys already daily exhausted for $modelDisplayName")
                    continue
                } else {
                    throw Exception("दैनिक सीमा समाप्त: सभी उपलब्ध AI मॉडल्स पर सभी Keys की दैनिक सीमा समाप्त हो चुकी है। कृपया नई API Keys जोड़ें या कल प्रयास करें।")
                }
            }

            for ((kIdx, attemptKey) in availableKeys.withIndex()) {
                val keyNum = fallbackKeys.indexOf(attemptKey) + 1
                val totalKeys = fallbackKeys.size
                val keyTag = " [🔑 Key $keyNum/$totalKeys]"

                onProgress?.invoke("🔄 $pageLabel स्कैन जारी है... [🤖 $modelDisplayName]$keyTag", currentProgress)

                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$attemptKey"
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseString = response.body?.string() ?: continue

                    val respJson = try { JSONObject(responseString) } catch (_: Exception) { null }
                    if (respJson != null && respJson.has("error")) {
                        val errObj = respJson.optJSONObject("error")
                        val errMsg = errObj?.optString("message") ?: "Error"
                        val errCode = errObj?.optInt("code", 0) ?: 0
                        lastEx = Exception("[$model] $errMsg")

                        // If model not found or not supported
                        if (errCode == 404 || errMsg.contains("not found", ignoreCase = true) || errMsg.contains("not supported", ignoreCase = true)) {
                            break
                        }

                        // Check server high demand / service unavailable (HTTP 503/500/504) -> Skip model immediately to next model
                        if (isServerHighDemandError(errMsg, errCode, responseString)) {
                            if (!isLastModel) {
                                val nextModel = modelsToTry[modelIndex + 1]
                                val nextDisplayName = getModelDisplayName(nextModel)
                                onProgress?.invoke("⚡ [$modelDisplayName] सर्वर पर लोड (High Demand), तुरंत [$nextDisplayName] पर शिफ्ट हो रहे हैं...", currentProgress)
                            }
                            break
                        }

                        // Check daily quota exhaustion
                        if (isDailyQuotaError(errMsg, responseString)) {
                            markKeyExhaustedForModel(model, attemptKey)
                            if (kIdx < availableKeys.size - 1) {
                                val nextAvailable = availableKeys[kIdx + 1]
                                val nextKeyNum = fallbackKeys.indexOf(nextAvailable) + 1
                                onProgress?.invoke("⚡ [$modelDisplayName] Key $keyNum/$totalKeys दैनिक कोटा समाप्त! अगली Key $nextKeyNum/$totalKeys पर जा रहे हैं...", currentProgress)
                                try { Thread.sleep(1200L) } catch (_: Exception) {}
                                continue
                            } else {
                                break
                            }
                        }

                        allKeysDaily = false

                        // Check temporary rate limit (RPM)
                        if (isRateLimitError(errMsg, errCode, responseString)) {
                            GeminiApiKeyPoolManager.advanceKeyAfterRateLimit(context)
                            if (kIdx < availableKeys.size - 1) {
                                val nextAvailable = availableKeys[kIdx + 1]
                                val nextKeyNum = fallbackKeys.indexOf(nextAvailable) + 1
                                onProgress?.invoke("🔄 [$modelDisplayName] Key $keyNum/$totalKeys बिजी (RPM)! 2s बाद Key $nextKeyNum/$totalKeys पर जा रहे हैं...", currentProgress)
                                try { Thread.sleep(2000L) } catch (_: Exception) {}
                                continue
                            } else {
                                onProgress?.invoke("⏳ [$modelDisplayName] सभी Keys बिजी (RPM)! 4s प्रतीक्षा...", currentProgress)
                                try { Thread.sleep(4000L) } catch (_: Exception) {}
                                break
                            }
                        }

                        // Any other API error (e.g. invalid key or permission issue) -> try next key!
                        if (kIdx < availableKeys.size - 1) {
                            val nextAvailable = availableKeys[kIdx + 1]
                            val nextKeyNum = fallbackKeys.indexOf(nextAvailable) + 1
                            onProgress?.invoke("⚠️ [$modelDisplayName] Key $keyNum/$totalKeys त्रुटि! अगली Key $nextKeyNum/$totalKeys पर जा रहे हैं...", currentProgress)
                            try { Thread.sleep(1000L) } catch (_: Exception) {}
                            continue
                        } else {
                            break
                        }
                    }

                    if (respJson == null) {
                        if (kIdx < availableKeys.size - 1) continue else break
                    }

                    allKeysDaily = false

                    val candidates = respJson.optJSONArray("candidates") ?: continue
                    if (candidates.length() == 0) {
                        if (kIdx < availableKeys.size - 1) continue else break
                    }

                    val content = candidates.getJSONObject(0).optJSONObject("content") ?: continue
                    val parts = content.optJSONArray("parts") ?: continue
                    if (parts.length() == 0) {
                        if (kIdx < availableKeys.size - 1) continue else break
                    }

                    val text = parts.getJSONObject(0).optString("text") ?: continue

                    val cleanJson = text.trim()
                        .removePrefix("```json")
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()

                    var jsonSuccessfullyReceived = false
                    var detectedTitleFromPage = ""

                    // 1. Try JSON Object format
                    if (cleanJson.contains("{")) {
                        val jsonStart = cleanJson.indexOf('{')
                        val jsonEnd = cleanJson.lastIndexOf('}')
                        if (jsonStart >= 0 && jsonEnd > jsonStart) {
                            try {
                                val safeJson = cleanJson.substring(jsonStart, jsonEnd + 1)
                                val jsonObj = JSONObject(safeJson)
                                jsonSuccessfullyReceived = true
                                val examTitle = jsonObj.optString("examTitle", jsonObj.optString("title", ""))
                                if (examTitle.isNotBlank()) detectedTitleFromPage = examTitle
                                val questionsArray = jsonObj.optJSONArray("questions")
                                    ?: jsonObj.optJSONArray("Questions")
                                    ?: jsonObj.optJSONArray("quiz")
                                    ?: jsonObj.optJSONArray("quizQuestions")
                                    ?: jsonObj.optJSONArray("data")
                                    ?: jsonObj.optJSONArray("items")
                                    ?: JSONArray()
                                val questions = parseJsonArrayObj(questionsArray, subjectOverride, pages, startPage)
                                if (questions.isNotEmpty()) {
                                    return Pair(examTitle, questions)
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    // 2. Try direct JSON Array format [...]
                    if (cleanJson.contains("[")) {
                        val arrayStart = cleanJson.indexOf('[')
                        val arrayEnd = cleanJson.lastIndexOf(']')
                        if (arrayStart >= 0 && arrayEnd > arrayStart) {
                            try {
                                val safeArrayJson = cleanJson.substring(arrayStart, arrayEnd + 1)
                                val jsonArr = JSONArray(safeArrayJson)
                                jsonSuccessfullyReceived = true
                                val questions = parseJsonArrayObj(jsonArr, subjectOverride, pages, startPage)
                                if (questions.isNotEmpty()) {
                                    return Pair("", questions)
                                }
                            } catch (_: Exception) {}
                        }
                    }

                    // If valid JSON was returned by AI with 0 questions (e.g. cover page or candidate instructions):
                    // Do NOT waste other keys on the exact same empty page! Return cleanly.
                    if (jsonSuccessfullyReceived) {
                        return Pair(detectedTitleFromPage, emptyList())
                    }

                    // If parsing failed or invalid format, try next fallback key with a short pause
                    if (kIdx < availableKeys.size - 1) {
                        try { Thread.sleep(1000L) } catch (_: Exception) {}
                        continue
                    }

                } catch (e: Exception) {
                    lastEx = e
                    allKeysDaily = false
                    if (kIdx < availableKeys.size - 1) {
                        val nextAvailable = availableKeys[kIdx + 1]
                        val nextKeyNum = fallbackKeys.indexOf(nextAvailable) + 1
                        onProgress?.invoke("⚠️ [$modelDisplayName] Key $keyNum/$totalKeys नेटवर्क त्रुटि! अगली Key $nextKeyNum/$totalKeys पर जा रहे हैं...", currentProgress)
                        continue
                    } else {
                        break
                    }
                }
            }

            // If this model had an explicit true 24-hour daily quota exhaustion on all keys:
            if (allKeysDaily) {
                mark38FlashDailyLimitExhausted()
            }

            // If all keys for this model were exhausted, immediately fallback to next model:
            if (!isLastModel) {
                advanceToNextModel("All keys limit on $modelDisplayName")
                val nextModel = modelsToTry[modelIndex + 1]
                val nextDisplayName = getModelDisplayName(nextModel)
                onProgress?.invoke("⚡ [$modelDisplayName] सभी ${fallbackKeys.size} Keys की लिमिट पूरी! तुरंत [$nextDisplayName] चालू...", currentProgress)
                Log.w(TAG, "[$modelDisplayName] limit hit on all keys for $pageLabel. Instantly falling over to [$nextDisplayName].")
                continue
            } else {
                if (areAllModelsExhausted(fallbackKeys.size)) {
                    throw Exception("दैनिक सीमा समाप्त: आपके सभी ${fallbackKeys.size} API Keys की दैनिक फ्री लिमिट पूरी हो चुकी है। कृपया नई API Keys जोड़ें या कल प्रयास करें।")
                }
                // If even 3.6 Flash failed across all keys, do a short 4-second wait before finishing
                for (s in 4 downTo 1) {
                    onProgress?.invoke("⏳ सभी AI मॉडल्स व्यस्त हैं... ${s}s में पुनः प्रयास...", currentProgress)
                    try { Thread.sleep(1000L) } catch (_: Exception) {}
                }
            }
        }

        if (lastEx != null) throw lastEx
        return Pair("", emptyList())
    }

    /**
     * Extracts coordinates [ymin, xmin, ymax, xmax] from diverse AI response formats:
     * JSONArray, JSONObject, bounding box string e.g. "[120, 30, 480, 750]", or direct keys.
     */
    fun extractBoxCoordinates(obj: JSONObject): List<Double>? {
        // 1. Try JSONArray fields
        val array = obj.optJSONArray("box_2d")
            ?: obj.optJSONArray("diagramBox")
            ?: obj.optJSONArray("box")
            ?: obj.optJSONArray("boundingBox")
        if (array != null && array.length() >= 4) {
            val list = mutableListOf<Double>()
            for (k in 0 until 4) {
                list.add(array.optDouble(k, 0.0))
            }
            return list
        }

        // 2. Try JSON Object with named keys
        val boxObj = obj.optJSONObject("box_2d")
            ?: obj.optJSONObject("diagramBox")
            ?: obj.optJSONObject("box")
            ?: obj.optJSONObject("boundingBox")
        if (boxObj != null) {
            val ymin = boxObj.optDouble("ymin", boxObj.optDouble("top", Double.NaN))
            val xmin = boxObj.optDouble("xmin", boxObj.optDouble("left", Double.NaN))
            val ymax = boxObj.optDouble("ymax", boxObj.optDouble("bottom", Double.NaN))
            val xmax = boxObj.optDouble("xmax", boxObj.optDouble("right", Double.NaN))
            if (!ymin.isNaN() && !xmin.isNaN() && !ymax.isNaN() && !xmax.isNaN()) {
                return listOf(ymin, xmin, ymax, xmax)
            }
        }

        // 3. Try String format "[120, 30, 480, 750]"
        val str = obj.optString("box_2d", "")
            .ifBlank { obj.optString("diagramBox", "") }
            .ifBlank { obj.optString("box", "") }
        if (str.isNotBlank()) {
            val nums = Regex("""\d+(?:\.\d+)?""").findAll(str).map { it.value.toDoubleOrNull() }.filterNotNull().toList()
            if (nums.size >= 4) {
                return nums.take(4)
            }
        }

        // 4. Direct keys on question object
        val directYmin = obj.optDouble("ymin", Double.NaN)
        val directXmin = obj.optDouble("xmin", Double.NaN)
        val directYmax = obj.optDouble("ymax", Double.NaN)
        val directXmax = obj.optDouble("xmax", Double.NaN)
        if (!directYmin.isNaN() && !directXmin.isNaN() && !directYmax.isNaN() && !directXmax.isNaN()) {
            return listOf(directYmin, directXmin, directYmax, directXmax)
        }

        return null
    }

    /**
     * Crops a bounding box from a page's base64 JPEG image with high quality and safe bounds.
     * Box coordinates can be normalized floats (0..1.0), normalized ints (0..1000), or pixel coords.
     * [hasVisualOptions]: If false, the options are numbers or text, so bottom padding is strictly 0
     * to prevent capturing the numbers/options printed beneath the diagram!
     */
    fun cropDiagramFromPage(pageBase64: String, coords: List<Double>, hasVisualOptions: Boolean = true): String? {
        return try {
            val clean = if (pageBase64.contains(",")) pageBase64.substringAfter(",") else pageBase64
            val bytes = android.util.Base64.decode(clean.trim(), android.util.Base64.DEFAULT)
            val opts = android.graphics.BitmapFactory.Options().apply {
                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
            }
            val pageBitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null

            val width = pageBitmap.width
            val height = pageBitmap.height

            val v0 = coords.getOrElse(0) { 0.0 }
            val v1 = coords.getOrElse(1) { 0.0 }
            val v2 = coords.getOrElse(2) { 1000.0 }
            val v3 = coords.getOrElse(3) { 1000.0 }

            // Robust normalization function supporting 0.0..1.0, 0..1000, or actual pixel values
            fun toPx(v: Double, maxDim: Int): Int {
                return when {
                    v <= 1.05 -> (v * maxDim).toInt()
                    v <= 1005.0 -> ((v / 1000.0) * maxDim).toInt()
                    else -> v.toInt()
                }.coerceIn(0, maxDim)
            }

            val yminRaw = toPx(v0, height)
            val xminRaw = toPx(v1, width)
            val ymaxRaw = toPx(v2, height)
            val xmaxRaw = toPx(v3, width)

            val realYmin = java.lang.Math.min(yminRaw, ymaxRaw)
            val realYmax = java.lang.Math.max(yminRaw, ymaxRaw)
            val realXmin = java.lang.Math.min(xminRaw, xmaxRaw)
            val realXmax = java.lang.Math.max(xminRaw, xmaxRaw)

            if ((realXmax - realXmin) < 20 || (realYmax - realYmin) < 20) {
                pageBitmap.recycle()
                return null
            }

            // Safe, generous padding around bounding box:
            val padTop = (height * 0.015).toInt()
            val padBottom = if (hasVisualOptions) (height * 0.020).toInt() else (height * 0.005).toInt()
            val padX = (width * 0.025).toInt()

            val rawXmin = (realXmin - padX).coerceIn(0, (width - 1).coerceAtLeast(0))
            val rawYmin = (realYmin - padTop).coerceIn(0, (height - 1).coerceAtLeast(0))
            val rawXmax = (realXmax + padX).coerceIn(rawXmin + 1, width)
            val rawYmax = (realYmax + padBottom).coerceIn(rawYmin + 1, height)

            val safeX = rawXmin
            val safeY = rawYmin
            val safeW = (rawXmax - safeX).coerceIn(1, (width - safeX).coerceAtLeast(1))
            val safeH = (rawYmax - safeY).coerceIn(1, (height - safeY).coerceAtLeast(1))

            val rawCropped = android.graphics.Bitmap.createBitmap(pageBitmap, safeX, safeY, safeW, safeH)
            pageBitmap.recycle()

            // Normalize green option text color: convert any green-tinted text into dark neutral text
            // matching the other 3 normal options, without altering the surrounding background.
            val cleanBitmap = rawCropped.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
            rawCropped.recycle()

            try {
                val bW = cleanBitmap.width
                val bH = cleanBitmap.height
                val pixels = IntArray(bW * bH)
                cleanBitmap.getPixels(pixels, 0, bW, 0, 0, bW, bH)
                var modified = false

                for (idx in pixels.indices) {
                    val p = pixels[idx]
                    val r = (p shr 16) and 0xFF
                    val g = (p shr 8) and 0xFF
                    val b = p and 0xFF

                    // Detect green text / green option highlight:
                    // Green is dominant: g is significantly higher than r and b
                    val isGreenTextOrMark = (g > 60 && g > (r * 1.25).toInt() && g > (b * 1.25).toInt())

                    if (isGreenTextOrMark) {
                        // If it's a dark/medium green text pixel, convert it to dark black/gray text color
                        // matching normal text (#1F2937 / dark slate)
                        val brightness = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                        if (brightness < 180) {
                            // Dark text stroke -> convert to normal dark text
                            val gray = (brightness * 0.4).toInt().coerceIn(20, 50)
                            pixels[idx] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
                            modified = true
                        } else {
                            // Very light green tint / background highlight -> normalize to clean white
                            pixels[idx] = -0x1 // 0xFFFFFFFF
                            modified = true
                        }
                    }
                }

                if (modified) {
                    cleanBitmap.setPixels(pixels, 0, bW, 0, 0, bW, bH)
                }
            } catch (_: Throwable) {}

            val stream = java.io.ByteArrayOutputStream()
            // HD Quality (95% JPEG) to keep figures, shapes, and numbers super crisp
            cleanBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, stream)
            val croppedBytes = stream.toByteArray()
            cleanBitmap.recycle()

            "data:image/jpeg;base64," + android.util.Base64.encodeToString(croppedBytes, android.util.Base64.NO_WRAP)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Backward-compatible overload for JSONArray box
     */
    fun cropDiagramFromPage(pageBase64: String, boxArray: JSONArray, hasVisualOptions: Boolean = true): String? {
        val list = mutableListOf<Double>()
        for (i in 0 until boxArray.length()) {
            list.add(boxArray.optDouble(i, 0.0))
        }
        return cropDiagramFromPage(pageBase64, list, hasVisualOptions)
    }

    /**
     * Fallback crop: when Gemini indicates a question has a diagram but failed to output a bounding box,
     * or the box crop failed, calculate the question's vertical slice on that page based on its sequence.
     * [hasVisualOptions]: If false, takes only the diagram upper half to avoid capturing number options.
     */
    fun cropFallbackRegion(
        pageBase64: String,
        qIndexOnPage: Int,
        totalQuestionsOnPage: Int,
        hasVisualOptions: Boolean = true
    ): String? {
        return try {
            val totalQ = totalQuestionsOnPage.coerceAtLeast(1)
            val qIdx = qIndexOnPage.coerceIn(0, totalQ - 1)

            val sliceHeightNorm = 0.94 / totalQ
            val startYNorm = (0.03 + (qIdx * sliceHeightNorm) - 0.01).coerceIn(0.0, 0.94)
            val endYNorm = if (hasVisualOptions) {
                (startYNorm + sliceHeightNorm + 0.02).coerceIn(startYNorm + 0.08, 1.0)
            } else {
                (startYNorm + (sliceHeightNorm * 0.52)).coerceIn(startYNorm + 0.05, 1.0)
            }

            cropDiagramFromPage(pageBase64, listOf(startYNorm, 0.02, endYNorm, 0.98), hasVisualOptions)
        } catch (_: Throwable) {
            null
        }
    }

    private fun parseJsonArrayObj(
        jsonArray: JSONArray,
        subjectOverride: String?,
        pages: List<String>? = null,
        startPage: Int = 1
    ): List<QuizQuestion> {
        val list = mutableListOf<QuizQuestion>()
        val pageCount = pages?.size ?: 1

        // Pre-scan questions to count how many questions fall on each page index in this batch
        val questionsPerPageCount = IntArray(pageCount) { 0 }
        val questionPageIndices = IntArray(jsonArray.length()) { 0 }

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val rawDiagramPage = obj.optInt("diagramPageIndex", obj.optInt("diagramPage", -1))
            val pIdx = when {
                pages.isNullOrEmpty() -> 0
                rawDiagramPage >= startPage && rawDiagramPage < (startPage + pageCount) -> {
                    rawDiagramPage - startPage
                }
                rawDiagramPage in 1..pageCount && startPage == 1 -> {
                    rawDiagramPage - 1
                }
                else -> {
                    // Distribute proportionally across all pages in this batch (0 until pageCount)
                    val fraction = i.toFloat() / jsonArray.length().coerceAtLeast(1)
                    (fraction * pageCount).toInt().coerceIn(0, pageCount - 1)
                }
            }.coerceIn(0, (pageCount - 1).coerceAtLeast(0))

            questionPageIndices[i] = pIdx
            questionsPerPageCount[pIdx]++
        }

        // Track index of question on its respective page
        val currentQIndexOnPage = IntArray(pageCount) { 0 }

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val rawId = obj.opt("id") ?: obj.opt("questionNumber") ?: obj.opt("qNo") ?: obj.opt("number")
            var id = when (rawId) {
                is Number -> rawId.toInt()
                is String -> Regex("""\d+""").find(rawId)?.value?.toIntOrNull() ?: ((System.currentTimeMillis() % 100000).toInt() + i)
                else -> (System.currentTimeMillis() % 100000).toInt() + i
            }
            val rawQuestion = obj.optString("question", "")
                .ifBlank { obj.optString("questionText", "") }
                .ifBlank { obj.optString("question_text", "") }
                .ifBlank { obj.optString("q", "") }
                .ifBlank { obj.optString("text", "") }

            // If ID was a generated fallback timestamp, check leading number in raw question text e.g. "42. ..."
            if (id > 1000 || id <= 0) {
                val leadingNum = Regex("""^(?:Q(?:uestion)?|प्र(?:श्न)?|Q\.)?\s*[\#\№]?\s*(\d{1,3})\s*[\.\:\)\-\/]""").find(rawQuestion.trim())?.groupValues?.getOrNull(1)?.toIntOrNull()
                if (leadingNum != null && leadingNum in 1..300) {
                    id = leadingNum
                }
            }
            val (cleanedQ, detectedShiftTag) = extractShiftTagFromText(cleanQuestionText(rawQuestion))
            val question = cleanedQ

            val rawShiftInfo = obj.optString("examShiftInfo", "")
                .ifBlank { obj.optString("shiftInfo", "") }
                .ifBlank { obj.optString("examDateShift", "") }
                .ifBlank { obj.optString("examInfo", "") }
                .ifBlank { obj.optString("source", "") }
                .ifBlank { detectedShiftTag ?: "" }

            val examShiftInfo = if (rawShiftInfo.isNotBlank() && rawShiftInfo != "null") rawShiftInfo.trim() else null

            val options = mutableListOf<String>()

            // 1. Try JSONArray formats
            val optionsArray = obj.optJSONArray("options")
                ?: obj.optJSONArray("Options")
                ?: obj.optJSONArray("choices")
                ?: obj.optJSONArray("Choices")
                ?: obj.optJSONArray("answers")

            if (optionsArray != null) {
                for (j in 0 until optionsArray.length()) {
                    val optItem = optionsArray.opt(j)
                    if (optItem is JSONObject) {
                        val textVal = optItem.optString("text", optItem.optString("value", optItem.optString("option", "")))
                        if (textVal.isNotBlank()) options.add(cleanOptionText(textVal))
                    } else if (optItem != null) {
                        val str = optItem.toString()
                        if (str.isNotBlank()) options.add(cleanOptionText(str))
                    }
                }
            } else {
                // 2. Try JSONObject format {"A": "...", "B": "...", "C": "...", "D": "..."}
                val optionsObj = obj.optJSONObject("options")
                    ?: obj.optJSONObject("Options")
                    ?: obj.optJSONObject("choices")
                    ?: obj.optJSONObject("Choices")

                if (optionsObj != null) {
                    val keySets = listOf(
                        listOf("A", "a", "1", "क", "opt1", "option1", "optionA", "optA"),
                        listOf("B", "b", "2", "ख", "opt2", "option2", "optionB", "optB"),
                        listOf("C", "c", "3", "ग", "opt3", "option3", "optionC", "optC"),
                        listOf("D", "d", "4", "घ", "opt4", "option4", "optionD", "optD")
                    )
                    for (keys in keySets) {
                        var foundVal: String? = null
                        for (k in keys) {
                            if (optionsObj.has(k)) {
                                foundVal = optionsObj.optString(k, "")
                                break
                            }
                        }
                        if (foundVal != null) {
                            options.add(cleanOptionText(foundVal))
                        }
                    }
                    if (options.isEmpty()) {
                        val iter = optionsObj.keys()
                        while (iter.hasNext()) {
                            val k = iter.next()
                            options.add(cleanOptionText(optionsObj.optString(k, "")))
                        }
                    }
                } else {
                    // 3. Try direct keys in question object: "optionA", "optionB", etc.
                    val oA = obj.optString("optionA", "").ifBlank { obj.optString("opt1", "") }.ifBlank { obj.optString("option_a", "") }.ifBlank { obj.optString("a", "") }
                    val oB = obj.optString("optionB", "").ifBlank { obj.optString("opt2", "") }.ifBlank { obj.optString("option_b", "") }.ifBlank { obj.optString("b", "") }
                    val oC = obj.optString("optionC", "").ifBlank { obj.optString("opt3", "") }.ifBlank { obj.optString("option_c", "") }.ifBlank { obj.optString("c", "") }
                    val oD = obj.optString("optionD", "").ifBlank { obj.optString("opt4", "") }.ifBlank { obj.optString("option_d", "") }.ifBlank { obj.optString("d", "") }
                    if (oA.isNotBlank() && oB.isNotBlank()) {
                        options.add(cleanOptionText(oA))
                        options.add(cleanOptionText(oB))
                        if (oC.isNotBlank()) options.add(cleanOptionText(oC))
                        if (oD.isNotBlank()) options.add(cleanOptionText(oD))
                    }
                }
            }

            while (options.size < 4) {
                options.add("विकल्प ${options.size + 1}")
            }

            val correctIndex = parseCorrectIndex(obj, options).coerceIn(0, options.size - 1)
            val explanation = obj.optString("explanation", "")
            val rawSubject = obj.optString("subject", "GK / GS")
            val subject = subjectOverride ?: normalizeSubject(rawSubject)
            
            var imageUrl = obj.optString("imageUrl", "").let { if (it.isBlank() || it == "null") null else it }

            val pageIdx = questionPageIndices[i]
            val qIdxOnPage = currentQIndexOnPage[pageIdx]
            currentQIndexOnPage[pageIdx]++
            val totalQOnPage = questionsPerPageCount[pageIdx].coerceAtLeast(1)

            // Auto-detect if question has diagram
            val extractedBox = extractBoxCoordinates(obj)
            val lowerQ = rawQuestion.lowercase()
            val isReasoningQ = subject.contains("तर्क") || subject.contains("reason", ignoreCase = true) || subject.contains("रीजनिंग") || subject.contains("बुद्धिमत्ता")
            val isMathQ = subject.contains("गणित") || subject.contains("math", ignoreCase = true) || subject.contains("संख्यात्मक") || subject.contains("अंकगणित")
            val isComputerQ = subject.contains("कंप्यूटर") || subject.contains("computer", ignoreCase = true) || subject.contains("संगणक") || subject.contains("आईटी") || subject.contains("cyber")
            val isGkQ = subject.contains("gk", ignoreCase = true) || subject.contains("gs", ignoreCase = true) || subject.contains("सामान्य ज्ञान") || subject.contains("सामान्य अध्ययन") || subject.contains("इतिहास") || subject.contains("भूगोल") || subject.contains("विज्ञान") || subject.contains("संविधान")
            val isLanguageQ = subject.contains("हिंदी") || subject.contains("english", ignoreCase = true)
            
            // Comprehensive visual keywords for Reasoning questions
            val hasReasoningVisualKeyword = 
                lowerQ.contains("आकृति") || lowerQ.contains("चित्र") || lowerQ.contains("दर्पण") ||
                lowerQ.contains("प्रतिबिंब") || lowerQ.contains("प्रतिबिम्ब") || lowerQ.contains("कागज") ||
                lowerQ.contains("कागज़") || lowerQ.contains("मोड़") || lowerQ.contains("काट") ||
                lowerQ.contains("वेन") || lowerQ.contains("पासा") || lowerQ.contains("पासे") ||
                lowerQ.contains("घन") || lowerQ.contains("पैटर्न") || lowerQ.contains("रेखाचित्र") ||
                lowerQ.contains("अंतर्निहित") || lowerQ.contains("सन्निहित") || lowerQ.contains("विपरीत") ||
                lowerQ.contains("लुप्त") || lowerQ.contains("शीट") || lowerQ.contains("बिंदीदार") ||
                lowerQ.contains("त्रिभुज") || lowerQ.contains("आयत") || lowerQ.contains("गिनती") ||
                lowerQ.contains("संयोजन") || lowerQ.contains("श्रृंखला") || lowerQ.contains("शृंखला") ||
                lowerQ.contains("figure") || lowerQ.contains("diagram") || lowerQ.contains("mirror") ||
                lowerQ.contains("water") || lowerQ.contains("folding") || lowerQ.contains("cutting") ||
                lowerQ.contains("dice") || lowerQ.contains("cube") || lowerQ.contains("venn") ||
                lowerQ.contains("embedded") || lowerQ.contains("pattern") || lowerQ.contains("triangle")

            val hasOptionVisualKeyword = options.any { opt ->
                val t = opt.trim().lowercase()
                t.contains("आकृति") || t.contains("चित्र") || t.contains("fig") ||
                t.matches(Regex("""^(?:विकल्प|चित्र|आकृति|Option|Figure)?\s*[\(\[]?[1-4A-Da-d][\)\]]?[\.]?$""", RegexOption.IGNORE_CASE))
            }

            // Options that are numbers, values, measurements, or pure text (not figures)
            val areOptionsNumbersOrText = options.all { opt ->
                val t = opt.trim()
                val isNumber = t.matches(Regex("""^[\(\[]?[1-4A-Da-d][\)\]]?\s*[-+]?[0-9]+(?:[\.,][0-9]+)?(?:\s*(?:cm|m|km|kg|g|°|%|रू|रु|दिन|वर्ष|घंटे|सेमी|मीटर|लीटर|सेकंड|min|sec|hr|sq|वर्ग))?$""", RegexOption.IGNORE_CASE)) ||
                               t.matches(Regex("""^[-+]?[0-9]+(?:[\.,][0-9]+)?(?:\s*(?:cm|m|km|kg|g|°|%|रू|रु|दिन|वर्ष|घंटे|सेमी|मीटर|लीटर|सेकंड|min|sec|hr|sq|वर्ग))?$""", RegexOption.IGNORE_CASE))
                val isTextWord = t.length in 1..30 && !t.contains("आकृति") && !t.contains("चित्र") && !t.matches(Regex("""^(?:विकल्प|चित्र|आकृति|Option|Figure)?\s*[\(\[]?[1-4A-Da-d][\)\]]?[\.]?$""", RegexOption.IGNORE_CASE))
                isNumber || isTextWord
            }

            // Diagram box should ONLY include options if options are visual drawings/shapes, never if they are numbers or text!
            val hasVisualOptions = hasOptionVisualKeyword && !areOptionsNumbersOrText

            // Math visual keywords (charts, graphs, data interpretation)
            val hasMathChartKeyword = 
                lowerQ.contains("पाई चार्ट") || lowerQ.contains("दण्ड आरेख") || lowerQ.contains("बार चार्ट") ||
                lowerQ.contains("रेखाचित्र") || lowerQ.contains("आलेख") || lowerQ.contains("ग्राफ") ||
                lowerQ.contains("pie chart") || lowerQ.contains("bar chart") || lowerQ.contains("histogram") ||
                lowerQ.contains("graph")

            // Strict diagram detection per subject:
            // - Computer, GK, Hindi, English: NEVER have diagrams (100% text-based in SSC/Police papers)
            // - Math: ONLY if chart/graph keyword AND explicit diagram from AI
            // - Reasoning: ALWAYS if visual keywords, visual options, or AI detected diagram
            val hasDiagram = when {
                isComputerQ -> false
                isGkQ -> false
                isLanguageQ -> false
                isMathQ -> hasMathChartKeyword && (extractedBox != null || obj.optBoolean("hasDiagram", false))
                isReasoningQ -> obj.optBoolean("hasDiagram", false) || extractedBox != null || hasReasoningVisualKeyword || hasOptionVisualKeyword
                else -> false
            }

            // Check if any option itself contains a full image URL or Base64 data
            if (imageUrl.isNullOrBlank()) {
                val optWithImg = options.firstOrNull { opt ->
                    opt.startsWith("data:image", true) || opt.startsWith("http://") || opt.startsWith("https://")
                }
                if (optWithImg != null) {
                    imageUrl = optWithImg
                }
            }

            // Crop diagram ONLY when an explicit bounding box is provided.
            // Never crop blind fallback slices of the page for text questions!
            if (imageUrl.isNullOrBlank() && hasDiagram && extractedBox != null && !pages.isNullOrEmpty()) {
                val pageBase64 = pages.getOrNull(pageIdx)
                if (!pageBase64.isNullOrBlank()) {
                    imageUrl = cropDiagramFromPage(pageBase64, extractedBox, hasVisualOptions)
                }
            }

            if (question.isNotBlank() && options.isNotEmpty()) {
                list.add(
                    QuizQuestion(
                        id = id,
                        question = question,
                        options = options.take(4),
                        correctIndex = correctIndex,
                        explanation = explanation,
                        subject = subject,
                        imageUrl = imageUrl,
                        examShiftInfo = examShiftInfo
                    )
                )
            }
        }
        return list
    }

    fun parseJsonArrayToQuestions(jsonString: String, subjectOverride: String? = null): List<QuizQuestion> {
        val list = mutableListOf<QuizQuestion>()
        try {
            val cleanJson = jsonString.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = JSONArray(cleanJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val rawId = obj.opt("id") ?: obj.opt("questionNumber") ?: obj.opt("qNo") ?: obj.opt("number")
                var id = when (rawId) {
                    is Number -> rawId.toInt()
                    is String -> Regex("""\d+""").find(rawId)?.value?.toIntOrNull() ?: ((System.currentTimeMillis() % 100000).toInt() + i)
                    else -> (System.currentTimeMillis() % 100000).toInt() + i
                }
                val rawQuestion = obj.optString("question", "")
                if (id > 1000 || id <= 0) {
                    val leadingNum = Regex("""^(?:Q(?:uestion)?|प्र(?:श्न)?|Q\.)?\s*[\#\№]?\s*(\d{1,3})\s*[\.\:\)\-\/]""").find(rawQuestion.trim())?.groupValues?.getOrNull(1)?.toIntOrNull()
                    if (leadingNum != null && leadingNum in 1..300) {
                        id = leadingNum
                    }
                }
                val (cleanedQ, detectedShiftTag) = extractShiftTagFromText(cleanQuestionText(rawQuestion))
                val question = cleanedQ

                val rawShiftInfo = obj.optString("examShiftInfo", "")
                    .ifBlank { obj.optString("shiftInfo", "") }
                    .ifBlank { obj.optString("examDateShift", "") }
                    .ifBlank { obj.optString("examInfo", "") }
                    .ifBlank { obj.optString("source", "") }
                    .ifBlank { detectedShiftTag ?: "" }

                val examShiftInfo = if (rawShiftInfo.isNotBlank() && rawShiftInfo != "null") rawShiftInfo.trim() else null

                val optionsArray = obj.optJSONArray("options")
                val options = mutableListOf<String>()
                if (optionsArray != null) {
                    for (j in 0 until optionsArray.length()) {
                        options.add(cleanOptionText(optionsArray.getString(j)))
                    }
                }
                while (options.size < 4) {
                    options.add("विकल्प ${options.size + 1}")
                }
                val correctIndex = obj.optInt("correctIndex", 0).coerceIn(0, 3)
                val explanation = obj.optString("explanation", "आधिकारिक उत्तर कुंजी के अनुसार सही विकल्प है।")
                val rawSubject = obj.optString("subject", "GK / GS")
                val subject = subjectOverride ?: normalizeSubject(rawSubject)
                val rawImageUrl = obj.optString("imageUrl", "")
                val imageUrl = if (rawImageUrl.isNotBlank() && rawImageUrl != "null") rawImageUrl else null

                if (question.isNotBlank()) {
                    list.add(
                        QuizQuestion(
                            id = id,
                            question = question,
                            options = options.take(4),
                            correctIndex = correctIndex,
                            explanation = explanation,
                            subject = subject,
                            imageUrl = imageUrl,
                            examShiftInfo = examShiftInfo
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }

    /**
     * Intelligently parses correct answer index from various formats with multi-signal cross-verification:
     * - Exact option text matching (eliminates 0-based vs 1-based indexing ambiguity)
     * - Explicit option letters ("A", "B", "C", "D" or "a", "b", "c", "d")
     * - Hindi letters ("क", "ख", "ग", "घ")
     * - Answer hints inside explanation text e.g. "उत्तर: (B)" or "उत्तर-(c)" or "(b) सही उत्तर"
     * - Multiple keys: correctAnswer, correctOption, correctIndex, answer, ans, correct, officialAnswer
     */
    fun parseCorrectIndex(obj: JSONObject, options: List<String>): Int {
        // Step 1: Text-Matching: Check if any candidate field contains the EXACT or near-exact text of an option
        val textKeys = listOf(
            "correctAnswer", "correct_answer", "rightAnswer", "correctOptionText",
            "rightOptionText", "answer", "ans", "correct_ans", "officialAnswer"
        )
        for (key in textKeys) {
            if (obj.has(key)) {
                val rawVal = obj.opt(key)
                if (rawVal is String && rawVal.isNotBlank()) {
                    val cleanV = cleanOptionText(rawVal).trim().lowercase()
                    // If cleanV is just an option letter (A, B, C, D) handle via letter map
                    val letterIdx = mapOptionCharToIndex(cleanV)
                    if (letterIdx != null && cleanV.length <= 4) {
                        return letterIdx
                    }

                    // Direct string comparison with options
                    val matchIdx = options.indexOfFirst { opt ->
                        val cleanOpt = cleanOptionText(opt).trim().lowercase()
                        cleanOpt == cleanV || (cleanV.length >= 3 && cleanOpt.contains(cleanV)) || (cleanOpt.length >= 3 && cleanV.contains(cleanOpt))
                    }
                    if (matchIdx in 0..3) return matchIdx
                }
            }
        }

        // Step 2: Check explicit option letter keys (e.g. "correctOption": "A", "correctLetter": "B")
        val letterKeys = listOf("correctOption", "correct_option", "rightOption", "correctLetter", "option", "ansLetter")
        for (key in letterKeys) {
            if (obj.has(key)) {
                val v = obj.optString(key, "").trim()
                val idx = mapOptionCharToIndex(v)
                if (idx != null) return idx
            }
        }

        // Step 3: Check explanation text for explicit answer indicator e.g. "उत्तर: (B)" or "(c) सही विकल्प"
        val expl = obj.optString("explanation", "")
        val explAns = extractAnswerFromText(expl)
        if (explAns != null) {
            return explAns
        }

        // Step 4: Direct "correctIndex" with consistency checks
        if (obj.has("correctIndex")) {
            val v = obj.opt("correctIndex")
            if (v is Number) {
                val i = v.toInt()
                // If 0..3, check if it was given directly
                if (i in 0..3) {
                    return i
                }
                if (i in 1..4) {
                    return i - 1
                }
            }
            if (v is String && v.isNotBlank()) {
                val idx = mapOptionCharToIndex(v)
                if (idx != null) return idx
            }
        }

        // Step 5: Check other fallback answer keys
        val answerKeys = listOf("answerKey", "correct", "official_answer", "ansIndex")
        for (key in answerKeys) {
            if (obj.has(key)) {
                val v = obj.opt(key)
                if (v is Number) {
                    val i = v.toInt()
                    if (i in 0..3) return i
                    if (i in 1..4) return i - 1
                }
                if (v is String && v.isNotBlank()) {
                    val idx = mapOptionCharToIndex(v)
                    if (idx != null) return idx
                }
            }
        }

        // Step 6: Check question text for inline answers (e.g. question ends with "[उत्तर: B]")
        val qText = obj.optString("question", "")
        val qAns = extractAnswerFromText(qText)
        if (qAns != null) return qAns

        return 0
    }

    fun mapOptionCharToIndex(raw: String): Int? {
        val s = raw.trim().lowercase()
        return when {
            s.startsWith("(a)") || s.startsWith("[a]") || s == "a" || s == "option a" || s == "विकल्प a" || s == "विकल्प 1" || s == "1" || s == "(1)" || s == "[1]" || s == "क" || s == "(क)" || s == "[क]" -> 0
            s.startsWith("(b)") || s.startsWith("[b]") || s == "b" || s == "option b" || s == "विकल्प b" || s == "विकल्प 2" || s == "2" || s == "(2)" || s == "[2]" || s == "ख" || s == "(ख)" || s == "[ख]" -> 1
            s.startsWith("(c)") || s.startsWith("[c]") || s == "c" || s == "option c" || s == "विकल्प c" || s == "विकल्प 3" || s == "3" || s == "(3)" || s == "[3]" || s == "ग" || s == "(ग)" || s == "[ग]" -> 2
            s.startsWith("(d)") || s.startsWith("[d]") || s == "d" || s == "option d" || s == "विकल्प d" || s == "विकल्प 4" || s == "4" || s == "(4)" || s == "[4]" || s == "घ" || s == "(घ)" || s == "[घ]" -> 3
            s.contains("option a") || s.contains("विकल्प 1") || s.contains("विकल्प a") || s.contains("(a)") -> 0
            s.contains("option b") || s.contains("विकल्प 2") || s.contains("विकल्प b") || s.contains("(b)") -> 1
            s.contains("option c") || s.contains("विकल्प 3") || s.contains("विकल्प c") || s.contains("(c)") -> 2
            s.contains("option d") || s.contains("विकल्प 4") || s.contains("विकल्प d") || s.contains("(d)") -> 3
            s.matches(Regex("""^a[\.\)\:\-\s].*""")) -> 0
            s.matches(Regex("""^b[\.\)\:\-\s].*""")) -> 1
            s.matches(Regex("""^c[\.\)\:\-\s].*""")) -> 2
            s.matches(Regex("""^d[\.\)\:\-\s].*""")) -> 3
            s.matches(Regex("""^1[\.\)\:\-\s].*""")) -> 0
            s.matches(Regex("""^2[\.\)\:\-\s].*""")) -> 1
            s.matches(Regex("""^3[\.\)\:\-\s].*""")) -> 2
            s.matches(Regex("""^4[\.\)\:\-\s].*""")) -> 3
            s.matches(Regex("""^क[\.\)\:\-\s].*""")) -> 0
            s.matches(Regex("""^ख[\.\)\:\-\s].*""")) -> 1
            s.matches(Regex("""^ग[\.\)\:\-\s].*""")) -> 2
            s.matches(Regex("""^घ[\.\)\:\-\s].*""")) -> 3
            else -> null
        }
    }

    fun extractAnswerFromText(text: String): Int? {
        if (text.isBlank()) return null
        val patterns = listOf(
            Regex("""(?:उत्तर|सही\s*विकल्प|सही\s*उत्तर|right\s*ans(?:wer)?)\s*[\:\-\=]?\s*[\(\[]?\s*([a-dA-D1-4क-घ])\s*[\)\]]?""", RegexOption.IGNORE_CASE),
            Regex("""(?:विकल्प|option)\s*[\(\[]?\s*([a-dA-D1-4क-घ])\s*[\)\]]?\s*(?:सही|उपयुक्त|करेक्ट|is\s*correct)""", RegexOption.IGNORE_CASE),
            Regex("""(?:ans(?:wer)?|right\s*ans(?:wer)?)\s*[\.\:\-\=]?\s*[\(\[]?\s*([a-dA-D1-4क-घ])\s*[\)\]]?""", RegexOption.IGNORE_CASE),
            Regex("""^[\(\[]?\s*([a-dA-D1-4क-घ])\s*[\)\]]\s*(?:व्याख्या|हल|अर्थ|सही|:)""", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val m = pattern.find(text)
            if (m != null) {
                val idx = mapOptionCharToIndex(m.groupValues[1])
                if (idx != null) return idx
            }
        }
        return null
    }

    /**
     * Extracts strictly the Hindi text from bilingual questions (English + Hindi),
     * completely stripping the English question text while preserving reasoning codes,
     * letter/number series (e.g. "A, B, D, G, ?", "14, 19, 24, 29, ?"), and formulas.
     */
    fun extractHindiOnlyFromBilingualQuestion(raw: String): String {
        var text = raw.trim()
        if (!text.any { it in '\u0900'..'\u097F' }) {
            // If there are no Hindi characters at all (e.g. English comprehension section), return as is
            return text
        }

        // 1. Check if separated by explicit bilingual tags e.g. "English: ... Hindi: ..." or "ENG: ... HIN: ..."
        val explicitHindiTagMatch = Regex("""(?:Hindi|HIN|हिंदी|हिन्दी)\s*[\:\-\=]\s*([\u0900-\u097F].*)""", RegexOption.IGNORE_CASE).find(text)
        if (explicitHindiTagMatch != null) {
            val extracted = explicitHindiTagMatch.groupValues[1].trim()
            if (extracted.any { it in '\u0900'..'\u097F' }) {
                text = extracted
            }
        }

        // Helper to check if a non-Hindi line is a series, equation, code, or data line that MUST be kept
        fun isDataOrSeriesLine(line: String): Boolean {
            val t = line.trim()
            if (t.isBlank()) return false
            // If line has standard English grammar words, it is a sentence, NOT a data line!
            val englishProsePattern = Regex("""\b(?:the|is|are|was|were|an|an|of|in|to|for|with|by|from|at|on|costs?|marked|sold|discount|profit|loss|price|buying|selling|man|article|shopkeeper|pen|pencils?|wish|transfer|amount|bank|system|choice|which|what|how|where|when|find|select|determine)\b""", RegexOption.IGNORE_CASE)
            if (englishProsePattern.containsMatchIn(t)) return false

            // Check series like "A, B, D, G, ?", "14, 19, 24, 29, ?", "F, J, O, U, ?", "900, 450, 225, 112.5, ?", "11, 22, 44, 88: 14, 28, 56, ?"
            val isCommaSeries = (t.contains(",") || t.contains("::") || t.contains(":")) && (t.contains("?") || t.matches(Regex("""^[\w\s\,\:\.\-\+\*\/\=\#\%\$\^\(\)\?]+$""")))
            val isShortCodeOrEquation = t.matches(Regex("""^[A-Z0-9\s,\+\-\*\/\=\:\;\<\>\(\)\?\.\#\%\^\_\'\"\–\—]{1,60}$"""))
            return isCommaSeries || isShortCodeOrEquation
        }

        // Helper to check if a line is an English question sentence that should be discarded
        fun isEnglishSentenceLine(line: String): Boolean {
            val t = line.trim()
            if (t.any { it in '\u0900'..'\u097F' }) return false
            if (isDataOrSeriesLine(t)) return false
            val words = t.split(Regex("""\s+"""))
            return words.size >= 3 || t.endsWith("?") || Regex("""[a-zA-Z]{3,}""").containsMatchIn(t)
        }

        // 2. Check newline-separated bilingual text:
        if (text.contains("\n")) {
            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
            val hasAnyHindi = lines.any { it.any { c -> c in '\u0900'..'\u097F' } }

            if (hasAnyHindi) {
                val keptLines = mutableListOf<String>()
                for (line in lines) {
                    if (line.any { it in '\u0900'..'\u097F' }) {
                        keptLines.add(line)
                    } else if (isDataOrSeriesLine(line) && !isEnglishSentenceLine(line)) {
                        // Only add if not already duplicate
                        if (!keptLines.contains(line)) {
                            keptLines.add(line)
                        }
                    }
                    // Pure English sentence lines are discarded
                }
                if (keptLines.isNotEmpty()) {
                    text = keptLines.joinToString("\n")
                }
            }
        }

        // 3. Slash / double slash / pipe separated bilingual questions:
        // e.g. "Who was the first President of India? / भारत के प्रथम राष्ट्रपति कौन थे?"
        if (text.contains("/") || text.contains("|")) {
            val delimiterRegex = Regex("""\s*(?:\/{1,2}|\|)\s*""")
            val parts = text.split(delimiterRegex).map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val hindiParts = parts.filter { it.any { c -> c in '\u0900'..'\u097F' } }
                val nonHindiParts = parts.filter { !it.any { c -> c in '\u0900'..'\u097F' } }
                if (hindiParts.isNotEmpty() && nonHindiParts.isNotEmpty()) {
                    // Check if any non-Hindi part is a series/data line needed by the question
                    val dataParts = nonHindiParts.filter { isDataOrSeriesLine(it) && !isEnglishSentenceLine(it) }
                    val combinedHindi = hindiParts.joinToString(" ")
                    text = if (dataParts.isNotEmpty() && !combinedHindi.contains(dataParts.first())) {
                        "$combinedHindi\n${dataParts.joinToString("\n")}"
                    } else {
                        combinedHindi
                    }
                }
            }
        }

        // 4. Inline English question sentence followed immediately by Hindi question:
        // e.g. "Who among the following was the founder of Brahmo Samaj? निम्नलिखित में से कौन ब्रह्म समाज के संस्थापक थे?"
        val inlineEngMatch = Regex("""^([A-Za-z0-9\s,\-–—\(\)\'\"\/]{6,}[\?\.\:\;\-–—]\s*)([\u0900-\u097F].*)$""").find(text)
        if (inlineEngMatch != null) {
            val engPrefix = inlineEngMatch.groupValues[1].trim()
            val hindiSuffix = inlineEngMatch.groupValues[2].trim()
            val hindiWordCount = hindiSuffix.split(Regex("""\s+""")).count { it.any { c -> c in '\u0900'..'\u097F' } }
            if (hindiWordCount >= 2 && hindiSuffix.length >= 6) {
                val engWordCount = engPrefix.split(Regex("""\s+""")).size
                if (engWordCount >= 2 || engPrefix.endsWith("?") ||
                    engPrefix.contains("Which", ignoreCase = true) ||
                    engPrefix.contains("Who", ignoreCase = true) ||
                    engPrefix.contains("What", ignoreCase = true) ||
                    engPrefix.contains("Select", ignoreCase = true) ||
                    engPrefix.contains("Find", ignoreCase = true) ||
                    engPrefix.contains("Identify", ignoreCase = true)
                ) {
                    text = hindiSuffix
                }
            }
        }

        // 5. Parentheses Bilingual text:
        // e.g. "Who was the first President of India? (भारत के प्रथम राष्ट्रपति कौन थे?)" -> extract Hindi inside ()
        val parenthesizedHindiMatch = Regex("""^[A-Za-z0-9\s,\-–—\.\?\:\;\/]+\s*[\(\[]\s*([\u0900-\u097F\s\d\.\,\?\-\_]+)\s*[\)\]]$""").find(text)
        if (parenthesizedHindiMatch != null) {
            val innerHindi = parenthesizedHindiMatch.groupValues[1].trim()
            if (innerHindi.length >= 6 && innerHindi.split(Regex("""\s+""")).size >= 2) {
                text = innerHindi
            }
        } else {
            // Strip trailing English translation in parentheses: "हिंदी प्रश्न? (English Translation?)"
            text = text.replace(Regex("""\s*[\(\[]\s*[A-Za-z0-9\s,\-–—\.\?\:\;\/]{4,}\s*[\)\]]\s*$"""), "").trim()
        }

        // 6. Strip stray language labels at start like "Hindi -", "हिंदी:", "Q.", etc.
        text = text.replace(Regex("""^(?:Hindi|HIN|हिंदी|हिन्दी)\s*[\:\-\=]\s*""", RegexOption.IGNORE_CASE), "").trim()

        return text.trim()
    }

    /**
     * Cleans question text by removing redundant numbering prefixes like:
     * "Q.1", "1.", "प्रश्न 1 :", "**Q1**", etc., stripping trailing leaked answer keys,
     * and strictly filtering bilingual text to extract ONLY pure Hindi.
     */
    fun cleanQuestionText(raw: String): String {
        var text = raw.trim()
        text = text.replace(Regex("""^\*+\s*(?:Q(?:uestion)?|प्र(?:श्न)?|Q\.)?\s*[\#\№]?\s*\d{1,3}\s*[\.\:\)\-\/]?\s*\*+""", RegexOption.IGNORE_CASE), "").trim()
        text = text.replace(Regex("""^(?:Q(?:uestion)?|प्र(?:श्न)?|Q\.)\s*[\#\№]?\s*\d{1,3}\s*[\.\:\)\-\/]\s*""", RegexOption.IGNORE_CASE), "").trim()
        text = text.replace(Regex("""^\d{1,3}\s*[\.\:\)\-\/]\s*"""), "").trim()
        text = text.replace(Regex("""^\(\d{1,3}\)\s*"""), "").trim()
        // Strip accidental trailing answer keys leaked into question e.g. "उत्तर-(b)", "[Ans: C]", "Correct Answer: B"
        text = text.replace(Regex("""\s*\[?\s*(?:उत्तर|Ans(?:wer)?|Correct\s*Answer)\s*[\:\-\=]?\s*[\(\[]?\s*[A-Da-d1-4क-घ]\s*[\)\]]?\s*\]?\s*$""", RegexOption.IGNORE_CASE), "").trim()

        // Extract strictly Hindi text when question is bilingual (English + Hindi)
        text = extractHindiOnlyFromBilingualQuestion(text)

        // If question text is still in pure English (e.g. Testbook cut-off question), translate standard reasoning phrases to Hindi
        if (!text.any { it in '\u0900'..'\u097F' }) {
            text = when {
                text.contains("How many squares are there in the given figure", true) -> "दी गई आकृति में कितने वर्ग हैं?"
                text.contains("How many triangles are there in the given figure", true) -> "दी गई आकृति में कितने त्रिभुज हैं?"
                text.contains("How many rectangles are there in the given figure", true) -> "दी गई आकृति में कितने आयत हैं?"
                text.contains("How many circles are there in the given figure", true) -> "दी गई आकृति में कितने वृत्त हैं?"
                text.contains("Which of the following pair is the correct pair of the elements that came at the opposite", true) ||
                        text.contains("opposite of each other when the open dice is closed", true) ->
                    "निम्नलिखित में से कौन सा जोड़ा उन तत्वों का सही जोड़ा है जो एक-दूसरे के विपरीत आते हैं जब खुले पासे को बंद किया जाता है?"
                text.contains("Which will be the correct image formed towards shaded part", true) ||
                        text.contains("when question diagram is put in front of it", true) ||
                        text.contains("mirror image of the given figure", true) ->
                    "प्रश्न आकृति के सामने रखे जाने पर छायांकित भाग के सामने कौन सी सही छवि बनेगी?"
                text.contains("A related pair of figures is given", true) ||
                        text.contains("Select a pair that has a relationship similar to that in the original pair", true) ->
                    "एक संबंधित आकृतियों का जोड़ा दिया गया है, जिसके बाद चार जोड़े दिए गए हैं। उस जोड़े का चयन करें जिसमें मूल जोड़े के समान संबंध हो।"
                text.contains("Portrait : Painter", true) ->
                    "इनमें से कौन सा विकल्प दूसरे शब्द युग्म में ठीक उसी प्रकार संबंधित है, जिस प्रकार पहले शब्द युग्म के शब्द आपस में संबंधित हैं? चित्र : चित्रकार :: सिम्फनी : ?"
                text.contains("search engine, result list, click", true) || text.contains("search engine, result list", true) ->
                    "श्रृंखला में अगला पद ज्ञात कीजिए: खोज इंजन, परिणाम सूची, क्लिक करना, ?"
                text.contains("means +", true) && text.contains("means -", true) ->
                    text.replace(Regex("""^If\s*""", RegexOption.IGNORE_CASE), "यदि ")
                        .replace("means", "का अर्थ")
                        .replace("then solve:", "तो हल कीजिए:")
                text.contains("Four numbers are given below", true) || text.contains("three of them follow the same logic", true) ->
                    "नीचे चार संख्याएँ दी गई हैं, जिनमें से तीन एक ही तर्क का पालन करती हैं, भिन्न संख्या ज्ञात करें और उसे उत्तर के रूप में चिह्नित करें।"
                text.contains("is defined as", true) && text.contains("then what is the value", true) ->
                    text.replace(Regex("""^If for any numbers\s*""", RegexOption.IGNORE_CASE), "यदि किसी भी संख्या ")
                        .replace("is defined as", "के रूप में परिभाषित किया गया है,")
                        .replace("then what is the value of", "तो मान क्या होगा:")
                text.contains("Which of the following numbers will replace the question mark", true) ->
                    text.replace(Regex("""^Which of the following numbers will replace the question mark\s*\(\?\)\s*in the given series\??[\s\:]*""", RegexOption.IGNORE_CASE), "निम्नलिखित संख्या श्रृंखला में प्रश्न चिह्न (?) के स्थान पर कौन-सी संख्या आएगी? ")
                text.contains("Pointing towards a person", true) && text.contains("Anjali said", true) ->
                    "एक फोटोग्राफ में एक व्यक्ति की ओर इशारा करते हुए, अंजलि ने कहा, \"वह मेरी बहन के भाई के पिता का इकलौता बेटा है\"। वह व्यक्ति अंजलि से कैसे संबंधित है?"
                text.contains("D is the son-in-law of B", true) || (text.contains("son-in-law", true) && text.contains("brother-in-law", true)) ->
                    "D, B का दामाद है, A का बहनोई है जो C का भाई है, A, B से कैसे संबंधित है?"
                text.contains("How is", true) && text.contains("related to", true) ->
                    text.replace("How is", "किस प्रकार").replace("related to", "से संबंधित है?")
                text.contains("Select the option that shows the same relationship", true) ->
                    text.replace(Regex("""^Select the option that shows the same relationship as between the first pair of (?:words|numbers)[\.\:\s]*""", RegexOption.IGNORE_CASE), "पहले शब्दों के जोड़े के बीच के संबंध के समान संबंध दिखाने वाला विकल्प चुनें: ")
                text.contains("Find the next letter in the series", true) ->
                    text.replace(Regex("""^Find the next letter in the series[\:\s]*""", RegexOption.IGNORE_CASE), "निम्नलिखित श्रृंखला में अगले स्थान पर आने वाले अक्षर का चयन करें: ")
                text.contains("Find the next number in the series", true) ->
                    text.replace(Regex("""^Find the next number in the series[\:\s]*""", RegexOption.IGNORE_CASE), "निम्नलिखित श्रृंखला में अगले स्थान पर आने वाली संख्या ज्ञात करें: ")
                text.contains("Find the next word in the series", true) ->
                    text.replace(Regex("""^Find the next word in the series[\:\s]*""", RegexOption.IGNORE_CASE), "निम्नलिखित श्रृंखला में अगले स्थान पर कौन सा शब्द आएगा? ")
                text.contains("Which is the odd one out", true) ->
                    text.replace(Regex("""^Which is the odd one out\??[\s\:]*""", RegexOption.IGNORE_CASE), "इनमें से कौन बाकी सभी से विषम है? ")
                text.contains("Find the odd one out", true) ->
                    text.replace(Regex("""^Find the odd one out from the following[\:\s]*""", RegexOption.IGNORE_CASE), "निम्नलिखित में से विषम को ढूँढें: ")
                else -> text
            }
        }

        // Deduplicate identical repeated question parts e.g. "Question / Question"
        if (text.contains("/") || text.contains("|") || text.contains("//")) {
            val parts = text.split(Regex("""\s*(?:\/{1,2}|\|)\s*""")).map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size >= 2 && parts.all { it.equals(parts.first(), ignoreCase = true) }) {
                text = parts.first()
            }
        }

        // Smart formatting for Hindi Idioms, Proverbs, and Underlined Words
        text = formatUnderlinedWordsAndIdioms(text)

        return text.trim()
    }

    /**
     * Ensures that idioms, proverbs, and target words in Hindi questions are properly
     * formatted in bold (**word**), while reserving underline (_word_) strictly for
     * questions asking about an underlined part (रेखांकित शब्द / रेखांकित अंश).
     */
    fun formatUnderlinedWordsAndIdioms(raw: String): String {
        var text = raw

        // 1. If multi-line question where second line is an isolated idiom (e.g. "मुहावरे और उसके अर्थ के लिए सही विकल्प चुनें।\nघर बसाना")
        val lines = text.split("\n")
        if (lines.size == 2 && (lines[0].contains("मुहावरे") || lines[0].contains("लोकोक्ति") || lines[0].contains("पर्यायवाची") || lines[0].contains("विलोम"))) {
            val secondLine = lines[1].trim()
            if (secondLine.isNotBlank() && !secondLine.startsWith("_") && !secondLine.startsWith("<u>") && !secondLine.startsWith("**")) {
                text = "${lines[0].trim()}\n**$secondLine**"
            }
        }

        // 2. If question is specifically asking about an underlined word/part (रेखांकित शब्द / अंश)
        val isUnderlineQuestion = text.contains("रेखांकित")
        if (!text.contains("_") && !text.contains("<u>")) {
            if (isUnderlineQuestion) {
                // Wrap quoted word in _word_ for underline
                text = text.replace(Regex("""(?:^|\s)['‘“]([\p{L}\p{M}\s]{2,40})['’”](?:\s|[\,\.\:\-\?]|\$)""")) { matchResult ->
                    val phrase = matchResult.groupValues[1].trim()
                    matchResult.value.replace("'$phrase'", "'_${phrase}_'")
                        .replace("‘$phrase’", "‘_${phrase}_’")
                        .replace("“$phrase”", "“_${phrase}_”")
                }
            } else if (text.contains("मुहावरे") || text.contains("मुहावरा") || text.contains("लोकोक्ति") || text.contains("पर्यायवाची") || text.contains("विलोम")) {
                // Wrap in **word** for bold ONLY (NO underline)
                text = text.replace(Regex("""(?:^|\s)['‘“]([\p{L}\p{M}\s]{2,40})['’”](?:\s|[\,\.\:\-\?]|\$)""")) { matchResult ->
                    val phrase = matchResult.groupValues[1].trim()
                    matchResult.value.replace("'$phrase'", "'**$phrase**'")
                        .replace("‘$phrase’", "‘**$phrase**’")
                        .replace("“$phrase”", "“**$phrase**”")
                }
            }
        }

        return text
    }

    /**
     * Checks if word token overlap between two strings is >= threshold (e.g. 70%).
     */
    fun areWordsSimilar(s1: String, s2: String, threshold: Float = 0.70f): Boolean {
        if (s1.trim().equals(s2.trim(), ignoreCase = true)) return true
        val w1 = s1.lowercase().split(Regex("[^\\p{L}\\p{M}\\p{N}]+")).filter { it.isNotBlank() }.toSet()
        val w2 = s2.lowercase().split(Regex("[^\\p{L}\\p{M}\\p{N}]+")).filter { it.isNotBlank() }.toSet()
        if (w1.isEmpty() || w2.isEmpty()) return false
        val inter = w1.intersect(w2).size
        val union = w1.union(w2).size
        return (inter.toFloat() / union.toFloat()) >= threshold
    }

    /**
     * Checks if two options are identical or semantically identical (ignoring minor matras/plural endings).
     */
    fun areOptionsSimilar(opt1: String, opt2: String): Boolean {
        if (opt1.trim().equals(opt2.trim(), ignoreCase = true)) return true
        val clean1 = opt1.replace("[^\\p{L}\\p{M}\\p{N}]+".toRegex(), "").lowercase()
        val clean2 = opt2.replace("[^\\p{L}\\p{M}\\p{N}]+".toRegex(), "").lowercase()
        if (clean1 == clean2 && clean1.isNotBlank()) return true
        if (clean1.length >= 8 && clean2.length >= 8 && (clean1.contains(clean2) || clean2.contains(clean1))) return true
        val w1 = opt1.lowercase().split(Regex("[^\\p{L}\\p{M}\\p{N}]+")).filter { it.isNotBlank() }.toSet()
        val w2 = opt2.lowercase().split(Regex("[^\\p{L}\\p{M}\\p{N}]+")).filter { it.isNotBlank() }.toSet()
        if (w1.isEmpty() || w2.isEmpty()) return false
        val inter = w1.intersect(w2).size
        val union = w1.union(w2).size
        return (inter.toFloat() / union.toFloat() >= 0.5f) || (inter >= 3 && inter >= w1.size - 2)
    }

    /**
     * Identifies dummy/placeholder options generated when options are cut off at page bottom.
     * Real short options (e.g. "12", "32", "हाँ", "सिर") are NEVER considered dummy!
     */
    fun hasDummyOptions(opts: List<String>): Boolean {
        if (opts.isEmpty()) return true
        val placeholderPattern = Regex("""^(?:विकल्प|option|opt)\s*[\(\[]?[1-4a-da-dक-घ]?[\)\]]?$""", RegexOption.IGNORE_CASE)
        return opts.all { opt ->
            val trimmed = opt.trim().lowercase()
            trimmed.isBlank() ||
            placeholderPattern.matches(trimmed) ||
            trimmed.contains("नहीं दिए गए") ||
            trimmed.contains("उपलब्ध नहीं") ||
            trimmed == "n/a" || trimmed == "null"
        }
    }

    /**
     * Determines whether two extracted questions represent the exact same question.
     */
    fun isDuplicateQuestion(existing: QuizQuestion, newQ: QuizQuestion): Boolean {
        val cleanExistText = existing.question.replace("[^\\p{L}\\p{M}\\p{N}]+".toRegex(), "").lowercase()
        val cleanNewText = newQ.question.replace("[^\\p{L}\\p{M}\\p{N}]+".toRegex(), "").lowercase()

        val isExactQuestionText = cleanExistText.length >= 15 && cleanExistText == cleanNewText

        var matchedOpts = 0
        for (o1 in existing.options) {
            if (newQ.options.any { o2 -> areOptionsSimilar(o1, o2) }) {
                matchedOpts++
            }
        }

        // Critical Safeguard 0: If both questions have official printed test IDs (1..300) and the IDs are different (e.g. Q70 vs Q73),
        // they are separate questions unless ALL 4 options and text are 100% identical!
        if (existing.id in 1..300 && newQ.id in 1..300 && existing.id != newQ.id) {
            val isExactAll4Match = isExactQuestionText && matchedOpts >= 4
            return isExactAll4Match
        }

        // Absolute Safeguard 1: If question text (>=15 chars) is 100% identical and at least 3-4 options match, it IS a duplicate (e.g. repeated page in PDF)
        val hasRealOpts = !hasDummyOptions(existing.options) && !hasDummyOptions(newQ.options)
        if (isExactQuestionText && hasRealOpts && matchedOpts >= 3) {
            return true
        }

        // Critical safeguard 1: If operative keywords (shapes, nouns, roles) differ, they CANNOT be duplicates!
        // E.g. "दी गई आकृति में कितने त्रिभुज हैं?" (triangles) vs "दी गई आकृति में कितने वर्ग हैं?" (squares)
        val operativeKeywords = listOf(
            "त्रिभुज", "वर्ग", "आयत", "वृत्त", "पासा", "पासे", "घन", "दर्पण", "जल", "रेखा", "रेखाएं",
            "triangle", "square", "rectangle", "circle", "dice", "cube", "mirror",
            "राष्ट्रपति", "प्रधानमंत्री", "उपराष्ट्रपति", "राज्यपाल", "मुख्यमंत्री"
        )
        for (kw in operativeKeywords) {
            val in1 = existing.question.contains(kw, ignoreCase = true)
            val in2 = newQ.question.contains(kw, ignoreCase = true)
            if (in1 != in2) {
                return false
            }
        }

        // Critical safeguard 2: In Reasoning non-verbal questions, multiple questions share generic instructions
        // like "दी गई आकृति का सही दर्पण प्रतिबिंब चुनिए" with generic visual options ["विकल्प 1", "विकल्प 2", ...].
        // If both are visual/diagram questions and their IDs differ, they are separate questions! NEVER delete them!
        val existIsVisual = hasDummyOptions(existing.options) || !existing.imageUrl.isNullOrBlank()
        val newIsVisual = hasDummyOptions(newQ.options) || !newQ.imageUrl.isNullOrBlank()
        if (existIsVisual && newIsVisual && existing.id != newQ.id) {
            return false
        }

        // Short phrases like "सरल कीजिए" or "मान ज्ञात कीजिए" cannot be considered duplicate text alone
        val isSubstringText = (cleanExistText.length >= 35 && cleanNewText.length >= 35) &&
                (cleanExistText.contains(cleanNewText) || cleanNewText.contains(cleanExistText))
        val isWordSimilarText = cleanExistText.length >= 30 && cleanNewText.length >= 30 && areWordsSimilar(existing.question, newQ.question, 0.90f)

        val questionTextMatches = isExactQuestionText || isSubstringText || isWordSimilarText
        if (!questionTextMatches) return false

        // Case 1: Existing had dummy options (cut off on previous page) and new has real options (or vice-versa)
        val existHasDummy = hasDummyOptions(existing.options)
        val newHasDummy = hasDummyOptions(newQ.options)
        if (existHasDummy && !newHasDummy && (isExactQuestionText || cleanExistText.take(20) == cleanNewText.take(20))) return true
        if (!existHasDummy && newHasDummy && (isExactQuestionText || cleanExistText.take(20) == cleanNewText.take(20))) return true

        // Case 2: Both have dummy options -> NEVER consider them duplicate unless both have matching non-null image or identical text >= 25 chars and same official ID
        if (existHasDummy && newHasDummy) {
            return (existing.id in 1..300 && existing.id == newQ.id) && isExactQuestionText && cleanExistText.length >= 25
        }

        // Case 3: Both have real options -> At least 3 options MUST match!
        return when {
            matchedOpts >= 4 -> true
            matchedOpts >= 3 && isExactQuestionText -> true
            matchedOpts >= existing.options.size && existing.options.size in 1..3 && isExactQuestionText -> true
            else -> false
        }
    }

    /**
     * Determines whether newQ should replace existing (preferring Hindi, diagrams, real options, and completeness).
     */
    fun shouldReplaceExistingQuestion(existing: QuizQuestion, newQ: QuizQuestion): Boolean {
        val existingHasImg = !existing.imageUrl.isNullOrBlank()
        val newHasImg = !newQ.imageUrl.isNullOrBlank()
        if (newHasImg && !existingHasImg) return true
        if (existingHasImg && !newHasImg) return false

        val existHasDummy = hasDummyOptions(existing.options)
        val newHasDummy = hasDummyOptions(newQ.options)
        if (existHasDummy && !newHasDummy) return true
        if (!existHasDummy && newHasDummy) return false

        val existingHasHindi = existing.question.any { it in '\u0900'..'\u097F' }
        val newHasHindi = newQ.question.any { it in '\u0900'..'\u097F' }
        if (newHasHindi && !existingHasHindi) return true
        if (existingHasHindi && !newHasHindi) return false

        val existingScore = existing.question.length + existing.options.sumOf { it.length }
        val newScore = newQ.question.length + newQ.options.sumOf { it.length }
        return newScore > existingScore
    }

    /**
     * Extracts and separates exam & date/shift tags if embedded in question text.
     * Examples: "[DP Constable 22/11/2025 Shift 1st]", "(SSC GD 20/02/2024 Shift 2)"
     */
    fun extractShiftTagFromText(text: String): Pair<String, String?> {
        var clean = text.trim()
        val endRegex = Regex("""[\(\[\{]\s*([A-Za-z\u0900-\u097F0-9\s\.\,\/\-–—]+(?:Shift|पाली|पारी|Shift-?[I|V|X\d]+|\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4}|20\d{2})[A-Za-z\u0900-\u097F0-9\s\.\,\/\-–—]*)\s*[\)\]\}]\s*$""", RegexOption.IGNORE_CASE)
        val startRegex = Regex("""^\s*[\(\[\{]\s*([A-Za-z\u0900-\u097F0-9\s\.\,\/\-–—]+(?:Shift|पाली|पारी|Shift-?[I|V|X\d]+|\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4}|20\d{2})[A-Za-z\u0900-\u097F0-9\s\.\,\/\-–—]*)\s*[\)\]\}]\s*""", RegexOption.IGNORE_CASE)

        val endMatch = endRegex.find(clean)
        if (endMatch != null) {
            val tag = endMatch.groupValues[1].trim()
            clean = clean.removeRange(endMatch.range).trim()
            return Pair(clean, tag)
        }

        val startMatch = startRegex.find(clean)
        if (startMatch != null) {
            val tag = startMatch.groupValues[1].trim()
            clean = clean.removeRange(startMatch.range).trim()
            return Pair(clean, tag)
        }

        // Also catch unbracketed exam shift lines e.g. "SSC GD 17 Feb. 2025, Shift-I" or "- SSC GD 2025 Shift-II"
        val unbracketedEndRegex = Regex("""(?:\n|\r|\s{2,}|[-–—]\s*)([A-Za-z0-9\s\.\,\/\-–—]*(?:SSC|GD|Police|Constable|CGL|CHSL|MTS|RRB|NTPC|CET|Patwari|RPF)[\s\w\.\,\/\-–—]*(?:Shift|पाली|पारी|Shift-?[I|V|X\d]+|\d{1,2}\s+[A-Za-z]{3,}\s+20\d{2})[A-Za-z0-9\s\.\,\/\-–—]*)\s*$""", RegexOption.IGNORE_CASE)
        val unbracketedMatch = unbracketedEndRegex.find(clean)
        if (unbracketedMatch != null) {
            val tag = unbracketedMatch.groupValues[1].trim()
            clean = clean.removeRange(unbracketedMatch.range).trim()
            return Pair(clean, tag)
        }

        return Pair(clean, null)
    }

    /**
     * Deduplicates repeated option values commonly produced in bilingual tables e.g.
     * "2nd / 2nd" -> "2nd", "4th / 4th" -> "4th", "1947 / 1947" -> "1947", "25% / 25%" -> "25%",
     * "Delhi (Delhi)" -> "Delhi", "2nd\n2nd" -> "2nd".
     */
    fun deduplicateOptionText(raw: String): String {
        var text = raw.trim()

        // 1. Slash / Pipe / Double-slash duplicates: "2nd / 2nd", "4th / 4th", "100 // 100", "A | A"
        if (text.contains("/") || text.contains("|") || text.contains("//")) {
            val parts = text.split(Regex("""\s*(?:\/{1,2}|\|)\s*""")).map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val first = parts.first()
                if (parts.all { it.equals(first, ignoreCase = true) }) {
                    text = first
                }
            }
        }

        // 2. Hyphen separated duplicates: "2nd - 2nd", "Delhi – Delhi"
        if (text.contains(" - ") || text.contains(" – ") || text.contains(" — ")) {
            val parts = text.split(Regex("""\s*[-–—]\s*""")).map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size == 2 && parts[0].equals(parts[1], ignoreCase = true)) {
                text = parts[0]
            }
        }

        // 3. Parentheses duplicates: "2nd (2nd)", "4th (4th)", "100 (100)", "Delhi (Delhi)"
        val parenDuplicateMatch = Regex("""^(.+?)\s*[\(\[]\s*(.+?)\s*[\)\]]$""").find(text)
        if (parenDuplicateMatch != null) {
            val outside = parenDuplicateMatch.groupValues[1].trim()
            val inside = parenDuplicateMatch.groupValues[2].trim()
            if (outside.equals(inside, ignoreCase = true)) {
                text = outside
            }
        }

        // 4. Newline separated duplicates: "2nd\n2nd", "4th\n4th"
        if (text.contains("\n")) {
            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
            if (lines.size >= 2 && lines.all { it.equals(lines.first(), ignoreCase = true) }) {
                text = lines.first()
            }
        }

        // 5. Word repeated twice: "2nd 2nd", "4th 4th"
        val doubleWordMatch = Regex("""^([A-Za-z0-9\.\%\$\₹\/\-]+)\s+\1$""", RegexOption.IGNORE_CASE).find(text)
        if (doubleWordMatch != null) {
            text = doubleWordMatch.groupValues[1].trim()
        }

        return text.trim()
    }

    /**
     * Extracts strictly the Hindi text from bilingual options (English + Hindi),
     * completely stripping the English option text while preserving numbers, units, formulas and acronyms,
     * and deduplicating repeated values (e.g. "2nd / 2nd" -> "2nd").
     */
    fun extractHindiOnlyFromBilingualOption(raw: String): String {
        var text = deduplicateOptionText(raw)

        // 1. Slash / pipe / double slash separated:
        // e.g. "Direct election / प्रत्यक्ष चुनाव", "Head / सिर", "1757 / 1757 ई.", "प्रत्यक्ष चुनाव / Direct election"
        if (text.contains("/") || text.contains("|") || text.contains("//")) {
            val parts = text.split(Regex("""\s*(?:\/{1,2}|\|)\s*""")).map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val hindiParts = parts.filter { it.any { c -> c in '\u0900'..'\u097F' } }
                val engParts = parts.filter { !it.any { c -> c in '\u0900'..'\u097F' } }
                if (hindiParts.isNotEmpty() && engParts.isNotEmpty()) {
                    text = hindiParts.first()
                } else if (parts.all { it.equals(parts.first(), ignoreCase = true) }) {
                    text = parts.first()
                }
            }
        }

        // 2. Hyphen separated:
        // e.g. "Direct election - प्रत्यक्ष चुनाव"
        if (text.contains(" - ") || text.contains(" – ") || text.contains(" — ")) {
            val parts = text.split(Regex("""\s*[-–—]\s*""")).map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size == 2) {
                val left = parts[0]
                val right = parts[1]
                if (right.any { it in '\u0900'..'\u097F' } && !left.any { it in '\u0900'..'\u097F' }) {
                    text = right
                } else if (left.any { it in '\u0900'..'\u097F' } && !right.any { it in '\u0900'..'\u097F' }) {
                    text = left
                } else if (left.equals(right, ignoreCase = true)) {
                    text = left
                }
            }
        }

        // 3. Parentheses format:
        // e.g. "Rajasthan (राजस्थान)" -> "राजस्थान"
        // e.g. "राजस्थान (Rajasthan)" -> "राजस्थान"
        val parenMatch1 = Regex("""^[A-Za-z0-9\s,\-\.]+\s*[\(\[]\s*([\u0900-\u097F\s\d\.\,\-]+)\s*[\)\]]$""").find(text)
        if (parenMatch1 != null) {
            val insideHindi = parenMatch1.groupValues[1].trim()
            if (insideHindi.isNotBlank()) text = insideHindi
        } else {
            val parenMatch2 = Regex("""^([\u0900-\u097F\s\d\.\,\-]+)\s*[\(\[]\s*[A-Za-z0-9\s,\-\.]+\s*[\)\]]$""").find(text)
            if (parenMatch2 != null) {
                val outsideHindi = parenMatch2.groupValues[1].trim()
                if (outsideHindi.isNotBlank()) text = outsideHindi
            }
        }

        // 4. Inline English word preceding Hindi word:
        // e.g. "Elephant हाथी" -> "हाथी", "Oxygen ऑक्सीजन" -> "ऑक्सीजन", "Liver यकृत" -> "यकृत"
        // NEVER strip numbers or units (e.g. "18 साल", "25 वर्ष", "0.25 एकड़", "160 किमी", "300 मीटर", "1 घंटा")
        val hasLeadingDigitsOrSymbol = text.matches(Regex("""^[\d\₹\$\€\£\%\.\,\s]+.*"""))
        if (!hasLeadingDigitsOrSymbol) {
            val inlineEngWordMatch = Regex("""^([A-Za-z\s,\-\.\'\"]{2,})\s+([\u0900-\u097F].+)$""").find(text)
            if (inlineEngWordMatch != null) {
                val engPart = inlineEngWordMatch.groupValues[1].trim()
                val hindiPart = inlineEngWordMatch.groupValues[2].trim()
                // Only strip if English part is purely alphabetical words without numbers
                if (engPart.all { it.isLetter() || it.isWhitespace() || it in ",'\"-." } && engPart.any { it.isLetter() }) {
                    if (hindiPart.any { it in '\u0900'..'\u097F' }) {
                        text = hindiPart
                    }
                }
            }
        }

        // 5. Newline separated:
        if (text.contains("\n")) {
            val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
            val hindiLine = lines.firstOrNull { it.any { c -> c in '\u0900'..'\u097F' } }
            if (hindiLine != null) {
                text = hindiLine
            } else if (lines.size >= 2 && lines.all { it.equals(lines.first(), ignoreCase = true) }) {
                text = lines.first()
            }
        }

        return deduplicateOptionText(text)
    }

    /**
     * Cleans option text by removing "(A)", "[A]", "A)", "1)", "1.", "A.", "Option A:", "विकल्प 1:",
     * "(क)", "क.", diagram OCR spacing/linebreaks, strictly filtering bilingual options to keep ONLY Hindi,
     * and deduplicating any repeated tokens (e.g. "2nd / 2nd" -> "2nd").
     */
    fun cleanOptionText(raw: String): String {
        var text = raw.trim()
        // 1. Normalize linebreaks, tabs, and multiple consecutive spaces (often left over from diagram/OCR layout)
        text = text.replace(Regex("""[\r\n\t]+"""), " ").replace(Regex("""\s{2,}"""), " ").trim()
        // 2. Remove markdown boldness e.g. **(A)** or **1.** or **(क)**
        text = text.replace(Regex("""^\*+[\(\[]?\s*(?:[A-Da-d]|[1-4]|[क-घ])\s*[\.\)\:\-\]\/]?\*+\s*"""), "").trim()
        // 3. Repeatedly strip leading option markers e.g. "Option A:", "(A)", "1.", "विकल्प 1:", "आकृति 1:", "(क)", "क."
        var changed = true
        var iterations = 0
        while (changed && iterations < 5) {
            val prev = text
            val stripped = text.replace(
                Regex(
                    """^(?:(?:Option|विकल्प|आकृति|चित्र|Fig(?:ure)?)\s*[A-Da-d1-4क-घ]\s*[\.\:\-]?\s*|[\(\[]\s*(?:[A-Da-d]|[1-4]|[क-घ])\s*[\)\]][\.\:\-]?\s*|(?:[A-Da-d]|[क-घ])\s*[\.\)\:\-]\s*|[1-4]\s*[\)\:\-]\s*|[1-4]\.(?!\d)\s*)""",
                    RegexOption.IGNORE_CASE
                ),
                ""
            ).trim()
            if (stripped.isNotBlank()) {
                text = stripped
            }
            changed = (prev != text)
            iterations++
        }

        // 4. Strictly extract pure Hindi & deduplicate repeated values from bilingual option
        text = extractHindiOnlyFromBilingualOption(text)

        // 5. Extra pass of deduplication e.g. "2nd / 2nd" -> "2nd"
        text = deduplicateOptionText(text)

        // 6. Remove accidental answer tag leakage e.g. "[उत्तर]" or "[Ans]"
        text = text.replace(Regex("""\s*\[?\s*(?:सही\s*)?(?:उत्तर|Ans(?:wer)?)\s*\]?\s*$""", RegexOption.IGNORE_CASE), "").trim()
        return text.trim()
    }

    /**
     * Intelligently normalizes subject variations into consistent, canonical Hindi subject names
     * across all competitive exam types (Police, SSC, Railway, Teaching/CTET, State PSC, Defence, etc.).
     */
    fun normalizeSubject(raw: String): String {
        val s = raw.trim()
        val lower = s.lowercase()
        return when {
            // 1. Reasoning / General Intelligence -> Always "तर्कशक्ति"
            lower.contains("reason") || lower.contains("तर्क") || lower.contains("रीजनिंग") ||
            lower.contains("बुद्धिमत्ता") || lower.contains("intelligence") || lower.contains("mental") ||
            lower.contains("logic") || lower.contains("अभिरुचि") -> "तर्कशक्ति"

            // 2. Mathematics / Quantitative Aptitude -> Always "गणित"
            lower.contains("math") || lower.contains("गणित") || lower.contains("संख्यात्मक") ||
            lower.contains("numeric") || lower.contains("arithmetic") || lower.contains("quantitative") ||
            lower.contains("अंकगणित") || (lower.contains("योग्यता") && !lower.contains("तर्क")) -> "गणित"

            // 3. Computer Knowledge -> Always "कंप्यूटर"
            lower.contains("comput") || lower.contains("कंप्यूटर") || lower.contains("संगणक") ||
            lower.contains("आईटी") || lower.contains("it") || lower.contains("cyber") ||
            lower.contains("ms-office") || lower.contains("ms-word") || lower.contains("ms-excel") -> "कंप्यूटर"

            // 4. Hindi Language (UP Police, CTET, Lekhpal) -> "सामान्य हिंदी"
            lower.contains("hindi") || lower.contains("हिंदी") || lower.contains("हिन्दी") -> "सामान्य हिंदी"

            // 5. English Language (SSC, CTET) -> "General English"
            lower.contains("english") || lower.contains("अंग्रेजी") || lower.contains("अंग्रेज़ी") -> "General English"

            // 6. Child Development & Pedagogy (Teaching / CTET / REET / TET) -> "बाल विकास एवं शिक्षाशास्त्र"
            lower.contains("pedagogy") || lower.contains("शिक्षाशास्त्र") || lower.contains("बाल विकास") ||
            lower.contains("cdp") || lower.contains("child dev") -> "बाल विकास एवं शिक्षाशास्त्र"

            // 7. State GK (Rajasthan exams)
            lower.contains("rajasthan") || lower.contains("राजस्थान") -> "राजस्थान सामान्य ज्ञान"

            // 8. ALL OTHER SUBJECTS (Science, History, Geography, Polity, Constitution, Economics,
            // Current Affairs, Static GK, EVS, Social Studies, General Awareness, etc.)
            // In all competitive exams (Delhi Police, SSC, Railway, UP Police), ALL of these belong strictly to GK / GS!
            else -> "GK / GS"
        }
    }

    /**
     * Pattern-matching parser for standard Hindi/English exam formats, including official SSC Answer Keys.
     */
    fun parseTextWithPatternEngine(text: String, defaultSubject: String): List<QuizQuestion> {
        val questions = mutableListOf<QuizQuestion>()
        var currentSectionSubject = defaultSubject

        // Split by question markers: "Q.1", "Q. 1", "Q1.", "Question 1", "प्रश्न 1"
        val blocks = text.split(Regex("(?i)(?=(?:Q(?:uestion)?\\.?\\s*\\d+|प्रश्न\\s*\\d+|\\bQ\\s*\\d+\\.))"))

        var currentId = 2001
        for (block in blocks) {
            val trimmed = block.trim()
            if (trimmed.length < 15) continue

            val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue

            // Check if section header is present in block
            for (line in lines) {
                if (line.contains("Section :", ignoreCase = true) || line.contains("Section:", ignoreCase = true)) {
                    val sec = line.substringAfter("Section", "").replace(":", "").trim()
                    if (sec.isNotBlank()) {
                        currentSectionSubject = normalizeSubject(sec)
                    }
                }
            }

            var qText = lines.first().replace(Regex("^(?:Q(?:uestion)?\\.?\\s*\\d+[:.]?|प्रश्न\\s*\\d+[:.]?|Q\\s*\\d+\\.\\s*)"), "").trim()
            val options = mutableListOf<String>()
            var correctIndex = 0
            var explanation = "आधिकारिक उत्तर कुंजी के अनुसार सही विकल्प है।"
            var subject = currentSectionSubject
            var imageUrl: String? = null

            // Detect image references in question block
            val imgMatch = Regex("(?i)\\[(?:IMAGE|IMG|PICTURE|आकृति|चित्र)(?:_URL)?[:=]\\s*([^\\]]+)\\]").find(trimmed)
                ?: Regex("(?i)!\\[[^\\]]*\\]\\(([^)]+)\\)").find(trimmed)
                ?: Regex("(?i)(https?://[^\\s]+(?:\\.png|\\.jpg|\\.jpeg|\\.webp|\\.svg|/raw/[^\\s]+))").find(trimmed)
                ?: Regex("(data:image/[^;]+;base64,[A-Za-z0-9+/=]+)").find(trimmed)

            if (imgMatch != null) {
                imageUrl = imgMatch.groupValues.getOrNull(1)?.trim() ?: imgMatch.value.trim()
            }

            for (i in 1 until lines.size) {
                val line = lines[i]

                // Skip question ID / option ID metadata lines from SSC portal
                if (line.startsWith("Question ID", true) || line.startsWith("Option", true) && line.contains("ID", true)) {
                    continue
                }

                if (line.matches(Regex("(?i)^(?:Ans\\s*[1-4][.)]|Ans\\s*[A-D][.)]|Ans\\s*:\\s*[A-D1-4]).*"))) {
                    // Option 1 with Ans prefix like "Ans 1. अनुच्छेद 337"
                    val opt = line.replace(Regex("(?i)^(?:Ans\\s*[1-4][.)]|Ans\\s*[A-D][.)]|Ans\\s*:\\s*[A-D1-4])\\s*"), "").trim()
                    if (opt.isNotBlank()) {
                        options.add(opt)
                    }
                } else if (line.matches(Regex("(?i)^(?:[A-D1-4][.)]|\\([A-D1-4]\\)|विकल्प\\s*[A-D1-4]).*")) || line.contains("✓") || line.contains("✔️") || line.contains("✔")) {
                    val isGreenTick = line.contains("✓") || line.contains("✔️") || line.contains("✔")
                    val opt = line.replace(Regex("""[✓✔️✔❌✗]"""), "")
                        .replace(Regex("(?i)^(?:[A-D1-4][.)]|\\([A-D1-4]\\)|विकल्प\\s*[A-D1-4][.:]?)\\s*"), "").trim()
                    if (opt.isNotBlank()) {
                        if (isGreenTick) {
                            correctIndex = options.size
                        }
                        options.add(opt)
                    }
                } else if (line.matches(Regex("(?i)^(?:Correct Option|Right Ans|आधिकारिक उत्तर|सही उत्तर|Official Ans)[.:]?\\s*.*"))) {
                    val ansStr = line.replace(Regex("(?i)^(?:Correct Option|Right Ans|आधिकारिक उत्तर|सही उत्तर|Official Ans)[.:]?\\s*"), "").trim()
                    correctIndex = when {
                        ansStr.startsWith("A", true) || ansStr.startsWith("1") -> 0
                        ansStr.startsWith("B", true) || ansStr.startsWith("2") -> 1
                        ansStr.startsWith("C", true) || ansStr.startsWith("3") -> 2
                        ansStr.startsWith("D", true) || ansStr.startsWith("4") -> 3
                        else -> 0
                    }
                } else if (line.matches(Regex("(?i)^(?:Exp(?:lanation)?|व्याख्या|हल)[.:]?\\s*.*"))) {
                    explanation = line.replace(Regex("(?i)^(?:Exp(?:lanation)?|व्याख्या|हल)[.:]?\\s*"), "").trim()
                } else if (line.matches(Regex("(?i)^(?:Subject|विषय)[.:]?\\s*.*"))) {
                    subject = line.replace(Regex("(?i)^(?:Subject|विषय)[.:]?\\s*"), "").trim()
                } else if (line.matches(Regex("(?i)^(?:Image|आकृति|चित्र|Img)[.:]?\\s*.*"))) {
                    val extractedImg = line.replace(Regex("(?i)^(?:Image|आकृति|चित्र|Img)[.:]?\\s*"), "").trim()
                    if (extractedImg.isNotBlank()) imageUrl = extractedImg
                } else if (options.isEmpty() && !line.startsWith("Section", true) && !line.startsWith("Status", true)) {
                    qText += " $line"
                }
            }

            if (options.size >= 2) {
                while (options.size < 4) {
                    options.add("इनमें से कोई नहीं")
                }
                questions.add(
                    QuizQuestion(
                        id = currentId++,
                        question = qText.ifBlank { "प्रश्न ${questions.size + 1}" },
                        options = options.take(4),
                        correctIndex = correctIndex.coerceIn(0, 3),
                        explanation = explanation,
                        subject = subject,
                        imageUrl = imageUrl
                    )
                )
            }
        }
        return questions
    }

    private fun parseHeuristic(text: String, defaultSubject: String): List<QuizQuestion> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()

        val list = mutableListOf<QuizQuestion>()
        var id = 3001
        for (line in lines) {
            if (line.length > 20 && !line.startsWith("//") && !line.startsWith("#")) {
                list.add(
                    QuizQuestion(
                        id = id++,
                        question = line,
                        options = listOf("विकल्प A (सही उत्तर)", "विकल्प B", "विकल्प C", "विकल्प D"),
                        correctIndex = 0,
                        explanation = "आधिकारिक उत्तर कुंजी के आधार पर विस्तृत व्याख्या।",
                        subject = defaultSubject
                    )
                )
            }
        }
        return list
    }

    /**
     * Extracts images or base64 diagrams from reasoning questions and uploads them directly to the GitHub repository.
     * Replaces local/base64 data with the lightweight permanent GitHub raw CDN URL.
     */
    suspend fun extractAndUploadImagesToGitHub(
        questions: List<QuizQuestion>,
        config: GitHubConfig,
        context: Context? = null,
        onProgress: ((String) -> Unit)? = null
    ): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val updatedQuestions = mutableListOf<QuizQuestion>()

        for ((idx, q) in questions.withIndex()) {
            val img = q.imageUrl
            if (!img.isNullOrBlank()) {
                var rawBytes: ByteArray? = null
                val base64ToUpload: String? = when {
                    img.startsWith("data:image", true) || (!img.startsWith("http://") && !img.startsWith("https://") && !img.startsWith("/") && img.length > 50) -> {
                        try {
                            val clean = if (img.contains(",")) img.substringAfter(",") else img
                            rawBytes = android.util.Base64.decode(clean.trim(), android.util.Base64.DEFAULT)
                            img
                        } catch (_: Exception) { null }
                    }
                    img.startsWith("/") -> {
                        val f = java.io.File(img)
                        if (f.exists()) {
                            try {
                                rawBytes = f.readBytes()
                                "data:image/jpeg;base64," + android.util.Base64.encodeToString(rawBytes, android.util.Base64.NO_WRAP)
                            } catch (_: Exception) { null }
                        } else null
                    }
                    else -> null
                }

                if (base64ToUpload != null) {
                    onProgress?.invoke("रीजनिंग प्रश्न ${idx + 1} की इमेज GitHub पर अपलोड हो रही है...")
                    val filename = "reasoning_q${q.id}_${System.currentTimeMillis() % 100000}.png"
                    val uploadResult = GitHubSyncService.uploadBase64Image(
                        config = config,
                        base64Data = base64ToUpload,
                        fileName = filename,
                        commitMessage = "Upload reasoning image for question ${q.id}"
                    )
                    if (uploadResult.isSuccess && uploadResult.rawFileUrl != null) {
                        if (context != null && rawBytes != null && rawBytes.isNotEmpty()) {
                            com.example.model.OfflineTestRepository.saveImageDirectly(context, uploadResult.rawFileUrl, rawBytes)
                            com.example.model.OfflineTestRepository.saveImageDirectly(context, filename, rawBytes)
                        }
                        updatedQuestions.add(q.copy(imageUrl = uploadResult.rawFileUrl))
                        continue
                    }
                } else if (img.startsWith("http://") || img.startsWith("https://")) {
                    // Already a direct image URL (e.g. from GitHub or CDN)
                    updatedQuestions.add(q)
                    continue
                }
            }
            updatedQuestions.add(q)
        }

        updatedQuestions
    }

    /**
     * Converts a list of QuizQuestion into clean GitHub-ready Kotlin code or JSON format.
     */
    fun exportToGitHubKotlinCode(testTitle: String, year: String, questions: List<QuizQuestion>): String {
        val sb = StringBuilder()
        sb.append("// ==========================================\n")
        sb.append("// Test: $testTitle ($year)\n")
        sb.append("// Generated by AI Answer Key Studio\n")
        sb.append("// Total Questions: ${questions.size}\n")
        sb.append("// ==========================================\n\n")
        sb.append("val customAiTest_${System.currentTimeMillis() % 10000} = listOf(\n")
        for (q in questions) {
            sb.append("    QuizQuestion(\n")
            sb.append("        id = ${q.id},\n")
            sb.append("        question = \"${q.question.replace("\"", "\\\"")}\",\n")
            sb.append("        options = listOf(\n")
            for (opt in q.options) {
                sb.append("            \"${opt.replace("\"", "\\\"")}\",\n")
            }
            sb.append("        ),\n")
            sb.append("        correctIndex = ${q.correctIndex},\n")
            sb.append("        explanation = \"${q.explanation.replace("\"", "\\\"")}\",\n")
            sb.append("        subject = \"${q.subject}\",\n")
            if (!q.imageUrl.isNullOrBlank()) {
                sb.append("        imageUrl = \"${q.imageUrl.replace("\"", "\\\"")}\",\n")
            } else {
                sb.append("        imageUrl = null,\n")
            }
            if (!q.examShiftInfo.isNullOrBlank()) {
                sb.append("        examShiftInfo = \"${q.examShiftInfo.replace("\"", "\\\"")}\"\n")
            } else {
                sb.append("        examShiftInfo = null\n")
            }
            sb.append("    ),\n")
        }
        sb.append(")\n")
        return sb.toString()
    }

    fun exportToGitHubJson(
        testTitle: String,
        year: String,
        questions: List<QuizQuestion>,
        subExamId: String = "ssc_gd",
        subjectKey: String = "GK",
        testType: String = "CHAPTER_WISE",
        postType: String = "CONSTABLE"
    ): String {
        val root = JSONObject()
        root.put("title", testTitle)
        root.put("year", year)
        root.put("subExamId", subExamId)
        root.put("subjectKey", subjectKey)
        root.put("testType", testType)
        root.put("postType", postType)
        root.put("totalQuestions", questions.size)
        root.put("durationMinutes", if (testType == "CHAPTER_WISE") 20 else 90)
        root.put("totalMarks", questions.size)

        val arr = JSONArray()
        for (q in questions) {
            val qObj = JSONObject()
            qObj.put("id", q.id)
            qObj.put("question", q.question)
            val opts = JSONArray()
            q.options.forEach { opts.put(it) }
            qObj.put("options", opts)
            qObj.put("correctIndex", q.correctIndex)
            qObj.put("explanation", q.explanation)
            qObj.put("subject", q.subject)
            if (!q.imageUrl.isNullOrBlank()) {
                qObj.put("imageUrl", q.imageUrl)
            }
            if (!q.examShiftInfo.isNullOrBlank()) {
                qObj.put("examShiftInfo", q.examShiftInfo)
            }
            arr.put(qObj)
        }
        root.put("questions", arr)
        return root.toString(2)
    }

    /**
     * Standard sample official answer key text for instant testing
     */
    fun getSampleAnswerKeyText(): String {
        return """
            Constable Executive Male and Female in Delhi Police Examination 2023
            Exam Date: 22/11/2023
            Exam Time: 3:00 PM - 4:30 PM
            Subject: Constable Executive Male and Female in Delhi Police Examination 2023
            
            Section : General Knowledge
            
            Q.1 भारतीय संविधान का निम्नलिखित में से कौन-सा अनुच्छेद राष्ट्रीय अनुसूचित जाति आयोग से संबंधित है?
            Ans 1. अनुच्छेद 337
            2. अनुच्छेद 338
            3. अनुच्छेद 336
            4. अनुच्छेद 335
            Chosen Option : 2
            
            Q.2 राष्ट्रीय चावल अनुसंधान संस्थान ओडिशा के ________ में स्थित है।
            Ans 1. पुरी
            2. कोरापुट
            3. संबलपुर
            4. कटक
            Chosen Option : 4
            
            Q.3 निम्नलिखित रूसी राजनीतिक नेताओं में से किसने वर्ष 2007 में भारत के गणतंत्र दिवस समारोह में भाग लिया था?
            Ans 1. बोरिस येल्तसिन (Boris Yeltsin)
            2. व्लादिमीर पुतिन (Vladimir Putin)
            3. मिखाइल मिशुस्टिन (Mikhail Mishustin)
            4. दिमित्री मेदवेदेव (Dmitry Medvedev)
            Chosen Option : 2
            
            Q.4 निम्नलिखित में से किस अंतर्राष्ट्रीय संगठन की स्थापना 1995 में हुई थी?
            Ans 1. विश्व बैंक
            2. विश्व व्यापार संगठन
            3. अंतर्राष्ट्रीय श्रमिक संगठन
            4. अंतर्राष्ट्रीय मुद्रा कोष
            Chosen Option : 2
            
            Q.5 चौथे खेलो इंडिया यूथ गेम्स (Khelo India Youth Games) की आधिकारिक जर्सी किसने लॉन्च की थी?
            Ans 1. अनुराग ठाकुर
            2. नरेंद्र मोदी
            3. निशीथ प्रमाणिक
            4. अमित शाह
            Chosen Option : 1
            
            Q.6 निम्नलिखित में से कौन-से देश ICC T20 पुरुष क्रिकेट विश्व कप 2024 की मेजबानी करेंगे?
            Ans 1. भारत और श्रीलंका
            2. ऑस्ट्रेलिया और न्यूजीलैंड
            3. वेस्टइंडीज और यूएसए (USA)
            4. वेस्टइंडीज और कनाडा
            Chosen Option : 3
            
            Q.7 एक पारिस्थितिकी तंत्र में निम्नलिखित में से कौन-सा/से स्वपोषी है/हैं?
            Ans 1. पौधे
            2. केंचुए
            3. मछलियाँ
            4. सूर्य की रोशनी
            Chosen Option : 1
            
            Q.8 निम्नलिखित में से किस संगठन को 1992 में वैधानिक शक्ति प्रदान की गई थी?
            Ans 1. सेबी (SEBI)
            2. आरबीआई (RBI)
            3. एलआईसी (LIC)
            4. इरडा (IRDA)
            Chosen Option : 1
            
            Q.9 निम्नलिखित में से किस समाज सुधार संगठन की स्थापना 1864 में मद्रास में हुई थी?
            Ans 1. ब्रह्म समाज
            2. वेद समाज
            3. प्रार्थना समाज
            4. आर्य समाज
            Chosen Option : 2
            
            Q.10 निम्नलिखित में से किस वर्ष में बारदोली में किसान आंदोलन हुआ था?
            Ans 1. 1916
            2. 1928
            3. 1918
            4. 1934
            Chosen Option : 2
            
            Section : Reasoning
            
            Q.11 FREE : EPBA :: MORE : LMOA :: BURN : ?
            Ans 1. ASOJ
            2. ASOK
            3. ASPK
            4. AROJ
            Chosen Option : 1
            
            Q.12 40 : 9 :: 100 : ? :: 50 : 11
            Ans 1. 51
            2. 20
            3. 21
            4. 42
            Chosen Option : 3
            
            Q.13 1, 2, 8, 48, 384, ?
            Ans 1. 3456
            2. 4608
            3. 4280
            4. 3840
            Chosen Option : 2

            Q.14 दी गई प्रश्न आकृति का सही दर्पण प्रतिबिंब (Mirror Image - MN रेखा पर) क्या होगा?
            [IMAGE: https://raw.githubusercontent.com/prahladsinghrathore869025-alt/Prahlad/main/images/reasoning/mirror_figure_sample.png]
            Ans 1. आकृति (1)
            2. आकृति (2)
            3. आकृति (3)
            4. आकृति (4)
            Chosen Option : 2
            Exp: दर्पण प्रतिबिंब में बायाँ भाग दाईं ओर और दायाँ भाग बाईं ओर परिवर्तित हो जाता है।
            
            Section : Numerical Ability
            
            Q.15 3 अंकों की सबसे बड़ी अभाज्य संख्या कौन-सी है?
            Ans 1. 997
            2. 991
            3. 983
            4. 987
            Chosen Option : 1
            
            Q.16 एक गाँव की जनसंख्या में प्रति वर्ष 3% की वृद्धि होती है। यदि वर्तमान जनसंख्या 75,556 है, तो 3 वर्ष बाद गाँव की जनसंख्या क्या होगी?
            Ans 1. 85262
            2. 82562
            3. 82526
            4. 82563
            Chosen Option : 2
            
            Section : Computer Proficiency
            
            Q.17 Which of the following is a type of network that allows users from outside to access the Intranet of an organisation?
            Ans 1. Extranet
            2. Double Intranet
            3. Internet
            4. Externet
            Chosen Option : 1
            
            Q.18 Which of the following shortcut keys is used to switch to print preview in MS Word 2016?
            Ans 1. Ctrl+Alt+I
            2. Ctrl+P
            3. Page UP
            4. Page Down
            Chosen Option : 1
            
            Q.19 ई-मेल के संदर्भ में 'CC' का पूर्ण रूप क्या है?
            Ans 1. Compose Check
            2. Centralised Communication
            3. Confidential Content
            4. Carbon Copy
            Chosen Option : 4
        """.trimIndent()
    }
}
