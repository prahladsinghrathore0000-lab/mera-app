import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

old_block = """                                        onDeleteCustom = {
                                            CustomChapterRepository.deleteChapter(subExam.id, chapter.id, context)
                                            Toast.makeText(context, "अध्याय '${chapter.title}' हटाया गया", Toast.LENGTH_SHORT).show()
                                        },"""

new_block = """                                        onDeleteCustom = {
                                            val questionsToDelete = chapter.questions
                                            CustomChapterRepository.deleteChapter(subExam.id, chapter.id, context)
                                            Toast.makeText(context, "अध्याय '${chapter.title}' ऐप से हटाया गया। GitHub सिंक हो रहा है...", Toast.LENGTH_LONG).show()
                                            scope.launch {
                                                com.example.service.OnlineTestSyncManager.deleteTestAssetsFromCloud(context, questionsToDelete)
                                                Toast.makeText(context, "✅ '${chapter.title}' ऐप और GitHub (Images + JSON) दोनों से डिलीट हो गया!", Toast.LENGTH_SHORT).show()
                                            }
                                        },"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
        f.write(content)
    print("Patched CategoryDetailBottomSheet")
else:
    print("Could not find old block in CategoryDetailBottomSheet")
