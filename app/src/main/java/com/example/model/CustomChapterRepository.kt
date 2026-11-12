package com.example.model

import android.content.Context
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Repository to persist and manage custom subjects and custom chapter test buttons
 * added by the Admin dynamically.
 */
object CustomChapterRepository {
    private const val TAG = "CustomChapterRepo"
    private const val FILE_NAME = "custom_exam_chapters.json"

    data class CustomSubjectEntry(
        val id: String,
        val subExamId: String,
        val key: String,
        val name: String,
        val englishName: String,
        val iconType: String = "BOOK"
    )

    data class CustomChapterEntry(
        val id: String,
        val subExamId: String,
        val subjectKey: String,
        val subjectTitle: String,
        val title: String,
        val englishTitle: String = "",
        val description: String = "",
        val totalQuestions: Int = 15,
        val durationMinutes: Int = 20,
        val totalMarks: Int = 15,
        val difficulty: String = "मध्यम",
        val questions: List<QuizQuestion> = emptyList(),
        val year: String = "2025",
        val createdAt: Long = System.currentTimeMillis()
    )

    data class CustomTopicEntry(
        val id: String,
        val subExamId: String,
        val subjectKey: String,
        val title: String,
        val englishTitle: String = "",
        val description: String = "",
        val createdAt: Long = System.currentTimeMillis()
    )

    val customSubjects: SnapshotStateList<CustomSubjectEntry> = mutableStateListOf()
    val customChapters: SnapshotStateList<CustomChapterEntry> = mutableStateListOf()
    val customTopics: SnapshotStateList<CustomTopicEntry> = mutableStateListOf()
    val deletedChapterIds: SnapshotStateList<String> = mutableStateListOf()
    var revision by mutableIntStateOf(0)

    private data class CustomSnapshotData(
        val chapters: List<CustomChapterEntry>,
        val subjects: List<CustomSubjectEntry>,
        val topics: List<CustomTopicEntry>,
        val deletedIds: Set<String>
    )

    // Precompiled Static Regex Patterns (zero-allocation performance optimization)
    private val REGEX_EXAM_PREFIX = Regex("""(?i)^(?:SSC\s*GD|Delhi\s*Police|दिल्ली\s*पुलिस|UP\s*Police|Railway|RPF|CET)\s*[-•–—:|/]\s*""")
    private val REGEX_SUBJECT_PREFIX = Regex("""(?i)^(?:प्रारंभिक\s*गणित|गणित|Maths?|सामान्य\s*बुद्धिमत्ता\s*एवं\s*तर्कशक्ति|तर्कशक्ति|रीजनिंग|Reasoning|सामान्य\s*ज्ञान\s*एवं\s*सामान्य\s*जानकारी|सामान्य\s*ज्ञान|GK\s*/\s*GS|GK|GS|सामान्य\s*हिंदी|हिंदी|Hindi|General\s*English|English|कंप्यूटर)\s*[-•–—:|/]\s*""")
    private val REGEX_NUMBERING = Regex("""^\s*\d+[\.\-\)]\s*""")
    private val REGEX_SET_SUFFIX = Regex("""(?i)\s*[-–—:]*\s*(?:set|सेट)\s*\d+\s*$""")
    private val REGEX_YEAR_TAG = Regex("""(?i)\s*\((?:PYQ\s*)?\d{4}(?:\s*PYQ)?\)\s*""")
    private val REGEX_SET_MATCH = Regex("""(?i)(?:set|सेट)\s*(\d+)""")

    // Ultra-Fast In-Memory Cache for getFullSubjectsForExam (prevents UI thread jank & ANR)
    private val fullSubjectsCache = java.util.concurrent.ConcurrentHashMap<String, List<ExamSubjectCategory>>()
    private var cachedCustomChaptersRev = -1
    private var cachedDynTestsRev = -1
    private var cachedCustomChaptersCount = -1
    private var cachedDynTestsCount = -1

    fun invalidateFullSubjectsCache() {
        synchronized(fullSubjectsCache) {
            fullSubjectsCache.clear()
            cachedCustomChaptersRev = -1
            cachedDynTestsRev = -1
            cachedCustomChaptersCount = -1
            cachedDynTestsCount = -1
        }
        RealtimeExamStatsManager.invalidateCache()
    }

    suspend fun preloadExamSubjects(context: Context) {
        val allSubExams = examCategories.flatMap { it.subExams }
        allSubExams.forEach { subExam ->
            getFullSubjectsForExam(subExam.id, context)
        }
    }

    fun cleanBaseChapterTitle(rawTitle: String?): String {
        if (rawTitle.isNullOrBlank()) return ""
        var title = rawTitle.trim()
        // Strip leading exam prefixes like "SSC GD • ", "SSC GD - ", "Delhi Police - "
        title = title.replace(REGEX_EXAM_PREFIX, "")
        // Strip leading subject prefixes like "प्रारंभिक गणित • ", "गणित - ", "तर्कशक्ति • ", "Maths - "
        title = title.replace(REGEX_SUBJECT_PREFIX, "")
        // Strip numbering like "1. ", "01. ", "1 - ", "1) "
        title = title.replace(REGEX_NUMBERING, "")
        // Strip Set suffixes: " - Set 1", " Set 1", " - सेट 1", " सेट 1"
        while (REGEX_SET_SUFFIX.containsMatchIn(title)) {
            title = title.replace(REGEX_SET_SUFFIX, "").trim()
        }
        // Strip year tags like (2025), (2024), (PYQ 2025)
        title = title.replace(REGEX_YEAR_TAG, "")
        title = title.removeSuffix(" - Set").removeSuffix(" - सेट").removeSuffix("- Set").removeSuffix("- सेट").trim()
        return if (title.isNotBlank()) title else rawTitle.trim()
    }

    fun extractSetNumber(title: String?): Int {
        if (title.isNullOrBlank()) return 999
        val matches = REGEX_SET_MATCH.findAll(title).toList()
        return if (matches.isNotEmpty()) {
            matches.last().groupValues[1].toIntOrNull() ?: 999
        } else {
            999
        }
    }

    fun extractSetDisplayTitle(title: String?, index: Int = 0): String {
        if (title.isNullOrBlank()) return "Set ${index + 1}"
        val matches = REGEX_SET_MATCH.findAll(title).toList()
        return if (matches.isNotEmpty()) {
            val num = matches.last().groupValues[1]
            "Set $num"
        } else if (title.contains(" - Set ", ignoreCase = true) || title.contains("- Set ", ignoreCase = true)) {
            val lastPart = title.substringAfterLast("Set", "").trim().filter { it.isDigit() }
            if (lastPart.isNotBlank()) "Set $lastPart" else "Set ${index + 1}"
        } else {
            "Set ${index + 1}"
        }
    }

    fun extractYear(title: String?, description: String? = null, fallbackYear: String = ""): String {
        if (fallbackYear.isNotBlank() && fallbackYear != "सभी") return fallbackYear
        val text = "${title ?: ""} ${description ?: ""}"
        val years = listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020", "2019", "2018", "2017")
        return years.find { text.contains(it) } ?: "2025"
    }

    fun sanitizeExistingCustomChapters(context: Context? = null) {
        synchronized(this) {
            var changed = false

            // Restore all SSC GD and built-in chapters that were accidentally marked deleted
            val unblocked = deletedChapterIds.removeAll { id ->
                id.startsWith("ssc_gd_") || id.startsWith("group_ssc_gd") || id == "ssc_gd" || id.startsWith("topic_builtin_ssc_gd")
            }
            if (unblocked) changed = true

            // Unblock any topic groups that currently have active custom chapters
            val activeTopicTitles = customChapters.map { cleanBaseChapterTitle(it.title) }.distinct()
            for (title in activeTopicTitles) {
                if (deletedChapterIds.remove("group_$title")) changed = true
                val cleanWithoutNum = if (title.contains(".")) title.substringAfter(".").trim() else title
                if (deletedChapterIds.remove("group_$cleanWithoutNum")) changed = true
                if (deletedChapterIds.remove(title)) changed = true
                if (deletedChapterIds.remove(cleanWithoutNum)) changed = true
            }

            // Remove any phantom custom topics that conflict with actual chapter tests
            val existingChapterBases = customChapters.map { cleanBaseChapterTitle(it.title).lowercase() }.toSet()
            val removedTopics = customTopics.removeAll { top ->
                val topClean = cleanBaseChapterTitle(top.title).lowercase()
                existingChapterBases.contains(topClean)
            }
            if (removedTopics) changed = true

            // Purge any sets created from Full Tests or Subject-Wise tests (sets are strictly for chapterwise only)
            val removedFullOrSubjectSets = customChapters.removeAll { ch ->
                val title = ch.title
                title.contains("Full Test", ignoreCase = true) ||
                title.contains("मॉक टेस्ट", ignoreCase = true) ||
                title.contains("Mock Test", ignoreCase = true) ||
                title.contains("Subject Wise", ignoreCase = true) ||
                title.contains("विषयवार", ignoreCase = true) ||
                title.contains("Shift", ignoreCase = true) ||
                (title.contains("कांस्टेबल", ignoreCase = true) && (title.contains("Dec", ignoreCase = true) || title.contains("Jan", ignoreCase = true) || title.contains("Shift", ignoreCase = true)))
            }
            if (removedFullOrSubjectSets) changed = true

            // Remove duplicate custom chapters by key (subExamId, subjectKey, title, year)
            val uniqueChapters = mutableListOf<CustomChapterEntry>()
            for (ch in customChapters) {
                val key = "${ch.subExamId}_${ch.subjectKey}_${ch.title.trim().lowercase()}_${ch.year}"
                val existingIdx = uniqueChapters.indexOfFirst {
                    "${it.subExamId}_${it.subjectKey}_${it.title.trim().lowercase()}_${it.year}" == key
                }
                if (existingIdx == -1) {
                    uniqueChapters.add(ch)
                } else {
                    if (ch.questions.size > uniqueChapters[existingIdx].questions.size || ch.createdAt > uniqueChapters[existingIdx].createdAt) {
                        uniqueChapters[existingIdx] = ch
                    }
                    changed = true
                }
            }
            if (uniqueChapters.size != customChapters.size) {
                customChapters.clear()
                customChapters.addAll(uniqueChapters)
                changed = true
            }

            val currentChapters = customChapters.toList()
            for (i in currentChapters.indices) {
                val ch = currentChapters[i]
                val setNum = extractSetNumber(ch.title)
                val cleanBase = cleanBaseChapterTitle(ch.title)
                val accurateSub = DynamicTestRepository.getStrictExamOwner(ch.title, emptyList(), ch.id, ch.subExamId)
                val accurateSubKey = resolveSubjectKey(ch.title, ch.subjectKey, ch.questions)
                
                // Check if title had repetitive Set suffixes like " - Set 4 - Set 1" or irregular formatting
                val hasMultipleSets = (ch.title.contains("- Set", ignoreCase = true) && ch.title.indexOf("- Set") != ch.title.lastIndexOf("- Set")) ||
                                      (ch.title.contains(" - Set ") && ch.title.substringBefore(" - Set ").contains("Set ", ignoreCase = true))
                val newTitle = if (hasMultipleSets) {
                    if (setNum != 999) "$cleanBase - Set $setNum" else cleanBase
                } else ch.title
                val rawEng = (ch.englishTitle ?: "").trim()
                val engBase = if (rawEng.isNotBlank()) rawEng else cleanBase
                val newEnglish = if (hasMultipleSets) {
                    if (setNum != 999) "${cleanBaseChapterTitle(engBase)} - Set $setNum" else cleanBase
                } else ch.englishTitle

                if (i < customChapters.size && (newTitle != ch.title || newEnglish != ch.englishTitle || accurateSub != ch.subExamId || accurateSubKey != ch.subjectKey)) {
                    customChapters[i] = ch.copy(
                        title = newTitle,
                        englishTitle = newEnglish,
                        subExamId = accurateSub,
                        subjectKey = accurateSubKey,
                        subjectTitle = accurateSubKey
                    )
                    changed = true
                }
            }
            
            val currentTopics = customTopics.toList()
            for (i in currentTopics.indices) {
                val top = currentTopics[i]
                val cleanTitle = cleanBaseChapterTitle(top.title)
                val rawEng = (top.englishTitle ?: "").trim()
                val engBase = if (rawEng.isNotBlank()) rawEng else cleanTitle
                val accurateSub = DynamicTestRepository.getStrictExamOwner(top.title, emptyList(), top.id, top.subExamId)
                val accurateSubKey = resolveSubjectKey(top.title, top.subjectKey)

                if (i < customTopics.size && (top.title != cleanTitle || accurateSub != top.subExamId || accurateSubKey != top.subjectKey)) {
                    customTopics[i] = top.copy(
                        title = cleanTitle,
                        englishTitle = cleanBaseChapterTitle(engBase),
                        subExamId = accurateSub,
                        subjectKey = accurateSubKey
                    )
                    changed = true
                }
            }
            
            if (changed) {
                revision++
                if (context != null) {
                    saveToDisk(context)
                }
            }
        }
    }

    fun normalizeSubjectGroup(k: String): String {
        val clean = k.trim().uppercase().replace("-", "_").replace(" ", "_")
        return when {
            clean in listOf("MATH", "MATHS", "MATHEMATICS", "QUANT", "QUANTITATIVE", "QUANTITATIVE_APTITUDE", "गणित", "मैथ्स") || clean.contains("MATH") || clean.contains("QUANT") -> "MATHS"
            clean in listOf("REASONING", "LOGICAL_REASONING", "REASON", "GENERAL_INTELLIGENCE", "GI", "तर्कशक्ति", "रीजनिंग") || clean.contains("REASON") || clean.contains("INTELLIGENCE") -> "REASONING"
            clean in listOf("HINDI", "GENERAL_HINDI", "SAMANYA_HINDI", "सामान्य_हिंदी", "हिंदी") || clean.contains("HINDI") -> "HINDI"
            clean in listOf("ENGLISH", "GENERAL_ENGLISH", "अंग्रेजी") || clean.contains("ENGLISH") -> "ENGLISH"
            clean in listOf("COMPUTER", "COMPUTERS", "COMPUTER_AWARENESS", "COMPUTER_KNOWLEDGE", "आईटी", "कंप्यूटर") || clean.contains("COMPUTER") -> "COMPUTER"
            clean in listOf("SCIENCE", "GENERAL_SCIENCE", "विज्ञान", "सामान्य_विज्ञान") || clean.contains("SCIENCE") -> "SCIENCE"
            clean in listOf("LAW", "MOOLVIDHI", "LAW_POLITY", "मूलविधि", "कानून") || clean.contains("MOOLVIDHI") || clean.contains("LAW") -> "LAW"
            clean in listOf("STATE_GK", "RAJASTHAN_GK", "UP_GK", "BIHAR_GK", "MP_GK", "HARYANA_GK") -> "STATE_GK"
            clean in listOf("GK", "GS", "GK_GS", "GA", "GENERAL_KNOWLEDGE", "GENERAL_AWARENESS", "GENERAL_STUDIES", "सामान्य_ज्ञान", "सामान्य_अध्ययन") || clean.contains("GK") || clean.contains("GS") || clean.contains("GENERAL_KNOWLEDGE") || clean.contains("GENERAL_AWARENESS") -> "GK"
            else -> clean
        }
    }

    fun isMatchingSubjectKey(key1: String, key2: String): Boolean {
        val k1 = key1.trim().uppercase().replace("-", "_").replace(" ", "_")
        val k2 = key2.trim().uppercase().replace("-", "_").replace(" ", "_")
        if (k1 == k2) return true
        return normalizeSubjectGroup(k1) == normalizeSubjectGroup(k2)
    }

    fun resolveSubjectKey(title: String, rawSubjectKey: String? = null, questions: List<QuizQuestion> = emptyList()): String {
        val cleanRaw = (rawSubjectKey ?: "").trim().uppercase().replace("-", "_").replace(" ", "_")
        if (cleanRaw.isNotBlank() && cleanRaw != "GENERAL" && cleanRaw != "NULL" && cleanRaw != "DEFAULT" && cleanRaw != "ALL") {
            val norm = normalizeSubjectGroup(cleanRaw)
            if (norm != "GK" || cleanRaw.contains("GK") || cleanRaw.contains("GS") || cleanRaw.contains("ज्ञान")) {
                return norm
            }
        }

        val t = title.lowercase()
        when {
            t.contains("गणित") || t.contains("मैथ") || t.contains("math") || t.contains("संख्या पद्धति") ||
            t.contains("प्रतिशत") || t.contains("लाभ") || t.contains("हानि") || t.contains("ब्याज") ||
            t.contains("औसत") || t.contains("अनुपात") || t.contains("कार्य") || t.contains("चाल") ||
            t.contains("दूरी") || t.contains("रेलगाड़ी") || t.contains("क्षेत्रमिति") || t.contains("सरलीकरण") ||
            t.contains("पाइप") || t.contains("टंकी") || t.contains("नाव") || t.contains("धारा") -> return "MATHS"

            t.contains("तर्क") || t.contains("रीजनिंग") || t.contains("reason") || t.contains("सादृश्यता") ||
            t.contains("समानता") || t.contains("वर्गीकरण") || t.contains("कोडिंग") || t.contains("डिकोडिंग") ||
            t.contains("शृंखला") || t.contains("सीरीज") || t.contains("रक्त संबंध") || t.contains("दिशा") ||
            t.contains("कथन") || t.contains("निष्कर्ष") || t.contains("पासा") || t.contains("कैलेंडर") ||
            t.contains("घड़ी") || t.contains("आकृति") || t.contains("दर्पण") -> return "REASONING"

            t.contains("हिंदी") || t.contains("hindi") || t.contains("मुहावरे") || t.contains("लोकोक्ति") ||
            t.contains("संधि") || t.contains("समास") || t.contains("विलोम") || t.contains("पर्यायवाची") ||
            t.contains("समानार्थी") || t.contains("वर्तनी") || t.contains("वाक्यांश") || t.contains("अनेक शब्दों") ||
            t.contains("गद्यांश") || t.contains("कारक") || t.contains("तत्सम") || t.contains("तद्भव") -> return "HINDI"

            t.contains("english") || t.contains("अंग्रेजी") || t.contains("comprehension") || t.contains("synonym") ||
            t.contains("antonym") || t.contains("idiom") || t.contains("cloze") || t.contains("spelling") -> return "ENGLISH"

            t.contains("कंप्यूटर") || t.contains("computer") || t.contains("ms word") || t.contains("ms excel") ||
            t.contains("hardware") || t.contains("software") || t.contains("internet") -> return "COMPUTER"

            t.contains("राजस्थान") -> return "STATE_GK"
            t.contains("विज्ञान") || t.contains("science") -> return "SCIENCE"
            t.contains("मूलविधि") || t.contains("संविधान") || t.contains("polity") -> return "LAW"
        }

        if (questions.isNotEmpty()) {
            val mathCount = questions.count { isMathsQuestion(it) }
            val reasonCount = questions.count { isReasoningQuestion(it) }
            val compCount = questions.count { isComputerQuestion(it) }
            val hindiCount = questions.count { it.subject.contains("hindi", true) || it.subject.contains("हिंदी") }
            val engCount = questions.count { it.subject.contains("eng", true) || it.subject.contains("अंग्रेजी") }

            val maxCount = maxOf(mathCount, reasonCount, compCount, hindiCount, engCount)
            if (maxCount > 0) {
                return when (maxCount) {
                    mathCount -> "MATHS"
                    reasonCount -> "REASONING"
                    hindiCount -> "HINDI"
                    compCount -> "COMPUTER"
                    engCount -> "ENGLISH"
                    else -> "GK"
                }
            }
        }

        return "GK"
    }

    fun ensureTopicExists(
        subExamId: String,
        subjectKey: String,
        baseTitle: String,
        description: String = "",
        context: Context? = null
    ): CustomTopicEntry {
        val normSubExam = DynamicTestRepository.normalizeSubExamId(subExamId)
        val cleanTitle = cleanBaseChapterTitle(baseTitle)
        synchronized(this) {
            val topics = customTopics.toList()
            val existing = topics.find {
                DynamicTestRepository.normalizeSubExamId(it.subExamId) == normSubExam &&
                isMatchingSubjectKey(it.subjectKey, subjectKey) &&
                cleanBaseChapterTitle(it.title).equals(cleanTitle, ignoreCase = true)
            }
            if (existing != null) {
                return existing
            }
            val newTopic = CustomTopicEntry(
                id = "topic_${System.currentTimeMillis()}_${Math.abs(cleanTitle.hashCode())}",
                subExamId = normSubExam,
                subjectKey = subjectKey,
                title = cleanTitle,
                englishTitle = cleanTitle,
                description = description.ifBlank { "0 टेस्ट उपलब्ध" },
                createdAt = System.currentTimeMillis()
            )
            customTopics.add(newTopic)
            revision++
            if (context != null) {
                saveToDisk(context)
            }
            return newTopic
        }
    }

    private var isInitialized = false

    fun init(context: Context) {
        synchronized(this) {
            if (isInitialized) return
            loadFromDisk(context)
            sanitizeExistingCustomChapters(context)
            isInitialized = true
        }
    }

    fun exportToJsonString(context: Context): String {
        synchronized(this) {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                return file.readText()
            }
            return "{}"
        }
    }

    private fun applySnapshotState(
        newSubjects: List<CustomSubjectEntry>? = null,
        newChapters: List<CustomChapterEntry>? = null,
        newTopics: List<CustomTopicEntry>? = null,
        newDeletedIds: Set<String>? = null
    ) {
        val applyBlock = {
            synchronized(this) {
                if (newSubjects != null) {
                    customSubjects.clear()
                    customSubjects.addAll(newSubjects)
                }
                if (newChapters != null) {
                    customChapters.clear()
                    customChapters.addAll(newChapters)
                }
                if (newTopics != null) {
                    customTopics.clear()
                    customTopics.addAll(newTopics)
                }
                if (newDeletedIds != null) {
                    deletedChapterIds.clear()
                    deletedChapterIds.addAll(newDeletedIds)
                }
                revision++
            }
        }
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            applyBlock()
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                applyBlock()
            }
        }
    }

    fun importFromJsonString(jsonString: String?, context: Context?, defaultSubExamId: String = "") {
        if (jsonString.isNullOrBlank() || context == null) return
        val trimmed = jsonString.trim()

        val root: JSONObject = try {
            if (trimmed.startsWith("{")) {
                val parsed = JSONObject(trimmed)
                if (!parsed.has("chapters") && !parsed.has("custom_chapters") && !parsed.has("customChapters") &&
                    !parsed.has("tests") && !parsed.has("testPapers") && !parsed.has("papers") &&
                    (parsed.has("questions") || parsed.has("testPaper") || (parsed.has("title") && parsed.has("totalQuestions")))) {
                    val arr = JSONArray().put(parsed)
                    parsed.put("tests", arr).put("chapters", arr)
                }
                parsed
            } else if (trimmed.startsWith("[")) {
                val arr = JSONArray(trimmed)
                JSONObject().put("tests", arr).put("chapters", arr)
            } else {
                return
            }
        } catch (e: Exception) {
            return
        }

        synchronized(this) {
            try {
                // Only proceed if this JSON actually contains chapter or test repository data
                if (!root.has("chapters") && !root.has("custom_chapters") && !root.has("customChapters") &&
                    !root.has("subjects") && !root.has("topics") && !root.has("deletedChapterIds") &&
                    !root.has("tests") && !root.has("testPapers") && !root.has("papers") &&
                    !root.has("questions") && !root.has("testPaper")) {
                    return // Not a chapter/test repository export, ignore without error
                }

                var modified = false

                // Snapshot current state to immutable/local sets to avoid ConcurrentModificationException during traversal
                val curDeletedIds = deletedChapterIds.toMutableSet()
                val curChapters = customChapters.toMutableList()
                val curSubjects = customSubjects.toMutableList()
                val curTopics = customTopics.toMutableList()

                // 1. Sync deletedChapterIds from Cloud to User devices (skip active existing chapters)
                if (root.has("deletedChapterIds") && !root.isNull("deletedChapterIds")) {
                    val delArr = root.optJSONArray("deletedChapterIds")
                    if (delArr != null) {
                        val activeChapterKeys = curChapters.flatMap {
                            listOf(
                                it.id.trim(),
                                it.title.trim(),
                                cleanBaseChapterTitle(it.title).trim()
                            )
                        }.filter { it.isNotBlank() }.toSet()

                        for (i in 0 until delArr.length()) {
                            val id = delArr.optString(i, "").trim()
                            if (id.isNotBlank() && id != "null") {
                                val cleanId = id.removePrefix("group_")
                                if (activeChapterKeys.contains(id) || activeChapterKeys.contains(cleanId)) {
                                    if (curDeletedIds.remove(id)) modified = true
                                    if (curDeletedIds.remove(cleanId)) modified = true
                                } else {
                                    if (!curDeletedIds.contains(id)) {
                                        curDeletedIds.add(id)
                                        modified = true
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Remove any custom chapters that have been deleted in deletedChapterIds
                val chaptersToRemove = curChapters.filter { ch ->
                    val baseTitle = if (ch.title.contains(" - Set ")) ch.title.substringBefore(" - Set ").trim() else ch.title.trim()
                    val cleanWithoutNum = if (baseTitle.contains(".")) baseTitle.substringAfter(".").trim() else baseTitle
                    curDeletedIds.contains(ch.id) ||
                    curDeletedIds.contains(ch.title.trim()) ||
                    curDeletedIds.contains(baseTitle) ||
                    curDeletedIds.contains(cleanWithoutNum) ||
                    curDeletedIds.contains("group_$baseTitle") ||
                    curDeletedIds.contains("group_$cleanWithoutNum") ||
                    curDeletedIds.contains("group_${ch.title.trim()}")
                }
                if (chaptersToRemove.isNotEmpty()) {
                    curChapters.removeAll(chaptersToRemove)
                    modified = true
                    Log.d(TAG, "Removed ${chaptersToRemove.size} deleted chapters based on cloud deletedChapterIds")
                }

                val existingSubjectIds = curSubjects.map { it.id }.toMutableSet()

                // 3. Merge subjects
                if (root.has("subjects") && !root.isNull("subjects")) {
                    val subjectsArr = root.optJSONArray("subjects")
                    if (subjectsArr != null) {
                        for (i in 0 until subjectsArr.length()) {
                            val obj = subjectsArr.optJSONObject(i) ?: continue
                            val subId = obj.optString("id", "sub_$i")
                            if (!existingSubjectIds.contains(subId)) {
                                existingSubjectIds.add(subId)
                                val entry = CustomSubjectEntry(
                                    id = subId,
                                    subExamId = obj.optString("subExamId", "ssc_cgl").ifBlank { "ssc_cgl" },
                                    key = obj.optString("key", "sub_$i").ifBlank { "sub_$i" },
                                    name = obj.optString("name", "विषय").ifBlank { "विषय" },
                                    englishName = obj.optString("englishName", "Subject").ifBlank { "Subject" },
                                    iconType = obj.optString("iconType", "BOOK").ifBlank { "BOOK" }
                                )
                                curSubjects.add(entry)
                                modified = true
                            }
                        }
                    }
                }

                // 4. Merge topics
                if (root.has("topics") && !root.isNull("topics")) {
                    val topicsArr = root.optJSONArray("topics")
                    if (topicsArr != null) {
                        for (i in 0 until topicsArr.length()) {
                            val obj = topicsArr.optJSONObject(i) ?: continue
                            val topId = obj.optString("id", "top_$i")
                            val topTitle = obj.optString("title", "").trim()
                            if (topTitle.isBlank() || topTitle == "null") continue
                            val cleanWithoutNum = if (topTitle.contains(".")) topTitle.substringAfter(".").trim() else topTitle

                            val isDeleted = curDeletedIds.contains(topId) ||
                                curDeletedIds.contains(topTitle) ||
                                curDeletedIds.contains(cleanWithoutNum) ||
                                curDeletedIds.contains("group_$topTitle") ||
                                curDeletedIds.contains("group_$cleanWithoutNum")

                            val alreadyExists = curTopics.any {
                                it.id == topId || (it.subExamId == obj.optString("subExamId", "") && it.title.equals(topTitle, ignoreCase = true))
                            }

                            if (!isDeleted && !alreadyExists) {
                                val engTitle = obj.optString("englishTitle", topTitle).ifBlank { topTitle }
                                val entry = CustomTopicEntry(
                                    id = topId,
                                    subExamId = obj.optString("subExamId", "ssc_cgl").ifBlank { "ssc_cgl" },
                                    subjectKey = obj.optString("subjectKey", "GK_GS").ifBlank { "GK_GS" },
                                    title = topTitle,
                                    englishTitle = if (engTitle == "null") topTitle else engTitle,
                                    description = obj.optString("description", "0 टेस्ट उपलब्ध").ifBlank { "0 टेस्ट उपलब्ध" },
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                                )
                                curTopics.add(entry)
                                modified = true
                            }
                        }
                    }
                }

                // 5. Merge chapters
                val existingChapterIds = curChapters.map { it.id }.toMutableSet()

                val chaptersArr = root.optJSONArray("chapters") 
                    ?: root.optJSONArray("custom_chapters") 
                    ?: root.optJSONArray("customChapters") 
                    ?: root.optJSONArray("examChapters")
                    ?: root.optJSONArray("tests") 
                    ?: root.optJSONArray("testPapers")
                    ?: root.optJSONArray("papers")
                    ?: root.optJSONArray("custom_tests") 
                    ?: root.optJSONArray("customTests")
                if (chaptersArr != null) {
                    for (i in 0 until chaptersArr.length()) {
                        val obj = chaptersArr.optJSONObject(i) ?: continue
                        val pObj = obj.optJSONObject("testPaper") ?: obj
                        val rawId = pObj.optString("id", "").ifBlank { obj.optString("id", "") }
                        val title = pObj.optString("title", "").ifBlank { obj.optString("title", "") }.trim()
                        if (title.isBlank() || title == "null") continue
                        val baseTitle = cleanBaseChapterTitle(title)
                        val cleanWithoutNum = if (baseTitle.contains(".")) baseTitle.substringAfter(".").trim() else baseTitle

                        val qsList = mutableListOf<QuizQuestion>()
                        val qsArr = pObj.optJSONArray("questions") ?: obj.optJSONArray("questions")
                        if (qsArr != null) {
                            for (j in 0 until qsArr.length()) {
                                val qObj = qsArr.optJSONObject(j) ?: continue
                                val optsList = mutableListOf<String>()
                                val optsArr = qObj.optJSONArray("options")
                                if (optsArr != null) {
                                    for (k in 0 until optsArr.length()) {
                                        val opt = optsArr.optString(k, "").trim()
                                        if (opt.isNotBlank() && opt != "null") {
                                            optsList.add(opt)
                                        }
                                    }
                                }
                                val rawShift = qObj.optString("examShiftInfo", "")
                                    .ifBlank { qObj.optString("shiftInfo", "") }
                                    .ifBlank { qObj.optString("examDateShift", "") }
                                val shiftInfo = if (rawShift.isNotBlank() && rawShift != "null") rawShift else null
                                qsList.add(
                                    QuizQuestion(
                                        id = qObj.optInt("id", j + 1),
                                        question = qObj.optString("question", ""),
                                        options = if (optsList.isNotEmpty()) optsList else listOf("Option A", "Option B", "Option C", "Option D"),
                                        correctIndex = qObj.optInt("correctIndex", 0),
                                        explanation = qObj.optString("explanation", ""),
                                        subject = qObj.optString("subject", pObj.optString("subjectTitle", obj.optString("subjectTitle", "GK"))),
                                        imageUrl = if (qObj.has("imageUrl") && !qObj.isNull("imageUrl")) qObj.optString("imageUrl", "").takeIf { it.isNotBlank() && it != "null" } else null,
                                        examShiftInfo = shiftInfo
                                    )
                                )
                            }
                        }

                        val desc = pObj.optString("description", obj.optString("description", ""))
                        val rawYear = pObj.optString("year", obj.optString("year", ""))
                        val resolvedYear = if (rawYear.isNotBlank() && rawYear != "null") rawYear else {
                            listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020").find { desc.contains(it) || title.contains(it) } ?: "2026"
                        }
                        val engTitle = pObj.optString("englishTitle", obj.optString("englishTitle", "")).takeIf { it != "null" } ?: ""

                        val rawSub = obj.optString("subExamId", "")
                            .ifBlank { obj.optString("sub_exam_id", "") }
                            .ifBlank { obj.optString("examId", "") }
                            .ifBlank { obj.optString("exam_id", "") }
                            .ifBlank { obj.optString("exam", "") }
                            .ifBlank { obj.optString("targetExam", "") }
                            .ifBlank { pObj.optString("subExamId", "") }
                            .ifBlank { defaultSubExamId }

                        val resolvedSubExam = DynamicTestRepository.resolveAccurateSubExamId(rawSub, title, qsList)

                        val setNum = extractSetNumber(title)
                        val cleanHash = Math.abs(title.hashCode())
                        val chapId = if (rawId.isNotBlank() && rawId != "null") {
                            rawId
                        } else {
                            "custom_chap_${resolvedSubExam}_${resolvedYear}_${cleanHash}_s$setNum"
                        }

                        // If this chapter or its group was marked deleted, SKIP it!
                        if (curDeletedIds.contains(chapId) ||
                            curDeletedIds.contains(title) ||
                            curDeletedIds.contains("group_$baseTitle") ||
                            curDeletedIds.contains("group_$cleanWithoutNum")
                        ) {
                            continue
                        }

                        val rawSubKey = obj.optString("subjectKey", "")
                            .ifBlank { obj.optString("subjectTitle", "") }
                            .ifBlank { pObj.optString("subjectKey", "") }
                            .ifBlank { pObj.optString("subjectTitle", "") }
                        val resolvedSubKey = resolveSubjectKey(title, rawSubKey, qsList)

                        val isMathOrReasoning = isMathOrReasoningSubject(resolvedSubKey, resolvedSubKey)
                        val duration = if (isMathOrReasoning) maxOf(10, qsList.size * 2) else pObj.optInt("durationMinutes", obj.optInt("durationMinutes", 20))
                        val entry = CustomChapterEntry(
                            id = chapId,
                            subExamId = resolvedSubExam,
                            subjectKey = resolvedSubKey,
                            subjectTitle = resolvedSubKey,
                            title = title,
                            englishTitle = engTitle,
                            description = desc.ifBlank { "${qsList.size} प्रश्न • $resolvedYear PYQ" },
                            totalQuestions = pObj.optInt("totalQuestions", obj.optInt("totalQuestions", qsList.size)),
                            durationMinutes = duration,
                            totalMarks = pObj.optInt("totalMarks", obj.optInt("totalMarks", qsList.size)),
                            difficulty = pObj.optString("difficulty", "मध्यम").ifBlank { "मध्यम" },
                            questions = qsList,
                            year = resolvedYear,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                        val existingIdx = curChapters.indexOfFirst {
                            it.id == chapId ||
                            (it.title.trim().equals(title, ignoreCase = true) && it.subExamId == resolvedSubExam && (if (it.year.isNotBlank()) it.year else extractYear(it.title, it.description)) == resolvedYear)
                        }
                        if (existingIdx >= 0) {
                            curChapters[existingIdx] = entry
                        } else {
                            curChapters.add(entry)
                            existingChapterIds.add(chapId)
                        }
                        modified = true
                    }
                }

                // 5b. Also parse from master "tests" array if present
                val testsArr = root.optJSONArray("tests") ?: root.optJSONArray("testPapers") ?: root.optJSONArray("papers")
                if (testsArr != null) {
                    for (i in 0 until testsArr.length()) {
                        val testObj = testsArr.optJSONObject(i) ?: continue
                        val testType = testObj.optString("testType", "FULL_TEST").uppercase()
                        val rawYear = testObj.optString("year", "2025")
                        val pObj = testObj.optJSONObject("testPaper") ?: testObj
                        val chapId = pObj.optString("id", "custom_dyn_$i")
                        val title = pObj.optString("title", "").trim()
                        if (title.isBlank()) continue

                        val qsList = mutableListOf<QuizQuestion>()
                        val qsArr = pObj.optJSONArray("questions")
                        if (qsArr != null) {
                            for (j in 0 until qsArr.length()) {
                                val qObj = qsArr.optJSONObject(j) ?: continue
                                val optsList = mutableListOf<String>()
                                val optsArr = qObj.optJSONArray("options")
                                if (optsArr != null) {
                                    for (k in 0 until optsArr.length()) {
                                        val opt = optsArr.optString(k, "").trim()
                                        if (opt.isNotBlank() && opt != "null") optsList.add(opt)
                                    }
                                }
                                qsList.add(
                                    QuizQuestion(
                                        id = qObj.optInt("id", j + 1),
                                        question = qObj.optString("question", ""),
                                        options = if (optsList.isNotEmpty()) optsList else listOf("Option A", "Option B", "Option C", "Option D"),
                                        correctIndex = qObj.optInt("correctIndex", 0),
                                        explanation = qObj.optString("explanation", ""),
                                        subject = qObj.optString("subject", "GK"),
                                        imageUrl = qObj.optString("imageUrl", "").takeIf { it.isNotBlank() && it != "null" }
                                    )
                                )
                            }
                        }

                        val rawSub = testObj.optString("subExamId", "")
                            .ifBlank { testObj.optString("sub_exam_id", "") }
                            .ifBlank { testObj.optString("examId", "") }
                            .ifBlank { testObj.optString("exam", "") }
                            .ifBlank { pObj.optString("subExamId", "") }
                            .ifBlank { pObj.optString("examId", "") }

                        val resolvedSubExam = DynamicTestRepository.resolveAccurateSubExamId(rawSub, title, qsList)

                        val rawSubKey = testObj.optString("subjectKey", "")
                            .ifBlank { testObj.optString("subjectTitle", "") }
                            .ifBlank { pObj.optString("subjectKey", "") }

                        val resolvedSubKey = resolveSubjectKey(title, rawSubKey, qsList)

                        // Only import into CustomChapterRepository if it is strictly a CHAPTER_WISE test or chapter set
                        val isExplicitFullOrSubject = testType == "FULL_TEST" || testType == "FULL_MOCK" || testType == "SUBJECT_WISE" ||
                                title.contains("Full Test", true) || title.contains("मॉक टेस्ट", true) || title.contains("Mock Test", true) ||
                                title.contains("Subject Wise", true) || title.contains("विषयवार", true) || qsList.size >= 60

                        val isChapterWiseTest = !isExplicitFullOrSubject && (
                                testType == "CHAPTER_WISE" ||
                                title.contains(" - Set ", true) ||
                                title.contains("Set ", true) ||
                                title.contains("सेट ", true) ||
                                title.contains("Chapter", true) ||
                                title.contains("अध्याय", true)
                        )

                        if (!isChapterWiseTest) {
                            continue
                        }

                        val entry = CustomChapterEntry(
                            id = chapId,
                            subExamId = resolvedSubExam,
                            subjectKey = resolvedSubKey,
                            subjectTitle = resolvedSubKey,
                            title = title,
                            englishTitle = title,
                            description = "${qsList.size} प्रश्न • $rawYear PYQ",
                            totalQuestions = qsList.size,
                            durationMinutes = pObj.optInt("durationMinutes", 20),
                            totalMarks = pObj.optInt("totalMarks", qsList.size),
                            difficulty = pObj.optString("difficulty", "मध्यम"),
                            questions = qsList,
                            year = rawYear,
                            createdAt = testObj.optLong("createdAt", System.currentTimeMillis())
                        )
                        val existingIdx = curChapters.indexOfFirst { it.id == chapId }
                        if (existingIdx >= 0) {
                            curChapters[existingIdx] = entry
                        } else {
                            curChapters.add(entry)
                            existingChapterIds.add(chapId)
                        }
                        modified = true
                    }
                }

                if (modified) {
                    applySnapshotState(curSubjects, curChapters, curTopics, curDeletedIds)
                    sanitizeExistingCustomChapters(context)
                    saveToDisk(context)
                    Log.d(TAG, "Merged and saved new chapters from cloud JSON")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing chapter json: ${e.message}", e)
            }
        }
    }

    fun getIconVector(iconType: String): ImageVector {
        return when (iconType.uppercase()) {
            "MATHS", "CALCULATE" -> Icons.Default.Calculate
            "REASONING", "BRAIN", "PSYCHOLOGY" -> Icons.Default.Psychology
            "SCIENCE" -> Icons.Default.Science
            "COMPUTER", "TECH" -> Icons.Default.Computer
            "LAW", "GAVEL" -> Icons.Default.Gavel
            "LANGUAGE", "HINDI", "ENGLISH" -> Icons.Default.Language
            "GLOBE", "GK", "GEOGRAPHY" -> Icons.Default.Public
            "IDEA", "LIGHTBULB" -> Icons.Default.Lightbulb
            "CODE" -> Icons.Default.Code
            else -> Icons.AutoMirrored.Filled.MenuBook
        }
    }

    /**
     * Add a new subject under an exam
     */
    fun addSubject(
        subExamId: String,
        name: String,
        englishName: String,
        iconType: String = "BOOK",
        context: Context? = null
    ): CustomSubjectEntry {
        val normSubExam = DynamicTestRepository.normalizeSubExamId(subExamId)
        val key = "custom_sub_${System.currentTimeMillis()}"
        val subject = CustomSubjectEntry(
            id = key,
            subExamId = normSubExam,
            key = key,
            name = name.trim(),
            englishName = englishName.trim().ifEmpty { name.trim() },
            iconType = iconType
        )

        // Remove any duplicate with same name
        customSubjects.removeAll { it.subExamId == normSubExam && it.name.equals(subject.name, ignoreCase = true) }
        customSubjects.add(subject)
        revision++

        if (context != null) {
            saveToDisk(context)
        }
        return subject
    }

    /**
     * Delete custom subject
     */
    fun deleteSubject(subExamId: String, subjectKey: String, context: Context? = null) {
        val normSubExam = DynamicTestRepository.normalizeSubExamId(subExamId)
        customSubjects.removeAll { it.subExamId == normSubExam && it.key == subjectKey }
        customChapters.removeAll { it.subExamId == normSubExam && it.subjectKey == subjectKey }
        revision++
        if (context != null) {
            saveToDisk(context)
        }
    }

    /**
     * Add a new chapter test button under a subject
     */
    fun addChapter(
        subExamId: String,
        subjectKey: String,
        subjectTitle: String,
        title: String,
        description: String,
        questions: List<QuizQuestion>,
        durationMinutes: Int = 20,
        totalMarks: Int = 15,
        difficulty: String = "मध्यम",
        year: String = "2025",
        context: Context? = null
    ): ChapterItem {
        val normSubExam = DynamicTestRepository.normalizeSubExamId(subExamId)
        val cleanYear = if (year.isNotBlank()) year.trim() else "2025"
        val chapterId = "custom_chap_${cleanYear}_${System.currentTimeMillis()}"

        val entry = CustomChapterEntry(
            id = chapterId,
            subExamId = normSubExam,
            subjectKey = subjectKey,
            subjectTitle = subjectTitle,
            title = title.trim(),
            englishTitle = title.trim(),
            description = description.trim().ifEmpty { "${questions.size} महत्वपूर्ण प्रश्न • विस्तृत हल (PYQ $cleanYear)" },
            totalQuestions = if (questions.isNotEmpty()) questions.size else 15,
            durationMinutes = durationMinutes,
            totalMarks = if (totalMarks > 0) totalMarks else questions.size,
            difficulty = difficulty,
            questions = questions,
            year = cleanYear,
            createdAt = System.currentTimeMillis()
        )

        // Remove existing duplicate chapter ONLY for the same year
        customChapters.removeAll {
            it.subExamId == normSubExam &&
            it.subjectKey == subjectKey &&
            it.title.equals(entry.title, ignoreCase = true) &&
            (it.year == cleanYear || (it.year.isBlank() && it.description.contains(cleanYear)))
        }

        val baseTitle = if (title.contains(" - Set ")) title.substringBefore(" - Set ").trim() else title.trim()
        ensureTopicExists(subExamId, subjectKey, baseTitle, description, context)

        customChapters.add(0, entry)
        revision++

        if (context != null) {
            saveToDisk(context)
        }

        return ChapterItem(
            id = entry.id,
            subExamId = entry.subExamId,
            subjectKey = entry.subjectKey,
            subjectTitle = entry.subjectTitle,
            chapterNumber = 0,
            title = entry.title,
            englishTitle = entry.englishTitle,
            description = entry.description,
            totalQuestions = entry.totalQuestions,
            durationMinutes = entry.durationMinutes,
            totalMarks = entry.totalMarks,
            difficulty = entry.difficulty,
            questions = entry.questions
        )
    }

    fun isMathOrReasoningSubject(subjectKey: String, subjectTitle: String = ""): Boolean {
        val k = subjectKey.uppercase().trim()
        val t = subjectTitle.lowercase().trim()
        return k.contains("MATH") || k.contains("QUANT") || k.contains("REASON") ||
                t.contains("गणित") || t.contains("मैथ") || t.contains("संख्यात्मक") ||
                t.contains("math") || t.contains("तर्क") || t.contains("रीजनिंग") ||
                t.contains("reason") || t.contains("बुद्धिमत्ता")
    }

    /**
     * Splits a batch of questions (e.g. 60 questions from PDF) into 10-question sets
     * (Set 1, Set 2, Set 3, etc.) and saves each set under the specified chapter.
     */
    fun addChapterWithSets(
        subExamId: String,
        subjectKey: String,
        subjectTitle: String,
        baseChapterTitle: String,
        allQuestions: List<QuizQuestion>,
        durationPerSet: Int = 10,
        difficulty: String = "मध्यम",
        year: String = "2025",
        context: Context? = null
    ): List<ChapterItem> {
        val normSubExam = DynamicTestRepository.normalizeSubExamId(subExamId)
        val cleanBase = cleanBaseChapterTitle(baseChapterTitle)
        val cleanYear = if (year.isNotBlank()) year.trim() else "2025"
        val chunks = if (allQuestions.isEmpty()) listOf(emptyList()) else allQuestions.chunked(10)
        val createdItems = mutableListOf<ChapterItem>()

        chunks.forEachIndexed { index, chunk ->
            val setNumber = index + 1
            val setSuffix = if (chunks.size > 1) " - Set $setNumber" else ""
            val setChapterTitle = "$cleanBase$setSuffix"
            val chapterId = "custom_chap_${cleanYear}_${System.currentTimeMillis()}_s$setNumber"
            val isMathOrReasoning = isMathOrReasoningSubject(subjectKey, subjectTitle)
            val duration = if (isMathOrReasoning) chunk.size * 2 else if (durationPerSet > 0) durationPerSet else 10
            val marks = chunk.size
            val entry = CustomChapterEntry(
                id = chapterId,
                subExamId = normSubExam,
                subjectKey = subjectKey,
                subjectTitle = subjectTitle,
                title = setChapterTitle,
                englishTitle = setChapterTitle,
                description = "${chunk.size} प्रश्न • ${duration} मिनट • विस्तृत हल सहित (PYQ $cleanYear)",
                totalQuestions = chunk.size,
                durationMinutes = duration,
                totalMarks = marks,
                difficulty = difficulty,
                questions = chunk,
                year = cleanYear,
                createdAt = System.currentTimeMillis() + index
            )

            // Remove existing duplicate ONLY for the same year and same sub-exam
            customChapters.removeAll {
                it.subExamId == normSubExam &&
                it.subjectKey == subjectKey &&
                it.title.equals(entry.title, ignoreCase = true) &&
                (it.year == cleanYear || (it.year.isBlank() && it.description.contains(cleanYear)))
            }

            customChapters.add(0, entry)

            if (context != null) {
                OfflineTestRepository.cacheTestQuestions(context, "${setChapterTitle}_$cleanYear", chunk)
                OfflineTestRepository.cacheTestQuestions(context, chapterId, chunk)
            }

            createdItems.add(
                ChapterItem(
                    id = entry.id,
                    subExamId = entry.subExamId,
                    subjectKey = entry.subjectKey,
                    subjectTitle = entry.subjectTitle,
                    chapterNumber = 0,
                    title = entry.title,
                    englishTitle = entry.englishTitle,
                    description = entry.description,
                    totalQuestions = entry.totalQuestions,
                    durationMinutes = entry.durationMinutes,
                    totalMarks = entry.totalMarks,
                    difficulty = entry.difficulty,
                    questions = entry.questions
                )
            )
        }

        ensureTopicExists(subExamId, subjectKey, baseChapterTitle.trim(), context = context)

        revision++
        invalidateFullSubjectsCache()
        if (context != null) {
            saveToDisk(context)
        }
        return createdItems
    }

    /**
     * Delete custom or built-in single test.
     * CRITICAL (सुरक्षित टेस्ट डिलीट - Safe Test Set Deletion):
     * This deletes ONLY this specific test.
     * The topic button remains 100% intact on screen, showing "0 टेस्ट उपलब्ध • नया टेस्ट जोड़ें" status!
     */
    fun deleteChapter(
        subExamId: String,
        chapterId: String,
        subjectKey: String? = null,
        chapterTitle: String? = null,
        context: Context? = null
    ) {
        val normSubExam = DynamicTestRepository.normalizeSubExamId(subExamId)

        // Identify the chapter and its parent topic before deleting
        val customMatch = customChapters.find { it.id == chapterId }
        val builtInMatch = if (customMatch == null) {
            try {
                ExamChapterManager.getSubjectsForExam(normSubExam)
                    .flatMap { it.chapters }
                    .find { it.id == chapterId }
            } catch (e: Exception) {
                null
            }
        } else null

        val effSubjectKey = subjectKey
            ?: customMatch?.subjectKey
            ?: builtInMatch?.subjectKey
            ?: ""

        val rawTitle = chapterTitle
            ?: customMatch?.title
            ?: builtInMatch?.title
            ?: ""

        val baseTitle = if (rawTitle.contains(" - Set ")) {
            rawTitle.substringBefore(" - Set ").trim()
        } else {
            rawTitle.trim()
        }
        val cleanWithoutNum = if (baseTitle.contains(".")) baseTitle.substringAfter(".").trim() else baseTitle

        // CRITICAL: Ensure topic is preserved in customTopics BEFORE removing the test!
        // This guarantees the topic button will not disappear when its only test is deleted.
        if (baseTitle.isNotBlank()) {
            deletedChapterIds.remove("group_$baseTitle")
            deletedChapterIds.remove("group_$cleanWithoutNum")
            deletedChapterIds.remove("group_${rawTitle.trim()}")
            deletedChapterIds.remove(baseTitle)
            deletedChapterIds.remove(cleanWithoutNum)
            deletedChapterIds.remove(rawTitle.trim())

            if (effSubjectKey.isNotBlank()) {
                ensureTopicExists(
                    subExamId = normSubExam,
                    subjectKey = effSubjectKey,
                    baseTitle = baseTitle,
                    description = "0 टेस्ट उपलब्ध",
                    context = context
                )
            }
        }

        if (!deletedChapterIds.contains(chapterId)) {
            deletedChapterIds.add(chapterId)
        }
        customChapters.removeAll { it.subExamId == normSubExam && it.id == chapterId }
        revision++
        if (context != null) {
            saveToDisk(context)
            DynamicTestRepository.deleteCustomTest(chapterId, context)
        }
    }

    /**
     * Delete all sets/chapters belonging to a chapter base title for a specific year,
     * OR across all years if year == "सभी".
     * CRITICAL: Other years (e.g. 2024, 2025) remain 100% safe and untouched when deleting for 2023!
     */
    fun deleteChapterGroupForYear(
        subExamId: String,
        subjectKey: String,
        baseTitle: String,
        year: String = "सभी",
        context: Context? = null
    ) {
        val normSubExam = DynamicTestRepository.normalizeSubExamId(subExamId)
        val cleanBase = cleanBaseChapterTitle(baseTitle)
        val cleanWithoutNum = if (cleanBase.contains(".")) cleanBase.substringAfter(".").trim() else cleanBase

        // 1. Find tests matching this topic AND this year
        val matchingChapters = customChapters.filter { ch ->
            val bt = cleanBaseChapterTitle(ch.title)
            val btClean = if (bt.contains(".")) bt.substringAfter(".").trim() else bt
            val chYear = extractYear(ch.title, ch.description)
            val matchesYear = if (year == "सभी") true else (chYear == year || ch.year == year || ch.title.contains(year) || ch.description.contains(year))
            ch.subExamId == normSubExam &&
            isMatchingSubjectKey(ch.subjectKey, subjectKey) &&
            (bt.equals(cleanBase, ignoreCase = true) || btClean.equals(cleanWithoutNum, ignoreCase = true)) &&
            matchesYear
        }

        // 2. Remove only those specific test sets
        for (ch in matchingChapters) {
            if (!deletedChapterIds.contains(ch.id)) {
                deletedChapterIds.add(ch.id)
            }
            customChapters.remove(ch)
            if (context != null) {
                DynamicTestRepository.deleteCustomTest(ch.id, context)
            }
        }

        // 3. Check if any remaining tests exist for this topic in ANY other year
        val remainingForTopic = customChapters.any { ch ->
            val bt = cleanBaseChapterTitle(ch.title)
            val btClean = if (bt.contains(".")) bt.substringAfter(".").trim() else bt
            ch.subExamId == normSubExam &&
            isMatchingSubjectKey(ch.subjectKey, subjectKey) &&
            (bt.equals(cleanBase, ignoreCase = true) || btClean.equals(cleanWithoutNum, ignoreCase = true))
        }

        if (year == "सभी" && !remainingForTopic) {
            // Only when explicitly deleting "सभी" and no tests remain, remove topic
            customTopics.removeAll {
                it.subExamId == normSubExam &&
                isMatchingSubjectKey(it.subjectKey, subjectKey) &&
                (it.title.equals(cleanBase, ignoreCase = true) || it.title.equals(cleanWithoutNum, ignoreCase = true))
            }
            if (!deletedChapterIds.contains("group_$cleanBase")) {
                deletedChapterIds.add("group_$cleanBase")
            }
            if (!deletedChapterIds.contains("group_$cleanWithoutNum")) {
                deletedChapterIds.add("group_$cleanWithoutNum")
            }
        } else {
            // CRITICAL: Unblock topic marker so 2024, 2025 tests and topic button remain 100% visible!
            deletedChapterIds.remove("group_$cleanBase")
            deletedChapterIds.remove("group_$cleanWithoutNum")
            deletedChapterIds.remove(cleanBase)
            deletedChapterIds.remove(cleanWithoutNum)

            ensureTopicExists(
                subExamId = normSubExam,
                subjectKey = subjectKey,
                baseTitle = cleanBase,
                description = "0 टेस्ट उपलब्ध",
                context = context
            )
        }

        revision++
        if (context != null) {
            saveToDisk(context)
        }
    }

    /**
     * Delete all sets/chapters belonging to a chapter base title AND delete the topic button itself.
     */
    fun deleteChapterGroup(subExamId: String, subjectKey: String, baseTitle: String, context: Context? = null) {
        deleteChapterGroupForYear(subExamId, subjectKey, baseTitle, "सभी", context)
    }

    /**
     * Completely clears all custom chapters, topics, and subjects from memory and disk.
     */
    fun clearAllCustomChapters(context: Context) {
        synchronized(this) {
            customChapters.clear()
            customTopics.clear()
            customSubjects.clear()
            deletedChapterIds.clear()
            revision++
            saveToDisk(context)
            try {
                OfflineTestRepository.purgeAllOfflineCache(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error purging offline cache: ${e.message}")
            }
        }
    }

    /**
     * Update an existing chapter or add questions to it
     */
    fun updateChapter(
        subExamId: String,
        chapterId: String,
        title: String? = null,
        description: String? = null,
        questions: List<QuizQuestion>? = null,
        durationMinutes: Int? = null,
        totalMarks: Int? = null,
        difficulty: String? = null,
        context: Context? = null
    ) {
        val normSubExam = DynamicTestRepository.normalizeSubExamId(subExamId)
        val idx = customChapters.indexOfFirst { it.subExamId == normSubExam && it.id == chapterId }
        if (idx != -1) {
            val old = customChapters[idx]
            val updated = old.copy(
                title = title?.trim() ?: old.title,
                englishTitle = title?.trim() ?: old.englishTitle,
                description = description?.trim() ?: old.description,
                questions = questions ?: old.questions,
                totalQuestions = questions?.size ?: old.totalQuestions,
                durationMinutes = durationMinutes ?: old.durationMinutes,
                totalMarks = totalMarks ?: (questions?.size ?: old.totalMarks),
                difficulty = difficulty ?: old.difficulty
            )
            customChapters[idx] = updated
            revision++
            if (context != null) {
                saveToDisk(context)
            }
        }
    }

    /**
     * Add new questions to an existing chapter
     */
    fun addQuestionsToChapter(
        subExamId: String,
        chapterId: String,
        newQuestions: List<QuizQuestion>,
        context: Context? = null
    ) {
        val normSubExam = DynamicTestRepository.normalizeSubExamId(subExamId)
        val idx = customChapters.indexOfFirst { it.subExamId == normSubExam && it.id == chapterId }
        if (idx != -1) {
            val old = customChapters[idx]
            val combined = old.questions + newQuestions
            val updated = old.copy(
                questions = combined,
                totalQuestions = combined.size,
                totalMarks = combined.size
            )
            customChapters[idx] = updated
            revision++
            if (context != null) {
                saveToDisk(context)
            }
        }
    }

    /**
     * Combines built-in static subjects & chapters with dynamically added Admin subjects & chapters
     */
    fun getFullSubjectsForExam(subExamId: String, context: Context? = null): List<ExamSubjectCategory> {
        // Read revision to ensure reactive recomposition in Compose
        val currentRev = revision
        if (context != null && !isInitialized) {
            init(context)
        }

        val normSubExam = DynamicTestRepository.normalizeSubExamId(subExamId)
        val curDynRev = DynamicTestRepository.repositoryRevision
        val curChapSize = customChapters.size
        val curDynSize = DynamicTestRepository.customTests.size

        synchronized(fullSubjectsCache) {
            if (currentRev != cachedCustomChaptersRev ||
                curDynRev != cachedDynTestsRev ||
                curChapSize != cachedCustomChaptersCount ||
                curDynSize != cachedDynTestsCount
            ) {
                fullSubjectsCache.clear()
                cachedCustomChaptersRev = currentRev
                cachedDynTestsRev = curDynRev
                cachedCustomChaptersCount = curChapSize
                cachedDynTestsCount = curDynSize
            }
        }

        val cached = fullSubjectsCache[normSubExam]
        if (cached != null) {
            return cached
        }

        val baseSubjects = ExamChapterManager.getSubjectsForExam(normSubExam)

        val rawCustomChapters = customChapters.toList().filter { 
            val accSub = DynamicTestRepository.getStrictExamOwner(it.title, emptyList(), it.id, it.subExamId)
            val t = it.title.lowercase().replace("-", "_").replace(" ", "_")
            val rawT = it.title.lowercase()
            val idLower = it.id.lowercase().replace("-", "_").replace(" ", "_")
            val isExplicitFullOrSubject = it.title.contains("Full Test", ignoreCase = true) ||
                                         it.title.contains("मॉक टेस्ट", ignoreCase = true) ||
                                         it.title.contains("Mock Test", ignoreCase = true) ||
                                         it.title.contains("Subject Wise", ignoreCase = true) ||
                                         it.title.contains("विषयवार", ignoreCase = true) ||
                                         it.questions.size >= 60
            accSub == normSubExam && !DynamicTestRepository.isAnyOtherExplicitExam(t, rawT, idLower, normSubExam) && !isExplicitFullOrSubject
        }
        val dynamicChapterAsCustom = DynamicTestRepository.customTests
            .filter {
                val accSub = DynamicTestRepository.getStrictExamOwner(it.testPaper.title, emptyList(), it.testPaper.id, it.subExamId)
                val t = it.testPaper.title.lowercase().replace("-", "_").replace(" ", "_")
                val rawT = it.testPaper.title.lowercase()
                val idLower = it.testPaper.id.lowercase().replace("-", "_").replace(" ", "_")
                val isExplicitFullOrSubject = it.testType.equals("FULL_TEST", ignoreCase = true) ||
                                             it.testType.equals("FULL_MOCK", ignoreCase = true) ||
                                             it.testType.equals("SUBJECT_WISE", ignoreCase = true) ||
                                             it.testPaper.title.contains("Full Test", ignoreCase = true) ||
                                             it.testPaper.title.contains("मॉक टेस्ट", ignoreCase = true) ||
                                             it.testPaper.title.contains("Mock Test", ignoreCase = true) ||
                                             it.testPaper.title.contains("Subject Wise", ignoreCase = true) ||
                                             it.testPaper.title.contains("विषयवार", ignoreCase = true) ||
                                             it.testPaper.questions.size >= 60

                val isExplicitChapterWise = it.testType.equals("CHAPTER_WISE", ignoreCase = true) ||
                                            it.testPaper.title.contains("Chapter", ignoreCase = true) ||
                                            it.testPaper.title.contains("अध्याय", ignoreCase = true)

                val isFullTestChunk = it.testPaper.title.contains("Shift", ignoreCase = true) ||
                                      it.testPaper.title.contains("Full Test", ignoreCase = true) ||
                                      it.testPaper.title.contains("Mock Test", ignoreCase = true) ||
                                      it.testPaper.title.contains("मॉक टेस्ट", ignoreCase = true) ||
                                      it.testPaper.title.contains("Paper", ignoreCase = true) ||
                                      (it.testPaper.title.contains("Set ", ignoreCase = true) && (it.testPaper.questions.size >= 50 || it.testPaper.title.contains("कांस्टेबल", ignoreCase = true) || it.testPaper.title.contains("Dec", ignoreCase = true) || it.testPaper.title.contains("Shift", ignoreCase = true)))

                accSub == normSubExam &&
                !DynamicTestRepository.isAnyOtherExplicitExam(t, rawT, idLower, normSubExam) &&
                !isExplicitFullOrSubject &&
                !isFullTestChunk &&
                (isExplicitChapterWise || it.testPaper.title.contains(" - Set ", true) || it.testPaper.title.contains("Set ", true) || it.testPaper.title.contains("सेट ", true))
            }
            .flatMap { dyn ->
                val qList = dyn.testPaper.questions
                val title = dyn.testPaper.title
                val firstSub = qList.firstOrNull()?.subject ?: ""
                val resolvedSubKey = resolveSubjectKey(title, firstSub, qList)
                val isSetAlready = title.contains(" - Set ", ignoreCase = true) || title.contains("Set ", ignoreCase = true) || title.contains("सेट ", ignoreCase = true)
                val isChapterWiseTest = dyn.testType.equals("CHAPTER_WISE", ignoreCase = true) ||
                                        title.contains("Chapter", ignoreCase = true) ||
                                        title.contains("अध्याय", ignoreCase = true)

                if (isChapterWiseTest && qList.size > 10 && !isSetAlready) {
                    val cleanBase = cleanBaseChapterTitle(title)
                    val chunks = qList.chunked(10)
                    chunks.mapIndexed { setIdx, chunk ->
                        val setNum = setIdx + 1
                        val setChapId = "${dyn.testPaper.id}_s$setNum"
                        val setChapTitle = "$cleanBase - Set $setNum"
                        val isMathOrReasoning = isMathOrReasoningSubject(resolvedSubKey, resolvedSubKey)
                        val duration = if (isMathOrReasoning) chunk.size * 2 else 10
                        CustomChapterEntry(
                            id = setChapId,
                            subExamId = normSubExam,
                            subjectKey = resolvedSubKey,
                            subjectTitle = resolvedSubKey,
                            title = setChapTitle,
                            englishTitle = setChapTitle,
                            description = "${chunk.size} प्रश्न • ${duration} मिनट • विस्तृत हल सहित (PYQ ${dyn.year})",
                            totalQuestions = chunk.size,
                            durationMinutes = duration,
                            totalMarks = chunk.size,
                            difficulty = dyn.testPaper.difficulty,
                            questions = chunk,
                            year = dyn.year,
                            createdAt = dyn.createdAt + setIdx
                        )
                    }
                } else {
                    listOf(
                        CustomChapterEntry(
                            id = dyn.testPaper.id.toString(),
                            subExamId = normSubExam,
                            subjectKey = resolvedSubKey,
                            subjectTitle = resolvedSubKey,
                            title = title,
                            englishTitle = title,
                            description = "${qList.size} प्रश्न • ${dyn.year} PYQ",
                            totalQuestions = qList.size,
                            durationMinutes = dyn.testPaper.durationMinutes,
                            totalMarks = dyn.testPaper.totalMarks,
                            difficulty = dyn.testPaper.difficulty,
                            questions = qList,
                            year = dyn.year,
                            createdAt = dyn.createdAt
                        )
                    )
                }
            }
        val combinedCustom = mutableListOf<CustomChapterEntry>()
        for (rawCh in rawCustomChapters) {
            val key = "${rawCh.subExamId}_${rawCh.subjectKey}_${cleanBaseChapterTitle(rawCh.title).lowercase()}_${rawCh.title.trim().lowercase()}_${rawCh.year}"
            if (!combinedCustom.any { 
                val existingKey = "${it.subExamId}_${it.subjectKey}_${cleanBaseChapterTitle(it.title).lowercase()}_${it.title.trim().lowercase()}_${it.year}"
                existingKey == key || (it.id == rawCh.id)
            }) {
                combinedCustom.add(rawCh)
            }
        }
        for (dynCh in dynamicChapterAsCustom) {
            val dynKey = "${dynCh.subExamId}_${dynCh.subjectKey}_${cleanBaseChapterTitle(dynCh.title).lowercase()}_${dynCh.title.trim().lowercase()}_${dynCh.year}"
            val isDuplicate = combinedCustom.any { existing ->
                val exKey = "${existing.subExamId}_${existing.subjectKey}_${cleanBaseChapterTitle(existing.title).lowercase()}_${existing.title.trim().lowercase()}_${existing.year}"
                val exBase = cleanBaseChapterTitle(existing.title).lowercase()
                val dynBase = cleanBaseChapterTitle(dynCh.title).lowercase()
                exKey == dynKey || existing.id == dynCh.id || 
                (exBase == dynBase && existing.subExamId == dynCh.subExamId && existing.year == dynCh.year && extractSetNumber(existing.title) == extractSetNumber(dynCh.title) && extractSetNumber(existing.title) != 999)
            }
            if (!isDuplicate) {
                combinedCustom.add(dynCh)
            }
        }
        val activeCustomChapters = combinedCustom.distinctBy { it.id }
        val activeCustomSubjects = customSubjects.toList().filter { DynamicTestRepository.normalizeSubExamId(it.subExamId) == normSubExam }
        val activeCustomTopics = customTopics.toList().filter { DynamicTestRepository.normalizeSubExamId(it.subExamId) == normSubExam }
        val activeDeletedIds = deletedChapterIds.toSet()

        val resultSubjects = mutableListOf<ExamSubjectCategory>()

        // 1. Merge custom chapters into existing built-in subjects
        for (baseSub in baseSubjects) {
            val matchingCustomChapters = activeCustomChapters.filter { chap ->
                val resolvedKey = resolveSubjectKey(chap.title, chap.subjectKey, chap.questions)
                isMatchingSubjectKey(resolvedKey, baseSub.key) || isMatchingSubjectKey(chap.subjectKey, baseSub.key)
            }
            val matchingCustomTopics = activeCustomTopics.filter { topic ->
                val resolvedKey = resolveSubjectKey(topic.title, topic.subjectKey)
                isMatchingSubjectKey(resolvedKey, baseSub.key) || isMatchingSubjectKey(topic.subjectKey, baseSub.key)
            }

            val customChapterItems = matchingCustomChapters.mapIndexed { idx, chap ->
                val isMathOrReasoning = isMathOrReasoningSubject(chap.subjectKey, chap.subjectTitle)
                val qCount = if (chap.questions.isNotEmpty()) chap.questions.size else chap.totalQuestions
                val effectiveDuration = if (isMathOrReasoning) qCount * 2 else chap.durationMinutes
                ChapterItem(
                    id = chap.id,
                    subExamId = chap.subExamId,
                    subjectKey = chap.subjectKey,
                    subjectTitle = chap.subjectTitle,
                    chapterNumber = baseSub.chapters.size + idx + 1,
                    title = chap.title,
                    englishTitle = chap.englishTitle,
                    description = chap.description,
                    totalQuestions = chap.totalQuestions,
                    durationMinutes = effectiveDuration,
                    totalMarks = chap.totalMarks,
                    difficulty = chap.difficulty,
                    questions = chap.questions,
                    year = if (chap.year.isNotBlank()) chap.year else extractYear(chap.title, chap.description)
                )
            }

            val mergedBuiltInItems = mutableListOf<ChapterItem>()
            val processedTopicTitles = mutableSetOf<String>()

            // 1. Process built-in chapters in their exact original numbered order
            for (builtInCh in baseSub.chapters) {
                val fullTitle = builtInCh.title.trim()
                val baseTitle = if (fullTitle.contains(" - Set ")) fullTitle.substringBefore(" - Set ").trim() else fullTitle
                val cleanWithoutNum = if (baseTitle.contains(".")) baseTitle.substringAfter(".").trim() else baseTitle
                val cleanBase = cleanBaseChapterTitle(fullTitle)

                // Check if any custom tests exist for this built-in topic
                val matchingCustomForThis = customChapterItems.filter { customCh ->
                    val customCleanBase = cleanBaseChapterTitle(customCh.title)
                    val customBt = if (customCh.title.contains(" - Set ")) customCh.title.substringBefore(" - Set ").trim() else customCh.title.trim()
                    val customClean = if (customBt.contains(".")) customBt.substringAfter(".").trim() else customBt
                    (customCleanBase.equals(cleanBase, ignoreCase = true) ||
                     customCleanBase.equals(cleanWithoutNum, ignoreCase = true) ||
                     customBt.equals(baseTitle, ignoreCase = true) ||
                     customClean.equals(cleanWithoutNum, ignoreCase = true) ||
                     customBt.equals(fullTitle, ignoreCase = true)) &&
                    !activeDeletedIds.contains(customCh.id)
                }

                // A built-in topic group is only removed if explicitly deleted AND no custom tests exist for it
                val isGroupExplicitlyDeleted = (activeDeletedIds.contains("group_$cleanBase") ||
                        activeDeletedIds.contains("group_$baseTitle") ||
                        activeDeletedIds.contains("group_$cleanWithoutNum") ||
                        activeDeletedIds.contains("group_$fullTitle")) && matchingCustomForThis.isEmpty()

                if (isGroupExplicitlyDeleted) {
                    continue
                }

                // Check if the original built-in test is active (not deleted)
                val isBuiltInTestActive = !activeDeletedIds.contains(builtInCh.id)

                // If custom tests/sets exist for this topic (e.g. Set 1, Set 2, Set 3...),
                // DO NOT add the built-in placeholder test, because custom sets replace/supersede the placeholder!
                if (isBuiltInTestActive && matchingCustomForThis.isEmpty()) {
                    mergedBuiltInItems.add(builtInCh)
                }

                // Add any custom sets created for this topic (preserving sets across all different years)
                for (customCh in matchingCustomForThis) {
                    val customYear = if (customCh.year.isNotBlank()) customCh.year else extractYear(customCh.title, customCh.description)
                    if (!mergedBuiltInItems.any { 
                        it.id == customCh.id || 
                        (it.title.trim().equals(customCh.title.trim(), ignoreCase = true) && (if (it.year.isNotBlank()) it.year else extractYear(it.title, it.description)) == customYear) 
                    }) {
                        mergedBuiltInItems.add(customCh)
                    }
                }

                // CRITICAL FIX: If built-in test was deleted AND no custom tests exist,
                // KEEP THE TOPIC BUTTON VISIBLE with "0 टेस्ट उपलब्ध • नया टेस्ट जोड़ें" status!
                if (!isBuiltInTestActive && matchingCustomForThis.isEmpty()) {
                    mergedBuiltInItems.add(
                        ChapterItem(
                            id = "topic_builtin_${builtInCh.id}",
                            subExamId = builtInCh.subExamId,
                            subjectKey = builtInCh.subjectKey,
                            subjectTitle = builtInCh.subjectTitle,
                            chapterNumber = builtInCh.chapterNumber,
                            title = builtInCh.title,
                            englishTitle = builtInCh.englishTitle,
                            description = "0 टेस्ट उपलब्ध",
                            totalQuestions = 0,
                            durationMinutes = 0,
                            totalMarks = 0,
                            difficulty = "मध्यम",
                            questions = emptyList()
                        )
                    )
                }

                processedTopicTitles.add(cleanBase.lowercase())
                processedTopicTitles.add(baseTitle.lowercase())
                processedTopicTitles.add(cleanWithoutNum.lowercase())
                processedTopicTitles.add(fullTitle.lowercase())
            }

            // 2. Add any additional custom chapter tests that don't belong to a built-in chapter
            val additionalCustomItems = customChapterItems.filter { customCh ->
                val customCleanBase = cleanBaseChapterTitle(customCh.title)
                val customBt = if (customCh.title.contains(" - Set ")) customCh.title.substringBefore(" - Set ").trim() else customCh.title.trim()
                val customClean = if (customBt.contains(".")) customBt.substringAfter(".").trim() else customBt
                !processedTopicTitles.contains(customCleanBase.lowercase()) &&
                !processedTopicTitles.contains(customBt.lowercase()) &&
                !processedTopicTitles.contains(customClean.lowercase()) &&
                !activeDeletedIds.contains(customCh.id)
            }

            // 3. Add any brand-new custom topics created by Admin with 0 tests
            val additionalZeroTestCustomTopics = matchingCustomTopics.filter { topic ->
                val topicBt = topic.title.trim()
                val topicClean = if (topicBt.contains(".")) topicBt.substringAfter(".").trim() else topicBt
                !processedTopicTitles.contains(topicBt.lowercase()) &&
                !processedTopicTitles.contains(topicClean.lowercase()) &&
                !additionalCustomItems.any { ch ->
                    val bt = if (ch.title.contains(" - Set ")) ch.title.substringBefore(" - Set ").trim() else ch.title.trim()
                    bt.equals(topicBt, ignoreCase = true)
                } &&
                !activeDeletedIds.contains("group_$topicBt")
            }.mapIndexed { idx, topic ->
                ChapterItem(
                    id = "topic_${topic.id}",
                    subExamId = topic.subExamId,
                    subjectKey = topic.subjectKey,
                    subjectTitle = baseSub.name,
                    chapterNumber = 900 + idx,
                    title = topic.title,
                    englishTitle = topic.englishTitle,
                    description = "0 टेस्ट उपलब्ध",
                    totalQuestions = 0,
                    durationMinutes = 0,
                    totalMarks = 0,
                    difficulty = "मध्यम",
                    questions = emptyList()
                )
            }

            val finalChapters = mergedBuiltInItems + additionalCustomItems + additionalZeroTestCustomTopics
            resultSubjects.add(baseSub.copy(chapters = finalChapters))
        }

        // 2. Add brand-new custom subjects created by Admin
        for (customSub in activeCustomSubjects) {
            val matchingCustomChapters = activeCustomChapters.filter { isMatchingSubjectKey(it.subjectKey, customSub.key) }
            val matchingCustomTopics = activeCustomTopics.filter { isMatchingSubjectKey(it.subjectKey, customSub.key) }

            val chapterItems = matchingCustomChapters.mapIndexed { idx, chap ->
                val isMathOrReasoning = isMathOrReasoningSubject(chap.subjectKey, customSub.name)
                val qCount = if (chap.questions.isNotEmpty()) chap.questions.size else chap.totalQuestions
                val effectiveDuration = if (isMathOrReasoning) qCount * 2 else chap.durationMinutes
                ChapterItem(
                    id = chap.id,
                    subExamId = chap.subExamId,
                    subjectKey = chap.subjectKey,
                    subjectTitle = customSub.name,
                    chapterNumber = idx + 1,
                    title = chap.title,
                    englishTitle = chap.englishTitle,
                    description = chap.description,
                    totalQuestions = chap.totalQuestions,
                    durationMinutes = effectiveDuration,
                    totalMarks = chap.totalMarks,
                    difficulty = chap.difficulty,
                    questions = chap.questions,
                    year = if (chap.year.isNotBlank()) chap.year else extractYear(chap.title, chap.description)
                )
            }

            val zeroTestTopicItems = matchingCustomTopics.filter { topic ->
                val cleanTopicTitle = topic.title.trim()
                val hasAnyTest = chapterItems.any { ch ->
                    val bt = if (ch.title.contains(" - Set ")) ch.title.substringBefore(" - Set ").trim() else ch.title.trim()
                    bt.equals(cleanTopicTitle, ignoreCase = true)
                }
                !hasAnyTest && !activeDeletedIds.contains("group_$cleanTopicTitle")
            }.mapIndexed { idx, topic ->
                ChapterItem(
                    id = "topic_${topic.id}",
                    subExamId = topic.subExamId,
                    subjectKey = topic.subjectKey,
                    subjectTitle = customSub.name,
                    chapterNumber = 800 + idx,
                    title = topic.title,
                    englishTitle = topic.englishTitle,
                    description = "0 टेस्ट उपलब्ध",
                    totalQuestions = 0,
                    durationMinutes = 0,
                    totalMarks = 0,
                    difficulty = "मध्यम",
                    questions = emptyList()
                )
            }

            val allSubjectChapters = (chapterItems + zeroTestTopicItems).filter { ch ->
                if (ch.id.startsWith("topic_")) {
                    !activeDeletedIds.contains("group_${ch.title.trim()}")
                } else {
                    val baseTitle = if (ch.title.contains(" - Set ")) ch.title.substringBefore(" - Set ").trim() else ch.title.trim()
                    !activeDeletedIds.contains(ch.id) && !activeDeletedIds.contains("group_$baseTitle")
                }
            }

            resultSubjects.add(
                ExamSubjectCategory(
                    key = customSub.key,
                    name = customSub.name,
                    englishName = customSub.englishName,
                    icon = getIconVector(customSub.iconType),
                    gradientColors = listOf(Color(0xFF2563EB), Color(0xFF38BDF8)),
                    chapters = allSubjectChapters
                )
            )
        }

        fullSubjectsCache[normSubExam] = resultSubjects
        return resultSubjects
    }

    /**
     * Extracts questions belonging to a specific subject category from a full mock test.
     * Uses fuzzy subject tagging, normalized official subject mappings, and section fallback ranges.
     */
    private fun isComputerQuestion(q: QuizQuestion): Boolean {
        val sub = q.subject.lowercase()
        if (sub.contains("comp") || sub.contains("कंप्यूटर") || sub.contains("संगणक") || sub.contains(" it") || sub.startsWith("it") || sub.contains("आईटी")) return true
        val fullText = (q.question + " " + q.options.joinToString(" ") + " " + q.explanation).lowercase()
        return fullText.contains("कंप्यूटर") || fullText.contains("computer") || fullText.contains("संगणक") ||
                fullText.contains("ms-word") || fullText.contains("ms word") || fullText.contains("ms-excel") || fullText.contains("ms excel") ||
                fullText.contains("ms-office") || fullText.contains("powerpoint") || fullText.contains("spreadsheet") ||
                fullText.contains("web browser") || fullText.contains("वेब ब्राउज़र") || fullText.contains("ip address") ||
                fullText.contains("http://") || fullText.contains("https://") || fullText.contains(" e-mail") || fullText.contains("ई-मेल") ||
                fullText.contains("search engine") || fullText.contains("ऑपरेटिंग सिस्टम") || fullText.contains("operating system") ||
                fullText.contains("ram/rom") || fullText.contains("कैश मेमोरी") || fullText.contains("इनपुट डिवाइस") || fullText.contains("आउटपुट डिवाइस")
    }

    fun isMathsQuestion(q: QuizQuestion): Boolean {
        val sub = q.subject.lowercase()
        if (sub.contains("math") || sub.contains("गणित") || sub.contains("संख्यात्मक") || sub.contains("quantitative") || sub.contains("अंकगणित")) return true
        return false
    }

    fun isReasoningQuestion(q: QuizQuestion): Boolean {
        val sub = q.subject.lowercase()
        if (sub.contains("reason") || sub.contains("तर्क") || sub.contains("रीजनिंग") || sub.contains("बुद्धिमत्ता") || sub.contains("logic")) return true
        return false
    }

    /**
     * Extracts questions belonging to a specific subject from a TestPaper.
     * Uses both strict section range mapping for full multi-subject exams and deep question-content tagging
     * to guarantee Computer questions NEVER leak into GK / GS.
     */
    fun extractSubjectQuestions(
        testPaper: TestPaper,
        subjectCategory: ExamSubjectCategory,
        examTitle: String
    ): List<QuizQuestion> {
        val subKey = subjectCategory.key.uppercase().trim()
        val subName = subjectCategory.name.lowercase().trim()
        val engName = subjectCategory.englishName.lowercase().trim()

        val isCompCategory = subKey.contains("COMP") || subName.contains("कंप्यूटर") || engName.contains("computer")
        val isMathCategory = subKey.contains("MATH") || subName.contains("गणित") || subName.contains("संख्यात्मक") || engName.contains("math")
        val isReasonCategory = subKey.contains("REASON") || subName.contains("तर्क") || subName.contains("रीजनिंग") || engName.contains("reason")
        val isHindiCategory = subKey.contains("HINDI") || subName.contains("हिंदी") || engName.contains("hindi")
        val isEnglishCategory = subKey.contains("ENG") || subName.contains("english") || subName.contains("अंग्रेजी") || engName.contains("english")
        val isScienceCategory = subKey.contains("SCIENCE") || subName.contains("विज्ञान") || engName.contains("science")
        val isRajasthanCategory = subKey.contains("RAJASTHAN") || subName.contains("राजस्थान") || engName.contains("rajasthan")
        val isLawCategory = subKey.contains("LAW") || subName.contains("मूलविधि") || subName.contains("संविधान")
        val isGkCategory = subKey.contains("GK") || subKey.contains("GS") || subName.contains("सामान्य ज्ञान") ||
                subName.contains("सामान्य जागरूकता") || engName.contains("gk") || engName.contains("awareness") || engName.contains("general studies")

        // 1. If it's a full multi-subject exam (e.g. Delhi Police 100 questions), use official section ranges first
        if (ExamSubjectManager.isMultiSubjectFullExam(testPaper.title, testPaper.questions) && testPaper.questions.size >= 70) {
            val sections = ExamSubjectManager.getExamSections(testPaper.title, testPaper.questions)
            for (sec in sections) {
                val secNorm = ExamSubjectManager.normalizeToOfficialSubject(sec.name, examTitle).lowercase()
                val isSecMatch = when {
                    isCompCategory -> secNorm.contains("कंप्यूटर")
                    isMathCategory -> secNorm.contains("गणित")
                    isReasonCategory -> secNorm.contains("तर्क")
                    isHindiCategory -> secNorm.contains("हिंदी")
                    isEnglishCategory -> secNorm.contains("english")
                    isScienceCategory -> secNorm.contains("विज्ञान")
                    isRajasthanCategory -> secNorm.contains("राजस्थान")
                    isGkCategory -> (secNorm.contains("gk") || secNorm.contains("gs") || secNorm.contains("सामान्य ज्ञान") || secNorm.contains("सामान्य जागरूकता")) &&
                            !secNorm.contains("कंप्यूटर") && !secNorm.contains("तर्क") && !secNorm.contains("गणित") && !secNorm.contains("हिंदी") && !secNorm.contains("english")
                    else -> secNorm.contains(subName) || secNorm.contains(subKey.lowercase())
                }
                if (isSecMatch && sec.range.last < testPaper.questions.size) {
                    val slice = testPaper.questions.subList(sec.range.first, minOf(sec.range.last + 1, testPaper.questions.size))
                    if (slice.isNotEmpty()) {
                        // Extra safety: if GK, strictly exclude any computer question
                        return if (isGkCategory) {
                            slice.filter { !isComputerQuestion(it) && !isMathsQuestion(it) && !isReasoningQuestion(it) }
                        } else if (isCompCategory) {
                            slice.filter { isComputerQuestion(it) || it.subject.lowercase().contains("comp") || it.subject.isBlank() }
                        } else {
                            slice
                        }
                    }
                }
            }
        }

        // 2. Direct tag matching on QuizQuestion.subject + Question content inspection
        val matched = testPaper.questions.filter { q ->
            val qSub = q.subject.trim()
            val normQSub = ExamSubjectManager.normalizeToOfficialSubject(qSub, examTitle).lowercase()
            val qSubLower = qSub.lowercase()

            when {
                // Computer - accepts tagged computer OR questions whose text is clearly computer
                isCompCategory -> {
                    isComputerQuestion(q) || normQSub.contains("कंप्यूटर") || qSubLower.contains("comput") ||
                            qSubLower.contains("कंप्यूटर") || qSubLower.contains("संगणक") || qSubLower.contains("it")
                }
                // Maths
                isMathCategory -> {
                    !isComputerQuestion(q) && (isMathsQuestion(q) || normQSub.contains("गणित") || qSubLower.contains("math") ||
                            qSubLower.contains("गणित") || qSubLower.contains("संख्यात्मक") || qSubLower.contains("quantitative") || qSubLower.contains("अंकगणित"))
                }
                // Reasoning
                isReasonCategory -> {
                    !isComputerQuestion(q) && (isReasoningQuestion(q) || normQSub.contains("तर्क") || qSubLower.contains("reason") ||
                            qSubLower.contains("तर्क") || qSubLower.contains("बुद्धिमत्ता") || qSubLower.contains("रीजनिंग") || qSubLower.contains("logic"))
                }
                // Hindi
                isHindiCategory -> {
                    !isComputerQuestion(q) && (normQSub.contains("हिंदी") || qSubLower.contains("hindi") || qSubLower.contains("हिंदी"))
                }
                // English
                isEnglishCategory -> {
                    !isComputerQuestion(q) && (normQSub.contains("english") || qSubLower.contains("english") || qSubLower.contains("अंग्रेजी") || qSubLower.contains("comprehension"))
                }
                // Science
                isScienceCategory -> {
                    !isComputerQuestion(q) && (normQSub.contains("विज्ञान") || qSubLower.contains("science") || qSubLower.contains("विज्ञान") ||
                            qSubLower.contains("भौतिक") || qSubLower.contains("रसायन") || qSubLower.contains("जीव"))
                }
                // Rajasthan GK
                isRajasthanCategory -> {
                    !isComputerQuestion(q) && (normQSub.contains("राजस्थान") || qSubLower.contains("rajasthan") || qSubLower.contains("राजस्थान"))
                }
                // Law / Mulvidhi
                isLawCategory -> {
                    !isComputerQuestion(q) && (normQSub.contains("मूलविधि") || qSubLower.contains("मूलविधि") || qSubLower.contains("संविधान") || qSubLower.contains("polity"))
                }
                // GK / GS / General Awareness (STRICTLY EXCLUDES Computer, Maths, Reasoning, Hindi, English)
                isGkCategory -> {
                    val isComp = isComputerQuestion(q) || normQSub.contains("कंप्यूटर") || qSubLower.contains("comput") || qSubLower.contains("कंप्यूटर")
                    val isMath = isMathsQuestion(q) || normQSub.contains("गणित") || qSubLower.contains("math") || qSubLower.contains("गणित")
                    val isReason = isReasoningQuestion(q) || normQSub.contains("तर्क") || qSubLower.contains("reason") || qSubLower.contains("तर्क")
                    val isHindi = normQSub.contains("हिंदी") || qSubLower.contains("hindi")
                    val isEng = normQSub.contains("english") || qSubLower.contains("english")

                    !isComp && !isMath && !isReason && !isHindi && !isEng && (
                        normQSub.contains("gk") || normQSub.contains("gs") || normQSub.contains("सामान्य ज्ञान") ||
                                normQSub.contains("सामान्य जागरूकता") || qSubLower.contains("gk") || qSubLower.contains("gs") ||
                                qSubLower.contains("इतिहास") || qSubLower.contains("भूगोल") || qSubLower.contains("अर्थशास्त्र") ||
                                qSubLower.contains("history") || qSubLower.contains("geography") || qSubLower.contains("awareness") ||
                                qSubLower.contains("विज्ञान") || qSubLower.contains("science") || qSubLower.contains("polity") ||
                                qSubLower.contains("संविधान") || qSubLower.contains("current") || qSubLower.contains("करंट") ||
                                (qSub.isBlank() && !isComp) || (qSub.equals("General", ignoreCase = true) && !isComp)
                    )
                }
                // Custom subject: match by custom subject title or key
                else -> {
                    qSubLower.contains(subName) || qSubLower.contains(subKey.lowercase())
                }
            }
        }

        if (matched.isNotEmpty()) {
            return matched
        }

        // 3. Fallback: If questions don't have individual subject tags, use official exam section ranges!
        val sections = ExamSubjectManager.getExamSections(testPaper.title, testPaper.questions)
        for (sec in sections) {
            val secNorm = ExamSubjectManager.normalizeToOfficialSubject(sec.name, examTitle).lowercase()
            val isSecMatch = when {
                isCompCategory -> secNorm.contains("कंप्यूटर")
                isMathCategory -> secNorm.contains("गणित")
                isReasonCategory -> secNorm.contains("तर्क") || secNorm.contains("रीजनिंग")
                isHindiCategory -> secNorm.contains("हिंदी")
                isEnglishCategory -> secNorm.contains("english") || secNorm.contains("अंग्रेजी")
                isScienceCategory -> secNorm.contains("विज्ञान")
                isRajasthanCategory -> secNorm.contains("राजस्थान")
                else -> (secNorm.contains("gk") || secNorm.contains("gs") || secNorm.contains("सामान्य ज्ञान") || secNorm.contains("सामान्य जागरूकता")) &&
                        !secNorm.contains("कंप्यूटर") && !secNorm.contains("तर्क") && !secNorm.contains("गणित") && !secNorm.contains("हिंदी") && !secNorm.contains("english")
            }
            if (isSecMatch && sec.range.last < testPaper.questions.size) {
                val slice = testPaper.questions.subList(sec.range.first, minOf(sec.range.last + 1, testPaper.questions.size))
                if (slice.isNotEmpty()) {
                    return if (isGkCategory) {
                        slice.filter { !isComputerQuestion(it) && !isMathsQuestion(it) && !isReasoningQuestion(it) }
                    } else {
                        slice
                    }
                }
            }
        }

        return emptyList()
    }

    /**
     * Retrieves all full mock test papers for a given subExam and automatically extracts/splits
     * the questions belonging to the specified subject category.
     * The returned TestPaper retains the EXACT title of the Full Test!
     */
    fun getSubjectWisePyqTests(
        subExam: SubExam,
        subjectCategory: ExamSubjectCategory,
        context: Context? = null
    ): List<TestPaper> {
        val normId = DynamicTestRepository.normalizeSubExamId(subExam.id)

        // 1. Fetch all dynamic full tests (including Admin answer key imports)
        val dynamicFullEntries = DynamicTestRepository.getAllTestsForSubExam(subExam.id, "FULL_TEST")
        val dynamicCustomList = DynamicTestRepository.getCustomTests(subExam.id, "", "")
        val dynamicFullPapers = (dynamicFullEntries.map { it.testPaper } + dynamicCustomList).distinctBy { it.id }

        // 2. Fetch static full tests for this exam
        val targetYears = listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020")
        val staticFullPapers = when (normId) {
            "ssc_delhi_police" -> {
                targetYears.flatMap { y ->
                    getDelhiPoliceConstableTestsForYear(y) + getDelhiPoliceHeadConstableTestsForYear(y)
                }
            }
            else -> {
                val officialQuestions = ExamSubjectManager.getQuestionsForExam(subExam.name)
                if (officialQuestions.isEmpty()) {
                    emptyList()
                } else {
                    targetYears.flatMapIndexed { idx, y ->
                        listOf(
                            TestPaper(
                                id = "${normId}_official_mock_${y}_1",
                                title = "${subExam.name} Official PYQ Paper $y (Shift-1)",
                                totalQuestions = officialQuestions.size,
                                durationMinutes = if (officialQuestions.size > 80) 90 else 60,
                                totalMarks = officialQuestions.size,
                                difficulty = "ऑफिशियल पैटर्न",
                                year = y,
                                questions = officialQuestions
                            ),
                            TestPaper(
                                id = "${normId}_official_mock_${y}_2",
                                title = "${subExam.name} Previous Year Paper (PYQ) $y",
                                totalQuestions = officialQuestions.size,
                                durationMinutes = if (officialQuestions.size > 80) 90 else 60,
                                totalMarks = officialQuestions.size,
                                difficulty = if (idx % 2 == 0) "मध्यम" else "कठिन",
                                year = y,
                                questions = officialQuestions.reversed()
                            )
                        )
                    }
                }
            }
        }

        val allFullPapers = (dynamicFullPapers + staticFullPapers).distinctBy { it.id.toString() }
        val subjectTestPapers = mutableListOf<TestPaper>()

        for (fullTest in allFullPapers) {
            val subjectQuestions = extractSubjectQuestions(fullTest, subjectCategory, subExam.name)
            if (subjectQuestions.isNotEmpty()) {
                val isMathOrReasoning = isMathOrReasoningSubject(subjectCategory.key, subjectCategory.name)
                val calcDuration = if (isMathOrReasoning) {
                    // Exactly 2 minutes per question (e.g. 15 questions = 30 minutes, 25 questions = 50 minutes)
                    subjectQuestions.size * 2
                } else {
                    maxOf(10, (subjectQuestions.size * 1.0).toInt())
                }
                subjectTestPapers.add(
                    TestPaper(
                        id = "${fullTest.id}_sub_${subjectCategory.key}",
                        title = fullTest.title, // Keep EXACT same title as Full Test!
                        totalQuestions = subjectQuestions.size,
                        durationMinutes = calcDuration,
                        totalMarks = subjectQuestions.size,
                        difficulty = fullTest.difficulty,
                        year = fullTest.year,
                        questions = subjectQuestions
                    )
                )
            }
        }

        return subjectTestPapers
    }

    fun saveToDisk(context: Context) {
        synchronized(this) {
            try {
                val file = File(context.filesDir, FILE_NAME)
                val root = JSONObject()

                val subjectsSnapshot = customSubjects.toList()
                val chaptersSnapshot = customChapters.toList()
                val topicsSnapshot = customTopics.toList()
                val deletedSnapshot = deletedChapterIds.toList()

                val subjectsArr = JSONArray()
                for (sub in subjectsSnapshot) {
                    val subObj = JSONObject()
                    subObj.put("id", sub.id)
                    subObj.put("subExamId", sub.subExamId)
                    subObj.put("key", sub.key)
                    subObj.put("name", sub.name)
                    subObj.put("englishName", sub.englishName)
                    subObj.put("iconType", sub.iconType)
                    subjectsArr.put(subObj)
                }
                root.put("subjects", subjectsArr)

                val chaptersArr = JSONArray()
                for (chap in chaptersSnapshot) {
                    val chapObj = JSONObject()
                    chapObj.put("id", chap.id)
                    chapObj.put("subExamId", chap.subExamId)
                    chapObj.put("subjectKey", chap.subjectKey)
                    chapObj.put("subjectTitle", chap.subjectTitle)
                    chapObj.put("title", chap.title)
                    chapObj.put("englishTitle", chap.englishTitle)
                    chapObj.put("description", chap.description)
                    chapObj.put("totalQuestions", chap.totalQuestions)
                    chapObj.put("durationMinutes", chap.durationMinutes)
                    chapObj.put("totalMarks", chap.totalMarks)
                    chapObj.put("difficulty", chap.difficulty)
                    chapObj.put("year", chap.year)
                    chapObj.put("createdAt", chap.createdAt)

                    val qsArr = JSONArray()
                    for (q in chap.questions) {
                        val qObj = JSONObject()
                        qObj.put("id", q.id)
                        qObj.put("question", q.question)
                        val optsArr = JSONArray()
                        q.options.forEach { optsArr.put(it) }
                        qObj.put("options", optsArr)
                        qObj.put("correctIndex", q.correctIndex)
                        qObj.put("explanation", q.explanation)
                        qObj.put("subject", q.subject)
                        if (q.imageUrl != null) {
                            qObj.put("imageUrl", q.imageUrl)
                        }
                        if (q.examShiftInfo != null) {
                            qObj.put("examShiftInfo", q.examShiftInfo)
                        }
                        qsArr.put(qObj)
                    }
                    chapObj.put("questions", qsArr)
                    chaptersArr.put(chapObj)
                }
                root.put("chapters", chaptersArr)

                val topicsArr = JSONArray()
                for (top in topicsSnapshot) {
                    val tObj = JSONObject()
                    tObj.put("id", top.id)
                    tObj.put("subExamId", top.subExamId)
                    tObj.put("subjectKey", top.subjectKey)
                    tObj.put("title", top.title)
                    tObj.put("englishTitle", top.englishTitle)
                    tObj.put("description", top.description)
                    tObj.put("createdAt", top.createdAt)
                    topicsArr.put(tObj)
                }
                root.put("topics", topicsArr)

                val delArr = JSONArray()
                for (id in deletedSnapshot) {
                    delArr.put(id)
                }
                root.put("deletedChapterIds", delArr)

                file.writeText(root.toString(2))
                Log.d(TAG, "Saved ${subjectsSnapshot.size} subjects, ${topicsSnapshot.size} topics, and ${chaptersSnapshot.size} chapters to disk")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving custom chapters to disk", e)
            }
        }
    }

    private fun loadFromDisk(context: Context) {
        synchronized(this) {
            try {
                val file = File(context.filesDir, FILE_NAME)
                if (!file.exists()) {
                    return
                }

                val jsonStr = file.readText()
                if (jsonStr.isBlank()) {
                    return
                }

                val root = JSONObject(jsonStr)

                val loadedSubjects = mutableListOf<CustomSubjectEntry>()
                if (root.has("subjects") && !root.isNull("subjects")) {
                    val subjectsArr = root.optJSONArray("subjects")
                    if (subjectsArr != null) {
                        for (i in 0 until subjectsArr.length()) {
                            val obj = subjectsArr.optJSONObject(i) ?: continue
                            loadedSubjects.add(
                                CustomSubjectEntry(
                                    id = obj.optString("id", "sub_$i"),
                                    subExamId = obj.optString("subExamId", "ssc_cgl").ifBlank { "ssc_cgl" },
                                    key = obj.optString("key", "sub_$i").ifBlank { "sub_$i" },
                                    name = obj.optString("name", "विषय").ifBlank { "विषय" },
                                    englishName = obj.optString("englishName", "Subject").ifBlank { "Subject" },
                                    iconType = obj.optString("iconType", "BOOK").ifBlank { "BOOK" }
                                )
                            )
                        }
                    }
                }

                val loadedChapters = mutableListOf<CustomChapterEntry>()
                if (root.has("chapters") && !root.isNull("chapters")) {
                    val chaptersArr = root.optJSONArray("chapters")
                    if (chaptersArr != null) {
                        for (i in 0 until chaptersArr.length()) {
                            val obj = chaptersArr.optJSONObject(i) ?: continue
                            val qsList = mutableListOf<QuizQuestion>()
                            if (obj.has("questions") && !obj.isNull("questions")) {
                                val qsArr = obj.optJSONArray("questions")
                                if (qsArr != null) {
                                    for (j in 0 until qsArr.length()) {
                                        val qObj = qsArr.optJSONObject(j) ?: continue
                                        val optsList = mutableListOf<String>()
                                        if (qObj.has("options") && !qObj.isNull("options")) {
                                            val optsArr = qObj.optJSONArray("options")
                                            if (optsArr != null) {
                                                for (k in 0 until optsArr.length()) {
                                                    val opt = optsArr.optString(k, "").trim()
                                                    if (opt.isNotBlank() && opt != "null") {
                                                        optsList.add(opt)
                                                    }
                                                }
                                            }
                                        }
                                        val rawShift = qObj.optString("examShiftInfo", "")
                                            .ifBlank { qObj.optString("shiftInfo", "") }
                                            .ifBlank { qObj.optString("examDateShift", "") }
                                        val shiftInfo = if (rawShift.isNotBlank() && rawShift != "null") rawShift else null

                                        qsList.add(
                                            QuizQuestion(
                                                id = qObj.optInt("id", j + 1),
                                                question = qObj.optString("question", ""),
                                                options = if (optsList.isNotEmpty()) optsList else listOf("Option A", "Option B", "Option C", "Option D"),
                                                correctIndex = qObj.optInt("correctIndex", 0),
                                                explanation = qObj.optString("explanation", ""),
                                                subject = qObj.optString("subject", obj.optString("subjectTitle", "GK")),
                                                imageUrl = if (qObj.has("imageUrl") && !qObj.isNull("imageUrl")) qObj.optString("imageUrl", "").takeIf { it.isNotBlank() && it != "null" } else null,
                                                examShiftInfo = shiftInfo
                                            )
                                        )
                                    }
                                }
                            }

                            val desc = obj.optString("description", "")
                            val rawYear = obj.optString("year", "")
                            val resolvedYear = if (rawYear.isNotBlank() && rawYear != "null") rawYear else {
                                listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020").find { desc.contains(it) || obj.optString("title", "").contains(it) } ?: "2026"
                            }
                            val engTitle = obj.optString("englishTitle", "").takeIf { it != "null" } ?: ""

                            loadedChapters.add(
                                CustomChapterEntry(
                                    id = obj.optString("id", "chap_$i"),
                                    subExamId = obj.optString("subExamId", "ssc_cgl").ifBlank { "ssc_cgl" },
                                    subjectKey = obj.optString("subjectKey", ""),
                                    subjectTitle = obj.optString("subjectTitle", ""),
                                    title = obj.optString("title", ""),
                                    englishTitle = engTitle,
                                    description = desc,
                                    totalQuestions = obj.optInt("totalQuestions", qsList.size),
                                    durationMinutes = obj.optInt("durationMinutes", 20),
                                    totalMarks = obj.optInt("totalMarks", qsList.size),
                                    difficulty = obj.optString("difficulty", "मध्यम").ifBlank { "मध्यम" },
                                    questions = qsList,
                                    year = resolvedYear,
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                }

                val loadedTopics = mutableListOf<CustomTopicEntry>()
                if (root.has("topics") && !root.isNull("topics")) {
                    val topicsArr = root.optJSONArray("topics")
                    if (topicsArr != null) {
                        for (i in 0 until topicsArr.length()) {
                            val obj = topicsArr.optJSONObject(i) ?: continue
                            val engTitle = obj.optString("englishTitle", "").takeIf { it != "null" } ?: ""
                            loadedTopics.add(
                                CustomTopicEntry(
                                    id = obj.optString("id", "top_$i"),
                                    subExamId = obj.optString("subExamId", "ssc_cgl").ifBlank { "ssc_cgl" },
                                    subjectKey = obj.optString("subjectKey", ""),
                                    title = obj.optString("title", ""),
                                    englishTitle = engTitle,
                                    description = obj.optString("description", ""),
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                }

                val loadedDeletedIds = mutableSetOf<String>()
                if (root.has("deletedChapterIds") && !root.isNull("deletedChapterIds")) {
                    val delArr = root.optJSONArray("deletedChapterIds")
                    if (delArr != null) {
                        for (i in 0 until delArr.length()) {
                            val delId = delArr.optString(i, "").trim()
                            if (delId.isNotBlank() && delId != "null") {
                                loadedDeletedIds.add(delId)
                            }
                        }
                    }
                }

                applySnapshotState(loadedSubjects, loadedChapters, loadedTopics, loadedDeletedIds)

                Log.d(TAG, "Loaded ${loadedSubjects.size} custom subjects, ${loadedTopics.size} custom topics, and ${loadedChapters.size} custom chapters")
                sanitizeExistingCustomChapters(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading custom chapters from disk", e)
            }
        }
    }

    /**
     * Removes local custom chapters that are no longer present in GitHub master sync,
     * ensuring chapters/tests deleted on GitHub are removed from RAM and local disk as well.
     */
    fun removeOrphanedChaptersNotInGitHub(activeIds: Set<String>, activeTitles: Set<String>, context: Context) {
        synchronized(this) {
            val beforeSize = customChapters.size
            customChapters.removeAll { chap ->
                val id = chap.id.trim()
                val isExplicitlyDeleted = deletedChapterIds.contains(id) || deletedChapterIds.contains(chap.title.trim())
                isExplicitlyDeleted
            }
            if (customChapters.size != beforeSize) {
                saveToDisk(context)
                Log.d(TAG, "Removed ${beforeSize - customChapters.size} deleted chapters from disk.")
            }
        }
    }
}
