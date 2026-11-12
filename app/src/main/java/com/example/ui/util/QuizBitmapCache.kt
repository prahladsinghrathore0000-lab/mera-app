package com.example.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Ultra high-speed in-memory LRU Bitmap cache and background preloader for Quiz questions.
 * Ensures zero-lag question flipping and instant image display even for tests with 100+ image-based questions.
 */
object QuizBitmapCache {
    private const val TAG = "QuizBitmapCache"

    // 32 MB Memory cache for decoded bitmaps
    private val memoryCache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    fun get(key: String): Bitmap? {
        val bmp = memoryCache.get(key)
        return if (bmp != null && !bmp.isRecycled) bmp else null
    }

    fun put(key: String, bitmap: Bitmap) {
        if (!bitmap.isRecycled) {
            memoryCache.put(key, bitmap)
        }
    }

    fun clear() {
        memoryCache.evictAll()
    }

    /**
     * Resolves and caches a bitmap synchronously or from memory cache.
     */
    fun resolveBitmapSync(context: Context, imageUrl: String?): Bitmap? {
        if (imageUrl.isNullOrBlank()) return null
        val cached = get(imageUrl)
        if (cached != null) return cached

        var bmp: Bitmap? = null

        // 1. Try Base64 string decode
        if (imageUrl.startsWith("data:image", true) ||
            (!imageUrl.startsWith("http") && !imageUrl.startsWith("/") && imageUrl.length > 50)
        ) {
            try {
                val clean = if (imageUrl.contains(",")) imageUrl.substringAfter(",") else imageUrl
                val bytes = Base64.decode(clean.trim(), Base64.DEFAULT)
                bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (e: Throwable) {
                Log.w(TAG, "Base64 decode error: ${e.message}")
            }
        }

        // 2. Try direct absolute file path
        if (bmp == null && imageUrl.startsWith("/")) {
            try {
                val file = File(imageUrl)
                if (file.exists() && file.length() > 0) {
                    bmp = BitmapFactory.decodeFile(file.absolutePath)
                }
            } catch (_: Throwable) {}
        }

        // 3. Try OfflineTestRepository cached image resolution
        if (bmp == null) {
            try {
                val cachedFile = com.example.model.OfflineTestRepository.getOfflineImageFile(context, imageUrl)
                if (cachedFile != null && cachedFile.exists() && cachedFile.length() > 0) {
                    bmp = BitmapFactory.decodeFile(cachedFile.absolutePath)
                }
            } catch (_: Throwable) {}
        }

        // 4. Try offline images directory resolution by simple filename
        if (bmp == null) {
            try {
                val offlineDir = File(context.filesDir, "offline_images")
                if (offlineDir.exists()) {
                    val simpleName = imageUrl.substringAfterLast("/").substringBefore("?")
                    if (simpleName.isNotBlank()) {
                        val candidateFile = File(offlineDir, simpleName)
                        if (candidateFile.exists() && candidateFile.length() > 0) {
                            bmp = BitmapFactory.decodeFile(candidateFile.absolutePath)
                        }
                    }
                    if (bmp == null) {
                        val allFiles = offlineDir.listFiles()
                        if (allFiles != null) {
                            val qMatch = Regex("""q(\d+)""").find(imageUrl)?.value
                            if (qMatch != null) {
                                val matchedFile = allFiles.firstOrNull { it.name.contains(qMatch) && it.length() > 0 }
                                if (matchedFile != null) {
                                    bmp = BitmapFactory.decodeFile(matchedFile.absolutePath)
                                }
                            }
                        }
                    }
                }
            } catch (_: Throwable) {}
        }

        if (bmp != null) {
            put(imageUrl, bmp)
        }
        return bmp
    }

    /**
     * Preloads images of a quiz asynchronously on IO dispatcher so questions display instantly
     */
    suspend fun preloadImages(context: Context, imageUrls: List<String?>) {
        withContext(Dispatchers.IO) {
            for (url in imageUrls) {
                if (!url.isNullOrBlank() && get(url) == null) {
                    resolveBitmapSync(context, url)
                }
            }
        }
    }
}
