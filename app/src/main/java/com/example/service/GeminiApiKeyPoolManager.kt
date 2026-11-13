package com.example.service

import android.content.Context
import com.example.ApiKeys
import com.example.BuildConfig

object GeminiApiKeyPoolManager {

    private const val PREFS_NAME = "gemini_api_keys_pool"
    private const val KEY_SLOT_1 = "key_slot_1"
    private const val KEY_SLOT_2 = "key_slot_2"
    private const val KEY_SLOT_3 = "key_slot_3"
    private const val KEY_SLOT_4 = "key_slot_4"
    private const val KEY_SLOT_5 = "key_slot_5"

    private val DEFAULT_PRIMARY_KEY: String
        get() = try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else ""
        } catch (_: Exception) {
            ""
        }

    // FIX: Ab seedha teri Apikeys.kt wali geminiKeys se uthayega
    val PRECONFIGURED_KEYS: List<String> = ApiKeys.geminiKeys

    fun isValidGeminiKey(k: String): Boolean {
        val trimmed = k.trim()
        if (trimmed.isBlank() || trimmed.length < 20) return false
        if (trimmed == "MY_GEMINI_API_KEY") return false
        if (trimmed.startsWith("YOUR_API_KEY")) return false
        return true
    }

    private var rotationIndex = 0

    fun getSavedKeys(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val list = mutableListOf<String>()
        val s1 = prefs.getString(KEY_SLOT_1, "") ?: ""
        val s2 = prefs.getString(KEY_SLOT_2, "") ?: ""
        val s3 = prefs.getString(KEY_SLOT_3, "") ?: ""
        val s4 = prefs.getString(KEY_SLOT_4, "") ?: ""
        val s5 = prefs.getString(KEY_SLOT_5, "") ?: ""
        if (isValidGeminiKey(s1)) list.add(s1.trim())
        if (isValidGeminiKey(s2)) list.add(s2.trim())
        if (isValidGeminiKey(s3)) list.add(s3.trim())
        if (isValidGeminiKey(s4)) list.add(s4.trim())
        if (isValidGeminiKey(s5)) list.add(s5.trim())
        if (list.isEmpty()) {
            val pool = mutableListOf<String>()
            for (k in PRECONFIGURED_KEYS) {
                if (isValidGeminiKey(k) && !pool.contains(k)) pool.add(k)
            }
            if (isValidGeminiKey(DEFAULT_PRIMARY_KEY) && !pool.contains(DEFAULT_PRIMARY_KEY)) pool.add(DEFAULT_PRIMARY_KEY)
            return pool.take(5)
        }
        return list.take(5)
    }

    fun saveKeys(context: Context, keys: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_SLOT_1, keys.getOrNull(0)?.trim() ?: "")
            putString(KEY_SLOT_2, keys.getOrNull(1)?.trim() ?: "")
            putString(KEY_SLOT_3, keys.getOrNull(2)?.trim() ?: "")
            putString(KEY_SLOT_4, keys.getOrNull(3)?.trim() ?: "")
            putString(KEY_SLOT_5, keys.getOrNull(4)?.trim() ?: "")
            apply()
        }
    }

    fun resetRotation() { rotationIndex = 0 }

    @Synchronized
    fun getNextKey(context: Context? = null): Triple<String, Int, Int> {
        val pool = if (context != null) getSavedKeys(context) else {
            val p = mutableListOf<String>()
            for (k in PRECONFIGURED_KEYS) if (isValidGeminiKey(k)) p.add(k)
            if (DEFAULT_PRIMARY_KEY.isNotBlank() && isValidGeminiKey(DEFAULT_PRIMARY_KEY) && !p.contains(DEFAULT_PRIMARY_KEY)) p.add(DEFAULT_PRIMARY_KEY)
            p
        }
        if (pool.isEmpty()) return Triple(DEFAULT_PRIMARY_KEY, 1, 1)
        val safeIdx = (rotationIndex % pool.size).coerceAtLeast(0)
        val selectedKey = pool[safeIdx]
        val keyNumber = safeIdx + 1
        rotationIndex = (rotationIndex + 1) % pool.size
        return Triple(selectedKey, keyNumber, pool.size)
    }

    @Synchronized
    fun advanceKeyAfterRateLimit(context: Context? = null) {
        val poolSize = if (context != null) getSavedKeys(context).size else PRECONFIGURED_KEYS.size
        if (poolSize > 1) rotationIndex = (rotationIndex + 1) % poolSize
    }

    fun getAllKeysForFallback(context: Context? = null, startingKey: String? = null): List<String> {
        val pool = if (context != null) getSavedKeys(context) else {
            val p = mutableListOf<String>()
            for (k in PRECONFIGURED_KEYS) if (isValidGeminiKey(k)) p.add(k)
            if (DEFAULT_PRIMARY_KEY.isNotBlank() && isValidGeminiKey(DEFAULT_PRIMARY_KEY) && !p.contains(DEFAULT_PRIMARY_KEY)) p.add(DEFAULT_PRIMARY_KEY)
            p.take(5)
        }
        if (pool.isEmpty()) return if (DEFAULT_PRIMARY_KEY.isNotBlank()) listOf(DEFAULT_PRIMARY_KEY) else emptyList()
        if (startingKey == null || !pool.contains(startingKey)) return pool
        val startIdx = pool.indexOf(startingKey)
        val reordered = mutableListOf<String>()
        for (i in pool.indices) reordered.add(pool[(startIdx + i) % pool.size])
        return reordered
    }
}
