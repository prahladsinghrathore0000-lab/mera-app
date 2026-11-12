package com.example.model

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Offline Test Repository:
 * Allows students to attempt unlocked tests completely offline (without internet).
 * Automatically caches questions JSON as well as all reasoning/question images locally
 * so tests can be attempted offline anytime!
 */
object OfflineTestRepository {
    private const val TAG = "OfflineTestRepo"
    private const val PREFS_NAME = "exam_mitra_offline_tests"
    private const val KEY_OFFLINE_CACHE_KEYS = "offline_cached_test_keys"

    // Key -> Boolean indicating offline availability
    val offlineReadyMap: SnapshotStateMap<String, Boolean> = mutableStateMapOf()
    private var prefs: SharedPreferences? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val repoScope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedKeys = prefs?.getStringSet(KEY_OFFLINE_CACHE_KEYS, emptySet()) ?: emptySet()
            savedKeys.forEach { key ->
                offlineReadyMap[key] = true
            }
        }
    }

    /**
     * Checks if active internet connection is currently available across Mobile Data, Wi-Fi, or Ethernet
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.applicationContext
                .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true // Fail open to avoid blocking users if service unavailable

            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                if (capabilities != null) {
                    val hasTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                    val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    if (hasTransport && hasInternet) {
                        return true
                    }
                }
            }

            // Fallback for devices or network configurations
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo != null && networkInfo.isConnectedOrConnecting
        } catch (e: Exception) {
            true // Default to connected to never trap users incorrectly
        }
    }

    /**
     * Determines whether a test can be played right now offline.
     * A test is playable offline only if it has been unlocked by watching an ad AND is cached.
     */
    fun isTestOfflineReady(vararg testKeys: String): Boolean {
        for (key in testKeys) {
            if (key.isBlank()) continue
            val normalized = key.trim().lowercase()
            val isUnlocked = UnlockedTestsRepository.isTestUnlocked(key, normalized)
            val isCached = offlineReadyMap[key] == true || offlineReadyMap[normalized] == true
            if (isUnlocked && isCached) return true
        }
        return false
    }

    /**
     * Returns the cached local image file for offline viewing if it exists on disk.
     */
    fun getOfflineImageFile(context: Context, imageUrl: String?): File? {
        if (imageUrl.isNullOrBlank()) return null
        val ctx = context.applicationContext
        val imagesDir = File(ctx.filesDir, "offline_images")
        if (!imagesDir.exists()) return null

        // 1. Direct path check
        if (imageUrl.startsWith("/")) {
            val f = File(imageUrl)
            if (f.exists() && f.length() > 0) return f
        }

        // 2. Exact simple filename in offline_images
        val simpleName = imageUrl.substringAfterLast("/").substringBefore("?")
        if (simpleName.isNotBlank()) {
            val namedFile = File(imagesDir, simpleName)
            if (namedFile.exists() && namedFile.length() > 0) return namedFile
        }

        // 3. Hash-based filename
        val safeHash = Math.abs(imageUrl.hashCode()).toString()
        val extension = when {
            imageUrl.endsWith(".png", true) -> ".png"
            imageUrl.endsWith(".jpg", true) || imageUrl.endsWith(".jpeg", true) -> ".jpg"
            imageUrl.endsWith(".webp", true) -> ".webp"
            else -> ".png"
        }
        val hashFile = File(imagesDir, "img_$safeHash$extension")
        if (hashFile.exists() && hashFile.length() > 0) return hashFile

        // 4. Also check without extension or with alternate extensions
        val hashJpg = File(imagesDir, "img_$safeHash.jpg")
        if (hashJpg.exists() && hashJpg.length() > 0) return hashJpg
        val hashPng = File(imagesDir, "img_$safeHash.png")
        if (hashPng.exists() && hashPng.length() > 0) return hashPng

        return null
    }

    /**
     * Synchronously saves image bytes directly to local storage under all lookup aliases
     */
    fun saveImageDirectly(context: Context, keyOrUrl: String, bytes: ByteArray) {
        if (keyOrUrl.isBlank() || bytes.isEmpty()) return
        try {
            val ctx = context.applicationContext
            val imagesDir = File(ctx.filesDir, "offline_images")
            if (!imagesDir.exists()) imagesDir.mkdirs()

            val safeHash = Math.abs(keyOrUrl.hashCode()).toString()
            val extension = when {
                keyOrUrl.endsWith(".png", true) -> ".png"
                keyOrUrl.endsWith(".jpg", true) || keyOrUrl.endsWith(".jpeg", true) -> ".jpg"
                keyOrUrl.endsWith(".webp", true) -> ".webp"
                else -> ".jpg"
            }

            // Save by hash
            val hashFile = File(imagesDir, "img_$safeHash$extension")
            hashFile.writeBytes(bytes)

            // Save by simple name if present
            val simpleName = keyOrUrl.substringAfterLast("/").substringBefore("?")
            if (simpleName.isNotBlank() && simpleName != keyOrUrl) {
                val namedFile = File(imagesDir, simpleName)
                namedFile.writeBytes(bytes)
            }

            Log.d(TAG, "Saved image bytes (${bytes.size} B) for key: $keyOrUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image directly: ${e.message}")
        }
    }

    /**
     * Downloads and caches an image file to disk for offline test readiness (Suspending).
     */
    suspend fun downloadAndSaveImageSuspending(context: Context, imageUrl: String): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (imageUrl.isBlank()) return@withContext false
        try {
            val ctx = context.applicationContext
            val existing = getOfflineImageFile(ctx, imageUrl)
            if (existing != null && existing.exists() && existing.length() > 0) {
                return@withContext true
            }

            val imagesDir = File(ctx.filesDir, "offline_images")
            if (!imagesDir.exists()) imagesDir.mkdirs()

            // Handle Base64 data URI directly
            if (imageUrl.startsWith("data:image", true) || (!imageUrl.startsWith("http") && !imageUrl.startsWith("/") && imageUrl.length > 100)) {
                try {
                    val clean = if (imageUrl.contains(",")) imageUrl.substringAfter(",") else imageUrl
                    val bytes = android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
                    if (bytes != null && bytes.isNotEmpty()) {
                        saveImageDirectly(ctx, imageUrl, bytes)
                        return@withContext true
                    }
                } catch (b64Ex: Exception) {
                    Log.e(TAG, "Failed to decode base64 image: ${b64Ex.message}")
                }
                return@withContext false
            }

            // Handle local file paths
            if (imageUrl.startsWith("/")) {
                val srcFile = File(imageUrl)
                if (srcFile.exists() && srcFile.length() > 0) {
                    val bytes = srcFile.readBytes()
                    saveImageDirectly(ctx, imageUrl, bytes)
                    return@withContext true
                }
                return@withContext false
            }

            // Remote URL download
            val request = Request.Builder().url(imageUrl).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    saveImageDirectly(ctx, imageUrl, bytes)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download image: ${e.message}")
        }
        return@withContext false
    }

    /**
     * Checks if a test and all its images are 100% saved locally on this phone.
     */
    fun isTestFullySavedOnPhone(context: Context, testKey: String, questions: List<QuizQuestion>? = null): Boolean {
        if (testKey.isBlank()) return false
        val ctx = context.applicationContext
        
        // 1. Check if JSON file exists in offline_tests
        val dir = File(ctx.filesDir, "offline_tests")
        val safeFileName = "test_" + testKey.hashCode() + ".json"
        val file = File(dir, safeFileName)
        val jsonExists = file.exists() && file.length() > 0

        val qs = questions ?: getOfflineQuestions(ctx, testKey)
        if (qs.isNullOrEmpty()) {
            return jsonExists
        }

        // 2. Check if all images exist
        for (q in qs) {
            if (!q.imageUrl.isNullOrBlank()) {
                val imgFile = getOfflineImageFile(ctx, q.imageUrl)
                if (imgFile == null || !imgFile.exists() || imgFile.length() == 0L) {
                    return false
                }
            }
            for (opt in q.options) {
                if (opt.startsWith("http") || opt.startsWith("data:image")) {
                    val imgFile = getOfflineImageFile(ctx, opt)
                    if (imgFile == null || !imgFile.exists() || imgFile.length() == 0L) {
                        return false
                    }
                }
            }
        }
        return true
    }

    /**
     * Downloads all images and saves the complete test JSON to phone storage on-demand.
     */
    suspend fun saveTestAndImagesToPhone(
        context: Context,
        testKey: String,
        questions: List<QuizQuestion>,
        onProgress: ((progress: Float, status: String) -> Unit)? = null
    ): Boolean = kotlinx.coroutines.withContext(Dispatchers.IO) {
        if (testKey.isBlank() || questions.isEmpty()) return@withContext false
        val ctx = context.applicationContext
        try {
            onProgress?.invoke(0.1f, "प्रश्नों की सूची तैयार हो रही है...")
            
            // 1. Save JSON first
            cacheTestQuestions(ctx, testKey, questions)

            // 2. Extract all images
            val imageList = mutableListOf<String>()
            for (q in questions) {
                if (!q.imageUrl.isNullOrBlank()) {
                    imageList.add(q.imageUrl)
                }
                for (opt in q.options) {
                    if (opt.startsWith("http") || opt.startsWith("data:image")) {
                        imageList.add(opt)
                    }
                }
            }

            val distinctImages = imageList.distinct()
            val totalImages = distinctImages.size

            if (totalImages > 0) {
                distinctImages.forEachIndexed { index, imgUrl ->
                    val progressVal = 0.2f + (0.75f * ((index + 1).toFloat() / totalImages.toFloat()))
                    onProgress?.invoke(progressVal, "चित्र ${index + 1}/$totalImages फोन में सुरक्षित हो रहा है...")
                    downloadAndSaveImageSuspending(ctx, imgUrl)
                }
            }

            // 3. Mark saved & unlocked
            markTestUnlockedAndOffline(ctx, testKey, questions)
            onProgress?.invoke(1.0f, "🎉 टेस्ट फोन में सुरक्षित हो गया!")
            Log.d(TAG, "Test '$testKey' and $totalImages images saved to phone storage successfully.")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save test and images to phone: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Downloads and caches an image file to disk for offline test readiness.
     */
    fun downloadAndCacheImage(context: Context, imageUrl: String) {
        if (imageUrl.isBlank()) return
        repoScope.launch {
            downloadAndSaveImageSuspending(context, imageUrl)
        }
    }

    /**
     * Caches questions and pre-downloads images to local disk WITHOUT unlocking the test.
     * This prepares the test for offline readiness as soon as the user watches an ad.
     */
    fun cacheTestQuestions(
        context: Context,
        testKey: String,
        questions: List<QuizQuestion>?
    ) {
        if (testKey.isBlank() || questions.isNullOrEmpty()) return
        val ctx = context.applicationContext
        try {
            val dir = File(ctx.filesDir, "offline_tests")
            if (!dir.exists()) dir.mkdirs()

            val safeFileName = "test_" + testKey.hashCode() + ".json"
            val file = File(dir, safeFileName)

            val array = JSONArray()
            for (q in questions) {
                val obj = JSONObject()
                obj.put("id", q.id)
                obj.put("question", q.question)
                val opts = JSONArray()
                q.options.forEach { opts.put(it) }
                obj.put("options", opts)
                obj.put("correctIndex", q.correctIndex)
                obj.put("explanation", q.explanation)
                obj.put("subject", q.subject)
                if (!q.imageUrl.isNullOrBlank()) {
                    obj.put("imageUrl", q.imageUrl)
                    // Trigger async background download / local caching for image
                    downloadAndCacheImage(ctx, q.imageUrl)
                }
                // Also cache any image in options
                q.options.forEach { opt ->
                    if (opt.startsWith("http") || opt.startsWith("data:image")) {
                        downloadAndCacheImage(ctx, opt)
                    }
                }
                array.put(obj)
            }

            file.writeText(array.toString())
            Log.d(TAG, "Cached ${questions.size} questions locally for test: $testKey")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache test data: ${e.message}", e)
        }
    }

    /**
     * Unlocks the test for lifetime use and marks it offline-ready after user watches a rewarded ad.
     */
    fun markTestUnlockedAndOffline(
        context: Context,
        testKey: String,
        questions: List<QuizQuestion>? = null
    ) {
        if (testKey.isBlank()) return
        val normalized = testKey.trim().lowercase()

        offlineReadyMap[testKey] = true
        offlineReadyMap[normalized] = true

        // Mark unlocked in UnlockedTestsRepository
        UnlockedTestsRepository.unlockTest(context, testKey, normalized)

        val ctx = context.applicationContext
        if (prefs == null) {
            prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        prefs?.let { p ->
            val existing = p.getStringSet(KEY_OFFLINE_CACHE_KEYS, emptySet())?.toMutableSet() ?: mutableSetOf()
            existing.add(testKey)
            existing.add(normalized)
            p.edit().putStringSet(KEY_OFFLINE_CACHE_KEYS, existing).apply()
        }

        if (!questions.isNullOrEmpty()) {
            cacheTestQuestions(context, testKey, questions)
        }
    }

    /**
     * Purges stale cached/downloaded versions of this test from the phone's local database/storage
     * and refreshes it with the newly edited questions so the user immediately gets fresh edited data!
     */
    fun purgeAndRefreshOfflineCache(
        context: Context,
        testKeys: List<String>,
        updatedQuestions: List<QuizQuestion>? = null
    ) {
        val ctx = context.applicationContext
        val dir = File(ctx.filesDir, "offline_tests")
        
        for (key in testKeys) {
            if (key.isBlank()) continue
            val normalized = key.trim().lowercase()
            
            // Remove from memory state
            offlineReadyMap.remove(key)
            offlineReadyMap.remove(normalized)
            
            // Delete the local JSON cache files from phone storage
            try {
                val safeFileName1 = "test_" + key.hashCode() + ".json"
                val file1 = File(dir, safeFileName1)
                if (file1.exists()) file1.delete()
                
                val safeFileName2 = "test_" + normalized.hashCode() + ".json"
                val file2 = File(dir, safeFileName2)
                if (file2.exists()) file2.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting old cached test file: ${e.message}")
            }
            
            // If updated questions are present, rewrite the fresh edited questions to phone storage
            if (!updatedQuestions.isNullOrEmpty()) {
                cacheTestQuestions(ctx, key, updatedQuestions)
                markTestUnlockedAndOffline(ctx, key, updatedQuestions)
            }
        }
    }

    /**
     * Backward-compatible alias for unlocking & marking offline.
     */
    fun saveTestOffline(
        context: Context,
        testKey: String,
        questions: List<QuizQuestion>?
    ) {
        markTestUnlockedAndOffline(context, testKey, questions)
    }

    /**
     * Purges all offline cached tests from disk and memory.
     */
    fun purgeAllOfflineCache(context: Context) {
        val ctx = context.applicationContext
        offlineReadyMap.clear()
        prefs?.edit()?.remove(KEY_OFFLINE_CACHE_KEYS)?.apply()
        try {
            val dir = File(ctx.filesDir, "offline_tests")
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error purging all offline tests: ${e.message}")
        }
    }

    /**
     * Loads offline cached questions from disk if available
     */
    fun getOfflineQuestions(context: Context, testKey: String): List<QuizQuestion>? {
        return try {
            val dir = File(context.applicationContext.filesDir, "offline_tests")
            val safeFileName = "test_" + testKey.hashCode() + ".json"
            val file = File(dir, safeFileName)
            if (!file.exists()) return null

            val json = file.readText()
            val array = JSONArray(json)
            val list = mutableListOf<QuizQuestion>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val optsArray = obj.getJSONArray("options")
                val opts = mutableListOf<String>()
                for (j in 0 until optsArray.length()) {
                    opts.add(optsArray.getString(j))
                }
                val rawImageUrl = obj.optString("imageUrl", "")
                val imageUrl = if (rawImageUrl.isNotBlank() && rawImageUrl != "null") rawImageUrl else null

                list.add(
                    QuizQuestion(
                        id = obj.getInt("id"),
                        question = obj.getString("question"),
                        options = opts,
                        correctIndex = obj.getInt("correctIndex"),
                        explanation = obj.optString("explanation", ""),
                        subject = obj.optString("subject", ""),
                        imageUrl = imageUrl
                    )
                )
            }
            if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            Log.e(TAG, "Error reading offline questions: ${e.message}", e)
            null
        }
    }
}
