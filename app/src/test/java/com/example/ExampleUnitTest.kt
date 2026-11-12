package com.example

import com.example.model.DynamicTestRepository
import com.example.model.ExamChapterManager
import com.example.model.TestPaper
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying dynamic repositories, exam subject hierarchy, and test routing.
 */
class ExampleUnitTest {
    @Test
    fun testDynamicTestRepositoryCategorization() {
        val subExamGd = DynamicTestRepository.resolveAccurateSubExamId("ssc_gd", "SSC GD Constable Mock 1")
        assertEquals("ssc_gd", subExamGd)

        val subExamDp = DynamicTestRepository.resolveAccurateSubExamId("ssc_delhi_police", "Delhi Police Constable 2025")
        assertEquals("ssc_delhi_police", subExamDp)

        val subExamUp = DynamicTestRepository.resolveAccurateSubExamId("up_police", "UP Police Constable Mock")
        assertEquals("up_police", subExamUp)
    }

    @Test
    fun testExamChapterManagerStructure() {
        val subjects = ExamChapterManager.getSubjectsForExam("ssc_gd")
        assertNotNull("Subjects must not be null for SSC GD", subjects)
        assertTrue("SSC GD must have multiple subjects", subjects.isNotEmpty())

        val maths = subjects.find { it.key == "MATHS" }
        assertNotNull("Maths subject must exist in SSC GD", maths)

        val reasoning = subjects.find { it.key == "REASONING" }
        assertNotNull("Reasoning subject must exist in SSC GD", reasoning)

        val gk = subjects.find { it.key == "GK_GS" || it.key == "GK" }
        assertNotNull("GK subject must exist in SSC GD", gk)

        val hindi = subjects.find { it.key == "HINDI" }
        assertNotNull("Hindi subject must exist in SSC GD", hindi)
    }
}
