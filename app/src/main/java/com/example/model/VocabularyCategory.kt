package com.example.model

enum class VocabularyCategory(
    val id: String,
    val title: String,
    val fileName: String,
    val description: String
) {
    EVERYDAY_WORDS(
        id = "everyday_words",
        title = "Everyday Words",
        fileName = "everyday_words.csv",
        description = "Essential daily words for foundational English communication."
    ),
    USEFUL_ENGLISH(
        id = "useful_english",
        title = "Useful English",
        fileName = "useful_english.csv",
        description = "Practical verbs, descriptive adjectives, and functional expressions."
    ),
    COMMON_CONVERSATIONS(
        id = "common_conversations",
        title = "Common Conversations",
        fileName = "common_conversations.csv",
        description = "Practical conversational vocabulary and communicative expressions."
    ),
    PROFICIENT_VOCABULARY(
        id = "proficient_vocabulary",
        title = "Proficient Vocabulary",
        fileName = "proficient_vocabulary.csv",
        description = "Nuanced words, higher-proficiency terminology, and articulate expressions."
    ),
    MASTER_VOCABULARY(
        id = "master_vocabulary",
        title = "Master Vocabulary",
        fileName = "master_vocabulary.csv",
        description = "Critical vocabulary, refined rhetoric, and sophisticated conceptual terms."
    );

    companion object {
        fun fromId(id: String): VocabularyCategory {
            // Backward compatibility: map legacy "advanced_vocabulary" to PROFICIENT_VOCABULARY
            if (id.equals("advanced_vocabulary", ignoreCase = true)) {
                return PROFICIENT_VOCABULARY
            }
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: EVERYDAY_WORDS
        }
    }
}
