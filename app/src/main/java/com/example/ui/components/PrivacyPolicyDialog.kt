package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

const val PRIVACY_POLICY_URL = "https://free-pyq.github.io/privacy-policy.html"
const val SUPPORT_EMAIL = "prahladsinghrathore869025@gmail.com"

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("गोपनीयता नीति (Privacy)", "सेवा शर्तें (Terms)", "विज्ञापन नीति (Ads)")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("privacy_policy_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "कानूनी एवं नीतियां",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "FREE PYQ • Legal & Disclosures",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "बंद करें",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF2563EB),
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedTabIndex) {
                        0 -> {
                            // Privacy Policy Content
                            item {
                                PolicySectionCard(
                                    icon = Icons.Default.VerifiedUser,
                                    title = "1. हमारा संकल्प (Our Commitment)",
                                    content = "FREE PYQ विद्यार्थियों के लिए बनाया गया 100% निःशुल्क और सुरक्षित शैक्षणिक ऐप है। हम आपकी व्यक्तिगत गोपनीयता (Privacy) का पूर्ण सम्मान करते हैं और आपके डेटा को सुरक्षित रखने के लिए प्रतिबद्ध हैं।"
                                )
                            }
                            item {
                                PolicySectionCard(
                                    icon = Icons.Default.Lock,
                                    title = "2. हम कौन सा डेटा एकत्र करते हैं (Data Collection)",
                                    content = "• प्रोफ़ाइल डेटा: Google Sign-In के माध्यम से आपका नाम, ईमेल आईडी और प्रोफ़ाइल चित्र।\n• टेस्ट प्रगति: आपके द्वारा दिए गए मॉक टेस्ट के अंक, समय, एक्यूरेसी और बुकमार्क किए गए प्रश्न।\n• तकनीकी डेटा: ऐप के सुचारू संचालन और सुरक्षा के लिए गैर-व्यक्तिगत डिवाइस पहचानकर्ता।"
                                )
                            }
                            item {
                                PolicySectionCard(
                                    icon = Icons.Default.Security,
                                    title = "3. डेटा का उपयोग (Data Usage & Non-Sale)",
                                    content = "• डेटा का उपयोग केवल विद्यार्थी के स्कोर कार्ड, लाइव लीडरबोर्ड में रैंक प्रदर्शित करने और कमजोर विषयों का विश्लेषण दिखाने के लिए किया जाता है।\n• हम कभी भी विद्यार्थियों का डेटा किसी भी तीसरे पक्ष (Third Party) को नहीं बेचते और न ही अनधिकृत साझा करते हैं।"
                                )
                            }
                            item {
                                PolicySectionCard(
                                    icon = Icons.Default.Gavel,
                                    title = "4. डेटा हटाने का अधिकार (Data Deletion)",
                                    content = "प्रत्येक विद्यार्थी को अपना खाता और सभी सेव किए गए टेस्ट रिकॉर्ड पूरी तरह हटाने का अधिकार है। आप किसी भी समय सपोर्ट ईमेल ($SUPPORT_EMAIL) पर संपर्क करके अपने डेटा को डिलीट करवा सकते हैं।"
                                )
                            }
                        }
                        1 -> {
                            // Terms of Service Content
                            item {
                                PolicySectionCard(
                                    icon = Icons.Default.Gavel,
                                    title = "1. सेवा की शर्तें (Terms of Service)",
                                    content = "FREE PYQ ऐप का उपयोग केवल व्यक्तिगत और शैक्षणिक तैयारी (Educational Purpose) के लिए अनुमत है। इस ऐप की सामग्री विभिन्न प्रतियोगी परीक्षाओं के पूर्व प्रश्नपत्रों (PYQ) पर आधारित है।"
                                )
                            }
                            item {
                                PolicySectionCard(
                                    icon = Icons.Default.Security,
                                    title = "2. सिस्टम सुरक्षा एवं उचित उपयोग (Fair Use)",
                                    content = "ऐप में किसी भी प्रकार की छेड़छाड़, रिवर्स इंजीनियरिंग, ऑटोमेटेड बॉट्स या अनधिकृत टेस्ट अनलॉकिंग पूर्णतः प्रतिबंधित है। सुरक्षा नियमों का उल्लंघन होने पर खाता प्रतिबंधित किया जा सकता है।"
                                )
                            }
                            item {
                                PolicySectionCard(
                                    icon = Icons.Default.VerifiedUser,
                                    title = "3. सटीकता एवं अस्वीकरण (Disclaimer)",
                                    content = "हम सभी प्रश्नों और उत्तरों को सटीक रखने का पूरा प्रयास करते हैं। यह ऐप किसी सरकारी संस्था या आयोग का आधिकारिक ऐप नहीं है; यह छात्रों की स्व-अध्ययन सहायता हेतु एक स्वतंत्र मंच है।"
                                )
                            }
                        }
                        2 -> {
                            // Advertising & AdMob Policy
                            item {
                                PolicySectionCard(
                                    icon = Icons.Default.Policy,
                                    title = "1. Google AdMob विज्ञापन नीति (AdMob Disclosure)",
                                    content = "ऐप को सभी विद्यार्थियों के लिए 100% फ्री रखने और सर्वर लागत निकालने हेतु ऐप में Google AdMob द्वारा विज्ञापन (Banner, Interstitial और Rewarded Ads) प्रदर्शित किए जाते हैं।"
                                )
                            }
                            item {
                                PolicySectionCard(
                                    icon = Icons.Default.Shield,
                                    title = "2. विज्ञापनों में छात्र सुरक्षा",
                                    content = "• Google AdMob की नीतियां बच्चों और छात्रों के अनुकूल विज्ञापन दिशानिर्देशों का कड़ाई से पालन करती हैं।\n• विज्ञापनों में कोई भी संवेदनशील या आपत्तिजनक सामग्री नहीं दिखाई जाती।"
                                )
                            }
                            item {
                                PolicySectionCard(
                                    icon = Icons.Default.VerifiedUser,
                                    title = "3. पुरस्कृत विज्ञापन (Rewarded Ads)",
                                    content = "विशेष टेस्ट पेपर्स को अनलॉक करने के लिए छात्र स्वेच्छा से पुरस्कृत वीडियो विज्ञापन देख सकते हैं। विज्ञापन पूरा देखने पर टेस्ट हमेशा के लिए अनलॉक हो जाता है।"
                                )
                            }
                        }
                    }

                    // Contact Info Card
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "गोपनीयता अधिकारी एवं सहायता:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        text = SUPPORT_EMAIL,
                                        fontSize = 12.sp,
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Policy URL", PRIVACY_POLICY_URL)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "पॉलिसी लिंक कॉपी हो गया!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("लिंक कॉपी करें", fontSize = 12.5.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("समझ गया (OK)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PolicySectionCard(
    icon: ImageVector,
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = content,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
