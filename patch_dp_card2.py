import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

content = re.sub(
    r'fun DelhiPoliceTestCard\(\s*testPaper: TestPaper,\s*index: Int,\s*isDynamic: Boolean = false,\s*onClick: \(\) -> Unit\s*\) \{',
    'fun DelhiPoliceTestCard(\n    testPaper: TestPaper,\n    index: Int,\n    isDynamic: Boolean = false,\n    isAdmin: Boolean = false,\n    onDeleteCustom: (() -> Unit)? = null,\n    onClick: () -> Unit\n) {',
    content
)

old_diff_regex = r'Text\(\s*text = cleanDifficulty,\s*fontSize = 11\.sp,\s*color = Color\(0xFF64748B\),\s*fontWeight = FontWeight\.Medium,\s*maxLines = 1\s*\)\s*\}\s*Spacer\(modifier = Modifier\.height\(8\.dp\)\)'

new_diff_str = """Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cleanDifficulty,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    if (isAdmin && isDynamic && onDeleteCustom != null) {
                        IconButton(onClick = onDeleteCustom, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                            Icon(Icons.Default.DeleteOutline, "Delete Test", tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))"""

if re.search(old_diff_regex, content):
    content = re.sub(old_diff_regex, new_diff_str, content)
    with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
        f.write(content)
    print("Patched DelhiPoliceTestCard")
else:
    print("Could not find regex")
