package com.example.model

/**
 * Delhi Police Tests:
 * Provides official built-in full tests and seamlessly integrates with
 * dynamic tests loaded from GitHub.
 */

data class TestPaper(
    val id: String,
    val title: String,
    val totalQuestions: Int,
    val durationMinutes: Int,
    val totalMarks: Int,
    val difficulty: String,
    val year: String = "2025",
    val questions: List<QuizQuestion> = emptyList()
)

val availableExamYears = listOf("2026", "2025", "2024", "2023", "2022", "2021", "2020")

fun getDelhiPoliceConstableTestsForYear(year: String): List<TestPaper> {
    val allBuiltIn = DefaultFullMockTests.getDelhiPoliceFullTests().map { it.testPaper }
    return if (year == "सभी" || year == "ALL" || year.isBlank()) {
        allBuiltIn
    } else {
        allBuiltIn.filter { it.year == year || it.title.contains(year) }
    }
}

fun getDelhiPoliceHeadConstableTestsForYear(year: String): List<TestPaper> {
    return emptyList()
}
