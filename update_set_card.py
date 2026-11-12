import re

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "r") as f:
    content = f.read()

# 1. Update the call site in CategoryDetailBottomSheet
call_site_old = """                                chaptersInGroup.forEachIndexed { setIndex, chapter ->
                                    val isCustomChapter = chapter.id.startsWith("custom_chap_")
                                    SetItemCard(
                                        chapter = chapter,
                                        index = setIndex,
                                        subExamName = subExam.name,
                                        subjectName = currentSubject?.name ?: "विषय",
                                        selectedYear = selectedChapterYear,
                                        isCustom = isCustomChapter,
                                        isAdmin = isAdmin,
                                        onDeleteCustom = {
                                            CustomChapterRepository.deleteChapter(subExam.id, chapter.id, context)
                                            Toast.makeText(context, "अध्याय '${chapter.title}' हटाया गया", Toast.LENGTH_SHORT).show()
                                        },
                                        onEditTest = {
                                            chapterToEdit = chapter
                                        },
                                        onStartTest = {
                                            val testTitle = if (selectedChapterYear != "सभी" && !chapter.title.contains(selectedChapterYear)) {
                                                "${subExam.name} - ${chapter.title} ($selectedChapterYear PYQ)"
                                            } else {
                                                "${subExam.name} - ${chapter.title}"
                                            }
                                            onStartTest(testTitle, chapter.questions)
                                        }
                                    )"""

call_site_new = """                                chaptersInGroup.forEachIndexed { setIndex, chapter ->
                                    val isCustomChapter = chapter.id.startsWith("custom_chap_")
                                    val computedTestTitle = if (selectedChapterYear != "सभी" && !chapter.title.contains(selectedChapterYear)) {
                                        "${subExam.name} - ${chapter.title} ($selectedChapterYear PYQ)"
                                    } else {
                                        "${subExam.name} - ${chapter.title}"
                                    }
                                    SetItemCard(
                                        testTitle = computedTestTitle,
                                        chapter = chapter,
                                        index = setIndex,
                                        subExamName = subExam.name,
                                        subjectName = currentSubject?.name ?: "विषय",
                                        selectedYear = selectedChapterYear,
                                        isCustom = isCustomChapter,
                                        isAdmin = isAdmin,
                                        onDeleteCustom = {
                                            CustomChapterRepository.deleteChapter(subExam.id, chapter.id, context)
                                            Toast.makeText(context, "अध्याय '${chapter.title}' हटाया गया", Toast.LENGTH_SHORT).show()
                                        },
                                        onEditTest = {
                                            chapterToEdit = chapter
                                        },
                                        onStartTest = {
                                            onStartTest(computedTestTitle, chapter.questions)
                                        }
                                    )"""

content = content.replace(call_site_old, call_site_new)

# 2. Update SetItemCard
set_card_old = """@Composable
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
}"""

set_card_new = """@Composable
fun SetItemCard(
    testTitle: String,
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

    val stats = com.example.model.TestHistoryRepository.attemptRecords[testTitle]
    val hasAttempted = stats != null

    val displayTitle = if (chapter.title.contains(" - Set ")) {
        "Set " + chapter.title.substringAfter(" - Set ").trim()
    } else {
        "Test ${index + 1}"
    }

    val cardBackgroundColor = if (hasAttempted) Color(0xFFF0FDF4) else Color(0xFFFFFFFF)
    val cardBorderColor = if (hasAttempted) Color(0xFF86EFAC) else if (isCustom) Color(0xFF93C5FD) else Color(0xFFE2E8F0)
    val iconColor = if (hasAttempted) Color(0xFF10B981) else Color(0xFF3B82F6)

    Surface(
        onClick = onStartTest,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(12.dp),
        color = cardBackgroundColor,
        shadowElevation = if (isPressed) 0.dp else 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor),
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
                    imageVector = if (hasAttempted) Icons.Default.CheckCircle else Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = iconColor,
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
                    if (hasAttempted) {
                        // Display score and attempts
                        val scoreFormatted = if (stats!!.highestScore % 1.0 == 0.0) stats.highestScore.toInt().toString() else stats.highestScore.toString()
                        val totalFormatted = if (stats.totalMarks % 1.0 == 0.0) stats.totalMarks.toInt().toString() else stats.totalMarks.toString()
                        Text(
                            text = "स्कोर: $scoreFormatted/$totalFormatted • ${stats.attemptsCount} बार दिया",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669) // Dark green
                        )
                    } else {
                        // Display default test stats
                        Text(
                            text = "${chapter.totalQuestions} प्रश्न • ${chapter.durationMinutes} मिनट",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
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
}"""

content = content.replace(set_card_old, set_card_new)

with open("app/src/main/java/com/example/ui/screens/CategoryDetailBottomSheet.kt", "w") as f:
    f.write(content)

print("Replacement successful.")
