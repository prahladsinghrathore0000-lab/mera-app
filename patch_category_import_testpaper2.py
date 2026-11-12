import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

# Define testPaperToEdit state inside the Composable function
start_composable = "@Composable\nfun CategoryDetailBottomSheet("
state_decl = "    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }"

# Look for chapterToEdit and append testPaperToEdit below it
if "var chapterToEdit" in content and "testPaperToEdit" not in content:
    content = content.replace("var chapterToEdit by remember { mutableStateOf<ChapterItem?>(null) }",
                              "var chapterToEdit by remember { mutableStateOf<ChapterItem?>(null) }\n    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }")
elif "testPaperToEdit" not in content:
    content = content.replace("val scope = rememberCoroutineScope()", "val scope = rememberCoroutineScope()\n    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }")


with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
    f.write(content)
print("Fixed testPaperToEdit state definition")
