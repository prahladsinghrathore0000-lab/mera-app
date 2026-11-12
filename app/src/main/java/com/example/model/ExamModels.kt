package com.example.model

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.CardBlueEnd
import com.example.ui.theme.CardBlueStart
import com.example.ui.theme.CardGreenEnd
import com.example.ui.theme.CardGreenStart
import com.example.ui.theme.CardRedEnd
import com.example.ui.theme.CardRedStart

data class SubExam(
    val id: String,
    val name: String,
    val fullTitle: String,
    val eligibility: String,
    val totalTests: Int = 0,
    val activeAspirants: String = "0"
)

data class ExamCategory(
    val id: String,
    val title: String,
    @DrawableRes val iconRes: Int,
    val gradientColors: List<Color>,
    val description: String,
    val subExams: List<SubExam>
)

val examCategories = listOf(
    ExamCategory(
        id = "ssc",
        title = "SSC Exams",
        iconRes = R.drawable.ic_ssc_emblem,
        gradientColors = listOf(CardBlueStart, CardBlueEnd),
        description = "Staff Selection Commission exams for central government posts",
        subExams = listOf(
            SubExam("ssc_delhi_police", "Delhi Police Constable", "Delhi Police Executive Recruitment", "12th Pass"),
            SubExam("ssc_cgl", "SSC CGL", "Combined Graduate Level Examination", "Graduation Degree"),
            SubExam("ssc_chsl", "SSC CHSL", "Combined Higher Secondary Level (10+2)", "12th Pass"),
            SubExam("ssc_cpo", "SSC CPO", "Central Police Organization (SI in DP & CAPFs)", "Graduation"),
            SubExam("ssc_mts", "SSC MTS & Havaldar", "Multi-Tasking (Non-Technical) Staff", "10th Pass"),
            SubExam("ssc_gd", "SSC GD Constable", "General Duty Constable in CAPFs & SSF", "10th Pass")
        )
    ),
    ExamCategory(
        id = "railway",
        title = "Railway Exams",
        iconRes = R.drawable.ic_railway_train,
        gradientColors = listOf(CardRedStart, CardRedEnd),
        description = "Indian Railways RRB & RRC recruitment examinations",
        subExams = listOf(
            SubExam("rrb_ntpc", "RRB NTPC", "Non-Technical Popular Categories (Graduate & Undergrad)", "12th / Degree"),
            SubExam("rrb_alp", "RRB ALP & Tech", "Assistant Loco Pilot & Technicians", "ITI / Diploma / B.Tech"),
            SubExam("rrb_group_d", "RRB Group D", "Track Maintainer, Assistant Pointsman & Posts", "10th / ITI"),
            SubExam("rrb_je", "RRB JE", "Junior Engineer (Civil, Mech, Electrical, Electronics)", "Diploma / B.Tech"),
            SubExam("rpf_si", "RPF SI & Constable", "Railway Protection Force Recruitment", "10th / Degree")
        )
    ),
    ExamCategory(
        id = "state",
        title = "State Exams",
        iconRes = R.drawable.ic_state_map,
        gradientColors = listOf(CardGreenStart, CardGreenEnd),
        description = "State Public Service Commission and State Police & Board Exams",
        subExams = listOf(
            SubExam("rajasthan_police", "Rajasthan Police", "Constable & Sub Inspector (SI) Recruitment", "10th / 12th / Degree"),
            SubExam("up_police", "UP Police", "Constable, SI & PAC Recruitment", "12th / Degree"),
            SubExam("patwari", "Patwari", "Revenue Board Patwari Recruitment Examination", "Graduation + RSCIT"),
            SubExam("lekhpal", "Lekhpal", "Rajasva Lekhpal & Chakbandi Lekhpal Examination", "12th / Degree"),
            SubExam("rajasthan_cet", "Rajasthan CET", "Common Eligibility Test (Graduate & 12th Level)", "12th / Graduation")
        )
    )
)

data class QuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val subject: String,
    val imageUrl: String? = null,
    val examShiftInfo: String? = null
)

val sampleQuizQuestions = emptyList<QuizQuestion>()

data class ExamNotification(
    val id: String,
    val title: String,
    val category: String,
    val timeAgo: String,
    val description: String,
    val isUrgent: Boolean = false
)

val examNotifications = emptyList<ExamNotification>()
