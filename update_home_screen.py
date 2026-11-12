import re

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    content = f.read()

# Replace the onStartTest block
old_onStartTest = """                onStartTest = { testTitle, questions ->
                    selectedCategoryForSheet = null
                    val isUnlocked = UnlockedTestsRepository.isTestUnlocked(testTitle)
                    if (isUnlocked) {
                        // Already unlocked! Directly launch test without ad (works offline & online!)
                        activeTestModalTitle = testTitle
                        activeTestQuestions = questions ?: OfflineTestRepository.getOfflineQuestions(context, testTitle)
                    } else {
                        // Needs 1-time ad watch to unlock lifetime free
                        if (OfflineTestRepository.isNetworkAvailable(context)) {
                            pendingTestToUnlock = Pair(testTitle, questions)
                        } else {
                            showOfflineUnlockNotice = true
                        }
                    }
                }"""

new_onStartTest = """                onStartTest = { testTitle, questions ->
                    val isUnlocked = UnlockedTestsRepository.isTestUnlocked(testTitle)
                    if (isUnlocked) {
                        // Already unlocked! Directly launch test without ad (works offline & online!)
                        selectedCategoryForSheet = null
                        activeTestModalTitle = testTitle
                        activeTestQuestions = questions ?: OfflineTestRepository.getOfflineQuestions(context, testTitle)
                    } else {
                        // Needs 1-time ad watch to unlock lifetime free
                        if (OfflineTestRepository.isNetworkAvailable(context)) {
                            pendingTestToUnlock = Pair(testTitle, questions)
                        } else {
                            showOfflineUnlockNotice = true
                        }
                    }
                }"""

# Also add selectedCategoryForSheet = null in onTestUnlocked
old_onTestUnlocked = """                onTestUnlocked = {
                    pendingTestToUnlock = null
                    activeTestModalTitle = title
                    activeTestQuestions = questions
                    // Automatically cache for lifetime offline use!
                    OfflineTestRepository.saveTestOffline(context, title, questions)
                }"""

new_onTestUnlocked = """                onTestUnlocked = {
                    pendingTestToUnlock = null
                    selectedCategoryForSheet = null
                    activeTestModalTitle = title
                    activeTestQuestions = questions
                    // Automatically cache for lifetime offline use!
                    OfflineTestRepository.saveTestOffline(context, title, questions)
                }"""


if old_onStartTest in content and old_onTestUnlocked in content:
    content = content.replace(old_onStartTest, new_onStartTest)
    content = content.replace(old_onTestUnlocked, new_onTestUnlocked)
    with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Failed to find old blocks")
    if old_onStartTest not in content:
        print("Missing: old_onStartTest")
    if old_onTestUnlocked not in content:
        print("Missing: old_onTestUnlocked")
