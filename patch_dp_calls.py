import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

# Replace block 1
old_block1 = """                                DelhiPoliceTestCard(
                                    testPaper = testPaper,
                                    index = index,
                                    isDynamic = isDynamic,
                                    onClick = {
                                        onStartTest(testPaper.title, testPaper.questions)
                                    }
                                )"""

new_block1 = """                                DelhiPoliceTestCard(
                                    testPaper = testPaper,
                                    index = index,
                                    isDynamic = isDynamic,
                                    isAdmin = isAdmin,
                                    onDeleteCustom = {
                                        val questionsToDelete = testPaper.questions
                                        com.example.model.DynamicTestRepository.deleteCustomTest(testPaper.id.toString(), context)
                                        android.widget.Toast.makeText(context, "टेस्ट '${testPaper.title}' ऐप से हटाया गया। GitHub सिंक हो रहा है...", android.widget.Toast.LENGTH_LONG).show()
                                        kotlinx.coroutines.GlobalScope.launch {
                                            com.example.service.OnlineTestSyncManager.deleteTestAssetsFromCloud(context, questionsToDelete)
                                            kotlinx.coroutines.Dispatchers.Main.dispatch(kotlin.coroutines.EmptyCoroutineContext) {
                                                android.widget.Toast.makeText(context, "✅ '${testPaper.title}' ऐप और GitHub (Images + JSON) दोनों से डिलीट हो गया!", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    onClick = {
                                        onStartTest(testPaper.title, testPaper.questions)
                                    }
                                )"""

# Replace block 2 (same as 1, so replace twice)
content = content.replace(old_block1, new_block1)

# Replace block 3
old_block3 = """                        DelhiPoliceTestCard(
                            testPaper = testPaper,
                            index = index,
                            isDynamic = true,
                            onClick = {
                                onStartTest(testPaper.title, testPaper.questions)
                            }
                        )"""

new_block3 = """                        DelhiPoliceTestCard(
                            testPaper = testPaper,
                            index = index,
                            isDynamic = true,
                            isAdmin = isAdmin,
                            onDeleteCustom = {
                                val questionsToDelete = testPaper.questions
                                com.example.model.DynamicTestRepository.deleteCustomTest(testPaper.id.toString(), context)
                                android.widget.Toast.makeText(context, "टेस्ट '${testPaper.title}' ऐप से हटाया गया। GitHub सिंक हो रहा है...", android.widget.Toast.LENGTH_LONG).show()
                                kotlinx.coroutines.GlobalScope.launch {
                                    com.example.service.OnlineTestSyncManager.deleteTestAssetsFromCloud(context, questionsToDelete)
                                    kotlinx.coroutines.Dispatchers.Main.dispatch(kotlin.coroutines.EmptyCoroutineContext) {
                                        android.widget.Toast.makeText(context, "✅ '${testPaper.title}' ऐप और GitHub (Images + JSON) दोनों से डिलीट हो गया!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onClick = {
                                onStartTest(testPaper.title, testPaper.questions)
                            }
                        )"""

content = content.replace(old_block3, new_block3)

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
    f.write(content)
print("Patched call sites")
