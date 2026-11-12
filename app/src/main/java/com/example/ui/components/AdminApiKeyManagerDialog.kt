package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.service.GeminiApiKeyPoolManager

/**
 * Dialog for Admin to inspect and configure multiple Gemini API Keys.
 * Enables sequential round-robin rotation with memory passing for high accuracy and 0 rate limit errors.
 */
@Composable
fun AdminApiKeyManagerDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val savedKeys = remember { GeminiApiKeyPoolManager.getSavedKeys(context) }

    var key1 by remember { mutableStateOf(savedKeys.getOrNull(0) ?: "") }
    var key2 by remember { mutableStateOf(savedKeys.getOrNull(1) ?: "") }
    var key3 by remember { mutableStateOf(savedKeys.getOrNull(2) ?: "") }
    var key4 by remember { mutableStateOf(savedKeys.getOrNull(3) ?: "") }
    var key5 by remember { mutableStateOf(savedKeys.getOrNull(4) ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Gemini API Keys रोटेशन पूल",
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "5 Gmail Keys = 5x कोटा + Zero 429 एरर",
                                fontSize = 11.5.sp,
                                color = Color(0xFF16A34A),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Info banner explaining sequential rotation with memory
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "जब AI PDF के पेजों को पढ़ेगा, तो वह 1-by-1 इन चाबियों को बारी-बारी से रोटेट करेगा (Key 1 ➔ Key 2 ➔ Key 3...)। पिछले प्रश्नों की मेमोरी सुरक्षित अगली Key को पास होती रहेगी!",
                            fontSize = 11.5.sp,
                            color = Color(0xFF166534),
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Key Input Fields Scrollable Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SingleKeyInputField(
                        index = 1,
                        label = "API Key 1 (डिफ़ॉल्ट / मुख्य खाता)",
                        value = key1,
                        onValueChange = { key1 = it }
                    )

                    SingleKeyInputField(
                        index = 2,
                        label = "API Key 2 (Gmail 2)",
                        value = key2,
                        onValueChange = { key2 = it }
                    )

                    SingleKeyInputField(
                        index = 3,
                        label = "API Key 3 (Gmail 3)",
                        value = key3,
                        onValueChange = { key3 = it }
                    )

                    SingleKeyInputField(
                        index = 4,
                        label = "API Key 4 (Gmail 4)",
                        value = key4,
                        onValueChange = { key4 = it }
                    )

                    SingleKeyInputField(
                        index = 5,
                        label = "API Key 5 (Gmail 5)",
                        value = key5,
                        onValueChange = { key5 = it }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val pre = GeminiApiKeyPoolManager.PRECONFIGURED_KEYS
                            key1 = pre.getOrNull(0) ?: ""
                            key2 = pre.getOrNull(1) ?: ""
                            key3 = pre.getOrNull(2) ?: ""
                            key4 = pre.getOrNull(3) ?: ""
                            key5 = pre.getOrNull(4) ?: ""
                            Toast.makeText(context, "एडमिन की सभी 5 सक्रिय Gemini Keys लोड हो गईं!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("डिफ़ॉल्ट Keys", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val activeList = listOf(key1, key2, key3, key4, key5)
                                .map { it.trim() }
                                .filter { it.isNotBlank() }

                            if (activeList.isEmpty()) {
                                Toast.makeText(context, "कृपया कम से कम 1 API Key दर्ज करें", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            GeminiApiKeyPoolManager.saveKeys(context, listOf(key1, key2, key3, key4, key5))
                            Toast.makeText(context, "🎉 ${activeList.size} API Keys रोटेशन पूल में सुरक्षित हो गईं!", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("सेटिंग्स सुरक्षित करें", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleKeyInputField(
    index: Int,
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .border(1.dp, if (value.isNotBlank()) Color(0xFF93C5FD) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (value.isNotBlank()) Color(0xFF2563EB) else Color(0xFF94A3B8)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = label,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            if (value.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = "सक्रिय (Active)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Gemini API Key दर्ज करें", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
            shape = RoundedCornerShape(8.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF2563EB),
                unfocusedBorderColor = Color(0xFFCBD5E1)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 12.sp,
                color = Color(0xFF0F172A)
            )
        )
    }
}
