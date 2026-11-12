import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

# I see it on lines 1114 and 1144! I need to remove those!
lines = content.split("\n")
new_lines = []
for i, line in enumerate(lines):
    if "var testPaperToEdit by remember { mutableStateOf<com.example.model.TestPaper?>(null) }" in line:
        if i + 1 != 145: # Keep only line 145
            continue
    new_lines.append(line)

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
    f.write("\n".join(new_lines))
print("Removed duplicates")
