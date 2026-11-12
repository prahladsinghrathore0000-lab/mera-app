import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

# Add testPaperToEdit state
if "var testPaperToEdit" not in content:
    content = content.replace("var chapterToEdit by remember { mutableStateOf<ChapterItem?>(null) }",
                              "var chapterToEdit by remember { mutableStateOf<ChapterItem?>(null) }\n    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }")

# Add dialog for testPaperToEdit
dialog_code = """
    if (testPaperToEdit != null) {
        val entry = com.example.model.DynamicTestRepository.customTests.find { it.testPaper.id == testPaperToEdit!!.id }
        if (entry != null) {
            com.example.ui.components.AdminEditTestPaperDialog(
                testPaper = testPaperToEdit!!,
                testCategory = entry.postType,
                subExamId = entry.subExamId,
                onDismiss = { testPaperToEdit = null },
                onSaved = { testPaperToEdit = null }
            )
        } else {
            testPaperToEdit = null
        }
    }
"""
if "AdminEditTestPaperDialog" not in content:
    content = content.replace("if (chapterToEdit != null) {", dialog_code + "\n    if (chapterToEdit != null) {")

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
    f.write(content)
print("Added testPaperToEdit state and dialog")
