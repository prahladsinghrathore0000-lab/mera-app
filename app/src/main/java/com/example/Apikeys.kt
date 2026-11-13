package com.example

object ApiKeys {
    val geminiKeys = listOf(
        BuildConfig.GEMINI_API_KEY_1,
        BuildConfig.GEMINI_API_KEY_2,
        BuildConfig.GEMINI_API_KEY_3,
        BuildConfig.GEMINI_API_KEY_4,
        BuildConfig.GEMINI_API_KEY_5
    ).filter { it.isNotEmpty() }

    val githubToken = BuildConfig.GH_SYNC_TOKEN

    fun getGeminiKey(index: Int = 0): String {
        if (geminiKeys.isEmpty()) return ""
        return geminiKeys[index % geminiKeys.size]
    }
}
