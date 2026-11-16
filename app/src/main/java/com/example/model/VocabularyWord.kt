package com.example.model

data class VocabularyWord(
    val id: String,
    val word: String,
    val englishPronunciation: String,
    val hindiPronunciation: String,
    val englishMeaning: String,
    val hindiMeaning: String,
    val synonyms: List<String>,
    val example: String,
    val wrongOptions: List<String>,
    val category: VocabularyCategory
)
