package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ChapterItem
import com.example.model.CustomChapterRepository
import com.example.model.ExamSubjectCategory
import com.example.model.QuizQuestion
import com.example.model.SubExam
import com.example.service.AiPromptRulesManager
import com.example.service.GeminiQuestionParserService
import com.example.service.GitHubConfig
import com.example.service.GitHubSyncService
import com.example.ui.util.TextFormatHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun adminTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF0F172A),
    unfocusedTextColor = Color(0xFF0F172A),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    cursorColor = Color(0xFF2563EB),
    focusedBorderColor = Color(0xFF2563EB),
    unfocusedBorderColor = Color(0xFFCBD5E1),
    focusedPlaceholderColor = Color(0xFF94A3B8),
    unfocusedPlaceholderColor = Color(0xFF94A3B8)
)

/**
 * Dialog for Admin to add a new Subject category under an exam
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminAddSubjectDialog(
    subExam: SubExam,
    onDismiss: () -> Unit,
    onSubjectAdded: (String) -> Unit
) {
    val context = LocalContext.current
    var subjectName by remember { mutableStateOf("") }
    var englishName by remember { mutableStateOf("") }
    var selectedIconType by remember { mutableStateOf("BOOK") }
    var isSaving by remember { mutableStateOf(false) }

    val iconOptions = listOf(
        Triple("BOOK", "सामान्य विषय", Icons.AutoMirrored.Filled.MenuBook),
        Triple("MATHS", "गणित / Math", Icons.Default.Calculate),
        Triple("REASONING", "रीजनिंग / Brain", Icons.Default.Psychology),
        Triple("SCIENCE", "विज्ञान / Science", Icons.Default.Science),
        Triple("COMPUTER", "कंप्यूटर / Tech", Icons.Default.Computer),
        Triple("LAW", "मूलविधि / कानून", Icons.Default.Gavel),
        Triple("LANGUAGE", "भाषा (हिंदी/Eng)", Icons.Default.Language),
        Triple("GK", "GK / भूगोल / GS", Icons.Default.Public),
        Triple("IDEA", "अन्य विशेष", Icons.Default.Lightbulb)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2563EB)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        text = "नया विषय जोड़ें (Add Subject)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "परीक्षा: ${subExam.name}",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("विषय का नाम (हिंदी / English)") },
                    placeholder = { Text("उदा. कंप्यूटर ज्ञान या राजस्थान कला-संस्कृति") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_new_subject_name_input")
                )

                OutlinedTextField(
                    value = englishName,
                    onValueChange = { englishName = it },
                    label = { Text("English Title (वैकल्पिक)") },
                    placeholder = { Text("e.g. Computer Awareness") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "विषय का आइकन चुनें (Select Icon):",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    iconOptions.forEach { (type, label, icon) ->
                        val isSelected = selectedIconType == type
                        Surface(
                            onClick = { selectedIconType = type },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF1D4ED8) else Color(0xFFCBD5E1)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFF475569),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF334155)
                                )
                            }
                        }
                    }
                }

                // Real-time Live Preview of Subject Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "⚡ लाइव बटन प्रीव्यू (Live Real-time Preview):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0F172A),
                            shadowElevation = 2.dp,
                            modifier = Modifier.height(42.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = CustomChapterRepository.getIconVector(selectedIconType),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = subjectName.trim().ifBlank { "विषय का नाम (ऊपर टाइप करें)" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "नया",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (subjectName.trim().isBlank()) {
                        Toast.makeText(context, "कृपया विषय का नाम दर्ज करें!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSaving = true
                    val added = CustomChapterRepository.addSubject(
                        subExamId = subExam.id,
                        name = subjectName.trim(),
                        englishName = englishName.trim().ifEmpty { subjectName.trim() },
                        iconType = selectedIconType,
                        context = context
                    )
                    isSaving = false
                    Toast.makeText(context, "विषय '${added.name}' सफलतापूर्वक जोड़ा गया!", Toast.LENGTH_SHORT).show()
                    onSubjectAdded(added.key)
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("admin_save_subject_btn")
            ) {
                Text("विषय जोड़ें (Save Subject)", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text("रद्द करें")
            }
        }
    )
}

/**
 * Full Dialog for Admin to add a new Chapter / Button with Question Creation (AI Auto-Gen or Manual)
 */
@Composable

/**
 * Generates authentic, high-quality MCQs based on the custom chapter name and subject
 */
private fun generateSampleQuestionsForChapter(chapterName: String, subjectName: String): List<QuizQuestion> {
    val cleanName = chapterName.trim()
    val isMaths = subjectName.contains("गणित") || subjectName.contains("Math") || cleanName.contains("प्रतिशत") || cleanName.contains("ब्याज") || cleanName.contains("अनुपात") || cleanName.contains("औसत") || cleanName.contains("कार्य")
    val isReasoning = subjectName.contains("तर्क") || subjectName.contains("Reasoning") || cleanName.contains("कोडिंग") || cleanName.contains("दिशा") || cleanName.contains("रक्त") || cleanName.contains("श्रृंखला") || cleanName.contains("कथन")
    val isComputer = subjectName.contains("कंप्यूटर") || subjectName.contains("Computer") || cleanName.contains("इंटरनेट") || cleanName.contains("MS")

    val questions = mutableListOf<QuizQuestion>()

    if (isMaths) {
        questions.add(
            QuizQuestion(
                id = 1,
                question = "$cleanName से संबंधित: यदि किसी राशि पर 10% वार्षिक दर से 2 वर्ष का चक्रवृद्धि ब्याज ₹420 है, तो वह मूलधन ज्ञात कीजिए?",
                options = listOf("₹2,000", "₹2,200", "₹2,500", "₹1,800"),
                correctIndex = 0,
                explanation = "सूत्र: C.I = P[(1 + R/100)^t - 1] => 420 = P[(1.1)^2 - 1] = P(0.21) => P = 420 / 0.21 = ₹2,000.",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 2,
                question = "$cleanName: दो संख्याओं का अनुपात 3 : 5 है और उनका योग 160 है। बड़ी संख्या क्या होगी?",
                options = listOf("60", "100", "80", "120"),
                correctIndex = 1,
                explanation = "अनुपात योग = 3 + 5 = 8 इकाई = 160 => 1 इकाई = 20. बड़ी संख्या = 5 * 20 = 100.",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 3,
                question = "$cleanName: एक दुकानदार किसी वस्तु को 20% लाभ पर बेचता है। यदि क्रय मूल्य ₹500 है, तो विक्रय मूल्य ज्ञात कीजिए?",
                options = listOf("₹550", "₹600", "₹620", "₹580"),
                correctIndex = 1,
                explanation = "विक्रय मूल्य (SP) = CP * (100 + लाभ%)/100 = 500 * (120/100) = ₹600.",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 4,
                question = "$cleanName: A किसी कार्य को 10 दिन में तथा B उसी कार्य को 15 दिन में पूरा करता है। दोनों मिलकर उस कार्य को कितने दिन में करेंगे?",
                options = listOf("5 दिन", "6 दिन", "8 दिन", "12 दिन"),
                correctIndex = 1,
                explanation = "संयुक्त समय = (A * B) / (A + B) = (10 * 15) / (10 + 15) = 150 / 25 = 6 दिन.",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 5,
                question = "$cleanName: 72 किमी/घंटा की चाल से चल रही 180 मीटर लंबी रेलगाड़ी एक खंभे को कितने सेकंड में पार करेगी?",
                options = listOf("9 सेकंड", "10 सेकंड", "12 सेकंड", "15 सेकंड"),
                correctIndex = 0,
                explanation = "चाल (m/s) = 72 * (5/18) = 20 m/s. समय = दूरी / चाल = 180 / 20 = 9 सेकंड.",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 6,
                question = "$cleanName: प्रथम 20 प्राकृतिक संख्याओं का औसत कितना होगा?",
                options = listOf("10", "10.5", "11", "11.5"),
                correctIndex = 1,
                explanation = "प्रथम n प्राकृतिक संख्याओं का औसत = (n + 1) / 2 = (20 + 1) / 2 = 10.5.",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 7,
                question = "$cleanName: यदि 12 वस्तुओं का क्रय मूल्य 9 वस्तुओं के विक्रय मूल्य के बराबर है, तो लाभ प्रतिशत क्या होगा?",
                options = listOf("25%", "33.33%", "20%", "40%"),
                correctIndex = 1,
                explanation = "12 CP = 9 SP => SP/CP = 12/9 = 4/3. लाभ% = (1/3) * 100 = 33.33%.",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 8,
                question = "$cleanName: किसी वृत्त का व्यास 28 सेमी है। उसकी परिधि (Circumference) क्या होगी?",
                options = listOf("88 सेमी", "176 सेमी", "44 सेमी", "616 सेमी"),
                correctIndex = 0,
                explanation = "परिधि = π * d = (22/7) * 28 = 88 सेमी.",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 9,
                question = "$cleanName: 24, 36 और 48 का महत्तम समापवर्तक (HCF) क्या होगा?",
                options = listOf("6", "12", "18", "24"),
                correctIndex = 1,
                explanation = "24, 36, 48 का सबसे बड़ा उभयनिष्ठ भाजक (HCF) 12 है।",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 10,
                question = "$cleanName: ₹5,000 की राशि पर 8% वार्षिक साधारण ब्याज की दर से 3 वर्ष का साधारण ब्याज कितना होगा?",
                options = listOf("₹1,000", "₹1,200", "₹1,400", "₹1,500"),
                correctIndex = 1,
                explanation = "SI = (P * R * T) / 100 = (5000 * 8 * 3) / 100 = ₹1,200.",
                subject = subjectName
            )
        )
    } else if (isReasoning) {
        questions.add(
            QuizQuestion(
                id = 1,
                question = "$cleanName: यदि किसी निश्चित कूट भाषा में 'MANGO' को 'NBOHP' लिखा जाता है, तो उसी भाषा में 'APPLE' को क्या लिखा जाएगा?",
                options = listOf("BQQMF", "BQPMF", "BQQNF", "CQPMF"),
                correctIndex = 0,
                explanation = "प्रत्येक अक्षर में +1 की वृद्धि हो रही है: A->B, P->Q, P->Q, L->M, E->F => BQQMF.",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 2,
                question = "$cleanName: दी गई श्रृंखला में अगला पद क्या होगा? 2, 5, 10, 17, 26, ?",
                options = listOf("35", "37", "39", "41"),
                correctIndex = 1,
                explanation = "पैटर्न: +3, +5, +7, +9, +11 => 26 + 11 = 37 (या n^2 + 1: 1^2+1, 2^2+1, ..., 6^2+1=37).",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 3,
                question = "$cleanName: एक व्यक्ति उत्तर की ओर 10 किमी चलता है, फिर दाएं मुड़कर 5 किमी चलता है, और अंत में दाएं मुड़कर 10 किमी चलता है। अब वह अपने प्रारंभिक बिंदु से किस दिशा में और कितनी दूरी पर है?",
                options = listOf("पूर्व दिशा में 5 किमी", "पश्चिम दिशा में 5 किमी", "उत्तर दिशा में 10 किमी", "दक्षिण दिशा में 5 किमी"),
                correctIndex = 0,
                explanation = "वह प्रारंभिक बिंदु से ठीक पूर्व (East) दिशा में 5 किमी की दूरी पर स्थित है।",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 4,
                question = "$cleanName: A, B का भाई है। C, A की माता है। D, C के पिता हैं। B का D से क्या संबंध है?",
                options = listOf("पुत्री", "पोता / नाती (Grandson)", "भाई", "पिता"),
                correctIndex = 1,
                explanation = "C, B की माता है और D, C के पिता हैं, अतः B, D का नाती/पोता है।",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 5,
                question = "$cleanName: विषम युग्म का चयन कीजिए:",
                options = listOf("भारत : नई दिल्ली", "जापान : टोक्यो", "ऑस्ट्रेलिया : सिडनी", "रूस : मॉस्को"),
                correctIndex = 2,
                explanation = "ऑस्ट्रेलिया की आधिकारिक राजधानी 'कैनबरा' है, सिडनी नहीं। बाकी सभी देश और उनकी राजधानियां हैं।",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 6,
                question = "$cleanName: 40 विद्यार्थियों की कक्षा में सुमित का स्थान ऊपर से 12वां है। नीचे से उसका स्थान क्या होगा?",
                options = listOf("28वां", "29वां", "30वां", "31वां"),
                correctIndex = 1,
                explanation = "नीचे से स्थान = कुल छात्र - ऊपर से स्थान + 1 = 40 - 12 + 1 = 29वां।",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 7,
                question = "$cleanName: कथन: सभी कुत्ते जानवर हैं। कुछ जानवर बिल्ली हैं। निष्कर्ष: I. कुछ कुत्ते बिल्ली हैं। II. कुछ जानवर कुत्ते हैं।",
                options = listOf("केवल निष्कर्ष I सही है", "केवल निष्कर्ष II सही है", "दोनों निष्कर्ष सही हैं", "कोई निष्कर्ष सही नहीं है"),
                correctIndex = 1,
                explanation = "क्योंकि सभी कुत्ते जानवर हैं, इसलिए कुछ जानवर निश्चित रूप से कुत्ते हैं (निष्कर्ष II सही है)।",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 8,
                question = "$cleanName: घड़ी में 4:30 बजने पर दोनों सुइयों के बीच कितने डिग्री का कोण बनेगा?",
                options = listOf("40°", "45°", "50°", "60°"),
                correctIndex = 1,
                explanation = "कोण = |30 * H - (11/2) * M| = |30 * 4 - (11/2) * 30| = |120 - 165| = 45°.",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 9,
                question = "$cleanName: यदि '+' का अर्थ '÷', '-' का अर्थ '+', '*' का अर्थ '-' और '÷' का अर्थ '*' है, तो 24 + 4 - 6 * 2 ÷ 3 = ?",
                options = listOf("6", "8", "12", "16"),
                correctIndex = 0,
                explanation = "चिह्न बदलने पर: 24 ÷ 4 + 6 - 2 * 3 = 6 + 6 - 6 = 6.",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 10,
                question = "$cleanName: प्रश्न चिह्न (?) के स्थान पर कौन सी संख्या आएगी? 8 : 64 :: 11 : ?",
                options = listOf("110", "121", "132", "144"),
                correctIndex = 1,
                explanation = "8 का वर्ग 64 है, उसी प्रकार 11 का वर्ग 121 (11^2 = 121) होगा।",
                subject = subjectName
            )
        )
    } else {
        // General Knowledge / Science / Computer / Special subjects
        questions.add(
            QuizQuestion(
                id = 1,
                question = "$cleanName: इस अध्याय से संबंधित निम्नलिखित में से कौन सा तथ्य ऐतिहासिक/वैज्ञानिक दृष्टि से सर्वाधिक प्रामाणिक है?",
                options = listOf("यह भारतीय संविधान व व्यवस्था का अभिन्न अंग है", "यह सीधे आर्थिक नीतियों से जुड़ा है", "यह मध्यकालीन इतिहास की प्रमुख घटना है", "यह आधुनिक वैज्ञानिक नवाचार है"),
                correctIndex = 0,
                explanation = "$cleanName प्रतियोगी परीक्षाओं के लिए अत्यंत महत्वपूर्ण विषय है जिसकी विस्तृत समझ आवश्यक है।",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 2,
                question = "$cleanName: भारतीय संविधान के किस अनुच्छेद में मौलिक अधिकारों (Fundamental Rights) का वर्णन किया गया है?",
                options = listOf("अनुच्छेद 12 से 35", "अनुच्छेद 36 से 51", "अनुच्छेद 5 से 11", "अनुच्छेद 51A"),
                correctIndex = 0,
                explanation = "भारतीय संविधान के भाग III में अनुच्छेद 12 से 35 तक 6 मौलिक अधिकारों का विस्तृत प्रावधान है।",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 3,
                question = "$cleanName: भारत की सबसे पुरानी पर्वत श्रृंखला निम्नलिखित में से कौन सी है?",
                options = listOf("हिमालय", "अरावली", "पश्चिमी घाट", "सतपुड़ा"),
                correctIndex = 1,
                explanation = "अरावली पर्वतमाला भारत की ही नहीं बल्कि विश्व की सबसे प्राचीन वलित पर्वत श्रृंखलाओं में से एक है।",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 4,
                question = "$cleanName: प्रकाश वर्ष (Light Year) निम्नलिखित में से किस भौतिक राशि की इकाई है?",
                options = listOf("समय", "दूरी (खगोलीय दूरी)", "प्रकाश की तीव्रता", "द्रव्यमान"),
                correctIndex = 1,
                explanation = "प्रकाश वर्ष खगोलीय दूरी की इकाई है जो प्रकाश द्वारा निर्वात में 1 वर्ष में तय की गई दूरी के बराबर होती है।",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 5,
                question = "$cleanName: भारतीय रिजर्व बैंक (RBI) की स्थापना किस वर्ष हुई थी?",
                options = listOf("1935", "1947", "1950", "1921"),
                correctIndex = 0,
                explanation = "भारतीय रिज़र्व बैंक की स्थापना 1 अप्रैल 1935 को हिल्टन यंग कमीशन की सिफारिश पर की गई थी।",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 6,
                question = "$cleanName: 'नीति आयोग' (NITI Aayog) के पदेन अध्यक्ष कौन होते हैं?",
                options = listOf("राष्ट्रपति", "प्रधानमंत्री", "वित्त मंत्री", "गृह मंत्री"),
                correctIndex = 1,
                explanation = "भारत के प्रधानमंत्री नीति आयोग (National Institution for Transforming India) के पदेन अध्यक्ष होते हैं।",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 7,
                question = "$cleanName: मानव शरीर में रक्तचाप (Blood Pressure) को नियंत्रित करने वाली ग्रंथि कौन सी है?",
                options = listOf("अधिवृक्क (Adrenal) ग्रंथि", "थायरॉयड ग्रंथि", "पीयूष (Pituitary) ग्रंथि", "अग्न्याशय"),
                correctIndex = 0,
                explanation = "अधिवृक्क (Adrenal) ग्रंथि एल्डोस्टेरोन और एड्रेनालाईन हार्मोन स्रावित कर रक्तचाप को नियंत्रित करती है।",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 8,
                question = "$cleanName: भारतीय राष्ट्रीय कांग्रेस (INC) की स्थापना 1885 में किसके द्वारा की गई थी?",
                options = listOf("ए. ओ. ह्यूम (A.O. Hume)", "व्योमेश चंद्र बनर्जी", "दादाभाई नौरोजी", "गोपाल कृष्ण गोखले"),
                correctIndex = 0,
                explanation = "भारतीय राष्ट्रीय कांग्रेस की स्थापना दिसंबर 1885 में बॉम्बे में रिटायर्ड आईसीएस अधिकारी एलन ऑक्टेवियन ह्यूम द्वारा की गई थी।",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 9,
                question = "$cleanName: सौरमंडल का सबसे बड़ा ग्रह निम्नलिखित में से कौन सा है?",
                options = listOf("बृहस्पति (Jupiter)", "शनि (Saturn)", "पृथ्वी (Earth)", "वरुण (Neptune)"),
                correctIndex = 0,
                explanation = "बृहस्पति (Jupiter) सौरमंडल का सबसे बड़ा और सबसे भारी ग्रह है।",
                subject = subjectName
            )
        )
        questions.add(
            QuizQuestion(
                id = 10,
                question = "$cleanName: कंप्यूटर में RAM (Random Access Memory) किस प्रकार की मेमोरी होती है?",
                options = listOf("अस्थिर (Volatile) मेमोरी", "स्थिर (Non-Volatile) मेमोरी", "सेकेंडरी मेमोरी", "ऑप्टिकल मेमोरी"),
                correctIndex = 0,
                explanation = "RAM एक वोलेटाइल (अस्थिर) प्राथमिक मेमोरी है, जिसमें कंप्यूटर बंद होने पर डेटा नष्ट हो जाता है।",
                subject = subjectName
            )
        )
    }

    return questions
}

/**
 * Dialog for Admin to edit questions, add more test questions, or change chapter properties
 */
@Composable
fun AdminEditChapterQuestionsDialog(
    subExam: SubExam,
    chapter: ChapterItem,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var chapterTitle by remember { mutableStateOf(CustomChapterRepository.cleanBaseChapterTitle(chapter.title)) }
    var chapterDesc by remember { mutableStateOf(chapter.description) }
    var durationMinutes by remember { mutableIntStateOf(chapter.durationMinutes) }
    var difficulty by remember { mutableStateOf(chapter.difficulty) }
    var selectedYear by remember { mutableStateOf(CustomChapterRepository.extractYear(chapter.title, chapter.description)) }

    val currentQuestions = remember { mutableStateListOf<QuizQuestion>().apply { addAll(chapter.questions) } }

    var addQuestionMode by remember { mutableIntStateOf(0) } // 0: Manual Input, 1: AI Auto Generate
    var newQText by remember { mutableStateOf("") }
    var newOpt1 by remember { mutableStateOf("") }
    var newOpt2 by remember { mutableStateOf("") }
    var newOpt3 by remember { mutableStateOf("") }
    var newOpt4 by remember { mutableStateOf("") }
    var newCorrectIdx by remember { mutableIntStateOf(0) }
    var newExplanation by remember { mutableStateOf("") }

    var aiGenCount by remember { mutableIntStateOf(5) }
    var isAiGeneratingQuestions by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var editingQuestionIndexInChapter by remember { mutableStateOf<Int?>(null) }
    var inlineEditingIndex by remember { mutableStateOf<Int?>(null) }

    editingQuestionIndexInChapter?.let { editIdx ->
        if (editIdx in currentQuestions.indices) {
            val qToEdit = currentQuestions[editIdx]
            com.example.ui.screens.QuestionEditDialog(
                question = qToEdit,
                index = editIdx,
                onDismiss = { editingQuestionIndexInChapter = null },
                onSave = { updatedQ ->
                    currentQuestions[editIdx] = updatedQ
                    editingQuestionIndexInChapter = null
                    Toast.makeText(context, "✅ प्रश्न ${editIdx + 1} सुरक्षित हो गया!", Toast.LENGTH_SHORT).show()
                },
                onDelete = {
                    currentQuestions.removeAt(editIdx)
                    editingQuestionIndexInChapter = null
                    Toast.makeText(context, "🗑️ प्रश्न ${editIdx + 1} हटा दिया गया", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "अध्याय टेस्ट एडिटर (Edit Test)",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${subExam.name} • ${currentQuestions.size} प्रश्न उपलब्ध",
                                fontSize = 11.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Chapter Title & Desc
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RealtimeOutlinedTextField(
                                    value = chapterTitle,
                                    onValueChange = { chapterTitle = it },
                                    label = { Text("अध्याय / बटन का शीर्षक (Title)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                RealtimeOutlinedTextField(
                                    value = chapterDesc,
                                    onValueChange = { chapterDesc = it },
                                    label = { Text("संक्षिप्त विवरण (Description)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("समय (मिनट):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf(15, 20, 30).forEach { mins ->
                                                val isSel = durationMinutes == mins
                                                Surface(
                                                    onClick = { durationMinutes = mins },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSel) Color(0xFF2563EB) else Color(0xFFEFF6FF),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(vertical = 5.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "${mins}m",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSel) Color.White else Color(0xFF2563EB)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("कठिनाई:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf("सरल", "मध्यम", "कठिन").forEach { diff ->
                                                val isSel = difficulty == diff
                                                Surface(
                                                    onClick = { difficulty = diff },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isSel) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(vertical = 5.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = diff,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSel) Color.White else Color(0xFF475569)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Column {
                                    Text("साल (Year):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020").forEach { yr ->
                                            val isSel = selectedYear == yr
                                            Surface(
                                                onClick = { selectedYear = yr },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSel) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) Color(0xFF0F172A) else Color(0xFFE2E8F0)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = yr,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (isSel) Color.White else Color(0xFF334155)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add More Questions Section
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "➕ इस टेस्ट में नए प्रश्न जोड़ें:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E3A8A)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(
                                            onClick = { addQuestionMode = 0 },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (addQuestionMode == 0) Color(0xFF2563EB) else Color.White
                                        ) {
                                            Text(
                                                text = "खुद टाइप करें",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (addQuestionMode == 0) Color.White else Color(0xFF2563EB),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                        Surface(
                                            onClick = { addQuestionMode = 1 },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (addQuestionMode == 1) Color(0xFF2563EB) else Color.White
                                        ) {
                                            Text(
                                                text = "🤖 AI ऑटो-जनरेट",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (addQuestionMode == 1) Color.White else Color(0xFF2563EB),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                if (addQuestionMode == 0) {
                                    // Manual question form with real-time live typing preview
                                    RealtimeOutlinedTextField(
                                        value = newQText,
                                        onValueChange = { newQText = it },
                                        placeholder = { Text("प्रश्न का टेक्स्ट दर्ज करें... (अंडरलाइन हेतु _शब्द_ लिखें)") },
                                        shape = RoundedCornerShape(8.dp),
                                        minLines = 2,
                                        maxLines = 5,
                                        trailingIcon = {
                                            if (newQText.isNotEmpty()) {
                                                IconButton(onClick = { newQText = "" }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedBorderColor = Color(0xFF2563EB),
                                            unfocusedBorderColor = Color(0xFFCBD5E1)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Formatting Helper Buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("त्वरित फॉर्मेटिंग:", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                        Surface(
                                            onClick = { newQText = newQText.ifEmpty { "" } + " _शब्द_ " },
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFEEF2FF),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC7D2FE))
                                        ) {
                                            Text("+ _अंडरलाइन_", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.5.sp, color = Color(0xFF4338CA), fontWeight = FontWeight.Bold)
                                        }
                                        Surface(
                                            onClick = { newQText = newQText.ifEmpty { "" } + " **शब्द** " },
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFF0FDF4),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                                        ) {
                                            Text("+ **बोल्ड**", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.5.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Real-time live formatted question preview as user types
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFEFF6FF),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("👁️ टाइप करते समय लाइव टेक्स्ट:", fontSize = 10.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                                                if (newQText.contains("_") || newQText.contains("*")) {
                                                    Text("✨ फॉर्मेटिंग सक्रिय", fontSize = 9.5.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (newQText.isNotBlank()) TextFormatHelper.parseMarkdown(newQText) else androidx.compose.ui.text.AnnotatedString("यहाँ लाइव प्रश्न दिखेगा..."),
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (newQText.isNotBlank()) Color(0xFF0F172A) else Color(0xFF94A3B8)
                                            )
                                        }
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("विकल्प व सही उत्तर (रेडियो बटन से सही विकल्प चुनें):", fontSize = 11.sp, color = Color(0xFF334155), fontWeight = FontWeight.SemiBold)

                                        listOf(
                                            Triple(0, newOpt1, { t: String -> newOpt1 = t }),
                                            Triple(1, newOpt2, { t: String -> newOpt2 = t }),
                                            Triple(2, newOpt3, { t: String -> newOpt3 = t }),
                                            Triple(3, newOpt4, { t: String -> newOpt4 = t })
                                        ).forEach { (idx, value, setter) ->
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    RadioButton(
                                                        selected = newCorrectIdx == idx,
                                                        onClick = { newCorrectIdx = idx },
                                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF16A34A))
                                                    )
                                                    RealtimeOutlinedTextField(
                                                        value = value,
                                                        onValueChange = setter,
                                                        placeholder = { Text("विकल्प ${'A' + idx}") },
                                                        singleLine = true,
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedContainerColor = Color.White,
                                                            unfocusedContainerColor = Color.White,
                                                            focusedBorderColor = if (newCorrectIdx == idx) Color(0xFF16A34A) else Color(0xFF2563EB),
                                                            unfocusedBorderColor = Color(0xFFCBD5E1)
                                                        ),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                if (value.isNotBlank()) {
                                                    Row(
                                                        modifier = Modifier.padding(start = 44.dp, top = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("लाइव: ", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                                        Text(
                                                            text = TextFormatHelper.parseMarkdown(value),
                                                            fontSize = 11.5.sp,
                                                            color = if (newCorrectIdx == idx) Color(0xFF166534) else Color(0xFF334155),
                                                            fontWeight = if (newCorrectIdx == idx) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    RealtimeOutlinedTextField(
                                        value = newExplanation,
                                        onValueChange = { newExplanation = it },
                                        placeholder = { Text("विस्तृत व्याख्या / हल (वैकल्पिक)") },
                                        shape = RoundedCornerShape(8.dp),
                                        minLines = 2,
                                        maxLines = 4,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (newExplanation.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEFCE8),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEF08A)),
                                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Text("💡 लाइव व्याख्या:", fontSize = 9.5.sp, color = Color(0xFF854D0E), fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = TextFormatHelper.parseMarkdown(newExplanation),
                                                    fontSize = 11.5.sp,
                                                    color = Color(0xFF713F12)
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (newQText.trim().isBlank()) {
                                                Toast.makeText(context, "कृपया प्रश्न दर्ज करें!", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val opts = listOf(
                                                newOpt1.trim().ifEmpty { "विकल्प A" },
                                                newOpt2.trim().ifEmpty { "विकल्प B" },
                                                newOpt3.trim().ifEmpty { "विकल्प C" },
                                                newOpt4.trim().ifEmpty { "विकल्प D" }
                                            )
                                            currentQuestions.add(
                                                QuizQuestion(
                                                    id = currentQuestions.size + 1,
                                                    question = newQText.trim(),
                                                    options = opts,
                                                    correctIndex = newCorrectIdx,
                                                    explanation = newExplanation.trim().ifEmpty { "सही उत्तर: ${opts[newCorrectIdx]}" },
                                                    subject = chapter.subjectTitle
                                                )
                                            )
                                            newQText = ""
                                            newOpt1 = ""
                                            newOpt2 = ""
                                            newOpt3 = ""
                                            newOpt4 = ""
                                            newCorrectIdx = 0
                                            newExplanation = ""
                                            Toast.makeText(context, "नया प्रश्न सफलतापूर्वक जोड़ा गया!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ यह प्रश्न लिस्ट में जोड़ें", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    // AI Auto Generate More Questions
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "जोड़ने वाले प्रश्नों की संख्या:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E3A8A)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf(5, 10, 15).forEach { cnt ->
                                                Surface(
                                                    onClick = { aiGenCount = cnt },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (aiGenCount == cnt) Color(0xFF2563EB) else Color.White
                                                ) {
                                                    Text(
                                                        text = "$cnt प्रश्न",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (aiGenCount == cnt) Color.White else Color(0xFF334155),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            isAiGeneratingQuestions = true
                                            com.example.service.AiBackgroundExecutionManager.startChapterQuestionsGeneration(
                                                context = context,
                                                chapterTitle = chapterTitle,
                                                subjectTitle = chapter.subjectTitle,
                                                count = aiGenCount,
                                                onCompleted = { result ->
                                                    isAiGeneratingQuestions = false
                                                    if (result.isSuccess && !result.getOrNull().isNullOrEmpty()) {
                                                        val generated = result.getOrNull()!!
                                                        val startId = currentQuestions.size + 1
                                                        val toAdd = generated.mapIndexed { idx, q ->
                                                            q.copy(id = startId + idx)
                                                        }
                                                        currentQuestions.addAll(toAdd)
                                                        Toast.makeText(context, "✅ Google AI द्वारा ${toAdd.size} नए प्रश्न जोड़े गए!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        val err = result.exceptionOrNull()?.message ?: "Unknown error"
                                                        Toast.makeText(context, "⚠️ AI एरर: $err", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            )
                                        },
                                        enabled = !isAiGeneratingQuestions,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (isAiGeneratingQuestions) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Google AI प्रश्न तैयार कर रहा है...", fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("🤖 AI से $aiGenCount प्रश्न ऑटो-जनरेट करके जोड़ें", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Existing Questions Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "वर्तमान प्रश्न सूची (${currentQuestions.size} प्रश्न):",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    if (currentQuestions.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("इस अध्याय में अभी कोई प्रश्न नहीं है। ऊपर से जोड़ें।", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    } else {
                        itemsIndexed(currentQuestions) { idx, q ->
                            if (inlineEditingIndex == idx) {
                                ChapterQuestionInlineEditor(
                                    question = q,
                                    index = idx,
                                    onSave = { updatedQ ->
                                        currentQuestions[idx] = updatedQ
                                        inlineEditingIndex = null
                                        Toast.makeText(context, "✅ प्रश्न ${idx + 1} सुरक्षित हो गया!", Toast.LENGTH_SHORT).show()
                                    },
                                    onCancel = {
                                        inlineEditingIndex = null
                                    },
                                    onOpenFullDialog = {
                                        editingQuestionIndexInChapter = idx
                                    }
                                )
                            } else {
                                Surface(
                                    onClick = { inlineEditingIndex = idx },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = TextFormatHelper.parseMarkdown("Q${idx + 1}. ${q.question}"),
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { inlineEditingIndex = idx },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Question", tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        if (inlineEditingIndex == idx) inlineEditingIndex = null
                                                        currentQuestions.removeAt(idx)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        q.options.forEachIndexed { optIdx, optText ->
                                            val isCorrect = optIdx == q.correctIndex
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(vertical = 1.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isCorrect) Color(0xFF16A34A) else Color(0xFFCBD5E1)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isCorrect) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = TextFormatHelper.parseMarkdown("${'A' + optIdx}. $optText"),
                                                    fontSize = 11.sp,
                                                    color = if (isCorrect) Color(0xFF166534) else Color(0xFF475569),
                                                    fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("रद्द करें")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                val isCustom = chapter.id.startsWith("custom_chap_")
                                if (isCustom) {
                                    CustomChapterRepository.deleteChapter(
                                        subExamId = subExam.id,
                                        chapterId = chapter.id,
                                        subjectKey = chapter.subjectKey,
                                        chapterTitle = chapter.title,
                                        context = context
                                    )
                                }
                                
                                val createdSets = CustomChapterRepository.addChapterWithSets(
                                    subExamId = subExam.id,
                                    subjectKey = chapter.subjectKey,
                                    subjectTitle = chapter.subjectTitle,
                                    baseChapterTitle = chapterTitle.trim(),
                                    allQuestions = currentQuestions.toList(),
                                    durationPerSet = 10,
                                    difficulty = difficulty,
                                    year = selectedYear,
                                    context = context
                                )

                                createdSets.forEach { createdChap ->
                                    val newTestPaper = com.example.model.TestPaper(
                                        id = createdChap.id,
                                        title = "${subExam.name} - ${createdChap.title} ($selectedYear PYQ)",
                                        totalQuestions = createdChap.questions.size,
                                        durationMinutes = createdChap.durationMinutes,
                                        totalMarks = createdChap.totalMarks,
                                        difficulty = createdChap.difficulty,
                                        year = selectedYear,
                                        questions = createdChap.questions
                                    )
                                    com.example.model.DynamicTestRepository.addCustomTest(
                                        categoryId = subExam.id,
                                        subExamId = subExam.id,
                                        postType = "CONSTABLE",
                                        year = selectedYear,
                                        testType = "CHAPTER_WISE",
                                        testPaper = newTestPaper,
                                        context = context
                                    )
                                }
                                
                                // Purge old local cache and sync edited chapter questions to GitHub
                                try {
                                    val allKeysToPurge = mutableListOf(chapter.id, chapter.title, "${subExam.id}_${chapter.id}", "${subExam.id}_${chapter.title}")
                                    createdSets.forEach {
                                        allKeysToPurge.add(it.id)
                                        allKeysToPurge.add(it.title)
                                    }
                                    com.example.model.OfflineTestRepository.purgeAndRefreshOfflineCache(
                                        context = context,
                                        testKeys = allKeysToPurge,
                                        updatedQuestions = currentQuestions.toList()
                                    )
                                    com.example.service.OnlineTestSyncManager.publishAllToCloud(context)
                                    com.example.service.OnlineTestSyncManager.syncFromCloud(context, isUserTriggered = false)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                
                                isSaving = false
                                Toast.makeText(context, "✅ अध्याय '${chapterTitle.trim()}' सुरक्षित हुआ व GitHub पर अपडेट हो गया!", Toast.LENGTH_LONG).show()
                                onSaved()
                            }
                        },
                        enabled = !isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.8f)
                            .height(44.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("सेव हो रहा है...")
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("टेस्ट सेव करें (${currentQuestions.size} प्रश्न)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RealtimeOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(10.dp),
    colors: androidx.compose.material3.TextFieldColors = OutlinedTextFieldDefaults.colors(),
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle.Default
) {
    var textFieldValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(text = value)) }

    androidx.compose.runtime.LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = value)
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { 
            textFieldValue = it
            onValueChange(it.text)
        },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        trailingIcon = trailingIcon,
        shape = shape,
        colors = colors,
        textStyle = textStyle
    )
}

@Composable
fun ChapterQuestionInlineEditor(
    question: QuizQuestion,
    index: Int,
    onSave: (QuizQuestion) -> Unit,
    onCancel: () -> Unit,
    onOpenFullDialog: () -> Unit
) {
    var qText by remember(question) { mutableStateOf(question.question) }
    var opt0 by remember(question) { mutableStateOf(question.options.getOrElse(0) { "" }) }
    var opt1 by remember(question) { mutableStateOf(question.options.getOrElse(1) { "" }) }
    var opt2 by remember(question) { mutableStateOf(question.options.getOrElse(2) { "" }) }
    var opt3 by remember(question) { mutableStateOf(question.options.getOrElse(3) { "" }) }
    var correctIdx by remember(question) { mutableIntStateOf(question.correctIndex.coerceIn(0, 3)) }
    var explanation by remember(question) { mutableStateOf(question.explanation) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2563EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "प्रश्न ${index + 1} लाइव एडिट",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        onClick = onOpenFullDialog,
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEFF6FF),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.OpenInFull, contentDescription = "Full Dialog", modifier = Modifier.size(12.dp), tint = Color(0xFF2563EB))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("फुल स्क्रीन", fontSize = 10.5.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Question Text Field
            RealtimeOutlinedTextField(
                value = qText,
                onValueChange = { qText = it },
                label = { Text("प्रश्न (Question Text)", fontSize = 11.5.sp) },
                placeholder = { Text("प्रश्न टाइप करें... (अंडरलाइन हेतु _शब्द_ लिखें)", fontSize = 11.5.sp) },
                minLines = 2,
                maxLines = 5,
                trailingIcon = {
                    if (qText.isNotEmpty()) {
                        IconButton(onClick = { qText = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.5.sp, color = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth()
            )

            // Formatting Helper Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("त्वरित फॉर्मेटिंग:", fontSize = 10.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                Surface(
                    onClick = { qText = qText.ifEmpty { "" } + " _शब्द_ " },
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFEEF2FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC7D2FE))
                ) {
                    Text("+ _अंडरलाइन_", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.5.sp, color = Color(0xFF4338CA), fontWeight = FontWeight.Bold)
                }
                Surface(
                    onClick = { qText = qText.ifEmpty { "" } + " **शब्द** " },
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Text("+ **बोल्ड**", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.5.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                }
            }

            // Real-time live formatted preview for question as user types
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFEFF6FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("👁️ टाइप करते समय लाइव टेक्स्ट (Live Preview):", fontSize = 10.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.Bold)
                        if (qText.contains("_") || qText.contains("*")) {
                            Text("✨ फॉर्मेटिंग सक्रिय", fontSize = 9.5.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (qText.isNotBlank()) TextFormatHelper.parseMarkdown(qText) else androidx.compose.ui.text.AnnotatedString("यहाँ लाइव प्रश्न दिखेगा..."),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (qText.isNotBlank()) Color(0xFF0F172A) else Color(0xFF94A3B8)
                    )
                }
            }

            // 4 Options
            Text("विकल्प व सही उत्तर (रेडियो बटन से सही विकल्प चुनें):", fontSize = 11.sp, color = Color(0xFF334155), fontWeight = FontWeight.Bold)
            val optList = listOf(
                Triple(0, opt0, { s: String -> opt0 = s }),
                Triple(1, opt1, { s: String -> opt1 = s }),
                Triple(2, opt2, { s: String -> opt2 = s }),
                Triple(3, opt3, { s: String -> opt3 = s })
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                optList.forEach { (oIdx, oVal, setVal) ->
                    val isCorr = oIdx == correctIdx
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCorr) Color(0xFFDCFCE7) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCorr) Color(0xFF16A34A) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isCorr,
                                    onClick = { correctIdx = oIdx },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF16A34A))
                                )
                                Text(
                                    text = "${('A' + oIdx)}. ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorr) Color(0xFF166534) else Color(0xFF1E293B)
                                )
                                RealtimeOutlinedTextField(
                                    value = oVal,
                                    onValueChange = setVal,
                                    placeholder = { Text("विकल्प ${'A' + oIdx}", fontSize = 11.5.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(6.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = if (isCorr) Color(0xFF16A34A) else Color(0xFF2563EB),
                                        unfocusedBorderColor = Color(0xFFCBD5E1)
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color(0xFF0F172A)),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Real-time live formatted preview for option
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isCorr) Color(0xFFDCFCE7).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 36.dp, top = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("👁️ लाइव: ", fontSize = 9.5.sp, color = if (isCorr) Color(0xFF166534) else Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (oVal.isNotBlank()) TextFormatHelper.parseMarkdown(oVal) else androidx.compose.ui.text.AnnotatedString("विकल्प ${'A' + oIdx}..."),
                                        fontSize = 11.sp,
                                        color = if (isCorr) Color(0xFF166534) else if (oVal.isNotBlank()) Color(0xFF1E293B) else Color(0xFF94A3B8),
                                        fontWeight = if (isCorr) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Explanation Field
            RealtimeOutlinedTextField(
                value = explanation,
                onValueChange = { explanation = it },
                label = { Text("व्याख्या / हल (Explanation - Optional)", fontSize = 11.5.sp) },
                placeholder = { Text("व्याख्या दर्ज करें... (अंडरलाइन हेतु _शब्द_ लिखें)", fontSize = 11.5.sp) },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth()
            )

            // Real-time live explanation preview
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFEFCE8),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEF08A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    Text("💡 लाइव व्याख्या प्रीव्यू:", fontSize = 9.5.sp, color = Color(0xFF854D0E), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (explanation.isNotBlank()) TextFormatHelper.parseMarkdown(explanation) else androidx.compose.ui.text.AnnotatedString("व्याख्या का प्रीव्यू यहाँ दिखेगा..."),
                        fontSize = 11.sp,
                        color = if (explanation.isNotBlank()) Color(0xFF713F12) else Color(0xFF94A3B8)
                    )
                }
            }

            // Save & Cancel Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("रद्द करें", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val validOpts = listOf(
                            opt0.trim().ifEmpty { "विकल्प A" },
                            opt1.trim().ifEmpty { "विकल्प B" },
                            opt2.trim().ifEmpty { "विकल्प C" },
                            opt3.trim().ifEmpty { "विकल्प D" }
                        )
                        val updated = question.copy(
                            question = qText.trim(),
                            options = validOpts,
                            correctIndex = correctIdx,
                            explanation = explanation.trim().ifEmpty { "सही उत्तर: ${validOpts[correctIdx]}" }
                        )
                        onSave(updated)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("सुरक्षित करें (Save)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
