package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.OfflineTestRepository
import com.example.model.UnlockedTestsRepository
import com.example.service.RewardedAdManager
import kotlinx.coroutines.delay

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
fun RewardAdUnlockDialog(
    testTitle: String,
    onDismiss: () -> Unit,
    onTestUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var isAdLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var failureCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        if (!RewardedAdManager.isAdReady) {
            isAdLoading = true
            RewardedAdManager.loadRewardedAd(
                context = context.applicationContext,
                onLoaded = { isAdLoading = false },
                onFailed = { 
                    isAdLoading = false
                    failureCount++
                }
            )
        }
    }

    val triggerUnlockSuccess = {
        // Permanently unlock test for lifetime and cache for offline play!
        UnlockedTestsRepository.unlockTest(context, testTitle)
        OfflineTestRepository.saveTestOffline(context, testTitle, null)
        onTestUnlocked()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .testTag("reward_ad_unlock_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Watch Ad",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Titles
                Text(
                    text = "टेस्ट अनलॉक करें",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "1 छोटा सा विज्ञापन देखकर हमेशा के लिए फ्री",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Test Details Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "चयनित टेस्ट",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = testTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Features List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureRow(icon = Icons.Default.CheckCircle, text = "सिर्फ 1 बार ऑनलाइन विज्ञापन देखना होगा")
                    FeatureRow(icon = Icons.Default.CardGiftcard, text = "टेस्ट हमेशा के लिए डिवाइस में सेव (Offline)")
                    FeatureRow(icon = Icons.Default.LockOpen, text = "री-अटेम्प्ट (Re-Attempt) पर कोई ऐड नहीं")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Error or Status message
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626),
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                if (isAdLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF2563EB)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "विज्ञापन लोड हो रहा है...",
                            fontSize = 13.sp,
                            color = Color(0xFF2563EB),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Action Buttons
                Button(
                    onClick = {
                        errorMessage = null
                        if (activity == null) {
                            errorMessage = "ऐप गतिविधि प्रारंभ नहीं हो सकी। कृपया पुनः प्रयास करें।"
                            return@Button
                        }

                        if (RewardedAdManager.isAdReady) {
                            RewardedAdManager.showRewardedAd(
                                activity = activity,
                                testTargetId = testTitle,
                                onRewardEarned = {
                                    triggerUnlockSuccess()
                                },
                                onAdDismissed = { },
                                onAdFailedToShow = {
                                    errorMessage = "विज्ञापन लोड हो रहा है... कृपया कुछ सेकंड बाद पुनः प्रयास करें।"
                                    isAdLoading = true
                                    RewardedAdManager.loadRewardedAd(
                                        context = activity.applicationContext,
                                        onLoaded = { isAdLoading = false },
                                        onFailed = { err ->
                                            isAdLoading = false
                                            errorMessage = "विज्ञापन लोड नहीं हो पाया: $err"
                                        }
                                    )
                                }
                            )
                        } else {
                            isAdLoading = true
                            RewardedAdManager.loadRewardedAd(
                                context = activity.applicationContext,
                                onLoaded = {
                                    isAdLoading = false
                                    errorMessage = null
                                    RewardedAdManager.showRewardedAd(
                                        activity = activity,
                                        testTargetId = testTitle,
                                        onRewardEarned = {
                                            triggerUnlockSuccess()
                                        },
                                        onAdFailedToShow = {
                                            errorMessage = "विज्ञापन लोड हो रहा है... कृपया कुछ सेकंड बाद प्रयास करें।"
                                        }
                                    )
                                },
                                onFailed = { error ->
                                    isAdLoading = false
                                    errorMessage = "विज्ञापन लोड हो रहा है... कृपया 5-10 सेकंड बाद पुनः प्रयास करें।"
                                }
                            )
                        }
                    },
                    enabled = !isAdLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("watch_ad_and_start_test_button")
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAdLoading) "विज्ञापन लोड हो रहा है..." else "पूरा Ad देखें और अनलॉक करें",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "बाद में करें (Cancel)", 
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color(0xFF334155),
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        )
    }
}
