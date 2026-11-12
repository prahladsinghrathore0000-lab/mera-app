import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

old_block = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailBottomSheet(
    category: ExamCategory,
    onDismiss: () -> Unit,
    onStartTest: (title: String, questions: List<QuizQuestion>?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()"""

new_block = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailBottomSheet(
    category: ExamCategory,
    onDismiss: () -> Unit,
    onStartTest: (title: String, questions: List<QuizQuestion>?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isAdmin = remember { AuthManager.isAdmin() }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
        f.write(content)
    print("Added isAdmin")
else:
    print("Could not find block")
