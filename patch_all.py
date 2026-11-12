import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

# Make sure state variable is at the top of CategoryDetailBottomSheet
if "var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }" not in content:
    content = content.replace("val scope = rememberCoroutineScope()\n    val isAdmin = remember { AuthManager.isAdmin() }", "val scope = rememberCoroutineScope()\n    val isAdmin = remember { AuthManager.isAdmin() }\n    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }")

if "import com.example.ui.components.AdminEditTestPaperDialog" not in content:
    content = content.replace("import com.example.ui.components.AdminEditChapterQuestionsDialog", "import com.example.ui.components.AdminEditChapterQuestionsDialog\nimport com.example.ui.components.AdminEditTestPaperDialog")

# Verify we don't have nested com.example...
content = content.replace("com.example.ui.components.com.example.ui.components.AdminEditTestPaperDialog", "com.example.ui.components.AdminEditTestPaperDialog")

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
    f.write(content)
print("Patched completely")
