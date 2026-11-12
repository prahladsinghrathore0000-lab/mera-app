package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CustomChapterRepository
import com.example.model.DynamicTestRepository
import com.example.model.RealtimeExamStatsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FREE PYQ", appName)
    }

    @Test
    fun `test ssc gd muhavare and test counter count`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        DynamicTestRepository.init(context)
        CustomChapterRepository.init(context)
        RealtimeExamStatsManager.init(context)

        val dcFile = File("src/test/resources/data_chapters.json")
        val dFile = File("src/test/resources/data.json")

        if (dFile.exists()) {
            val dContent = dFile.readText()
            CustomChapterRepository.importFromJsonString(dContent, context, "ssc_gd")
            DynamicTestRepository.parseAndMergeJson(dContent, context, defaultSubExamId = "ssc_gd")
        }

        if (dcFile.exists()) {
            val dcContent = dcFile.readText()
            CustomChapterRepository.importFromJsonString(dcContent, context, "ssc_gd")
            DynamicTestRepository.parseAndMergeJson(dcContent, context, defaultSubExamId = "ssc_gd")
        }

        DynamicTestRepository.purgeCorruptedExamCrossContamination(context)
        CustomChapterRepository.sanitizeExistingCustomChapters(context)

        val realCountSscGd = RealtimeExamStatsManager.getRealTestCount("ssc_gd", "SSC GD")
        println("REAL TEST COUNT FOR SSC GD = $realCountSscGd")

        val realCountDp = RealtimeExamStatsManager.getRealTestCount("ssc_delhi_police", "Delhi Police")
        println("REAL TEST COUNT FOR DELHI POLICE = $realCountDp")

        val realCountCgl = RealtimeExamStatsManager.getRealTestCount("ssc_cgl", "SSC CGL")
        println("REAL TEST COUNT FOR SSC CGL = $realCountCgl")

        val realCountMts = RealtimeExamStatsManager.getRealTestCount("ssc_mts", "SSC MTS")
        println("REAL TEST COUNT FOR SSC MTS = $realCountMts")

        val subjects = CustomChapterRepository.getFullSubjectsForExam("ssc_gd", context)
        val hindiSub = subjects.find { it.key == "HINDI" }
        val muhTests = hindiSub?.chapters?.filter { it.title.contains("मुहावरे") } ?: emptyList()
        println("MUHAVARE TESTS IN HINDI = ${muhTests.size}")
        for (m in muhTests) {
            println(" -> ${m.id} | ${m.title} | ${m.year} | Qs: ${m.questions.size}")
        }
    }
}
