import re

with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "r") as f:
    content = f.read()

old_block = """                                            scope.launch {
                                                isDeleting = true
                                                statusMessage = "टेस्ट डिलीट किया जा रहा है..."
                                                DynamicTestRepository.deleteCustomTest(entry.testPaper.id.toString(), context)
                                                val config = GitHubSyncService.getSavedConfig(context)
                                                val masterJson = DynamicTestRepository.exportAllToMasterJson()
                                                val uploadResult = GitHubSyncService.uploadJsonFile(
                                                    config = config,
                                                    filePath = config.folderPath.ifBlank { "data.json" },
                                                    jsonString = masterJson,
                                                    commitMessage = "Delete test: ${entry.testPaper.title}"
                                                )
                                                isDeleting = false
                                                testsList = DynamicTestRepository.getAllCustomTests()
                                                if (uploadResult.isSuccess) {
                                                    statusMessage = "✅ '${entry.testPaper.title}' ऐप और GitHub दोनों से डिलीट हो गया!"
                                                    android.widget.Toast.makeText(context, "सफलतापूर्वक डिलीट कर दिया गया", android.widget.Toast.LENGTH_SHORT).show()
                                                } else {
                                                    statusMessage = "⚠️ लोकल ऐप से डिलीट हो गया, पर GitHub सिंक एरर: ${uploadResult.message}"
                                                }
                                            }"""

new_block = """                                            scope.launch {
                                                isDeleting = true
                                                statusMessage = "टेस्ट और उसकी इमेजेज (Images) GitHub से डिलीट की जा रही हैं..."
                                                val questionsToDelete = entry.testPaper.questions
                                                DynamicTestRepository.deleteCustomTest(entry.testPaper.id.toString(), context)
                                                com.example.service.OnlineTestSyncManager.deleteTestAssetsFromCloud(context, questionsToDelete)
                                                isDeleting = false
                                                testsList = DynamicTestRepository.getAllCustomTests()
                                                statusMessage = "✅ '${entry.testPaper.title}' ऐप और GitHub (Images + JSON) दोनों से डिलीट हो गया!"
                                                android.widget.Toast.makeText(context, "सफलतापूर्वक डिलीट कर दिया गया", android.widget.Toast.LENGTH_SHORT).show()
                                            }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open("app/src/main/java/com/example/ui/screens/ProfileScreen.kt", "w") as f:
        f.write(content)
    print("Patched ProfileScreen")
else:
    print("Could not find old block in ProfileScreen")
