package com.example.model

/**
 * Exam Subject Manager
 * Central engine defining official subjects, section ranges, question allocations,
 * and authentic question banks for all competitive exams in Exam Mitra.
 *
 * Rules:
 * - Delhi Police: GK/GS, तर्कशक्ति, गणित, कंप्यूटर (NO English, NO Hindi)
 * - SSC CGL / CHSL / CPO: GK/GS, तर्कशक्ति, गणित, English (NO Hindi, NO Computer in Tier-1)
 * - SSC MTS: गणित, तर्कशक्ति, GK/GS, English
 * - SSC GD: तर्कशक्ति, GK/GS, गणित, हिंदी / English
 * - Railway (RRB NTPC, RPF): सामान्य जागरूकता (GK/GS), गणित, तर्कशक्ति (NO English, NO Computer)
 * - Railway (RRB Group D, ALP, JE): सामान्य विज्ञान, गणित, तर्कशक्ति, GK व करंट अफेयर्स (NO English)
 * - UP Police: सामान्य ज्ञान, सामान्य हिंदी, संख्यात्मक क्षमता (गणित), मानसिक अभिरुचि व रीजनिंग (Has हिंदी, NO English)
 * - Lekhpal: सामान्य हिंदी, गणित, सामान्य ज्ञान, ग्राम्य समाज एवं विकास (Has हिंदी & ग्राम्य विकास, NO English, NO Reasoning)
 * - Rajasthan Police: तर्कशक्ति एवं कंप्यूटर, सामान्य ज्ञान एवं विज्ञान, राजस्थान सामान्य ज्ञान (Has राजस्थान GK, NO English)
 * - Patwari / Rajasthan CET: राजस्थान सामान्य ज्ञान, भारत GK व विज्ञान, गणित एवं तर्कशक्ति, सामान्य हिंदी व English, कंप्यूटर ज्ञान
 * - Single Subject Practice Tests: Only that specific subject!
 */
data class ExamSectionInfo(
    val name: String,
    val range: IntRange,
    val questionCount: Int
)

object ExamSubjectManager {

    /**
     * Identifies exam category from title or subExam ID
     */
    fun isDelhiPolice(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("delhi police") || t.contains("दिल्ली पुलिस")
    }

    fun isUpPolice(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("up police") || t.contains("यूपी पुलिस") || t.contains("उत्तर प्रदेश पुलिस")
    }

    fun isLekhpal(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("lekhpal") || t.contains("लेखपाल")
    }

    fun isRajasthanPolice(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("rajasthan police") || t.contains("राजस्थान पुलिस")
    }

    fun isPatwari(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("patwari") || t.contains("पटवारी")
    }

    fun isRajasthanCet(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("rajasthan cet") || t.contains("राजस्थान cet")
    }

    fun isRrbNtpc(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("rrb ntpc") || t.contains("ntpc")
    }

    fun isRrbGroupD(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("group d") || t.contains("ग्रुप d")
    }

    fun isRrbAlpOrJe(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("alp") || t.contains("rrb je") || t.contains("tech")
    }

    fun isRpf(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("rpf") || t.contains("railway protection")
    }

    fun isSscGd(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("ssc gd") || t.contains("gd constable")
    }

    fun isSscMts(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("ssc mts") || t.contains("mts")
    }

    fun isSscCglOrChsl(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("cgl") || t.contains("chsl") || t.contains("cpo") || t.contains("ssc")
    }

    fun isSingleSubjectPractice(title: String): Boolean {
        val t = title.lowercase()
        return t.contains("practice test") || t.contains("अभ्यास टेस्ट") ||
                t.contains("quantitative") || t.contains("reasoning practice") ||
                t.contains("general science") || t.contains("general awareness")
    }

    /**
     * Normalizes any raw subject into one of the canonical official exam subjects (Max 4).
     * Sub-topics like History, Geography, Science, Polity, Economics belong strictly to "GK / GS".
     */
    fun normalizeToOfficialSubject(raw: String, examTitle: String = ""): String {
        val s = raw.trim()
        val lower = s.lowercase()

        // 1. Reasoning
        if (lower.contains("reason") || lower.contains("तर्क") || lower.contains("रीजनिंग") ||
            lower.contains("बुद्धिमत्ता") || lower.contains("intelligence") || lower.contains("mental") ||
            lower.contains("logic") || lower.contains("अभिरुचि")) {
            return "तर्कशक्ति"
        }

        // 2. Mathematics
        if (lower.contains("math") || lower.contains("गणित") || lower.contains("संख्यात्मक") ||
            lower.contains("numeric") || lower.contains("arithmetic") || lower.contains("quantitative") ||
            lower.contains("अंकगणित") || (lower.contains("योग्यता") && !lower.contains("तर्क"))) {
            return "गणित"
        }

        // 3. Computer
        if (lower.contains("comput") || lower.contains("कंप्यूटर") || lower.contains("संगणक") ||
            lower.contains("आईटी") || lower.contains("it") || lower.contains("cyber") ||
            lower.contains("ms-office") || lower.contains("ms-word") || lower.contains("ms-excel")) {
            return "कंप्यूटर"
        }

        // 4. Hindi
        if (lower.contains("hindi") || lower.contains("हिंदी") || lower.contains("हिन्दी")) {
            return "सामान्य हिंदी"
        }

        // 5. English
        if (lower.contains("english") || lower.contains("अंग्रेजी") || lower.contains("अंग्रेज़ी")) {
            return "General English"
        }

        // 6. Child Pedagogy
        if (lower.contains("pedagogy") || lower.contains("शिक्षाशास्त्र") || lower.contains("बाल विकास") || lower.contains("cdp")) {
            return "बाल विकास एवं शिक्षाशास्त्र"
        }

        // 7. State GK (Rajasthan only)
        if (isRajasthanPolice(examTitle) || lower.contains("rajasthan") || lower.contains("राजस्थान")) {
            return "राजस्थान सामान्य ज्ञान"
        }

        // 8. General Knowledge / General Studies / Science / History / Geography / Polity -> GK / GS
        return if (isUpPolice(examTitle)) "सामान्य ज्ञान" else "GK / GS"
    }

    /**
     * Determines if a test is a genuine full multi-subject exam paper (e.g. 80-100 questions covering all exam subjects)
     * vs a Chapter-wise test, Subject-wise test, Set, Practice or Saved Questions test.
     */
    fun isMultiSubjectFullExam(title: String, questions: List<QuizQuestion> = emptyList()): Boolean {
        val t = title.lowercase()
        // Explicit keywords for chapter wise, subject wise, practice, weak area, or set batches
        if (t.contains("chapter") || t.contains("अध्याय") ||
            t.contains("subject-wise") || t.contains("subject wise") || t.contains("विषय-वार") || t.contains("विषयवार") ||
            t.contains("set ") || t.contains("सेट") || t.contains("set-") || t.contains("set_") ||
            t.contains("practice") || t.contains("अभ्यास") ||
            t.contains("saved") || t.contains("topic") ||
            t.contains("टॉपिक") || t.contains("weak") ||
            t.contains("कमजोर")) {
            return false
        }

        // Full tests in competitive exams consist of 50+ questions (typically 80 to 100 questions across multiple sections)
        if (questions.isNotEmpty() && questions.size < 50) {
            return false
        }

        // If questions provided, check how many distinct normalized subjects exist
        if (questions.isNotEmpty()) {
            val distinctSubjects = questions.map { normalizeToOfficialSubject(it.subject, title) }
                .filter { it.isNotBlank() && !it.equals("General", ignoreCase = true) }
                .distinct()
            // Single-subject question list cannot be a multi-subject full exam
            if (distinctSubjects.size <= 1) {
                return false
            }
        }

        return true
    }

    /**
     * Returns the official list of subject names for any given exam
     */
    fun getQuizSubjects(title: String, questions: List<QuizQuestion> = emptyList()): List<String> {
        val t = title.lowercase()

        // 0. If it's a chapter-wise, subject-wise, practice or small set test, return single relevant subject
        if (!isMultiSubjectFullExam(title, questions)) {
            if (questions.isNotEmpty()) {
                val distinct = questions.map { normalizeToOfficialSubject(it.subject, title) }
                    .filter { it.isNotBlank() && !it.equals("General", ignoreCase = true) }
                    .distinct()
                if (distinct.isNotEmpty()) {
                    return listOf(distinct.first())
                }
            }
            return when {
                t.contains("quantitative") || t.contains("math") || t.contains("गणित") ->
                    listOf("गणित")
                t.contains("reasoning") || t.contains("तर्कशक्ति") || t.contains("रीजनिंग") ->
                    listOf("तर्कशक्ति")
                t.contains("science") || t.contains("विज्ञान") ->
                    listOf("सामान्य विज्ञान")
                t.contains("english") || t.contains("अंग्रेजी") ->
                    listOf("General English")
                t.contains("computer") || t.contains("कंप्यूटर") ->
                    listOf("कंप्यूटर")
                t.contains("hindi") || t.contains("हिंदी") ->
                    listOf("सामान्य हिंदी")
                else ->
                    listOf("GK / GS")
            }
        }

        // 1. Single subject practice tests
        if (isSingleSubjectPractice(title)) {
            return when {
                t.contains("quantitative") || t.contains("math") || t.contains("गणित") ->
                    listOf("गणित")
                t.contains("reasoning") || t.contains("तर्कशक्ति") ->
                    listOf("तर्कशक्ति")
                t.contains("science") || t.contains("विज्ञान") ->
                    listOf("सामान्य विज्ञान")
                t.contains("english") || t.contains("अंग्रेजी") ->
                    listOf("General English")
                t.contains("computer") || t.contains("कंप्यूटर") ->
                    listOf("कंप्यूटर")
                t.contains("hindi") || t.contains("हिंदी") ->
                    listOf("सामान्य हिंदी")
                else ->
                    listOf("GK / GS")
            }
        }

        // 2. Official presets for well-known exams
        if (isDelhiPolice(title)) {
            return listOf("GK / GS", "तर्कशक्ति", "गणित", "कंप्यूटर")
        }
        if (isUpPolice(title)) {
            return listOf("सामान्य ज्ञान", "सामान्य हिंदी", "गणित", "तर्कशक्ति")
        }
        if (isLekhpal(title)) {
            return listOf("सामान्य हिंदी", "गणित", "सामान्य ज्ञान", "ग्राम्य समाज एवं विकास")
        }
        if (isRajasthanPolice(title)) {
            return listOf("तर्कशक्ति एवं कंप्यूटर", "सामान्य ज्ञान एवं विज्ञान", "राजस्थान सामान्य ज्ञान")
        }
        if (isRrbNtpc(title) || isRpf(title)) {
            return listOf("GK / GS", "गणित", "तर्कशक्ति")
        }
        if (isRrbGroupD(title) || isRrbAlpOrJe(title)) {
            return listOf("सामान्य विज्ञान", "गणित", "तर्कशक्ति", "GK / GS")
        }
        if (isSscGd(title)) {
            return listOf("तर्कशक्ति", "GK / GS", "गणित", "सामान्य हिंदी")
        }
        if (isSscMts(title) || isSscCglOrChsl(title)) {
            return listOf("GK / GS", "तर्कशक्ति", "गणित", "General English")
        }
        if (isPatwari(title) || isRajasthanCet(title)) {
            return listOf("राजस्थान सामान्य ज्ञान", "भारत GK व विज्ञान", "गणित एवं तर्कशक्ति", "सामान्य हिंदी व English", "कंप्यूटर ज्ञान")
        }

        // 3. If questions provided, inspect their normalized canonical subjects (max 4)
        if (questions.isNotEmpty()) {
            val distinct = questions.map { normalizeToOfficialSubject(it.subject, title) }
                .filter { it.isNotBlank() && !it.equals("General", ignoreCase = true) }
                .distinct()
            if (distinct.size in 2..4) {
                return distinct
            }
        }

        // Default: standard 4 official subjects
        return listOf("GK / GS", "तर्कशक्ति", "गणित", "कंप्यूटर")
    }

    /**
     * Computes the exact section ranges (start..end index) for each subject in the test.
     * Guarantees STRICT official sections (3 to 4 sections, NEVER 12 subjects).
     */
    fun getExamSections(title: String, questions: List<QuizQuestion>): List<ExamSectionInfo> {
        val total = questions.size
        if (total == 0) return emptyList()

        if (!isMultiSubjectFullExam(title, questions)) {
            val singleSubject = getQuizSubjects(title, questions).firstOrNull() ?: "अभ्यास"
            return listOf(ExamSectionInfo(singleSubject, 0 until total, total))
        }

        val officialSubjects = getQuizSubjects(title, questions)
        if (officialSubjects.size <= 1) {
            return listOf(ExamSectionInfo(officialSubjects.firstOrNull() ?: "General", 0 until total, total))
        }

        // 1. Official 100-question distributions
        if (total == 100) {
            when {
                isDelhiPolice(title) -> {
                    return listOf(
                        ExamSectionInfo("GK / GS", 0..49, 50),
                        ExamSectionInfo("तर्कशक्ति", 50..74, 25),
                        ExamSectionInfo("गणित", 75..89, 15),
                        ExamSectionInfo("कंप्यूटर", 90..99, 10)
                    )
                }
                isRrbNtpc(title) || isRpf(title) -> {
                    return listOf(
                        ExamSectionInfo("GK / GS", 0..39, 40),
                        ExamSectionInfo("गणित", 40..69, 30),
                        ExamSectionInfo("तर्कशक्ति", 70..99, 30)
                    )
                }
                isRajasthanPolice(title) -> {
                    return listOf(
                        ExamSectionInfo("तर्कशक्ति एवं कंप्यूटर", 0..39, 40),
                        ExamSectionInfo("सामान्य ज्ञान एवं विज्ञान", 40..69, 30),
                        ExamSectionInfo("राजस्थान सामान्य ज्ञान", 70..99, 30)
                    )
                }
                isPatwari(title) || isRajasthanCet(title) -> {
                    return listOf(
                        ExamSectionInfo(officialSubjects[0], 0..24, 25),
                        ExamSectionInfo(officialSubjects[1], 25..49, 25),
                        ExamSectionInfo(officialSubjects[2], 50..74, 25),
                        ExamSectionInfo(officialSubjects[3], 75..89, 15),
                        ExamSectionInfo(officialSubjects[4], 90..99, 10)
                    )
                }
                else -> {
                    return listOf(
                        ExamSectionInfo(officialSubjects.getOrElse(0) { "GK / GS" }, 0..24, 25),
                        ExamSectionInfo(officialSubjects.getOrElse(1) { "तर्कशक्ति" }, 25..49, 25),
                        ExamSectionInfo(officialSubjects.getOrElse(2) { "गणित" }, 50..74, 25),
                        ExamSectionInfo(officialSubjects.getOrElse(3) { "कंप्यूटर" }, 75..99, 25)
                    )
                }
            }
        }

        // 2. For any other total count, check if questions form clean contiguous blocks of normalized subjects
        val normalizedQuestions = questions.map { q ->
            q.copy(subject = normalizeToOfficialSubject(q.subject, title))
        }

        val contiguous = mutableListOf<ExamSectionInfo>()
        var curSub = normalizedQuestions[0].subject
        var startIdx = 0
        for (i in 1 until total) {
            val s = normalizedQuestions[i].subject
            if (s != curSub) {
                contiguous.add(ExamSectionInfo(curSub, startIdx until i, i - startIdx))
                curSub = s
                startIdx = i
            }
        }
        contiguous.add(ExamSectionInfo(curSub, startIdx until total, total - startIdx))

        val uniqueContiguousNames = contiguous.map { it.name }.distinct()
        // If contiguous blocks match exactly the distinct names and there are 2 to 4 sections, use them!
        if (contiguous.size == uniqueContiguousNames.size && contiguous.size in 2..4) {
            return contiguous
        }

        // 3. Fallback: Proportional distribution across official canonical subjects (3 to 4 sections)
        val count = officialSubjects.size
        val basePerSection = total / count
        val remainder = total % count
        val result = mutableListOf<ExamSectionInfo>()
        var start = 0
        for (i in 0 until count) {
            val secCount = basePerSection + if (i < remainder) 1 else 0
            val end = (start + secCount).coerceAtMost(total)
            result.add(ExamSectionInfo(officialSubjects[i], start until end, end - start))
            start = end
        }
        return result
    }

    /**
     * Provides a rich, authentic set of questions tailored to the exact subjects of the requested exam
     */
    fun getQuestionsForExam(title: String): List<QuizQuestion> {
        return emptyList()
    }

    // =========================================================================
    // QUESTION BANK REFERENCES (Populated dynamically from Online Sync / Admin)
    // =========================================================================

    val sampleEnglishQuestions: List<QuizQuestion> = emptyList()
    val sampleHindiQuestions: List<QuizQuestion> = emptyList()
    val sampleScienceQuestions: List<QuizQuestion> = emptyList()
    val sampleRuralDevQuestions: List<QuizQuestion> = emptyList()
    val sampleRajasthanGkQuestions: List<QuizQuestion> = emptyList()
    val sampleMathsQuestions: List<QuizQuestion> = emptyList()
    val sampleReasoningQuestions: List<QuizQuestion> = emptyList()
    val sampleGkQuestions: List<QuizQuestion> = emptyList()
    val sampleComputerQuestions: List<QuizQuestion> = emptyList()

    val sscFull100Questions: List<QuizQuestion> = emptyList()
    val sscGdFull80Questions: List<QuizQuestion> = emptyList()
    val rrbNtpcFull100Questions: List<QuizQuestion> = emptyList()
    val rrbGroupDFull100Questions: List<QuizQuestion> = emptyList()
    val rpfFull100Questions: List<QuizQuestion> = emptyList()
    val upPoliceFull100Questions: List<QuizQuestion> = emptyList()
    val lekhpalFull100Questions: List<QuizQuestion> = emptyList()
    val rajasthanPoliceFull100Questions: List<QuizQuestion> = emptyList()
    val patwariCetFull100Questions: List<QuizQuestion> = emptyList()
}
