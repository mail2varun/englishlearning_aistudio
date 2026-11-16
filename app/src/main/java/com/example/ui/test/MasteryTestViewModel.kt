package com.example.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.UserProgressRepository
import com.example.data.VocabularyRepository
import com.example.model.VocabularyCategory
import com.example.model.VocabularyWord
import com.example.tts.TextToSpeechHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class QuestionOption(
    val text: String,
    val isCorrect: Boolean
)

sealed interface TestFeedbackState {
    object Idle : TestFeedbackState
    data class Correct(val word: VocabularyWord, val streak: Int) : TestFeedbackState
    data class Incorrect(
        val word: VocabularyWord,
        val chosenOptionText: String,
        val correctOptionText: String,
        val wasRefresher: Boolean = false
    ) : TestFeedbackState
}

data class MasteryTestUiState(
    val category: VocabularyCategory,
    val levelNumber: Int,
    val totalLevelWords: Int = 0,
    val currentWord: VocabularyWord? = null,
    val currentOptions: List<QuestionOption> = emptyList(),
    val streakCounts: Map<String, Int> = emptyMap(), // wordId -> consecutive correct (0..3)
    val completedWordIds: Set<String> = emptySet(),
    val feedbackState: TestFeedbackState = TestFeedbackState.Idle,
    val totalQuestionsAnswered: Int = 0,
    val totalMistakes: Int = 0,
    val mistakeWordsList: List<VocabularyWord> = emptyList(),
    val isTestCompleted: Boolean = false
) {
    val masteredCount: Int
        get() = completedWordIds.size

    val progressFraction: Float
        get() = if (totalLevelWords > 0) completedWordIds.size.toFloat() / totalLevelWords else 0f

    val currentWordStreak: Int
        get() = currentWord?.let { streakCounts[it.id] ?: 0 } ?: 0
}

class MasteryTestViewModel(
    private val category: VocabularyCategory,
    private val levelNumber: Int,
    private val vocabularyRepository: VocabularyRepository,
    private val userProgressRepository: UserProgressRepository,
    private val ttsHelper: TextToSpeechHelper
) : ViewModel() {

    private val allLevelWords: List<VocabularyWord>
    private val recentWordIds = ArrayDeque<String>()
    private val presentationCounts = mutableMapOf<String, Int>()

    private val _uiState = MutableStateFlow(
        MasteryTestUiState(
            category = category,
            levelNumber = levelNumber
        )
    )
    val uiState: StateFlow<MasteryTestUiState> = _uiState.asStateFlow()

    init {
        val level = vocabularyRepository.getLevel(
            category = category,
            levelNumber = levelNumber,
            unlockedLevelKeys = userProgressRepository.unlockedLevels.value,
            completedLevelKeys = userProgressRepository.completedLevels.value,
            learnedWordIds = userProgressRepository.learnedWords.value
        )
        allLevelWords = level?.words ?: emptyList()

        val initialStreaks = allLevelWords.associate { it.id to 0 }
        _uiState.value = _uiState.value.copy(
            totalLevelWords = allLevelWords.size,
            streakCounts = initialStreaks,
            completedWordIds = emptySet()
        )

        loadNextQuestion()
    }

    /**
     * Picks the next word according to the user's specific mastery rules:
     * - Incomplete words are those with streak < 3.
     * - Dynamic Refresher Challenge: If remaining incomplete words <= 3 and completed words exist,
     *   there is a 40% chance of pulling a refresher question from completed words.
     *   (If the learner slips on the refresher, streak drops to 0 and returns to incomplete pool!)
     * - Anti-repetition: Avoids back-to-back immediate repeats by tracking recent history.
     * - Fair round distribution: In early rounds, ensures all words are introduced before repeating.
     */
    fun loadNextQuestion() {
        val state = _uiState.value
        val incompleteWords = allLevelWords.filter { (state.streakCounts[it.id] ?: 0) < 3 }

        if (incompleteWords.isEmpty()) {
            // All words have reached 3 consecutive correct!
            finishTest()
            return
        }

        val completedWords = allLevelWords.filter { (state.streakCounts[it.id] ?: 0) >= 3 }

        // Determine if we should trigger the 40% completed-word challenge mechanic
        val shouldDrawRefresher = incompleteWords.size <= 3 &&
                completedWords.isNotEmpty() &&
                Random.nextFloat() < 0.40f

        val selectedWord: VocabularyWord = if (shouldDrawRefresher) {
            // Select from completed words as a high-stakes mastery refresher challenge
            pickRefresherWord(completedWords)
        } else {
            // Select from incomplete words with fair distribution and spacing
            pickIncompleteWord(incompleteWords, state.streakCounts)
        }

        // Record recent history to strictly prevent back-to-back repeats
        recentWordIds.addLast(selectedWord.id)
        while (recentWordIds.size > 2) {
            recentWordIds.removeFirst()
        }
        presentationCounts[selectedWord.id] = (presentationCounts[selectedWord.id] ?: 0) + 1

        // Generate 4 randomized options: 1 correct + 3 randomly chosen wrong options from CSV
        val options = generateOptions(selectedWord)

        _uiState.value = _uiState.value.copy(
            currentWord = selectedWord,
            currentOptions = options,
            feedbackState = TestFeedbackState.Idle
        )
    }

    private fun pickRefresherWord(completedWords: List<VocabularyWord>): VocabularyWord {
        // Exclude recent words to avoid repeating the last question or two
        val nonRecent = completedWords.filter { it.id !in recentWordIds }
        if (nonRecent.isNotEmpty()) {
            return nonRecent.random()
        }
        // Fallback: exclude immediate last word if possible
        val notImmediateLast = completedWords.filter { it.id != recentWordIds.lastOrNull() }
        if (notImmediateLast.isNotEmpty()) {
            return notImmediateLast.random()
        }
        return completedWords.random()
    }

    private fun pickIncompleteWord(
        incompleteWords: List<VocabularyWord>,
        streaks: Map<String, Int>
    ): VocabularyWord {
        // 1. Exclude recent words if there are sufficient candidates
        val candidates = if (incompleteWords.size > 2) {
            val nonRecent = incompleteWords.filter { it.id !in recentWordIds }
            if (nonRecent.isNotEmpty()) nonRecent else incompleteWords.filter { it.id != recentWordIds.lastOrNull() }
        } else {
            incompleteWords
        }
        val safeCandidates = candidates.ifEmpty { incompleteWords }

        // 2. Initial round fairness: prioritize words never presented yet (timesPresented == 0)
        val unseen = safeCandidates.filter { (presentationCounts[it.id] ?: 0) == 0 }
        if (unseen.isNotEmpty()) {
            return unseen.random()
        }

        // 3. For seen words: prioritize words with the lowest streak (e.g. recently missed / streak 0)
        // so mistakes are reinforced after proper spacing, shuffled among ties
        val minStreak = safeCandidates.minOf { streaks[it.id] ?: 0 }
        val lowestStreakCandidates = safeCandidates.filter { (streaks[it.id] ?: 0) == minStreak }
        return lowestStreakCandidates.random()
    }

    private fun generateOptions(word: VocabularyWord): List<QuestionOption> {
        val correctOption = QuestionOption(
            text = word.englishMeaning.ifBlank { word.hindiMeaning },
            isCorrect = true
        )

        // Randomly pick 3 distinct wrong options from CSV's wrong_option_1..6
        val availableWrong = word.wrongOptions.filter { it.isNotBlank() && it != correctOption.text }
        val chosenWrongTexts = if (availableWrong.size >= 3) {
            availableWrong.shuffled().take(3)
        } else {
            // Fallback: borrow meanings from other words in the level if needed
            val otherMeanings = allLevelWords
                .filter { it.id != word.id }
                .map { it.englishMeaning }
                .filter { it.isNotBlank() && it != correctOption.text }
                .shuffled()
            (availableWrong + otherMeanings).distinct().take(3)
        }

        val wrongOptions = chosenWrongTexts.map { QuestionOption(text = it, isCorrect = false) }
        val allOptions = (listOf(correctOption) + wrongOptions).shuffled()
        return allOptions
    }

    fun submitAnswer(option: QuestionOption) {
        val state = _uiState.value
        val word = state.currentWord ?: return
        if (state.feedbackState !is TestFeedbackState.Idle) return

        val newTotalQuestions = state.totalQuestionsAnswered + 1
        val streaks = state.streakCounts.toMutableMap()
        val completedSet = state.completedWordIds.toMutableSet()
        val mistakesList = state.mistakeWordsList.toMutableList()

        if (option.isCorrect) {
            // Correct answer
            val currentStreak = streaks[word.id] ?: 0
            val newStreak = currentStreak + 1
            streaks[word.id] = newStreak

            if (newStreak >= 3) {
                completedSet.add(word.id)
                userProgressRepository.markLearned(word.id)
            }

            _uiState.value = state.copy(
                streakCounts = streaks,
                completedWordIds = completedSet,
                totalQuestionsAnswered = newTotalQuestions,
                feedbackState = TestFeedbackState.Correct(word, newStreak)
            )
        } else {
            // Incorrect answer!
            // Counter resets to 0 even if previously marked completed!
            val wasRefresher = (state.streakCounts[word.id] ?: 0) >= 3
            streaks[word.id] = 0
            completedSet.remove(word.id) // Returns to incomplete pool!

            if (!mistakesList.any { it.id == word.id }) {
                mistakesList.add(word)
            }
            userProgressRepository.addQuizMistake(word.id)

            val correctOpt = state.currentOptions.firstOrNull { it.isCorrect }?.text ?: word.englishMeaning

            _uiState.value = state.copy(
                streakCounts = streaks,
                completedWordIds = completedSet,
                totalQuestionsAnswered = newTotalQuestions,
                totalMistakes = state.totalMistakes + 1,
                mistakeWordsList = mistakesList,
                feedbackState = TestFeedbackState.Incorrect(
                    word = word,
                    chosenOptionText = option.text,
                    correctOptionText = correctOpt,
                    wasRefresher = wasRefresher
                )
            )
        }
    }

    fun speakWord(text: String) {
        ttsHelper.speak(text)
    }

    fun dismissFeedbackAndProceed() {
        loadNextQuestion()
    }

    private fun finishTest() {
        val state = _uiState.value
        val correctCount = (state.totalQuestionsAnswered - state.totalMistakes).coerceAtLeast(0)
        userProgressRepository.recordTestResult(state.totalQuestionsAnswered, correctCount)
        userProgressRepository.markLevelCompleted(category, levelNumber)
        userProgressRepository.unlockNextLevel(category, levelNumber)

        _uiState.value = _uiState.value.copy(
            isTestCompleted = true,
            feedbackState = TestFeedbackState.Idle
        )
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
            return MasteryTestViewModel(category, levelNumber, vocabularyRepository, userProgressRepository, ttsHelper) as T
        }
    }
}
