package com.example.model

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Dynamic Test Repository:
 * Holds all dynamic, online-synced, and Admin-created mock test papers.
 * Automatically persists to local disk (JSON) so tests remain available offline once fetched,
 * and automatically loads new tests dynamically whenever the user is online without requiring app updates!
 */
object DynamicTestRepository {
    private const val TAG = "DynamicTestRepo"
    private const val FILE_NAME = "dynamic_cloud_tests.json"

    // Precompiled Static Regex Patterns (eliminates GC pauses & CPU spikes)
    private val SET_ID_REGEX = Regex(""".*_s\d+.*""")
    private val FOREIGN_EXAM_PREFIX_REGEX = Regex("""^(?:dyn_)?(?:ssc_gd|ssc_delhi_police|up_police|rajasthan_police|rpf_si|rrb_ntpc|rrb_group_d|rrb_alp|rrb_je|ssc_cgl|ssc_chsl|ssc_cpo|ssc_mts|patwari|lekhpal|rajasthan_cet)_""")
    private val WHITESPACE_REGEX = Regex("""\s+""")

    /**
     * Determines whether a test paper is a chapter-wise set or practice set,
     * ensuring it is NEVER mistakenly displayed in Full Mock Tests.
     */
    fun isChapterWiseTest(testType: String, title: String, id: String): Boolean {
        val tType = testType.trim().uppercase()
        val tLower = title.trim().lowercase()
        val idLower = id.trim().lowercase()

        // 1. Explicit test type check
        if (tType == "CHAPTER_WISE" || tType == "CHAPTER" || tType.contains("CHAP")) {
            return true
        }

        // 2. ID check for chapter prefixes / markers
        if (idLower.startsWith("custom_chap_") ||
            idLower.contains("_chap_") ||
            idLower.contains("_chapter_") ||
            idLower.contains("_set_") ||
            id.matches(SET_ID_REGEX)) {
            return true
        }

        // 3. Keyword checks in Title
        if (tLower.contains("chapter") || tLower.contains("अध्याय") ||
            tLower.contains("topic") || tLower.contains("टॉपिक")) {
            return true
        }

        // 4. Set indicators (e.g., "Set 1", "Set-1", "सेट 1", "Practice Set", "अभ्यास सेट")
        val isExplicitFull = tLower.contains("full test") || tLower.contains("फुल टेस्ट") ||
                tLower.contains("full mock") || tLower.contains("फुल मॉक") ||
                tLower.contains("mock test") || tLower.contains("मॉक टेस्ट") ||
                tLower.contains("shift") || tLower.contains("शिफ्ट") ||
                tLower.contains("pyq paper") || tLower.contains("official paper")

        val hasSetMarker = tLower.contains("practice set") || tLower.contains("प्रैक्टिस सेट") ||
                tLower.contains("अभ्यास सेट") ||
                tLower.matches(Regex(".*\\bset[ -_]?[0-9]+.*")) ||
                tLower.matches(Regex(".*सेट[ -_]?[0-9]+.*")) ||
                tLower.contains("set ") || tLower.contains("सेट ") ||
                tLower.contains(" - set") || tLower.contains(" - सेट")

        if (hasSetMarker && !isExplicitFull) {
            return true
        }

        return false
    }

    val customTests: SnapshotStateList<CustomTestEntry> = mutableStateListOf()
    val deletedTestIds: SnapshotStateList<String> = mutableStateListOf()
    var repositoryRevision by androidx.compose.runtime.mutableIntStateOf(0)
    private var isInitialized = false
    private val examIndexCache = java.util.concurrent.ConcurrentHashMap<String, List<CustomTestEntry>>()

    fun triggerUiRevisionUpdate() {
        val update = {
            repositoryRevision++
        }
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            update()
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                update()
            }
        }
    }

    private fun rebuildExamIndexMap() {
        examIndexCache.clear()
        val grouped = customTests.groupBy { normalizeSubExamId(it.subExamId) }
        examIndexCache.putAll(grouped)
        triggerUiRevisionUpdate()
        try {
            CustomChapterRepository.invalidateFullSubjectsCache()
            RealtimeExamStatsManager.invalidateCache()
        } catch (_: Throwable) {}
    }

    data class CustomTestEntry(
        val categoryId: String,
        val subExamId: String,
        val postType: String = "CONSTABLE", // "CONSTABLE", "HEAD_CONSTABLE", or "GENERAL"
        val year: String = "2025",
        val testType: String = "FULL_TEST", // "FULL_TEST" or "CHAPTER_WISE"
        val testPaper: TestPaper,
        val isLiveDynamic: Boolean = true,
        val createdAt: Long = System.currentTimeMillis()
    )

    fun init(context: Context) {
        synchronized(this) {
            if (isInitialized) return
            loadFromDisk(context)
            purgeCorruptedExamCrossContamination(context)
            isInitialized = true
        }
    }

    /**
     * Adds or updates a dynamic test in memory and optionally persists to disk
     */
    fun addCustomTest(
        categoryId: String,
        subExamId: String,
        postType: String,
        year: String,
        testType: String,
        testPaper: TestPaper,
        context: Context? = null,
        persistToDisk: Boolean = true
    ) {
        // Enforce strict exam ownership based on title and content: never allow tests to be saved under wrong exam!
        val strictExam = getStrictExamOwner(testPaper.title, testPaper.questions, testPaper.id, subExamId)
        val cleanSubExam = strictExam
        val cleanPost = postType.uppercase().trim().ifEmpty { "CONSTABLE" }
        val cleanYear = year.trim().ifEmpty { "2025" }
        val cleanType = testType.uppercase().trim().ifEmpty { "FULL_TEST" }

        // Sanitize difficulty so user-facing card matches standard static papers
        val cleanDiff = if (testPaper.difficulty.contains("AI", ignoreCase = true) ||
            testPaper.difficulty.contains("जेनरेटेड") ||
            testPaper.difficulty.contains("ऑनलाइन लाइव")
        ) {
            "ऑफिशियल पैटर्न"
        } else {
            testPaper.difficulty.ifBlank { "ऑफिशियल पैटर्न" }
        }
        val sanitizedPaper = testPaper.copy(difficulty = cleanDiff)

        // Ensure unique composite ID so tests with same title in different exams/years never collide or overwrite
        val rawId = sanitizedPaper.id.toString().trim()
        val uniquePaperId = if (!rawId.startsWith(cleanSubExam) && !rawId.contains("_" + cleanYear)) {
            "${cleanSubExam}_${cleanYear}_${cleanPost.lowercase()}_$rawId"
        } else {
            rawId
        }
        val finalPaper = if (uniquePaperId != rawId) sanitizedPaper.copy(id = uniquePaperId) else sanitizedPaper

        // Remove from deleted list if re-added
        deletedTestIds.remove(finalPaper.id.toString())
        deletedTestIds.remove(finalPaper.title.trim())
        deletedTestIds.remove(rawId)

        // Remove existing test ONLY if it belongs to the EXACT same exam, year, and ID/Title
        customTests.removeAll {
            it.subExamId == cleanSubExam &&
            it.year == cleanYear &&
            (it.testPaper.id == finalPaper.id || it.testPaper.id == rawId || it.testPaper.title.equals(finalPaper.title.trim(), ignoreCase = true))
        }

        val entry = CustomTestEntry(
            categoryId = categoryId.ifEmpty { "SSC Exams" },
            subExamId = cleanSubExam,
            postType = cleanPost,
            year = cleanYear,
            testType = cleanType,
            testPaper = finalPaper,
            isLiveDynamic = true,
            createdAt = System.currentTimeMillis()
        )

        customTests.add(0, entry)
        rebuildExamIndexMap()
        repositoryRevision++
        CustomChapterRepository.invalidateFullSubjectsCache()
        RealtimeExamStatsManager.invalidateCache()

        if (persistToDisk && context != null) {
            saveToDisk(context)
        }
    }

    /**
     * Deletes a custom test by its ID and saves to disk
     */
    fun deleteCustomTest(testPaperId: String, context: Context? = null): Boolean {
        val cleanId = testPaperId.trim()
        if (cleanId.isNotBlank() && !deletedTestIds.contains(cleanId)) {
            deletedTestIds.add(cleanId)
        }
        val removed = customTests.removeAll { it.testPaper.id.toString() == cleanId || it.testPaper.title.trim() == cleanId }
        rebuildExamIndexMap()
        if (context != null) {
            saveToDisk(context)
            try {
                OfflineTestRepository.purgeAndRefreshOfflineCache(context, listOf(cleanId))
            } catch (e: Exception) {
                Log.e(TAG, "Error purging offline cache on delete: ${e.message}")
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    com.example.service.OnlineTestSyncManager.publishAllToCloud(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Error publishing deletion to GitHub: ${e.message}")
                }
            }
        }
        return removed
    }

    /**
     * Clears all dynamic tests from memory and disk completely.
     */
    fun clearAllTests(context: Context) {
        synchronized(this) {
            customTests.clear()
            deletedTestIds.clear()
            saveToDisk(context)
            try {
                OfflineTestRepository.purgeAllOfflineCache(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error purging offline cache: ${e.message}")
            }
        }
    }

    /**
     * Restores all tests that were previously deleted by clearing the deleted test IDs list and persisting to disk.
     */
    fun restoreAllDeletedTests(context: Context) {
        synchronized(this) {
            deletedTestIds.clear()
            saveToDisk(context)
            rebuildExamIndexMap()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    com.example.service.OnlineTestSyncManager.syncFromCloud(context, isUserTriggered = true)
                    com.example.service.OnlineTestSyncManager.publishAllToCloud(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing/publishing restored tests to GitHub: ${e.message}")
                }
            }
            Log.d(TAG, "Restored all deleted tests. deletedTestIds cleared and cloud sync/publish triggered.")
        }
    }

    /**
     * Completely purges all local test storage on the phone (dynamic_cloud_tests.json and offline_tests).
     */
    fun purgeAllLocalDiskTests(context: Context) {
        synchronized(this) {
            customTests.clear()
            examIndexCache.clear()
            deletedTestIds.clear()
            try {
                val file = File(context.applicationContext.filesDir, FILE_NAME)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting dynamic_cloud_tests.json: ${e.message}")
            }
            try {
                OfflineTestRepository.purgeAllOfflineCache(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error purging offline cache: ${e.message}")
            }
            Log.d(TAG, "Purged all local disk tests successfully.")
        }
    }

    /**
     * Clears all dynamic tests from memory and disk except Delhi Police tests.
     */
    fun clearAllExceptDelhiPolice(context: Context) {
        synchronized(this) {
            customTests.removeAll { entry ->
                val norm = normalizeSubExamId(entry.subExamId)
                norm != "ssc_delhi_police"
            }
            saveToDisk(context)
        }
    }

    /**
     * Determines whether a test paper explicitly belongs to a specific exam based on its title,
     * questions, or test ID.
     */
    fun isSpecificExamTest(examId: String, title: String, questions: List<QuizQuestion> = emptyList(), paperId: String = ""): Boolean {
        val normTarget = normalizeSubExamId(examId)
        val inferred = inferExplicitExamFromContent(title, questions, paperId)
        return inferred != null && inferred == normTarget
    }

    /**
     * Identifies if a test explicitly mentions a specific exam in title, ID, or content.
     * Returns null if it is generic/neutral.
     */
    fun inferExplicitExamFromContent(title: String, questions: List<QuizQuestion> = emptyList(), paperId: String = ""): String? {
        val t = title.lowercase().replace("-", "_").replace(" ", "_")
        val rawT = title.lowercase()
        val idLower = paperId.lowercase().replace("-", "_").replace(" ", "_")
        val shiftLower = questions.firstOrNull { !it.examShiftInfo.isNullOrBlank() }?.examShiftInfo?.lowercase() ?: ""

        // 1. Delhi Police (Constable / Head Constable / Executive / SI)
        if (t.contains("delhi") || rawT.contains("दिल्ली") || shiftLower.contains("delhi") || shiftLower.contains("दिल्ली") || shiftLower.contains("dp ") || idLower.contains("delhi") || idLower.contains("dp_") || t.contains("dp_constable") || t.contains("dp_head") || t.contains("delhipolice") || rawT.contains("dp constable")) {
            return "ssc_delhi_police"
        }

        // 2. UP Police
        if (t.contains("up_police") || t.contains("uppolice") || rawT.contains("up police") || rawT.contains("यूपी पुलिस") ||
            rawT.contains("उत्तर प्रदेश पुलिस") || shiftLower.contains("up police") || shiftLower.contains("यूपी") || idLower.contains("up_police") || idLower.contains("uppolice")) {
            return "up_police"
        }

        // 3. Rajasthan Police
        if (t.contains("rajasthan_police") || t.contains("rajpolice") || rawT.contains("rajasthan police") || rawT.contains("राजस्थान पुलिस") ||
            rawT.contains("राज पुलिस") || shiftLower.contains("rajasthan police") || idLower.contains("rajasthan_police") || idLower.contains("rajpolice")) {
            return "rajasthan_police"
        }

        // 4. RPF SI & Constable
        if (t.contains("rpf") || rawT.contains("rpf si") || rawT.contains("rpf") || rawT.contains("आरपीएफ") ||
            rawT.contains("railway police") || shiftLower.contains("rpf") || idLower.contains("rpf")) {
            return "rpf_si"
        }

        // 5. RRB NTPC
        if (t.contains("ntpc") || rawT.contains("ntpc") || rawT.contains("एनटीपीसी") || shiftLower.contains("ntpc") || idLower.contains("ntpc")) {
            return "rrb_ntpc"
        }

        // 6. RRB Group D
        if (t.contains("group_d") || t.contains("groupd") || rawT.contains("group d") || rawT.contains("ग्रुप d") ||
            rawT.contains("ग्रुप-d") || shiftLower.contains("group d") || idLower.contains("group_d") || idLower.contains("groupd") || idLower.contains("rrc")) {
            return "rrb_group_d"
        }

        // 7. RRB ALP & Technician
        if (t.contains("alp") || rawT.contains("technician") || rawT.contains("एएलपी") || rawT.contains("लोको पायलट") ||
            rawT.contains("loco pilot") || shiftLower.contains("alp") || idLower.contains("alp")) {
            return "rrb_alp"
        }

        // 8. RRB JE
        if (t.contains("rrb_je") || t.contains("rrb je") || rawT.contains("junior engineer") || rawT.contains("जूनियर इंजीनियर") ||
            shiftLower.contains("je") || idLower.contains("rrb_je")) {
            return "rrb_je"
        }

        // 9. SSC CGL
        if (t.contains("cgl") || rawT.contains("cgl") || rawT.contains("सीजीएल") || idLower.contains("cgl") || rawT.contains("combined graduate")) {
            return "ssc_cgl"
        }

        // 10. SSC CHSL
        if (t.contains("chsl") || rawT.contains("chsl") || rawT.contains("सीएचएसएल") || idLower.contains("chsl") || rawT.contains("10+2")) {
            return "ssc_chsl"
        }

        // 11. SSC CPO
        if (t.contains("cpo") || rawT.contains("cpo") || rawT.contains("सीपीओ") || idLower.contains("cpo")) {
            return "ssc_cpo"
        }

        // 12. SSC MTS & Havaldar
        if (t.contains("mts") || rawT.contains("mts") || rawT.contains("हवलदार") || rawT.contains("havaldar") ||
            rawT.contains("एमटीएस") || idLower.contains("mts")) {
            return "ssc_mts"
        }

        // 13. Patwari
        if (t.contains("patwari") || rawT.contains("पटवारी") || idLower.contains("patwari")) {
            return "patwari"
        }

        // 14. Lekhpal
        if (t.contains("lekhpal") || rawT.contains("लेखपाल") || idLower.contains("lekhpal")) {
            return "lekhpal"
        }

        // 15. Rajasthan CET
        if (t.contains("cet") || rawT.contains("cet") || rawT.contains("सीईटी") || idLower.contains("cet")) {
            return "rajasthan_cet"
        }

        // 16. SSC GD
        if (t.contains("ssc_gd") || rawT.contains("ssc gd") || rawT.contains("gd constable") || rawT.contains("जीडी") ||
            shiftLower.contains("ssc gd") || shiftLower.contains("gd ") || idLower.contains("ssc_gd") || idLower.contains("gd_") || t.contains("gd_") || t == "gd" || t.endsWith("_gd") ||
            rawT.contains("capf")) {
            return "ssc_gd"
        }

        return null
    }

    /**
     * Checks if a title, raw title, or paper ID has explicit markers pointing to another exam.
     */
    fun isAnyOtherExplicitExam(t: String, rawT: String, idLower: String, currentExam: String): Boolean {
        // If the title explicitly belongs to currentExam, ignore any conflicting foreign ID prefix
        val explicitFromTitle = inferExplicitExamFromContent(rawT, emptyList(), "")
        if (explicitFromTitle != null) {
            return explicitFromTitle != currentExam
        }

        if (currentExam != "ssc_delhi_police" && (t.contains("delhi") || rawT.contains("दिल्ली") || idLower.contains("delhi") || idLower.contains("dp_") || t.contains("dp_constable") || rawT.contains("dp constable"))) return true
        if (currentExam != "up_police" && (t.contains("up_police") || t.contains("uppolice") || rawT.contains("up police") || rawT.contains("यूपी पुलिस") || rawT.contains("उत्तर प्रदेश पुलिस") || idLower.contains("up_police"))) return true
        if (currentExam != "rajasthan_police" && (t.contains("rajasthan_police") || t.contains("rajpolice") || rawT.contains("rajasthan police") || rawT.contains("राजस्थान पुलिस") || rawT.contains("राज पुलिस") || idLower.contains("rajasthan_police"))) return true
        if (currentExam != "rpf_si" && (t.contains("rpf") || rawT.contains("rpf si") || rawT.contains("आरपीएफ") || rawT.contains("railway police") || idLower.contains("rpf"))) return true
        if (currentExam != "rrb_ntpc" && (t.contains("ntpc") || rawT.contains("ntpc") || rawT.contains("एनटीपीसी") || idLower.contains("ntpc"))) return true
        if (currentExam != "rrb_group_d" && (t.contains("group_d") || t.contains("groupd") || rawT.contains("group d") || rawT.contains("ग्रुप d") || idLower.contains("group_d"))) return true
        if (currentExam != "rrb_alp" && (t.contains("alp") || rawT.contains("technician") || rawT.contains("एएलपी") || rawT.contains("लोको पायलट") || idLower.contains("alp"))) return true
        if (currentExam != "rrb_je" && (t.contains("rrb_je") || t.contains("rrb je") || rawT.contains("junior engineer") || idLower.contains("rrb_je"))) return true
        if (currentExam != "ssc_cgl" && (t.contains("cgl") || rawT.contains("cgl") || rawT.contains("सीजीएल") || idLower.contains("cgl"))) return true
        if (currentExam != "ssc_chsl" && (t.contains("chsl") || rawT.contains("chsl") || rawT.contains("सीएचएसएल") || idLower.contains("chsl"))) return true
        if (currentExam != "ssc_cpo" && (t.contains("cpo") || rawT.contains("cpo") || rawT.contains("सीपीओ") || idLower.contains("cpo"))) return true
        if (currentExam != "ssc_mts" && (t.contains("mts") || rawT.contains("mts") || rawT.contains("हवलदार") || rawT.contains("havaldar") || idLower.contains("mts"))) return true
        if (currentExam != "patwari" && (t.contains("patwari") || rawT.contains("पटवारी") || idLower.contains("patwari"))) return true
        if (currentExam != "lekhpal" && (t.contains("lekhpal") || rawT.contains("लेखपाल") || idLower.contains("lekhpal"))) return true
        if (currentExam != "rajasthan_cet" && (t.contains("cet") || rawT.contains("cet") || rawT.contains("सीईटी") || idLower.contains("cet"))) return true
        if (currentExam != "ssc_gd" && (t.contains("ssc_gd") || rawT.contains("ssc gd") || rawT.contains("gd constable") || rawT.contains("जीडी") || idLower.contains("ssc_gd"))) return true
        return false
    }

    /**
     * Universal Strict Exam Classifier & Owner Resolver:
     * Guarantees that EVERY test paper is strictly locked to ONE specific exam.
     * Prevents cross-contamination across ALL 16 exams in the application.
     */
    fun getStrictExamOwner(
        title: String,
        questions: List<QuizQuestion> = emptyList(),
        paperId: String = "",
        rawSubExamId: String = ""
    ): String {
        val t = title.lowercase().replace("-", "_").replace(" ", "_")
        val rawT = title.lowercase()
        val idLower = paperId.lowercase().replace("-", "_").replace(" ", "_")
        val rawSubNorm = if (rawSubExamId.isNotBlank()) normalizeSubExamId(rawSubExamId) else ""

        // 1. Absolute Priority: Respect the folder/file assigned rawSubExamId if valid!
        if (rawSubNorm.isNotBlank() && rawSubNorm != "constable" && rawSubNorm != "ssc" && rawSubNorm != "general" && rawSubNorm != "all") {
            return rawSubNorm
        }

        // 2. Fallback: Check explicit exam markers in title, ID, or content if rawSubExamId was blank/generic
        val explicitContentExam = inferExplicitExamFromContent(title, questions, paperId)
        if (explicitContentExam != null) {
            return explicitContentExam
        }

        // 3. Fallback: check title hints
        val inferred = inferSubExamFromTitle(title, questions)
        if (inferred.isNotBlank() && inferred != "general") {
            return inferred
        }

        return rawSubNorm
    }

    /**
     * Universal Leakage Prevention Guard:
     * Validates that a test paper does NOT belong to ANY other exam.
     * Prevents cross-contamination across ALL exams in the app.
     */
    fun isNotLeakingIntoExam(targetExamId: String, title: String, questions: List<QuizQuestion> = emptyList(), paperId: String = "", rawSubExamId: String = ""): Boolean {
        val normTarget = normalizeSubExamId(targetExamId)
        val owner = getStrictExamOwner(title, questions, paperId, rawSubExamId)
        return owner == normTarget
    }

    fun isDelhiPoliceTest(title: String, questions: List<QuizQuestion> = emptyList(), paperId: String = ""): Boolean {
        return getStrictExamOwner(title, questions, paperId, "") == "ssc_delhi_police"
    }

    fun isSscGdTest(title: String, questions: List<QuizQuestion> = emptyList(), paperId: String = ""): Boolean {
        return getStrictExamOwner(title, questions, paperId, "") == "ssc_gd"
    }

    fun resolveAccurateSubExamId(rawSubExam: String, title: String, questions: List<QuizQuestion> = emptyList(), paperId: String = ""): String {
        return getStrictExamOwner(title, questions, paperId, rawSubExam)
    }

    /**
     * Sanitizes all existing custom tests in memory and cleans mislabeled subExamId or testType
     */
    fun sanitizeExistingDynamicTests(context: Context? = null) {
        synchronized(this) {
            var modified = false

            // CRITICAL FIX: Unblock and remove any active test ID or title from deletedTestIds so active tests are NEVER hidden or deleted
            val activeKeys = customTests.flatMap {
                listOf(
                    it.testPaper.id.toString().trim(),
                    it.testPaper.title.trim(),
                    it.testPaper.title.trim().lowercase()
                )
            }.filter { it.isNotBlank() }.toSet()

            if (deletedTestIds.removeAll(activeKeys)) {
                modified = true
            }

            // Remove any plain-text titles from deletedTestIds so valid tests in other years are never blocked
            val unblockedTitles = deletedTestIds.removeAll { id ->
                !id.startsWith("custom_") && !id.startsWith("dyn_") && !id.startsWith("ssc_") && !id.startsWith("test_") && !id.startsWith("chap_") && id.contains(" ")
            }
            if (unblockedTitles) modified = true

            // Deduplicate customTests and protect full tests from being misclassified as chapterwise
            val uniqueTests = mutableListOf<CustomTestEntry>()
            for (test in customTests) {
                val strictOwner = getStrictExamOwner(test.testPaper.title, test.testPaper.questions, test.testPaper.id, test.subExamId)
                val rawTitle = test.testPaper.title
                val qCount = test.testPaper.questions.size

                val isExplicitFull = test.testType.equals("FULL_TEST", true) || qCount >= 30 ||
                        rawTitle.contains("Shift", ignoreCase = true) ||
                        rawTitle.contains("Full Test", ignoreCase = true) ||
                        rawTitle.contains("मॉक टेस्ट", ignoreCase = true) ||
                        rawTitle.contains("Full Mock", ignoreCase = true) ||
                        rawTitle.contains("फुल मॉक", ignoreCase = true) ||
                        rawTitle.contains("Mock Test", ignoreCase = true) ||
                        rawTitle.contains("Paper", ignoreCase = true) ||
                        rawTitle.contains("पेपर", ignoreCase = true)

                val isChapterSet = isChapterWiseTest(test.testType, rawTitle, test.testPaper.id)

                val effectiveTestType = if (isChapterSet) {
                    "CHAPTER_WISE"
                } else if (test.testType.equals("SUBJECT_WISE", true) || rawTitle.contains("Subject Wise", true) || rawTitle.contains("विषयवार", true)) {
                    "SUBJECT_WISE"
                } else {
                    "FULL_TEST"
                }

                val key = "${strictOwner}_${effectiveTestType}_${rawTitle.trim().lowercase()}_${test.year}"
                val existingIdx = uniqueTests.indexOfFirst {
                    "${it.subExamId}_${it.testType}_${it.testPaper.title.trim().lowercase()}_${it.year}" == key
                }

                var fixedPaper = test.testPaper
                // Cleanse foreign exam prefixes across all exams
                val oldId = fixedPaper.id
                if (FOREIGN_EXAM_PREFIX_REGEX.containsMatchIn(oldId)) {
                    val replaced = oldId.replaceFirst(FOREIGN_EXAM_PREFIX_REGEX, "dyn_${strictOwner}_")
                    if (replaced != oldId) {
                        fixedPaper = fixedPaper.copy(id = replaced)
                        modified = true
                    }
                }

                val fixedTest = test.copy(subExamId = strictOwner, testType = effectiveTestType, testPaper = fixedPaper)

                if (existingIdx == -1) {
                    uniqueTests.add(fixedTest)
                } else {
                    if (fixedTest.testPaper.questions.size > uniqueTests[existingIdx].testPaper.questions.size || fixedTest.createdAt > uniqueTests[existingIdx].createdAt) {
                        uniqueTests[existingIdx] = fixedTest
                    }
                    modified = true
                }
            }

            // Build atomic sanitized list
            val sanitizedList = mutableListOf<CustomTestEntry>()
            for (entry in uniqueTests) {
                val strictOwner = getStrictExamOwner(entry.testPaper.title, entry.testPaper.questions, entry.testPaper.id, entry.subExamId)
                val rawTitle = entry.testPaper.title

                val isChapterSet = isChapterWiseTest(entry.testType, rawTitle, entry.testPaper.id)

                val accurateTestType = if (isChapterSet) {
                    "CHAPTER_WISE"
                } else if (entry.testType.equals("SUBJECT_WISE", true) || rawTitle.contains("Subject Wise", true) || rawTitle.contains("विषयवार", true)) {
                    "SUBJECT_WISE"
                } else {
                    "FULL_TEST"
                }

                val fixedEntry = if (strictOwner != entry.subExamId || accurateTestType != entry.testType) {
                    entry.copy(subExamId = strictOwner, testType = accurateTestType)
                } else {
                    entry
                }

                sanitizedList.add(fixedEntry)
                if (fixedEntry.subExamId != entry.subExamId || fixedEntry.testType != entry.testType) {
                    modified = true
                }
            }

            if (modified || sanitizedList.size != customTests.size) {
                applySnapshotState(newTests = sanitizedList)
                if (context != null) {
                    saveToDisk(context)
                }
            }
        }
    }

    /**
     * Complete purge of any exam cross-contamination across all exams.
     */
    fun purgeCorruptedExamCrossContamination(context: Context? = null) {
        sanitizeExistingDynamicTests(context)
    }

    /**
     * Returns all dynamic tests matching the subExamId, postType, and year
     */
    fun getCustomTests(
        subExamId: String,
        postType: String,
        year: String
    ): List<TestPaper> {
        val normSubExam = normalizeSubExamId(subExamId)
        val normPost = postType.uppercase().trim()
        val normYear = year.trim()

        val candidateEntries = customTests.filter {
            val resolved = getStrictExamOwner(it.testPaper.title, it.testPaper.questions, it.testPaper.id, it.subExamId)
            resolved == normSubExam || normalizeSubExamId(it.subExamId) == normSubExam
        }

        val dynamicMatching = candidateEntries
            .filter { entry ->
                val title = entry.testPaper.title

                val notDeleted = !deletedTestIds.contains(entry.testPaper.id) &&
                        !deletedTestIds.contains(entry.testPaper.title.trim())
                if (!notDeleted) return@filter false

                // Chapter-wise tests and sets must NEVER appear in Full Tests!
                if (isChapterWiseTest(entry.testType, title, entry.testPaper.id)) return@filter false
                if (entry.testType.equals("SUBJECT_WISE", true) || title.contains("Subject Wise", true) || title.contains("विषयवार", true)) return@filter false

                val matchesPost = normPost.isEmpty() || normPost == "ALL" ||
                        entry.postType.isBlank() || entry.postType.equals("ALL", true) ||
                        entry.postType.equals("GENERAL", true) || entry.postType.equals(normPost, true) ||
                        entry.postType.contains(normPost, true) || normPost.contains(entry.postType, true)

                val matchesYear = normYear.isEmpty() || normYear == "सभी" || normYear == "ALL" ||
                        entry.year.isBlank() || entry.year == normYear || entry.year.equals("ALL", true) ||
                        entry.testPaper.year == normYear || entry.testPaper.title.contains(normYear)

                matchesPost && matchesYear
            }
            .map { it.testPaper }

        val defaultBuiltIn = DefaultFullMockTests.getFullTestsForSubExam(normSubExam)
            .filter { entry ->
                val notDeleted = !deletedTestIds.contains(entry.testPaper.id) && !deletedTestIds.contains(entry.testPaper.title)
                val matchesPost = normPost.isEmpty() || normPost == "ALL" || entry.postType.isBlank() || entry.postType.equals(normPost, ignoreCase = true) || entry.postType == "GENERAL"
                val matchesYear = normYear.isEmpty() || normYear == "सभी" || normYear == "ALL" || entry.year.isBlank() || entry.year == normYear || entry.testPaper.year == normYear || entry.testPaper.title.contains(normYear)
                notDeleted && matchesPost && matchesYear
            }
            .map { it.testPaper }

        return (defaultBuiltIn + dynamicMatching).distinctBy { it.id.toString() }
    }

    /**
     * Returns all custom tests for a subExam regardless of post/year (e.g. for general subExams)
     */
    fun getAllTestsForSubExam(subExamId: String, testType: String? = null): List<CustomTestEntry> {
        val normSubExam = normalizeSubExamId(subExamId)
        val targetType = testType?.trim()?.uppercase()
        val candidateEntries = customTests.filter {
            val resolved = getStrictExamOwner(it.testPaper.title, it.testPaper.questions, it.testPaper.id, it.subExamId)
            resolved == normSubExam || normalizeSubExamId(it.subExamId) == normSubExam
        }

        return candidateEntries.filter { entry ->
            val title = entry.testPaper.title

            val notDeleted = !deletedTestIds.contains(entry.testPaper.id) &&
                    !deletedTestIds.contains(entry.testPaper.title.trim())
            if (!notDeleted) return@filter false

            val isChapter = isChapterWiseTest(entry.testType, title, entry.testPaper.id)
            val isSubject = !isChapter && (entry.testType.equals("SUBJECT_WISE", true) || title.contains("Subject Wise", true) || title.contains("विषयवार", true))

            val matchesType = when (targetType) {
                "CHAPTER_WISE" -> isChapter
                "FULL_TEST" -> !isChapter && !isSubject
                "SUBJECT_WISE" -> isSubject
                else -> true
            }

            matchesType
        }.distinctBy { it.testPaper.id.toString() }
    }

    fun getAllCustomTests(): List<CustomTestEntry> {
        return customTests.toList()
    }

    /**
     * Returns any extra years introduced by dynamic tests (e.g., 2026, 2025) for a given subExam
     */
    fun getDynamicYearsForSubExam(subExamId: String, postType: String? = null): List<String> {
        val normSubExam = normalizeSubExamId(subExamId)
        val list = customTests
            .filter { entry ->
                val resolvedExam = getStrictExamOwner(entry.testPaper.title, entry.testPaper.questions, entry.testPaper.id, entry.subExamId)
                val matchesExam = resolvedExam == normSubExam || normalizeSubExamId(entry.subExamId) == normSubExam
                val matchesPost = postType == null || postType.isBlank() || postType == "ALL" ||
                        entry.postType.isBlank() || entry.postType.equals("ALL", true) ||
                        entry.postType.equals("GENERAL", true) || entry.postType.equals(postType, ignoreCase = true) ||
                        entry.postType.contains(postType, ignoreCase = true) || postType.contains(entry.postType, ignoreCase = true)
                matchesExam && matchesPost
            }
            .flatMap { listOf(it.year, it.testPaper.year) }
            .filter { it.isNotBlank() && it != "सभी" && it != "ALL" }
            .distinct()
            .sortedDescending()
        return (listOf("सभी") + list).distinct()
    }

    /**
     * Normalizes exam IDs to handle minor discrepancies (e.g. 'delhi police' vs 'ssc_delhi_police')
     */
    fun normalizeSubExamId(id: String): String {
        val clean = id.trim().lowercase().replace("-", "_").replace(" ", "_")
        return when {
            clean.contains("delhi_police") || clean.contains("delhipolice") || clean.contains("दिल्ली_पुलिस") || clean == "dp" || clean.startsWith("dp_") || clean.contains("delhi") || clean.contains("दिल्ली") -> "ssc_delhi_police"
            clean.contains("up_police") || clean.contains("uppolice") || clean.contains("यूपी_पुलिस") || clean.contains("उत्तर_प्रदेश_पुलिस") || (clean.contains("up") && (clean.contains("police") || clean.contains("constable") || clean.contains("si"))) -> "up_police"
            clean.contains("rajasthan_police") || clean.contains("rajpolice") || clean.contains("राजस्थान_पुलिस") || clean.contains("राज_पुलिस") || ((clean.contains("rajasthan") || clean.contains("raj")) && (clean.contains("police") || clean.contains("constable") || clean.contains("si"))) -> "rajasthan_police"
            clean.contains("rpf") || clean.contains("railway_police") || clean.contains("आरपीएफ") -> "rpf_si"
            clean.contains("cgl") || clean.contains("सीजीएल") -> "ssc_cgl"
            clean.contains("chsl") || clean.contains("सीएचएसएल") || clean.contains("10+2") -> "ssc_chsl"
            clean.contains("cpo") || clean.contains("सीपीओ") -> "ssc_cpo"
            clean.contains("mts") || clean.contains("एमटीएस") || clean.contains("havaldar") || clean.contains("हवलदार") -> "ssc_mts"
            clean.contains("ntpc") || clean.contains("एनटीपीसी") -> "rrb_ntpc"
            clean.contains("group_d") || clean.contains("groupd") || clean.contains("ग्रुप_d") || clean.contains("rrc") -> "rrb_group_d"
            clean.contains("alp") || clean.contains("technician") || clean.contains("एएलपी") || clean.contains("loco") -> "rrb_alp"
            clean.contains("je") || clean.contains("junior_engineer") || clean.contains("जूनियर_इंजीनियर") -> "rrb_je"
            clean.contains("patwari") || clean.contains("पटवारी") -> "patwari"
            clean.contains("lekhpal") || clean.contains("लेखपाल") -> "lekhpal"
            clean.contains("cet") || clean.contains("सीईटी") || clean.contains("rajasthan_cet") || clean.contains("raj_cet") -> "rajasthan_cet"
            clean.contains("ssc_gd") || clean.contains("gd_constable") || clean.contains("जीडी") || clean == "gd" || clean.startsWith("gd_") || clean.endsWith("_gd") || clean == "ssc" || clean == "constable" || clean.contains("capf") -> "ssc_gd"
            clean.startsWith("ssc_") -> clean
            clean.startsWith("rrb_") -> clean
            else -> clean
        }
    }

    /**
     * Serializes all dynamic tests into JSON and saves to internal disk storage
     */
    fun saveToDisk(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            synchronized(this@DynamicTestRepository) {
                try {
                    val file = File(appContext.filesDir, FILE_NAME)
                    val root = JSONObject()
                    root.put("version", 2)
                    root.put("lastSaved", System.currentTimeMillis())

                    val testsSnapshot = customTests.toList()
                    val deletedSnapshot = deletedTestIds.toList()

                    val array = JSONArray()
                    for (entry in testsSnapshot) {
                        val item = JSONObject()
                        item.put("categoryId", entry.categoryId)
                        item.put("subExamId", entry.subExamId)
                        item.put("postType", entry.postType)
                        item.put("year", entry.year)
                        item.put("testType", entry.testType)
                        item.put("isLiveDynamic", entry.isLiveDynamic)
                        item.put("createdAt", entry.createdAt)

                        val paper = entry.testPaper
                        val pObj = JSONObject()
                        pObj.put("id", paper.id)
                        pObj.put("title", paper.title)
                        pObj.put("totalQuestions", paper.totalQuestions)
                        pObj.put("durationMinutes", paper.durationMinutes)
                        pObj.put("totalMarks", paper.totalMarks)
                        pObj.put("difficulty", paper.difficulty)
                        pObj.put("year", paper.year)

                        val qArray = JSONArray()
                        for (q in paper.questions) {
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
                            qArray.put(qObj)
                        }
                        pObj.put("questions", qArray)
                        item.put("testPaper", pObj)

                        array.put(item)
                    }
                    root.put("tests", array)

                    val delArr = JSONArray()
                    for (id in deletedSnapshot) {
                        delArr.put(id)
                    }
                    root.put("deletedTestIds", delArr)

                    file.writeText(root.toString())
                    Log.d(TAG, "Saved ${testsSnapshot.size} dynamic tests to disk asynchronously.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving dynamic tests: ${e.message}", e)
                }
            }
        }
    }

    private fun applySnapshotState(
        newTests: List<CustomTestEntry>? = null,
        newDeletedIds: Set<String>? = null
    ) {
        synchronized(this) {
            if (newTests != null) {
                customTests.clear()
                customTests.addAll(newTests)
                rebuildExamIndexMap()
            }
            if (newDeletedIds != null) {
                deletedTestIds.clear()
                deletedTestIds.addAll(newDeletedIds)
            }
        }
        triggerUiRevisionUpdate()
    }

    /**
     * Loads dynamic tests from internal disk storage
     */
    fun loadFromDisk(context: Context) {
        try {
            val file = File(context.applicationContext.filesDir, FILE_NAME)
            if (!file.exists()) return

            val content = file.readText()
            if (content.isBlank()) return

            parseAndMergeJson(content, context, persist = false)
            sanitizeExistingDynamicTests(context)
            Log.d(TAG, "Loaded ${customTests.size} dynamic tests from disk.")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading dynamic tests from disk: ${e.message}", e)
        }
    }

    /**
     * Parses JSON string (from Cloud GitHub or local file) and merges tests intelligently
     * Returns count of new or updated tests.
     */
    fun parseAndMergeJson(
        jsonString: String,
        context: Context,
        persist: Boolean = true,
        updateUiSnapshot: Boolean = true,
        defaultSubExamId: String = "",
        allKnownActiveIds: Set<String> = emptySet(),
        allKnownActiveTitles: Set<String> = emptySet()
    ): Int {
        var addedCount = 0
        synchronized(this) {
            try {
                val trimmed = jsonString.trim()
                val curTests = customTests.toList().toMutableList()
                val curDeletedIds = deletedTestIds.toMutableSet()
                var modified = false

                if (trimmed.startsWith("{")) {
                    val root = JSONObject(trimmed)

                    // Check if it's the master bundle format with "tests" or "chapters" array
                    val array = root.optJSONArray("tests")
                        ?: root.optJSONArray("testPapers")
                        ?: root.optJSONArray("papers")
                        ?: root.optJSONArray("custom_tests")
                        ?: root.optJSONArray("customTests")
                        ?: root.optJSONArray("chapters")
                        ?: root.optJSONArray("custom_chapters")
                        ?: root.optJSONArray("customChapters")

                    // Collect active incoming test IDs and titles so we don't accidentally delete them if they exist in deletedTestIds
                    val activeIdsAndTitles = mutableSetOf<String>()
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue
                            val pObj = item.optJSONObject("testPaper") ?: item
                            val id = pObj.optString("id", "").ifBlank { item.optString("id", "") }
                            val title = pObj.optString("title", "").ifBlank { item.optString("title", "") }.trim()
                            if (id.isNotBlank()) activeIdsAndTitles.add(id)
                            if (title.isNotBlank()) activeIdsAndTitles.add(title)
                        }
                    } else if (root.has("title") || root.has("questions") || root.has("testPaper")) {
                        val pObj = root.optJSONObject("testPaper") ?: root
                        val id = pObj.optString("id", "").ifBlank { root.optString("id", "") }
                        val title = pObj.optString("title", "").ifBlank { root.optString("title", "") }.trim()
                        if (id.isNotBlank()) activeIdsAndTitles.add(id)
                        if (title.isNotBlank()) activeIdsAndTitles.add(title)
                    }

                    // Build master active set across memory, incoming JSON, and repo session
                    val masterActiveKeys = activeIdsAndTitles + allKnownActiveIds + allKnownActiveTitles +
                            curTests.map { it.testPaper.id.toString().trim() } +
                            curTests.map { it.testPaper.title.trim() }

                    // Remove any active test ID/title from curDeletedIds immediately
                    for (act in masterActiveKeys) {
                        if (act.isNotBlank()) {
                            if (curDeletedIds.remove(act)) modified = true
                            if (curDeletedIds.remove(act.lowercase())) modified = true
                        }
                    }

                    // 1. Sync deleted test IDs from Cloud to User device (skip active tests)
                    if (root.has("deletedTestIds")) {
                        val delArr = root.optJSONArray("deletedTestIds")
                        if (delArr != null) {
                            for (i in 0 until delArr.length()) {
                                val delId = delArr.optString(i, "").trim()
                                if (delId.isNotBlank() && delId != "null") {
                                    val isActiveTest = masterActiveKeys.contains(delId) ||
                                            masterActiveKeys.contains(delId.lowercase()) ||
                                            curTests.any {
                                                it.testPaper.id.toString().equals(delId, ignoreCase = true) ||
                                                it.testPaper.title.trim().equals(delId, ignoreCase = true)
                                            }

                                    if (isActiveTest) {
                                        if (curDeletedIds.remove(delId)) modified = true
                                    } else {
                                        if (!curDeletedIds.contains(delId)) {
                                            curDeletedIds.add(delId)
                                            modified = true
                                        }
                                        curTests.removeAll {
                                            it.testPaper.id.toString().equals(delId, ignoreCase = true) ||
                                            it.testPaper.title.trim().equals(delId, ignoreCase = true)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (array != null) {
                        for (i in 0 until array.length()) {
                            val item = array.optJSONObject(i) ?: continue
                            if (parseSingleTestEntry(item, curTests, curDeletedIds, context, defaultSubExamId) || parseSingleTestFormat(item, curTests, curDeletedIds, context, defaultSubExamId)) {
                                addedCount++
                                modified = true
                            }
                        }
                    } else if (root.has("title") || root.has("questions") || root.has("testPaper")) {
                        // Single test paper format
                        if (parseSingleTestFormat(root, curTests, curDeletedIds, context, defaultSubExamId) || parseSingleTestEntry(root, curTests, curDeletedIds, context, defaultSubExamId)) {
                            addedCount++
                            modified = true
                        }
                    }
                } else if (trimmed.startsWith("[")) {
                    // Array of test entries
                    val array = JSONArray(trimmed)
                    val activeIdsAndTitles = mutableSetOf<String>()
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val pObj = item.optJSONObject("testPaper") ?: item
                        val id = pObj.optString("id", "").ifBlank { item.optString("id", "") }
                        val title = pObj.optString("title", "").ifBlank { item.optString("title", "") }.trim()
                        if (id.isNotBlank()) activeIdsAndTitles.add(id)
                        if (title.isNotBlank()) activeIdsAndTitles.add(title)
                    }
                    for (act in activeIdsAndTitles) {
                        if (curDeletedIds.remove(act)) {
                            modified = true
                        }
                    }

                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        if (parseSingleTestEntry(item, curTests, curDeletedIds, context, defaultSubExamId) || parseSingleTestFormat(item, curTests, curDeletedIds, context, defaultSubExamId)) {
                            addedCount++
                            modified = true
                        }
                    }
                }

                if (updateUiSnapshot) {
                    if (modified || addedCount > 0) {
                        applySnapshotState(newTests = curTests, newDeletedIds = curDeletedIds)
                        sanitizeExistingDynamicTests(context)
                        if (persist) {
                            saveToDisk(context)
                        }
                    }
                } else {
                    if (modified || addedCount > 0) {
                        applySnapshotState(newTests = curTests, newDeletedIds = curDeletedIds)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing dynamic test JSON: ${e.message}", e)
            }
        }
        return addedCount
    }

    private fun parseSingleTestEntry(
        item: JSONObject,
        curTests: MutableList<CustomTestEntry>,
        curDeletedIds: Set<String>,
        context: Context,
        defaultSubExamId: String = ""
    ): Boolean {
        return try {
            val categoryId = item.optString("categoryId", "SSC Exams")
            val pObj = item.optJSONObject("testPaper") ?: item
            val idStr = pObj.optString("id", "").ifBlank { item.optString("id", "") }
            val titleStr = pObj.optString("title", "").ifBlank { item.optString("title", "") }.trim()
            val postType = item.optString("postType", if (titleStr.contains("head constable", true)) "HEAD_CONSTABLE" else "CONSTABLE").uppercase()
            val year = item.optString("year", pObj.optString("year", "2025"))
            val rawTestType = item.optString("testType", pObj.optString("testType", "")).uppercase()
            val isLiveDynamic = item.optBoolean("isLiveDynamic", true)
            val createdAt = item.optLong("createdAt", System.currentTimeMillis())

            val questionsArr = pObj.optJSONArray("questions") ?: item.optJSONArray("questions")
            val questions = parseQuestionsArray(questionsArr, context)
            if (questions.isEmpty()) return false

            // If test is present in GitHub repository JSON, automatically un-delete it if it was previously deleted
            if (curDeletedIds is MutableSet) {
                if (idStr.isNotBlank()) curDeletedIds.remove(idStr)
                if (titleStr.isNotBlank()) curDeletedIds.remove(titleStr)
            }

            val rawSubExam = item.optString("subExamId", "")
                .ifBlank { item.optString("sub_exam_id", "") }
                .ifBlank { item.optString("examId", "") }
                .ifBlank { item.optString("exam_id", "") }
                .ifBlank { item.optString("exam", "") }
                .ifBlank { item.optString("examName", "") }
                .ifBlank { item.optString("targetExam", "") }
                .ifBlank { pObj.optString("subExamId", "") }
                .ifBlank { pObj.optString("sub_exam_id", "") }
                .ifBlank { pObj.optString("examId", "") }
                .ifBlank { pObj.optString("exam_id", "") }
                .ifBlank { defaultSubExamId }

            val subExamId = resolveAccurateSubExamId(rawSubExam, titleStr, questions)

            val isChapterType = isChapterWiseTest(rawTestType, titleStr, idStr)
            val isSubjectType = !isChapterType && (rawTestType == "SUBJECT_WISE" || titleStr.contains("Subject Wise", true) || titleStr.contains("विषयवार", true))
            val testType = if (isChapterType) "CHAPTER_WISE" else if (isSubjectType) "SUBJECT_WISE" else "FULL_TEST"

            val rawDiff = pObj.optString("difficulty", "ऑफिशियल पैटर्न")
            val cleanDiff = if (rawDiff.contains("AI", ignoreCase = true) || rawDiff.contains("जेनरेटेड") || rawDiff.contains("ऑनलाइन लाइव")) "ऑफिशियल पैटर्न" else rawDiff.ifBlank { "ऑफिशियल पैटर्न" }

            val finalPaperId = if (idStr.isNotBlank()) {
                if (!idStr.startsWith(subExamId) && !idStr.contains("_" + year)) {
                    "${subExamId}_${year}_${postType.lowercase()}_$idStr"
                } else {
                    idStr
                }
            } else {
                val cleanTitleHash = Math.abs(titleStr.trim().replace(WHITESPACE_REGEX, " ").lowercase().hashCode())
                "dyn_${subExamId}_${postType}_${year}_${cleanTitleHash}"
            }

            val paper = TestPaper(
                id = finalPaperId,
                title = if (titleStr.isNotBlank()) titleStr else "Mock Test ($year)",
                totalQuestions = questions.size,
                durationMinutes = pObj.optInt("durationMinutes", if (questions.size >= 80) 90 else if (isChapterType) 20 else 60),
                totalMarks = pObj.optInt("totalMarks", questions.size),
                difficulty = cleanDiff,
                year = year,
                questions = questions
            )

            // Remove existing test ONLY if same ID or (same title AND same subExamId AND same year)
            curTests.removeAll {
                it.testPaper.id == paper.id || (idStr.isNotBlank() && it.testPaper.id == idStr) ||
                (it.testPaper.title.equals(paper.title.trim(), ignoreCase = true) && it.subExamId == subExamId && it.year == year)
            }

            curTests.add(
                0,
                CustomTestEntry(
                    categoryId = categoryId,
                    subExamId = subExamId,
                    postType = postType,
                    year = year,
                    testType = testType,
                    testPaper = paper,
                    isLiveDynamic = isLiveDynamic,
                    createdAt = createdAt
                )
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseSingleTestEntry: ${e.message}", e)
            false
        }
    }

    private fun parseSingleTestFormat(
        root: JSONObject,
        curTests: MutableList<CustomTestEntry>,
        curDeletedIds: Set<String>,
        context: Context,
        defaultSubExamId: String = ""
    ): Boolean {
        return try {
            val title = root.optString("title", "Online Live Test").trim()
            val idStr = root.optString("id", "")
            val year = root.optString("year", "2025")
            val categoryId = root.optString("categoryId", "SSC Exams")
            val questionsArr = root.optJSONArray("questions")
            val questions = parseQuestionsArray(questionsArr, context)
            if (questions.isEmpty()) return false

            // If test is present in GitHub repository JSON, automatically un-delete it if it was previously deleted
            if (curDeletedIds is MutableSet) {
                if (idStr.isNotBlank()) curDeletedIds.remove(idStr)
                if (title.isNotBlank()) curDeletedIds.remove(title)
            }

            val rawSubExam = root.optString("subExamId", "")
                .ifBlank { root.optString("sub_exam_id", "") }
                .ifBlank { root.optString("examId", "") }
                .ifBlank { root.optString("exam_id", "") }
                .ifBlank { root.optString("exam", "") }
                .ifBlank { root.optString("examName", "") }
                .ifBlank { root.optString("targetExam", "") }
                .ifBlank { defaultSubExamId }

            val subExamId = resolveAccurateSubExamId(rawSubExam, title, questions)
            val postType = root.optString("postType", if (title.contains("head constable", ignoreCase = true)) "HEAD_CONSTABLE" else "CONSTABLE").uppercase()
            val rawTestType = root.optString("testType", "").uppercase()

            val isChapterType = isChapterWiseTest(rawTestType, title, idStr)
            val isSubjectType = !isChapterType && (rawTestType == "SUBJECT_WISE" || title.contains("Subject Wise", true) || title.contains("विषयवार", true))
            val testType = if (isChapterType) "CHAPTER_WISE" else if (isSubjectType) "SUBJECT_WISE" else "FULL_TEST"

            val rawDiff = root.optString("difficulty", "ऑफिशियल पैटर्न")
            val cleanDiff = if (rawDiff.contains("AI", ignoreCase = true) || rawDiff.contains("जेनरेटेड") || rawDiff.contains("ऑनलाइन लाइव")) "ऑफिशियल पैटर्न" else rawDiff.ifBlank { "ऑफिशियल पैटर्न" }

            val finalPaperId = if (idStr.isNotBlank()) {
                if (!idStr.startsWith(subExamId) && !idStr.contains("_" + year)) {
                    "${subExamId}_${year}_${postType.lowercase()}_$idStr"
                } else {
                    idStr
                }
            } else {
                val cleanTitleHash = Math.abs(title.trim().replace(WHITESPACE_REGEX, " ").lowercase().hashCode())
                "dyn_${subExamId}_${postType}_${year}_${cleanTitleHash}"
            }

            val paper = TestPaper(
                id = finalPaperId,
                title = title,
                totalQuestions = questions.size,
                durationMinutes = root.optInt("durationMinutes", if (questions.size >= 80) 90 else if (isChapterType) 20 else 60),
                totalMarks = root.optInt("totalMarks", questions.size),
                difficulty = cleanDiff,
                year = year,
                questions = questions
            )

            // Remove existing test ONLY if same ID or (same title AND same subExamId AND same year)
            curTests.removeAll {
                it.testPaper.id == paper.id || (idStr.isNotBlank() && it.testPaper.id == idStr) ||
                (it.testPaper.title.equals(paper.title.trim(), ignoreCase = true) && it.subExamId == subExamId && it.year == year)
            }

            curTests.add(
                0,
                CustomTestEntry(
                    categoryId = categoryId,
                    subExamId = subExamId,
                    postType = postType,
                    year = year,
                    testType = testType,
                    testPaper = paper,
                    isLiveDynamic = true,
                    createdAt = System.currentTimeMillis()
                )
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in parseSingleTestFormat: ${e.message}", e)
            false
        }
    }

    private fun parseQuestionsArray(array: JSONArray?, context: Context): List<QuizQuestion> {
        if (array == null) return emptyList()
        val list = mutableListOf<QuizQuestion>()
        for (i in 0 until array.length()) {
            val qObj = array.optJSONObject(i) ?: continue
            val optsArr = qObj.optJSONArray("options")
            val opts = mutableListOf<String>()
            if (optsArr != null) {
                for (j in 0 until optsArr.length()) {
                    val opt = optsArr.optString(j, "").trim()
                    if (opt.isNotBlank() && opt != "null") {
                        opts.add(opt)
                    }
                }
            }
            if (opts.isEmpty()) {
                opts.addAll(listOf("Option A", "Option B", "Option C", "Option D"))
            }
            val rawImg = qObj.optString("imageUrl", "")
            val imgUrl = if (rawImg.isNotBlank() && rawImg != "null") rawImg else null

            val rawShift = qObj.optString("examShiftInfo", "")
                .ifBlank { qObj.optString("shiftInfo", "") }
                .ifBlank { qObj.optString("examDateShift", "") }
                .ifBlank { qObj.optString("examInfo", "") }
            val shiftInfo = if (rawShift.isNotBlank() && rawShift != "null") rawShift else null

            val rawCorrectIndex = qObj.optInt("correctIndex", 0)
            val safeCorrectIndex = if (rawCorrectIndex in opts.indices) rawCorrectIndex else 0

            list.add(
                QuizQuestion(
                    id = qObj.optInt("id", i + 1),
                    question = qObj.optString("question", "Question ${i + 1}"),
                    options = opts,
                    correctIndex = safeCorrectIndex,
                    explanation = qObj.optString("explanation", ""),
                    subject = qObj.optString("subject", "GK / GS"),
                    imageUrl = imgUrl,
                    examShiftInfo = shiftInfo
                )
            )
        }
        return list
    }

    fun inferSubExamFromTitle(title: String, questions: List<QuizQuestion> = emptyList()): String {
        val t = title.lowercase().replace("-", "_").replace(" ", "_")
        val rawT = title.lowercase()

        // 1. Explicit Exam Mentions in Title (Handles underscores, spaces, Hindi & abbreviations)
        if (t.contains("delhi_police") || t.contains("delhipolice") || rawT.contains("delhi police") ||
            rawT.contains("दिल्ली पुलिस") || t.contains("dp_") || t.contains("dp_constable") || t == "dp" || t.contains("delhi") || rawT.contains("दिल्ली")) {
            return "ssc_delhi_police"
        }
        if (t.contains("ssc_gd") || rawT.contains("ssc gd") || rawT.contains("gd constable") ||
            rawT.contains("जीडी") || t.contains("gd_") || t == "gd" || t.endsWith("_gd")) {
            return "ssc_gd"
        }
        if (t.contains("cgl") || rawT.contains("cgl")) return "ssc_cgl"
        if (t.contains("chsl") || rawT.contains("chsl")) return "ssc_chsl"
        if (t.contains("cpo") || rawT.contains("cpo")) return "ssc_cpo"
        if (t.contains("mts") || rawT.contains("हवलदार") || rawT.contains("havaldar")) return "ssc_mts"
        if (t.contains("ntpc") || rawT.contains("ntpc")) return "rrb_ntpc"
        if (t.contains("group_d") || t.contains("groupd") || rawT.contains("group d") || rawT.contains("ग्रुप d")) return "rrb_group_d"
        if (t.contains("alp") || rawT.contains("technician") || rawT.contains("एएलपी")) return "rrb_alp"
        if (t.contains("je") || rawT.contains("junior engineer")) return "rrb_je"
        if (t.contains("rpf") || rawT.contains("rpf si")) return "rpf_si"
        if (t.contains("up_police") || t.contains("uppolice") || rawT.contains("up police") || rawT.contains("यूपी पुलिस")) return "up_police"
        if (t.contains("rajasthan_police") || t.contains("rajpolice") || rawT.contains("rajasthan police") || rawT.contains("राजस्थान पुलिस")) return "rajasthan_police"
        if (t.contains("patwari") || rawT.contains("पटवारी")) return "patwari"
        if (t.contains("lekhpal") || rawT.contains("लेखपाल")) return "lekhpal"
        if (t.contains("cet") || rawT.contains("cet")) return "rajasthan_cet"

        val norm = normalizeSubExamId(title)
        if (norm != title && norm.isNotBlank()) {
            return norm
        }

        return ""
    }

    /**
     * Exports all dynamic tests to a master JSON string for publishing to GitHub `data.json`
     */
    fun exportAllToMasterJson(): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("lastUpdated", java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()))
        root.put("totalTests", customTests.size)

        val array = JSONArray()
        for (entry in customTests) {
            val item = JSONObject()
            item.put("categoryId", entry.categoryId)
            item.put("subExamId", entry.subExamId)
            item.put("postType", entry.postType)
            item.put("year", entry.year)
            item.put("testType", entry.testType)
            item.put("isLiveDynamic", entry.isLiveDynamic)
            item.put("createdAt", entry.createdAt)

            val paper = entry.testPaper
            val pObj = JSONObject()
            pObj.put("id", paper.id)
            pObj.put("title", paper.title)
            pObj.put("totalQuestions", paper.totalQuestions)
            pObj.put("durationMinutes", paper.durationMinutes)
            pObj.put("totalMarks", paper.totalMarks)
            pObj.put("difficulty", paper.difficulty)
            pObj.put("year", paper.year)

            val qArray = JSONArray()
            for (q in paper.questions) {
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
                qArray.put(qObj)
            }
            pObj.put("questions", qArray)
            item.put("testPaper", pObj)

            array.put(item)
        }
        root.put("tests", array)

        val delArr = JSONArray()
        for (id in deletedTestIds) {
            delArr.put(id)
        }
        root.put("deletedTestIds", delArr)

        // Also export all custom chapters so master json contains full chapter bank
        val chapArr = JSONArray()
        for (chap in com.example.model.CustomChapterRepository.customChapters) {
            val cObj = JSONObject()
            cObj.put("id", chap.id)
            cObj.put("subExamId", chap.subExamId)
            cObj.put("subjectKey", chap.subjectKey)
            cObj.put("subjectTitle", chap.subjectTitle)
            cObj.put("title", chap.title)
            cObj.put("englishTitle", chap.englishTitle)
            cObj.put("description", chap.description)
            cObj.put("totalQuestions", chap.totalQuestions)
            cObj.put("durationMinutes", chap.durationMinutes)
            cObj.put("totalMarks", chap.totalMarks)
            cObj.put("difficulty", chap.difficulty)
            cObj.put("year", chap.year)
            cObj.put("createdAt", chap.createdAt)

            val qArray = JSONArray()
            for (q in chap.questions) {
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
                qArray.put(qObj)
            }
            cObj.put("questions", qArray)
            chapArr.put(cObj)
        }
        root.put("chapters", chapArr)

        return root.toString(2)
    }

    /**
     * Removes local custom tests that are no longer present in GitHub master sync,
     * ensuring tests deleted on GitHub are deleted locally as well.
     */
    fun removeOrphanedTestsNotInGitHub(activeIds: Set<String>, activeTitles: Set<String>, context: Context) {
        synchronized(this) {
            val beforeSize = customTests.size
            val normalizedActiveTitles = activeTitles.map { it.trim().replace(WHITESPACE_REGEX, " ").lowercase() }.toSet()
            val normalizedActiveIds = activeIds.map { it.trim() }.toSet()

            customTests.removeAll { entry ->
                val id = entry.testPaper.id.toString().trim()
                val isExplicitlyDeleted = deletedTestIds.contains(id) || deletedTestIds.contains(entry.testPaper.title.trim())
                // Only prune tests that were explicitly marked as deleted by the admin
                isExplicitlyDeleted
            }
            rebuildExamIndexMap()
            if (customTests.size != beforeSize) {
                saveToDisk(context)
                Log.d(TAG, "Removed ${beforeSize - customTests.size} deleted tests from disk.")
            }
        }
    }
}
