package com.example.service

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages administrator-defined strict custom rules and instructions that are
 * dynamically injected into every Google Gemini AI prompt for test parsing and question extraction.
 */
object AiPromptRulesManager {
    private const val PREFS_NAME = "exam_mitra_ai_rules_prefs"
    private const val KEY_FULL_TEST_RULES = "ai_strict_full_test_rules"
    private const val KEY_CHAPTER_WISE_RULES = "ai_strict_chapter_wise_rules"
    private const val KEY_CUSTOM_RULES_LEGACY = "ai_strict_custom_rules"

    val DEFAULT_FULL_TEST_RULES = """
1. [केवल शुद्ध हिंदी भाषा व रीजनिंग/गणित डेटा निष्कर्षण (Pure Hindi & Reasoning Data)]:
   - सामान्य प्रश्नों में केवल हिंदी भाग निकालें। अंग्रेजी अनुवाद छोड़ें।
   - 🚨 रीजनिंग (Reasoning) व गणित (Maths) के लिए सख्त नियम:
     * कोडिंग-डिकोडिंग (Coding e.g. 'ROSE' = '68'), अक्षर श्रृंखला (Letter Series e.g. 'AZ, BY, CX, ?'), संख्या श्रृंखला, गणितीय समीकरण (e.g. '24 + 12 ÷ 4 - 8 × 2 = 36'), रक्त संबंध कूट (e.g. 'P + Q - R'), और बैठक व्यवस्था के सभी अक्षरों, प्रतीकों, व्यंजकों को हूबहू "question" फ़ील्ड में पूरा लिखें। इन्हें अंग्रेजी अनुवाद समझकर कभी न छोड़ें!
     * न्याय निगमन / कथन और निष्कर्ष (Syllogism): सभी कथनों (कथन: I, II...) और सभी निष्कर्षों (निष्कर्ष: I, II...) को अलग-अलग पंक्तियों में पूरा लिखें।
     * यदि प्रश्न में आरेख/चित्र (Diagram) है, तो भी प्रश्न का पूरा लिखित निर्देश (उदा. 'दी गई आकृति का सही दर्पण प्रतिबिंब चुनें:') "question" में पूरा लिखें।
2. [विकल्पों से केवल हिंदी/संख्या/अक्षर भाग लें]:
   - सभी विकल्पों (A, B, C, D) में से स्लैश (/) के बाद वाला हिंदी भाग लें।
   - यदि विकल्प संख्याएँ (e.g. 12, 45, 80), निष्कर्ष (e.g. 'केवल निष्कर्ष I अनुसरण करता है'), या अक्षर हैं तो उन्हें 100% पूरा व सटीक रखें।
   - विकल्प में लेबल (A, B, C, D) न रखें।
3. [100% सटीक उत्तर मिलान व समाधान (Strict Answer Key & AI Solving - 0% Error)]:
   - PDF/कागज़ में प्रश्न के नीचे या उत्तर तालिका में लिखे 'Correct Answer / Official Answer: X' के आधार पर 100% सटीक सही उत्तर चुनें।
   - 0-आधारित इंडेक्स मैपिंग: A/1 -> "correctIndex": 0 ("correctOption": "A"), B/2 -> "correctIndex": 1 ("correctOption": "B"), C/3 -> "correctIndex": 2 ("correctOption": "C"), D/4 -> "correctIndex": 3 ("correctOption": "D").
   - "correctAnswer" फ़ील्ड में सही विकल्प का पूरा टेक्स्ट अक्षरशः हूबहू दें।
   - यदि उत्तरमाला नहीं है, तो AI द्वारा प्रश्न को 100% प्रामाणिक रूप से हल करें।
   - अंतिम प्रश्न (Last Question) तक उत्तर की सत्यता की विशेष पुष्टि करें।
4. [विज्ञापन व वॉटरमार्क पूरी तरह हटाएं (Strip Ads & Watermarks)]:
   - 'SSC CGL LIVE CLASSES', 'testbook', 'Enroll Now', 'link.testbook.com', 'SuperCoaching' आदि जैसे किसी भी विज्ञापन, यूआरएल या वॉटरमार्क को पूरी तरह छोड़ दें।
5. [टेक्स्ट फॉर्मेटिंग: केवल बोल्ड (**शब्द**) करें, अंडरलाइन (_शब्द_) केवल रेखांकित प्रश्नों में करें]:
   - 🌟 मुहावरे, लोकोक्तियां, पर्यायवाची व विलोम शब्द: मुख्य शब्द/मुहावरे को हमेशा केवल बोल्ड `**मुहावरा**` या `**शब्द**` में लिखें (अंडरलाइन न करें)।
     * उदा 1: 'निम्नलिखित प्रश्न में दिए गए मुहावरे के अर्थ को व्यक्त करने वाला विकल्प चुनें:\n**बासी बचे न कुत्ता खाय**'
     * उदा 2: '**‘टिप्पस भिड़ाना’** मुहावरे का सही अर्थ होगा-'
     * उदा 3: 'दिए गए मुहावरे का सही अर्थ वाला विकल्प है: **हुक्का भरना**'
     * उदा 4: 'दिए गए शब्द का विलोम शब्द चुनें: **‘उत्थान’**'
   - 🌟 केवल रेखांकित अंश (Underlined Part in PDF/Question) में अंडरलाइन `_अंश_` करें:
     * जहाँ PDF या प्रश्न में वाक्य के किसी पद के नीचे वास्तविक रेखा (Underline) है (उदा. 'रेखांकित शब्द/अंश को प्रतिस्थापित करें'): सिर्फ वहाँ `_शब्द_` लगाएं।
     * उदा 1: 'उँगली पकड़ कर _माथा_ पकड़ना।'
     * उदा 2: 'तुम _किस काम के_ हो?'
     * उदा 3: 'वह _प्रतिदिन_ विद्यालय जाता है।'
   - 🌟 रीजनिंग व गणित: कूट/श्रृंखला व समीकरणों को केवल बोल्ड `**ROSE = 68**` या `**AZ, BY, CX, ?**` लिखें।
6. [डायग्राम क्रॉप नियम]: डायग्राम में सिर्फ वही विकल्प साथ में लेने हैं जिनमें आकृति/चित्र हो। जिन विकल्पों में संख्या/नंबर/टेक्स्ट हो, उन्हें डायग्राम इमेज से बाहर रखें।
7. [संक्षिप्त हिंदी व्याख्या]: प्रत्येक प्रश्न के साथ 'उत्तर: (A/B/C/D) - [विकल्प]' सहित 1-2 पंक्तियों में सटीक व प्रामाणिक हिंदी व्याख्या (Explanation) अवश्य लिखें।
""".trimIndent()

    val DEFAULT_CHAPTER_WISE_RULES = """
1. [लेआउट पठन: 2-कॉलम या सिंगल कॉलम] पहले पेज लेआउट देखें। यदि 2-कॉलम (Rojgar Publication, Pinnacle, Kiran, Youth Competition) है, तो पहले बाएँ कॉलम के सभी प्रश्न ऊपर से नीचे निकालें, फिर दाएँ कॉलम के। यदि सिंगल कॉलम है, तो सीधा ऊपर से नीचे पढ़ें।
2. [PDF के सभी प्रश्न हूबहू 100% निकालें] अपलोड की गई PDF या पृष्ठ पर मौजूद प्रत्येक प्रश्न को बिना किसी चूक के अक्षरशः (Verbatim) हूबहू निकालें। किसी भी प्रश्न को न छोड़ें, न बदलें।
3. [रीजनिंग (Reasoning) व गणित डेटा पूर्णता]:
   - रीजनिंग के सभी कूट (Coding), अक्षर श्रृंखला (Letter Series e.g. 'AZ, BY, CX, ?'), संख्या श्रृंखला, गणितीय संक्रियाएं (e.g. '24 + 12 ÷ 4 = ?'), रक्त संबंध (A + B का अर्थ...), बैठक व्यवस्था, और कथन एवं निष्कर्ष (Statements & Conclusions) के सभी कथनों/निष्कर्षों को "question" फ़ील्ड में 100% पूरा व स्पष्ट लिखें।
   - आकृति वाले प्रश्नों में भी प्रश्न का पूरा लिखित निर्देश "question" में अवश्य शामिल करें।
4. [टेक्स्ट फॉर्मेटिंग: केवल बोल्ड (**शब्द**) करें, अंडरलाइन (_शब्द_) केवल रेखांकित प्रश्नों में करें]:
   - 🌟 मुहावरे, लोकोक्तियां, पर्यायवाची, विलोम व वर्तनी: मुख्य शब्द/मुहावरे को हमेशा केवल बोल्ड `**मुहावरा**` या `**शब्द**` में लिखें (अंडरलाइन न करें)।
     * उदा 1: 'निम्नलिखित प्रश्न में दिए गए मुहावरे के अर्थ को सर्वश्रेष्ठ रूप से व्यक्त करने वाला विकल्प चुनें:\n**बासी बचे न कुत्ता खाय**'
     * उदा 2: '**‘टिप्पस भिड़ाना’** मुहावरे का सही अर्थ होगा-'
     * उदा 3: 'दिए गए मुहावरे का सही अर्थ वाला विकल्प है: **हुक्का भरना**'
     * उदा 4: 'दिए गए शब्द का पर्यायवाची शब्द चुनें: **‘अमृत’**'
   - 🌟 केवल रेखांकित अंश (Underlined Part in PDF/Question) में अंडरलाइन `_अंश_` करें:
     * जहाँ PDF या प्रश्न में वाक्य के किसी पद के नीचे वास्तविक रेखा (Underline) है (उदा. 'रेखांकित शब्द/अंश को प्रतिस्थापित करें'): सिर्फ वहाँ `_शब्द_` लगाएं।
     * उदा 1: 'उँगली पकड़ कर _माथा_ पकड़ना।'
     * उदा 2: 'तुम _किस काम के_ हो?'
     * उदा 3: 'वह _प्रतिदिन_ विद्यालय जाता है।'
   - 🌟 रीजनिंग व गणित: कूट/श्रृंखला को बोल्ड `**AZ, BY, CX, ?**` या `**ROSE = 68**` में लिखें।
5. [100% सटीक उत्तरमाला व AI समाधान - 0% त्रुटि गारंटी (Strict Answer Key & Accurate AI Solving)]:
   - 🌟 उत्तर तालिका (Answer Key): पृष्ठ के नीचे, कॉलम के अंत में, या पास में छपी उत्तरमाला (उदा. 1-(c), 2-(a), 3-(b)... / 1.C, 2.B) से प्रत्येक प्रश्न के उत्तर का 100% सटीक मिलान करें।
   - 🌟 व्याख्या खंड (Explanation Header): यदि प्रश्न के नीचे व्याख्या में 'उत्तर: (B)' या 'हल: (b)' छपा है, तो वही विकल्प 100% सही उत्तर चुनें।
   - 🌟 AI सेल्फ-सॉल्विंग (जब उत्तरमाला न छपी हो):
     * हिंदी व्याकरण (मुहावरे, लोकोक्तियां, पर्यायवाची, विलोम शब्द, शुद्ध वर्तनी, संधि, समास, अनेक शब्दों के लिए एक शब्द): मानक प्रामाणिक हिंदी व्याकरण (NCERT) के अनुसार 100% शुद्ध उत्तर ही चुनें।
     * रीजनिंग व गणित: प्रश्नों (श्रृंखला, कोडिंग, समीकरण) को चरणबद्ध रूप से हल करके ही सही विकल्प का इंडेक्स सेट करें।
     * सामान्य ज्ञान (GK/GS): ऐतिहासिक व आधिकारिक तथ्यों से पूरी तरह सत्यापित सही विकल्प चुनें।
   - 🌟 विकल्प इंडेक्स (0-आधारित):
     * विकल्प A / (1) -> "correctIndex": 0, "correctOption": "A"
     * विकल्प B / (2) -> "correctIndex": 1, "correctOption": "B"
     * विकल्प C / (3) -> "correctIndex": 2, "correctOption": "C"
     * विकल्प D / (4) -> "correctIndex": 3, "correctOption": "D"
   - 🌟 "correctAnswer" फ़ील्ड में सही विकल्प का पूरा टेक्स्ट अक्षरशः हूबहू दें ताकि विकल्प और उत्तर में 100% सामंजस्य रहे।
6. [परीक्षा, वर्ष एवं शिफ्ट टैग] प्रश्न के नीचे या पास में लिखे परीक्षा नाम, वर्ष और शिफ्ट (उदा. 'SSC GD 2024 Shift-I', 'SSC GD 17 Feb. 2025, Shift-I') को 'examShiftInfo' में अवश्य लिखें।
7. [🚫 NO AI HALLUCINATION & EXACT STOP] अपनी तरफ से (AI Memory से) एक भी प्रश्न नहीं जोड़ना है! पेज जहाँ खत्म हो रहा है, वहीं JSON को समाप्त करें! आगे की गिनती खुद से न बनाएं!
8. [बहु-पंक्ति प्रश्न संयोजन] यदि प्रश्न में 2 पंक्तियाँ हैं (जैसे निर्देश + मुहावरा/शब्द या निर्देश + समीकरण), तो दोनों पंक्तियों को मिलाकर पूर्ण प्रश्न बनाएं ताकि कोई भाग न छूटे।
9. [डायग्राम क्रॉप नियम] डायग्राम में केवल वही विकल्प साथ में लें जिनमें आकृति हो। संख्या/टेक्स्ट वाले विकल्पों को आरेख बॉक्स के बाहर रखें।
10. [सटीक व्याख्या] प्रत्येक प्रश्न के साथ 'उत्तर: (A/B/C/D) - [सही विकल्प]' सहित 1-2 पंक्तियों की प्रामाणिक हिंदी व्याख्या अवश्य शामिल करें।
""".trimIndent()

    val DEFAULT_RULES = DEFAULT_FULL_TEST_RULES

    @Volatile
    private var cachedFullRules: String? = null

    @Volatile
    private var cachedChapterRules: String? = null

    /**
     * Returns the active rules based on testMode ("FULL_TEST" or "CHAPTER_WISE").
     */
    fun getRulesForMode(context: Context? = null, mode: String = "FULL_TEST"): String {
        return if (mode.equals("CHAPTER_WISE", ignoreCase = true)) {
            getChapterWiseRules(context)
        } else {
            getFullTestRules(context)
        }
    }

    /**
     * Returns rules for Full Mock Test mode.
     */
    fun getFullTestRules(context: Context? = null): String {
        cachedFullRules?.let { return it }
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_FULL_TEST_RULES, null) ?: prefs.getString(KEY_CUSTOM_RULES_LEGACY, null)
            if (!saved.isNullOrBlank()) {
                if (!saved.contains("रीजनिंग (Reasoning)") || !saved.contains("0% Error") || !saved.contains("correctAnswer")) {
                    saveFullTestRules(context, DEFAULT_FULL_TEST_RULES)
                    return DEFAULT_FULL_TEST_RULES
                }
                cachedFullRules = saved
                return saved
            }
        }
        cachedFullRules = DEFAULT_FULL_TEST_RULES
        return DEFAULT_FULL_TEST_RULES
    }

    /**
     * Returns rules for Chapter Wise mode.
     */
    fun getChapterWiseRules(context: Context? = null): String {
        cachedChapterRules?.let { return it }
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_CHAPTER_WISE_RULES, null)
            if (!saved.isNullOrBlank()) {
                if (!saved.contains("0% त्रुटि") || (!saved.contains("बायाँ कॉलम") && !saved.contains("बाएँ कॉलम")) || !saved.contains("HALLUCINATION") || !saved.contains("रेखांकित") || !saved.contains("correctAnswer")) {
                    // Automatically migrate legacy rule to the 100% verbatim PDF extraction and 0% error reasoning rule
                    saveChapterWiseRules(context, DEFAULT_CHAPTER_WISE_RULES)
                    return DEFAULT_CHAPTER_WISE_RULES
                }
                cachedChapterRules = saved
                return saved
            }
        }
        cachedChapterRules = DEFAULT_CHAPTER_WISE_RULES
        return DEFAULT_CHAPTER_WISE_RULES
    }

    /**
     * Legacy backward-compatible accessor
     */
    fun getCustomRules(context: Context? = null): String {
        return getFullTestRules(context)
    }

    /**
     * Saves rules for a specific mode ("FULL_TEST" or "CHAPTER_WISE").
     */
    fun saveRulesForMode(context: Context?, mode: String, rules: String) {
        if (mode.equals("CHAPTER_WISE", ignoreCase = true)) {
            saveChapterWiseRules(context, rules)
        } else {
            saveFullTestRules(context, rules)
        }
    }

    fun saveFullTestRules(context: Context?, rules: String) {
        val trimmed = rules.trim()
        val toSave = if (trimmed.isNotBlank()) trimmed else DEFAULT_FULL_TEST_RULES
        cachedFullRules = toSave
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_FULL_TEST_RULES, toSave).apply()
        }
    }

    fun saveChapterWiseRules(context: Context?, rules: String) {
        val trimmed = rules.trim()
        val toSave = if (trimmed.isNotBlank()) trimmed else DEFAULT_CHAPTER_WISE_RULES
        cachedChapterRules = toSave
        if (context != null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CHAPTER_WISE_RULES, toSave).apply()
        }
    }

    fun saveCustomRules(context: Context?, rules: String) {
        saveFullTestRules(context, rules)
    }

    /**
     * Resets rules for the specified mode to factory defaults.
     */
    fun resetToDefault(context: Context?, mode: String = "FULL_TEST"): String {
        return if (mode.equals("CHAPTER_WISE", ignoreCase = true)) {
            cachedChapterRules = DEFAULT_CHAPTER_WISE_RULES
            if (context != null) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_CHAPTER_WISE_RULES, DEFAULT_CHAPTER_WISE_RULES).apply()
            }
            DEFAULT_CHAPTER_WISE_RULES
        } else {
            cachedFullRules = DEFAULT_FULL_TEST_RULES
            if (context != null) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putString(KEY_FULL_TEST_RULES, DEFAULT_FULL_TEST_RULES).apply()
            }
            DEFAULT_FULL_TEST_RULES
        }
    }
}
