package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.UserProgressRepository
import com.example.data.VocabularyRepository
import com.example.model.VocabularyCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CategorySummary(
    val category: VocabularyCategory,
    val totalWords: Int,
    val totalLevels: Int,
    val completedLevels: Int,
    val progressFraction: Float
)

data class HomeUiState(
    val wordsTodayCount: Int = 0,
    val dailyGoal: Int = 10,
    val totalLearnedCount: Int = 0,
    val totalWordsInApp: Int = 0,
    val totalCompletedLevels: Int = 0,
    val totalLevelsInApp: Int = 0,
    val lastActiveCategory: VocabularyCategory = VocabularyCategory.EVERYDAY_WORDS,
    val lastActiveLevelNumber: Int = 1,
    val categorySummaries: List<CategorySummary> = emptyList(),
    val motivationalQuoteEng: String = "Small Daily Efforts Leads To Fluency"
)

class HomeViewModel(
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshDashboard()
    }

    fun refreshDashboard() {
        val todayCount = userProgressRepository.todayStudiedWords.value.size
        val goal = userProgressRepository.dailyGoal.value
        val learnedCount = userProgressRepository.learnedWords.value.size
        val completedKeys = userProgressRepository.completedLevels.value
        val unlockedKeys = userProgressRepository.unlockedLevels.value
        val lastCatId = userProgressRepository.lastActiveCategory.value
        val lastLvl = userProgressRepository.lastActiveLevel.value
        val lastCat = VocabularyCategory.fromId(lastCatId)

        var totalWords = 0
        var totalLevels = 0
        var completedLevelsCount = 0

        val summaries = VocabularyCategory.entries.map { cat ->
            val words = vocabularyRepository.getWordsForCategory(cat)
            val levels = vocabularyRepository.getLevelsForCategory(cat, unlockedKeys, completedKeys, userProgressRepository.learnedWords.value)
            val completed = levels.count { it.isCompleted }

            totalWords += words.size
            totalLevels += levels.size
            completedLevelsCount += completed

            CategorySummary(
                category = cat,
                totalWords = words.size,
                totalLevels = levels.size,
                completedLevels = completed,
                progressFraction = if (levels.isNotEmpty()) completed.toFloat() / levels.size else 0f
            )
        }

        _uiState.value = HomeUiState(
            wordsTodayCount = todayCount,
            dailyGoal = goal,
            totalLearnedCount = learnedCount,
            totalWordsInApp = totalWords,
            totalCompletedLevels = completedLevelsCount,
            totalLevelsInApp = totalLevels,
            lastActiveCategory = lastCat,
            lastActiveLevelNumber = lastLvl,
            categorySummaries = summaries
        )
    }

    class Factory(
        private val vocabularyRepository: VocabularyRepository,
        private val userProgressRepository: UserProgressRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(vocabularyRepository, userProgressRepository) as T
        }
    }
}
