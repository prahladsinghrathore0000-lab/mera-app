package com.example.model

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import org.json.JSONArray
import org.json.JSONObject

/**
 * Mastery level for a topic or subject.
 */
enum class MasteryLevel(val label: String, val hindiLabel: String) {
    WEAK("Weak Area", "कमज़ोर टॉपिक (सुधारें)"),
    MODERATE("Moderate", "औसत (प्रैक्टिस करें)"),
    STRONG("Strong", "मजबूत पकड़ (शानदार)")
}

/**
 * Topic weakness / strength statistics.
 */
data class TopicWeaknessStat(
    val topicName: String,
    val subjectName: String,
    val totalCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val skippedCount: Int,
    val accuracy: Int,
    val masteryLevel: MasteryLevel,
    val recommendation: String
)

/**
 * Subject level summary with top weak and strong topics.
 */
data class SubjectWeaknessSummary(
    val subjectName: String,
    val totalQuestions: Int,
    val correctQuestions: Int,
    val wrongQuestions: Int,
    val skippedQuestions: Int,
    val accuracy: Int,
    val masteryLevel: MasteryLevel,
    val topics: List<TopicWeaknessStat>
)

/**
 * Detailed record of a question answered wrongly or skipped.
 */
data class MistakeQuestionRecord(
    val questionId: Int,
    val testTitle: String,
    val subject: String,
    val topic: String,
    val questionText: String,
    val options: List<String>,
    val correctIndex: Int,
    val userSelectedOption: Int?, // null if skipped
    val explanation: String,
    val timestamp: Long
)

/**
 * Topic Detector: Intelligently categorizes questions into official syllabus topics
 * across all major competitive exam subjects.
 */
object TopicDetector {
    fun detectTopic(question: QuizQuestion): String {
        val q = question.question.lowercase()
        val s = question.subject.lowercase()

        return when {
            // 1. Quantitative Aptitude / Mathematics
            s.contains("गणित") || s.contains("math") || s.contains("अंकगणित") || s.contains("संख्यात्मक") -> {
                when {
                    q.contains("लाभ") || q.contains("हानि") || q.contains("profit") || q.contains("loss") || q.contains("क्रय") || q.contains("विक्रय") || q.contains("बट्टा") || q.contains("discount") ->
                        "लाभ-हानि एवं बट्टा (Profit & Loss)"
                    q.contains("प्रतिशत") || q.contains("percentage") || q.contains("%") ->
                        "प्रतिशत (Percentage)"
                    q.contains("ब्याज") || q.contains("interest") || q.contains("चक्रवृद्धि") || q.contains("साधारण ब्याज") || q.contains("ci") || q.contains("si") ->
                        "साधारण व चक्रवृद्धि ब्याज (SI & CI)"
                    q.contains("चाल") || q.contains("दूरी") || q.contains("speed") || q.contains("distance") || q.contains("ट्रेन") || q.contains("रेलगाड़ी") || q.contains("km/h") || q.contains("मीटर/सेकंड") ->
                        "समय, चाल और दूरी (Time & Distance)"
                    q.contains("कार्य") || q.contains("दिन") || q.contains("work") || q.contains("पाइप") || q.contains("टंकी") || q.contains("cistern") || q.contains("मजदूर") ->
                        "समय और कार्य (Time & Work)"
                    q.contains("अनुपात") || q.contains("समानुपात") || q.contains("ratio") || q.contains("proportion") || q.contains("मिश्रण") || q.contains("mixture") ->
                        "अनुपात व मिश्रण (Ratio & Mixture)"
                    q.contains("औसत") || q.contains("average") || q.contains("mean") ->
                        "औसत (Average)"
                    q.contains("क्षेत्रफल") || q.contains("आयतन") || q.contains("त्रिज्या") || q.contains("वृत्त") || q.contains("त्रिभुज") || q.contains("mensuration") || q.contains("area") || q.contains("volume") || q.contains("शंकु") || q.contains("बेलन") ->
                        "क्षेत्रमिति व ज्यामिति (Mensuration)"
                    q.contains("hcf") || q.contains("lcm") || q.contains("ल.स.") || q.contains("म.स.") ->
                        "ल.स. और म.स. (LCM & HCF)"
                    else ->
                        "संख्या पद्धति व सरलीकरण (Number System)"
                }
            }

            // 2. Reasoning / तर्कशक्ति
            s.contains("तर्कशक्ति") || s.contains("reasoning") || s.contains("मानसिक अभिरुचि") -> {
                when {
                    q.contains("कोडिंग") || q.contains("coding") || q.contains("decod") || q.contains("कूट") || q.contains("लिखा जाता है") ->
                        "कोडिंग-डिकोडिंग (Coding-Decoding)"
                    q.contains("शृंखला") || q.contains("श्रृंखला") || q.contains("series") || q.contains("लुप्त पद") || q.contains("अगला पद") || q.contains("pattern") ->
                        "संख्या व अक्षर श्रृंखला (Series)"
                    q.contains("कथन") || q.contains("निष्कर्ष") || q.contains("statement") || q.contains("conclusion") || q.contains("syllogism") || q.contains("न्याय") ->
                        "कथन एवं निष्कर्ष (Syllogism)"
                    q.contains("दिशा") || q.contains("direction") || q.contains("उत्तर") || q.contains("दक्षिण") || q.contains("पूर्व") || q.contains("पश्चिम") || q.contains("बाएं") || q.contains("दाएं") ->
                        "दिशा एवं दूरी परीक्षण (Direction Test)"
                    q.contains("रक्त") || q.contains("पिता") || q.contains("माता") || q.contains("भाई") || q.contains("बहन") || q.contains("blood") || q.contains("relation") || q.contains("चाचा") || q.contains("मामा") ->
                        "रक्त संबंध (Blood Relations)"
                    q.contains("सादृश्य") || q.contains("संबंध") || q.contains("analogy") || q.contains("विषम") || q.contains("वर्गीकरण") || q.contains("odd one") ->
                        "सादृश्यता एवं वर्गीकरण (Analogy)"
                    q.contains("दर्पण") || q.contains("प्रतिबिंब") || q.contains("mirror") || q.contains("water image") || q.contains("कागज") || q.contains("folding") || q.contains("आकृति") ->
                        "अशाब्दिक रीजनिंग (Non-Verbal & Mirror)"
                    q.contains("बैठक") || q.contains("seating") || q.contains("वृत्त में") || q.contains("पंक्ति") || q.contains("रैंकिंग") || q.contains("स्थान") ->
                        "बैठक व्यवस्था व रैंकिंग (Seating Arrangement)"
                    else ->
                        "तार्किक तर्कशक्ति (Logical Reasoning)"
                }
            }

            // 3. Computer Knowledge
            s.contains("कंप्यूटर") || s.contains("computer") -> {
                when {
                    q.contains("excel") || q.contains("word") || q.contains("powerpoint") || q.contains("ms office") || q.contains("स्प्रेडशीट") || q.contains("ctrl+") || q.contains("शॉर्टकट") || q.contains("फ़ॉर्मूला") ->
                        "MS Office (Word, Excel) & Shortcuts"
                    q.contains("ram") || q.contains("rom") || q.contains("memory") || q.contains("byte") || q.contains("bit") || q.contains("मेमोरी") || q.contains("cpu") || q.contains("हार्डवेयर") || q.contains("इनपुट") || q.contains("आउटपुट") ->
                        "Hardware, Memory & CPU Architecture"
                    q.contains("internet") || q.contains("http") || q.contains("ip") || q.contains("नेटवर्क") || q.contains("www") || q.contains("ब्राउज़र") || q.contains("ईमेल") || q.contains("url") || q.contains("tcp") ->
                        "Internet, Protocols & Networking"
                    q.contains("वायरस") || q.contains("virus") || q.contains("malware") || q.contains("साइबर") || q.contains("सुरक्षा") || q.contains("ऑपरेटिंग सिस्टम") || q.contains("windows") || q.contains("linux") ->
                        "OS & Cybersecurity Fundamentals"
                    else ->
                        "कंप्यूटर सामान्य ज्ञान (Computer Basics)"
                }
            }

            // 4. General Science (Physics, Chemistry, Biology)
            s.contains("विज्ञान") || s.contains("science") -> {
                when {
                    q.contains("विटामिन") || q.contains("कोशिका") || q.contains("रोग") || q.contains("रक्त") || q.contains("हृदय") || q.contains("biology") || q.contains("पादप") || q.contains("प्रकाश संश्लेषण") || q.contains("जीवाणु") || q.contains("हार्मोन") ->
                        "जीव विज्ञान (Biology & Human Physiology)"
                    q.contains("अम्ल") || q.contains("क्षार") || q.contains("तत्व") || q.contains("परमाणु") || q.contains("chemistry") || q.contains("गैस") || q.contains("धातु") || q.contains("आवर्त सारणी") || q.contains("ph मान") ->
                        "रसायन विज्ञान (Chemistry & Reactions)"
                    else ->
                        "भौतिक विज्ञान (Physics, Units & Motion)"
                }
            }

            // 5. General Hindi
            s.contains("हिंदी") || s.contains("hindi") -> {
                when {
                    q.contains("संधि") || q.contains("समास") || q.contains("उपसर्ग") || q.contains("प्रत्यय") ->
                        "संधि, समास व उपसर्ग-प्रत्यय"
                    q.contains("विलोम") || q.contains("पर्यायवाची") || q.contains("समानार्थी") || q.contains("अनेकार्थी") ->
                        "विलोम, पर्यायवाची व शब्द ज्ञान"
                    q.contains("मुहावरे") || q.contains("लोकोक्ति") || q.contains("कहावत") ->
                        "मुहावरे एवं लोकोक्तियां"
                    q.contains("शुद्ध") || q.contains("वर्तनी") || q.contains("अशुद्ध") || q.contains("त्रुटि") ->
                        "वर्तनी एवं वाक्य शुद्धि"
                    else ->
                        "हिंदी व्याकरण व रचना"
                }
            }

            // 6. English Language
            s.contains("english") || s.contains("अंग्रेजी") -> {
                when {
                    q.contains("synonym") || q.contains("antonym") || q.contains("spelt") || q.contains("spelling") || q.contains("idiom") || q.contains("one word") || q.contains("phrase") ->
                        "Vocabulary, Idioms & Spelling"
                    q.contains("error") || q.contains("grammatical") || q.contains("blank") || q.contains("tense") || q.contains("preposition") || q.contains("voice") || q.contains("narration") ->
                        "Grammar, Tenses & Error Spotting"
                    else ->
                        "Comprehension & Sentence Structure"
                }
            }

            // 7. Rajasthan GK
            s.contains("राजस्थान") || s.contains("rajasthan") -> {
                when {
                    q.contains("नदी") || q.contains("पहाड़") || q.contains("अभयारण्य") || q.contains("जलवायु") || q.contains("खनिज") || q.contains("अरावली") || q.contains("झील") ->
                        "राजस्थान का भूगोल व नदियां"
                    q.contains("किला") || q.contains("दुर्ग") || q.contains("मंदिर") || q.contains("मेला") || q.contains("नृत्य") || q.contains("चित्रकला") || q.contains("त्योहार") || q.contains("लोक देवता") ->
                        "राजस्थान कला, संस्कृति व मेले"
                    else ->
                        "राजस्थान इतिहास व प्रशासनिक व्यवस्था"
                }
            }

            // 8. Lekhpal Rural Development
            s.contains("ग्राम्य") || s.contains("ग्रामीण") || s.contains("lekhpal") -> {
                "ग्राम्य समाज, भूमि पैमाइश व सरकारी योजनाएं"
            }

            // 9. General Knowledge (GK / GS)
            else -> {
                when {
                    q.contains("अनुच्छेद") || q.contains("संविधान") || q.contains("राष्ट्रपति") || q.contains("संसद") || q.contains("न्यायालय") || q.contains("मौलिक") || q.contains("polity") || q.contains("राज्यपाल") || q.contains("संशोधन") ->
                        "भारतीय संविधान एवं राजव्यवस्था (Polity)"
                    q.contains("नदी") || q.contains("पर्वत") || q.contains("दर्रा") || q.contains("झील") || q.contains("मिट्टी") || q.contains("geography") || q.contains("राजधानी") || q.contains("वन") || q.contains("बांध") || q.contains("कर्क रेखा") ->
                        "भारत एवं विश्व का भूगोल (Geography)"
                    q.contains("मुगल") || q.contains("मौर्य") || q.contains("गांधी") || q.contains("आंदोलन") || q.contains("1857") || q.contains("क्रांति") || q.contains("history") || q.contains("सल्तनत") || q.contains("सिंधु") || q.contains("वैदिक") ->
                        "भारतीय इतिहास व राष्ट्रीय आंदोलन (History)"
                    q.contains("बैंक") || q.contains("rbi") || q.contains("जीडीपी") || q.contains("बजट") || q.contains("मुद्रा") || q.contains("economy") || q.contains("योजना") || q.contains("नीति आयोग") ->
                        "भारतीय अर्थव्यवस्था व बजट (Economy)"
                    q.contains("नृत्य") || q.contains("त्योहार") || q.contains("पुरस्कार") || q.contains("दिवस") || q.contains("पुस्तक") || q.contains("खिलाड़ी") || q.contains("ओलंपिक") || q.contains("राजधानी") || q.contains("मुख्यालय") ->
                        "स्टैटिक GK (कला, संस्कृति, दिवस व पुरस्कार)"
                    else ->
                        "सामान्य ज्ञान एवं करेंट अफेयर्स"
                }
            }
        }
    }
}

/**
 * Reactive Repository that aggregates topic-wise and subject-wise accuracy,
 * identifies weak topics in real-time, and stores all user mistakes and skipped questions.
 */
object WeakAreaAnalyticsRepository {
    private const val PREFS_NAME = "exam_mitra_weak_analytics"
    private const val KEY_TOPICS_JSON = "saved_topic_stats_json"
    private const val KEY_MISTAKES_JSON = "saved_mistakes_json"
    private const val KEY_COMPLETED_TESTS_COUNT = "saved_completed_tests_count"
    private const val KEY_DATA_VERSION = "analytics_data_version"
    private const val CURRENT_VERSION = 2

    const val REQUIRED_TESTS_FOR_ANALYSIS = 10

    // Key: "Subject___TopicName" -> TopicWeaknessStat
    val topicStatsMap: SnapshotStateMap<String, TopicWeaknessStat> = mutableStateMapOf()

    // List of recent mistakes & skipped questions for review
    val mistakeQuestionsList: SnapshotStateList<MistakeQuestionRecord> = mutableStateListOf()

    // Number of full mock tests completed by the user
    private val _completedTestsCount = androidx.compose.runtime.mutableIntStateOf(0)
    var rawCompletedTestsCount: Int
        get() = _completedTestsCount.intValue
        set(value) { _completedTestsCount.intValue = value }

    private var prefs: SharedPreferences? = null
    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadFromPrefs()
            isInitialized = true
        }
    }

    private fun loadFromPrefs() {
        prefs?.let { p ->
            val version = p.getInt(KEY_DATA_VERSION, 1)
            // If legacy version with dummy pre-seeded data, clear it to start clean with authentic user data
            if (version < CURRENT_VERSION) {
                p.edit().clear().putInt(KEY_DATA_VERSION, CURRENT_VERSION).apply()
                topicStatsMap.clear()
                mistakeQuestionsList.clear()
                rawCompletedTestsCount = 0
                return
            }

            rawCompletedTestsCount = p.getInt(KEY_COMPLETED_TESTS_COUNT, 0)

            val topicsJson = p.getString(KEY_TOPICS_JSON, null)
            if (topicsJson != null) {
                try {
                    val arr = JSONArray(topicsJson)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val stat = TopicWeaknessStat(
                            topicName = obj.getString("topicName"),
                            subjectName = obj.getString("subjectName"),
                            totalCount = obj.getInt("totalCount"),
                            correctCount = obj.getInt("correctCount"),
                            wrongCount = obj.getInt("wrongCount"),
                            skippedCount = obj.getInt("skippedCount"),
                            accuracy = obj.getInt("accuracy"),
                            masteryLevel = MasteryLevel.valueOf(obj.getString("masteryLevel")),
                            recommendation = obj.getString("recommendation")
                        )
                        val key = "${stat.subjectName}___${stat.topicName}"
                        topicStatsMap[key] = stat
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            val mistakesJson = p.getString(KEY_MISTAKES_JSON, null)
            if (mistakesJson != null) {
                try {
                    val arr = JSONArray(mistakesJson)
                    mistakeQuestionsList.clear()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val optionsArr = obj.getJSONArray("options")
                        val optionsList = mutableListOf<String>()
                        for (j in 0 until optionsArr.length()) {
                            optionsList.add(optionsArr.getString(j))
                        }
                        val userAns = if (obj.has("userSelectedOption") && !obj.isNull("userSelectedOption")) {
                            obj.getInt("userSelectedOption")
                        } else null

                        mistakeQuestionsList.add(
                            MistakeQuestionRecord(
                                questionId = obj.getInt("questionId"),
                                testTitle = obj.getString("testTitle"),
                                subject = obj.getString("subject"),
                                topic = obj.getString("topic"),
                                questionText = obj.getString("questionText"),
                                options = optionsList,
                                correctIndex = obj.getInt("correctIndex"),
                                userSelectedOption = userAns,
                                explanation = obj.optString("explanation", ""),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                            )
                        )
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    private fun saveToPrefs() {
        prefs?.let { p ->
            try {
                val topicsArr = JSONArray()
                topicStatsMap.values.forEach { stat ->
                    val obj = JSONObject()
                    obj.put("topicName", stat.topicName)
                    obj.put("subjectName", stat.subjectName)
                    obj.put("totalCount", stat.totalCount)
                    obj.put("correctCount", stat.correctCount)
                    obj.put("wrongCount", stat.wrongCount)
                    obj.put("skippedCount", stat.skippedCount)
                    obj.put("accuracy", stat.accuracy)
                    obj.put("masteryLevel", stat.masteryLevel.name)
                    obj.put("recommendation", stat.recommendation)
                    topicsArr.put(obj)
                }

                val mistakesArr = JSONArray()
                mistakeQuestionsList.take(60).forEach { m ->
                    val obj = JSONObject()
                    obj.put("questionId", m.questionId)
                    obj.put("testTitle", m.testTitle)
                    obj.put("subject", m.subject)
                    obj.put("topic", m.topic)
                    obj.put("questionText", m.questionText)
                    val optArr = JSONArray()
                    m.options.forEach { optArr.put(it) }
                    obj.put("options", optArr)
                    obj.put("correctIndex", m.correctIndex)
                    if (m.userSelectedOption != null) {
                        obj.put("userSelectedOption", m.userSelectedOption)
                    } else {
                        obj.put("userSelectedOption", JSONObject.NULL)
                    }
                    obj.put("explanation", m.explanation)
                    obj.put("timestamp", m.timestamp)
                    mistakesArr.put(obj)
                }

                p.edit()
                    .putInt(KEY_DATA_VERSION, CURRENT_VERSION)
                    .putInt(KEY_COMPLETED_TESTS_COUNT, rawCompletedTestsCount)
                    .putString(KEY_TOPICS_JSON, topicsArr.toString())
                    .putString(KEY_MISTAKES_JSON, mistakesArr.toString())
                    .apply()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    /**
     * Called on EVERY test submission in QuizDialog:
     * Analyzes every question, updates topic correctness/skips/mistakes,
     * recalibrates mastery level, and stores the mistakes for instant review.
     */
    fun recordTestResults(
        context: Context?,
        testTitle: String,
        questions: List<QuizQuestion>,
        userAnswers: Map<Int, Int>
    ) {
        if (questions.isEmpty()) return
        if (context != null && prefs == null) {
            init(context)
        }

        rawCompletedTestsCount++
        val currentTime = System.currentTimeMillis()

        // 1. Group questions by (Subject, Topic)
        val grouped = questions.indices.groupBy { idx ->
            val q = questions[idx]
            val topic = TopicDetector.detectTopic(q)
            q.subject.trim().ifBlank { "GK / GS" } to topic
        }

        grouped.forEach { (pair, indices) ->
            val (subjectName, topicName) = pair
            val key = "${subjectName}___${topicName}"
            val existing = topicStatsMap[key]

            var newTotal = existing?.totalCount ?: 0
            var newCorrect = existing?.correctCount ?: 0
            var newWrong = existing?.wrongCount ?: 0
            var newSkipped = existing?.skippedCount ?: 0

            indices.forEach { idx ->
                val q = questions[idx]
                val userChoice = userAnswers[idx]
                newTotal++

                if (userChoice == null) {
                    // Skipped
                    newSkipped++
                    // Record to mistakes / skipped log
                    mistakeQuestionsList.add(
                        0,
                        MistakeQuestionRecord(
                            questionId = q.id,
                            testTitle = testTitle,
                            subject = subjectName,
                            topic = topicName,
                            questionText = q.question,
                            options = q.options,
                            correctIndex = q.correctIndex,
                            userSelectedOption = null,
                            explanation = q.explanation,
                            timestamp = currentTime
                        )
                    )
                } else if (userChoice == q.correctIndex) {
                    // Correct
                    newCorrect++
                } else {
                    // Wrong
                    newWrong++
                    mistakeQuestionsList.add(
                        0,
                        MistakeQuestionRecord(
                            questionId = q.id,
                            testTitle = testTitle,
                            subject = subjectName,
                            topic = topicName,
                            questionText = q.question,
                            options = q.options,
                            correctIndex = q.correctIndex,
                            userSelectedOption = userChoice,
                            explanation = q.explanation,
                            timestamp = currentTime
                        )
                    )
                }
            }

            val attempted = newCorrect + newWrong
            val accuracy = if (attempted > 0) ((newCorrect * 100.0) / attempted).toInt() else 0

            val mastery = when {
                accuracy < 55 || (newWrong >= 3 && accuracy < 65) -> MasteryLevel.WEAK
                accuracy in 55..74 -> MasteryLevel.MODERATE
                else -> MasteryLevel.STRONG
            }

            val recommendation = when (mastery) {
                MasteryLevel.WEAK -> "इस टॉपिक में ${newWrong} गलतियां और ${newSkipped} प्रश्न छूटे हैं। तुरंत फॉर्मूले व थ्योरी रिवाइज करें।"
                MasteryLevel.MODERATE -> "सटीकता ${accuracy}% है। 15-20 अतिरिक्त अभ्यास प्रश्न हल करके स्पीड व सटीकता बढ़ाएं।"
                MasteryLevel.STRONG -> "शानदार पकड़ (${accuracy}% सटीकता)! परीक्षा तक रिवीज़न जारी रखें।"
            }

            topicStatsMap[key] = TopicWeaknessStat(
                topicName = topicName,
                subjectName = subjectName,
                totalCount = newTotal,
                correctCount = newCorrect,
                wrongCount = newWrong,
                skippedCount = newSkipped,
                accuracy = accuracy,
                masteryLevel = mastery,
                recommendation = recommendation
            )
        }

        // Limit mistakes list to most recent 80 questions to conserve memory
        if (mistakeQuestionsList.size > 80) {
            val trimmed = mistakeQuestionsList.take(80)
            mistakeQuestionsList.clear()
            mistakeQuestionsList.addAll(trimmed)
        }

        saveToPrefs()
    }

    /**
     * Returns all subject summaries calculated dynamically from all topic records.
     */
    fun getSubjectSummaries(): List<SubjectWeaknessSummary> {
        val grouped = topicStatsMap.values.groupBy { it.subjectName }
        return grouped.map { (subj, topicsList) ->
            val totalQ = topicsList.sumOf { it.totalCount }
            val totalCorrect = topicsList.sumOf { it.correctCount }
            val totalWrong = topicsList.sumOf { it.wrongCount }
            val totalSkipped = topicsList.sumOf { it.skippedCount }
            val attempted = totalCorrect + totalWrong
            val acc = if (attempted > 0) ((totalCorrect * 100.0) / attempted).toInt() else 0

            val subjMastery = when {
                acc < 55 -> MasteryLevel.WEAK
                acc in 55..74 -> MasteryLevel.MODERATE
                else -> MasteryLevel.STRONG
            }

            SubjectWeaknessSummary(
                subjectName = subj,
                totalQuestions = totalQ,
                correctQuestions = totalCorrect,
                wrongQuestions = totalWrong,
                skippedQuestions = totalSkipped,
                accuracy = acc,
                masteryLevel = subjMastery,
                topics = topicsList.sortedBy { it.accuracy }
            )
        }.sortedBy { it.accuracy }
    }

    /**
     * Returns top critical weak topics across all subjects.
     */
    fun getTopWeakTopics(limit: Int = 5): List<TopicWeaknessStat> {
        return topicStatsMap.values
            .filter { it.masteryLevel == MasteryLevel.WEAK || it.accuracy < 60 || it.wrongCount >= 3 }
            .sortedWith(compareBy<TopicWeaknessStat> { it.accuracy }.thenByDescending { it.wrongCount + it.skippedCount })
            .take(limit)
    }

    /**
     * Returns top strong topics across all subjects.
     */
    fun getTopStrongTopics(limit: Int = 5): List<TopicWeaknessStat> {
        return topicStatsMap.values
            .filter { it.masteryLevel == MasteryLevel.STRONG || it.accuracy >= 75 }
            .sortedByDescending { it.accuracy }
            .take(limit)
    }

    /**
     * Generates a focused custom practice test from questions in user's weak topics!
     */
    fun generateWeakTopicsPracticeTest(title: String = "कमज़ोर टॉपिक सुधार टेस्ट (Weak Areas Booster)"): List<QuizQuestion> {
        // Collect mistake questions or pull from authentic question banks
        if (mistakeQuestionsList.isNotEmpty()) {
            return mistakeQuestionsList.take(25).map { m ->
                QuizQuestion(
                    id = m.questionId,
                    question = m.questionText,
                    options = m.options,
                    correctIndex = m.correctIndex,
                    explanation = m.explanation,
                    subject = m.subject
                )
            }
        }
        // Fallback to sample maths & reasoning weak pool
        return ExamSubjectManager.sampleMathsQuestions + ExamSubjectManager.sampleReasoningQuestions.take(10)
    }

    /**
     * Returns total completed tests by user.
     */
    fun getCompletedTestsCount(): Int {
        val historyAttempts = TestHistoryRepository.attemptRecords.values.sumOf { it.attemptsCount }
        return maxOf(rawCompletedTestsCount, historyAttempts)
    }

    /**
     * Analysis is unlocked only when user has completed at least 5 tests.
     */
    fun isAnalysisUnlocked(): Boolean {
        return getCompletedTestsCount() >= REQUIRED_TESTS_FOR_ANALYSIS
    }
}
