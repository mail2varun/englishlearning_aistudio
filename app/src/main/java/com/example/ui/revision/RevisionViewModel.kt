package com.example.ui.revision

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.UserProgressRepository
import com.example.data.VocabularyRepository
import com.example.model.VocabularyWord
import com.example.tts.TextToSpeechHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RevisionTab(val title: String) {
    MISTAKES("Quiz Mistakes"),
    BOOKMARKS("Bookmarks"),
    LEARNED("Learned Words")
}

data class RevisionUiState(
    val selectedTab: RevisionTab = RevisionTab.MISTAKES,
    val words: List<VocabularyWord> = emptyList(),
    val bookmarkedIds: Set<String> = emptySet(),
    val learnedIds: Set<String> = emptySet(),
    val mistakeIds: Set<String> = emptySet()
)

class RevisionViewModel(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val ttsHelper: TextToSpeechHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(RevisionUiState())
    val uiState: StateFlow<RevisionUiState> = _uiState.asStateFlow()

    init {
        loadWordsForTab(_uiState.value.selectedTab)
    }

    fun selectTab(tab: RevisionTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        loadWordsForTab(tab)
    }

    fun loadWordsForTab(tab: RevisionTab = _uiState.value.selectedTab) {
        val mistakes = userProgressRepository.quizMistakeWords.value
        val bookmarks = userProgressRepository.bookmarkedWords.value
        val learned = userProgressRepository.learnedWords.value

        val targetIds = when (tab) {
            RevisionTab.MISTAKES -> mistakes
            RevisionTab.BOOKMARKS -> bookmarks
            RevisionTab.LEARNED -> learned
        }

        val allWords = vocabularyRepository.getAllWords()
        val filtered = allWords.filter { targetIds.contains(it.id) }

        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            words = filtered,
            bookmarkedIds = bookmarks,
            learnedIds = learned,
            mistakeIds = mistakes
        )
    }

    fun speakWord(word: String) {
        ttsHelper.speak(word)
    }

    fun toggleBookmark(wordId: String) {
        userProgressRepository.toggleBookmark(wordId)
        loadWordsForTab()
    }

    fun toggleLearned(wordId: String) {
        userProgressRepository.toggleLearned(wordId)
        loadWordsForTab()
    }

    fun removeMistake(wordId: String) {
        userProgressRepository.removeQuizMistake(wordId)
        loadWordsForTab()
    }

    class Factory(
        private val vocabularyRepository: VocabularyRepository,
        private val userProgressRepository: UserProgressRepository,
        private val ttsHelper: TextToSpeechHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RevisionViewModel(vocabularyRepository, userProgressRepository, ttsHelper) as T
        }
    }
}
