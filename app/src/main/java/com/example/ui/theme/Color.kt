package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandNavy = Color(0xFF1E3A8A)
val BrandBlue = Color(0xFF2563EB)
val BrandLightBlue = Color(0xFF38BDF8)

val CardBlueStart = Color(0xFF1E3A8A)
val CardBlueEnd = Color(0xFF2563EB)

val CardRedStart = Color(0xFF7F1D1D)
val CardRedEnd = Color(0xFFDC2626)

val CardGreenStart = Color(0xFF14532D)
val CardGreenEnd = Color(0xFF15803D)

val ScreenBackground: Color
    @Composable
    get() = if (ThemeManager.isDarkMode) Color(0xFF0F172A) else Color(0xFFF8F9FA)

val SurfaceCard: Color
    @Composable
    get() = if (ThemeManager.isDarkMode) Color(0xFF1E293B) else Color(0xFFFFFFFF)

val SurfaceCardSecondary: Color
    @Composable
    get() = if (ThemeManager.isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)

val CardBorderColor: Color
    @Composable
    get() = if (ThemeManager.isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)

val TextPrimary: Color
    @Composable
    get() = if (ThemeManager.isDarkMode) Color(0xFFF8FAFC) else Color(0xFF111827)

val TextSecondary: Color
    @Composable
    get() = if (ThemeManager.isDarkMode) Color(0xFF94A3B8) else Color(0xFF4B5563)

val TextMuted: Color
    @Composable
    get() = if (ThemeManager.isDarkMode) Color(0xFF64748B) else Color(0xFF9CA3AF)

// Quiz / Test Taking dark mode helpers
val QuizScreenBg: Color
    @Composable
    get() = if (ThemeManager.isDarkMode) Color(0xFF0F172A) else Color(0xFFF3F6F0)

val QuizQuestionTextColor: Color
    @Composable
    get() = if (ThemeManager.isDarkMode) Color(0xFFF8FAFC) else Color(0xFF1E281C)

val QuizPillBg: Color
    @Composable
    get() = if (ThemeManager.isDarkMode) Color(0xFF1E293B) else Color(0xFFE2EBE0)

val QuizPillText: Color
    @Composable
    get() = if (ThemeManager.isDarkMode) Color(0xFFE2E8F0) else Color(0xFF3D4F37)

val Primary40 = Color(0xFF1D4ED8)
val PrimaryContainer = Color(0xFFDBEAFE)
val OnPrimaryContainer = Color(0xFF1E40AF)


