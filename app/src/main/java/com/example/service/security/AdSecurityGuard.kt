package com.example.service.security

import android.os.SystemClock
import android.util.Log
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Ad Security Guard:
 * Prevents Ad-Bypass, Instant Callback Spoofing, Fast Forward hacks,
 * and automated Reward Injection attacks.
 */
object AdSecurityGuard {
    private const val TAG = "AdSecurityGuard"

    // Minimum real-world milliseconds required for a rewarded ad to legitimately play
    // Set to 1500ms so test ads on fast emulators/devices are not falsely flagged
    private const val MIN_AD_WATCH_DURATION_MS = 1500L // 1.5 seconds threshold
    private const val SESSION_EXPIRY_MS = 120000L // 2 minutes maximum session validity

    private val activeAdSessions = ConcurrentHashMap<String, AdSession>()
    private val random = SecureRandom()

    data class AdSession(
        val sessionId: String,
        val testTargetId: String,
        val startTimeRealtimeMs: Long,
        val sessionNonce: Long
    )

    /**
     * Initiates a secured Ad session with a cryptographically secure token and timestamp.
     */
    fun createAdSession(testTargetId: String): String {
        val sessionId = UUID.randomUUID().toString()
        val nonce = random.nextLong()
        val session = AdSession(
            sessionId = sessionId,
            testTargetId = testTargetId.trim().lowercase(),
            startTimeRealtimeMs = SystemClock.elapsedRealtime(),
            sessionNonce = nonce
        )
        activeAdSessions[sessionId] = session
        Log.d(TAG, "Secured Ad session started for test: $testTargetId (Session: $sessionId)")
        return sessionId
    }

    /**
     * Validates if the ad watch was genuine, met minimum watch duration,
     * was not replayed or fast-forwarded by memory modding tools,
     * and returns the cryptographic unlock token.
     */
    fun validateAndConsumeReward(sessionId: String, testTargetId: String): RewardValidationResult {
        val session = activeAdSessions.remove(sessionId)
        if (session == null) {
            Log.w(TAG, "Ad verification failed: Invalid or expired session ID: $sessionId")
            return RewardValidationResult(
                isValid = false,
                reason = "Invalid or expired Ad Session"
            )
        }

        val currentTimeMs = SystemClock.elapsedRealtime()
        val elapsedMs = currentTimeMs - session.startTimeRealtimeMs

        // Check if session timed out
        if (elapsedMs > SESSION_EXPIRY_MS) {
            Log.w(TAG, "Ad verification failed: Session timed out (${elapsedMs}ms)")
            return RewardValidationResult(
                isValid = false,
                reason = "Session expired. Please watch ad again."
            )
        }

        // Check if watch duration was bypassed (Instant callback exploit)
        if (elapsedMs < MIN_AD_WATCH_DURATION_MS) {
            Log.w(TAG, "Ad verification alert: Instant ad callback detected (${elapsedMs}ms). Potential bypass attempt.")
            return RewardValidationResult(
                isValid = false,
                reason = "Ad playback was interrupted or skipped too quickly."
            )
        }

        // Verify target matches
        val cleanTarget = testTargetId.trim().lowercase()
        if (session.testTargetId != cleanTarget) {
            Log.w(TAG, "Ad verification failed: Target mismatch (${session.testTargetId} vs $cleanTarget)")
            return RewardValidationResult(
                isValid = false,
                reason = "Target mismatch"
            )
        }

        // Generate cryptographic proof
        val hmacToken = AppSecurityManager.generateTestHmacSignature(cleanTarget)
        Log.i(TAG, "Ad legitimately verified! Watch duration: ${elapsedMs / 1000.0}s. Test: $testTargetId")

        return RewardValidationResult(
            isValid = true,
            elapsedSeconds = (elapsedMs / 1000).toInt(),
            hmacToken = hmacToken
        )
    }

    data class RewardValidationResult(
        val isValid: Boolean,
        val reason: String? = null,
        val elapsedSeconds: Int = 0,
        val hmacToken: String? = null
    )
}
