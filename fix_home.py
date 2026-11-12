import re

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    content = f.read()

# Remove selectedCategoryForSheet = null from onStartTest
content = content.replace(
"""                    if (isUnlocked) {
                        // Already unlocked! Directly launch test without ad (works offline & online!)
                        selectedCategoryForSheet = null
                        activeTestModalTitle = testTitle""",
"""                    if (isUnlocked) {
                        // Already unlocked! Directly launch test without ad (works offline & online!)
                        activeTestModalTitle = testTitle"""
)

# Remove selectedCategoryForSheet = null from onTestUnlocked
content = content.replace(
"""                onTestUnlocked = {
                    pendingTestToUnlock = null
                    selectedCategoryForSheet = null
                    activeTestModalTitle = title""",
"""                onTestUnlocked = {
                    pendingTestToUnlock = null
                    activeTestModalTitle = title"""
)

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(content)
print("HomeScreen fixed")
