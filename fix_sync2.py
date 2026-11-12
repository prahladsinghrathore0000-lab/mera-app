import re

with open("app/src/main/java/com/example/service/OnlineTestSyncManager.kt", "r") as f:
    content = f.read()

content = content.replace('Regex("\\[IMAGE: (.*?)\\]")', 'Regex("""\\[IMAGE: (.*?)\\]""")')

with open("app/src/main/java/com/example/service/OnlineTestSyncManager.kt", "w") as f:
    f.write(content)
print("Fixed regex in OnlineTestSyncManager.kt")
