import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

old_sig = """fun DelhiPoliceTestCard(
    testPaper: TestPaper,
    index: Int,
    isDynamic: Boolean = false,
    onClick: () -> Unit
) {"""

new_sig = """fun DelhiPoliceTestCard(
    testPaper: TestPaper,
    index: Int,
    isDynamic: Boolean = false,
    isAdmin: Boolean = false,
    onDeleteCustom: (() -> Unit)? = null,
    onClick: () -> Unit
) {"""

old_diff = """                Text(
                    text = cleanDifficulty,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(8.dp))"""

new_diff = """                Row(verticalAlignment = Alignment.CenterVertically) {
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

if old_sig in content and old_diff in content:
    content = content.replace(old_sig, new_sig)
    content = content.replace(old_diff, new_diff)
    with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
        f.write(content)
    print("Patched DelhiPoliceTestCard")
else:
    print("Could not find blocks in CategoryDetailBottomSheet")
