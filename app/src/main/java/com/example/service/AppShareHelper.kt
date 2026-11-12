package com.example.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Utility helper to share the Exam Mitra / FREE PYQ app across WhatsApp, Telegram,
 * system share chooser, and clipboard.
 */
object AppShareHelper {
    // Official App Share / Play Store URL
    const val APP_SHARE_URL = "https://play.google.com/store/apps/details?id=com.aistudio.examprep.kspvm"
    const val SUPPORT_EMAIL = "prahladsinghrathore869025@gmail.com"
    const val TELEGRAM_CHANNEL_URL = "https://t.me/ExamMitraFreePYQ"
    const val WHATSAPP_SUPPORT_NUMBER = "918690250000" // Support helpline

    /**
     * Standard attractive Hindi/English message designed to maximize shares and installs.
     */
    fun getShareMessage(): String {
        return """
            📚 *Exam Mitra (FREE PYQ) - प्रतियोगी परीक्षा तैयारी ऐप* 🎯
            
            SSC, Delhi Police (Constable / Head Constable), Railway (NTPC / Group D), UP Police और State Exams की तैयारी के लिए सबसे बेहतरीन और 100% फ्री ऐप!
            
            ✨ *ऐप की मुख्य विशेषताएं:*
            🔥 100+ फुल मॉक टेस्ट्स एवं प्रीवियस ईयर पेपर्स (PYQ)
            ⚡ नए टेस्ट्स ऑटोमैटिक लाइव (बिना ऐप अपडेट किए सीधे नए टेस्ट)
            📖 चैप्टर-वाइज़ टेस्ट्स और विस्तृत हिंदी/इंग्लिश हल
            📶 100% ऑफलाइन टेस्ट मोड (बिना इंटरनेट के भी टेस्ट दें)
            🎯 कमज़ोर टॉपिक सुधार (Weak Areas Booster) प्रैक्टिस
            🛡️ 100% सेफ और विज्ञापन-मुक्त स्मूथ इंटरफेस
            
            📲 अभी अपने मोबाइल में डाउनलोड करें और दोस्तों के साथ तैयारी शुरू करें:
            $APP_SHARE_URL
        """.trimIndent()
    }

    /**
     * Open system share sheet (WhatsApp, Telegram, Facebook, SMS, Bluetooth, etc.)
     */
    fun shareApp(context: Context) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, "Exam Mitra - 100% Free Mock Tests & PYQ App")
                putExtra(Intent.EXTRA_TEXT, getShareMessage())
                type = "text/plain"
            }
            val chooser = Intent.createChooser(sendIntent, "Exam Mitra ऐप दोस्तों के साथ शेयर करें")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "शेयर करने में समस्या आई: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Direct share via WhatsApp
     */
    fun shareOnWhatsApp(context: Context) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, getShareMessage())
                type = "text/plain"
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(sendIntent)
        } catch (e: Exception) {
            // Fallback to general share if WhatsApp is not installed
            shareApp(context)
        }
    }

    /**
     * Direct share via Telegram
     */
    fun shareOnTelegram(context: Context) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, getShareMessage())
                type = "text/plain"
                setPackage("org.telegram.messenger")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(sendIntent)
        } catch (e: Exception) {
            // Fallback to general share if Telegram is not installed
            shareApp(context)
        }
    }

    /**
     * Copy share text and link to clipboard
     */
    fun copyShareLink(context: Context) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Exam Mitra App Link", getShareMessage())
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "शेयर लिंक और मैसेज कॉपी हो गया! दोस्तों को भेजें 📋", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "कॉपी नहीं हो सका: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open direct WhatsApp chat with support
     */
    fun openWhatsAppSupport(context: Context, userQuery: String? = null) {
        try {
            val baseMessage = if (!userQuery.isNullOrBlank()) {
                "नमस्ते Exam Mitra 24/7 सपोर्ट टीम!\nमेरी समस्या/सवाल: $userQuery"
            } else {
                "नमस्ते Exam Mitra 24/7 सपोर्ट टीम! मुझे ऐप से संबंधित सहायता चाहिए।"
            }
            val encodedMessage = Uri.encode(baseMessage)
            val uri = Uri.parse("https://wa.me/$WHATSAPP_SUPPORT_NUMBER?text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to email if WhatsApp cannot be opened
            openEmailSupport(context, userQuery)
        }
    }

    /**
     * Open email support client
     */
    fun openEmailSupport(context: Context, userQuery: String? = null) {
        try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$SUPPORT_EMAIL")
                putExtra(Intent.EXTRA_SUBJECT, "[Exam Mitra 24/7 Support] सहायता अनुरोध")
                putExtra(
                    Intent.EXTRA_TEXT,
                    """
                    नमस्ते सपोर्ट टीम,
                    
                    ${userQuery ?: "मुझे ऐप उपयोग करने में सहायता की आवश्यकता है। कृपया मेरा मार्गदर्शन करें।"}
                    
                    ---
                    Device Info: Android
                    App: Exam Mitra (v2.5)
                    """.trimIndent()
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(emailIntent, "ईमेल द्वारा सहायता प्राप्त करें"))
        } catch (e: Exception) {
            Toast.makeText(context, "ईमेल क्लाइंट नहीं मिला: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open Telegram Community / Channel
     */
    fun openTelegramCommunity(context: Context) {
        try {
            val uri = Uri.parse(TELEGRAM_CHANNEL_URL)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "टेलीग्राम नहीं खुल सका", Toast.LENGTH_SHORT).show()
        }
    }
}
