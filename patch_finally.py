import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

# Make double sure the exact variable spelling exists
content = content.replace("var chapterToEdit by remember { mutableStateOf<ChapterItem?>(null) }", "var chapterToEdit by remember { mutableStateOf<ChapterItem?>(null) }\n    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }")
# If I inserted it already at the top, I'll probably get duplicates. Let's clean it up properly.
content = re.sub(r'^\s*var testPaperToEdit by remember \{ mutableStateOf<com\.example\.model\.TestPaper\?>\(null\) \}\n?', '', content, flags=re.MULTILINE)

content = content.replace("var subjectToDelete by remember { mutableStateOf<ExamSubjectCategory?>(null) }", 
                          "var subjectToDelete by remember { mutableStateOf<ExamSubjectCategory?>(null) }\n    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }")

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
    f.write(content)
print("Should be finally patched")
