package com.example.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedQuestionDao {

    @Query("SELECT * FROM saved_questions ORDER BY savedTimestamp DESC")
    fun getAllSavedQuestions(): Flow<List<SavedQuestionEntity>>

    @Query("SELECT * FROM saved_questions WHERE subject = :subject ORDER BY savedTimestamp DESC")
    fun getSavedQuestionsBySubject(subject: String): Flow<List<SavedQuestionEntity>>

    @Query("SELECT DISTINCT subject FROM saved_questions ORDER BY subject ASC")
    fun getAllSavedSubjects(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM saved_questions")
    fun getSavedCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_questions WHERE questionText = :questionText LIMIT 1)")
    suspend fun isQuestionSaved(questionText: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: SavedQuestionEntity): Long

    @Query("DELETE FROM saved_questions WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM saved_questions WHERE questionText = :questionText")
    suspend fun deleteByText(questionText: String)

    @Query("DELETE FROM saved_questions")
    suspend fun deleteAll()
}
