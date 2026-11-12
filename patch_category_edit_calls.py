import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

# Replace signature of DelhiPoliceTestCard to accept onEditTest
old_sig = """fun DelhiPoliceTestCard(
    testPaper: TestPaper,
    index: Int,
    isDynamic: Boolean = false,
    isAdmin: Boolean = false,
    onDeleteCustom: (() -> Unit)? = null,
    onClick: () -> Unit
) {"""

new_sig = """fun DelhiPoliceTestCard(
    testPaper: TestPaper,
    index: Int,
    isDynamic: Boolean = false,
    isAdmin: Boolean = false,
    onDeleteCustom: (() -> Unit)? = null,
    onEditTest: (() -> Unit)? = null,
    onClick: () -> Unit
) {"""

# Replace UI in DelhiPoliceTestCard to show Edit icon
old_diff = """                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cleanDifficulty,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    if (isAdmin && isDynamic && onDeleteCustom != null) {
                        IconButton(onClick = onDeleteCustom, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                            Icon(Icons.Default.DeleteOutline, "Delete Test", tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }"""

new_diff = """                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cleanDifficulty,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    if (isAdmin && isDynamic) {
                        if (onEditTest != null) {
                            IconButton(onClick = onEditTest, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                                Icon(Icons.Default.Edit, "Edit Test", tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                            }
                        }
                        if (onDeleteCustom != null) {
                            IconButton(onClick = onDeleteCustom, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                                Icon(Icons.Default.DeleteOutline, "Delete Test", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }"""

# Update calls to DelhiPoliceTestCard 1 (sub-exam dynamic list)
old_call1 = """                                DelhiPoliceTestCard(
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

new_call1 = """                                DelhiPoliceTestCard(
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
                                    onEditTest = {
                                        testPaperToEdit = testPaper
                                    },
                                    onClick = {
                                        onStartTest(testPaper.title, testPaper.questions)
                                    }
                                )"""

# Update calls to DelhiPoliceTestCard 3 (Admin Custom generated tests section)
old_call3 = """                        DelhiPoliceTestCard(
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

new_call3 = """                        DelhiPoliceTestCard(
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
                            onEditTest = {
                                testPaperToEdit = testPaper
                            },
                            onClick = {
                                onStartTest(testPaper.title, testPaper.questions)
                            }
                        )"""


if old_sig in content:
    content = content.replace(old_sig, new_sig)
    content = content.replace(old_diff, new_diff)
    content = content.replace(old_call1, new_call1)
    content = content.replace(old_call3, new_call3)
    
    # Also fix import
    if "import androidx.compose.material.icons.filled.Edit" not in content:
        content = content.replace("import androidx.compose.material.icons.filled.DeleteOutline", "import androidx.compose.material.icons.filled.DeleteOutline\nimport androidx.compose.material.icons.filled.Edit")
        
    with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
        f.write(content)
    print("Patched edit button UI and calls")
else:
    print("Could not find sig")

