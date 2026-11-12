package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.service.AiPromptRulesManager

/**
 * Admin dialog to customize and strictly enforce custom prompt instructions and rules for Google Gemini AI.
 * Any text typed here is dynamically injected into all AI PDF-to-test prompts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminAiPromptRulesDialog(
    initialRules: String = "",
    initialMode: String = "FULL_TEST",
    onDismiss: () -> Unit,
    onRulesSaved: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(if (initialMode.equals("CHAPTER_WISE", ignoreCase = true)) "CHAPTER_WISE" else "FULL_TEST") }
    
    var fullTestRulesText by remember {
        mutableStateOf(
            if (initialRules.isNotBlank() && selectedMode == "FULL_TEST") initialRules
            else AiPromptRulesManager.getFullTestRules(context)
        )
    }

    var chapterWiseRulesText by remember {
        mutableStateOf(
            if (initialRules.isNotBlank() && selectedMode == "CHAPTER_WISE") initialRules
            else AiPromptRulesManager.getChapterWiseRules(context)
        )
    }

    val currentRulesText = if (selectedMode == "CHAPTER_WISE") chapterWiseRulesText else fullTestRulesText

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFFEFF6FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "AI प्रॉम्प्ट सख्त नियम (Strict Rules)",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "फुल टेस्ट व चैप्टर वाइज के अलग-अलग नियम",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                // Mode Selector Tabs (Full Test vs Chapter Wise)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = { selectedMode = "FULL_TEST" },
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedMode == "FULL_TEST") Color(0xFF2563EB) else Color.Transparent,
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "📋 फुल टेस्ट नियम",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMode == "FULL_TEST") Color.White else Color(0xFF475569)
                            )
                        }
                    }

                    Surface(
                        onClick = { selectedMode = "CHAPTER_WISE" },
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedMode == "CHAPTER_WISE") Color(0xFF7C3AED) else Color.Transparent,
                        modifier = Modifier.weight(1f).height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "📚 चैप्टर-वाइज़ नियम",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMode == "CHAPTER_WISE") Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }

                // Info Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMode == "CHAPTER_WISE") Color(0xFFF5F3FF) else Color(0xFFF0FDF4)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (selectedMode == "CHAPTER_WISE") Color(0xFFDDD6FE) else Color(0xFFBBF7D0)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (selectedMode == "CHAPTER_WISE") Color(0xFF7C3AED) else Color(0xFF16A34A),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = if (selectedMode == "CHAPTER_WISE") {
                                "💡 [चैप्टर-वाइज़ मोड] AI केवल चयनित विषय/चैप्टर के प्रश्न निकालेगा और उन्हें 10-10 प्रश्नों के सेट्स में ऑटो-बैच करेगा। दोनों मोड्स के नियम पूरी तरह अलग रखे गए हैं।"
                            } else {
                                "💡 [फुल टेस्ट मोड] AI परीक्षा के ऑफिशियल ब्लूप्रिंट के अनुसार सभी 4 विषयों के प्रश्न क्रमबद्ध निकालेगा। दोनों मोड्स के नियम कभी आपस में मिक्स नहीं होंगे।"
                            },
                            fontSize = 12.sp,
                            color = if (selectedMode == "CHAPTER_WISE") Color(0xFF5B21B6) else Color(0xFF166534),
                            lineHeight = 17.sp
                        )
                    }
                }

                // Quick Preset Chips to append rules
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "⚡ त्वरित नियम जोड़ें (Quick Suggestions):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = if (selectedMode == "CHAPTER_WISE") {
                            listOf(
                                "🎯 सिर्फ इस चैप्टर के प्रश्न लें" to "दिए गए PDF में से सिर्फ और सिर्फ चयनित अध्याय के प्रश्न निकालें, अन्य टॉपिक छोड़ दें।",
                                "📦 10-10 प्रश्न सेट्स" to "प्रश्नों को 10-10 के क्रमिक सेट्स (Set 1, Set 2...) में व्यवस्थित करें।",
                                "🚫 नंबर विकल्प डायग्राम में न लें" to "जिन विकल्पों में संख्या या टेक्स्ट हो, उन्हें डायग्राम इमेज में शामिल न करें।",
                                "✍️ 100% हूबहू हिंदी" to "प्रश्न और विकल्पों का मूल हिंदी टेक्स्ट अक्षरशः हूबहू रखें।",
                                "📖 2 पंक्तियों में व्याख्या" to "प्रत्येक प्रश्न की 1-2 पंक्तियों में स्पष्ट हिंदी व्याख्या अवश्य दें।"
                            )
                        } else {
                            listOf(
                                "🇮🇳 केवल शुद्ध हिंदी टेक्स्ट लें" to "द्विभाषी PDF में से केवल हिंदी का टेक्स्ट निकालें, अंग्रेजी की पंक्तियां पूरी तरह छोड़ दें।",
                                "✂️ स्लैश (/) के बाद वाला हिंदी विकल्प लें" to "सभी विकल्पों में से स्लैश (/) के बाद वाला केवल हिंदी भाग निकालें (उदा. 'A) Head / सिर' से सिर्फ 'सिर')।",
                                "🎯 Correct Answer से 100% सही उत्तर" to "प्रश्न के नीचे लिखे 'Correct Answer: X' (A->0, B->1, C->2, D->3) से 100% सटीक उत्तर चुनें।",
                                "🚫 विज्ञापन/वॉटरमार्क हटाएं" to "'SSC CGL LIVE CLASSES', 'testbook' आदि किसी भी विज्ञापन व लिंक को पूरी तरह छोड़ दें।",
                                "🎯 केवल आकृति विकल्प डायग्राम में लें" to "डायग्राम में सिर्फ वही विकल्प साथ में लेने हैं जिनमें आकृति/चित्र हो, नंबर वाले विकल्प अलग रखें।",
                                "📖 2 पंक्तियों में व्याख्या" to "प्रत्येक प्रश्न की 1-2 पंक्तियों में स्पष्ट हिंदी व्याख्या (Explanation) अनिवार्य रूप से दें।"
                            )
                        }

                        presets.forEach { (chipLabel, ruleToAppend) ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.clickable {
                                    if (selectedMode == "CHAPTER_WISE") {
                                        val current = chapterWiseRulesText.trim()
                                        if (!current.contains(ruleToAppend)) {
                                            chapterWiseRulesText = if (current.isBlank()) ruleToAppend
                                            else "$current\n• $ruleToAppend"
                                        }
                                    } else {
                                        val current = fullTestRulesText.trim()
                                        if (!current.contains(ruleToAppend)) {
                                            fullTestRulesText = if (current.isBlank()) ruleToAppend
                                            else "$current\n• $ruleToAppend"
                                        }
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (selectedMode == "CHAPTER_WISE") Color(0xFF7C3AED) else Color(0xFF2563EB),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = chipLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }
                    }
                }

                // Text Input Area with High Contrast & Crystal-Clear Visibility
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedMode == "CHAPTER_WISE") "अध्याय-वार AI नियम टाइप करें:" else "फुल टेस्ट AI नियम टाइप करें:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "${currentRulesText.length} वर्ण | ${currentRulesText.lines().size} लाइन",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    OutlinedTextField(
                        value = if (selectedMode == "CHAPTER_WISE") chapterWiseRulesText else fullTestRulesText,
                        onValueChange = {
                            if (selectedMode == "CHAPTER_WISE") chapterWiseRulesText = it
                            else fullTestRulesText = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 300.dp),
                        placeholder = {
                            Text(
                                text = "उदा. 1. डायग्राम में सिर्फ वही विकल्प साथ में लेने हैं जिनमें आकृति हो...\n2. नंबर वाले विकल्प डायग्राम में न लें...",
                                fontSize = 13.5.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 19.sp
                            )
                        },
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A),
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedBorderColor = if (selectedMode == "CHAPTER_WISE") Color(0xFF7C3AED) else Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = if (selectedMode == "CHAPTER_WISE") Color(0xFF7C3AED) else Color(0xFF2563EB)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Live Preview of how AI will read it
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "👀 [${if (selectedMode == "CHAPTER_WISE") "Chapter Wise" else "Full Test"}] AI प्रॉम्प्ट इंजेक्शन प्रीव्यू:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = if (currentRulesText.isNotBlank()) currentRulesText else "(कोई नियम दर्ज नहीं)",
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 17.sp
                        )
                    }
                }

                // Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            AiPromptRulesManager.saveFullTestRules(context, fullTestRulesText)
                            AiPromptRulesManager.saveChapterWiseRules(context, chapterWiseRulesText)
                            Toast.makeText(context, "✅ दोनों मोड्स के AI नियम सफलतापूर्वक सेव हो गए!", Toast.LENGTH_SHORT).show()
                            onRulesSaved(currentRulesText)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedMode == "CHAPTER_WISE") Color(0xFF7C3AED) else Color(0xFF2563EB)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "💾 नियम सेव करें (Save AI Rules)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (selectedMode == "CHAPTER_WISE") {
                                    chapterWiseRulesText = AiPromptRulesManager.resetToDefault(context, "CHAPTER_WISE")
                                } else {
                                    fullTestRulesText = AiPromptRulesManager.resetToDefault(context, "FULL_TEST")
                                }
                                Toast.makeText(context, "🔄 इस मोड के डिफ़ॉल्ट नियम बहाल किए गए", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B)),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "डिफ़ॉल्ट रीसेट",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "रद्द करें",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
