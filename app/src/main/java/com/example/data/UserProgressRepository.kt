package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.VocabularyCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserProgressRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("english_learning_prefs", Context.MODE_PRIVATE)

    private val _unlockedLevels = MutableStateFlow(loadStringSet(KEY_UNLOCKED_LEVELS))
    val unlockedLevels: StateFlow<Set<String>> = _unlockedLevels.asStateFlow()

    private val _completedLevels = MutableStateFlow(loadStringSet(KEY_COMPLETED_LEVELS))
    val completedLevels: StateFlow<Set<String>> = _completedLevels.asStateFlow()

    private val _learnedWords = MutableStateFlow(loadStringSet(KEY_LEARNED_WORDS))
    val learnedWords: StateFlow<Set<String>> = _learnedWords.asStateFlow()

    private val _bookmarkedWords = MutableStateFlow(loadStringSet(KEY_BOOKMARKED_WORDS))
    val bookmarkedWords: StateFlow<Set<String>> = _bookmarkedWords.asStateFlow()

    private val _quizMistakeWords = MutableStateFlow(loadStringSet(KEY_QUIZ_MISTAKES))
    val quizMistakeWords: StateFlow<Set<String>> = _quizMistakeWords.asStateFlow()

    private val _todayStudiedWords = MutableStateFlow(loadTodayStudiedWords())
    val todayStudiedWords: StateFlow<Set<String>> = _todayStudiedWords.asStateFlow()

    private val _dailyGoal = MutableStateFlow(prefs.getInt(KEY_DAILY_GOAL, 10))
    val dailyGoal: StateFlow<Int> = _dailyGoal.asStateFlow()

    private val _speechSpeed = MutableStateFlow(prefs.getFloat(KEY_SPEECH_SPEED, 1.0f))
    val speechSpeed: StateFlow<Float> = _speechSpeed.asStateFlow()

    private val _showHindiPronunciation = MutableStateFlow(prefs.getBoolean(KEY_SHOW_HINDI_PRONUNCIATION, true))
    val showHindiPronunciation: StateFlow<Boolean> = _showHindiPronunciation.asStateFlow()

    private val _lastActiveCategory = MutableStateFlow(
        prefs.getString(KEY_LAST_CATEGORY, VocabularyCategory.EVERYDAY_WORDS.id) ?: VocabularyCategory.EVERYDAY_WORDS.id
    )
    val lastActiveCategory: StateFlow<String> = _lastActiveCategory.asStateFlow()

    private val _lastActiveLevel = MutableStateFlow(prefs.getInt(KEY_LAST_LEVEL, 1))
    val lastActiveLevel: StateFlow<Int> = _lastActiveLevel.asStateFlow()

    private val _totalTestsTaken = MutableStateFlow(prefs.getInt(KEY_TOTAL_TESTS, 0))
    val totalTestsTaken: StateFlow<Int> = _totalTestsTaken.asStateFlow()

    private val _totalQuestionsAnswered = MutableStateFlow(prefs.getInt(KEY_TOTAL_QUESTIONS, 0))
    val totalQuestionsAnswered: StateFlow<Int> = _totalQuestionsAnswered.asStateFlow()

    private val _totalCorrectAnswers = MutableStateFlow(prefs.getInt(KEY_TOTAL_CORRECT, 0))
    val totalCorrectAnswers: StateFlow<Int> = _totalCorrectAnswers.asStateFlow()

    init {
        // Migration: migrate any legacy "advanced_vocabulary" keys to "proficient_vocabulary"
        var unlockedChanged = false
        val currentUnlocked = _unlockedLevels.value.map { key ->
            if (key.startsWith("advanced_vocabulary_")) {
                unlockedChanged = true
                key.replace("advanced_vocabulary_", "proficient_vocabulary_")
            } else {
                key
            }
        }.toMutableSet()

        var completedChanged = false
        val currentCompleted = _completedLevels.value.map { key ->
            if (key.startsWith("advanced_vocabulary_")) {
                completedChanged = true
                key.replace("advanced_vocabulary_", "proficient_vocabulary_")
            } else {
                key
            }
        }.toMutableSet()

        // Unlock Level 1 for all categories by default if not set
        for (cat in VocabularyCategory.entries) {
            val key = VocabularyRepository.makeLevelKey(cat, 1)
            if (!currentUnlocked.contains(key)) {
                currentUnlocked.add(key)
                unlockedChanged = true
            }
        }

        if (unlockedChanged) {
            saveStringSet(KEY_UNLOCKED_LEVELS, currentUnlocked)
            _unlockedLevels.value = currentUnlocked
        }

        if (completedChanged) {
            saveStringSet(KEY_COMPLETED_LEVELS, currentCompleted)
            _completedLevels.value = currentCompleted
        }

        // Migrate last active category if it was advanced_vocabulary
        if (_lastActiveCategory.value == "advanced_vocabulary") {
            prefs.edit().putString(KEY_LAST_CATEGORY, VocabularyCategory.PROFICIENT_VOCABULARY.id).apply()
            _lastActiveCategory.value = VocabularyCategory.PROFICIENT_VOCABULARY.id
        }
    }

    fun unlockNextLevel(category: VocabularyCategory, currentLevelNumber: Int) {
        val nextLevelKey = VocabularyRepository.makeLevelKey(category, currentLevelNumber + 1)
        val updated = _unlockedLevels.value.toMutableSet()
        if (updated.add(nextLevelKey)) {
            saveStringSet(KEY_UNLOCKED_LEVELS, updated)
            _unlockedLevels.value = updated
        }
    }

    fun markLevelCompleted(category: VocabularyCategory, levelNumber: Int) {
        val levelKey = VocabularyRepository.makeLevelKey(category, levelNumber)
        val updated = _completedLevels.value.toMutableSet()
        if (updated.add(levelKey)) {
            saveStringSet(KEY_COMPLETED_LEVELS, updated)
            _completedLevels.value = updated
        }
        // Also unlock next level when a level is completed
        unlockNextLevel(category, levelNumber)
    }

    fun toggleLearned(wordId: String) {
        val updated = _learnedWords.value.toMutableSet()
        if (updated.contains(wordId)) {
            updated.remove(wordId)
        } else {
            updated.add(wordId)
            recordWordStudiedToday(wordId)
        }
        saveStringSet(KEY_LEARNED_WORDS, updated)
        _learnedWords.value = updated
    }

    fun markLearned(wordId: String) {
        val updated = _learnedWords.value.toMutableSet()
        if (updated.add(wordId)) {
            saveStringSet(KEY_LEARNED_WORDS, updated)
            _learnedWords.value = updated
            recordWordStudiedToday(wordId)
        }
    }

    fun toggleBookmark(wordId: String) {
        val updated = _bookmarkedWords.value.toMutableSet()
        if (updated.contains(wordId)) {
            updated.remove(wordId)
        } else {
            updated.add(wordId)
        }
        saveStringSet(KEY_BOOKMARKED_WORDS, updated)
        _bookmarkedWords.value = updated
    }

    fun addQuizMistake(wordId: String) {
        val updated = _quizMistakeWords.value.toMutableSet()
        if (updated.add(wordId)) {
            saveStringSet(KEY_QUIZ_MISTAKES, updated)
            _quizMistakeWords.value = updated
        }
    }

    fun removeQuizMistake(wordId: String) {
        val updated = _quizMistakeWords.value.toMutableSet()
        if (updated.remove(wordId)) {
            saveStringSet(KEY_QUIZ_MISTAKES, updated)
            _quizMistakeWords.value = updated
        }
    }

    fun recordWordStudiedToday(wordId: String) {
        val today = getCurrentDateKey()
        val savedDate = prefs.getString(KEY_STUDIED_DATE, "")
        val set = if (savedDate == today) {
            _todayStudiedWords.value.toMutableSet()
        } else {
            mutableSetOf()
        }
        if (set.add(wordId)) {
            prefs.edit()
                .putString(KEY_STUDIED_DATE, today)
                .putStringSet(KEY_STUDIED_TODAY, set)
                .apply()
            _todayStudiedWords.value = set
        }
    }

    fun recordTestResult(questionsCount: Int, correctCount: Int) {
        val tests = _totalTestsTaken.value + 1
        val questions = _totalQuestionsAnswered.value + questionsCount
        val correct = _totalCorrectAnswers.value + correctCount

        prefs.edit()
            .putInt(KEY_TOTAL_TESTS, tests)
            .putInt(KEY_TOTAL_QUESTIONS, questions)
            .putInt(KEY_TOTAL_CORRECT, correct)
            .apply()

        _totalTestsTaken.value = tests
        _totalQuestionsAnswered.value = questions
        _totalCorrectAnswers.value = correct
    }

    fun setDailyGoal(goal: Int) {
        prefs.edit().putInt(KEY_DAILY_GOAL, goal).apply()
        _dailyGoal.value = goal
    }

    fun setSpeechSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_SPEECH_SPEED, speed).apply()
        _speechSpeed.value = speed
    }

    fun setShowHindiPronunciation(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_HINDI_PRONUNCIATION, show).apply()
        _showHindiPronunciation.value = show
    }

    fun setLastActive(category: VocabularyCategory, levelNumber: Int) {
        prefs.edit()
            .putString(KEY_LAST_CATEGORY, category.id)
            .putInt(KEY_LAST_LEVEL, levelNumber)
            .apply()
        _lastActiveCategory.value = category.id
        _lastActiveLevel.value = levelNumber
    }

    fun resetAllProgress() {
        prefs.edit().clear().apply()
        _completedLevels.value = emptySet()
        _learnedWords.value = emptySet()
        _bookmarkedWords.value = emptySet()
        _quizMistakeWords.value = emptySet()
        _todayStudiedWords.value = emptySet()
        _totalTestsTaken.value = 0
        _totalQuestionsAnswered.value = 0
        _totalCorrectAnswers.value = 0
        _dailyGoal.value = 10
        _speechSpeed.value = 1.0f
        _showHindiPronunciation.value = true
        _lastActiveCategory.value = VocabularyCategory.EVERYDAY_WORDS.id
        _lastActiveLevel.value = 1

        val defaultUnlocked = VocabularyCategory.entries.map {
            VocabularyRepository.makeLevelKey(it, 1)
        }.toSet()
        saveStringSet(KEY_UNLOCKED_LEVELS, defaultUnlocked)
        _unlockedLevels.value = defaultUnlocked
    }

    private fun loadStringSet(key: String): Set<String> {
        return prefs.getStringSet(key, emptySet())?.toSet() ?: emptySet()
    }

    private fun saveStringSet(key: String, set: Set<String>) {
        prefs.edit().putStringSet(key, set).apply()
    }

    private fun loadTodayStudiedWords(): Set<String> {
        val today = getCurrentDateKey()
        val savedDate = prefs.getString(KEY_STUDIED_DATE, "")
        return if (savedDate == today) {
            prefs.getStringSet(KEY_STUDIED_TODAY, emptySet())?.toSet() ?: emptySet()
        } else {
            emptySet()
        }
    }

    private fun getCurrentDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    companion object {
        private const val KEY_UNLOCKED_LEVELS = "unlocked_levels"
        private const val KEY_COMPLETED_LEVELS = "completed_levels"
        private const val KEY_LEARNED_WORDS = "learned_words"
        private const val KEY_BOOKMARKED_WORDS = "bookmarked_words"
        private const val KEY_QUIZ_MISTAKES = "quiz_mistakes"
        private const val KEY_STUDIED_TODAY = "studied_today"
        private const val KEY_STUDIED_DATE = "studied_date"
        private const val KEY_DAILY_GOAL = "daily_goal"
        private const val KEY_SPEECH_SPEED = "speech_speed"
        private const val KEY_SHOW_HINDI_PRONUNCIATION = "show_hindi_pronunciation"
        private const val KEY_LAST_CATEGORY = "last_category"
        private const val KEY_LAST_LEVEL = "last_level"
        private const val KEY_TOTAL_TESTS = "total_tests"
        private const val KEY_TOTAL_QUESTIONS = "total_questions"
        private const val KEY_TOTAL_CORRECT = "total_correct"
    }
}
