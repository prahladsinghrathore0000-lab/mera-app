import re

with open("app/src/main/java/com/example/service/GitHubSyncService.kt", "r") as f:
    content = f.read()

delete_func = """
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
"""

if "fun deleteFile" not in content:
    content = content.replace("object GitHubSyncService {", "object GitHubSyncService {\n" + delete_func)
    with open("app/src/main/java/com/example/service/GitHubSyncService.kt", "w") as f:
        f.write(content)
    print("Added deleteFile to GitHubSyncService")
else:
    print("deleteFile already exists")
