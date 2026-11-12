import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

# I see it missing right after isAdmin in CategoryDetailBottomSheet
content = content.replace("    val isAdmin = remember { AuthManager.isAdmin() }", "    val isAdmin = remember { AuthManager.isAdmin() }\n    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }")

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
    f.write(content)
print("State declared")
