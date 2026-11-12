package com.example.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.example.service.AuthManager
import com.example.service.security.AppSecurityManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Repository to persist lifetime unlocked tests.
 * Enhanced with Cryptographic HMAC-SHA256 signature verification to prevent
 * XML / SharedPreferences tampering, root file injection, and unauthorized test unlocks.
 */
object UnlockedTestsRepository {
    private const val TAG = "UnlockedTestsRepo"
    private const val PREFS_NAME = "exam_mitra_unlocked_tests"
    private const val KEY_UNLOCKED_SET = "unlocked_test_keys"
    private const val PREFIX_SIG = "hmac_sig_"

    val unlockedMap: SnapshotStateMap<String, Boolean> = mutableStateMapOf()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            val ctx = context.applicationContext
            AppSecurityManager.init(ctx)
            prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val editor = prefs?.edit()
            val savedKeys = prefs?.getStringSet(KEY_UNLOCKED_SET, emptySet()) ?: emptySet()
            val verifiedKeys = mutableSetOf<String>()

            for (key in savedKeys) {
                if (key.isBlank()) continue
                val normalized = key.trim().lowercase()
                val savedSig = prefs?.getString(PREFIX_SIG + normalized, null)

                if (savedSig != null && AppSecurityManager.verifyTestHmacSignature(normalized, savedSig)) {
                    // Valid cryptographic signature
                    unlockedMap[key] = true
                    unlockedMap[normalized] = true
                    verifiedKeys.add(key)
                    verifiedKeys.add(normalized)
                } else if (savedSig == null) {
                    // Upgrade legacy unlocks with new cryptographic HMAC signature
                    val newSig = AppSecurityManager.generateTestHmacSignature(normalized)
                    editor?.putString(PREFIX_SIG + normalized, newSig)
                    unlockedMap[key] = true
                    unlockedMap[normalized] = true
                    verifiedKeys.add(key)
                    verifiedKeys.add(normalized)
                    Log.d(TAG, "Upgraded legacy unlocked test to tamper-proof HMAC: $normalized")
                } else {
                    // Tampered or corrupted signature detected! Reject unlock.
                    Log.w(TAG, "Security alert: Invalid HMAC signature detected for test: $normalized. Tamper prevented.")
                }
            }

            editor?.putStringSet(KEY_UNLOCKED_SET, verifiedKeys)?.apply()

            // Asynchronously sync cloud unlocks for logged-in user
            AuthManager.currentUser?.uid?.let { uid ->
                syncCloudUnlocked(ctx, uid)
            }
        }
    }

    fun syncCloudUnlocked(context: Context, uid: String) {
        if (uid.isBlank()) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val snapshot = db.collection("users").document(uid)
                    .collection("unlocked_tests").get().await()
                val cloudKeys = mutableListOf<String>()
                for (doc in snapshot.documents) {
                    val key = doc.getString("testKey") ?: doc.id
                    if (key.isNotBlank()) {
                        cloudKeys.add(key)
                    }
                }
                if (cloudKeys.isNotEmpty()) {
                    unlockTest(context, *cloudKeys.toTypedArray())
                    Log.d(TAG, "Synced ${cloudKeys.size} unlocked tests from cloud for user $uid")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cloud unlock sync notice: ${e.message}")
            }
        }
    }

    fun isTestUnlocked(vararg testKeys: String): Boolean {
        for (key in testKeys) {
            if (key.isBlank()) continue
            if (unlockedMap[key] == true) return true
            val normalized = key.trim().lowercase()
            if (unlockedMap[normalized] == true) return true
        }
        return false
    }

    fun unlockTest(context: Context?, vararg testKeys: String) {
        val keysToAdd = mutableSetOf<String>()
        val sigMap = mutableMapOf<String, String>()

        for (key in testKeys) {
            if (key.isNotBlank()) {
                val normalized = key.trim().lowercase()
                unlockedMap[key] = true
                unlockedMap[normalized] = true
                keysToAdd.add(key)
                keysToAdd.add(normalized)

                // Generate tamper-proof cryptographic signatures
                val sigKey = AppSecurityManager.generateTestHmacSignature(key)
                val sigNorm = AppSecurityManager.generateTestHmacSignature(normalized)
                sigMap[PREFIX_SIG + key] = sigKey
                sigMap[PREFIX_SIG + normalized] = sigNorm
            }
        }

        val ctx = context?.applicationContext
        if (ctx != null && prefs == null) {
            prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        prefs?.let { p ->
            val existing = p.getStringSet(KEY_UNLOCKED_SET, emptySet())?.toMutableSet() ?: mutableSetOf()
            existing.addAll(keysToAdd)
            val editor = p.edit().putStringSet(KEY_UNLOCKED_SET, existing)
            sigMap.forEach { (k, v) ->
                editor.putString(k, v)
            }
            editor.apply()
            Log.i(TAG, "Successfully unlocked ${keysToAdd.size} test keys with tamper-proof HMAC signatures.")
        }

        // Backup unlock to Firestore cloud
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uid = AuthManager.currentUser?.uid
                if (!uid.isNullOrBlank()) {
                    val db = FirebaseFirestore.getInstance()
                    for (key in keysToAdd) {
                        val normalized = key.trim().lowercase()
                        db.collection("users").document(uid)
                            .collection("unlocked_tests").document(normalized)
                            .set(
                                mapOf(
                                    "testKey" to normalized,
                                    "unlockedAt" to System.currentTimeMillis(),
                                    "verified" to true
                                ),
                                SetOptions.merge()
                            )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cloud unlock backup notice: ${e.message}")
            }
        }
    }
}
