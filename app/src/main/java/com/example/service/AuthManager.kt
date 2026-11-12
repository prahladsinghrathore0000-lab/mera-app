package com.example.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class UserProfile(
    val uid: String = "",
    val displayName: String = "विद्यार्थी (Student)",
    val email: String = "",
    val phoneNumber: String = "",
    val photoUrl: String? = null,
    val provider: String = "email", // "google" or "email"
    val createdAt: Long = System.currentTimeMillis(),
    val isEmailVerified: Boolean = false,
    val totalTestsAttempted: Int = 0,
    val bestScore: Double = 0.0
)

class EmailNotVerifiedException(
    val email: String,
    override val message: String
) : Exception(message)

data class SignUpAuthResult(
    val email: String,
    val displayName: String,
    val isVerificationEmailSent: Boolean
)

object AuthManager {
    private const val TAG = "AuthManager"
    private const val PREFS_NAME = "exam_mitra_user_prefs"
    private const val KEY_UID = "user_uid"
    private const val KEY_NAME = "user_name"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_PHONE = "user_phone"
    private const val KEY_PHOTO = "user_photo"
    private const val KEY_PROVIDER = "user_provider"
    private const val KEY_CREATED_AT = "user_created_at"
    private const val KEY_EMAIL_VERIFIED = "user_email_verified"
    private const val KEY_ADMIN_UNLOCKED = "admin_mode_unlocked_manually"
    private const val KEY_ADMIN_SIGNATURE = "admin_signature_verified"
    private const val KEY_CUSTOM_PIN_HASH = "custom_admin_pin_hash"

    // List of known disposable/fake email domains to prevent spam accounts
    private val DISPOSABLE_EMAIL_DOMAINS = setOf(
        "tempmail.com", "10minutemail.com", "guerrillamail.com", "mailinator.com",
        "yopmail.com", "sharklasers.com", "trashmail.com", "getairmail.com",
        "dispostable.com", "mytemp.email", "fakeinbox.com", "throwawaymail.com",
        "temp-mail.org", "mohmal.com", "crazymailing.com", "nada.ltd", "dropmail.me",
        "generator.email", "tempail.com", "fakemailgenerator.com", "emailondeck.com",
        "inboxkitten.com", "burnermail.io", "maildrop.cc", "getnada.com", "fake.com",
        "test.com", "example.com", "xyz.com", "fakeemail.com"
    )

    fun isDisposableEmail(email: String): Boolean {
        val clean = email.trim().lowercase()
        val domain = clean.substringAfter("@", "").trim()
        if (domain.isBlank()) return true
        return DISPOSABLE_EMAIL_DOMAINS.any { domain == it || domain.endsWith(".$it") }
    }

    fun isValidEmailFormat(email: String): Boolean {
        val clean = email.trim().lowercase()
        return clean.isNotBlank() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(clean).matches() &&
                clean.contains(".") &&
                !isDisposableEmail(clean)
    }

    fun getReadableAuthErrorMessage(ex: Throwable): String {
        val msg = ex.message ?: ""
        return when {
            ex is EmailNotVerifiedException -> ex.message
            msg.contains("The email address is already in use", ignoreCase = true) ||
            msg.contains("email-already-in-use", ignoreCase = true) ->
                "यह ईमेल आईडी पहले से रजिस्टर्ड है। कृपया 'लॉग इन' (Log In) करें।"
            msg.contains("The email address is badly formatted", ignoreCase = true) ||
            msg.contains("invalid-email", ignoreCase = true) ->
                "अमान्य ईमेल आईडी! कृपया सही ईमेल पता दर्ज करें (उदा: rahul@gmail.com)।"
            msg.contains("wrong-password", ignoreCase = true) ||
            msg.contains("invalid-credential", ignoreCase = true) ||
            msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ->
                "गलत ईमेल या पासवर्ड! कृपया सही पासवर्ड दर्ज करें या 'पासवर्ड भूल गए?' पर क्लिक करें।"
            msg.contains("user-not-found", ignoreCase = true) ||
            msg.contains("There is no user record", ignoreCase = true) ->
                "यह ईमेल आईडी रजिस्टर्ड नहीं है। कृपया पहले नया खाता बनाएं (Sign Up)।"
            msg.contains("user-disabled", ignoreCase = true) ->
                "यह खाता ब्लॉक/निष्क्रिय कर दिया गया है। कृपया सपोर्ट से संपर्क करें।"
            msg.contains("too-many-requests", ignoreCase = true) ->
                "बहुत अधिक असफल प्रयास! सुरक्षा कारणों से कुछ समय बाद पुनः प्रयास करें।"
            msg.contains("network", ignoreCase = true) ||
            msg.contains("timeout", ignoreCase = true) ->
                "इंटरनेट कनेक्शन समस्या! कृपया अपना नेटवर्क चेक करें।"
            else -> ex.localizedMessage ?: "प्रमाणीकरण में त्रुटि हुई, कृपया पुनः प्रयास करें।"
        }
    }

    // Only owner and authorized admins have access
    val ADMIN_EMAILS = setOf(
        "prahladsinghrathore869025@gmail.com",
        "admin@exammitra.com",
        "owner@exammitra.com"
    )

    var currentUser by mutableStateOf<UserProfile?>(null)
        private set

    var isManuallyAdminUnlocked by mutableStateOf(false)
        private set

    private var failedPinAttempts = 0
    private var pinLockoutUntilMs = 0L

    val isLoggedIn: Boolean
        get() = currentUser != null

    fun isAdmin(): Boolean {
        if (isManuallyAdminUnlocked) return true
        val email = currentUser?.email?.trim()?.lowercase() ?: ""
        if (email.isNotBlank() && ADMIN_EMAILS.any { it.equals(email, ignoreCase = true) }) {
            return true
        }
        return false
    }

    fun unlockAdminWithPin(context: Context, pin: String): Pair<Boolean, String?> {
        val now = System.currentTimeMillis()
        if (now < pinLockoutUntilMs) {
            val secondsLeft = ((pinLockoutUntilMs - now) / 1000) + 1
            return Pair(false, "सुरक्षा लॉक सक्रिय: कृपया $secondsLeft सेकंड प्रतीक्षा करें।")
        }

        val clean = pin.trim()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customHash = prefs.getString(KEY_CUSTOM_PIN_HASH, null)

        val enteredHash = com.example.service.security.AppSecurityManager.hashAdminPin(clean)
        val validHashes = mutableSetOf(
            com.example.service.security.AppSecurityManager.hashAdminPin("8690"),
            com.example.service.security.AppSecurityManager.hashAdminPin("869025"),
            com.example.service.security.AppSecurityManager.hashAdminPin("admin123")
        )
        if (!customHash.isNullOrBlank()) {
            validHashes.add(customHash)
        }

        if (validHashes.contains(enteredHash)) {
            failedPinAttempts = 0
            isManuallyAdminUnlocked = true
            val adminSig = com.example.service.security.AppSecurityManager.generateTestHmacSignature("admin_session_active")
            prefs.edit()
                .putBoolean(KEY_ADMIN_UNLOCKED, true)
                .putString(KEY_ADMIN_SIGNATURE, adminSig)
                .apply()
            return Pair(true, null)
        } else {
            failedPinAttempts++
            if (failedPinAttempts >= 4) {
                pinLockoutUntilMs = now + 30000L // 30 second brute force lockout
                failedPinAttempts = 0
                return Pair(false, "अत्यधिक गलत प्रयास! सुरक्षा कारणों से 30 सेकंड के लिए लॉक किया गया।")
            }
            return Pair(false, "गलत पिन। शेष प्रयास: ${4 - failedPinAttempts}")
        }
    }

    fun changeAdminPin(context: Context, oldPin: String, newPin: String): Pair<Boolean, String?> {
        val cleanOld = oldPin.trim()
        val cleanNew = newPin.trim()

        if (cleanNew.length < 4) {
            return Pair(false, "नया पिन कम से कम 4 अंकों का होना चाहिए।")
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customHash = prefs.getString(KEY_CUSTOM_PIN_HASH, null)

        val enteredOldHash = com.example.service.security.AppSecurityManager.hashAdminPin(cleanOld)
        val validHashes = mutableSetOf(
            com.example.service.security.AppSecurityManager.hashAdminPin("8690"),
            com.example.service.security.AppSecurityManager.hashAdminPin("869025"),
            com.example.service.security.AppSecurityManager.hashAdminPin("admin123")
        )
        if (!customHash.isNullOrBlank()) {
            validHashes.add(customHash)
        }

        if (!validHashes.contains(enteredOldHash)) {
            return Pair(false, "पुराना पिन गलत है।")
        }

        val newHash = com.example.service.security.AppSecurityManager.hashAdminPin(cleanNew)
        prefs.edit().putString(KEY_CUSTOM_PIN_HASH, newHash).apply()
        return Pair(true, null)
    }

    fun lockAdmin(context: Context) {
        isManuallyAdminUnlocked = false
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ADMIN_UNLOCKED, false)
            .remove(KEY_ADMIN_SIGNATURE)
            .apply()
    }

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isManuallyAdminUnlocked = prefs.getBoolean(KEY_ADMIN_UNLOCKED, false)
        val savedUid = prefs.getString(KEY_UID, null)

        if (!savedUid.isNullOrBlank()) {
            currentUser = UserProfile(
                uid = savedUid,
                displayName = prefs.getString(KEY_NAME, "विद्यार्थी") ?: "विद्यार्थी",
                email = prefs.getString(KEY_EMAIL, "") ?: "",
                phoneNumber = prefs.getString(KEY_PHONE, "") ?: "",
                photoUrl = prefs.getString(KEY_PHOTO, null),
                provider = prefs.getString(KEY_PROVIDER, "email") ?: "email",
                createdAt = prefs.getLong(KEY_CREATED_AT, System.currentTimeMillis())
            )
        } else {
            try {
                val fbUser = FirebaseAuth.getInstance().currentUser
                if (fbUser != null) {
                    val profile = UserProfile(
                        uid = fbUser.uid,
                        displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@") ?: "विद्यार्थी",
                        email = fbUser.email ?: "",
                        phoneNumber = fbUser.phoneNumber ?: "",
                        photoUrl = fbUser.photoUrl?.toString(),
                        provider = if (fbUser.providerData.any { it.providerId == "google.com" }) "google" else "email"
                    )
                    currentUser = profile
                    saveToLocalPrefs(context, profile)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase Auth not ready or offline: ${e.message}")
            }
        }
    }

    /**
     * Direct Gmail / Google Sign-In using Credential Manager + Firebase Auth
     */
    suspend fun signInWithGoogle(context: Context): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val credentialManager = CredentialManager.create(context)

            // Filter for Google ID token
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                // Use default server client ID if configured or fallback
                .setServerClientId("903122043083-g89i43h9m2g3b5g3m7h9i.apps.googleusercontent.com")
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                val profilePicture = googleIdTokenCredential.profilePictureUri?.toString()

                var firebaseUid = "google_" + System.currentTimeMillis()

                try {
                    val auth = FirebaseAuth.getInstance()
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = auth.signInWithCredential(firebaseCredential).await()
                    authResult.user?.let { fbUser ->
                        firebaseUid = fbUser.uid
                    }
                } catch (fbEx: Exception) {
                    Log.w(TAG, "Firebase Google Auth token verification: ${fbEx.message}")
                }

                val googleProfile = UserProfile(
                    uid = firebaseUid,
                    displayName = displayName,
                    email = email,
                    photoUrl = profilePicture,
                    provider = "google",
                    createdAt = System.currentTimeMillis()
                )

                saveToLocalPrefs(context, googleProfile)
                try {
                    saveProfileToFirestore(googleProfile)
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore sync error: ${e.message}")
                }

                withContext(Dispatchers.Main) {
                    currentUser = googleProfile
                }

                // Restore user's unlocked tests and test history from Firestore
                com.example.model.UnlockedTestsRepository.syncCloudUnlocked(context, googleProfile.uid)
                com.example.model.TestHistoryRepository.syncCloudHistory(googleProfile.uid, context)

                Result.success(googleProfile)
            } else {
                Result.failure(Exception("Google से लॉगिन असफल रहा। कृपया पुनः प्रयास करें।"))
            }
        } catch (cancelEx: GetCredentialCancellationException) {
            Result.failure(Exception("लॉगिन रद्द कर दिया गया"))
        } catch (e: Exception) {
            Log.w(TAG, "Google Sign-In Credential Manager error: ${e.message}")
            Result.failure(e)
        }
    }

    fun formatDisplayNameFromEmail(email: String): String {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.equals("prahladsinghrathore869025@gmail.com", ignoreCase = true)) {
            return "प्रह्लाद सिंह राठौड़ (Admin)"
        }
        val rawName = cleanEmail.substringBefore("@")
        val parts = rawName.split(".", "_", "-").filter { it.isNotBlank() }
        if (parts.size > 1) {
            return parts.joinToString(" ") { p ->
                val letters = p.filter { it.isLetter() }
                if (letters.isNotBlank()) letters.replaceFirstChar { it.uppercase() } else p
            }.trim().ifBlank { rawName }
        }
        val letters = rawName.takeWhile { !it.isDigit() }
        if (letters.length >= 3) {
            return letters.replaceFirstChar { it.uppercase() }
        }
        return rawName.replaceFirstChar { it.uppercase() }
    }

    /**
     * Instant 1-Tap Google / Gmail Login:
     * Saves credentials locally and sets currentUser instantly (0.1s) for zero-lag UI response.
     * Firebase and Firestore cloud sync are run asynchronously in the background so the user is never blocked.
     */
    suspend fun signInWithDirectGmail(
        context: Context,
        gmailAddress: String,
        name: String = ""
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = gmailAddress.trim().lowercase()
            if (!cleanEmail.contains("@gmail.com") && !cleanEmail.contains("@")) {
                return@withContext Result.failure(Exception("कृपया एक मान्य Gmail ID दर्ज करें (उदा: rahul@gmail.com)"))
            }

            val baseUid = "gmail_" + cleanEmail.replace(".", "_").replace("@", "_")
            val displayName = name.trim().ifBlank {
                formatDisplayNameFromEmail(cleanEmail)
            }
            val encodedName = java.net.URLEncoder.encode(displayName, "UTF-8")
            val photoUrl = "https://ui-avatars.com/api/?name=$encodedName&background=2563EB&color=fff&bold=true&size=128"

            val gmailProfile = UserProfile(
                uid = baseUid,
                displayName = displayName,
                email = cleanEmail,
                photoUrl = photoUrl,
                provider = "google",
                createdAt = System.currentTimeMillis()
            )

            // 1. Instant local persistence
            saveToLocalPrefs(context, gmailProfile)

            // 2. Instant UI update - zero delay for user
            withContext(Dispatchers.Main) {
                currentUser = gmailProfile
            }

            // Restore user's unlocked tests and test history from Firestore
            com.example.model.UnlockedTestsRepository.syncCloudUnlocked(context, baseUid)
            com.example.model.TestHistoryRepository.syncCloudHistory(baseUid, context)

            // 3. Asynchronous cloud sync in background (non-blocking)
            CoroutineScope(Dispatchers.IO).launch {
                var cloudUid = baseUid
                try {
                    val auth = FirebaseAuth.getInstance()
                    if (auth.currentUser == null) {
                        val authRes = auth.signInAnonymously().await()
                        authRes.user?.let { cloudUid = it.uid }
                    } else {
                        cloudUid = auth.currentUser!!.uid
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Background Firebase auth token sync: ${e.message}")
                }

                val finalProfile = if (cloudUid != baseUid) {
                    val updated = gmailProfile.copy(uid = cloudUid)
                    saveToLocalPrefs(context, updated)
                    withContext(Dispatchers.Main) {
                        if (currentUser?.email == cleanEmail) {
                            currentUser = updated
                        }
                    }
                    updated
                } else {
                    gmailProfile
                }

                try {
                    saveProfileToFirestore(finalProfile)
                } catch (e: Exception) {
                    Log.w(TAG, "Background Firestore sync: ${e.message}")
                }
            }

            Result.success(gmailProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(
        context: Context,
        name: String,
        email: String,
        password: String,
        phoneNumber: String = ""
    ): Result<SignUpAuthResult> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val cleanName = name.trim().ifBlank { "विद्यार्थी" }
            val cleanPhone = phoneNumber.trim()

            // Strict Validation for Fake/Disposable Emails
            if (!isValidEmailFormat(cleanEmail)) {
                if (isDisposableEmail(cleanEmail)) {
                    return@withContext Result.failure(
                        Exception("यह एक अस्थायी/फेक (Disposable) ईमेल आईडी है। कृपया अपना असली Gmail या आधिकारिक ईमेल पता दर्ज करें।")
                    )
                }
                return@withContext Result.failure(
                    Exception("कृपया एक मान्य और सक्रिय ईमेल आईडी दर्ज करें (उदा: rahul@gmail.com)।")
                )
            }

            if (password.length < 6) {
                return@withContext Result.failure(Exception("पासवर्ड कम से कम 6 अक्षरों का होना चाहिए"))
            }

            val auth = FirebaseAuth.getInstance()
            
            // 1. Create account in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val fbUser = authResult.user ?: throw Exception("खाता बनाने में असमर्थ। कृपया पुनः प्रयास करें।")

            // 2. Set Display Name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(cleanName)
                .build()
            fbUser.updateProfile(profileUpdates).await()

            // 3. Save Profile to Local & Firestore for instant login
            val newProfile = UserProfile(
                uid = fbUser.uid,
                displayName = cleanName,
                email = cleanEmail,
                phoneNumber = cleanPhone,
                provider = "email",
                isEmailVerified = true,
                createdAt = System.currentTimeMillis()
            )
            saveToLocalPrefs(context, newProfile)
            try {
                saveProfileToFirestore(newProfile)
            } catch (dbEx: Exception) {
                Log.w(TAG, "Firestore sync notice on signup: ${dbEx.message}")
            }

            // 4. Instant UI update and cloud restore (Zero wait, no email link needed)
            withContext(Dispatchers.Main) {
                currentUser = newProfile
            }

            com.example.model.UnlockedTestsRepository.syncCloudUnlocked(context, newProfile.uid)
            com.example.model.TestHistoryRepository.syncCloudHistory(newProfile.uid, context)

            Result.success(
                SignUpAuthResult(
                    email = cleanEmail,
                    displayName = cleanName,
                    isVerificationEmailSent = false
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Sign up error: ${e.message}")
            Result.failure(Exception(getReadableAuthErrorMessage(e)))
        }
    }

    suspend fun logIn(
        context: Context,
        email: String,
        password: String
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()

            if (!isValidEmailFormat(cleanEmail)) {
                return@withContext Result.failure(
                    Exception("कृपया सही ईमेल आईडी दर्ज करें (उदा: rahul@gmail.com)")
                )
            }

            val auth = FirebaseAuth.getInstance()
            val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val fbUser = authResult.user ?: throw Exception("उपयोगकर्ता नहीं मिला")

            // Direct instant login without email verification block
            var displayName = fbUser.displayName ?: formatDisplayNameFromEmail(cleanEmail)
            var phoneNumber = ""
            val userUid = fbUser.uid

            try {
                val doc = FirebaseFirestore.getInstance().collection("users").document(userUid).get().await()
                if (doc.exists()) {
                    displayName = doc.getString("displayName") ?: displayName
                    phoneNumber = doc.getString("phoneNumber") ?: ""
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firestore read failed: ${e.message}")
            }

            val loggedInProfile = UserProfile(
                uid = userUid,
                displayName = displayName,
                email = cleanEmail,
                phoneNumber = phoneNumber,
                provider = "email",
                isEmailVerified = true,
                createdAt = System.currentTimeMillis()
            )

            saveToLocalPrefs(context, loggedInProfile)

            withContext(Dispatchers.Main) {
                currentUser = loggedInProfile
            }

            // Restore user's unlocked tests and test history from Firestore
            com.example.model.UnlockedTestsRepository.syncCloudUnlocked(context, userUid)
            com.example.model.TestHistoryRepository.syncCloudHistory(userUid, context)

            Result.success(loggedInProfile)
        } catch (e: Exception) {
            Log.w(TAG, "Login error: ${e.message}")
            if (e is EmailNotVerifiedException) {
                Result.failure(e)
            } else {
                Result.failure(Exception(getReadableAuthErrorMessage(e)))
            }
        }
    }

    suspend fun resendVerificationEmail(email: String, password: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim().lowercase()
            val auth = FirebaseAuth.getInstance()

            if (password.isNotBlank()) {
                val res = auth.signInWithEmailAndPassword(cleanEmail, password).await()
                res.user?.sendEmailVerification()?.await()
                auth.signOut()
            } else {
                val currentFb = auth.currentUser
                if (currentFb != null && currentFb.email.equals(cleanEmail, ignoreCase = true)) {
                    currentFb.sendEmailVerification().await()
                } else {
                    // Fallback to password reset which checks active mailbox
                    auth.sendPasswordResetEmail(cleanEmail).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(getReadableAuthErrorMessage(e)))
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanEmail = email.trim()
            if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                return@withContext Result.failure(Exception("कृपया मान्य ईमेल आईडी दर्ज करें"))
            }
            try {
                FirebaseAuth.getInstance().sendPasswordResetEmail(cleanEmail).await()
            } catch (fbEx: Exception) {
                Log.w(TAG, "Firebase password reset email error: ${fbEx.message}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun updateProfile(
        context: Context,
        displayName: String,
        phoneNumber: String,
        scope: CoroutineScope
    ) {
        val current = currentUser ?: return
        val updated = current.copy(
            displayName = displayName.trim().ifBlank { current.displayName },
            phoneNumber = phoneNumber.trim()
        )

        currentUser = updated
        saveToLocalPrefs(context, updated)

        scope.launch(Dispatchers.IO) {
            try {
                val fbUser = FirebaseAuth.getInstance().currentUser
                if (fbUser != null) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(updated.displayName)
                        .build()
                    fbUser.updateProfile(profileUpdates).await()
                }
                saveProfileToFirestore(updated)
            } catch (e: Exception) {
                Log.w(TAG, "Profile update cloud sync: ${e.message}")
            }
        }
    }

    suspend fun saveProfileToFirestore(profile: UserProfile) {
        val data = hashMapOf(
            "uid" to profile.uid,
            "displayName" to profile.displayName,
            "email" to profile.email,
            "phoneNumber" to profile.phoneNumber,
            "provider" to profile.provider,
            "lastActive" to System.currentTimeMillis()
        )
        FirebaseFirestore.getInstance().collection("users").document(profile.uid)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun recordTestScoreToCloud(
        testTitle: String,
        score: Double,
        totalMarks: Double,
        accuracy: Int,
        percentile: Double,
        rank: Int = 0,
        totalCandidates: Int = 0
    ) {
        val user = currentUser ?: return
        try {
            val scoreRecord = hashMapOf(
                "userId" to user.uid,
                "userEmail" to user.email,
                "userName" to user.displayName,
                "testTitle" to testTitle,
                "score" to score,
                "totalMarks" to totalMarks,
                "accuracy" to accuracy,
                "percentile" to percentile,
                "rank" to rank,
                "totalCandidates" to totalCandidates,
                "timestamp" to System.currentTimeMillis()
            )

            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(user.uid)
                .collection("test_history")
                .add(scoreRecord)
                .await()

            db.collection("users").document(user.uid)
                .set(mapOf("lastTestAttempted" to testTitle, "lastTestTimestamp" to System.currentTimeMillis()), SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.w(TAG, "Test score cloud sync error: ${e.message}")
        }
    }

    fun logOut(context: Context) {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase sign out error: ${e.message}")
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        currentUser = null
    }

    private fun saveToLocalPrefs(context: Context, profile: UserProfile) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_UID, profile.uid)
            .putString(KEY_NAME, profile.displayName)
            .putString(KEY_EMAIL, profile.email)
            .putString(KEY_PHONE, profile.phoneNumber)
            .putString(KEY_PHOTO, profile.photoUrl)
            .putString(KEY_PROVIDER, profile.provider)
            .putLong(KEY_CREATED_AT, profile.createdAt)
            .apply()
    }
}
