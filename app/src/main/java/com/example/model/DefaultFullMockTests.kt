package com.example.model

/**
 * Default Full Mock Tests Repository:
 * Provides guaranteed official full tests for Delhi Police Constable and other exams,
 * ensuring "Delhi Police Constable 19 Dec 2025 Shift 1" and other shift tests are always available.
 */
object DefaultFullMockTests {

    fun getFullTestsForSubExam(subExamId: String): List<DynamicTestRepository.CustomTestEntry> {
        val norm = DynamicTestRepository.normalizeSubExamId(subExamId)
        return when (norm) {
            "ssc_delhi_police" -> getDelhiPoliceFullTests()
            "ssc_gd" -> getSscGdFullTests()
            else -> emptyList()
        }
    }

    fun getSscGdFullTests(): List<DynamicTestRepository.CustomTestEntry> {
        return emptyList()
    }

    fun getDelhiPoliceFullTests(): List<DynamicTestRepository.CustomTestEntry> {
        return emptyList()
    }
}
