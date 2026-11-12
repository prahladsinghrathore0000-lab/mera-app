package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CustomChapterRepository
import com.example.model.DynamicTestRepository
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DiagnosticReport(
    val totalCustomTests: Int,
    val delhiPoliceTestsCount: Int,
    val fullTestsCount: Int,
    val subjectWiseTestsCount: Int,
    val invalidSetsOnFullOrSubjectCount: Int,
    val totalCustomChapters: Int,
    val corruptOrLeakedTestsCount: Int,
    val issuesList: List<String>,
    val cacheSizeKb: Long,
    val isHealthy: Boolean
)

@Composable
fun AppHealthDiagnosticsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(true) }
    var report by remember { mutableStateOf<DiagnosticReport?>(null) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    fun runDiagnosticScan() {
        isScanning = true
        actionMessage = null
        scope.launch(Dispatchers.IO) {
            try {
                // Ensure repos are initialized
                DynamicTestRepository.init(context)
                CustomChapterRepository.init(context)

                val allTests = DynamicTestRepository.getAllCustomTests()
                val totalTests = allTests.size

                var delhiPoliceCount = 0
                var fullCount = 0
                var subjectWiseCount = 0
                var invalidSetsCount = 0
                var leakedCount = 0
                val issues = mutableListOf<String>()

                for (entry in allTests) {
                    val title = entry.testPaper.title ?: ""
                    val subExam = entry.subExamId ?: ""
                    val type = entry.testType ?: ""

                    if (subExam.contains("delhi_police", ignoreCase = true) || title.contains("Delhi Police", ignoreCase = true)) {
                        delhiPoliceCount++
                    }

                    val isFullOrSub = title.contains("Shift", ignoreCase = true) ||
                            title.contains("Full Test", ignoreCase = true) ||
                            title.contains("मॉक टेस्ट", ignoreCase = true) ||
                            title.contains("Subject Wise", ignoreCase = true) ||
                            title.contains("विषयवार", ignoreCase = true) ||
                            type.equals("FULL_TEST", true) ||
                            type.equals("SUBJECT_WISE", true)

                    val isSet = title.contains(" - Set ", ignoreCase = true) ||
                            title.contains("Set ", ignoreCase = true) ||
                            title.contains("सेट ", ignoreCase = true)

                    if (isFullOrSub) {
                        if (type.equals("SUBJECT_WISE", ignoreCase = true) || title.contains("Subject Wise", ignoreCase = true) || title.contains("विषयवार", ignoreCase = true)) {
                            subjectWiseCount++
                        } else {
                            fullCount++
                        }
                        if (isSet) {
                            invalidSetsCount++
                            issues.emplaceIfUnique("Invalid Set created on Full/Subject test: '$title' (Sets are strictly for chapterwise only)")
                        }
                    } else {
                        if (type.equals("FULL_TEST", true) || type.equals("SUBJECT_WISE", true)) {
                            if (type.equals("SUBJECT_WISE", true)) subjectWiseCount++ else fullCount++
                        }
                    }

                    // Check for potential cross-contamination / issues
                    if (subExam.isBlank()) {
                        leakedCount++
                        issues.emplaceIfUnique("Unassigned SubExam ID for test: '$title'")
                    }

                    if (entry.testPaper.questions.isNullOrEmpty()) {
                        leakedCount++
                        issues.emplaceIfUnique("Test has 0 questions: '$title'")
                    }
                }

                val chaptersList = CustomChapterRepository.customChapters.toList()
                val customChaptersCount = chaptersList.size
                for (chap in chaptersList) {
                    val chapTitle = chap.title ?: ""
                    if (chap.questions.isNullOrEmpty()) {
                        leakedCount++
                        issues.emplaceIfUnique("Chapter-wise test has 0 questions: '$chapTitle'")
                    }
                }
                
                // Approximate cache size from files in filesDir
                val filesDir = context.filesDir
                var totalBytes = 0L
                filesDir.listFiles()?.forEach { file ->
                    if (file.name.contains("test") || file.name.contains("chapter") || file.name.contains("json")) {
                        totalBytes += file.length()
                    }
                }
                val cacheKb = totalBytes / 1024

                val healthy = leakedCount == 0 && invalidSetsCount == 0 && issues.isEmpty()

                val generatedReport = DiagnosticReport(
                    totalCustomTests = totalTests,
                    delhiPoliceTestsCount = delhiPoliceCount,
                    fullTestsCount = fullCount,
                    subjectWiseTestsCount = subjectWiseCount,
                    invalidSetsOnFullOrSubjectCount = invalidSetsCount,
                    totalCustomChapters = customChaptersCount,
                    corruptOrLeakedTestsCount = leakedCount + invalidSetsCount,
                    issuesList = issues,
                    cacheSizeKb = cacheKb,
                    isHealthy = healthy
                )

                withContext(Dispatchers.Main) {
                    report = generatedReport
                    isScanning = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isScanning = false
                    actionMessage = "Scan Error: ${e.localizedMessage}"
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        runDiagnosticScan()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2563EB)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "🔍 App Health & Diagnostics",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "सेल्फ-डायग्नोसिस और डेटाबेस हेल्थ चेक",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp, max = 480.dp)
            ) {
                if (isScanning) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF2563EB))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "डेटाबेस और रिपॉजिटरी की गहरी जाँच हो रही है...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                } else {
                    val rep = report
                    if (rep != null) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Overall Health Status Banner
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (rep.isHealthy) Color(0xFFDCFCE7) else Color(0xFFFEF2F2)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (rep.isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (rep.isHealthy) Color(0xFF16A34A) else Color(0xFFDC2626),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (rep.isHealthy) "सब कुछ बिल्कुल सही है (System Healthy)" else "कुछ समस्याएँ पाई गईं (Issues Detected)",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (rep.isHealthy) Color(0xFF166534) else Color(0xFF991B1B)
                                            )
                                            Text(
                                                text = if (rep.isHealthy) "कोई करप्शन या लीक नहीं मिला।" else "${rep.corruptOrLeakedTestsCount} समस्याएँ मिली हैं जिन्हें ठीक किया जा सकता है।",
                                                fontSize = 11.sp,
                                                color = if (rep.isHealthy) Color(0xFF15803D) else Color(0xFFB91C1C)
                                            )
                                        }
                                    }
                                }
                            }

                            // Copy Full Report & Warnings Button
                            item {
                                val clipboardManager = LocalClipboardManager.current
                                OutlinedButton(
                                    onClick = {
                                        val reportText = buildString {
                                            appendLine("=== APP HEALTH & DIAGNOSTICS REPORT ===")
                                            appendLine("Status: ${if (rep.isHealthy) "Healthy" else "Issues Detected (${rep.corruptOrLeakedTestsCount})"}")
                                            appendLine("Total Custom Tests: ${rep.totalCustomTests}")
                                            appendLine("Delhi Police Tests: ${rep.delhiPoliceTestsCount}")
                                            appendLine("Full Tests: ${rep.fullTestsCount}")
                                            appendLine("Subject-Wise Tests: ${rep.subjectWiseTestsCount}")
                                            appendLine("Cache Size: ${rep.cacheSizeKb} KB")
                                            if (rep.issuesList.isNotEmpty()) {
                                                appendLine("\nDetected Warnings / Issues:")
                                                rep.issuesList.forEach { appendLine("• $it") }
                                            } else {
                                                appendLine("\nNo warnings detected.")
                                            }
                                        }
                                        clipboardManager.setText(AnnotatedString(reportText))
                                        Toast.makeText(context, "पूरी डायग्नोस्टिक रिपोर्ट और वार्निंग्स क्लिपबोर्ड पर कॉपी हो गईं!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                                    border = BorderStroke(1.dp, Color(0xFF2563EB))
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("📋 पूरी रिपोर्ट और वार्निंग्स कॉपी करें", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Statistics Grid Card
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "📊 डेटा और टेस्ट काउंट्स (Data Statistics)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2563EB)
                                        )
                                        Divider(color = Color(0xFFF1F5F9))
                                        DiagnosticStatRow(label = "कुल कस्टम टेस्ट (Total Tests)", value = "${rep.totalCustomTests}")
                                        DiagnosticStatRow(label = "दिल्ली पुलिस टेस्ट्स (Delhi Police)", value = "${rep.delhiPoliceTestsCount}")
                                        DiagnosticStatRow(label = "फुल टेस्ट्स (Full Tests)", value = "${rep.fullTestsCount}")
                                        DiagnosticStatRow(label = "सब्जेक्ट-वाइज टेस्ट्स (Subject-Wise)", value = "${rep.subjectWiseTestsCount}")
                                         DiagnosticStatRow(label = "चैप्टर-वाइज टेस्ट्स (Chapter-Wise)", value = "${rep.totalCustomChapters}")
                                        DiagnosticStatRow(label = "लोकल डिस्क कैश साइज (Cache Size)", value = "${rep.cacheSizeKb} KB")
                                    }
                                }
                            }

                            // Issues List if any
                            if (rep.issuesList.isNotEmpty()) {
                                item {
                                    val clipboardManager = LocalClipboardManager.current
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "⚠️ पहचानी गई त्रुटियाँ (Detected Warnings)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309)
                                                )
                                                OutlinedButton(
                                                    onClick = {
                                                        val textToCopy = rep.issuesList.joinToString("\n") { "• $it" }
                                                        clipboardManager.setText(AnnotatedString(textToCopy))
                                                        Toast.makeText(context, "त्रुटियाँ क्लिपबोर्ड पर कॉपी हो गईं!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(12.dp),
                                                        tint = Color(0xFFB45309)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("कॉपी करें", fontSize = 10.sp, color = Color(0xFFB45309))
                                                }
                                            }
                                            rep.issuesList.forEach { issue ->
                                                Text(
                                                    text = "• $issue",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF92400E)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (actionMessage != null) {
                                item {
                                    Surface(
                                        color = Color(0xFFEFF6FF),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = actionMessage ?: "",
                                            fontSize = 12.sp,
                                            color = Color(0xFF1D4ED8),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                DynamicTestRepository.sanitizeExistingDynamicTests(context)
                                CustomChapterRepository.sanitizeExistingCustomChapters(context)
                                DynamicTestRepository.purgeCorruptedExamCrossContamination(context)
                                withContext(Dispatchers.Main) {
                                    actionMessage = "✅ डेटाबेस सफलतापूर्वक सैनिटाइज और ठीक कर दिया गया!"
                                    runDiagnosticScan()
                                    Toast.makeText(context, "डेटाबेस सैनिटाइज हो गया!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    actionMessage = "Error fixing: ${e.localizedMessage}"
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("डेटाबेस ठीक करें (Sanitize Database)", fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { runDiagnosticScan() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("री-स्कैन", fontSize = 12.sp)
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("बंद करें", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        },
        dismissButton = null
    )
}

@Composable
fun DiagnosticStatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.5.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

private fun <E> MutableList<E>.emplaceIfUnique(element: E) {
    if (!this.contains(element)) {
        this.add(element)
    }
}
