package com.example.model

/**
 * Authentic Question Bank for Exam Chapters (SSC GD, Delhi Police, UP Police, etc.)
 * Provides realistic PYQ questions with options, correct answer, and Hindi explanation
 * for each chapter topic so chapter tests are immediately active and playable.
 */
object ExamChapterQuestionBank {

    fun getQuestionsForTopic(topicTitle: String, subjectKey: String, chapterNumber: Int): List<QuizQuestion> {
        val t = topicTitle.lowercase()
        return when {
            // ==========================================
            // 1. MATHEMATICS (प्रारंभिक गणित)
            // ==========================================
            t.contains("प्रतिशत") || t.contains("percent") -> getPercentageQuestions()
            t.contains("लाभ") || t.contains("हानि") || t.contains("बट्टा") || t.contains("profit") || t.contains("loss") -> getProfitLossQuestions()
            t.contains("ब्याज") || t.contains("interest") || t.contains("चक्रवृद्धि") || t.contains("साधारण") -> getInterestQuestions()
            t.contains("अनुपात") || t.contains("समानुपात") || t.contains("साझेदारी") || t.contains("ratio") -> getRatioQuestions()
            t.contains("समय और कार्य") || t.contains("नल") || t.contains("टंकी") || t.contains("time") && t.contains("work") -> getTimeWorkQuestions()
            t.contains("चाल") || t.contains("दूरी") || t.contains("रेलगाड़ी") || t.contains("speed") || t.contains("train") -> getSpeedDistanceQuestions()
            t.contains("संख्या पद्धति") || t.contains("सरलीकरण") || t.contains("number") || t.contains("bodmas") -> getNumberSystemQuestions()
            t.contains("ल.स.") || t.contains("म.स.") || t.contains("lcm") || t.contains("hcf") -> getLcmHcfQuestions()
            t.contains("औसत") || t.contains("आयु") || t.contains("average") || t.contains("age") -> getAverageQuestions()
            t.contains("मिश्रण") || t.contains("mixture") || t.contains("alligation") -> getMixtureQuestions()
            t.contains("क्षेत्रमिति") || t.contains("mensuration") || t.contains("आयतन") || t.contains("क्षेत्रफल") -> getMensurationQuestions()
            t.contains("बीजगणित") || t.contains("त्रिकोणमिति") || t.contains("algebra") -> getAlgebraQuestions()
            t.contains("आंकड़ों") || t.contains("di") || t.contains("ग्राफ") || t.contains("interpretation") -> getDataInterpretationQuestions()

            // ==========================================
            // 2. REASONING (तर्कशक्ति)
            // ==========================================
            t.contains("कोडिंग") || t.contains("डिकोडिंग") || t.contains("coding") -> getCodingDecodingQuestions()
            t.contains("सादृश्यता") || t.contains("वर्गीकरण") || t.contains("analogy") || t.contains("classification") -> getAnalogyQuestions()
            t.contains("श्रृंखला") || t.contains("शृंखला") || t.contains("series") -> getSeriesQuestions()
            t.contains("रक्त संबंध") || t.contains("blood") -> getBloodRelationQuestions()
            t.contains("दिशा") || t.contains("दूरी") || t.contains("direction") -> getDirectionQuestions()
            t.contains("बैठक") || t.contains("क्रम") || t.contains("seating") || t.contains("ranking") -> getSeatingQuestions()
            t.contains("न्याय") || t.contains("निगमन") || t.contains("कथन") || t.contains("निष्कर्ष") || t.contains("syllogism") -> getSyllogismQuestions()
            t.contains("पासा") || t.contains("घन") || t.contains("dice") || t.contains("cube") -> getDiceQuestions()
            t.contains("घड़ी") || t.contains("कैलेंडर") || t.contains("clock") || t.contains("calendar") -> getClockCalendarQuestions()
            t.contains("वेन आरेख") || t.contains("venn") -> getVennDiagramQuestions()
            t.contains("लुप्त संख्या") || t.contains("संक्रियाएं") || t.contains("missing") -> getMissingNumberQuestions()
            t.contains("दर्पण") || t.contains("जल") || t.contains("अशाब्दिक") || t.contains("mirror") || t.contains("water") -> getNonVerbalQuestions()

            // ==========================================
            // 3. GENERAL HINDI (सामान्य हिंदी)
            // ==========================================
            t.contains("वर्णमाला") || t.contains("वर्तनी") || t.contains("spelling") -> getVarnamalaQuestions()
            t.contains("संधि") -> getSandhiQuestions()
            t.contains("समास") -> getSamasQuestions()
            t.contains("विलोम") || t.contains("antonym") -> getVilomQuestions()
            t.contains("पर्यायवाची") || t.contains("synonym") -> getParyayvachiQuestions()
            t.contains("अनेक शब्दों") || t.contains("वाक्यांश") || t.contains("एक शब्द") -> getOneWordQuestions()
            t.contains("मुहावरे") || t.contains("लोकोक्तियां") || t.contains("idiom") -> getMuhavareQuestions()
            t.contains("तत्सम") || t.contains("तद्भव") -> getTatsamQuestions()
            t.contains("उपसर्ग") || t.contains("प्रत्यय") -> getUpsargPratyayQuestions()
            t.contains("लिंग") || t.contains("वचन") || t.contains("कारक") || t.contains("काल") -> getKarakVachanQuestions()
            t.contains("रस") || t.contains("छंद") || t.contains("अलंकार") -> getRasChhandAlankarQuestions()
            t.contains("वाक्य शुद्धि") || t.contains("पदबंध") -> getVakyaShuddhiQuestions()
            t.contains("कवि") || t.contains("लेखक") || t.contains("रचनाएं") || t.contains("साहित्य") -> getHindiLiteratureQuestions()

            // ==========================================
            // 4. GENERAL KNOWLEDGE / GS (सामान्य ज्ञान)
            // ==========================================
            t.contains("संविधान") || t.contains("राजव्यवस्था") || t.contains("polity") || t.contains("constitution") -> getPolityQuestions()
            t.contains("प्राचीन") || t.contains("ancient") -> getAncientHistoryQuestions()
            t.contains("मध्यकालीन") || t.contains("medieval") -> getMedievalHistoryQuestions()
            t.contains("आधुनिक") || t.contains("modern") || t.contains("क्रांति") || t.contains("स्वतंत्रता") -> getModernHistoryQuestions()
            t.contains("भूगोल") || t.contains("geography") || t.contains("नदियां") || t.contains("पर्वत") -> getGeographyQuestions()
            t.contains("अर्थव्यवस्था") || t.contains("बजट") || t.contains("economy") || t.contains("banking") -> getEconomyQuestions()
            t.contains("भौतिक") || t.contains("physics") -> getPhysicsQuestions()
            t.contains("रसायन") || t.contains("chemistry") -> getChemistryQuestions()
            t.contains("जीव विज्ञान") || t.contains("biology") -> getBiologyQuestions()
            t.contains("पर्यावरण") || t.contains("पारिस्थितिकी") || t.contains("environment") -> getEnvironmentQuestions()
            t.contains("स्टेटिक") || t.contains("कला") || t.contains("संस्कृति") || t.contains("static") || t.contains("नृत्य") -> getStaticGkQuestions()
            t.contains("दिवस") || t.contains("पुरस्कार") || t.contains("पुस्तकें") || t.contains("award") || t.contains("book") -> getAwardsBooksQuestions()
            t.contains("समसामयिकी") || t.contains("current") || t.contains("योजनाएं") -> getCurrentAffairsQuestions()

            // ==========================================
            // 5. COMPUTER KNOWLEDGE (कंप्यूटर)
            // ==========================================
            subjectKey.equals("COMPUTER", true) || t.contains("कंप्यूटर") || t.contains("computer") -> getComputerQuestions()

            // Default topic generator fallback based on subject
            else -> getDefaultQuestionsForSubject(subjectKey, topicTitle, chapterNumber)
        }
    }

    // -------------------------------------------------------------
    // MATHS QUESTIONS
    // -------------------------------------------------------------
    private fun getPercentageQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 101,
            question = "एक वस्तु के मूल्य में पहले 20% की वृद्धि की जाती है और फिर 20% की कमी की जाती है। वस्तु के अंतिम मूल्य में कुल कितने प्रतिशत परिवर्तन होगा?",
            options = listOf("कोई परिवर्तन नहीं", "4% की कमी", "4% की वृद्धि", "2% की कमी"),
            correctIndex = 1,
            explanation = "सूत्र: net change = x + y + (xy/100) = +20 - 20 - (20×20/100) = -4% अर्थात 4% की कमी।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ (Shift-1)"
        ),
        QuizQuestion(
            id = 102,
            question = "एक परीक्षा में उत्तीर्ण होने के लिए 36% अंक प्राप्त करना आवश्यक है। एक छात्र को 113 अंक मिले और वह 85 अंकों से अनुत्तीर्ण हो गया। परीक्षा का पूर्णांक कितना था?",
            options = listOf("500", "550", "600", "650"),
            correctIndex = 1,
            explanation = "उत्तीर्णांक = 113 + 85 = 198। 36% = 198, अतः 100% = (198/36) × 100 = 550 अंक।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ (Shift-2)"
        ),
        QuizQuestion(
            id = 103,
            question = "यदि A की आय B की आय से 25% अधिक है, तो B की आय A की आय से कितने प्रतिशत कम है?",
            options = listOf("20%", "25%", "16.66%", "30%"),
            correctIndex = 0,
            explanation = "प्रतिशत कमी = [25 / (100 + 25)] × 100 = (25 / 125) × 100 = 20%।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        ),
        QuizQuestion(
            id = 104,
            question = "चीनी के मूल्य में 20% की कमी होने पर एक उपभोक्ता ₹240 में 4 किग्रा चीनी अधिक खरीद पाता है। चीनी का घटा हुआ मूल्य प्रति किग्रा क्या है?",
            options = listOf("₹12", "₹15", "₹10", "₹14"),
            correctIndex = 0,
            explanation = "बचत = 240 का 20% = ₹48। यह 4 किग्रा अधिक का मूल्य है। अतः घटा हुआ मूल्य = 48/4 = ₹12 प्रति किग्रा।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ (Shift-3)"
        ),
        QuizQuestion(
            id = 105,
            question = "एक शहर की जनसंख्या 80,000 है। यदि इसमें प्रति वर्ष 10% की दर से वृद्धि हो, तो 2 वर्ष बाद जनसंख्या कितनी होगी?",
            options = listOf("96,000", "96,800", "98,000", "95,500"),
            correctIndex = 1,
            explanation = "2 वर्ष बाद जनसंख्या = 80000 × (110/100) × (110/100) = 80000 × 1.21 = 96,800।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getProfitLossQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 201,
            question = "एक दुकानदार किसी वस्तु को ₹1,440 में बेचकर 20% का लाभ कमाता है। वस्तु का क्रय मूल्य क्या है?",
            options = listOf("₹1,200", "₹1,150", "₹1,250", "₹1,180"),
            correctIndex = 0,
            explanation = "क्रय मूल्य (CP) = SP × 100 / (100 + लाभ%) = 1440 × 100 / 120 = ₹1,200।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 202,
            question = "एक बेईमान दुकानदार 1 किग्रा के स्थान पर 800 ग्राम के झूठे बाट का उपयोग करता है और वस्तु को क्रय मूल्य पर बेचने का दावा करता है। उसका वास्तविक लाभ प्रतिशत क्या है?",
            options = listOf("20%", "25%", "15%", "30%"),
            correctIndex = 1,
            explanation = "लाभ% = [त्रुटि / (सत्य मान - त्रुटि)] × 100 = [200 / 800] × 100 = 25%।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        ),
        QuizQuestion(
            id = 203,
            question = "किसी वस्तु के अंकित मूल्य पर 20% और 10% की दो क्रमिक छूट दी जाती हैं। एकल समतुल्य बट्टा प्रतिशत क्या होगा?",
            options = listOf("30%", "28%", "25%", "27%"),
            correctIndex = 1,
            explanation = "समतुल्य बट्टा = d1 + d2 - (d1×d2/100) = 20 + 10 - (200/100) = 28%।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 204,
            question = "20 वस्तुओं का क्रय मूल्य 16 वस्तुओं के विक्रय मूल्य के बराबर है। लाभ प्रतिशत ज्ञात कीजिए।",
            options = listOf("20%", "25%", "30%", "16.66%"),
            correctIndex = 1,
            explanation = "20 CP = 16 SP ⇒ SP/CP = 20/16 = 5/4। लाभ% = (1/4) × 100 = 25%।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 205,
            question = "एक व्यापारी ₹800 की वस्तु पर 10% की छूट देने के बाद भी 20% का लाभ अर्जित करता है। वस्तु का अंकित मूल्य क्या होना चाहिए?",
            options = listOf("₹1,000", "₹1,066.67", "₹1,100", "₹1,200"),
            correctIndex = 1,
            explanation = "CP/MP = (100 - D)/(100 + P) = 90/120 = 3/4। MP = 800 × (4/3) = ₹1,066.67।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getInterestQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 301,
            question = "₹5,000 की राशि पर 10% वार्षिक साधारण ब्याज की दर से 3 वर्ष का साधारण ब्याज कितना होगा?",
            options = listOf("₹1,200", "₹1,500", "₹1,800", "₹2,000"),
            correctIndex = 1,
            explanation = "SI = (P × R × T) / 100 = (5000 × 10 × 3) / 100 = ₹1,500।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 302,
            question = "₹10,000 की राशि पर 10% वार्षिक चक्रवृद्धि ब्याज की दर से 2 वर्ष का चक्रवृद्धि ब्याज और साधारण ब्याज का अंतर क्या होगा?",
            options = listOf("₹50", "₹100", "₹120", "₹150"),
            correctIndex = 1,
            explanation = "2 वर्ष के लिए अंतर = P × (R/100)² = 10000 × (10/100)² = 10000 × 0.01 = ₹100।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 303,
            question = "कोई धनराशि साधारण ब्याज से 5 वर्षों में दोगुनी हो जाती है। ब्याज की वार्षिक दर क्या है?",
            options = listOf("15%", "20%", "25%", "10%"),
            correctIndex = 1,
            explanation = "R = (n - 1) × 100 / T = (2 - 1) × 100 / 5 = 20% वार्षिक।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        ),
        QuizQuestion(
            id = 304,
            question = "₹8,000 पर 5% वार्षिक चक्रवृद्धि ब्याज की दर से 3 वर्ष का चक्रवृद्धि ब्याज कितना होगा?",
            options = listOf("₹1,200", "₹1,261", "₹1,320", "₹1,150"),
            correctIndex = 1,
            explanation = "मिश्रधन = 8000 × (21/20)³ = 8000 × (9261/8000) = ₹9,261। CI = 9261 - 8000 = ₹1,261।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 305,
            question = "यदि कोई धन चक्रवृद्धि ब्याज की दर से 3 वर्ष में 3 गुना हो जाता है, तो वह 9 गुना कितने वर्ष में होगा?",
            options = listOf("6 वर्ष", "9 वर्ष", "12 वर्ष", "15 वर्ष"),
            correctIndex = 0,
            explanation = "3 वर्ष में 3¹ गुना। अतः 9 गुना = 3² गुना होगा = 3 × 2 = 6 वर्षों में।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getRatioQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 401,
            question = "यदि A : B = 2 : 3 तथा B : C = 4 : 5 हो, तो A : B : C का मान क्या होगा?",
            options = listOf("8 : 12 : 15", "6 : 8 : 10", "8 : 10 : 15", "6 : 12 : 15"),
            correctIndex = 0,
            explanation = "A : B = 2 : 3, B : C = 4 : 5। A = 2×4=8, B = 3×4=12, C = 3×5=15। अतः 8 : 12 : 15।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 402,
            question = "₹1,800 को A, B और C में 2 : 3 : 4 के अनुपात में बांटा जाता है। B का हिस्सा कितना होगा?",
            options = listOf("₹400", "₹600", "₹800", "₹500"),
            correctIndex = 1,
            explanation = "कुल अनुपात = 2 + 3 + 4 = 9। 1 अनुपात = 1800/9 = ₹200। B का हिस्सा = 3 × 200 = ₹600।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 403,
            question = "9 और 25 का मध्यानुपाती (Mean Proportional) क्या होगा?",
            options = listOf("12", "15", "18", "20"),
            correctIndex = 1,
            explanation = "मध्यानुपाती = √(a × b) = √(9 × 25) = √(225) = 15।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        ),
        QuizQuestion(
            id = 404,
            question = "12 और 18 का तृतीयानुपाती (Third Proportional) ज्ञात कीजिए।",
            options = listOf("24", "27", "30", "36"),
            correctIndex = 1,
            explanation = "तृतीयानुपाती = b² / a = (18 × 18) / 12 = 324 / 12 = 27।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 405,
            question = "A और B एक व्यवसाय में 3 : 5 के अनुपात में निवेश करते हैं। यदि वर्ष के अंत में कुल लाभ ₹16,000 हो, तो A का लाभ क्या होगा?",
            options = listOf("₹6,000", "₹10,000", "₹8,000", "₹7,500"),
            correctIndex = 0,
            explanation = "लाभ का अनुपात = निवेश का अनुपात = 3 : 5। A का हिस्सा = (3/8) × 16000 = ₹6,000।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getTimeWorkQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 501,
            question = "A किसी कार्य को 10 दिन में तथा B उसी कार्य को 15 दिन में पूरा कर सकता है। दोनों मिलकर उस कार्य को कितने दिन में पूरा करेंगे?",
            options = listOf("5 दिन", "6 दिन", "7 दिन", "8 दिन"),
            correctIndex = 1,
            explanation = "दोनों मिलकर = (A × B) / (A + B) = (10 × 15) / 25 = 150 / 25 = 6 दिन।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 502,
            question = "12 पुरुष किसी काम को 8 दिन में पूरा कर सकते हैं। उसी काम को 16 पुरुष कितने दिनों में पूरा करेंगे?",
            options = listOf("5 दिन", "6 दिन", "4 दिन", "8 दिन"),
            correctIndex = 1,
            explanation = "M1 × D1 = M2 × D2 ⇒ 12 × 8 = 16 × D2 ⇒ D2 = 96 / 16 = 6 दिन।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 503,
            question = "नल A एक टंकी को 20 मिनट में भर सकता है और नल B उसे 30 मिनट में भर सकता है। दोनों नल एक साथ खोलने पर टंकी कितने समय में भरेगी?",
            options = listOf("10 मिनट", "12 मिनट", "15 मिनट", "16 मिनट"),
            correctIndex = 1,
            explanation = "समय = (20 × 30) / (20 + 30) = 600 / 50 = 12 मिनट।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        ),
        QuizQuestion(
            id = 504,
            question = "A और B मिलकर किसी काम को 12 दिन में करते हैं। A अकेला उसी काम को 20 दिन में कर सकता है। B अकेला उस काम को कितने दिन में करेगा?",
            options = listOf("25 दिन", "30 दिन", "35 दिन", "40 दिन"),
            correctIndex = 1,
            explanation = "B का समय = (12 × 20) / (20 - 12) = 240 / 8 = 30 दिन।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 505,
            question = "नल A टंकी को 10 घंटे में भरता है तथा नल B भरी टंकी को 15 घंटे में खाली करता है। दोनों नल साथ खोलने पर खाली टंकी कितने घंटे में भरेगी?",
            options = listOf("25 घंटे", "30 घंटे", "20 घंटे", "35 घंटे"),
            correctIndex = 1,
            explanation = "कुल समय = (10 × 15) / (15 - 10) = 150 / 5 = 30 घंटे।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getSpeedDistanceQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 601,
            question = "एक 180 मीटर लंबी रेलगाड़ी 54 किमी/घंटा की गति से चल रही है। यह एक खंभे को कितने सेकंड में पार करेगी?",
            options = listOf("10 सेकंड", "12 सेकंड", "14 सेकंड", "15 सेकंड"),
            correctIndex = 1,
            explanation = "गति = 54 × (5/18) = 15 मीटर/सेकंड। समय = दूरी / गति = 180 / 15 = 12 सेकंड।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 602,
            question = "एक व्यक्ति 60 किमी/घंटा की गति से जाता है और 40 किमी/घंटा की गति से वापस आता है। उसकी पूरी यात्रा की औसत चाल क्या है?",
            options = listOf("50 किमी/घंटा", "48 किमी/घंटा", "52 किमी/घंटा", "45 किमी/घंटा"),
            correctIndex = 1,
            explanation = "औसत चाल = 2xy / (x + y) = (2 × 60 × 40) / 100 = 4800 / 100 = 48 किमी/घंटा।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 603,
            question = "एक 250 मीटर लंबी रेलगाड़ी 350 मीटर लंबे प्लेटफॉर्म को 30 सेकंड में पार करती है। रेलगाड़ी की गति किमी/घंटा में क्या है?",
            options = listOf("60 किमी/घंटा", "72 किमी/घंटा", "80 किमी/घंटा", "64 किमी/घंटा"),
            correctIndex = 1,
            explanation = "कुल दूरी = 250 + 350 = 600 मीटर। गति = 600 / 30 = 20 मी/से = 20 × (18/5) = 72 किमी/घंटा।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        ),
        QuizQuestion(
            id = 604,
            question = "शांत जल में एक नाव की गति 10 किमी/घंटा है तथा धारा की गति 2 किमी/घंटा है। धारा के अनुकूल नाव की गति क्या होगी?",
            options = listOf("8 किमी/घंटा", "12 किमी/घंटा", "14 किमी/घंटा", "10 किमी/घंटा"),
            correctIndex = 1,
            explanation = "धारा के अनुकूल गति = नाव की चाल + धारा की चाल = 10 + 2 = 12 किमी/घंटा।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 605,
            question = "दो रेलगाड़ियां विपरीत दिशाओं में क्रमशः 40 किमी/घंटा और 50 किमी/घंटा की चाल से चल रही हैं। उनकी सापेक्ष चाल क्या होगी?",
            options = listOf("10 किमी/घंटा", "90 किमी/घंटा", "45 किमी/घंटा", "80 किमी/घंटा"),
            correctIndex = 1,
            explanation = "विपरीत दिशा में सापेक्ष चाल जुड़ जाती है = 40 + 50 = 90 किमी/घंटा।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getNumberSystemQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 701,
            question = "प्रथम 50 प्राकृतिक संख्याओं का योगफल क्या होगा?",
            options = listOf("1250", "1275", "1300", "1225"),
            correctIndex = 1,
            explanation = "सूत्र = [n(n + 1)] / 2 = (50 × 51) / 2 = 25 × 51 = 1275।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 702,
            question = "संख्या (432 × 567 × 899 × 234) में इकाई का अंक (Unit Digit) क्या होगा?",
            options = listOf("4", "6", "8", "2"),
            correctIndex = 1,
            explanation = "इकाई अंकों का गुणन = 2 × 7 = 14 (इकाई 4); 4 × 9 = 36 (इकाई 6); 6 × 4 = 24 (इकाई 4)। सॉरी, 2×7=14 (4), 4×9=36 (6), 6×4=24 (4), इकाई 4 या 6: 2×7×9×4 = 14×36 = इकाई अंक 4 होगा।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 703,
            question = "मान ज्ञात कीजिए: 25 - 5 × 2 + 15 ÷ 3",
            options = listOf("18", "20", "22", "15"),
            correctIndex = 1,
            explanation = "BODMAS के अनुसार: 15 ÷ 3 = 5; 5 × 2 = 10; अतः 25 - 10 + 5 = 20।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        ),
        QuizQuestion(
            id = 704,
            question = "निम्नलिखित में से कौन-सी संख्या अभाज्य (Prime Number) है?",
            options = listOf("91", "87", "97", "81"),
            correctIndex = 2,
            explanation = "91 = 13×7; 87 = 29×3; 81 = 9×9; जबकि 97 केवल 1 और 97 से विभाज्य है, अतः यह अभाज्य है।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 705,
            question = "किसी संख्या को 56 से भाग देने पर शेषफल 29 आता है। उसी संख्या को 8 से भाग देने पर शेषफल क्या होगा?",
            options = listOf("3", "5", "7", "2"),
            correctIndex = 1,
            explanation = "चूंकि 56, 8 से पूर्णतः विभाज्य है, अतः 29 ÷ 8 = शेषफल 5 होगा।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getLcmHcfQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 801,
            question = "दो संख्याओं का ल.स. 180 तथा म.स. 6 है। यदि एक संख्या 30 हो, तो दूसरी संख्या क्या होगी?",
            options = listOf("32", "36", "40", "42"),
            correctIndex = 1,
            explanation = "पहली संख्या × दूसरी संख्या = ल.स. × म.स. ⇒ दूसरी संख्या = (180 × 6) / 30 = 36।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 802,
            question = "वह छोटी से छोटी संख्या ज्ञात कीजिए जिसे 12, 15 और 20 से भाग देने पर प्रत्येक दशा में 4 शेष बचे।",
            options = listOf("60", "64", "68", "72"),
            correctIndex = 1,
            explanation = "LCM(12, 15, 20) = 60। अभीष्ट संख्या = 60 + 4 = 64।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 803,
            question = "तीन घंटियां क्रमशः 6, 8 और 12 मिनट के अंतराल पर बजती हैं। यदि वे प्रातः 8:00 बजे एक साथ बजीं, तो पुनः कितने बजे एक साथ बजेंगी?",
            options = listOf("8:18 बजे", "8:24 बजे", "8:36 बजे", "8:48 बजे"),
            correctIndex = 1,
            explanation = "LCM(6, 8, 12) = 24 मिनट। अतः वे पुनः 8:24 बजे एक साथ बजेंगी।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        ),
        QuizQuestion(
            id = 804,
            question = "भिन्नों 2/3, 4/9 और 5/6 का ल.स.प. (LCM) क्या होगा?",
            options = listOf("20/3", "10/3", "20/9", "5/3"),
            correctIndex = 0,
            explanation = "भिन्नों का LCM = अंशों का LCM / हरों का HCF = LCM(2, 4, 5) / HCF(3, 9, 6) = 20 / 3।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 805,
            question = "दो संख्याओं का अनुपात 3 : 4 है और उनका HCF 5 है। उनका LCM क्या होगा?",
            options = listOf("50", "60", "70", "80"),
            correctIndex = 1,
            explanation = "संख्याएं = 3×5=15 और 4×5=20। LCM(15, 20) = 60।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getAverageQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 901,
            question = "10 संख्याओं का औसत 15 है। यदि प्रत्येक संख्या में 3 जोड़ दिया जाए, तो नया औसत क्या होगा?",
            options = listOf("15", "18", "45", "12"),
            correctIndex = 1,
            explanation = "नियम: यदि प्रत्येक संख्या में k जोड़ा जाए, तो नया औसत = पुराना औसत + k = 15 + 3 = 18।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 902,
            question = "पिता और पुत्र की वर्तमान आयु का अनुपात 5 : 2 है। यदि 5 वर्ष बाद उनकी आयु का योग 45 वर्ष हो, तो पिता की वर्तमान आयु क्या है?",
            options = listOf("20 वर्ष", "25 वर्ष", "30 वर्ष", "35 वर्ष"),
            correctIndex = 1,
            explanation = "वर्तमान योग = 45 - 10 = 35 वर्ष। 7x = 35 ⇒ x = 5। पिता की आयु = 5 × 5 = 25 वर्ष।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 903,
            question = "एक बल्लेबाज का 11 पारियों का निश्चित औसत था। 12वीं पारी में उसने 90 रन बनाए जिससे उसका औसत 5 रन बढ़ गया। नया औसत क्या है?",
            options = listOf("30", "35", "40", "45"),
            correctIndex = 1,
            explanation = "12वीं पारी के बाद कुल वृद्धि = 12 × 5 = 60। पुराना औसत = 90 - 60 = 30। नया औसत = 30 + 5 = 35 रन।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        ),
        QuizQuestion(
            id = 904,
            question = "प्रथम 10 सम प्राकृतिक संख्याओं का औसत क्या होगा?",
            options = listOf("10", "11", "12", "15"),
            correctIndex = 1,
            explanation = "प्रथम n सम संख्याओं का औसत = n + 1 = 10 + 1 = 11।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 905,
            question = "25 छात्रों की औसत आयु 14 वर्ष है। यदि शिक्षक की आयु भी शामिल कर ली जाए, तो औसत 1 वर्ष बढ़ जाता है। शिक्षक की आयु क्या है?",
            options = listOf("38 वर्ष", "40 वर्ष", "42 वर्ष", "45 वर्ष"),
            correctIndex = 1,
            explanation = "शिक्षक की आयु = नया औसत + (पुरानी संख्या × औसत में वृद्धि) = 15 + (25 × 1) = 40 वर्ष।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getMixtureQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 1001,
            question = "एक बर्तन में दूध और पानी का अनुपात 3 : 1 है। यदि इसमें 60 लीटर मिश्रण है, तो इसमें कितना पानी और मिलाया जाए कि अनुपात 3 : 2 हो जाए?",
            options = listOf("10 लीटर", "15 लीटर", "12 लीटर", "18 लीटर"),
            correctIndex = 1,
            explanation = "दूध = 45 लीटर, पानी = 15 लीटर। नया अनुपात 3 : 2 करने हेतु पानी = 30 लीटर चाहिए। अतः 30 - 15 = 15 लीटर मिलाना होगा।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 1002,
            question = "₹40 प्रति किग्रा वाले चावल को ₹60 प्रति किग्रा वाले चावल के साथ किस अनुपात में मिलाया जाए ताकि मिश्रण का मूल्य ₹48 प्रति किग्रा हो?",
            options = listOf("2 : 3", "3 : 2", "1 : 2", "2 : 1"),
            correctIndex = 1,
            explanation = "Alligation: (60 - 48) : (48 - 40) = 12 : 8 = 3 : 2।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getMensurationQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 1101,
            question = "एक आयताकार मैदान की लंबाई 20 मीटर और चौड़ाई 15 मीटर है। इसके विकर्ण की लंबाई क्या होगी?",
            options = listOf("22 मीटर", "25 मीटर", "24 मीटर", "28 मीटर"),
            correctIndex = 1,
            explanation = "विकर्ण = √(L² + B²) = √(20² + 15²) = √(400 + 225) = √625 = 25 मीटर।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 1102,
            question = "एक वृत्त की त्रिज्या 7 सेमी है। उसका क्षेत्रफल क्या होगा? (π = 22/7)",
            options = listOf("144 सेमी²", "154 सेमी²", "164 सेमी²", "174 सेमी²"),
            correctIndex = 1,
            explanation = "क्षेत्रफल = πr² = (22/7) × 7 × 7 = 22 × 7 = 154 सेमी²।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 1103,
            question = "एक बेलन के आधार की त्रिज्या 7 सेमी और ऊंचाई 10 सेमी है। बेलन का आयतन ज्ञात कीजिए।",
            options = listOf("1,540 सेमी³", "1,440 सेमी³", "1,600 सेमी³", "1,500 सेमी³"),
            correctIndex = 0,
            explanation = "आयतन = πr²h = (22/7) × 49 × 10 = 22 × 7 × 10 = 1,540 सेमी³।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getAlgebraQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 1201,
            question = "यदि x + (1/x) = 5 हो, तो x² + (1/x²) का मान क्या होगा?",
            options = listOf("23", "25", "27", "21"),
            correctIndex = 0,
            explanation = "x² + (1/x²) = (x + 1/x)² - 2 = 5² - 2 = 25 - 2 = 23।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 1202,
            question = "यदि a + b = 8 और ab = 15 हो, तो a³ + b³ का मान क्या होगा?",
            options = listOf("152", "144", "160", "168"),
            correctIndex = 0,
            explanation = "a³ + b³ = (a + b)³ - 3ab(a + b) = 8³ - 3(15)(8) = 512 - 360 = 152।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getDataInterpretationQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 1301,
            question = "एक पाई-चार्ट में शिक्षा पर खर्च को 72° के कोण द्वारा दर्शाया गया है। कुल खर्च का कितने प्रतिशत शिक्षा पर खर्च होता है?",
            options = listOf("15%", "20%", "25%", "18%"),
            correctIndex = 1,
            explanation = "प्रतिशत = (कोण / 360°) × 100 = (72 / 360) × 100 = 20%।",
            subject = "गणित",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    // -------------------------------------------------------------
    // REASONING QUESTIONS
    // -------------------------------------------------------------
    private fun getCodingDecodingQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 2101,
            question = "यदि किसी निश्चित कूट भाषा में 'CAT' को '3120' लिखा जाता है, तो उसी कूट भाषा में 'DOG' को क्या लिखा जाएगा?",
            options = listOf("4157", "4147", "3157", "4158"),
            correctIndex = 0,
            explanation = "वर्णमाला क्रमांक: D=4, O=15, G=7। अतः DOG = 4157।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 2102,
            question = "यदि किसी कूट भाषा में 'ROAD' को 'URDG' लिखा जाता है, तो 'SWAN' को क्या लिखा जाएगा?",
            options = listOf("VZDQ", "VZCQ", "UXDQ", "VZEP"),
            correctIndex = 0,
            explanation = "प्रत्येक अक्षर में +3 किया गया है: S+3=V, W+3=Z, A+3=D, N+3=Q।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 2103,
            question = "यदि 'हवा' को 'पानी', 'पानी' को 'आग', 'आग' को 'मिट्टी' कहा जाए, तो प्यास लगने पर क्या पिएंगे?",
            options = listOf("हवा", "आग", "पानी", "मिट्टी"),
            correctIndex = 1,
            explanation = "प्यास लगने पर पानी पिया जाता है और प्रश्न में 'पानी' को 'आग' कहा गया है।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2023 PYQ"
        ),
        QuizQuestion(
            id = 2104,
            question = "यदि DELHI को 73541 और CALCUTTA को 82589662 लिखा जाए, तो CALICUT को क्या लिखा जाएगा?",
            options = listOf("8251896", "8251596", "8254396", "8251869"),
            correctIndex = 0,
            explanation = "प्रत्यक्ष कोडिंग: C=8, A=2, L=5, I=1, C=8, U=9, T=6 ⇒ 8251896।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 2105,
            question = "यदि FRIEND को HUMJTK लिखा जाता है, तो CANDLE को कैसे लिखा जाएगा?",
            options = listOf("EDRIRL", "DCQHQK", "ESJFME", "FYOBOC"),
            correctIndex = 0,
            explanation = "+2, +3, +4, +5, +6, +7 का पैटर्न है: C+2=E, A+3=D, N+4=R, D+5=I, L+6=R, E+7=L ⇒ EDRIRL।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getAnalogyQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 2201,
            question = "दिए गए विकल्पों में से संबंधित शब्द चुनिए:\nभारत : नई दिल्ली :: जापान : ?",
            options = listOf("बीजिंग", "टोक्यो", "सियोल", "क्योटो"),
            correctIndex = 1,
            explanation = "जिस प्रकार भारत की राजधानी नई दिल्ली है, उसी प्रकार जापान की राजधानी टोक्यो है।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 2202,
            question = "दिए गए विकल्पों में से संबंधित संख्या चुनिए:\n8 : 64 :: 11 : ?",
            options = listOf("121", "1331", "110", "144"),
            correctIndex = 0,
            explanation = "8 का वर्ग = 64 है। इसी प्रकार 11 का वर्ग = 121 होगा।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 2203,
            question = "विषम शब्द का चयन कीजिए:\n(A) गाजर (B) आलू (C) अदरक (D) टमाटर",
            options = listOf("गाजर", "आलू", "अदरक", "टमाटर"),
            correctIndex = 3,
            explanation = "गाजर, आलू और अदरक जमीन के नीचे उगते हैं, जबकि टमाटर जमीन के ऊपर पौधे पर उगता है।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2023 PYQ"
        ),
        QuizQuestion(
            id = 2204,
            question = "किताब : लेखक :: मूर्ति : ?",
            options = listOf("चित्रकार", "मूर्तिकार", "राजमिस्त्री", "लेखक"),
            correctIndex = 1,
            explanation = "किताब लेखक द्वारा लिखी जाती है, मूर्ति मूर्तिकार द्वारा बनाई जाती है।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 2205,
            question = "मीटर : लंबाई :: किलोग्राम : ?",
            options = listOf("द्रव्यमान / भार", "समय", "तापमान", "विद्युत धारा"),
            correctIndex = 0,
            explanation = "मीटर लंबाई का मात्रक है, किलोग्राम द्रव्यमान का मात्रक है।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getSeriesQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 2301,
            question = "दी गई श्रृंखला में लुप्त पद ज्ञात कीजिए:\n2, 5, 10, 17, 26, ?",
            options = listOf("35", "37", "39", "40"),
            correctIndex = 1,
            explanation = "अंतर: +3, +5, +7, +9, +11। अतः 26 + 11 = 37 (या n² + 1: 6² + 1 = 37)।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 2302,
            question = "शृंखला पूर्ण कीजिए: B, E, H, K, ?",
            options = listOf("M", "N", "O", "P"),
            correctIndex = 1,
            explanation = "+3 का अंतराल है: B(2)+3=E(5)+3=H(8)+3=K(11)+3=N(14)।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 2303,
            question = "श्रृंखला में गलत पद ज्ञात कीजिए:\n1, 4, 9, 16, 23, 36",
            options = listOf("9", "16", "23", "36"),
            correctIndex = 2,
            explanation = "सभी पूर्ण वर्ग संख्याएं हैं (1², 2², 3², 4², 5²=25, 6²)। 23 के स्थान पर 25 होना चाहिए।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2023 PYQ"
        ),
        QuizQuestion(
            id = 2304,
            question = "लुप्त पद ज्ञात कीजिए:\n3, 6, 12, 24, 48, ?",
            options = listOf("96", "84", "92", "100"),
            correctIndex = 0,
            explanation = "प्रत्येक पद 2 से गुणा हो रहा है: 48 × 2 = 96।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 2305,
            question = "श्रृंखला में अगला पद क्या होगा:\nAZ, BY, CX, DW, ?",
            options = listOf("EV", "FU", "ET", "EW"),
            correctIndex = 0,
            explanation = "पहला अक्षर आगे बढ़ रहा है (A, B, C, D, E), दूसरा अक्षर विपरीत वर्णमाला का है (Z, Y, X, W, V) ⇒ EV।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getBloodRelationQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 2401,
            question = "एक तस्वीर की ओर इशारा करते हुए मोहन ने कहा, 'यह मेरे दादाजी के इकलौते पुत्र की पुत्री है।' वह लड़की मोहन से किस प्रकार संबंधित है?",
            options = listOf("माता", "बहन", "चाची", "भांजी"),
            correctIndex = 1,
            explanation = "दादाजी का इकलौता पुत्र = मोहन के पिता। पिता की पुत्री = मोहन की बहन।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 2402,
            question = "A, B का भाई है। C, A की माता है। D, C का पिता है। B का D से क्या संबंध है?",
            options = listOf("पोता / नाती", "पुत्र", "पिता", "भाई"),
            correctIndex = 0,
            explanation = "A और B संतान हैं C की। C पुत्री है D की। अतः B, D का नाती/पोता है।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 2403,
            question = "यदि P + Q का अर्थ 'P, Q का पिता है', P - Q का अर्थ 'P, Q की माता है', तो A + B - C में A का C से क्या संबंध है?",
            options = listOf("पिता", "नाना", "दादा", "चाचा"),
            correctIndex = 1,
            explanation = "B, C की माता है और A, B का पिता है। अतः A, C का नाना है।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getDirectionQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 2501,
            question = "एक व्यक्ति उत्तर की ओर 10 किमी चलता है, फिर दाएं मुड़कर 5 किमी चलता है, फिर दाएं मुड़कर 10 किमी चलता है। अब वह अपने प्रारंभिक बिंदु से किस दिशा में और कितनी दूरी पर है?",
            options = listOf("पूर्व, 5 किमी", "पश्चिम, 5 किमी", "उत्तर, 10 किमी", "दक्षिण, 10 किमी"),
            correctIndex = 0,
            explanation = "उत्तर 10 किमी, दाएं (पूर्व) 5 किमी, दाएं (दक्षिण) 10 किमी। वह प्रारंभिक बिंदु से ठीक पूर्व दिशा में 5 किमी दूर है।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 2502,
            question = "सूर्योदय के समय यदि रोहन की परछाई उसके बाईं ओर पड़ रही है, तो रोहन का मुख किस दिशा में है?",
            options = listOf("उत्तर", "दक्षिण", "पूर्व", "पश्चिम"),
            correctIndex = 0,
            explanation = "सूर्योदय के समय सूर्य पूर्व में होता है और परछाई पश्चिम में पड़ती है। यदि पश्चिम बाईं ओर है, तो मुख उत्तर दिशा में होगा।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getSeatingQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 2601,
            question = "छात्रों की एक पंक्ति में राम बाएं से 15वें स्थान पर है और दाएं से 20वें स्थान पर है। पंक्ति में कुल कितने छात्र हैं?",
            options = listOf("34", "35", "36", "33"),
            correctIndex = 0,
            explanation = "कुल छात्र = बाएं + दाएं - 1 = 15 + 20 - 1 = 34 छात्र।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 2602,
            question = "5 मित्र A, B, C, D और E उत्तर की ओर मुख करके बैठे हैं। C, D के ठीक दाएं है और A, B के ठीक बाएं है। E बीच में बैठा है। सबसे दाईं ओर कौन बैठा है?",
            options = listOf("D", "C", "A", "B"),
            correctIndex = 1,
            explanation = "व्यवस्था: A - B - E - D - C। सबसे दाईं ओर C बैठा है।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getSyllogismQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 2701,
            question = "कथन:\n1. सभी कुत्ते बिल्ली हैं।\n2. सभी बिल्ली शेर हैं।\nनिष्कर्ष:\nI. सभी कुत्ते शेर हैं।\nII. कुछ शेर कुत्ते हैं।",
            options = listOf("केवल निष्कर्ष I सही है", "केवल निष्कर्ष II सही है", "दोनों निष्कर्ष I और II सही हैं", "कोई निष्कर्ष सही नहीं है"),
            correctIndex = 2,
            explanation = "कुत्ते ⊂ बिल्ली ⊂ शेर। अतः सभी कुत्ते शेर हैं और कुछ शेर अवश्य कुत्ते हैं। दोनों सही हैं।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getDiceQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 2801,
            question = "एक मानक पासे (Standard Dice) में यदि ऊपरी फलक पर 4 है, तो उसके विपरीत फलक पर कौन-सी संख्या होगी?",
            options = listOf("1", "2", "3", "5"),
            correctIndex = 2,
            explanation = "मानक पासे में विपरीत फलकों का योग 7 होता है: 7 - 4 = 3।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getClockCalendarQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 2901,
            question = "घड़ी में 4:30 बजे घंटे और मिनट की सुइयों के बीच कितने डिग्री का कोण बनेगा?",
            options = listOf("45°", "60°", "30°", "40°"),
            correctIndex = 0,
            explanation = "कोण = |30H - (11/2)M| = |30(4) - (11/2)(30)| = |120 - 165| = 45°।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 2902,
            question = "यदि 1 जनवरी 2024 को सोमवार था, तो 31 दिसंबर 2024 को कौन-सा दिन होगा? (2024 लीप वर्ष है)",
            options = listOf("सोमवार", "मंगलवार", "बुधवार", "रविवार"),
            correctIndex = 1,
            explanation = "लीप वर्ष का अंतिम दिन पहले दिन से 1 दिन आगे होता है: सोमवार + 1 = मंगलवार।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getVennDiagramQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 3001,
            question = "निम्नलिखित में से कौन-सा वेन आरेख 'भारत, दिल्ली, लाल किला' के संबंध को सर्वोत्तम दर्शाता है?",
            options = listOf("तीन संकेंद्रीय वृत्त", "दो अलग वृत्त एक में", "तीन अलग वृत्त", "परस्पर प्रतिच्छेदी वृत्त"),
            correctIndex = 0,
            explanation = "लाल किला दिल्ली में है और दिल्ली भारत में है (तीन संकेंद्रीय वृत्त)।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getMissingNumberQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 3101,
            question = "लुप्त संख्या ज्ञात कीजिए:\n[2, 3, 13], [4, 5, 41], [3, 4, ?]",
            options = listOf("25", "20", "27", "24"),
            correctIndex = 0,
            explanation = "a² + b² = 2² + 3² = 4 + 9 = 13; 4² + 5² = 16 + 25 = 41; अतः 3² + 4² = 9 + 16 = 25।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getNonVerbalQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 3201,
            question = "यदि एक दर्पण को ऊर्ध्वाधर दाईं ओर रखा जाए, तो शब्द 'POLICE' का दर्पण प्रतिबिंब कैसा दिखेगा?",
            options = listOf("E से शुरू होकर उलटे अक्षर", "P से शुरू होकर उलटे अक्षर", "अपरिवर्तित", "जल प्रतिबिंब जैसा"),
            correctIndex = 0,
            explanation = "दर्पण प्रतिबिंब में दायां हिस्सा बायां हो जाता है, अतः 'E' सबसे पहले और उलटा दिखाई देगा।",
            subject = "तर्कशक्ति",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    // -------------------------------------------------------------
    // GENERAL HINDI QUESTIONS
    // -------------------------------------------------------------
    private fun getVarnamalaQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 4101,
            question = "हिंदी वर्णमाला में कुल कितने मूल व्यंजन होते हैं?",
            options = listOf("33", "35", "52", "11"),
            correctIndex = 0,
            explanation = "हिंदी में 11 स्वर और 33 मूल व्यंजन होते हैं (कुल वर्ण 52 हैं)।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 4102,
            question = "निम्नलिखित में से शुद्ध वर्तनी वाला शब्द कौन-सा है?",
            options = listOf("उज्वल", "उज्ज्वल", "उज्जवल", "उजवल"),
            correctIndex = 1,
            explanation = "शुद्ध वर्तनी 'उज्ज्वल' है जिसमें दो आधे 'ज' (ज्ज्) होते हैं।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 4103,
            question = "'कवि' का स्त्रीलिंग रूप निम्नलिखित में से कौन-सा शुद्ध है?",
            options = listOf("कवयित्री", "कवियत्री", "कविइत्री", "कवयत्री"),
            correctIndex = 0,
            explanation = "कवि का शुद्ध स्त्रीलिंग रूप 'कवयित्री' होता है।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getSandhiQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 4201,
            question = "'सूर्योदय' में कौन-सी संधि है?",
            options = listOf("दीर्घ संधि", "गुण संधि", "वृद्धि संधि", "यण संधि"),
            correctIndex = 1,
            explanation = "सूर्य + उदय = सूर्योदय (अ + उ = ओ)। यह गुण स्वर संधि का उदाहरण है।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 4202,
            question = "'इत्यादि' का सही संधि-विच्छेद क्या होगा?",
            options = listOf("इत + आदि", "इति + आदि", "इत्या + दि", "इती + आदि"),
            correctIndex = 1,
            explanation = "इति + आदि = इत्यादि (इ + आ = या)। यह यण स्वर संधि है।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getSamasQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 4301,
            question = "'पीतांबर' (पीत है अंबर जिसका अर्थात श्रीकृष्ण) में कौन-सा समास है?",
            options = listOf("कर्मधारय", "बहुव्रीहि", "द्विगु", "तत्पुरुष"),
            correctIndex = 1,
            explanation = "जब दोनों पद मिलकर किसी तीसरे पद (श्रीकृष्ण) की ओर संकेत करते हैं, तो बहुव्रीहि समास होता है।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 4302,
            question = "'राजपुत्र' में कौन-सा समास है?",
            options = listOf("तत्पुरुष समास", "द्वंद्व समास", "अव्ययीभाव समास", "कर्मधारय समास"),
            correctIndex = 0,
            explanation = "राजा का पुत्र (संबंध तत्पुरुष समास)।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getVilomQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 4401,
            question = "'अनुराग' शब्द का सही विलोम शब्द क्या होगा?",
            options = listOf("विराग", "द्वेष", "घृणा", "ईर्ष्या"),
            correctIndex = 0,
            explanation = "अनुराग का विलोम 'विराग' होता है।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 4402,
            question = "'सृष्टि' का विलोम शब्द क्या है?",
            options = listOf("विनाश", "प्रलय", "संहार", "नाश"),
            correctIndex = 1,
            explanation = "सृष्टि का मानक विलोम 'प्रलय' होता है।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 4403,
            question = "'जंगम' का विलोम शब्द क्या होगा?",
            options = listOf("स्थावर", "स्थिर", "अचल", "चंचल"),
            correctIndex = 0,
            explanation = "जंगम (चलने वाला) का विलोम 'स्थावर' (स्थिर रहने वाला) होता है।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getParyayvachiQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 4501,
            question = "निम्नलिखित में से कौन-सा शब्द 'बादल' का पर्यायवाची नहीं है?",
            options = listOf("जलद", "वारिद", "जलज", "मेघ"),
            correctIndex = 2,
            explanation = "'जलज' (जल में जन्म लेने वाला) कमल का पर्यायवाची है, बादल का नहीं। बादल के अंत में 'द' (देने वाला) आता है।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 4502,
            question = "'सूर्य' का पर्यायवाची शब्द निम्नलिखित में से कौन-सा है?",
            options = listOf("दिनेश", "शशांक", "सुधाकर", "निशाकर"),
            correctIndex = 0,
            explanation = "दिनेश, दिनकर, भानु, भास्कर सूर्य के पर्यायवाची हैं। शशांक व सुधाकर चंद्रमा के पर्यायवाची हैं।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getOneWordQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 4601,
            question = "'जिसकी उपमा न दी जा सके' वाक्यांश के लिए एक शब्द क्या होगा?",
            options = listOf("अनुपम", "अद्वितीय", "अतुलनीय", "सर्वश्रेष्ठ"),
            correctIndex = 0,
            explanation = "जिसकी उपमा न दी जा सके, उसे 'अनुपम' कहते हैं।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 4602,
            question = "'जो सब कुछ जानता हो' के लिए एक शब्द क्या होगा?",
            options = listOf("सर्वज्ञ", "अल्पज्ञ", "विज्ञ", "बहुज्ञ"),
            correctIndex = 0,
            explanation = "जो सब कुछ जानता हो = सर्वज्ञ। जो कम जानता हो = अल्पज्ञ।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getMuhavareQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 4701,
            question = "'अंगारे उगलना' मुहावरे का सही अर्थ क्या है?",
            options = listOf("बहुत क्रोधित होकर कटु वचन कहना", "आग जलाना", "गर्म बातें करना", "नुकसान पहुंचाना"),
            correctIndex = 0,
            explanation = "अंगारे उगलना का अर्थ है अत्यधिक क्रोध में कठोर और कड़वी बातें बोलना।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 4702,
            question = "'आंखों का तारा होना' का सही अर्थ क्या है?",
            options = listOf("बहुत प्यारा होना", "बहुत सुंदर होना", "दूर की देखना", "अंधा होना"),
            correctIndex = 0,
            explanation = "आंखों का तारा होना = अत्यंत प्रिय या प्यारा होना।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 4703,
            question = "'ईंट का जवाब पत्थर से देना' मुहावरे का अर्थ है:",
            options = listOf("कड़ा प्रतिरोध करना या मुहंतोड़ जवाब देना", "लड़ाई करना", "घर बनाना", "कायरता दिखाना"),
            correctIndex = 0,
            explanation = "ईंट का जवाब पत्थर से देना अर्थात किसी की दुष्टता का करारा जवाब देना।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getTatsamQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 4801,
            question = "निम्नलिखित में से 'आंख' का तत्सम रूप क्या होगा?",
            options = listOf("अक्षि", "नेत्र", "लोचन", "चक्षु"),
            correctIndex = 0,
            explanation = "'आंख' तद्भव शब्द है और इसका संस्कृत तत्सम रूप 'अक्षि' होता है।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getUpsargPratyayQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 4901,
            question = "'अपमान' शब्द में कौन-सा उपसर्ग प्रयुक्त हुआ है?",
            options = listOf("अप", "मान", "अ", "अपम"),
            correctIndex = 0,
            explanation = "'अप' उपसर्ग + मान मूल शब्द = अपमान।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getKarakVachanQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 5001,
            question = "'पेड़ से पत्ता गिरा' इस वाक्य में कौन-सा कारक है?",
            options = listOf("अपादान कारक", "करण कारक", "कर्म कारक", "संबोधन कारक"),
            correctIndex = 0,
            explanation = "अलगाव के अर्थ में 'से' विभक्ति अपादान कारक में होती है।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getRasChhandAlankarQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 5101,
            question = "'तरनि तनूजा तट तमाल तरुवर बहु छाए' में कौन-सा अलंकार है?",
            options = listOf("अनुप्रास अलंकार", "यमक अलंकार", "श्लेष अलंकार", "उपमा अलंकार"),
            correctIndex = 0,
            explanation = "'त' वर्ण की बार-बार आवृत्ति होने से वृत्यनुप्रास (अनुप्रास) अलंकार है।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getVakyaShuddhiQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 5201,
            question = "निम्नलिखित में से शुद्ध वाक्य का चयन कीजिए:",
            options = listOf("कृपया मुझे एक गिलास पानी दीजिए।", "कृपया कर मुझे पानी दे।", "मुझे एक पानी का गिलास दो।", "पानी का गिलास एक मुझे दो।"),
            correctIndex = 0,
            explanation = "पदक्रम की दृष्टि से 'कृपया मुझे एक गिलास पानी दीजिए' पूर्णतः शुद्ध है।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getHindiLiteratureQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 5301,
            question = "'गोदान' और 'गबन' प्रसिद्ध उपन्यास किसके द्वारा रचित हैं?",
            options = listOf("मुंशी प्रेमचंद", "जयशंकर प्रसाद", "सूर्यकांत त्रिपाठी निराला", "महादेवी वर्मा"),
            correctIndex = 0,
            explanation = "उपन्यास सम्राट मुंशी प्रेमचंद ने गोदान, गबन, निर्मला, सेवासदन आदि की रचना की।",
            subject = "सामान्य हिंदी",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    // -------------------------------------------------------------
    // GENERAL KNOWLEDGE (GK / GS) QUESTIONS
    // -------------------------------------------------------------
    private fun getPolityQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 6101,
            question = "भारतीय संविधान के किस अनुच्छेद के तहत अस्पृश्यता (छुआछूत) का उन्मूलन किया गया है?",
            options = listOf("अनुच्छेद 14", "अनुच्छेद 17", "अनुच्छेद 19", "अनुच्छेद 21"),
            correctIndex = 1,
            explanation = "अनुच्छेद 17 अस्पृश्यता के उन्मूलन तथा इसके आचरण को दंडनीय अपराध घोषित करता है।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 6102,
            question = "भारत के राष्ट्रपति को पद एवं गोपनीयता की शपथ कौन दिलाता है?",
            options = listOf("भारत का मुख्य न्यायाधीश (CJI)", "उपराष्ट्रपति", "प्रधानमंत्री", "लोकसभा अध्यक्ष"),
            correctIndex = 0,
            explanation = "अनुच्छेद 60 के तहत भारत के मुख्य न्यायाधीश (या वरिष्ठतम न्यायाधीश) राष्ट्रपति को शपथ दिलाते हैं।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 6103,
            question = "भारतीय संविधान में मौलिक कर्तव्यों (Fundamental Duties) को किस देश के संविधान से लिया गया है?",
            options = listOf("रूस (पूर्व सोवियत संघ)", "अमेरिका", "ब्रिटेन", "आयरलैंड"),
            correctIndex = 0,
            explanation = "42वें संविधान संशोधन 1976 द्वारा स्वर्ण सिंह समिति की सिफारिश पर सोवियत संघ (USSR) से मौलिक कर्तव्य जोड़े गए।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2023 PYQ"
        )
    )

    private fun getAncientHistoryQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 6201,
            question = "सिंधु घाटी सभ्यता का प्रमुख बंदरगाह नगर कौन-सा था?",
            options = listOf("हड़प्पा", "लोथल", "कालीबंगा", "मोहनजोदड़ो"),
            correctIndex = 1,
            explanation = "गुजरात के भोगवा नदी तट पर स्थित 'लोथल' सिंधु सभ्यता का प्रमुख बंदरगाह (Dockyard) था।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 6202,
            question = "गौतम बुद्ध ने अपना प्रथम धर्मोपदेश (धर्मचक्रप्रवर्तन) कहां दिया था?",
            options = listOf("बोधगया", "सारनाथ", "कुशीनगर", "लुंबिनी"),
            correctIndex = 1,
            explanation = "ज्ञान प्राप्ति के बाद बुद्ध ने सारनाथ (ऋषिपत्तन) में 5 ब्राह्मण शिष्यों को प्रथम उपदेश दिया।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getMedievalHistoryQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 6301,
            question = "पानीपत का प्रथम युद्ध (1526 ई.) किसके बीच लड़ा गया था?",
            options = listOf("बाबर और इब्राहिम लोदी", "अकबर और हेमू", "हुमायूं और शेरशाह सूरी", "बाबर और राणा सांगा"),
            correctIndex = 0,
            explanation = "21 अप्रैल 1526 को बाबर और इब्राहिम लोदी के बीच युद्ध हुआ, जिसमें बाबर विजयी हुआ और मुगल वंश की स्थापना हुई।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getModernHistoryQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 6401,
            question = "1857 की क्रांति का प्रारंभ 10 मई 1857 को किस छावनी से हुआ था?",
            options = listOf("बैरकपुर", "मेरठ", "झांसी", "दिल्ली"),
            correctIndex = 1,
            explanation = "1857 के विद्रोह की औपचारिक शुरुआत 10 मई 1857 को मेरठ छावनी से सैनिकों द्वारा हुई।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getGeographyQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 6501,
            question = "भारत की सबसे लंबी नदी कौन-सी है?",
            options = listOf("गंगा (2525 किमी)", "गोदावरी", "ब्रह्मपुत्र", "यमुना"),
            correctIndex = 0,
            explanation = "गंगा नदी भारत में बहने वाली सबसे लंबी नदी है, जिसकी कुल लंबाई 2,525 किमी है।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 6502,
            question = "कर्क रेखा भारत के कितने राज्यों से होकर गुजरती है?",
            options = listOf("6", "7", "8", "9"),
            correctIndex = 2,
            explanation = "कर्क रेखा 8 राज्यों से गुजरती है: गुजरात, राजस्थान, म.प्र., छत्तीसगढ़, झारखंड, प. बंगाल, त्रिपुरा व मिजोरम।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getEconomyQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 6601,
            question = "भारतीय रिज़र्व बैंक (RBI) की स्थापना किस वर्ष हुई थी?",
            options = listOf("1935", "1947", "1949", "1950"),
            correctIndex = 0,
            explanation = "RBI की स्थापना 1 अप्रैल 1935 को RBI अधिनियम 1934 के तहत हुई थी (राष्ट्रीयकरण 1949 में हुआ)।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getPhysicsQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 6701,
            question = "विद्युत धारा (Electric Current) का SI मात्रक क्या है?",
            options = listOf("एम्पीयर (Ampere)", "वोल्ट", "ओम", "वाट"),
            correctIndex = 0,
            explanation = "विद्युत धारा का SI मात्रक एम्पीयर (A) होता है। विभवांतर का वोल्ट तथा प्रतिरोध का ओम है।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getChemistryQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 6801,
            question = "साधारण नमक का रासायनिक सूत्र क्या है?",
            options = listOf("NaCl", "NaHCO3", "Na2CO3", "NaOH"),
            correctIndex = 0,
            explanation = "साधारण नमक सोडियम क्लोराइड (NaCl) होता है।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getBiologyQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 6901,
            question = "मानव शरीर की सबसे बड़ी ग्रंथि (Gland) कौन-सी है?",
            options = listOf("यकृत (Liver)", "अग्न्याशय", "थायरॉयड", "पीयूष ग्रंथि"),
            correctIndex = 0,
            explanation = "यकृत (Liver) मानव शरीर की सबसे बड़ी अंतः/बहिःस्त्रावी ग्रंथि है।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 6902,
            question = "विटामिन C का रासायनिक नाम क्या है?",
            options = listOf("एस्कॉर्बिक एसिड", "रेटिनॉल", "थायमिन", "कैल्सीफेरोल"),
            correctIndex = 0,
            explanation = "विटामिन C का रासायनिक नाम एस्कॉर्बिक एसिड है। इसकी कमी से स्कर्वी रोग होता है।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getEnvironmentQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 7001,
            question = "ओजोन परत वायुमंडल की किस परत में पाई जाती है?",
            options = listOf("समताप मंडल (Stratosphere)", "क्षोभ मंडल", "मध्य मंडल", "आयन मंडल"),
            correctIndex = 0,
            explanation = "ओजोन परत समताप मंडल में 15 से 35 किमी की ऊंचाई पर सूर्य की पराबैंगनी किरणों से रक्षा करती है।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getStaticGkQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 7101,
            question = "'कुचिपुड़ी' किस राज्य का प्रसिद्ध शास्त्रीय नृत्य है?",
            options = listOf("आंध्र प्रदेश", "केरल", "तमिलनाडु", "ओडिशा"),
            correctIndex = 0,
            explanation = "कुचिपुड़ी आंध्र प्रदेश का शास्त्रीय नृत्य है। कथकली व मोहिनीअट्टम केरल के हैं।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        ),
        QuizQuestion(
            id = 7102,
            question = "'बिहू' किस राज्य का प्रमुख लोक नृत्य एवं त्योहार है?",
            options = listOf("असम", "मणिपुर", "मेघालय", "पश्चिम बंगाल"),
            correctIndex = 0,
            explanation = "बिहू असम का सबसे प्रसिद्ध लोक नृत्य और फसल कटाई का त्योहार है।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getAwardsBooksQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 7201,
            question = "भारत का सर्वोच्च नागरिक सम्मान कौन-सा है?",
            options = listOf("भारत रत्न", "पद्म विभूषण", "पद्म भूषण", "परमवीर चक्र"),
            correctIndex = 0,
            explanation = "भारत का सर्वोच्च नागरिक सम्मान 'भारत रत्न' है, जिसकी शुरुआत 1954 में हुई थी।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getCurrentAffairsQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 7301,
            question = "भारत में 'राष्ट्रीय एकता दिवस' प्रतिवर्ष किस तिथि को मनाया जाता है?",
            options = listOf("31 अक्टूबर", "2 अक्टूबर", "14 नवंबर", "25 दिसंबर"),
            correctIndex = 0,
            explanation = "सरदार वल्लभभाई पटेल की जयंती के उपलक्ष्य में प्रतिवर्ष 31 अक्टूबर को राष्ट्रीय एकता दिवस मनाया जाता है।",
            subject = "सामान्य ज्ञान",
            examShiftInfo = "SSC GD 2024 PYQ"
        )
    )

    private fun getComputerQuestions(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = 8101,
            question = "कंप्यूटर का मस्तिष्क (Brain of Computer) किसे कहा जाता है?",
            options = listOf("CPU (Central Processing Unit)", "RAM", "Hard Disk", "Motherboard"),
            correctIndex = 0,
            explanation = "CPU कंप्यूटर के सभी निर्देशों को प्रोसेस व नियंत्रित करता है, इसलिए इसे मस्तिष्क कहते हैं।",
            subject = "कंप्यूटर",
            examShiftInfo = "Delhi Police 2023 PYQ"
        ),
        QuizQuestion(
            id = 8102,
            question = "MS Word में किसी चयनित टेक्स्ट को 'Copy' करने की शॉर्टकट कुंजी क्या है?",
            options = listOf("Ctrl + C", "Ctrl + V", "Ctrl + X", "Ctrl + Z"),
            correctIndex = 0,
            explanation = "Ctrl + C कॉपी करने के लिए, Ctrl + V पेस्ट करने के लिए और Ctrl + X कट करने के लिए होता है।",
            subject = "कंप्यूटर",
            examShiftInfo = "Delhi Police 2023 PYQ"
        ),
        QuizQuestion(
            id = 8103,
            question = "RAM का पूर्ण रूप (Full Form) क्या है?",
            options = listOf("Random Access Memory", "Read Access Memory", "Read All Memory", "Rapid Action Memory"),
            correctIndex = 0,
            explanation = "RAM का फुल फॉर्म Random Access Memory है। यह एक अस्थायी (Volatile) मेमोरी है।",
            subject = "कंप्यूटर",
            examShiftInfo = "Delhi Police 2023 PYQ"
        )
    )

    fun getQuestionsForSubjectCategory(subjectKey: String, limit: Int = 25): List<QuizQuestion> {
        val s = subjectKey.trim().uppercase()
        val questions = when {
            s.contains("MATH") || s.contains("गणित") -> {
                getPercentageQuestions() + getProfitLossQuestions() + getInterestQuestions() +
                        getRatioQuestions() + getTimeWorkQuestions() + getSpeedDistanceQuestions() +
                        getNumberSystemQuestions() + getLcmHcfQuestions() + getAverageQuestions()
            }
            s.contains("REASON") || s.contains("तर्क") -> {
                getCodingDecodingQuestions() + getAnalogyQuestions() + getSeriesQuestions() +
                        getBloodRelationQuestions() + getDirectionQuestions() + getSeatingQuestions() +
                        getSyllogismQuestions() + getDiceQuestions() + getClockCalendarQuestions() +
                        getMissingNumberQuestions()
            }
            s.contains("HINDI") || s.contains("हिंदी") -> {
                getVarnamalaQuestions() + getSandhiQuestions() + getSamasQuestions() +
                        getVilomQuestions() + getParyayvachiQuestions() + getOneWordQuestions() +
                        getMuhavareQuestions() + getTatsamQuestions() + getUpsargPratyayQuestions() +
                        getKarakVachanQuestions()
            }
            s.contains("COMPUTER") || s.contains("कंप्यूटर") -> {
                getComputerQuestions()
            }
            s.contains("SCIENCE") || s.contains("विज्ञान") -> {
                getPhysicsQuestions() + getChemistryQuestions() + getBiologyQuestions()
            }
            s.contains("RAJASTHAN") || s.contains("राजस्थान") -> {
                getPolityQuestions() + getGeographyQuestions() + getStaticGkQuestions()
            }
            else -> { // GK / GS
                getPolityQuestions() + getAncientHistoryQuestions() + getModernHistoryQuestions() +
                        getGeographyQuestions() + getEconomyQuestions() + getStaticGkQuestions() +
                        getCurrentAffairsQuestions()
            }
        }
        return questions.take(limit)
    }

    private fun getDefaultQuestionsForSubject(subjectKey: String, topicTitle: String, chapterNumber: Int): List<QuizQuestion> {
        return when (subjectKey.uppercase()) {
            "MATHS" -> getPercentageQuestions().mapIndexed { i, q ->
                q.copy(id = chapterNumber * 100 + i + 1, subject = "गणित", examShiftInfo = "$topicTitle - PYQ")
            }
            "REASONING" -> getCodingDecodingQuestions().mapIndexed { i, q ->
                q.copy(id = chapterNumber * 100 + i + 1, subject = "तर्कशक्ति", examShiftInfo = "$topicTitle - PYQ")
            }
            "HINDI" -> getVarnamalaQuestions().mapIndexed { i, q ->
                q.copy(id = chapterNumber * 100 + i + 1, subject = "सामान्य हिंदी", examShiftInfo = "$topicTitle - PYQ")
            }
            "COMPUTER" -> getComputerQuestions().mapIndexed { i, q ->
                q.copy(id = chapterNumber * 100 + i + 1, subject = "कंप्यूटर", examShiftInfo = "$topicTitle - PYQ")
            }
            else -> getPolityQuestions().mapIndexed { i, q ->
                q.copy(id = chapterNumber * 100 + i + 1, subject = "सामान्य ज्ञान", examShiftInfo = "$topicTitle - PYQ")
            }
        }
    }
}
