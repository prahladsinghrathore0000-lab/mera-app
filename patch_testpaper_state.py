import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

# Make sure testPaperToEdit is available at the top level of the Composable
# Remove it from ChapterWiseSelectionScreen
content = content.replace("    var chapterToEdit by remember { mutableStateOf<ChapterItem?>(null) }\n    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }", "    var chapterToEdit by remember { mutableStateOf<ChapterItem?>(null) }")

# Add it to CategoryDetailBottomSheet
content = content.replace("val scope = rememberCoroutineScope()\n    val isAdmin = remember { AuthManager.isAdmin() }", "val scope = rememberCoroutineScope()\n    val isAdmin = remember { AuthManager.isAdmin() }\n    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }")


with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
    f.write(content)
print("Moved testPaperToEdit state to CategoryDetailBottomSheet top level")
