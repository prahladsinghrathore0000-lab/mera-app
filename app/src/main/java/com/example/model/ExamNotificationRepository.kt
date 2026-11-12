package com.example.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.service.GitHubSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository to manage in-app exam notifications with cloud sync via GitHub.
 * Ensures admin broadcast notifications are synced across all users,
 * and eliminates hardcoded fake default notifications.
 */
object ExamNotificationRepository {
    private const val TAG = "ExamNotificationRepo"
    private const val PREFS_NAME = "exam_mitra_notifications"
    private const val KEY_NOTIFICATIONS_JSON = "saved_notifications_json"
    private const val CLOUD_FILE_NAME = "notifications.json"

    val notificationsList: SnapshotStateList<ExamNotification> = mutableStateListOf()
    private var prefs: SharedPreferences? = null

    // Known fake default IDs and titles to sanitize legacy cached data
    private val fakeNotificationIds = setOf(
        "n1", "n2", "n3", "n4",
        "default_n1", "default_n2", "default_n3", "default_n4"
    )

    private val fakeNotificationTitles = setOf(
        "SSC CGL 2026 Tier-1 Official Notification",
        "RRB NTPC City Intimation Slip Released",
        "State PSC Prelims Answer Key Out",
        "Daily Free Current Affairs Quiz Live"
    )

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadNotifications()
        }
        // Sync latest notifications from cloud on startup
        CoroutineScope(Dispatchers.IO).launch {
            syncFromCloud(context)
        }
    }

    private fun loadNotifications() {
        try {
            val jsonString = prefs?.getString(KEY_NOTIFICATIONS_JSON, null)
            notificationsList.clear()

            if (!jsonString.isNullOrBlank()) {
                val jsonArray = JSONArray(jsonString)
                val loadedList = mutableListOf<ExamNotification>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.optString("id", "").trim()
                    val title = obj.optString("title", "").trim()

                    // Filter out fake hardcoded notifications
                    if (id in fakeNotificationIds || title in fakeNotificationTitles) {
                        continue
                    }

                    if (id.isNotBlank() && title.isNotBlank()) {
                        loadedList.add(
                            ExamNotification(
                                id = id,
                                title = title,
                                category = obj.optString("category", "General"),
                                timeAgo = obj.optString("timeAgo", "Just now"),
                                description = obj.optString("description", ""),
                                isUrgent = obj.optBoolean("isUrgent", false)
                            )
                        )
                    }
                }
                notificationsList.addAll(loadedList)
                saveNotifications()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading notifications: ${e.message}")
        }
    }

    private fun saveNotifications() {
        try {
            val jsonArray = JSONArray()
            notificationsList.forEach { notif ->
                val obj = JSONObject().apply {
                    put("id", notif.id)
                    put("title", notif.title)
                    put("category", notif.category)
                    put("timeAgo", notif.timeAgo)
                    put("description", notif.description)
                    put("isUrgent", notif.isUrgent)
                }
                jsonArray.put(obj)
            }
            prefs?.edit()?.putString(KEY_NOTIFICATIONS_JSON, jsonArray.toString())?.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notifications: ${e.message}")
        }
    }

    /**
     * Fetch broadcast notifications from GitHub cloud repository
     */
    suspend fun syncFromCloud(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        withContext(Dispatchers.IO) {
            try {
                val config = GitHubSyncService.getSavedConfig(context)
                val result = GitHubSyncService.fetchFileContent(config, CLOUD_FILE_NAME)

                if (result.isSuccess) {
                    val jsonContent = result.getOrNull() ?: ""
                    if (jsonContent.isNotBlank()) {
                        val jsonArray = JSONArray(jsonContent.trim())
                        val freshList = mutableListOf<ExamNotification>()

                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val id = obj.optString("id", "").trim()
                            val title = obj.optString("title", "").trim()

                            if (id in fakeNotificationIds || title in fakeNotificationTitles) {
                                continue
                            }

                            if (id.isNotBlank() && title.isNotBlank()) {
                                freshList.add(
                                    ExamNotification(
                                        id = id,
                                        title = title,
                                        category = obj.optString("category", "General"),
                                        timeAgo = obj.optString("timeAgo", "Just now"),
                                        description = obj.optString("description", ""),
                                        isUrgent = obj.optBoolean("isUrgent", false)
                                    )
                                )
                            }
                        }

                        withContext(Dispatchers.Main) {
                            notificationsList.clear()
                            notificationsList.addAll(freshList)
                            saveNotifications()
                        }
                        Log.d(TAG, "Successfully synced ${freshList.size} notifications from GitHub")
                        onComplete?.invoke(true)
                        return@withContext
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing notifications from cloud: ${e.message}")
            }
            onComplete?.invoke(false)
        }
    }

    /**
     * Publish current notifications to GitHub cloud repository so all users get updates
     */
    fun publishToCloud(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonArray = JSONArray()
                notificationsList.forEach { notif ->
                    if (notif.id !in fakeNotificationIds && notif.title !in fakeNotificationTitles) {
                        val obj = JSONObject().apply {
                            put("id", notif.id)
                            put("title", notif.title)
                            put("category", notif.category)
                            put("timeAgo", notif.timeAgo)
                            put("description", notif.description)
                            put("isUrgent", notif.isUrgent)
                        }
                        jsonArray.put(obj)
                    }
                }

                val config = GitHubSyncService.getSavedConfig(context)
                val uploadResult = GitHubSyncService.uploadJsonFile(
                    config = config,
                    filePath = CLOUD_FILE_NAME,
                    jsonString = jsonArray.toString(2),
                    commitMessage = "Admin updated broadcast notifications"
                )
                if (uploadResult.isSuccess) {
                    Log.d(TAG, "Notifications successfully uploaded to GitHub")
                } else {
                    Log.e(TAG, "Failed to upload notifications to GitHub: ${uploadResult.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error publishing notifications to cloud: ${e.message}")
            }
        }
    }

    /**
     * Admin broadcast: Send a new notification to all users
     */
    fun addNotification(
        context: Context,
        title: String,
        description: String,
        category: String = "Important Update",
        isUrgent: Boolean = false,
        timeAgo: String = "Just now"
    ): ExamNotification {
        val newNotif = ExamNotification(
            id = "admin_notif_${System.currentTimeMillis()}",
            title = title.trim(),
            category = category.trim(),
            timeAgo = timeAgo,
            description = description.trim(),
            isUrgent = isUrgent
        )
        // Insert at top so users see latest first
        notificationsList.add(0, newNotif)
        saveNotifications()
        publishToCloud(context)
        return newNotif
    }

    /**
     * Admin delete: Delete an individual notification by ID
     */
    fun deleteNotification(context: Context, id: String): Boolean {
        val removed = notificationsList.removeAll { it.id == id }
        if (removed) {
            saveNotifications()
            publishToCloud(context)
        }
        return removed
    }

    /**
     * Admin clear all notifications
     */
    fun clearAllNotifications(context: Context) {
        notificationsList.clear()
        saveNotifications()
        publishToCloud(context)
    }

    /**
     * Reset/clear notifications list
     */
    fun resetToDefaults(context: Context) {
        notificationsList.clear()
        saveNotifications()
        publishToCloud(context)
    }
}
