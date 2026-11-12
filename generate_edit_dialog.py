import re

with open("app/src/main/java/com/example/ui/components/AdminChapterManagerDialogs.kt", "r") as f:
    content = f.read()

# Extract AdminEditChapterQuestionsDialog
start_str = "fun AdminEditChapterQuestionsDialog("
end_str = "        )\n    }\n}\n"

start_idx = content.find(start_str)
end_idx = content.find(end_str, start_idx) + len(end_str)

dialog_code = content[start_idx:end_idx]

# Adapt for TestPaper
dialog_code = dialog_code.replace("fun AdminEditChapterQuestionsDialog(", "fun AdminEditTestPaperDialog(")
dialog_code = dialog_code.replace("    chapter: ChapterItem,", "    testPaper: com.example.model.TestPaper,\n    testCategory: String,\n    subExamId: String,")
dialog_code = dialog_code.replace("chapter.title", "testPaper.title")
dialog_code = dialog_code.replace("chapterTitle", "testTitle")
dialog_code = dialog_code.replace("chapter.description", "\"\"")
dialog_code = dialog_code.replace("var chapterDesc by remember { mutableStateOf(\"\") }", "var testDesc by remember { mutableStateOf(\"\") }")
dialog_code = dialog_code.replace("chapterDesc", "testDesc")
dialog_code = dialog_code.replace("chapter.durationMinutes", "testPaper.durationMinutes")
dialog_code = dialog_code.replace("chapter.difficulty", "testPaper.difficulty")
dialog_code = dialog_code.replace("chapter.questions", "testPaper.questions")
dialog_code = dialog_code.replace("अध्याय / बटन का शीर्षक (Title)", "टेस्ट का शीर्षक (Title)")

# Update save logic
save_logic_old = """                                val isCustom = chapter.id.startsWith("custom_chap_")
                                if (isCustom) {
                                    CustomChapterRepository.deleteChapter(subExam.id, chapter.id, context)
                                }
                                
                                val createdSets = CustomChapterRepository.addChapterWithSets(
                                    subExamId = subExam.id,
                                    subjectKey = chapter.subjectKey,
                                    subjectTitle = chapter.subjectTitle,
                                    baseChapterTitle = testTitle.trim(),
                                    allQuestions = currentQuestions.toList(),
                                    durationPerSet = 10,
                                    difficulty = difficulty,
                                    context = context
                                )
                                createdSets.forEach { createdChap ->
                                    val newTestPaper = com.example.model.TestPaper(
                                        id = createdChap.id,
                                        title = "${subExam.name} - ${createdChap.title} ($selectedYear PYQ)",
                                        totalQuestions = createdChap.questions.size,
                                        durationMinutes = createdChap.durationMinutes,
                                        totalMarks = createdChap.totalMarks,
                                        difficulty = createdChap.difficulty,
                                        year = selectedYear,
                                        questions = createdChap.questions
                                    )
                                    com.example.model.DynamicTestRepository.addCustomTest(
                                        categoryId = subExam.id,
                                        subExamId = subExam.id,
                                        postType = "CONSTABLE",
                                        year = selectedYear,
                                        testType = "CHAPTER_WISE",
                                        testPaper = newTestPaper,
                                        context = context
                                    )
                                }"""

save_logic_new = """                                com.example.model.DynamicTestRepository.deleteCustomTest(testPaper.id, context)
                                
                                val newTestPaper = testPaper.copy(
                                    title = testTitle.trim(),
                                    durationMinutes = durationMinutes,
                                    difficulty = difficulty,
                                    year = selectedYear,
                                    questions = currentQuestions.toList(),
                                    totalQuestions = currentQuestions.size,
                                    totalMarks = currentQuestions.size
                                )
                                
                                com.example.model.DynamicTestRepository.addCustomTest(
                                    categoryId = subExamId,
                                    subExamId = subExamId,
                                    postType = testCategory,
                                    year = selectedYear,
                                    testType = "FULL_TEST",
                                    testPaper = newTestPaper,
                                    context = context
                                )
                                
                                // Now, optionally republish to GitHub if the app is online
                                try {
                                    com.example.service.OnlineTestSyncManager.publishAllToCloud(context)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }"""

dialog_code = dialog_code.replace(save_logic_old, save_logic_new)

with open("app/src/main/java/com/example/ui/components/AdminTestPaperManagerDialogs.kt", "w") as f:
    f.write("package com.example.ui.components\n\n")
    # Write some basic imports
    f.write("import android.widget.Toast\n")
    f.write("import androidx.compose.foundation.background\n")
    f.write("import androidx.compose.foundation.layout.*\n")
    f.write("import androidx.compose.foundation.lazy.LazyColumn\n")
    f.write("import androidx.compose.foundation.lazy.itemsIndexed\n")
    f.write("import androidx.compose.foundation.shape.RoundedCornerShape\n")
    f.write("import androidx.compose.material.icons.Icons\n")
    f.write("import androidx.compose.material.icons.filled.*\n")
    f.write("import androidx.compose.material3.*\n")
    f.write("import androidx.compose.runtime.*\n")
    f.write("import androidx.compose.ui.Alignment\n")
    f.write("import androidx.compose.ui.Modifier\n")
    f.write("import androidx.compose.ui.graphics.Color\n")
    f.write("import androidx.compose.ui.platform.LocalContext\n")
    f.write("import androidx.compose.ui.text.font.FontWeight\n")
    f.write("import androidx.compose.ui.unit.dp\n")
    f.write("import androidx.compose.ui.unit.sp\n")
    f.write("import androidx.compose.ui.window.Dialog\n")
    f.write("import androidx.compose.ui.window.DialogProperties\n")
    f.write("import kotlinx.coroutines.launch\n")
    f.write("import com.example.model.*\n")
    f.write("\n")
    f.write(dialog_code)

print("Created AdminTestPaperManagerDialogs.kt")
