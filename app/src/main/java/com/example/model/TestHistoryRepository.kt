package com.example.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Data model for recording test attempts, high scores, accuracy, and timestamps.
 */
data class TestAttemptStats(
    val testKey: String,
    val attemptsCount: Int,
    val highestScore: Double,
    val lastScore: Double,
    val totalMarks: Double,
    val accuracy: Int,
    val percentile: Double,
    val lastAttemptTimestamp: Long
)

/**
 * Global reactive repository tracking test completion, attempt counts, and best scores.
 * Fully persists all attempts offline in local SharedPreferences.
 */
object TestHistoryRepository {
    private const val PREFS_NAME = "exam_mitra_test_history"
    private const val KEY_ATTEMPT_KEYS = "saved_attempt_keys"

    val attemptRecords: SnapshotStateMap<String, TestAttemptStats> = mutableStateMapOf()
    private var prefs: SharedPreferences? = null

    init {
        // Dummy data removed to show actual user performance dynamically
    }

    fun init(context: Context) {
        if (prefs == null) {
            val ctx = context.applicationContext
            prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadFromPrefs()

            com.example.service.AuthManager.currentUser?.uid?.let { uid ->
                syncCloudHistory(uid, ctx)
            }
        }
    }

    private fun loadFromPrefs() {
        prefs?.let { p ->
            val keys = p.getStringSet(KEY_ATTEMPT_KEYS, emptySet()) ?: emptySet()
            keys.forEach { key ->
                val jsonStr = p.getString("stat_$key", null)
                if (jsonStr != null) {
                    try {
                        val obj = JSONObject(jsonStr)
                        val stats = TestAttemptStats(
                            testKey = obj.getString("testKey"),
                            attemptsCount = obj.getInt("attemptsCount"),
                            highestScore = obj.getDouble("highestScore"),
                            lastScore = obj.getDouble("lastScore"),
                            totalMarks = obj.getDouble("totalMarks"),
                            accuracy = obj.getInt("accuracy"),
                            percentile = obj.getDouble("percentile"),
                            lastAttemptTimestamp = obj.getLong("lastAttemptTimestamp")
                        )
                        attemptRecords[key] = stats
                    } catch (e: Exception) {
                        // ignore malformed
                    }
                }
            }
        }
    }

    private fun recordAttemptInMemory(
        testKey: String,
        score: Double,
        totalMarks: Double,
        accuracy: Int,
        percentile: Double
    ) {
        val existing = attemptRecords[testKey]
        val newAttemptCount = (existing?.attemptsCount ?: 0) + 1
        val newHighScore = if (existing != null) maxOf(existing.highestScore, score) else score

        val stats = TestAttemptStats(
            testKey = testKey,
            attemptsCount = newAttemptCount,
            highestScore = newHighScore,
            lastScore = score,
            totalMarks = totalMarks,
            accuracy = accuracy,
            percentile = percentile,
            lastAttemptTimestamp = System.currentTimeMillis()
        )
        attemptRecords[testKey] = stats
    }

    fun recordAttempt(
        testKey: String,
        score: Double,
        totalMarks: Double,
        accuracy: Int,
        percentile: Double,
        context: Context? = null
    ) {
        recordAttemptInMemory(testKey, score, totalMarks, accuracy, percentile)

        val ctx = context?.applicationContext
        if (ctx != null && prefs == null) {
            prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        val stats = attemptRecords[testKey]
        if (stats != null && prefs != null) {
            try {
                val obj = JSONObject()
                obj.put("testKey", stats.testKey)
                obj.put("attemptsCount", stats.attemptsCount)
                obj.put("highestScore", stats.highestScore)
                obj.put("lastScore", stats.lastScore)
                obj.put("totalMarks", stats.totalMarks)
                obj.put("accuracy", stats.accuracy)
                obj.put("percentile", stats.percentile)
                obj.put("lastAttemptTimestamp", stats.lastAttemptTimestamp)

                val p = prefs!!
                val existingKeys = p.getStringSet(KEY_ATTEMPT_KEYS, emptySet())?.toMutableSet() ?: mutableSetOf()
                existingKeys.add(testKey)
                p.edit()
                    .putStringSet(KEY_ATTEMPT_KEYS, existingKeys)
                    .putString("stat_$testKey", obj.toString())
                    .apply()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun getStats(vararg testKeys: String): TestAttemptStats? {
        for (key in testKeys) {
            val stats = attemptRecords[key]
            if (stats != null) return stats
        }
        return null
    }

    /**
     * Restores all past test attempts and scores from Firebase Firestore
     * when the user logs in or reinstalls the app on a new device.
     */
    fun syncCloudHistory(uid: String, context: Context?) {
        if (uid.isBlank()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("users").document(uid)
                    .collection("test_history").get().await()
                for (doc in snapshot.documents) {
                    val title = doc.getString("testTitle") ?: continue
                    val score = doc.getDouble("score") ?: 0.0
                    val totalMarks = doc.getDouble("totalMarks") ?: 100.0
                    val accuracy = doc.getLong("accuracy")?.toInt() ?: 0
                    val percentile = doc.getDouble("percentile") ?: 0.0
                    recordAttempt(title, score, totalMarks, accuracy, percentile, context)
                }
                Log.d("TestHistoryRepo", "Restored ${snapshot.size()} test history records from cloud for user $uid")
            } catch (e: Exception) {
                Log.w("TestHistoryRepo", "Cloud history sync notice: ${e.message}")
            }
        }
    }
}
