package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.service.GitHubConfig
import com.example.service.GitHubSyncService
import kotlinx.coroutines.launch

@Composable
fun GitHubConfigDialog(
    initialConfig: GitHubConfig,
    onDismiss: () -> Unit,
    onSaveConfig: (GitHubConfig) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val savedToken = remember { GitHubSyncService.getSavedConfig(context).token }
    var token by remember { mutableStateOf(initialConfig.token.ifBlank { savedToken }) }
    var owner by remember { mutableStateOf(initialConfig.owner.ifBlank { "prahladsinghrathore869025-alt" }) }
    var repo by remember { mutableStateOf(initialConfig.repo.ifBlank { "Prahlad" }) }
    var branch by remember { mutableStateOf(initialConfig.branch.ifBlank { "main" }) }
    var folderPath by remember { mutableStateOf(initialConfig.folderPath.ifBlank { "data.json" }) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSuccess by remember { mutableStateOf(false) }
    var showHelpGuide by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "GitHub Auto-Sync सेटिंग्स",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "AI टेस्ट पेपर्स को सीधे GitHub में ऑटो-सेव करें",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Quick Fill Banner & Help toggle
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "💡 क्विक सेटअप (Quick Setup)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E40AF)
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            if (token.isBlank()) {
                                                val savedToken = GitHubSyncService.getSavedConfig(context).token
                                                if (savedToken.isNotBlank()) token = savedToken
                                            }
                                            owner = GitHubSyncService.DEFAULT_OWNER
                                            repo = GitHubSyncService.DEFAULT_REPO
                                            branch = GitHubSyncService.DEFAULT_BRANCH
                                            folderPath = GitHubSyncService.DEFAULT_FOLDER
                                            Toast.makeText(context, "डिफ़ॉल्ट Repo जानकारी भर दी गई! (टोकन सुरक्षित है)", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1D4ED8))
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("डिफ़ॉल्ट Repo भरें", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    text = "नीचे दिए गए 5 डिब्बों (Fields) में सही जानकारी भरें ताकि आपकी बनाई गई टेस्ट फाइल्स बिना किसी रुकावट के GitHub रिपॉजिटरी में JSON के रूप में सेव हो सकें।",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 16.sp
                                )

                                Text(
                                    text = if (showHelpGuide) "▲ टोकन गाइड छुपाएं" else "▼ GitHub Token कैसे प्राप्त करें? गाइड देखें",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    // Help Guide section
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "📌 GitHub Personal Access Token (PAT) कैसे बनाएं?",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "1. GitHub.com पर लॉगिन करें ➔ ऊपर प्रोफाइल आइकन पर क्लिक कर Settings में जाएं।\n" +
                                            "2. नीचे Developer Settings ➔ Personal Access Tokens (Tokens Classic) चुनें।\n" +
                                            "3. 'Generate new token' पर क्लिक करें और 'repo' बॉक्स को टिक (चेक) करें।\n" +
                                            "4. जनरेट हुए टोकन (ghp_...) को कॉपी करके नीचे बॉक्स नंबर 1 में पेस्ट करें।",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF78350F),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // 1. TOKEN BOX
                    item {
                        ConfigInputField(
                            boxNumber = "1",
                            title = "GitHub Personal Access Token (टोकन)",
                            description = "GitHub API टोकन (ghp_... या github_pat_...)",
                            exampleHint = "उदाहरण: ghp_abc123xyz456...",
                            value = token,
                            onValueChange = { token = it },
                            icon = Icons.Default.Key,
                            isPassword = true
                        )
                    }

                    // 2. OWNER / USERNAME BOX
                    item {
                        ConfigInputField(
                            boxNumber = "2",
                            title = "GitHub Owner / Username (यूजरनेम)",
                            description = "आपका GitHub Username या Organization का नाम",
                            exampleHint = "उदाहरण: prahlad-singh या exam-portal-admin",
                            value = owner,
                            onValueChange = { owner = it },
                            icon = Icons.Default.Person
                        )
                    }

                    // 3. REPOSITORY NAME BOX
                    item {
                        ConfigInputField(
                            boxNumber = "3",
                            title = "Repository Name (रिपॉजिटरी का नाम)",
                            description = "जिस GitHub Repo में टेस्ट पेपर्स की JSON फाइलें सेव होंगी",
                            exampleHint = "उदाहरण: exam-question-bank या test-series-2026",
                            value = repo,
                            onValueChange = { repo = it },
                            icon = Icons.Default.Storage
                        )
                    }

                    // 4. BRANCH BOX
                    item {
                        ConfigInputField(
                            boxNumber = "4",
                            title = "Git Branch (ब्रांच का नाम)",
                            description = "डिफ़ॉल्ट ब्रांच जहाँ फाइल्स कमिट होंगी (आमतौर पर main)",
                            exampleHint = "उदाहरण: main या master",
                            value = branch,
                            onValueChange = { branch = it },
                            icon = Icons.Default.Code
                        )
                    }

                    // 5. FOLDER PATH BOX
                    item {
                        ConfigInputField(
                            boxNumber = "5",
                            title = "Folder Path (फ़ोल्डर का रास्ता)",
                            description = "रिपॉजिटरी के अंदर का सब-फ़ोल्डर जहाँ JSON फाइलें रखी जाएंगी",
                            exampleHint = "उदाहरण: tests या data/exams",
                            value = folderPath,
                            onValueChange = { folderPath = it },
                            icon = Icons.Default.Folder
                        )
                    }

                    // Test Connection Result feedback
                    if (testResult != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (testSuccess) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                border = BorderStroke(1.dp, if (testSuccess) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (testSuccess) Icons.Default.CheckCircle else Icons.Default.Close,
                                            contentDescription = null,
                                            tint = if (testSuccess) Color(0xFF16A34A) else Color(0xFFDC2626),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = if (testSuccess) "✅ कनेक्शन सफल (GitHub Connected)" else "❌ कनेक्शन विफल (Error)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (testSuccess) Color(0xFF166534) else Color(0xFF991B1B)
                                        )
                                    }
                                    Text(
                                        text = testResult!!,
                                        fontSize = 12.sp,
                                        color = if (testSuccess) Color(0xFF166534) else Color(0xFF991B1B),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Direct Cloud Sync Button
                    Button(
                        onClick = {
                            val effectiveToken = token.trim().ifBlank { savedToken }
                            val cleanConfig = GitHubConfig(
                                owner = owner.trim(),
                                repo = repo.trim(),
                                branch = branch.trim().ifBlank { "main" },
                                token = effectiveToken,
                                folderPath = folderPath.trim().ifBlank { "tests" }
                            )
                            onSaveConfig(cleanConfig)
                            GitHubSyncService.saveConfig(context, cleanConfig)
                            Toast.makeText(context, "GitHub से सभी टेस्ट लोड किए जा रहे हैं...", Toast.LENGTH_SHORT).show()
                            com.example.service.OnlineTestSyncManager.forceSyncNow(context) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📥 GitHub से सभी टेस्ट सिंक / डाउनलोड करें", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Restore Deleted Tests Button
                    OutlinedButton(
                        onClick = {
                            com.example.model.DynamicTestRepository.restoreAllDeletedTests(context)
                            com.example.service.OnlineTestSyncManager.forceSyncNow(context) { _, msg ->
                                Toast.makeText(context, "🔄 हटाए गए टेस्ट रीस्टोर कर दिए गए: $msg", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF166534)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🔄 गलती से हटाए गए टेस्ट वापस लाएं (Restore)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Test Connection Button
                        OutlinedButton(
                            onClick = {
                                if (owner.isBlank() || repo.isBlank()) {
                                    Toast.makeText(context, "कृपया यूज़रनेम और रेपो नाम भरें", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                coroutineScope.launch {
                                    isTestingConnection = true
                                    testResult = null
                                    val testConfig = GitHubConfig(
                                        owner = owner.trim(),
                                        repo = repo.trim(),
                                        branch = branch.trim().ifBlank { "main" },
                                        token = token.trim(),
                                        folderPath = folderPath.trim().ifBlank { "tests" }
                                    )
                                    val diagnosis = GitHubSyncService.diagnoseConnection(testConfig)
                                    isTestingConnection = false
                                    if (diagnosis.isSuccess) {
                                        testSuccess = true
                                        val sb = StringBuilder()
                                        sb.append("• रिपॉजिटरी: ${diagnosis.repoFullName}\n")
                                        if (diagnosis.authenticatedUser != null) {
                                            sb.append("• लॉगिन यूज़र: @${diagnosis.authenticatedUser}\n")
                                        }
                                        sb.append("• डिफ़ॉल्ट ब्रांच: ${diagnosis.defaultBranch}\n")
                                        if (diagnosis.discoveredJsonFiles.isNotEmpty()) {
                                            sb.append("• 📄 ${diagnosis.discoveredJsonFiles.size} JSON टेस्ट फाइलें मिलीं:\n")
                                            diagnosis.discoveredJsonFiles.take(5).forEach {
                                                sb.append("   - $it\n")
                                            }
                                            if (diagnosis.discoveredJsonFiles.size > 5) {
                                                sb.append("   ...और ${diagnosis.discoveredJsonFiles.size - 5} अन्य फाइलें")
                                            }
                                        } else {
                                            sb.append("• ⚠️ रिपॉजिटरी में अभी कोई .json फाइल नहीं मिली। (नीचे 'Save Settings' करके नया AI टेस्ट बनाएं व Commit करें)")
                                        }
                                        testResult = sb.toString()
                                    } else {
                                        testSuccess = false
                                        testResult = diagnosis.errorMessage ?: "कनेक्शन फेल हुआ"
                                    }
                                }
                            },
                            enabled = !isTestingConnection,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("जाँच...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("कनेक्शन टेस्ट", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Save Button
                        Button(
                            onClick = {
                                val effectiveToken = token.trim().ifBlank { savedToken }
                                val cleanConfig = GitHubConfig(
                                    owner = owner.trim(),
                                    repo = repo.trim(),
                                    branch = branch.trim().ifBlank { "main" },
                                    token = effectiveToken,
                                    folderPath = folderPath.trim().ifBlank { "tests" }
                                )
                                onSaveConfig(cleanConfig)
                                GitHubSyncService.saveConfig(context, cleanConfig)
                                com.example.service.OnlineTestSyncManager.forceSyncNow(context)
                                Toast.makeText(context, "🎉 GitHub सेटिंग्स सेव हो गईं और टेस्ट सिंक शुरू हो गया!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("सेटिंग्स सेव करें", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigInputField(
    boxNumber: String,
    title: String,
    description: String,
    exampleHint: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    isPassword: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = boxNumber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        text = exampleHint,
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Medium
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC),
                    cursorColor = Color(0xFF2563EB),
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
