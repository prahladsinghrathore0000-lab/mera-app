import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

# Find the start of ChapterGroupHeaderCard
start_idx = content.find("@Composable\nfun ChapterGroupHeaderCard")

new_components = """@Composable
fun ChapterGroupHeaderCard(
    baseTitle: String,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFEFF6FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = baseTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun SetItemCard(
    chapter: ChapterItem,
    index: Int,
    subExamName: String,
    subjectName: String,
    selectedYear: String = "2025",
    isCustom: Boolean = false,
    isAdmin: Boolean = false,
    onDeleteCustom: (() -> Unit)? = null,
    onEditTest: (() -> Unit)? = null,
    onStartTest: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        label = "set_card_scale"
    )

    val displayTitle = if (chapter.title.contains(" - Set ")) {
        "Set " + chapter.title.substringAfter(" - Set ").trim()
    } else {
        "Test ${index + 1}"
    }

    Surface(
        onClick = onStartTest,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFFFFF),
        shadowElevation = if (isPressed) 0.dp else 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isCustom) Color(0xFF93C5FD) else Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .testTag("set_card_${chapter.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = displayTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "${chapter.totalQuestions} प्रश्न • ${chapter.durationMinutes} मिनट",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (isAdmin && onEditTest != null) {
                    IconButton(
                        onClick = onEditTest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (isCustom && isAdmin && onDeleteCustom != null) {
                    IconButton(
                        onClick = onDeleteCustom,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
"""

if start_idx != -1:
    new_content = content[:start_idx] + new_components
    with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
        f.write(new_content)
    print("Updated components successfully.")
else:
    print("Could not find start index.")
