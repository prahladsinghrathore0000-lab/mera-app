package com.example.model

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object SavedQuestionsRepository {

    private val inMemorySavedQuestions: SnapshotStateList<SavedQuestionEntity> = mutableStateListOf()
    private var isInitialized = false

    fun initRepository(context: Context, scope: CoroutineScope) {
        if (isInitialized) return
        isInitialized = true

        val database = AppDatabase.getInstance(context)
        val dao = database.savedQuestionDao()

        scope.launch(Dispatchers.IO) {
            dao.getAllSavedQuestions().collectLatest { entities ->
                inMemorySavedQuestions.clear()
                inMemorySavedQuestions.addAll(entities)
            }
        }
    }

    private suspend fun seedInitialSavedQuestions(dao: SavedQuestionDao) {
        val sample1 = SavedQuestionEntity(
            originalQuestionId = 1,
            questionText = "भारतीय संविधान का निम्नलिखित में से कौन-सा अनुच्छेद राष्ट्रीय अनुसूचित जाति आयोग से संबंधित है?",
            optionA = "अनुच्छेद 337",
            optionB = "अनुच्छेद 338",
            optionC = "अनुच्छेद 336",
            optionD = "अनुच्छेद 335",
            correctIndex = 1,
            explanation = "भारतीय संविधान के अनुच्छेद 338 के तहत राष्ट्रीय अनुसूचित जाति आयोग (NCSC) का गठन किया गया है।",
            subject = "GK / GS",
            examTitle = "Delhi Police Constable 2023"
        )
        val sample2 = SavedQuestionEntity(
            originalQuestionId = 2,
            questionText = "FREE : EPBA :: MORE : LMOA :: BURN : ?",
            optionA = "ASOJ",
            optionB = "ASOK",
            optionC = "ASPK",
            optionD = "AROJ",
            correctIndex = 0,
            explanation = "प्रत्येक अक्षर में घटते क्रम (-1, -2, -3...) का तर्क है: B-1=A, U-2=S, R-3=O, N-4=J -> ASOJ.",
            subject = "तर्कशक्ति (Reasoning)",
            examTitle = "Delhi Police 2023"
        )
        val sample3 = SavedQuestionEntity(
            originalQuestionId = 3,
            questionText = "3 अंकों की सबसे बड़ी अभाज्य संख्या (Prime Number) कौन-सी है?",
            optionA = "997",
            optionB = "991",
            optionC = "983",
            optionD = "987",
            correctIndex = 0,
            explanation = "3 अंकों की सबसे बड़ी अभाज्य संख्या 997 है क्योंकि यह 1 और 997 के अतिरिक्त किसी अन्य संख्या से विभाजित नहीं होती।",
            subject = "संख्यात्मक योग्यता (Maths)",
            examTitle = "Delhi Police 2023"
        )
        val sample4 = SavedQuestionEntity(
            originalQuestionId = 4,
            questionText = "Which of the following is a type of network that allows users from outside to access the Intranet of an organisation?",
            optionA = "Extranet",
            optionB = "Double Intranet",
            optionC = "Internet",
            optionD = "Externet",
            correctIndex = 0,
            explanation = "Extranet एक नियंत्रित निजी नेटवर्क है जो किसी संगठन के बाहर के अधिकृत उपयोगकर्ताओं को इंट्रानेट तक सुरक्षित पहुँच प्रदान करता है।",
            subject = "कंप्यूटर ज्ञान",
            examTitle = "Delhi Police 2023"
        )

        dao.insertQuestion(sample1)
        dao.insertQuestion(sample2)
        dao.insertQuestion(sample3)
        dao.insertQuestion(sample4)
    }

    fun getReactiveSavedQuestions(): SnapshotStateList<SavedQuestionEntity> {
        return inMemorySavedQuestions
    }

    fun isSaved(questionText: String): Boolean {
        val trimmed = questionText.trim()
        return inMemorySavedQuestions.any { it.questionText.trim() == trimmed }
    }

    fun saveQuestion(
        context: Context,
        question: QuizQuestion,
        subject: String,
        examTitle: String,
        scope: CoroutineScope
    ) {
        val trimmedText = question.question.trim()
        if (isSaved(trimmedText)) return

        val optA = question.options.getOrElse(0) { "" }
        val optB = question.options.getOrElse(1) { "" }
        val optC = question.options.getOrElse(2) { "" }
        val optD = question.options.getOrElse(3) { "" }

        val entity = SavedQuestionEntity(
            originalQuestionId = question.id,
            questionText = trimmedText,
            optionA = optA,
            optionB = optB,
            optionC = optC,
            optionD = optD,
            correctIndex = question.correctIndex,
            explanation = question.explanation,
            subject = if (subject.isNotBlank()) subject else question.subject,
            examTitle = if (examTitle.isNotBlank()) examTitle else "Exam Test",
            imageUrl = question.imageUrl
        )

        inMemorySavedQuestions.add(0, entity)

        scope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).savedQuestionDao()
            dao.insertQuestion(entity)
        }
    }

    fun removeQuestion(
        context: Context,
        questionText: String,
        scope: CoroutineScope
    ) {
        val trimmedText = questionText.trim()
        inMemorySavedQuestions.removeAll { it.questionText.trim() == trimmedText }

        scope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).savedQuestionDao()
            dao.deleteByText(trimmedText)
        }
    }

    fun deleteById(
        context: Context,
        id: Int,
        scope: CoroutineScope
    ) {
        inMemorySavedQuestions.removeAll { it.id == id }

        scope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).savedQuestionDao()
            dao.deleteById(id)
        }
    }

    fun clearAll(
        context: Context,
        scope: CoroutineScope
    ) {
        inMemorySavedQuestions.clear()

        scope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).savedQuestionDao()
            dao.deleteAll()
        }
    }

    /**
     * Converts a list of SavedQuestionEntity to QuizQuestion for interactive practice & re-test!
     */
    fun entityToQuizQuestion(entity: SavedQuestionEntity): QuizQuestion {
        val options = mutableListOf<String>()
        if (entity.optionA.isNotBlank()) options.add(entity.optionA)
        if (entity.optionB.isNotBlank()) options.add(entity.optionB)
        if (entity.optionC.isNotBlank()) options.add(entity.optionC)
        if (entity.optionD.isNotBlank()) options.add(entity.optionD)

        return QuizQuestion(
            id = if (entity.originalQuestionId > 0) entity.originalQuestionId else entity.id + 5000,
            question = entity.questionText,
            options = options,
            correctIndex = entity.correctIndex.coerceIn(0, (options.size - 1).coerceAtLeast(0)),
            explanation = entity.explanation,
            subject = entity.subject,
            imageUrl = entity.imageUrl
        )
    }
}
