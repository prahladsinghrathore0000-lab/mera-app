package com.example.service

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GitHubConfig(
    val owner: String = "prahladsinghrathore869025-alt",
    val repo: String = "Prahlad",
    val branch: String = "main",
    val token: String = "",
    val folderPath: String = "data.json"
)

data class GitHubUploadResult(
    val isSuccess: Boolean,
    val message: String,
    val fileUrl: String? = null,
    val rawFileUrl: String? = null,
    val commitSha: String? = null,
    val isAlreadyExisting: Boolean = false
)

object GitHubSyncService {

    suspend fun deleteFile(
        config: GitHubConfig,
        filePath: String,
        commitMessage: String = "Delete file via AI Admin"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanPath = filePath.trimStart('/')
            val getUrl = "https://api.github.com/repos/${config.owner}/${config.repo}/contents/$cleanPath?ref=${config.branch}"
            val getRequest = Request.Builder()
                .url(getUrl)
                .addHeader("Authorization", "Bearer ${config.token}")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "ExamApp-AI-Admin")
                .get()
                .build()
            
            val getResponse = client.newCall(getRequest).execute()
            if (!getResponse.isSuccessful) {
                return@withContext Result.success("File not found or already deleted")
            }
            
            val getResponseBody = getResponse.body?.string() ?: ""
            val getJson = JSONObject(getResponseBody)
            val sha = getJson.optString("sha")
            
            if (sha.isEmpty()) {
                return@withContext Result.failure(Exception("SHA not found for deletion"))
            }
            
            val delBodyJson = JSONObject().apply {
                put("message", commitMessage)
                put("sha", sha)
                put("branch", config.branch)
            }
            
            val delUrl = "https://api.github.com/repos/${config.owner}/${config.repo}/contents/$cleanPath"
            val delRequest = Request.Builder()
                .url(delUrl)
                .addHeader("Authorization", "Bearer ${config.token}")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "ExamApp-AI-Admin")
                .delete(delBodyJson.toString().toRequestBody("application/json".toMediaType()))
                .build()
                
            val delResponse = client.newCall(delRequest).execute()
            if (delResponse.isSuccessful || delResponse.code == 200) {
                return@withContext Result.success("Deleted $cleanPath")
            } else {
                return@withContext Result.failure(Exception("Failed to delete: ${delResponse.message}"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }


    private const val PREFS_NAME = "admin_github_sync_prefs"
    private const val KEY_OWNER = "github_owner"
    private const val KEY_REPO = "github_repo"
    private const val KEY_BRANCH = "github_branch"
    private const val KEY_TOKEN = "github_token"
    private const val KEY_FOLDER = "github_folder"

    val DEFAULT_TOKEN = ""
    const val DEFAULT_OWNER = "prahladsinghrathore869025-alt"
    const val DEFAULT_REPO = "Prahlad"
    const val DEFAULT_BRANCH = "main"
    const val DEFAULT_FOLDER = "data.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getSavedConfig(context: Context): GitHubConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedToken = prefs.getString(KEY_TOKEN, "")?.trim() ?: ""
        val savedOwner = prefs.getString(KEY_OWNER, "")?.trim() ?: ""
        val savedRepo = prefs.getString(KEY_REPO, "")?.trim() ?: ""
        val savedBranch = prefs.getString(KEY_BRANCH, "")?.trim() ?: ""
        val savedFolder = prefs.getString(KEY_FOLDER, "")?.trim() ?: ""

        return GitHubConfig(
            owner = if (savedOwner.isNotBlank()) savedOwner else DEFAULT_OWNER,
            repo = if (savedRepo.isNotBlank()) savedRepo else DEFAULT_REPO,
            branch = if (savedBranch.isNotBlank()) savedBranch else DEFAULT_BRANCH,
            token = if (savedToken.isNotBlank()) savedToken else DEFAULT_TOKEN,
            folderPath = if (savedFolder.isNotBlank()) savedFolder else DEFAULT_FOLDER
        )
    }

    fun saveConfig(context: Context, config: GitHubConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingToken = prefs.getString(KEY_TOKEN, "")?.trim() ?: ""
        // Critical: Never accidentally erase an existing saved token with an empty string
        val tokenToSave = if (config.token.isNotBlank()) config.token.trim() else existingToken

        prefs.edit()
            .putString(KEY_OWNER, config.owner.trim().ifEmpty { DEFAULT_OWNER })
            .putString(KEY_REPO, config.repo.trim().ifEmpty { DEFAULT_REPO })
            .putString(KEY_BRANCH, config.branch.trim().ifEmpty { DEFAULT_BRANCH })
            .putString(KEY_TOKEN, tokenToSave)
            .putString(KEY_FOLDER, config.folderPath.trim().ifEmpty { DEFAULT_FOLDER })
            .apply()
    }

    data class ConnectionDiagnosis(
        val isSuccess: Boolean,
        val authenticatedUser: String?,
        val repoFullName: String?,
        val isPrivate: Boolean,
        val defaultBranch: String,
        val discoveredJsonFiles: List<String>,
        val errorMessage: String? = null
    )

    suspend fun diagnoseConnection(config: GitHubConfig): ConnectionDiagnosis = withContext(Dispatchers.IO) {
        if (config.owner.isBlank() || config.repo.isBlank()) {
            return@withContext ConnectionDiagnosis(
                isSuccess = false,
                authenticatedUser = null,
                repoFullName = null,
                isPrivate = false,
                defaultBranch = "main",
                discoveredJsonFiles = emptyList(),
                errorMessage = "GitHub Owner और Repository Name भरना अनिवार्य है।"
            )
        }

        var authUser: String? = null
        // 1. Check user token if provided
        if (config.token.isNotBlank()) {
            try {
                val userReq = Request.Builder()
                    .url("https://api.github.com/user")
                    .addHeader("Authorization", "Bearer ${config.token.trim()}")
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .addHeader("User-Agent", "ExamApp-AI-Admin")
                    .get()
                    .build()
                val userResp = client.newCall(userReq).execute()
                val userBody = userResp.body?.string() ?: ""
                if (userResp.isSuccessful && userBody.isNotBlank()) {
                    val uJson = JSONObject(userBody)
                    authUser = if (uJson.has("login") && !uJson.isNull("login")) uJson.optString("login", "").takeIf { it.isNotBlank() } else null
                } else {
                    val errJson = try { JSONObject(userBody) } catch (e: Exception) { JSONObject() }
                    val msg = errJson.optString("message", "HTTP ${userResp.code}")
                    return@withContext ConnectionDiagnosis(
                        isSuccess = false,
                        authenticatedUser = null,
                        repoFullName = null,
                        isPrivate = false,
                        defaultBranch = "main",
                        discoveredJsonFiles = emptyList(),
                        errorMessage = "टोकन अमान्य है: $msg (कृपया सही Token दर्ज करें)"
                    )
                }
            } catch (e: Exception) {
                return@withContext ConnectionDiagnosis(
                    isSuccess = false,
                    authenticatedUser = null,
                    repoFullName = null,
                    isPrivate = false,
                    defaultBranch = "main",
                    discoveredJsonFiles = emptyList(),
                    errorMessage = "नेटवर्क त्रुटि: ${e.localizedMessage}"
                )
            }
        }

        // 2. Check Repository
        try {
            val repoReqBuilder = Request.Builder()
                .url("https://api.github.com/repos/${config.owner.trim()}/${config.repo.trim()}")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "ExamApp-AI-Admin")

            if (config.token.isNotBlank()) {
                repoReqBuilder.addHeader("Authorization", "Bearer ${config.token.trim()}")
            }

            val repoResp = client.newCall(repoReqBuilder.build()).execute()
            val repoBody = repoResp.body?.string() ?: ""

            if (!repoResp.isSuccessful) {
                val errJson = try { JSONObject(repoBody) } catch (e: Exception) { JSONObject() }
                val msg = errJson.optString("message", "HTTP ${repoResp.code}")
                return@withContext ConnectionDiagnosis(
                    isSuccess = false,
                    authenticatedUser = authUser,
                    repoFullName = "${config.owner}/${config.repo}",
                    isPrivate = false,
                    defaultBranch = "main",
                    discoveredJsonFiles = emptyList(),
                    errorMessage = "रिपॉजिटरी नहीं मिली: $msg (Owner: '${config.owner}', Repo: '${config.repo}')"
                )
            }

            val rJson = JSONObject(repoBody)
            val fullName = rJson.optString("full_name", "${config.owner}/${config.repo}")
            val isPriv = rJson.optBoolean("private", false)
            val defBranch = rJson.optString("default_branch", "main")

            // 3. Discover JSON files in repo
            val treeRes = fetchRepoTree(config.copy(branch = defBranch))
            val jsonList = treeRes.getOrNull() ?: emptyList()

            ConnectionDiagnosis(
                isSuccess = true,
                authenticatedUser = authUser,
                repoFullName = fullName,
                isPrivate = isPriv,
                defaultBranch = defBranch,
                discoveredJsonFiles = jsonList
            )
        } catch (e: Exception) {
            ConnectionDiagnosis(
                isSuccess = false,
                authenticatedUser = authUser,
                repoFullName = "${config.owner}/${config.repo}",
                isPrivate = false,
                defaultBranch = "main",
                discoveredJsonFiles = emptyList(),
                errorMessage = "रिपॉजिटरी जांच त्रुटि: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Test if the GitHub credentials & repository are valid and accessible
     */
    fun verifyConnection(config: GitHubConfig): Result<String> {
        val res = kotlinx.coroutines.runBlocking { diagnoseConnection(config) }
        return if (res.isSuccess) {
            Result.success("GitHub कनेक्टेड! ${res.repoFullName} (${res.discoveredJsonFiles.size} JSON फाइल्स मिलीं)")
        } else {
            Result.failure(Exception(res.errorMessage ?: "अज्ञात त्रुटि"))
        }
    }

    /**
     * Automatically uploads/commits a JSON file directly to GitHub repository
     */
    suspend fun uploadJsonFile(
        config: GitHubConfig,
        filePath: String,
        jsonString: String,
        commitMessage: String
    ): GitHubUploadResult = withContext(Dispatchers.IO) {
        if (config.token.isBlank() || config.owner.isBlank() || config.repo.isBlank()) {
            return@withContext GitHubUploadResult(
                isSuccess = false,
                message = "कृपया पहले अपना GitHub Token और Repo डिटेल्स सेट करें।"
            )
        }

        try {
            val cleanPath = filePath.trim().removePrefix("/")
            val getFileUrl = "https://api.github.com/repos/${config.owner}/${config.repo}/contents/$cleanPath?ref=${config.branch}"

            // 1. Check if file already exists to obtain SHA
            var existingSha: String? = null
            val checkRequest = Request.Builder()
                .url(getFileUrl)
                .addHeader("Authorization", "Bearer ${config.token}")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "ExamApp-AI-Admin")
                .get()
                .build()

            val checkResponse = client.newCall(checkRequest).execute()
            if (checkResponse.isSuccessful) {
                val bodyStr = checkResponse.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                existingSha = json.optString("sha")
            }

            // 2. Base64 encode JSON content
            val base64Content = Base64.encodeToString(jsonString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            // 3. Prepare PUT payload
            val putBodyJson = JSONObject().apply {
                put("message", commitMessage)
                put("content", base64Content)
                put("branch", config.branch)
                if (!existingSha.isNullOrBlank()) {
                    put("sha", existingSha)
                }
            }

            val putUrl = "https://api.github.com/repos/${config.owner}/${config.repo}/contents/$cleanPath"
            val requestBody = putBodyJson.toString().toRequestBody("application/json".toMediaType())

            val putRequest = Request.Builder()
                .url(putUrl)
                .addHeader("Authorization", "Bearer ${config.token}")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "ExamApp-AI-Admin")
                .put(requestBody)
                .build()

            val putResponse = client.newCall(putRequest).execute()
            val putResponseBody = putResponse.body?.string() ?: ""

            if (putResponse.isSuccessful || putResponse.code == 201 || putResponse.code == 200) {
                val respJson = JSONObject(putResponseBody)
                val contentObj = respJson.optJSONObject("content")
                val htmlUrl = contentObj?.optString("html_url") ?: "https://github.com/${config.owner}/${config.repo}/blob/${config.branch}/$cleanPath"
                val rawUrl = "https://raw.githubusercontent.com/${config.owner}/${config.repo}/${config.branch}/$cleanPath"
                val commitObj = respJson.optJSONObject("commit")
                val sha = commitObj?.optString("sha")

                val wasAlreadySaved = !existingSha.isNullOrBlank()
                GitHubUploadResult(
                    isSuccess = true,
                    message = if (wasAlreadySaved) "यह फ़ाइल पहले से GitHub पर मौजूद थी (अपडेट कर दी गई है)" else "फ़ाइल सफलतापूर्वक GitHub पर सेव और कमिट हो गई!",
                    fileUrl = htmlUrl,
                    rawFileUrl = rawUrl,
                    commitSha = sha,
                    isAlreadyExisting = wasAlreadySaved
                )
            } else {
                val errorJson = try { JSONObject(putResponseBody) } catch (e: Exception) { JSONObject() }
                val errorMsg = errorJson.optString("message", "HTTP ${putResponse.code}")
                GitHubUploadResult(
                    isSuccess = false,
                    message = "GitHub कमिट फेल हुआ: $errorMsg"
                )
            }
        } catch (e: Exception) {
            GitHubUploadResult(
                isSuccess = false,
                message = "अपलोड एरर: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Uploads a binary image (ByteArray or Base64) to the GitHub repository and returns the direct CDN raw link.
     * Keeps question JSON files small & fast!
     */
    suspend fun uploadImageBytes(
        config: GitHubConfig,
        imageBytes: ByteArray,
        filePath: String,
        commitMessage: String = "Upload reasoning question image"
    ): GitHubUploadResult = withContext(Dispatchers.IO) {
        if (config.token.isBlank() || config.owner.isBlank() || config.repo.isBlank()) {
            return@withContext GitHubUploadResult(
                isSuccess = false,
                message = "GitHub Token और Repo सेट नहीं है।"
            )
        }

        try {
            val cleanPath = filePath.trim().removePrefix("/")
            val getFileUrl = "https://api.github.com/repos/${config.owner}/${config.repo}/contents/$cleanPath?ref=${config.branch}"

            var existingSha: String? = null
            val checkRequest = Request.Builder()
                .url(getFileUrl)
                .addHeader("Authorization", "Bearer ${config.token}")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "ExamApp-AI-Admin")
                .get()
                .build()

            val checkResponse = client.newCall(checkRequest).execute()
            if (checkResponse.isSuccessful) {
                val bodyStr = checkResponse.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                existingSha = json.optString("sha")
            }

            val base64Content = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            val putBodyJson = JSONObject().apply {
                put("message", commitMessage)
                put("content", base64Content)
                put("branch", config.branch)
                if (!existingSha.isNullOrBlank()) {
                    put("sha", existingSha)
                }
            }

            val putUrl = "https://api.github.com/repos/${config.owner}/${config.repo}/contents/$cleanPath"
            val requestBody = putBodyJson.toString().toRequestBody("application/json".toMediaType())

            val putRequest = Request.Builder()
                .url(putUrl)
                .addHeader("Authorization", "Bearer ${config.token}")
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "ExamApp-AI-Admin")
                .put(requestBody)
                .build()

            val putResponse = client.newCall(putRequest).execute()
            val putResponseBody = putResponse.body?.string() ?: ""

            if (putResponse.isSuccessful || putResponse.code == 201 || putResponse.code == 200) {
                val respJson = JSONObject(putResponseBody)
                val contentObj = respJson.optJSONObject("content")
                val htmlUrl = contentObj?.optString("html_url") ?: "https://github.com/${config.owner}/${config.repo}/blob/${config.branch}/$cleanPath"
                val rawUrl = "https://raw.githubusercontent.com/${config.owner}/${config.repo}/${config.branch}/$cleanPath"
                val commitObj = respJson.optJSONObject("commit")
                val sha = commitObj?.optString("sha")

                GitHubUploadResult(
                    isSuccess = true,
                    message = "इमेज सफलतापूर्वक GitHub पर अपलोड हो गई!",
                    fileUrl = htmlUrl,
                    rawFileUrl = rawUrl,
                    commitSha = sha
                )
            } else {
                val errorJson = try { JSONObject(putResponseBody) } catch (e: Exception) { JSONObject() }
                val errorMsg = errorJson.optString("message", "HTTP ${putResponse.code}")
                GitHubUploadResult(
                    isSuccess = false,
                    message = "GitHub इमेज अपलोड फेल: $errorMsg"
                )
            }
        } catch (e: Exception) {
            GitHubUploadResult(
                isSuccess = false,
                message = "इमेज अपलोड एरर: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Uploads Base64 image data string (e.g. data:image/png;base64,iVBORw0KGgo...)
     */
    suspend fun uploadBase64Image(
        config: GitHubConfig,
        base64Data: String,
        fileName: String,
        commitMessage: String = "Upload reasoning image"
    ): GitHubUploadResult {
        val pureBase64 = if (base64Data.contains(",")) {
            base64Data.substringAfter(",")
        } else {
            base64Data
        }.trim()

        val bytes = try {
            Base64.decode(pureBase64, Base64.DEFAULT)
        } catch (e: Exception) {
            return GitHubUploadResult(isSuccess = false, message = "अमान्य Base64 इमेज डेटा: ${e.message}")
        }

        val folder = if (config.folderPath.endsWith(".json", true)) "images/reasoning" else "${config.folderPath.trim('/')}/images"
        val fullPath = "$folder/$fileName"

        return uploadImageBytes(config, bytes, fullPath, commitMessage)
    }

    /**
     * Fetches file content from GitHub repository with Authorization header.
     * Supports both GitHub Contents API and raw content.
     */
    suspend fun fetchFileContent(
        config: GitHubConfig,
        filePath: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (config.owner.isBlank() || config.repo.isBlank()) {
            return@withContext Result.failure(Exception("GitHub Owner or Repo is missing"))
        }

        val cleanPath = filePath.trim().removePrefix("/")
        val encodedPath = cleanPath.split("/").joinToString("/") { java.net.URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        val branchesToTry = listOf(config.branch.ifBlank { "main" }, "main", "master").distinct()

        for (branchName in branchesToTry) {
            // Try Method 1: GitHub Contents API with Bearer / Token
            try {
                val apiUrl = "https://api.github.com/repos/${config.owner}/${config.repo}/contents/$encodedPath?ref=$branchName"
                val builder = Request.Builder()
                    .url(apiUrl)
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .addHeader("User-Agent", "ExamApp-Cloud-Sync")

                if (config.token.isNotBlank()) {
                    builder.addHeader("Authorization", "Bearer ${config.token.trim()}")
                }

                val response = client.newCall(builder.build()).execute()
                val bodyStr = response.body?.string() ?: ""

                if (response.isSuccessful && bodyStr.isNotBlank()) {
                    val json = JSONObject(bodyStr)
                    if (json.has("content") && json.optString("content").isNotBlank()) {
                        val encoded = json.getString("content").replace("\n", "").replace("\r", "")
                        val decodedBytes = Base64.decode(encoded, Base64.DEFAULT)
                        val text = String(decodedBytes, Charsets.UTF_8)
                        if (text.isNotBlank()) {
                            return@withContext Result.success(text)
                        }
                    } else if (json.has("sha") && json.optString("sha").isNotBlank()) {
                        // GitHub Contents API leaves content empty for large files or certain encodings; fetch via Git Blob API!
                        val sha = json.getString("sha")
                        val blobResult = fetchBlobContent(config, sha)
                        if (blobResult.isSuccess) {
                            return@withContext blobResult
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback to next method
            }

            // Try Method 2: Raw GitHub Content URL with Token
            try {
                val rawUrl = "https://raw.githubusercontent.com/${config.owner}/${config.repo}/$branchName/$encodedPath"
                val builder = Request.Builder()
                    .url(rawUrl)
                    .addHeader("User-Agent", "ExamApp-Cloud-Sync")
                    .addHeader("Cache-Control", "no-cache")

                if (config.token.isNotBlank()) {
                    builder.addHeader("Authorization", "token ${config.token.trim()}")
                }

                val response = client.newCall(builder.build()).execute()
                val bodyStr = response.body?.string()

                if (response.isSuccessful && !bodyStr.isNullOrBlank()) {
                    return@withContext Result.success(bodyStr)
                }
            } catch (e: Exception) {
                // Fallback
            }

            // Try Method 3: Raw GitHub Content URL without Authorization (for public repositories)
            try {
                val rawUrl = "https://raw.githubusercontent.com/${config.owner}/${config.repo}/$branchName/$encodedPath"
                val builder = Request.Builder()
                    .url(rawUrl)
                    .addHeader("User-Agent", "ExamApp-Cloud-Sync")
                    .addHeader("Cache-Control", "no-cache")

                val response = client.newCall(builder.build()).execute()
                val bodyStr = response.body?.string()

                if (response.isSuccessful && !bodyStr.isNullOrBlank()) {
                    return@withContext Result.success(bodyStr)
                }
            } catch (e: Exception) {
                // Fallback
            }
        }

        return@withContext Result.failure(Exception("फाइल नहीं मिली या एक्सेस नहीं हो सकी: $cleanPath"))
    }

    /**
     * Fetches file content directly via Git Blob API using its SHA (handles files > 1MB up to 100MB)
     */
    suspend fun fetchBlobContent(
        config: GitHubConfig,
        sha: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val blobUrl = "https://api.github.com/repos/${config.owner}/${config.repo}/git/blobs/$sha"
            val builder = Request.Builder()
                .url(blobUrl)
                .addHeader("Accept", "application/vnd.github.v3+json")
                .addHeader("User-Agent", "ExamApp-Cloud-Sync")

            if (config.token.isNotBlank()) {
                builder.addHeader("Authorization", "Bearer ${config.token.trim()}")
            }

            val response = client.newCall(builder.build()).execute()
            val bodyStr = response.body?.string() ?: ""

            if (response.isSuccessful && bodyStr.isNotBlank()) {
                val json = JSONObject(bodyStr)
                val encoded = json.optString("content", "").replace("\n", "").replace("\r", "")
                if (encoded.isNotBlank()) {
                    val decodedBytes = Base64.decode(encoded, Base64.DEFAULT)
                    val text = String(decodedBytes, Charsets.UTF_8)
                    if (text.isNotBlank()) {
                        return@withContext Result.success(text)
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return@withContext Result.failure(Exception("Blob content not found for sha $sha"))
    }

    /**
     * Recursively fetches the file tree of the GitHub repository to discover all JSON test files.
     * Tries multiple branches and falls back to contents API.
     */
    suspend fun fetchRepoTree(
        config: GitHubConfig
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        if (config.owner.isBlank() || config.repo.isBlank()) {
            return@withContext Result.failure(Exception("GitHub Owner or Repo is missing"))
        }

        val branchesToTry = listOf(config.branch.ifBlank { "main" }, "main", "master").distinct()
        val discoveredPaths = mutableSetOf<String>()

        for (branchName in branchesToTry) {
            try {
                val treeUrl = "https://api.github.com/repos/${config.owner}/${config.repo}/git/trees/$branchName?recursive=1"
                val builder = Request.Builder()
                    .url(treeUrl)
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .addHeader("User-Agent", "ExamApp-Cloud-Sync")

                if (config.token.isNotBlank()) {
                    builder.addHeader("Authorization", "Bearer ${config.token.trim()}")
                }

                val response = client.newCall(builder.build()).execute()
                val bodyStr = response.body?.string() ?: ""

                if (response.isSuccessful && bodyStr.isNotBlank()) {
                    val root = JSONObject(bodyStr)
                    val treeArr = root.optJSONArray("tree") ?: JSONArray()

                    for (i in 0 until treeArr.length()) {
                        val item = treeArr.getJSONObject(i)
                        val type = item.optString("type", "")
                        val path = item.optString("path", "")
                        if (type == "blob" && path.endsWith(".json", ignoreCase = true)) {
                            discoveredPaths.add(path)
                        }
                    }

                    if (discoveredPaths.isNotEmpty()) {
                        return@withContext Result.success(discoveredPaths.toList())
                    }
                }
            } catch (e: Exception) {
                // Try next branch or contents API
            }
        }

        // Fallback: Query GitHub Contents API for root and common folders
        val foldersToCheck = (listOf("", "tests", "data", "exams", "chapters", "tests/ssc_gd", "tests/delhi_police", "tests/ssc_delhi_police", "tests/ssc_cgl") + 
                listOf(config.folderPath.trim('/'))).filter { it.isNotBlank() || it.isEmpty() }.distinct()
        for (folder in foldersToCheck) {
            try {
                val cleanFolder = folder.trim('/')
                val url = if (cleanFolder.isBlank()) {
                    "https://api.github.com/repos/${config.owner}/${config.repo}/contents"
                } else {
                    "https://api.github.com/repos/${config.owner}/${config.repo}/contents/$cleanFolder"
                }

                val builder = Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/vnd.github.v3+json")
                    .addHeader("User-Agent", "ExamApp-Cloud-Sync")

                if (config.token.isNotBlank()) {
                    builder.addHeader("Authorization", "Bearer ${config.token.trim()}")
                }

                val response = client.newCall(builder.build()).execute()
                val bodyStr = response.body?.string() ?: ""

                if (response.isSuccessful && bodyStr.isNotBlank()) {
                    if (bodyStr.trim().startsWith("[")) {
                        val arr = JSONArray(bodyStr)
                        for (i in 0 until arr.length()) {
                            val item = arr.getJSONObject(i)
                            val path = item.optString("path", "")
                            val name = item.optString("name", "")
                            if (name.endsWith(".json", ignoreCase = true)) {
                                discoveredPaths.add(path.ifBlank { name })
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        if (discoveredPaths.isNotEmpty()) {
            Result.success(discoveredPaths.toList())
        } else {
            Result.failure(Exception("रिपॉजिटरी में कोई .json फाइल नहीं मिली"))
        }
    }
}
