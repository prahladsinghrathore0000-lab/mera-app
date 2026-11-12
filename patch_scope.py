import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

# Add testPaperToEdit state to the ChapterWiseSelectionScreen too, since that's where the unresolved references are.
content = content.replace("    var chapterToEdit by remember { mutableStateOf<ChapterItem?>(null) }", "    var chapterToEdit by remember { mutableStateOf<ChapterItem?>(null) }\n    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }")

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
    f.write(content)
print("State added to inner screen")
