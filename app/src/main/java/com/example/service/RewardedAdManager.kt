package com.example.service

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.service.security.AdSecurityGuard
import com.example.service.security.AppSecurityManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions

object RewardedAdManager {
    private const val TAG = "RewardedAdManager"

    // Official 100% Real Production AdMob Rewarded Ad Unit ID (Strictly production ads only)
    private const val PRODUCTION_AD_UNIT_ID = "ca-app-pub-7374038442998632/3870702406"

    private var rewardedAd: RewardedAd? = null
    var isAdLoading by mutableStateOf(false)
        private set
    var isAdReady by mutableStateOf(false)
        private set
    var adStatusMessage by mutableStateOf("Ad ready")
        private set

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            AppSecurityManager.init(context)

            // Post to main looper to ensure thread safety
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    MobileAds.disableMediationAdapterInitialization(context)

                    MobileAds.initialize(context) { status ->
                        isInitialized = true
                        Log.d(TAG, "Google Mobile Ads SDK initialized for Production: $status")
                        loadRewardedAd(context)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in MobileAds.initialize: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Google Mobile Ads: ${e.message}", e)
        }
    }

    fun loadRewardedAd(
        context: Context,
        onLoaded: (() -> Unit)? = null,
        onFailed: ((String) -> Unit)? = null
    ) {
        if (!isInitialized) {
            init(context)
        }

        if (isAdLoading || (rewardedAd != null && isAdReady)) {
            if (isAdReady) onLoaded?.invoke()
            return
        }

        isAdLoading = true
        adStatusMessage = "Loading Production AdMob Ad..."
        val adRequest = AdRequest.Builder().build()

        try {
            RewardedAd.load(
                context,
                PRODUCTION_AD_UNIT_ID,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        isAdLoading = false
                        isAdReady = true
                        adStatusMessage = "Production AdMob Ad ready"
                        Log.d(TAG, "Production Rewarded Ad loaded successfully (Unit: $PRODUCTION_AD_UNIT_ID)!")
                        onLoaded?.invoke()
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.w(TAG, "Production Ad load failed (Code ${loadAdError.code}): ${loadAdError.message}")
                        rewardedAd = null
                        isAdLoading = false
                        isAdReady = false
                        val detailedReason = when (loadAdError.code) {
                            3 -> "Code 3 (NO_FILL): Google AdMob डैशबोर्ड में ऐप अप्रूव होना या ऐप स्टोर लिंक होना बाकी है।"
                            0 -> "Code 0 (INTERNAL_ERROR): AdMob सर्वर रिस्पांस या नेटवर्क इश्यू।"
                            1 -> "Code 1 (INVALID_REQUEST): Ad Unit ID या App ID अमान्य है।"
                            2 -> "Code 2 (NETWORK_ERROR): इंटरनेट कनेक्शन एरर।"
                            else -> "Code ${loadAdError.code}: ${loadAdError.message}"
                        }
                        adStatusMessage = detailedReason
                        onFailed?.invoke(detailedReason)
                    }
                }
            )
        } catch (e: Throwable) {
            rewardedAd = null
            isAdLoading = false
            isAdReady = false
            Log.e(TAG, "Exception during RewardedAd.load: ${e.message}", e)
            onFailed?.invoke(e.message ?: "Failed to load production ad")
        }
    }

    fun showRewardedAd(
        activity: Activity,
        testTargetId: String = "general_test",
        onRewardEarned: () -> Unit,
        onAdDismissed: (() -> Unit)? = null,
        onAdFailedToShow: (() -> Unit)? = null
    ) {
        val currentAd = rewardedAd
        if (currentAd != null) {
            var rewardEarned = false

            // Attach AdMob Server-Side Verification (SSV) options
            try {
                val currentUid = AuthManager.currentUser?.uid?.ifBlank { null }
                    ?: "user_${android.provider.Settings.Secure.getString(activity.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "anon"}"
                val ssvOptions = ServerSideVerificationOptions.Builder()
                    .setUserId(currentUid)
                    .setCustomData(testTargetId.trim().lowercase())
                    .build()
                currentAd.setServerSideVerificationOptions(ssvOptions)
                Log.d(TAG, "Configured Server-Side Verification for user: $currentUid, test: $testTargetId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed setting ServerSideVerificationOptions: ${e.message}")
            }

            // Create a cryptographically secure session before showing ad
            val adSessionId = AdSecurityGuard.createAdSession(testTargetId)

            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad dismissed")
                    rewardedAd = null
                    isAdReady = false
                    loadRewardedAd(activity.applicationContext)
                    onAdDismissed?.invoke()
                    if (rewardEarned) {
                        // Validate with AdSecurityGuard before unlocking
                        val validation = AdSecurityGuard.validateAndConsumeReward(adSessionId, testTargetId)
                        if (validation.isValid) {
                            Log.i(TAG, "Ad watch successfully validated! Granting unlock.")
                            onRewardEarned()
                        } else {
                            Log.w(TAG, "Ad reward rejected by security guard: ${validation.reason}")
                        }
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "Ad failed to show: ${adError.message}")
                    rewardedAd = null
                    isAdReady = false
                    loadRewardedAd(activity.applicationContext)
                    onAdFailedToShow?.invoke()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad showed full screen")
                }
            }

            currentAd.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.type} amount: ${rewardItem.amount}")
                rewardEarned = true
            }
        } else {
            Log.w(TAG, "Rewarded ad not loaded yet, loading now")
            loadRewardedAd(activity.applicationContext)
            onAdFailedToShow?.invoke()
        }
    }
}
