import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

# Make absolutely sure it exists right under CategoryDetailBottomSheet sig
content = content.replace("@Composable\nfun CategoryDetailBottomSheet(\n    category: ExamCategory,\n    onDismiss: () -> Unit,\n    onStartTest: (title: String, questions: List<QuizQuestion>?) -> Unit\n) {\n    val context = LocalContext.current\n    val scope = rememberCoroutineScope()\n    val isAdmin = remember { AuthManager.isAdmin() }", 
                          "@Composable\nfun CategoryDetailBottomSheet(\n    category: ExamCategory,\n    onDismiss: () -> Unit,\n    onStartTest: (title: String, questions: List<QuizQuestion>?) -> Unit\n) {\n    val context = LocalContext.current\n    val scope = rememberCoroutineScope()\n    val isAdmin = remember { AuthManager.isAdmin() }\n    var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }")

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
    f.write(content)
print("State reinserted")
