package com.example.model

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.example.service.AuthManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class LeaderboardEntry(
    val rank: Int = 0,
    val userId: String = "",
    val userName: String = "",
    val userLocation: String = "",
    val score: Double = 0.0,
    val totalMarks: Double = 100.0,
    val accuracy: Int = 0,
    val isCurrentUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class CutoffBenchmark(
    val cutoffUR: Double,
    val cutoffOBC: Double,
    val cutoffSC: Double,
    val averageScore: Double,
    val topperScore: Double,
    val totalMarks: Double
) {
    val formattedCutoffUR: String get() = String.format("%.2f", cutoffUR)
    val formattedCutoffOBC: String get() = String.format("%.2f", cutoffOBC)
    val formattedAverageScore: String get() = String.format("%.2f", averageScore)
    val formattedTopperScore: String get() = String.format("%.2f", topperScore)
}

object TestLeaderboardRepository {
    private const val TAG = "TestLeaderboardRepo"
    private const val PREFS_NAME = "exam_mitra_leaderboards_v4"
    private const val FIRESTORE_COLLECTION = "test_leaderboards"

    // In-memory cache for fast UI access
    private val memoryCache: SnapshotStateMap<String, List<LeaderboardEntry>> = mutableStateMapOf()

    /**
     * Dynamically calculates cutoff thresholds strictly scaled to the test's total marks.
     */
    fun computeCutoff(
        totalMarks: Double,
        realScores: List<Double> = emptyList(),
        currentUserScore: Double? = null
    ): CutoffBenchmark {
        val safeTotal = totalMarks.coerceAtLeast(1.0)
        val cutoffUR = (safeTotal * 0.68).coerceAtLeast(0.5)
        val cutoffOBC = (safeTotal * 0.62).coerceAtLeast(0.5)
        val cutoffSC = (safeTotal * 0.54).coerceAtLeast(0.5)

        val allScores = if (currentUserScore != null) realScores + currentUserScore else realScores
        val averageScore = if (allScores.isNotEmpty()) {
            allScores.average()
        } else {
            (safeTotal * 0.52).coerceAtLeast(0.5)
        }

        val topperScore = if (allScores.isNotEmpty()) {
            allScores.maxOrNull() ?: currentUserScore ?: (safeTotal * 0.90)
        } else {
            currentUserScore ?: (safeTotal * 0.90)
        }

        return CutoffBenchmark(
            cutoffUR = cutoffUR,
            cutoffOBC = cutoffOBC,
            cutoffSC = cutoffSC,
            averageScore = averageScore,
            topperScore = topperScore,
            totalMarks = safeTotal
        )
    }

    /**
     * Records the user's test result locally and to Firestore,
     * then merges real user submissions from the cloud to produce a 100% authentic leaderboard without duplicate entries.
     */
    suspend fun recordAndFetchLeaderboard(
        context: Context,
        testTitle: String,
        userScore: Double,
        totalMarks: Double,
        accuracy: Int
    ): Pair<List<LeaderboardEntry>, LeaderboardEntry> = withContext(Dispatchers.IO) {
        val user = AuthManager.currentUser
        val emailPrefix = user?.email?.substringBefore("@") ?: ""
        val displayName = when {
            user != null && user.displayName.isNotBlank() && !user.displayName.contains("Student", ignoreCase = true) -> user.displayName
            emailPrefix.isNotBlank() -> emailPrefix
            else -> "विद्यार्थी (You)"
        }
        val location = "जयपुर (RJ)"
        val guestId = getOrGenerateGuestId(context)
        val uid = user?.uid ?: guestId

        val currentUserEntry = LeaderboardEntry(
            userId = uid,
            userName = displayName,
            userLocation = location,
            score = userScore,
            totalMarks = totalMarks,
            accuracy = accuracy,
            isCurrentUser = true,
            timestamp = System.currentTimeMillis()
        )

        // 1. Save to local preferences (deduplicated)
        saveLocalAttempt(context, testTitle, currentUserEntry)

        // 2. Sync to cloud Firestore and fetch real submissions
        val cloudAttempts = syncCloudAttempt(testTitle, currentUserEntry, guestId)

        // 3. Combine cloud + local attempts
        val localAttempts = loadLocalAttempts(context, testTitle)
        val allRealAttempts = (cloudAttempts + localAttempts)

        // 4. Build 100% real ranked leaderboard with strict single-entry-per-user deduplication
        val leaderboard = buildRankedLeaderboard(allRealAttempts, currentUserEntry, emailPrefix, guestId)
        val userWithRank = leaderboard.firstOrNull { it.isCurrentUser } ?: currentUserEntry.copy(rank = leaderboard.size)

        // Cache in memory for instant Compose re-rendering
        withContext(Dispatchers.Main) {
            memoryCache[testTitle] = leaderboard
        }

        Pair(leaderboard, userWithRank)
    }

    /**
     * Fast synchronous getter that returns cached or local real submissions immediately.
     */
    fun getDynamicLeaderboard(
        context: Context,
        testTitle: String,
        userScore: Double,
        totalMarks: Double,
        accuracy: Int,
        airRank: Int
    ): Pair<List<LeaderboardEntry>, LeaderboardEntry> {
        val cached = memoryCache[testTitle]
        if (!cached.isNullOrEmpty()) {
            val userEntry = cached.firstOrNull { it.isCurrentUser } ?: LeaderboardEntry(
                rank = airRank,
                userName = AuthManager.currentUser?.displayName?.ifBlank { "विद्यार्थी (You)" } ?: "विद्यार्थी (You)",
                userLocation = "जयपुर (RJ)",
                score = userScore,
                totalMarks = totalMarks,
                accuracy = accuracy,
                isCurrentUser = true
            )
            return Pair(cached, userEntry)
        }

        val guestId = getOrGenerateGuestId(context)
        val localAttempts = loadLocalAttempts(context, testTitle)
        val user = AuthManager.currentUser
        val emailPrefix = user?.email?.substringBefore("@") ?: ""
        val displayName = when {
            user != null && user.displayName.isNotBlank() && !user.displayName.contains("Student", ignoreCase = true) -> user.displayName
            emailPrefix.isNotBlank() -> emailPrefix
            else -> "विद्यार्थी (You)"
        }
        val uid = user?.uid ?: guestId

        val currentUserEntry = LeaderboardEntry(
            rank = 1,
            userId = uid,
            userName = displayName,
            userLocation = "जयपुर (RJ)",
            score = userScore,
            totalMarks = totalMarks,
            accuracy = accuracy,
            isCurrentUser = true,
            timestamp = System.currentTimeMillis()
        )

        val leaderboard = buildRankedLeaderboard(localAttempts, currentUserEntry, emailPrefix, guestId)
        val userWithRank = leaderboard.firstOrNull { it.isCurrentUser } ?: currentUserEntry.copy(rank = 1)
        return Pair(leaderboard, userWithRank)
    }

    private fun normalizeUserKey(name: String): String {
        return name.trim().lowercase()
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")
            .replace(".", "")
            .replace("(you)", "")
            .replace("⭐", "")
            .replace("विद्यार्थी", "")
            .replace("अभ्यर्थी", "")
            .replace("student", "")
            .replace("aspirant", "")
    }

    private fun isCurrentUserAttempt(
        entry: LeaderboardEntry,
        currentUid: String,
        currentName: String,
        currentEmailPrefix: String,
        guestDeviceId: String
    ): Boolean {
        if (entry.isCurrentUser) return true
        val uid = entry.userId.trim()
        if (uid.isNotEmpty() && (uid == currentUid || (guestDeviceId.isNotEmpty() && uid == guestDeviceId))) {
            return true
        }

        val normEntryName = normalizeUserKey(entry.userName)
        val normCurrentName = normalizeUserKey(currentName)
        val normEmailPrefix = normalizeUserKey(currentEmailPrefix)

        // If entry has a generic empty normalized name, it's a device guest attempt
        if (normEntryName.isEmpty()) return true

        // Check if name matches current name or email prefix
        if (normCurrentName.isNotEmpty() && (normEntryName == normCurrentName || normEntryName.contains(normCurrentName) || normCurrentName.contains(normEntryName))) {
            return true
        }
        if (normEmailPrefix.isNotEmpty() && (normEntryName == normEmailPrefix || normEntryName.contains(normEmailPrefix) || normEmailPrefix.contains(normEntryName))) {
            return true
        }

        return false
    }

    private fun getCanonicalKey(
        entry: LeaderboardEntry,
        currentUid: String,
        currentName: String,
        currentEmailPrefix: String,
        guestDeviceId: String
    ): String {
        if (isCurrentUserAttempt(entry, currentUid, currentName, currentEmailPrefix, guestDeviceId)) {
            return "CANONICAL_CURRENT_USER"
        }

        val normName = normalizeUserKey(entry.userName)
        if (normName.isNotEmpty()) {
            return "USER_NAME_$normName"
        }

        val uid = entry.userId.trim()
        if (uid.isNotEmpty() && !uid.startsWith("user_")) {
            return "USER_UID_$uid"
        }

        return "CANONICAL_CURRENT_USER"
    }

    private fun buildRankedLeaderboard(
        realAttempts: List<LeaderboardEntry>,
        currentUserEntry: LeaderboardEntry,
        currentEmailPrefix: String = "",
        guestDeviceId: String = ""
    ): List<LeaderboardEntry> {
        val currentUid = currentUserEntry.userId
        val currentName = currentUserEntry.userName

        // Group by canonical user key to guarantee strictly ONE entry per real user
        val groupedByUser = mutableMapOf<String, LeaderboardEntry>()

        for (attempt in realAttempts) {
            val key = getCanonicalKey(attempt, currentUid, currentName, currentEmailPrefix, guestDeviceId)
            val isCurrent = (key == "CANONICAL_CURRENT_USER")
            val sanitizedAttempt = attempt.copy(
                isCurrentUser = isCurrent,
                userName = if (isCurrent) currentName else attempt.userName
            )

            val existing = groupedByUser[key]
            if (existing == null) {
                groupedByUser[key] = sanitizedAttempt
            } else {
                // Keep candidate's best attempt: Highest Score -> Highest Accuracy -> Most recent timestamp
                val isBetter = when {
                    sanitizedAttempt.score > existing.score -> true
                    sanitizedAttempt.score == existing.score && sanitizedAttempt.accuracy > existing.accuracy -> true
                    sanitizedAttempt.score == existing.score && sanitizedAttempt.accuracy == existing.accuracy && sanitizedAttempt.timestamp > existing.timestamp -> true
                    else -> false
                }
                if (isBetter) {
                    groupedByUser[key] = sanitizedAttempt
                }
            }
        }

        // Ensure current user's latest/best score is present
        val currentKey = "CANONICAL_CURRENT_USER"
        val existingUserEntry = groupedByUser[currentKey]
        if (existingUserEntry == null || currentUserEntry.score >= existingUserEntry.score) {
            groupedByUser[currentKey] = currentUserEntry.copy(isCurrentUser = true)
        }

        // Sort all distinct real candidates: Score DESC -> Accuracy DESC -> Timestamp ASC
        val sorted = groupedByUser.values.sortedWith(
            compareByDescending<LeaderboardEntry> { it.score }
                .thenByDescending { it.accuracy }
                .thenBy { it.timestamp }
        )

        // Assign clean, consecutive ranks (1, 2, 3...)
        return sorted.mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }
    }

    private fun getOrGenerateGuestId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var guestId = prefs.getString("local_device_guest_id", null)
        if (guestId.isNullOrBlank()) {
            guestId = "user_${UUID.randomUUID().toString().take(8)}"
            prefs.edit().putString("local_device_guest_id", guestId).apply()
        }
        return guestId
    }

    private fun saveLocalAttempt(context: Context, testTitle: String, entry: LeaderboardEntry) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val key = "test_${testTitle.hashCode()}"
            val existingJson = prefs.getString(key, "[]")
            val arr = JSONArray(existingJson)

            val currentUid = entry.userId
            val currentName = entry.userName.trim().lowercase()

            // Remove any previous attempt for the same user (by userId OR by userName)
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val uId = obj.optString("userId", "")
                val uName = obj.optString("userName", "").trim().lowercase()
                val isSame = (uId.isNotEmpty() && uId == currentUid) ||
                        (uName.isNotEmpty() && uName == currentName) ||
                        uName == "विद्यार्थी (you)"
                if (!isSame) {
                    newArr.put(obj)
                }
            }

            val obj = JSONObject().apply {
                put("userId", entry.userId)
                put("userName", entry.userName)
                put("userLocation", entry.userLocation)
                put("score", entry.score)
                put("totalMarks", entry.totalMarks)
                put("accuracy", entry.accuracy)
                put("timestamp", entry.timestamp)
            }
            newArr.put(obj)

            prefs.edit().putString(key, newArr.toString()).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save local attempt: ${e.message}")
        }
    }

    private fun loadLocalAttempts(context: Context, testTitle: String): List<LeaderboardEntry> {
        val results = mutableListOf<LeaderboardEntry>()
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val key = "test_${testTitle.hashCode()}"
            val json = prefs.getString(key, null) ?: return emptyList()
            val arr = JSONArray(json)
            val currentUid = AuthManager.currentUser?.uid ?: getOrGenerateGuestId(context)
            val currentName = AuthManager.currentUser?.displayName?.ifBlank { "विद्यार्थी (You)" } ?: "विद्यार्थी (You)"
            val normCurrentName = currentName.trim().lowercase()

            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val uid = obj.optString("userId", "")
                val name = obj.optString("userName", "अभ्यर्थी")
                val isCurrent = (uid.isNotEmpty() && uid == currentUid) ||
                        (name.trim().lowercase() == normCurrentName) ||
                        name.trim().lowercase() == "विद्यार्थी (you)"
                results.add(
                    LeaderboardEntry(
                        userId = uid,
                        userName = if (isCurrent) currentName else name,
                        userLocation = obj.optString("userLocation", "ऑनलाइन"),
                        score = obj.optDouble("score", 0.0),
                        totalMarks = obj.optDouble("totalMarks", 100.0),
                        accuracy = obj.optInt("accuracy", 0),
                        isCurrentUser = isCurrent,
                        timestamp = obj.optLong("timestamp", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load local attempts: ${e.message}")
        }
        return results
    }

    private suspend fun syncCloudAttempt(
        testTitle: String,
        entry: LeaderboardEntry,
        guestDeviceId: String = ""
    ): List<LeaderboardEntry> {
        val cloudList = mutableListOf<LeaderboardEntry>()
        try {
            val db = FirebaseFirestore.getInstance()
            val safeDocId = testTitle.replace("/", "_").replace(".", "_").take(80)

            // Save user attempt under the user's document ID to keep latest/best record
            val attemptData = hashMapOf(
                "userId" to entry.userId,
                "userName" to entry.userName,
                "userLocation" to entry.userLocation,
                "score" to entry.score,
                "totalMarks" to entry.totalMarks,
                "accuracy" to entry.accuracy,
                "timestamp" to entry.timestamp
            )

            db.collection(FIRESTORE_COLLECTION)
                .document(safeDocId)
                .collection("attempts")
                .document(entry.userId)
                .set(attemptData, SetOptions.merge())
                .await()

            // If user logged in and has a separate guest ID, clean old guest doc from cloud
            if (guestDeviceId.isNotEmpty() && guestDeviceId != entry.userId) {
                try {
                    db.collection(FIRESTORE_COLLECTION)
                        .document(safeDocId)
                        .collection("attempts")
                        .document(guestDeviceId)
                        .delete()
                } catch (_: Exception) {}
            }

            // Fetch top real attempts from cloud
            val snapshot = db.collection(FIRESTORE_COLLECTION)
                .document(safeDocId)
                .collection("attempts")
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()

            val currentUid = AuthManager.currentUser?.uid ?: entry.userId
            for (doc in snapshot.documents) {
                val uid = doc.getString("userId") ?: doc.id
                cloudList.add(
                    LeaderboardEntry(
                        userId = uid,
                        userName = doc.getString("userName") ?: "अभ्यर्थी",
                        userLocation = doc.getString("userLocation") ?: "ऑनलाइन",
                        score = doc.getDouble("score") ?: 0.0,
                        totalMarks = doc.getDouble("totalMarks") ?: 100.0,
                        accuracy = doc.getLong("accuracy")?.toInt() ?: 0,
                        isCurrentUser = (uid == currentUid || (guestDeviceId.isNotEmpty() && uid == guestDeviceId)),
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cloud leaderboard sync failed (offline or unauthenticated): ${e.message}")
        }
        return cloudList
    }
}
