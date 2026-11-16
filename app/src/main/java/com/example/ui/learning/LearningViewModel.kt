package com.example.ui.learning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.UserProgressRepository
import com.example.data.VocabularyRepository
import com.example.model.LevelInfo
import com.example.model.VocabularyCategory
import com.example.model.VocabularyWord
import com.example.tts.TextToSpeechHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LearningUiState(
    val category: VocabularyCategory,
    val levelNumber: Int,
    val levelInfo: LevelInfo?,
    val words: List<VocabularyWord> = emptyList(),
    val currentIndex: Int = 0,
    val isMeaningRevealed: Boolean = true,
    val isSessionFinished: Boolean = false
) {
    val currentWord: VocabularyWord?
        get() = words.getOrNull(currentIndex)

    val progressFraction: Float
        get() = if (words.isNotEmpty()) (currentIndex + 1).toFloat() / words.size else 0f
}

class LearningViewModel(
    private val category: VocabularyCategory,
    private val levelNumber: Int,
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val ttsHelper: TextToSpeechHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LearningUiState(
            category = category,
            levelNumber = levelNumber,
            levelInfo = null
        )
    )
    val uiState: StateFlow<LearningUiState> = _uiState.asStateFlow()

    init {
        loadLevel()
        userProgressRepository.setLastActive(category, levelNumber)
    }

    fun loadLevel() {
        val level = vocabularyRepository.getLevel(
            category = category,
            levelNumber = levelNumber,
            unlockedLevelKeys = userProgressRepository.unlockedLevels.value,
            completedLevelKeys = userProgressRepository.completedLevels.value,
            learnedWordIds = userProgressRepository.learnedWords.value
        )
        val words = level?.words ?: emptyList()
        _uiState.value = _uiState.value.copy(
            levelInfo = level,
            words = words,
            currentIndex = 0,
            isMeaningRevealed = true,
            isSessionFinished = false
        )
        // Record first word studied today if words exist
        words.firstOrNull()?.let { userProgressRepository.recordWordStudiedToday(it.id) }
    }

    fun nextWord() {
        val state = _uiState.value
        if (state.currentIndex < state.words.size - 1) {
            val nextIdx = state.currentIndex + 1
            _uiState.value = state.copy(
                currentIndex = nextIdx,
                isMeaningRevealed = true
            )
            state.words.getOrNull(nextIdx)?.let {
                userProgressRepository.recordWordStudiedToday(it.id)
            }
        } else {
            finishSession()
        }
    }

    fun previousWord() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            _uiState.value = state.copy(
                currentIndex = state.currentIndex - 1,
                isMeaningRevealed = true
            )
        }
    }

    fun toggleMeaningReveal() {
        _uiState.value = _uiState.value.copy(
            isMeaningRevealed = !_uiState.value.isMeaningRevealed
        )
    }

    fun toggleLearned(wordId: String) {
        userProgressRepository.toggleLearned(wordId)
    }

    fun toggleBookmark(wordId: String) {
        userProgressRepository.toggleBookmark(wordId)
    }

    fun speakCurrentWord() {
        val word = _uiState.value.currentWord?.word ?: return
        ttsHelper.speak(word)
    }

    fun speakExample() {
        val example = _uiState.value.currentWord?.example ?: return
        if (example.isNotBlank()) {
            ttsHelper.speak(example)
        }
    }

    fun resetSessionForReview() {
        _uiState.value = _uiState.value.copy(
            currentIndex = 0,
            isSessionFinished = false
        )
    }

    fun finishSession() {
        _uiState.value = _uiState.value.copy(isSessionFinished = true)
    }

    class Factory(
        private val category: VocabularyCategory,
        private val levelNumber: Int,
        private val vocabularyRepository: VocabularyRepository,
        private val userProgressRepository: UserProgressRepository,
        private val ttsHelper: TextToSpeechHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LearningViewModel(category, levelNumber, vocabularyRepository, userProgressRepository, ttsHelper) as T
        }
    }
}
