import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

if "import androidx.compose.material.icons.filled.DeleteOutline" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.MenuBook", "import androidx.compose.material.icons.filled.MenuBook\nimport androidx.compose.material.icons.filled.DeleteOutline")
    with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
        f.write(content)
    print("Added DeleteOutline import")
else:
    print("Already exists")
