import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

# Add proper package prefix where AdminEditTestPaperDialog is used
content = content.replace("AdminEditTestPaperDialog(\n                testPaper",
                          "com.example.ui.components.AdminEditTestPaperDialog(\n                testPaper")

# Define state properly
if "var testPaperToEdit by remember" not in content:
    content = content.replace("var chapterToEdit by remember { mutableStateOf<ChapterItem?>(null) }", 
                              "var chapterToEdit by remember { mutableStateOf<ChapterItem?>(null) }\n    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }")

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
    f.write(content)
print("Fixed issues")
