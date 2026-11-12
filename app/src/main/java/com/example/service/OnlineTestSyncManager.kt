package com.example.service

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.model.DynamicTestRepository
import com.example.model.OfflineTestRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Online Test Sync Manager:
 * Automatically synchronizes mock tests, papers, and questions directly from the Cloud / GitHub repository.
 * Whenever the admin uploads or publishes a new test, it becomes instantly available to all online users
 * without needing any app updates from the Play Store!
 */
object OnlineTestSyncManager {

    suspend fun deleteTestAssetsFromCloud(context: Context, questions: List<com.example.model.QuizQuestion>) {
        val config = GitHubSyncService.getSavedConfig(context)
        if (config.token.isBlank() || config.owner.isBlank() || config.repo.isBlank()) {
            return
        }
        
        // Extract all githubusercontent image URLs
        val githubPrefix = "https://raw.githubusercontent.com/${config.owner}/${config.repo}/${config.branch}/"
        val imagePaths = mutableSetOf<String>()
        
        for (q in questions) {
            val qText = q.question
            val imgRegex = Regex("""\[IMAGE: (.*?)\]""")
            val matches = imgRegex.findAll(qText)
            for (m in matches) {
                val url = m.groupValues[1]
                if (url.startsWith(githubPrefix)) {
                    val path = url.removePrefix(githubPrefix)
                    imagePaths.add(path)
                }
            }
        }
        
        for (path in imagePaths) {
            try {
                GitHubSyncService.deleteFile(config, path, "Admin deleted test: removing asset $path")
                Log.d(TAG, "Deleted asset from GitHub: $path")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete asset $path: ${e.message}")
            }
        }
        
        // Republish to update the master JSON files
        publishAllToCloud(context)
    }

    private const val TAG = "OnlineTestSyncMgr"
    private const val PREFS_NAME = "online_test_sync_prefs"
    private const val KEY_LAST_SYNC_TIME = "last_cloud_sync_timestamp"
    private const val KEY_LAST_SYNC_COUNT = "last_cloud_sync_count"
    private const val AUTO_SYNC_COOLDOWN_MS = 60 * 60 * 1000L // 1 hour cooldown for auto-sync

    var isSyncing by mutableStateOf(false)
        private set

    var lastSyncTimeMs by mutableLongStateOf(0L)
        private set

    var lastSyncMessage by mutableStateOf<String?>("क्लाउड सिंक तैयार")
        private set

    var lastNewTestsFetched by mutableIntStateOf(0)
        private set

    var hasStartupSyncCompleted by mutableStateOf(false)

    val liveDynamicTestsCount: Int
        get() = DynamicTestRepository.customTests.size

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val syncScope = CoroutineScope(Dispatchers.IO)
    private var isInitialized = false
    private var networkDebounceJob: Job? = null

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val ctx = context.applicationContext
        DynamicTestRepository.init(ctx)
        com.example.model.CustomChapterRepository.init(ctx)
        com.example.model.RealtimeExamStatsManager.init(ctx)

        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        lastSyncTimeMs = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        lastNewTestsFetched = prefs.getInt(KEY_LAST_SYNC_COUNT, 0)
    }

    /**
     * Automatically triggers a cloud sync only when needed during startup loading.
     * After startup, never runs in background to guarantee 120 FPS butter-smooth UI.
     */
    fun autoSyncIfOnline(context: Context) {
        if (hasStartupSyncCompleted || isSyncing) return
        if (!OfflineTestRepository.isNetworkAvailable(context)) return

        syncScope.launch {
            syncFromCloud(context, isUserTriggered = false)
        }
    }

    /**
     * Trigger explicit sync from UI components
     */
    fun forceSyncNow(context: Context, onResult: ((Boolean, String) -> Unit)? = null) {
        syncScope.launch {
            val result = syncFromCloud(context, isUserTriggered = true)
            withContext(Dispatchers.Main) {
                onResult?.invoke(result.first, result.second)
            }
        }
    }

    /**
     * Fetches all latest tests from Cloud / GitHub repository and merges into DynamicTestRepository
     */
    suspend fun syncFromCloud(
        context: Context,
        isUserTriggered: Boolean = false
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (isSyncing) {
            return@withContext Pair(false, "सिंकिंग पहले से जारी है...")
        }

        if (!OfflineTestRepository.isNetworkAvailable(context)) {
            val msg = "इंटरनेट कनेक्शन नहीं है। पहले से सेव किए गए ${DynamicTestRepository.customTests.size} टेस्ट उपलब्ध हैं।"
            lastSyncMessage = msg
            return@withContext Pair(false, msg)
        }

        val beforeTestCount = DynamicTestRepository.customTests.size
        val beforeChapCount = com.example.model.CustomChapterRepository.customChapters.size

        if (isUserTriggered) {
            isSyncing = true
            lastSyncMessage = "GitHub से नवीनतम टेस्ट लोड हो रहे हैं..."
        }

        val config = GitHubSyncService.getSavedConfig(context)
        var totalAdded = 0
        val scannedFiles = mutableSetOf<String>()

        try {
            val jsonFilesToFetch = mutableListOf<String>()
            if (!isUserTriggered) {
                // Fast path for background / non-user triggered: fetch master manifests directly in <1 second
                jsonFilesToFetch.add("data.json")
                jsonFilesToFetch.add("data_chapters.json")
                jsonFilesToFetch.add("custom_exam_chapters.json")
            } else {
                // 1. Discover all JSON files in the GitHub repository tree for manual sync
                val treeResult = GitHubSyncService.fetchRepoTree(config)
                if (treeResult.isSuccess) {
                    val allJsonFiles = treeResult.getOrNull() ?: emptyList()
                    val masterManifests = allJsonFiles.filter { it.equals("data.json", ignoreCase = true) || it.endsWith("/data.json", ignoreCase = true) }
                    val chapterManifests = allJsonFiles.filter { it.contains("chapter", ignoreCase = true) }
                    val otherJsonFiles = allJsonFiles.filter { it !in masterManifests && it !in chapterManifests }

                    jsonFilesToFetch.addAll(masterManifests)
                    jsonFilesToFetch.addAll(chapterManifests)
                    jsonFilesToFetch.addAll(otherJsonFiles)
                } else {
                    val cleanFolder = if (config.folderPath.isNotBlank()) config.folderPath.trim('/') else "data.json"
                    jsonFilesToFetch.add(cleanFolder)
                    jsonFilesToFetch.add("data.json")
                    jsonFilesToFetch.add("custom_exam_chapters.json")
                    jsonFilesToFetch.add("data_chapters.json")
                }
            }

            // 2. Fetch all discovered JSON files and collect all active test IDs/titles across the entire repository
            val activeGithubIds = mutableSetOf<String>()
            val activeGithubTitles = mutableSetOf<String>()
            val activeChapterIds = mutableSetOf<String>()
            val activeChapterTitles = mutableSetOf<String>()

            val fetchedPayloads = mutableListOf<Triple<String, String, String>>()

            for (filePath in jsonFilesToFetch.distinct()) {
                if (scannedFiles.contains(filePath)) continue
                scannedFiles.add(filePath)

                try {
                    val contentResult = GitHubSyncService.fetchFileContent(config, filePath)
                    if (contentResult.isSuccess) {
                        val content = contentResult.getOrNull() ?: ""
                        if (content.isNotBlank()) {
                            val inferredExam = inferExamFromPath(filePath)
                            fetchedPayloads.add(Triple(filePath, content, inferredExam))
                            try {
                                val trimmed = content.trim()
                                if (trimmed.startsWith("{")) {
                                    val root = org.json.JSONObject(trimmed)
                                    val arr = root.optJSONArray("tests") ?: root.optJSONArray("testPapers") ?: root.optJSONArray("papers") ?: root.optJSONArray("custom_tests") ?: root.optJSONArray("customTests")
                                    if (arr != null) {
                                        for (i in 0 until arr.length()) {
                                            val item = arr.optJSONObject(i)
                                            val pObj = item?.optJSONObject("testPaper") ?: item
                                            val id = pObj?.optString("id", "") ?: ""
                                            val title = pObj?.optString("title", "") ?: ""
                                            if (id.isNotBlank()) activeGithubIds.add(id.trim())
                                            if (title.isNotBlank()) activeGithubTitles.add(title.trim())
                                        }
                                    } else {
                                        // Single test paper JSON format (e.g., individual files inside tests/...)
                                        val pObj = root.optJSONObject("testPaper") ?: root
                                        val id = pObj.optString("id", "").ifBlank { root.optString("id", "") }
                                        val title = pObj.optString("title", "").ifBlank { root.optString("title", "") }
                                        if (id.isNotBlank()) activeGithubIds.add(id.trim())
                                        if (title.isNotBlank()) activeGithubTitles.add(title.trim())
                                    }
                                    val chapArr = root.optJSONArray("chapters") ?: root.optJSONArray("custom_chapters") ?: root.optJSONArray("customChapters")
                                    if (chapArr != null) {
                                        for (i in 0 until chapArr.length()) {
                                            val cObj = chapArr.optJSONObject(i)
                                            val id = cObj?.optString("id", "") ?: ""
                                            val title = cObj?.optString("title", "") ?: ""
                                            if (id.isNotBlank()) activeChapterIds.add(id.trim())
                                            if (title.isNotBlank()) activeChapterTitles.add(title.trim())
                                        }
                                    } else if (root.has("chapter") || root.has("chapterName") || root.has("isChapter")) {
                                        val cId = root.optString("id", "")
                                        val cTitle = root.optString("title", "").ifBlank { root.optString("chapterName", "") }
                                        if (cId.isNotBlank()) activeChapterIds.add(cId.trim())
                                        if (cTitle.isNotBlank()) activeChapterTitles.add(cTitle.trim())
                                    }
                                } else if (trimmed.startsWith("[")) {
                                    val arr = org.json.JSONArray(trimmed)
                                    for (i in 0 until arr.length()) {
                                        val item = arr.optJSONObject(i)
                                        val pObj = item?.optJSONObject("testPaper") ?: item
                                        val id = pObj?.optString("id", "") ?: ""
                                        val title = pObj?.optString("title", "") ?: ""
                                        if (id.isNotBlank()) activeGithubIds.add(id.trim())
                                        if (title.isNotBlank()) activeGithubTitles.add(title.trim())
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not fetch file $filePath: ${e.message}")
                }
            }

            // Unblock active GitHub IDs and titles from deleted lists before parsing
            for (act in activeGithubIds + activeGithubTitles) {
                if (act.isNotBlank()) {
                    DynamicTestRepository.deletedTestIds.remove(act)
                    DynamicTestRepository.deletedTestIds.remove(act.lowercase())
                }
            }
            for (act in activeChapterIds + activeChapterTitles) {
                if (act.isNotBlank()) {
                    com.example.model.CustomChapterRepository.deletedChapterIds.remove(act)
                    com.example.model.CustomChapterRepository.deletedChapterIds.remove("group_$act")
                }
            }

            // Now parse and merge each fetched file payload safely
            for ((filePath, content, inferredExam) in fetchedPayloads) {
                try {
                    if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
                        com.example.model.CustomChapterRepository.importFromJsonString(content, context, inferredExam)
                    }

                    if (content.contains("\"examStats\"") || content.contains("\"aspirants\"")) {
                        com.example.model.RealtimeExamStatsManager.updateStatsFromJson(content, context)
                    }

                    val count = DynamicTestRepository.parseAndMergeJson(
                        jsonString = content,
                        context = context,
                        persist = false,
                        updateUiSnapshot = false,
                        defaultSubExamId = inferredExam,
                        allKnownActiveIds = activeGithubIds,
                        allKnownActiveTitles = activeGithubTitles
                    )
                    totalAdded += count
                    Log.d(TAG, "Fetched and merged $count tests from $filePath")
                } catch (e: Exception) {
                    Log.w(TAG, "Could not parse payload from $filePath: ${e.message}")
                }
            }

            if (activeGithubIds.isNotEmpty() || activeGithubTitles.isNotEmpty()) {
                DynamicTestRepository.removeOrphanedTestsNotInGitHub(activeGithubIds, activeGithubTitles, context)
            }
            if (activeChapterIds.isNotEmpty() || activeChapterTitles.isNotEmpty()) {
                com.example.model.CustomChapterRepository.removeOrphanedChaptersNotInGitHub(activeChapterIds, activeChapterTitles, context)
            }

            val currentTestCount = DynamicTestRepository.customTests.size
            val currentChapCount = com.example.model.CustomChapterRepository.customChapters.size
            val hasChanges = totalAdded > 0 || currentTestCount != beforeTestCount || currentChapCount != beforeChapCount

            if (hasChanges) {
                // Sanitize, purge cross-contamination and persist all merged data to local storage so it stays permanently available offline
                DynamicTestRepository.purgeCorruptedExamCrossContamination(context)
                com.example.model.CustomChapterRepository.sanitizeExistingCustomChapters(context)
                DynamicTestRepository.saveToDisk(context)
                com.example.model.CustomChapterRepository.saveToDisk(context)
                DynamicTestRepository.triggerUiRevisionUpdate()
            }

            val now = System.currentTimeMillis()
            lastSyncTimeMs = now
            lastNewTestsFetched = totalAdded

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_LAST_SYNC_TIME, now)
                .putInt(KEY_LAST_SYNC_COUNT, totalAdded)
                .apply()

            // Sync broadcast notifications from cloud
            com.example.model.ExamNotificationRepository.syncFromCloud(context)

            val totalCustomChapters = com.example.model.CustomChapterRepository.customChapters.size
            val totalMockTests = DynamicTestRepository.customTests.size
            val successMsg = if (totalAdded > 0) {
                "🎉 GitHub से $totalAdded नए टेस्ट सफलतापूर्वक लोड हो गए! (कुल: ${totalMockTests + totalCustomChapters})"
            } else if (totalMockTests > 0 || totalCustomChapters > 0) {
                "✨ सभी टेस्ट अप-टू-डेट हैं! कुल ऑनलाइन टेस्ट: ${totalMockTests + totalCustomChapters}"
            } else {
                "✨ GitHub से कनेक्ट हो गया, पर रिपॉजिटरी में कोई JSON टेस्ट फाइल नहीं मिली।"
            }

            lastSyncMessage = successMsg
            isSyncing = false
            Log.d(TAG, "Cloud sync completed: $successMsg")
            return@withContext Pair(true, successMsg)
        } catch (e: Exception) {
            isSyncing = false
            val errorMsg = "सिंक त्रुटि: ${e.localizedMessage ?: "नेटवर्क समस्या"}"
            lastSyncMessage = errorMsg
            Log.e(TAG, "Sync exception: $errorMsg", e)
            return@withContext Pair(false, errorMsg)
        }
    }

    /**
     * Publishes all dynamic tests to Cloud / GitHub repository so all students receive it instantly.
     * Safely syncs from Cloud first to ensure no remote sets/chapters on GitHub are overwritten or lost.
     */
    suspend fun publishAllToCloud(context: Context): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val config = GitHubSyncService.getSavedConfig(context)
        if (config.token.isBlank() || config.owner.isBlank() || config.repo.isBlank()) {
            return@withContext Pair(false, "कृपया पहले Profile में जाकर Admin GitHub Token और Repo सेट करें।")
        }

        // First sync all remote tests from GitHub into memory to prevent losing/overwriting remote sets
        try {
            syncFromCloud(context, isUserTriggered = false)
        } catch (e: Exception) {
            Log.w(TAG, "Pre-publish sync warning: ${e.message}")
        }

        val masterJson = DynamicTestRepository.exportAllToMasterJson()
        val targetFile = if (config.folderPath.endsWith(".json", true)) config.folderPath.trim('/') else "data.json"

        val uploadResult = GitHubSyncService.uploadJsonFile(
            config = config,
            filePath = targetFile,
            jsonString = masterJson,
            commitMessage = "Live Sync: Update ${DynamicTestRepository.customTests.size} tests manifest (${System.currentTimeMillis()})"
        )

        val chaptersJson = com.example.model.CustomChapterRepository.exportToJsonString(context)
        if (chaptersJson != "{}" && chaptersJson.isNotBlank()) {
            val chaptersTargetFile = if (targetFile.endsWith(".json", true)) targetFile.replace(".json", "_chapters.json", true) else "custom_exam_chapters.json"
            GitHubSyncService.uploadJsonFile(
                config = config,
                filePath = chaptersTargetFile,
                jsonString = chaptersJson,
                commitMessage = "Live Sync: Update chapter tests metadata (${System.currentTimeMillis()})"
            )
        }

        if (uploadResult.isSuccess) {
            val now = System.currentTimeMillis()
            lastSyncTimeMs = now
            val msg = "🎉 टेस्ट क्लाउड पर लाइव पब्लिश हो गया! सभी ऑनलाइन विद्यार्थियों को बिना ऐप अपडेट किए यह तुरंत दिखेगा।"
            lastSyncMessage = msg
            return@withContext Pair(true, msg)
        } else {
            return@withContext Pair(false, "क्लाउड पब्लिशिंग फेल: ${uploadResult.message}")
        }
    }

    /**
     * Called whenever an Admin edits questions of ANY test (Full Mock Test or Chapter-Wise Test).
     * 1. Purges stale local offline database/cache from the phone so students and admin receive clean online data
     * 2. Updates GitHub repository with fresh edited questions
     * 3. Syncs and refreshes active memory state
     */
    suspend fun onTestEdited(
        context: Context,
        testId: String,
        testTitle: String,
        subExamId: String,
        isChapterWise: Boolean,
        updatedQuestions: List<com.example.model.QuizQuestion>
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        // 1. Purge stale local cache from phone storage and update with fresh questions
        val testKeys = listOf(
            testId,
            testTitle,
            "${subExamId}_${testId}",
            "${subExamId}_${testTitle}",
            "chap_$testId",
            "chap_$testTitle"
        )
        OfflineTestRepository.purgeAndRefreshOfflineCache(context, testKeys, updatedQuestions)

        // 2. Publish updated JSON files to GitHub
        val publishResult = publishAllToCloud(context)

        // 3. Trigger immediate local sync to refresh active state in memory
        try {
            syncFromCloud(context, isUserTriggered = false)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing from cloud after edit: ${e.message}")
        }

        return@withContext publishResult
    }

    private fun inferExamFromPath(path: String): String {
        val p = path.lowercase()
        return when {
            p.contains("delhi") || p.contains("dp_") -> "ssc_delhi_police"
            p.contains("up_police") || p.contains("uppolice") || p.contains("uttar_pradesh") -> "up_police"
            p.contains("rajasthan_police") || p.contains("rajpolice") || p.contains("rajasthan") -> "rajasthan_police"
            p.contains("rpf") -> "rpf_si"
            p.contains("ntpc") -> "rrb_ntpc"
            p.contains("group_d") || p.contains("groupd") || p.contains("rrc") -> "rrb_group_d"
            p.contains("alp") || p.contains("technician") -> "rrb_alp"
            p.contains("rrb_je") || p.contains("je") -> "rrb_je"
            p.contains("cgl") -> "ssc_cgl"
            p.contains("chsl") -> "ssc_chsl"
            p.contains("cpo") -> "ssc_cpo"
            p.contains("mts") -> "ssc_mts"
            p.contains("patwari") -> "patwari"
            p.contains("lekhpal") -> "lekhpal"
            p.contains("cet") -> "rajasthan_cet"
            p.contains("gd") || p.contains("ssc_gd") -> "ssc_gd"
            else -> ""
        }
    }
}
