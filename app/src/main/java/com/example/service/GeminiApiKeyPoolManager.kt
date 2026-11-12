package com.example.service

import android.content.Context
import com.example.BuildConfig

/**
 * Manages Gemini API Key Pool with Sequential Round-Robin Rotation and memory context.
 * Pre-configured with the admin's working Gemini API keys so zero manual setup is required.
 * Rotates through keys sequentially on rate-limits without prematurely jumping models.
 */
object GeminiApiKeyPoolManager {

    private const val PREFS_NAME = "gemini_api_keys_pool"
    private const val KEY_SLOT_1 = "key_slot_1"
    private const val KEY_SLOT_2 = "key_slot_2"
    private const val KEY_SLOT_3 = "key_slot_3"
    private const val KEY_SLOT_4 = "key_slot_4"
    private const val KEY_SLOT_5 = "key_slot_5"

    // Default primary key from project build environment
    private val DEFAULT_PRIMARY_KEY: String
        get() = try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else ""
        } catch (_: Exception) {
            ""
        }

    // Pre-configured 5 working Gemini Keys supplied by admin for automatic failover and seamless rotation
    val PRECONFIGURED_KEYS: List<String> = listOf(
        "YOUR_API_KEY_1",
        "YOUR_API_KEY_2",
        "YOUR_API_KEY_3",
        "YOUR_API_KEY_4",
        "YOUR_API_KEY_5"
    )

    fun isValidGeminiKey(k: String): Boolean {
        val trimmed = k.trim()
        if (trimmed.isBlank() || trimmed.length < 20) return false
        if (trimmed == "MY_GEMINI_API_KEY") return false
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

        // If no user keys saved in SharedPreferences, default automatically to the 5 preconfigured keys
        if (list.isEmpty()) {
            val pool = mutableListOf<String>()
            for (k in PRECONFIGURED_KEYS) {
                if (isValidGeminiKey(k) && !pool.contains(k)) {
                    pool.add(k)
                }
            }
            if (isValidGeminiKey(DEFAULT_PRIMARY_KEY) && !pool.contains(DEFAULT_PRIMARY_KEY)) {
                pool.add(DEFAULT_PRIMARY_KEY)
            }
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

    /**
     * Resets rotation index at the start of a PDF extraction
     */
    fun resetRotation() {
        rotationIndex = 0
    }

    /**
     * Gets the next API key in round-robin sequence.
     * Returns Triple(Key, KeyNumber (1-based), TotalKeysInPool)
     */
    @Synchronized
    fun getNextKey(context: Context? = null): Triple<String, Int, Int> {
        val pool = if (context != null) {
            getSavedKeys(context)
        } else {
            val p = mutableListOf<String>()
            for (k in PRECONFIGURED_KEYS) {
                if (isValidGeminiKey(k)) p.add(k)
            }
            if (DEFAULT_PRIMARY_KEY.isNotBlank() && isValidGeminiKey(DEFAULT_PRIMARY_KEY) && !p.contains(DEFAULT_PRIMARY_KEY)) {
                p.add(DEFAULT_PRIMARY_KEY)
            }
            p
        }

        if (pool.isEmpty()) {
            return Triple(DEFAULT_PRIMARY_KEY, 1, 1)
        }

        val safeIdx = (rotationIndex % pool.size).coerceAtLeast(0)
        val selectedKey = pool[safeIdx]
        val keyNumber = safeIdx + 1
        rotationIndex = (rotationIndex + 1) % pool.size

        return Triple(selectedKey, keyNumber, pool.size)
    }

    /**
     * Advances rotation to the next key after a rate-limit error,
     * so subsequent pages do not reuse the rate-limited key.
     */
    @Synchronized
    fun advanceKeyAfterRateLimit(context: Context? = null) {
        val poolSize = if (context != null) getSavedKeys(context).size else PRECONFIGURED_KEYS.size
        if (poolSize > 1) {
            rotationIndex = (rotationIndex + 1) % poolSize
        }
    }

    /**
     * Returns full key pool for fallback attempts if a particular key encounters a 429 quota block.
     * Rotates the list so startingKey is first, followed sequentially by all remaining keys in the pool.
     */
    fun getAllKeysForFallback(context: Context? = null, startingKey: String? = null): List<String> {
        val pool = if (context != null) {
            getSavedKeys(context)
        } else {
            val p = mutableListOf<String>()
            for (k in PRECONFIGURED_KEYS) {
                if (isValidGeminiKey(k)) p.add(k)
            }
            if (DEFAULT_PRIMARY_KEY.isNotBlank() && isValidGeminiKey(DEFAULT_PRIMARY_KEY) && !p.contains(DEFAULT_PRIMARY_KEY)) {
                p.add(DEFAULT_PRIMARY_KEY)
            }
            p.take(5)
        }

        if (pool.isEmpty()) {
            return if (DEFAULT_PRIMARY_KEY.isNotBlank()) listOf(DEFAULT_PRIMARY_KEY) else emptyList()
        }

        if (startingKey == null || !pool.contains(startingKey)) {
            return pool
        }

        val startIdx = pool.indexOf(startingKey)
        val reordered = mutableListOf<String>()
        for (i in pool.indices) {
            reordered.add(pool[(startIdx + i) % pool.size])
        }
        return reordered
    }
}
