import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

content = content.replace("com.example.ui.components.com.example.ui.components.AdminEditTestPaperDialog(",
                          "AdminEditTestPaperDialog(")

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
    f.write(content)
print("Fixed package duplicate")
