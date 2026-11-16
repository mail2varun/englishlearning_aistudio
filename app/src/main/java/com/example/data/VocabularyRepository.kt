package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.LevelInfo
import com.example.model.VocabularyCategory
import com.example.model.VocabularyWord

class VocabularyRepository(private val context: Context) {

    private val wordsCache = mutableMapOf<VocabularyCategory, List<VocabularyWord>>()

    /**
     * Loads and caches all vocabulary words for the given category from Android assets.
     */
    fun getWordsForCategory(category: VocabularyCategory): List<VocabularyWord> {
        wordsCache[category]?.let { return it }

        return try {
            context.assets.open(category.fileName).use { inputStream ->
                val words = CsvParser.parseVocabularyCsv(inputStream, category)
                wordsCache[category] = words
                words
            }
        } catch (e: Exception) {
            Log.e("VocabularyRepository", "Failed to load ${category.fileName}", e)
            emptyList()
        }
    }

    /**
     * Dynamically generates 10-word level groups from the category's actual words.
     * Level 1 is always unlocked by default. Subsequent levels unlock when marked as unlocked
     * in UserProgressRepository.
     */
    fun getLevelsForCategory(
        category: VocabularyCategory,
        unlockedLevelKeys: Set<String>,
        completedLevelKeys: Set<String>,
        learnedWordIds: Set<String>
    ): List<LevelInfo> {
        val words = getWordsForCategory(category)
        if (words.isEmpty()) return emptyList()

        val totalWords = words.size
        val totalLevels = (totalWords + 9) / 10
        val levels = mutableListOf<LevelInfo>()

        for (lvl in 1..totalLevels) {
            val startIndex = (lvl - 1) * 10
            val endIndex = minOf(startIndex + 10, totalWords)
            val levelWords = words.subList(startIndex, endIndex)

            val levelKey = makeLevelKey(category, lvl)
            val isUnlocked = lvl == 1 || unlockedLevelKeys.contains(levelKey)
            val isCompleted = completedLevelKeys.contains(levelKey)
            val learnedCount = levelWords.count { learnedWordIds.contains(it.id) }

            levels.add(
                LevelInfo(
                    category = category,
                    levelNumber = lvl,
                    startIndex = startIndex,
                    endIndex = endIndex,
                    totalWords = levelWords.size,
                    words = levelWords,
                    isUnlocked = isUnlocked,
                    isCompleted = isCompleted,
                    learnedCount = learnedCount
                )
            )
        }

        return levels
    }

    /**
     * Retrieves a specific level for a category.
     */
    fun getLevel(
        category: VocabularyCategory,
        levelNumber: Int,
        unlockedLevelKeys: Set<String>,
        completedLevelKeys: Set<String>,
        learnedWordIds: Set<String>
    ): LevelInfo? {
        val allLevels = getLevelsForCategory(category, unlockedLevelKeys, completedLevelKeys, learnedWordIds)
        return allLevels.firstOrNull { it.levelNumber == levelNumber }
    }

    /**
     * Finds a word by its unique ID across all loaded categories.
     */
    fun getWordById(wordId: String): VocabularyWord? {
        for (category in VocabularyCategory.entries) {
            val match = getWordsForCategory(category).firstOrNull { it.id == wordId }
            if (match != null) return match
        }
        return null
    }

    /**
     * Returns all words across all categories.
     */
    fun getAllWords(): List<VocabularyWord> {
        return VocabularyCategory.entries.flatMap { getWordsForCategory(it) }
    }

    companion object {
        fun makeLevelKey(category: VocabularyCategory, levelNumber: Int): String {
            return "${category.id}_lvl_${levelNumber}"
        }
    }
}
