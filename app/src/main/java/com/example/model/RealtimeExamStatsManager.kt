package com.example.model

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.example.service.AuthManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * RealtimeExamStatsManager:
 * Provides real-time, dynamic calculation of available test counts and active aspirants
 * for all competitive exams (SSC, Railway, State Exams, etc.).
 *
 * Replaces all static, hardcoded numbers with real, live data synchronized from:
 * 1. DynamicTestRepository (actual live/offline tests with questions)
 * 2. CustomChapterRepository (actual chapter tests with questions)
 * 3. Firebase Firestore 'exam_stats' collection (live real-time snapshot listener)
 * 4. Local attempt activity & user profiles
 */
object RealtimeExamStatsManager {
    private const val TAG = "RealtimeExamStatsMgr"
    private const val PREFS_NAME = "realtime_exam_stats_prefs"
    private const val FIRESTORE_COLLECTION = "exam_stats"

    // Reactive State Maps for Jetpack Compose: updates trigger instant recomposition
    val realAspirantsMap: SnapshotStateMap<String, Long> = mutableStateMapOf()

    private var isInitialized = false
    private var firestoreListener: ListenerRegistration? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Performance Cache: Prevents heavy list filtering and regex checks on every frame
    private val testCountCache = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private var cachedCustomTestsSize = -1
    private var cachedCustomChaptersSize = -1
    private var cachedRevision = -1

    fun invalidateCache() {
        synchronized(testCountCache) {
            testCountCache.clear()
            cachedCustomTestsSize = -1
            cachedCustomChaptersSize = -1
            cachedRevision = -1
        }
    }

    suspend fun preloadAllStats(context: Context) {
        invalidateCache()
        val allSubExams = examCategories.flatMap { it.subExams }
        allSubExams.forEach { subExam ->
            getRealTestCount(subExam.id, subExam.name, context)
        }
    }

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val ctx = context.applicationContext
        loadCachedStats(ctx)
        setupFirestoreRealtimeListener(ctx)
    }

    /**
     * Calculates the exact number of REAL tests available for a given subExam.
     * Includes both Full Mock Tests and Chapter-Wise Tests (built-in + uploaded custom sets)
     * that actually contain executable questions.
     * Cached efficiently so Compose recompositions never hang or stutter.
     */
    fun getRealTestCount(subExamId: String, subExamName: String = "", context: Context? = null): Int {
        val normId = DynamicTestRepository.normalizeSubExamId(subExamId)

        val curTestsSize = DynamicTestRepository.customTests.size
        val curChaptersSize = CustomChapterRepository.customChapters.size
        val repoRevision = CustomChapterRepository.revision

        synchronized(testCountCache) {
            if (curTestsSize != cachedCustomTestsSize || curChaptersSize != cachedCustomChaptersSize || repoRevision != cachedRevision) {
                testCountCache.clear()
                cachedCustomTestsSize = curTestsSize
                cachedCustomChaptersSize = curChaptersSize
                cachedRevision = repoRevision
            }
        }

        return testCountCache.getOrPut(normId) {
            val uniqueTestKeys = mutableSetOf<String>()

            // 1. Active Full Mock Tests for this exam
            val fullTests = DynamicTestRepository.getCustomTests(normId, "", "")
            fullTests.forEach { test ->
                val matchesExam = DynamicTestRepository.isNotLeakingIntoExam(
                    normId, test.title, test.questions, test.id, normId
                )
                if (matchesExam) {
                    val key = if (test.id.isNotBlank()) test.id else test.title.trim().lowercase()
                    uniqueTestKeys.add(key)
                }
            }

            // 2. Active Chapter Wise Sets (that contain questions) for this exam
            val fullSubjects = CustomChapterRepository.getFullSubjectsForExam(normId, context)
            fullSubjects.forEach { subject ->
                subject.chapters.forEach { chapter ->
                    val hasQuestions = chapter.questions.isNotEmpty() || chapter.totalQuestions > 0
                    val matchesExam = DynamicTestRepository.isNotLeakingIntoExam(
                        normId, chapter.title, chapter.questions, chapter.id, chapter.subExamId
                    )
                    if (hasQuestions && matchesExam) {
                        val key = if (chapter.id.isNotBlank()) chapter.id else chapter.title.trim().lowercase()
                        uniqueTestKeys.add(key)
                    }
                }
            }

            uniqueTestKeys.size
        }
    }

    /**
     * Retrieves the real number of active aspirants for a given subExam in real time.
     */
    fun getRealAspirantsCount(subExamId: String, subExamName: String = ""): Long {
        val normId = DynamicTestRepository.normalizeSubExamId(subExamId)
        val cloudCount = realAspirantsMap[normId] ?: 0L
        val localAspirant = getLocalAspirantContribution(normId)
        return maxOf(cloudCount, localAspirant)
    }

    /**
     * Formats real test count cleanly in Hindi: "0 टेस्ट", "1 टेस्ट", "5 टेस्ट"
     */
    fun formatTests(count: Int): String {
        return when (count) {
            0 -> "0 टेस्ट"
            1 -> "1 टेस्ट"
            else -> "$count टेस्ट"
        }
    }

    /**
     * Formats real aspirants count cleanly:
     * - 0 -> "0 Aspirants"
     * - 1 -> "1 Aspirant"
     * - 250 -> "250 Aspirants"
     * - 1500 -> "1.5K Aspirants"
     * - 150000 -> "1.5 Lakh Aspirants"
     */
    fun formatAspirants(count: Long): String {
        return when {
            count <= 0L -> "0 Aspirants"
            count == 1L -> "1 Aspirant"
            count < 1000L -> "$count Aspirants"
            count < 100000L -> {
                val k = count / 1000.0
                if (count % 1000L == 0L) {
                    "${count / 1000}K Aspirants"
                } else {
                    String.format(Locale.US, "%.1fK Aspirants", k)
                }
            }
            else -> {
                val lakh = count / 100000.0
                if (count % 100000L == 0L) {
                    "${count / 100000} Lakh Aspirants"
                } else {
                    String.format(Locale.US, "%.1f Lakh Aspirants", lakh)
                }
            }
        }
    }

    fun getRealAspirantsText(subExamId: String, subExamName: String = ""): String {
        val count = getRealAspirantsCount(subExamId, subExamName)
        return formatAspirants(count)
    }

    /**
     * Records real user activity for a subExam (viewing tests, starting a test, or submitting scores).
     * Immediately increments local state for instant feedback, and updates Firestore in real time.
     */
    fun recordAspirantActivity(context: Context, subExamId: String, subExamName: String = "") {
        val normId = DynamicTestRepository.normalizeSubExamId(subExamId)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sessionKey = "recorded_session_$normId"
        val alreadyRecordedThisSession = prefs.getBoolean(sessionKey, false)

        if (!alreadyRecordedThisSession) {
            val current = realAspirantsMap[normId] ?: 0L
            realAspirantsMap[normId] = current + 1
            prefs.edit().putBoolean(sessionKey, true).apply()
            saveCachedStats(context)
        }

        scope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val currentUid = AuthManager.currentUser?.uid ?: ""
                val docRef = db.collection(FIRESTORE_COLLECTION).document(normId)

                db.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    val existingCount = snapshot.getLong("aspirantsCount") ?: 0L
                    val newCount = if (!alreadyRecordedThisSession) existingCount + 1 else maxOf(existingCount, 1L)

                    val payload = hashMapOf<String, Any>(
                        "subExamId" to normId,
                        "examName" to subExamName.ifBlank { normId },
                        "aspirantsCount" to newCount,
                        "lastActiveTimestamp" to System.currentTimeMillis()
                    )
                    if (currentUid.isNotEmpty()) {
                        payload["lastActiveUserUid"] = currentUid
                    }
                    transaction.set(docRef, payload, SetOptions.merge())
                }.await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to record aspirant activity to Firestore: ${e.message}")
            }
        }
    }

    /**
     * Real-time Firestore snapshot listener:
     * Receives live updates whenever any aspirant anywhere joins, attempts a test,
     * or interacts with an exam, updating UI instantly without refresh!
     */
    private fun setupFirestoreRealtimeListener(context: Context) {
        try {
            val db = FirebaseFirestore.getInstance()
            firestoreListener?.remove()
            firestoreListener = db.collection(FIRESTORE_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Firestore exam_stats listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        for (doc in snapshot.documents) {
                            val subId = DynamicTestRepository.normalizeSubExamId(doc.id)
                            val count = doc.getLong("aspirantsCount")
                                ?: doc.getLong("activeAspirants")
                                ?: doc.getLong("count")
                                ?: 0L
                            if (count >= 0L) {
                                realAspirantsMap[subId] = count
                            }
                        }
                        saveCachedStats(context)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not initialize Firestore real-time listener: ${e.message}")
        }
    }

    /**
     * Parses cloud stats JSON (e.g. from GitHub sync or admin broadcast)
     */
    fun updateStatsFromJson(jsonString: String, context: Context? = null) {
        try {
            if (jsonString.startsWith("{")) {
                val root = JSONObject(jsonString)
                if (root.has("examStats")) {
                    val array = root.getJSONArray("examStats")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val id = DynamicTestRepository.normalizeSubExamId(obj.optString("subExamId"))
                        val count = obj.optLong("aspirants", obj.optLong("aspirantsCount", 0L))
                        if (id.isNotBlank() && count > 0L) {
                            realAspirantsMap[id] = count
                        }
                    }
                }
            } else if (jsonString.startsWith("[")) {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = DynamicTestRepository.normalizeSubExamId(obj.optString("subExamId"))
                    val count = obj.optLong("aspirants", obj.optLong("aspirantsCount", 0L))
                    if (id.isNotBlank() && count > 0L) {
                        realAspirantsMap[id] = count
                    }
                }
            }
            if (context != null) {
                saveCachedStats(context)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing exam stats json: ${e.message}")
        }
    }

    private fun getLocalAspirantContribution(normId: String): Long {
        val hasAttempt = TestHistoryRepository.attemptRecords.keys.any { key ->
            val clean = key.lowercase()
            clean.contains(normId) ||
            (normId == "ssc_delhi_police" && (clean.contains("delhi") || clean.contains("police"))) ||
            (normId == "rajasthan_police" && clean.contains("rajasthan")) ||
            (normId == "up_police" && clean.contains("up police")) ||
            (normId == "rrb_ntpc" && clean.contains("ntpc")) ||
            (normId == "ssc_cgl" && clean.contains("cgl"))
        }
        return if (hasAttempt) 1L else 0L
    }

    private fun loadCachedStats(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val allEntries = prefs.all
            for ((key, value) in allEntries) {
                if (key.startsWith("aspirants_") && value is Long) {
                    val subId = key.removePrefix("aspirants_")
                    realAspirantsMap[subId] = value
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error loading cached exam stats: ${e.message}")
        }
    }

    private fun saveCachedStats(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            for ((subId, count) in realAspirantsMap) {
                editor.putLong("aspirants_$subId", count)
            }
            editor.apply()
        } catch (e: Exception) {
            Log.w(TAG, "Error saving cached exam stats: ${e.message}")
        }
    }
}
