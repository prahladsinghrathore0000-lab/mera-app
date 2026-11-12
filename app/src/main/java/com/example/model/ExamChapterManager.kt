package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Chapter Test Item representing a specific topic in a subject
 */
data class ChapterItem(
    val id: String,
    val subExamId: String,
    val subjectKey: String,
    val subjectTitle: String,
    val chapterNumber: Int,
    val title: String,
    val englishTitle: String,
    val description: String = "",
    val totalQuestions: Int = 15,
    val durationMinutes: Int = 20,
    val totalMarks: Int = 15,
    val difficulty: String = "मध्यम",
    val questions: List<QuizQuestion> = emptyList(),
    val year: String = "2025"
)

/**
 * Subject Category containing all its chapters
 */
data class ExamSubjectCategory(
    val key: String,
    val name: String,
    val englishName: String,
    val icon: ImageVector,
    val gradientColors: List<Color>,
    val chapters: List<ChapterItem>
)

/**
 * Central Chapter Management Engine for All Competitive Exams (SSC, Railway, State Exams, Police, etc.)
 */
object ExamChapterManager {

    /**
     * Returns the official list of subject categories with all their chapter buttons for any given exam.
     */
    fun getSubjectsForExam(subExamId: String): List<ExamSubjectCategory> {
        val normId = DynamicTestRepository.normalizeSubExamId(subExamId)

        return when (normId) {
            // 1. Delhi Police Constable / Head Constable
            "ssc_delhi_police" -> listOf(
                createGkSubject(normId, "सामान्य ज्ञान व करंट अफेयर्स (GK / GS)"),
                createReasoningSubject(normId, "तर्कशक्ति (Reasoning)"),
                createMathsSubject(normId, "संख्यात्मक योग्यता (Mathematics)"),
                createComputerSubject(normId, "कंप्यूटर ज्ञान (Computer Knowledge)")
            )

            // 2. SSC CGL / CHSL / CPO / MTS
            "ssc_cgl", "ssc_chsl", "ssc_cpo" -> listOf(
                createReasoningSubject(normId, "तर्कशक्ति (General Intelligence & Reasoning)"),
                createMathsSubject(normId, "गणित (Quantitative Aptitude)"),
                createGkSubject(normId, "सामान्य ज्ञान (General Awareness)"),
                createEnglishSubject(normId, "General English (अंग्रेज़ी भाषा)")
            )

            "ssc_mts" -> listOf(
                createMathsSubject(normId, "संख्यात्मक एवं गणितीय क्षमता (Numerical Math)"),
                createReasoningSubject(normId, "तर्कशक्ति क्षमता (Reasoning Ability)"),
                createGkSubject(normId, "सामान्य जागरूकता (General Awareness)"),
                createEnglishSubject(normId, "English Language & Comprehension")
            )

            // 3. SSC GD Constable
            "ssc_gd" -> listOf(
                createReasoningSubject(normId, "सामान्य बुद्धिमत्ता एवं तर्कशक्ति"),
                createGkSubject(normId, "सामान्य ज्ञान एवं सामान्य जानकारी"),
                createMathsSubject(normId, "प्रारंभिक गणित (Elementary Mathematics)"),
                createHindiSubject(normId, "सामान्य हिंदी (General Hindi)"),
                createEnglishSubject(normId, "General English")
            )

            // 4. Railway RRB NTPC / RPF
            "rrb_ntpc", "rpf_si" -> listOf(
                createGkSubject(normId, "सामान्य जागरूकता (General Awareness)"),
                createMathsSubject(normId, "गणित (Mathematics)"),
                createReasoningSubject(normId, "सामान्य बुद्धिमत्ता एवं तर्कशक्ति (Reasoning)")
            )

            // 5. Railway RRB Group D / ALP / Tech / JE
            "rrb_group_d", "rrb_alp", "rrb_je" -> listOf(
                createScienceSubject(normId, "सामान्य विज्ञान (General Science)"),
                createMathsSubject(normId, "गणित (Mathematics)"),
                createReasoningSubject(normId, "सामान्य बुद्धिमत्ता एवं तर्कशक्ति (Reasoning)"),
                createGkSubject(normId, "सामान्य जागरूकता एवं करंट अफेयर्स")
            )

            // 6. UP Police Constable / SI
            "up_police" -> listOf(
                createGkSubject(normId, "सामान्य ज्ञान (General Knowledge)"),
                createHindiSubject(normId, "सामान्य हिंदी (General Hindi)"),
                createMathsSubject(normId, "संख्यात्मक एवं मानसिक योग्यता (Maths)"),
                createReasoningSubject(normId, "मानसिक अभिरुचि, बुद्धिलब्धि एवं तार्किक क्षमता"),
                createLawSubject(normId, "मूलविधि, संविधान एवं पुलिस प्रणाली")
            )

            // 7. UP Lekhpal
            "lekhpal" -> listOf(
                createHindiSubject(normId, "सामान्य हिंदी (General Hindi)"),
                createMathsSubject(normId, "गणित (Mathematics)"),
                createGkSubject(normId, "सामान्य ज्ञान (General Knowledge)"),
                createLekhpalRuralSubject(normId, "ग्राम्य समाज एवं विकास (Rural Development)")
            )

            // 8. Rajasthan Police Constable
            "rajasthan_police" -> listOf(
                createRajasthanGkSubject(normId, "राजस्थान का इतिहास, कला, संस्कृति एवं भूगोल"),
                createReasoningSubject(normId, "विवेचना एवं तार्किक योग्यता"),
                createComputerSubject(normId, "कंप्यूटर का सामान्य ज्ञान"),
                createGkSubject(normId, "सामान्य ज्ञान, विज्ञान एवं समसामयिकी")
            )

            // 9. Patwari & Rajasthan CET
            "patwari", "rajasthan_cet" -> listOf(
                createRajasthanGkSubject(normId, "राजस्थान का सामान्य ज्ञान (Rajasthan GK)"),
                createGkSubject(normId, "भारत का संविधान, इतिहास एवं भूगोल"),
                createScienceSubject(normId, "दैनिक विज्ञान (General Science)"),
                createMathsSubject(normId, "गणित (Mathematics)"),
                createReasoningSubject(normId, "तार्किक योग्यता (Reasoning)"),
                createHindiSubject(normId, "सामान्य हिंदी"),
                createEnglishSubject(normId, "General English"),
                createComputerSubject(normId, "कंप्यूटर ज्ञान (Computer Basics)")
            )

            // Default fallback: 4 Core Subjects
            else -> listOf(
                createMathsSubject(normId, "गणित (Mathematics)"),
                createReasoningSubject(normId, "तर्कशक्ति (Reasoning)"),
                createGkSubject(normId, "सामान्य ज्ञान व विज्ञान (GK / GS)"),
                createHindiSubject(normId, "सामान्य हिंदी (General Hindi)")
            )
        }
    }

    /**
     * Converts a ChapterItem into a fully executable TestPaper object
     */
    fun getChapterTestPaper(chapter: ChapterItem): TestPaper {
        val questions = if (chapter.questions.isNotEmpty()) {
            chapter.questions
        } else {
            generateQuestionsForChapter(chapter)
        }

        val isMathOrReasoning = chapter.subjectKey.contains("MATH", true) ||
                chapter.subjectKey.contains("REASON", true) ||
                chapter.subjectTitle.contains("गणित") ||
                chapter.subjectTitle.contains("तर्क") ||
                chapter.subjectTitle.contains("रीजनिंग")
        val effectiveDuration = if (isMathOrReasoning) {
            questions.size * 2
        } else {
            chapter.durationMinutes
        }

        return TestPaper(
            id = chapter.id,
            title = "${chapter.subjectTitle} : ${chapter.title}",
            totalQuestions = questions.size,
            durationMinutes = effectiveDuration,
            totalMarks = questions.size,
            difficulty = chapter.difficulty,
            year = "चैप्टर टेस्ट",
            questions = questions
        )
    }

    // =========================================================================
    // SUBJECT & CHAPTER CREATION FACTORIES
    // =========================================================================

    private fun buildChapterItem(
        subExamId: String,
        subjectKey: String,
        subjectTitle: String,
        idx: Int,
        hName: String,
        eName: String,
        desc: String,
        difficulty: String
    ): ChapterItem {
        return ChapterItem(
            id = "${subExamId}_${subjectKey.lowercase()}_ch_${idx + 1}",
            subExamId = subExamId,
            subjectKey = subjectKey,
            subjectTitle = subjectTitle,
            chapterNumber = idx + 1,
            title = "${idx + 1}. $hName",
            englishTitle = eName,
            description = desc,
            totalQuestions = 0,
            durationMinutes = 0,
            totalMarks = 0,
            difficulty = difficulty,
            questions = emptyList()
        )
    }

    private fun createMathsSubject(subExamId: String, title: String): ExamSubjectCategory {
        val chapters = listOf(
            Triple("प्रतिशत", "Percentage", "भिन्न, प्रतिशत वृद्धि/कमी, खपत एवं मूल्य आधारित प्रश्न"),
            Triple("लाभ, हानि एवं बट्टा", "Profit, Loss & Discount", "क्रय-विक्रय मूल्य, अंकित मूल्य, क्रमिक छूट व बेईमान दुकानदार"),
            Triple("साधारण एवं चक्रवृद्धि ब्याज", "Simple & Compound Interest", "दर, समय, मिश्रधन, SI और CI का अंतर व किस्त आधारित प्रश्न"),
            Triple("अनुपात, समानुपात एवं साझेदारी", "Ratio, Proportion & Partnership", "समानुपाती भाग, मिश्रण, सिक्कों वाले प्रश्न एवं व्यापार साझेदारी"),
            Triple("समय और कार्य / नल और टंकी", "Time & Work / Pipes & Cisterns", "कार्य क्षमता, बारी-बारी से काम, नल भरने व खाली करने के प्रश्न"),
            Triple("चाल, समय और दूरी / रेलगाड़ी", "Speed, Time & Distance / Trains", "औसत गति, सापेक्ष चाल, प्लेटफार्म पार करना एवं विपरीत दिशा"),
            Triple("संख्या पद्धति एवं सरलीकरण", "Number System & Simplification", "BODMAS नियम, इकाई अंक, शेषफल प्रमेय, अभाज्य संख्याएं"),
            Triple("ल.स.प. तथा म.स.प.", "LCM & HCF", "गुणनखंड विधि, घंटियों वाले प्रश्न, वृत्ताकार मार्ग पर मिलना"),
            Triple("औसत एवं आयु सम्बन्धी प्रश्न", "Average & Age Problems", "समूह में शामिल/बाहर होना, औसत चाल, पूर्व व वर्तमान आयु अनुपात"),
            Triple("मिश्रण एवं पृथक्करण", "Alligation & Mixture", "दूध-पानी का अनुपात, प्रतिस्थापन विधि, मिश्रित मूल्य ज्ञात करना"),
            Triple("क्षेत्रमिति (2D एवं 3D)", "Mensuration 2D & 3D", "त्रिभुज, आयत, वृत्त, बेलन, शंकु, गोला का क्षेत्रफल व आयतन"),
            Triple("बीजगणित एवं त्रिकोणमिति", "Algebra & Trigonometry", "सर्वसमिकाएं, बहुपद, त्रिकोणमितीय अनुपात व ऊंचाई-दूरी"),
            Triple("आंकड़ों की व्याख्या (DI)", "Data Interpretation", "पाई चार्ट, बार ग्राफ, लाइन ग्राफ एवं टेबल आधारित विश्लेषणात्मक प्रश्न")
        )

        return ExamSubjectCategory(
            key = "MATHS",
            name = title,
            englishName = "Mathematics & Quantitative Aptitude",
            icon = Icons.Default.Calculate,
            gradientColors = listOf(Color(0xFF2563EB), Color(0xFF38BDF8)),
            chapters = chapters.mapIndexed { idx, (hName, eName, desc) ->
                buildChapterItem(
                    subExamId = subExamId,
                    subjectKey = "MATHS",
                    subjectTitle = "गणित",
                    idx = idx,
                    hName = hName,
                    eName = eName,
                    desc = desc,
                    difficulty = if (idx in listOf(2, 4, 10, 11)) "कठिन" else "मध्यम"
                )
            }
        )
    }

    private fun createReasoningSubject(subExamId: String, title: String): ExamSubjectCategory {
        val chapters = listOf(
            Triple("कोडिंग और डिकोडिंग", "Coding & Decoding", "अक्षर कोडिंग, संख्या कोडिंग, प्रतिस्थापन एवं बाइनरी कोडिंग"),
            Triple("सादृश्यता एवं वर्गीकरण", "Analogy & Classification", "शब्द, संख्या व अक्षर सादृश्यता, विषम पद का चयन"),
            Triple("संख्या एवं वर्णमाला श्रृंखला", "Series Completion", "अंक श्रृंखला, अक्षर श्रृंखला, गलत पद व लुप्त पद खोजना"),
            Triple("रक्त संबंध", "Blood Relations", "इशारा करने वाले प्रश्न, पारिवारिक वृक्ष एवं कोडेड ब्लड रिलेशन"),
            Triple("दिशा और दूरी परीक्षण", "Direction & Distance", "प्रारंभिक बिंदु से दूरी, दिशा परिवर्तन, परछाई व पाइथागोरस प्रमेय"),
            Triple("बैठक व्यवस्था एवं क्रम परीक्षण", "Seating Arrangement & Ranking", "पंक्ति में स्थान, वृत्ताकार व वर्गाकार मेज पर बैठक"),
            Triple("न्याय निगमन (कथन एवं निष्कर्ष)", "Syllogism & Statements", "सभी, कुछ, कोई नहीं आधारित तार्किक निष्कर्ष"),
            Triple("पासा एवं घन", "Dice & Cube", "मानक पासा, साधारण पासा, खुला पासा एवं रंगे हुए घन"),
            Triple("घड़ी एवं कैलेंडर", "Clock & Calendar", "सुइयों के बीच कोण, समकोण, दर्पण प्रतिबिंब एवं दिन ज्ञात करना"),
            Triple("वेन आरेख", "Venn Diagram", "ज्यामितीय वेन आरेख एवं तीन समूहों का पारस्परिक संबंध"),
            Triple("लुप्त संख्या एवं गणितीय संक्रियाएं", "Missing Number & Mathematical Ops", "मैट्रिक्स में लुप्त पद, चिह्नों का प्रतिस्थापन"),
            Triple("अशाब्दिक तर्कशक्ति (दर्पण व जल छवि)", "Non-Verbal: Mirror & Water Image", "दर्पण प्रतिबिंब, जल प्रतिबिंब, कागज मोड़ना व काटना")
        )

        return ExamSubjectCategory(
            key = "REASONING",
            name = title,
            englishName = "General Intelligence & Reasoning",
            icon = Icons.Default.Psychology,
            gradientColors = listOf(Color(0xFF7C3AED), Color(0xFFA78BFA)),
            chapters = chapters.mapIndexed { idx, (hName, eName, desc) ->
                buildChapterItem(
                    subExamId = subExamId,
                    subjectKey = "REASONING",
                    subjectTitle = "तर्कशक्ति",
                    idx = idx,
                    hName = hName,
                    eName = eName,
                    desc = desc,
                    difficulty = if (idx in listOf(5, 6, 8)) "कठिन" else "मध्यम"
                )
            }
        )
    }

    private fun createGkSubject(subExamId: String, title: String): ExamSubjectCategory {
        val chapters = listOf(
            Triple("भारतीय संविधान एवं राजव्यवस्था", "Indian Polity & Constitution", "प्रस्तावना, मौलिक अधिकार, नीति निदेशक तत्व, संसद, राष्ट्रपति व अनुच्छेद"),
            Triple("प्राचीन भारत का इतिहास", "Ancient Indian History", "सिंधु घाटी सभ्यता, वैदिक काल, मौर्य वंश, गुप्त काल व बौद्ध/जैन धर्म"),
            Triple("मध्यकालीन भारत का इतिहास", "Medieval Indian History", "दिल्ली सल्तनत, मुगल साम्राज्य, भक्ति आंदोलन, मराठा साम्राज्य"),
            Triple("आधुनिक भारत का इतिहास", "Modern Indian History", "1857 की क्रांति, गवर्नर जनरल, राष्ट्रीय आंदोलन व स्वतंत्रता संग्राम"),
            Triple("भारत एवं विश्व का भूगोल", "Geography of India & World", "नदियां, पर्वत, झीलें, जलवायु, वनस्पति, खनिज, सौरमंडल व महाद्वीप"),
            Triple("भारतीय अर्थव्यवस्था एवं बजट", "Indian Economy & Banking", "पंचवर्षीय योजनाएं, RBI, मौद्रिक नीति, GDP, GST, बजट व बैंक"),
            Triple("सामान्य भौतिक विज्ञान", "Physics", "गति के नियम, कार्य-ऊर्जा, प्रकाश, ध्वनि, ऊष्मा, विद्युत एवं प्रमुख मात्रक"),
            Triple("सामान्य रसायन विज्ञान", "Chemistry", "परमाणु संरचना, अम्ल-क्षार, धातु-अधातु, आवर्त सारणी एवं रासायनिक सूत्र"),
            Triple("सामान्य जीव विज्ञान", "Biology", "मानव शरीर तंत्र, कोशिका, रोग एवं उपचार, विटामिन, पोषण एवं पादप जगत"),
            Triple("पर्यावरण एवं पारिस्थितिकी", "Environment & Ecology", "जैव विविधता, राष्ट्रीय उद्यान, वन्यजीव अभयारण्य, प्रदूषण व सम्मेलन"),
            Triple("स्टेटिक GK, कला व संस्कृति", "Static GK, Art & Culture", "शास्त्रीय नृत्य, लोकगीत, प्रसिद्ध मंदिर, त्योहार, यूनेस्को धरोहर स्थल"),
            Triple("महत्वपूर्ण दिवस, पुरस्कार व पुस्तकें", "Important Days, Awards & Books", "राष्ट्रीय/अंतर्राष्ट्रीय दिवस, नोबेल, भारत रत्न, प्रसिद्ध पुस्तकें व लेखक"),
            Triple("राष्ट्रीय एवं अंतर्राष्ट्रीय समसामयिकी", "Current Affairs 2024-2026", "नवीनतम सरकारी योजनाएं, खेल, शिखर सम्मेलन, युद्धाभ्यास व नियुक्तियां")
        )

        return ExamSubjectCategory(
            key = "GK_GS",
            name = title,
            englishName = "General Awareness & General Studies",
            icon = Icons.Default.Public,
            gradientColors = listOf(Color(0xFF059669), Color(0xFF34D399)),
            chapters = chapters.mapIndexed { idx, (hName, eName, desc) ->
                buildChapterItem(
                    subExamId = subExamId,
                    subjectKey = "GK_GS",
                    subjectTitle = "सामान्य ज्ञान",
                    idx = idx,
                    hName = hName,
                    eName = eName,
                    desc = desc,
                    difficulty = if (idx in listOf(0, 4, 12)) "कठिन" else "मध्यम"
                )
            }
        )
    }

    private fun createHindiSubject(subExamId: String, title: String): ExamSubjectCategory {
        val chapters = listOf(
            Triple("हिंदी वर्णमाला एवं वर्तनी शुद्धि", "Varnamala & Spellings", "स्वर, व्यंजन, उच्चारण स्थान, अघोष-सघोष, अल्पप्राण-महाप्राण, शुद्ध वर्तनी"),
            Triple("संधि एवं संधि-विच्छेद", "Sandhi & Rules", "स्वर संधि (दीर्घ, गुण, वृद्धि, यण, अयादि), व्यंजन संधि एवं विसर्ग संधि"),
            Triple("समास एवं समास विग्रह", "Samas & Types", "अव्ययीभाव, तत्पुरुष, कर्मधारय, द्विगु, द्वंद्व एवं बहुव्रीहि समास"),
            Triple("विलोम शब्द", "Antonyms (Vilom)", "विपरीतार्थक शब्द, उपसर्ग जोड़कर बने विलोम शब्द"),
            Triple("पर्यायवाची एवं समानार्थी शब्द", "Synonyms (Paryayvachi)", "महत्वपूर्ण प्राकृतिक, धार्मिक व दैनिक उपयोगी पर्यायवाची शब्द"),
            Triple("अनेक शब्दों के लिए एक शब्द", "One Word Substitution", "वाक्यांश के लिए एकल शब्द व उनके सूक्ष्म अर्थ"),
            Triple("मुहावरे एवं लोकोक्तियां", "Idioms & Proverbs", "प्रचलित मुहावरे, कहावतें एवं उनका सही वाक्य प्रयोग"),
            Triple("तत्सम एवं तद्भव शब्द", "Tatsam & Tadbhav", "संस्कृत के मूल शब्द एवं उनसे परिवर्तित हिंदी रूप"),
            Triple("उपसर्ग एवं प्रत्यय", "Prefix & Suffix", "संस्कृत, हिंदी एवं विदेशी उपसर्ग, कृदंत व तद्धित प्रत्यय"),
            Triple("लिंग, वचन, कारक एवं काल", "Gender, Number, Case & Tense", "कारक के 8 भेद, विभक्ति चिह्न, एकवचन-बहुवचन परिवर्तन"),
            Triple("रस, छंद एवं अलंकार", "Ras, Chhand & Alankar", "रस के अंग, स्थायी भाव, दोहा, चौपाई, अनुप्रास, यमक, श्लेष, उपमा, रूपक"),
            Triple("वाक्य शुद्धि एवं पदबंध", "Sentence Correction", "व्याकरणिक अशुद्धियां, लिंग-वचन सम्बन्धी अशुद्धि, पदक्रम"),
            Triple("प्रमुख कवि, लेखक एवं उनकी रचनाएं", "Hindi Literature & Awards", "आदिकाल, भक्तिकाल, रीतिकाल, आधुनिक काल, ज्ञानपीठ व साहित्य अकादमी")
        )

        return ExamSubjectCategory(
            key = "HINDI",
            name = title,
            englishName = "General Hindi (सामान्य हिंदी)",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            gradientColors = listOf(Color(0xFFEA580C), Color(0xFFFDBA74)),
            chapters = chapters.mapIndexed { idx, (hName, eName, desc) ->
                buildChapterItem(
                    subExamId = subExamId,
                    subjectKey = "HINDI",
                    subjectTitle = "सामान्य हिंदी",
                    idx = idx,
                    hName = hName,
                    eName = eName,
                    desc = desc,
                    difficulty = if (idx in listOf(1, 2, 10)) "कठिन" else "मध्यम"
                )
            }
        )
    }

    private fun createEnglishSubject(subExamId: String, title: String): ExamSubjectCategory {
        val chapters = listOf(
            Triple("Spotting the Errors", "Error Detection", "Subject-verb agreement, tenses, prepositions, articles & pronouns"),
            Triple("Synonyms & Antonyms", "Vocabulary Mastery", "High-frequency exam vocabulary with contextual meanings"),
            Triple("Idioms and Phrases", "Idiomatic Expressions", "Common proverbs, phrasal verbs and situational usage"),
            Triple("One Word Substitution", "Lexical Precision", "Scientific terms, professions, fears, manias & governments"),
            Triple("Fill in the Blanks & Prepositions", "Sentence Completion", "Grammar rules, fixed prepositions & conjunctions"),
            Triple("Sentence Improvement", "Phrase Replacement", "Modifying underlined phrases for grammatical correctness"),
            Triple("Active and Passive Voice", "Voice Transformation", "Rules of conversion across all tenses & interrogatives"),
            Triple("Direct and Indirect Speech", "Narration", "Reported speech, pronoun changes, reporting verbs & modals"),
            Triple("Spelling Check & Correction", "Orthography", "Commonly misspelled words and double-letter rules"),
            Triple("Cloze Test & Reading Comprehension", "Passage Skills", "Theme identification, inference & fill-in paragraph blanks")
        )

        return ExamSubjectCategory(
            key = "ENGLISH",
            name = title,
            englishName = "General English Language",
            icon = Icons.Default.Language,
            gradientColors = listOf(Color(0xFF4F46E5), Color(0xFF818CF8)),
            chapters = chapters.mapIndexed { idx, (hName, eName, desc) ->
                buildChapterItem(
                    subExamId = subExamId,
                    subjectKey = "ENGLISH",
                    subjectTitle = "General English",
                    idx = idx,
                    hName = hName,
                    eName = eName,
                    desc = desc,
                    difficulty = if (idx in listOf(0, 6, 7)) "कठिन" else "मध्यम"
                )
            }
        )
    }

    private fun createComputerSubject(subExamId: String, title: String): ExamSubjectCategory {
        val chapters = listOf(
            Triple("MS Word (माइक्रोसॉफ्ट वर्ड)", "MS Word & Document Processing", "फॉन्ट, पैराग्राफ फॉर्मेटिंग, हेडर-फूटर, मेल मर्ज, टेबल्स व शॉर्टकट्स"),
            Triple("MS Excel (स्प्रेडशीट एवं सूत्र)", "MS Excel & Formulas", "वर्कशीट, सेल्स, SUM, AVERAGE, VLOOKUP, IF सूत्र, चार्ट्स व शॉर्टकट कीज"),
            Triple("MS PowerPoint (प्रस्तुति टूल्स)", "MS PowerPoint & Slides", "स्लाइड मास्टर, एनिमेशन, ट्रांजिशन, स्लाइड शो शॉर्टकट्स"),
            Triple("इंटरनेट, ब्राउज़र एवं URL", "Internet, Browsers & URLs", "WWW, HTTP/HTTPS, सर्च इंजन, DNS, IP एड्रेस, कुकीज व वेब प्रोटोकॉल्स"),
            Triple("ई-मेल (Email Protocols & Usage)", "Email & Protocols", "SMTP, POP3, IMAP, To, CC, BCC, स्पैम एवं अटैचमेंट्स"),
            Triple("कंप्यूटर हार्डवेयर एवं इनपुट/आउटपुट", "Hardware & I/O Devices", "CPU, ALU, कंट्रोल यूनिट, कीबोर्ड, स्कैनर, प्रिंटर के प्रकार व पोर्ट्स"),
            Triple("ऑपरेटिंग सिस्टम एवं मेमोरी", "OS & Memory Hierarchy", "RAM, ROM, कैश मेमोरी, सेकेंडरी स्टोरेज, विंडोज एवं लिनक्स फंडामेंटल्स"),
            Triple("साइबर सुरक्षा, वायरस व एंटीवायरस", "Cyber Security & Malware", "मैलवेयर, ट्रोजन, रैनसमवेयर, फ़िशिंग, फ़ायरवॉल व एन्क्रिप्शन"),
            Triple("कंप्यूटर नेटवर्क फंडामेंटल्स", "Computer Networks", "LAN, WAN, MAN, टोपोलॉजी (Star, Ring, Mesh), OSI मॉडल के लेयर्स"),
            Triple("शॉर्टकट कीज एवं फुल फॉर्म्स", "Shortcuts & Abbreviations", "Ctrl+A से Z तक, फंक्शन कीज (F1-F12), RAM, ROM, URL, PDF, BIOS फुल फॉर्म्स")
        )

        return ExamSubjectCategory(
            key = "COMPUTER",
            name = title,
            englishName = "Computer Knowledge & MS Office",
            icon = Icons.Default.Computer,
            gradientColors = listOf(Color(0xFF0284C7), Color(0xFF38BDF8)),
            chapters = chapters.mapIndexed { idx, (hName, eName, desc) ->
                buildChapterItem(
                    subExamId = subExamId,
                    subjectKey = "COMPUTER",
                    subjectTitle = "कंप्यूटर ज्ञान",
                    idx = idx,
                    hName = hName,
                    eName = eName,
                    desc = desc,
                    difficulty = if (idx in listOf(0, 1, 8)) "कठिन" else "मध्यम"
                )
            }
        )
    }

    private fun createScienceSubject(subExamId: String, title: String): ExamSubjectCategory {
        val chapters = listOf(
            Triple("मात्रक, मापन एवं गति के नियम", "Units, Measurement & Motion", "SI मात्रक, दूरी, विस्थापन, वेग, त्वरण, न्यूटन के गति नियम व संवेग संरक्षण"),
            Triple("कार्य, ऊर्जा और शक्ति", "Work, Energy & Power", "गतिज ऊर्जा, स्थितिज ऊर्जा, ऊर्जा संरक्षण का नियम व शक्ति के व्यावहारिक मात्रक"),
            Triple("गुरुत्वाकर्षण एवं दाब", "Gravitation & Pressure", "सार्वत्रिक गुरुत्वाकर्षण नियम, 'g' का मान, पलायन वेग, पास्कल का नियम"),
            Triple("प्रकाश, परावर्तन एवं अपवर्तन", "Light, Reflection & Optics", "समतल, अवतल व उत्तल दर्पण, लेंस सूत्र, अपवर्तनांक, पूर्ण आंतरिक परावर्तन"),
            Triple("विद्युत, धारा एवं चुंबकत्व", "Electricity & Magnetism", "ओम का नियम, प्रतिरोध, श्रेणी व समांतर क्रम, विद्युत शक्ति, फ्लेमिंग का नियम"),
            Triple("पदार्थ की अवस्थाएं एवं परमाणु संरचना", "States of Matter & Atomic Structure", "ठोस, द्रव, गैस, प्लाज्मा, इलेक्ट्रॉन, प्रोटॉन, न्यूट्रॉन, समस्थानिक"),
            Triple("अम्ल, क्षार, लवण एवं pH मान", "Acids, Bases, Salts & pH", "pH स्केल, लिटमस पेपर, उदासीनीकरण, बेकिंग सोडा, धावन सोडा, प्लास्टर ऑफ पेरिस"),
            Triple("धातु, अधातु एवं आवर्त सारणी", "Metals, Non-metals & Periodic Table", "आधुनिक आवर्त सारणी, मेंडेलीव की सारणी, संक्षारण, मिश्र धातुएं"),
            Triple("कोशिका एवं आनुवंशिकी", "Cell Biology & Genetics", "पादप व जंतु कोशिका, माइटोकॉन्ड्रिया, DNA, RNA, मेंडल के नियम"),
            Triple("मानव पाचन, श्वसन एवं परिसंचरण तंत्र", "Human Body Systems", "पाचक एंजाइम, हृदय संरचना, रक्त समूह (ABO, Rh), फेफड़े व धमनी-शिरा"),
            Triple("विटामिन, पोषण एवं प्रमुख मानव रोग", "Vitamins, Nutrition & Diseases", "विटामिन की कमी से रोग, जीवाणु, विषाणु व प्रोटोजोआ जनित रोग एवं टीके"),
            Triple("पादप आकारिकी एवं प्रकाश संश्लेषण", "Plant Physiology & Photosynthesis", "जड़, तना, पत्ती, जाइलम-फ्लोएम, प्रकाश संश्लेषण प्रक्रिया व पादप हार्मोन")
        )

        return ExamSubjectCategory(
            key = "SCIENCE",
            name = title,
            englishName = "General Science (Physics, Chem, Bio)",
            icon = Icons.Default.Science,
            gradientColors = listOf(Color(0xFF0D9488), Color(0xFF2DD4BF)),
            chapters = chapters.mapIndexed { idx, (hName, eName, desc) ->
                buildChapterItem(
                    subExamId = subExamId,
                    subjectKey = "SCIENCE",
                    subjectTitle = "सामान्य विज्ञान",
                    idx = idx,
                    hName = hName,
                    eName = eName,
                    desc = desc,
                    difficulty = if (idx in listOf(3, 4, 7)) "कठिन" else "मध्यम"
                )
            }
        )
    }

    private fun createRajasthanGkSubject(subExamId: String, title: String): ExamSubjectCategory {
        val chapters = listOf(
            Triple("राजस्थान का इतिहास एवं प्रमुख राजवंश", "Rajasthan History & Dynasties", "गुहिल-सिसोदिया वंश (मेवाड़), राठौड़ वंश (मारवाड़), कछवाहा (आमेर) व चौहान वंश"),
            Triple("1857 की क्रांति, प्रजामंडल एवं एकीकरण", "1857 Revolt & Integration", "नसीराबाद, नीमच, एरिनपुरा क्रांति, प्रमुख प्रजामंडल व राजस्थान एकीकरण के 7 चरण"),
            Triple("राजस्थान के प्रमुख दुर्ग, महल एवं हवेलियां", "Forts, Palaces & Monuments", "चित्तौड़गढ़, कुंभलगढ़, मेहरानगढ़, रणथंभौर, जैसलमेर दुर्ग व शेखावाटी की हवेलियां"),
            Triple("लोक देवता, लोक देवियां एवं संत संप्रदाय", "Folk Deities & Saints", "पंचपीर (रामदेवजी, पाबूजी, गोगाजी, हड़बूजी, मेहाजी), दादू दयाल, जांभोजी व जसनाथी"),
            Triple("राजस्थान के मेले, त्योहार एवं लोक नृत्य", "Fairs, Festivals & Folk Dances", "पुष्कर मेला, रामदेवरा, तेजाजी मेला, घूमर, गैर, कालबेलिया, तेरहताली व चरी नृत्य"),
            Triple("राजस्थान की चित्रकला, हस्तकला एवं वेशभूषा", "Art, Paintings & Handicrafts", "मेवाड़, मारवाड़, किशनगढ़ (बणी-ठणी), थेवा कला, ब्लू पॉटरी, उस्ता कला व बंधेज"),
            Triple("राजस्थान की भौगोलिक स्थिति एवं भौतिक प्रदेश", "Physiography & Geography", "मरुस्थलीय प्रदेश, अरावली पर्वतमाला, पूर्वी मैदान एवं हाड़ौती का पठार"),
            Triple("राजस्थान की नदियां, झीलें एवं सिंचाई परियोजनाएं", "Rivers, Lakes & Irrigation", "चंबल, बनास, लूनी, माही नदी, सांभर, जयसमंद, इंदिरा गांधी नहर परियोजना"),
            Triple("राजस्थान की जलवायु, मिट्टी एवं वन संपदा", "Climate, Soils & Forest Wealth", "कोपेन का जलवायु वर्गीकरण, लाल-पीली मिट्टी, खेजड़ी, अभयारण्य व राष्ट्रीय उद्यान"),
            Triple("राजस्थान के खनिज, ऊर्जा एवं उद्योग", "Minerals, Energy & Industries", "सीसा-जस्ता, संगमरमर, तांबा, सौर ऊर्जा नीति, पवन ऊर्जा व रीको (RIICO)"),
            Triple("राजस्थान की प्रशासनिक व्यवस्था एवं करंट अफेयर्स", "Polity & Current Affairs", "राज्यपाल, मुख्यमंत्री, विधानसभा, मुख्य सचिव, लोकायुक्त एवं नवीनतम योजनाएं")
        )

        return ExamSubjectCategory(
            key = "RAJ_GK",
            name = title,
            englishName = "Rajasthan General Knowledge",
            icon = Icons.Default.Castle,
            gradientColors = listOf(Color(0xFFD97706), Color(0xFFFBBF24)),
            chapters = chapters.mapIndexed { idx, (hName, eName, desc) ->
                buildChapterItem(
                    subExamId = subExamId,
                    subjectKey = "RAJ_GK",
                    subjectTitle = "राजस्थान सामान्य ज्ञान",
                    idx = idx,
                    hName = hName,
                    eName = eName,
                    desc = desc,
                    difficulty = if (idx in listOf(0, 1, 6)) "कठिन" else "मध्यम"
                )
            }
        )
    }

    private fun createLawSubject(subExamId: String, title: String): ExamSubjectCategory {
        val chapters = listOf(
            Triple("भारतीय दंड संहिता (IPC 1860) के महत्वपूर्ण प्रावधान", "IPC Provisions", "अपराध की परिभाषा, हत्या, हमला, चोरी, डकैती, बलात्कार व मानहानि की धाराएं"),
            Triple("दंड प्रक्रिया संहिता (CrPC 1973) एवं गिरफ्तारी नियम", "CrPC & Police Arrest Rules", "FIR दर्ज करने की प्रक्रिया, जमानती/गैर-जमानती अपराध, तलाशी व जब्ती"),
            Triple("मोटर वाहन अधिनियम एवं यातायात नियम", "Motor Vehicles Act", "ड्राइविंग लाइसेंस, चालान, नशे में वाहन चलाना, हेलमेट/सीट बेल्ट व दंड प्रावधान"),
            Triple("महिला एवं बच्चों के संरक्षण सम्बन्धी कानून", "Women & Child Protection Laws", "घरेलू हिंसा अधिनियम 2005, POCSO एक्ट 2012 व दहेज प्रतिषेध अधिनियम"),
            Triple("SC/ST अत्याचार निवारण अधिनियम 1989", "SC/ST Prevention of Atrocities Act", "जातिसूचक अपमान, विशेष अदालतें, त्वरित जांच व मुआवजे के नियम"),
            Triple("साइबर अपराध कानून एवं IT Act 2000", "Cyber Laws & IT Act", "हैकिंग, डेटा चोरी, अश्लील सामग्री प्रसार, पहचान की चोरी व धारा 66"),
            Triple("सूचना का अधिकार (RTI 2005) एवं भ्रष्टाचार निवारण", "RTI & Prevention of Corruption Act", "आवेदन प्रक्रिया, 30 दिन की समय-सीमा, केंद्रीय सूचना आयोग व लोकायुक्त"),
            Triple("मानवाधिकार संरक्षण एवं जनहित याचिका (PIL)", "Human Rights & PIL", "NHRC की शक्तियां, अनुच्छेद 32/226 के तहत रिट, पुलिस अभिरक्षा में अधिकार")
        )

        return ExamSubjectCategory(
            key = "LAW_CONST",
            name = title,
            englishName = "Basic Law, Police System & Constitution",
            icon = Icons.Default.Gavel,
            gradientColors = listOf(Color(0xFF991B1B), Color(0xFFF87171)),
            chapters = chapters.mapIndexed { idx, (hName, eName, desc) ->
                buildChapterItem(
                    subExamId = subExamId,
                    subjectKey = "LAW_CONST",
                    subjectTitle = "मूलविधि एवं संविधान",
                    idx = idx,
                    hName = hName,
                    eName = eName,
                    desc = desc,
                    difficulty = if (idx in listOf(0, 1, 3)) "कठिन" else "मध्यम"
                )
            }
        )
    }

    private fun createLekhpalRuralSubject(subExamId: String, title: String): ExamSubjectCategory {
        val chapters = listOf(
            Triple("ग्राम विकास का भारतीय संदर्भ एवं ग्राम्य समाज", "Rural Society in India", "ग्रामीण सामाजिक संरचना, जाति व्यवस्था, संयुक्त परिवार व ग्रामीण जीवन शैली"),
            Triple("ग्राम विकास कार्यक्रम एवं प्रमुख सरकारी योजनाएं", "Rural Development Schemes", "मनरेगा (MGNREGA), पीएम आवास योजना, किसान क्रेडिट कार्ड (KCC), स्वामित्व योजना"),
            Triple("भूमि सुधार, चकबंदी एवं राजस्व परिषद", "Land Reforms & Chakbandi", "जमींदारी उन्मूलन, चकबंदी प्रक्रिया, राजस्व न्यायालय व लेखपाल के कार्य व कर्तव्य"),
            Triple("भू-अभिलेख, खसरा, खतौनी एवं नक्शा प्रणाली", "Land Records: Khasra & Khatauni", "खसरा, खतौनी, सजरा (गांव का नक्शा), दाखिल-खारिज व पैमाइश के पैमाने (बीघा, बिस्वा, एकड़)"),
            Triple("त्रिस्तरीय पंचायती राज व्यवस्था एवं 73वां संशोधन", "Panchayati Raj System (73rd Amd)", "ग्राम पंचायत, क्षेत्र पंचायत, जिला पंचायत, ग्राम सभा की बैठकें व 29 विषय"),
            Triple("ग्रामीण कृषि, सिंचाई एवं पशुपालन", "Rural Agriculture & Animal Husbandry", "फसलों के प्रकार (रबी, खरीफ, जायद), नलकूप/नहर सिंचाई, पशुधन गणना व दुग्ध उत्पादन"),
            Triple("ग्रामीण स्वास्थ्य, स्वच्छता एवं पेयजल योजनाएं", "Rural Health & Sanitation", "जल जीवन मिशन, स्वच्छ भारत मिशन (ग्रामीण), आशा बहुएं व प्राथमिक स्वास्थ्य केंद्र")
        )

        return ExamSubjectCategory(
            key = "LEKHPAL_RURAL",
            name = title,
            englishName = "Rural Development & Society (ग्राम्य विकास)",
            icon = Icons.Default.Grass,
            gradientColors = listOf(Color(0xFF15803D), Color(0xFF86EFAC)),
            chapters = chapters.mapIndexed { idx, (hName, eName, desc) ->
                buildChapterItem(
                    subExamId = subExamId,
                    subjectKey = "LEKHPAL_RURAL",
                    subjectTitle = "ग्राम्य समाज एवं विकास",
                    idx = idx,
                    hName = hName,
                    eName = eName,
                    desc = desc,
                    difficulty = if (idx in listOf(2, 3, 4)) "कठिन" else "मध्यम"
                )
            }
        )
    }

    // =========================================================================
    // CHAPTER QUESTIONS GENERATOR WITH AUTHENTIC HINDI EXPLANATIONS
    // =========================================================================

    private fun generateQuestionsForChapter(chapter: ChapterItem): List<QuizQuestion> {
        return ExamChapterQuestionBank.getQuestionsForTopic(chapter.title, chapter.subjectKey, chapter.chapterNumber)
    }
}
