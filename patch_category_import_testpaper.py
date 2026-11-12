import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

if "import com.example.ui.components.AdminEditTestPaperDialog" not in content:
    content = content.replace("import com.example.ui.components.AdminEditChapterQuestionsDialog",
                              "import com.example.ui.components.AdminEditChapterQuestionsDialog\nimport com.example.ui.components.AdminEditTestPaperDialog")
    with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
        f.write(content)
    print("Added AdminEditTestPaperDialog import")
else:
    print("Already exists")
