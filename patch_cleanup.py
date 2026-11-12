import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

# Remove all declarations
content = re.sub(r'^\s*var testPaperToEdit by remember \{ mutableStateOf<com\.example\.model\.TestPaper\?>\(null\) \}\n?', '', content, flags=re.MULTILINE)

# Add exactly one declaration right after val isAdmin
content = content.replace("    val scope = rememberCoroutineScope()\n    val isAdmin = remember { AuthManager.isAdmin() }", 
                          "    val scope = rememberCoroutineScope()\n    val isAdmin = remember { AuthManager.isAdmin() }\n    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }")

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
    f.write(content)
print("Cleaned duplicate state declarations")
