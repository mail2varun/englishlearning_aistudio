package com.example.model

data class LevelInfo(
    val category: VocabularyCategory,
    val levelNumber: Int,
    val startIndex: Int,
    val endIndex: Int,
    val totalWords: Int,
    val words: List<VocabularyWord>,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
    val learnedCount: Int
) {
    val wordRangeText: String
        get() = "Words ${startIndex + 1}–$endIndex"
}
