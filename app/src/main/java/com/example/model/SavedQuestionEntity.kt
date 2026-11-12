package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a Saved / Bookmarked Question on the user's local device.
 */
@Entity(tableName = "saved_questions")
data class SavedQuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val originalQuestionId: Int = 0,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctIndex: Int,
    val explanation: String = "",
    val subject: String = "General",
    val examTitle: String = "Custom Test",
    val imageUrl: String? = null,
    val savedTimestamp: Long = System.currentTimeMillis()
)
