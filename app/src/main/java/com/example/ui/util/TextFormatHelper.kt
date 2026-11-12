package com.example.ui.util

import android.util.LruCache
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

object TextFormatHelper {
    private val markdownCache = LruCache<String, AnnotatedString>(1000)

    /**
     * Parses text containing HTML tags (<u>...</u>, <b>...</b>, <strong>...</strong>, <ins>...</ins>)
     * or Markdown (_underlined_, **bold**) into an AnnotatedString with proper styling so
     * underlined and bolded words (such as idioms, proverbs, grammar words, and formulas)
     * are rendered visually distinct. Cached for zero-latency rendering.
     */
    fun parseMarkdown(text: String): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")
        val cached = markdownCache.get(text)
        if (cached != null) return cached

        val parsed = buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                // 1. Check HTML <u>...</u> or <ins>...</ins>
                if (text.startsWith("<u>", i, ignoreCase = true)) {
                    val endIdx = text.indexOf("</u>", i + 3, ignoreCase = true)
                    if (endIdx != -1) {
                        pushStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold))
                        append(text.substring(i + 3, endIdx))
                        pop()
                        i = endIdx + 4
                        continue
                    }
                }
                if (text.startsWith("<ins>", i, ignoreCase = true)) {
                    val endIdx = text.indexOf("</ins>", i + 5, ignoreCase = true)
                    if (endIdx != -1) {
                        pushStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold))
                        append(text.substring(i + 5, endIdx))
                        pop()
                        i = endIdx + 6
                        continue
                    }
                }

                // 2. Check HTML <b>...</b> or <strong>...</strong>
                if (text.startsWith("<b>", i, ignoreCase = true)) {
                    val endIdx = text.indexOf("</b>", i + 3, ignoreCase = true)
                    if (endIdx != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(text.substring(i + 3, endIdx))
                        pop()
                        i = endIdx + 4
                        continue
                    }
                }
                if (text.startsWith("<strong>", i, ignoreCase = true)) {
                    val endIdx = text.indexOf("</strong>", i + 8, ignoreCase = true)
                    if (endIdx != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(text.substring(i + 8, endIdx))
                        pop()
                        i = endIdx + 9
                        continue
                    }
                }

                // 3. Check Markdown Bold **...**
                if (text.startsWith("**", i)) {
                    val endIdx = text.indexOf("**", i + 2)
                    if (endIdx != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(text.substring(i + 2, endIdx))
                        pop()
                        i = endIdx + 2
                        continue
                    }
                }

                // 4. Check Markdown Double Underline __...__
                if (text.startsWith("__", i)) {
                    val endIdx = text.indexOf("__", i + 2)
                    if (endIdx != -1 && endIdx > i + 2) {
                        pushStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold))
                        append(text.substring(i + 2, endIdx))
                        pop()
                        i = endIdx + 2
                        continue
                    }
                }

                // 5. Check Markdown Single Underline _..._
                if (text[i] == '_' && i + 1 < text.length) {
                    val endIdx = text.indexOf('_', i + 1)
                    if (endIdx != -1 && endIdx > i + 1) {
                        pushStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Bold))
                        append(text.substring(i + 1, endIdx))
                        pop()
                        i = endIdx + 1
                        continue
                    }
                }

                append(text[i])
                i++
            }
        }
        markdownCache.put(text, parsed)
        return parsed
    }
}
