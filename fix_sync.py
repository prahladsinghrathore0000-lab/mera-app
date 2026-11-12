import re

with open("app/src/main/java/com/example/service/OnlineTestSyncManager.kt", "r") as f:
    content = f.read()

old_func = """
    suspend fun deleteTestAssetsFromCloud(context: Context, questions: List<com.example.model.QuizQuestion>) {
        val config = GitHubSyncService.getSavedConfig(context)
        if (config.token.isBlank() || config.owner.isBlank() || config.repo.isBlank()) {
            return
        }
        
        // Extract all githubusercontent image URLs
        val githubPrefix = "https://raw.githubusercontent.com/${config.owner}/${config.repo}/${config.branch}/"
        val imagePaths = mutableSetOf<String>()
        
        for (q in questions) {
            val qText = q.questionText
            val imgRegex = Regex("\\[IMAGE: (.*?)\\]")
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
"""

new_func = """
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
            val imgRegex = Regex("\\[IMAGE: (.*?)\\]")
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
"""

if "deleteTestAssetsFromCloud" in content:
    content = content.replace(old_func, new_func)
    with open("app/src/main/java/com/example/service/OnlineTestSyncManager.kt", "w") as f:
        f.write(content)
    print("Fixed OnlineTestSyncManager.kt")
else:
    print("Not found")
