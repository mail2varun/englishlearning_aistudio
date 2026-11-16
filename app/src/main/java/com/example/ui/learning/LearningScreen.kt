package com.example.ui.learning

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UserProgressRepository
import com.example.model.VocabularyWord
import com.example.ui.components.CommonTopBar
import com.example.ui.components.PronunciationChip
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealContainerLight
import com.example.ui.theme.TealPrimary

@Composable
fun LearningScreen(
    viewModel: LearningViewModel,
    userProgressRepository: UserProgressRepository,
    onNavigateToTest: () -> Unit,
    onBackClick: () -> Unit
) {
    BackHandler { onBackClick() }

    val state by viewModel.uiState.collectAsState()
    val bookmarkedIds by userProgressRepository.bookmarkedWords.collectAsState()
    val learnedIds by userProgressRepository.learnedWords.collectAsState()
    val showHindiPronunciation by userProgressRepository.showHindiPronunciation.collectAsState()

    androidx.compose.runtime.LaunchedEffect(state.levelNumber) {
        viewModel.resetSessionForReview()
    }

    val currentWord = state.currentWord

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "Level ${state.levelNumber}",
                subtitle = state.category.title,
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            if (currentWord != null) {
                LearningBottomDock(
                    currentIndex = state.currentIndex,
                    totalWords = state.words.size,
                    isLearned = learnedIds.contains(currentWord.id),
                    isBookmarked = bookmarkedIds.contains(currentWord.id),
                    onPrevious = { viewModel.previousWord() },
                    onNext = { viewModel.nextWord() },
                    onToggleLearned = { viewModel.toggleLearned(currentWord.id) },
                    onToggleBookmark = { viewModel.toggleBookmark(currentWord.id) }
                )
            }
        }
    ) { innerPadding ->
        if (currentWord == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("No words available in this level.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .testTag("learning_screen_content"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Word ${state.currentIndex + 1} of ${state.words.size}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary
                    )
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (learnedIds.contains(currentWord.id)) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (learnedIds.contains(currentWord.id)) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (learnedIds.contains(currentWord.id)) "Learned" else "In Progress",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (learnedIds.contains(currentWord.id)) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { state.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = TealPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main Flashcard - Direct visibility, word & pronunciation in one line
            MainFlashcard(
                word = currentWord,
                showHindiPronunciation = showHindiPronunciation,
                onSpeakWord = { viewModel.speakCurrentWord() },
                onSpeakExample = { viewModel.speakExample() }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Level Completed Dialog
        if (state.isSessionFinished) {
            AlertDialog(
                onDismissRequest = { /* dismiss handled by buttons */ },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Lesson Completed! 🎉",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        text = "You have studied all ${state.words.size} words! Take the mastery test to pass this level and unlock the next level, or review the words anytime.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onNavigateToTest,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dialog_start_test_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Quiz,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Test Now")
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { viewModel.resetSessionForReview() },
                            modifier = Modifier.testTag("dialog_review_words_button")
                        ) {
                            Text("Review Words")
                        }
                        TextButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("dialog_back_to_levels_button")
                        ) {
                            Text("Levels")
                        }
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainFlashcard(
    word: VocabularyWord,
    showHindiPronunciation: Boolean,
    onSpeakWord: () -> Unit,
    onSpeakExample: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("main_flashcard"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Row: English Word & Pronunciation Chip on the SAME line!
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TealPrimary,
                        fontSize = 28.sp
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                PronunciationChip(
                    englishPronunciation = word.englishPronunciation,
                    hindiPronunciation = word.hindiPronunciation,
                    onSpeakClick = onSpeakWord,
                    showHindiPronunciation = showHindiPronunciation
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Meaning: Only the Hindi meaning without any label
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = TealContainerLight.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = word.hindiMeaning,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF134E4A),
                            fontSize = 26.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 2. Synonyms - High visibility & clear distinction
            if (word.synonyms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            text = "SYNONYMS",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFB45309),
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            word.synonyms.forEach { synonym ->
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        text = synonym,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF78350F),
                                            fontSize = 16.sp
                                        ),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. English Definition - Increased visibility
            if (word.englishMeaning.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = "English Definition",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = word.englishMeaning,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                lineHeight = 22.sp
                            )
                        )
                    }
                }
            }

            // 4. Example Sentence - Tap to listen
            if (word.example.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSpeakExample() }
                        .testTag("example_sentence_card"),
                    color = TealPrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealPrimary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Example (Tap to Listen):",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TealPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "\"${word.example}\"",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TealPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak Example",
                                tint = TealPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LearningBottomDock(
    currentIndex: Int,
    totalWords: Int,
    isLearned: Boolean,
    isBookmarked: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleLearned: () -> Unit,
    onToggleBookmark: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Button
            OutlinedButton(
                onClick = onPrevious,
                enabled = currentIndex > 0,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("learning_prev_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Word",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Prev")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Bookmark Toggle
            IconButton(
                onClick = onToggleBookmark,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("learning_bookmark_button")
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Bookmark",
                    tint = if (isBookmarked) AmberSecondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Learned Toggle
            IconButton(
                onClick = onToggleLearned,
                modifier = Modifier
                    .size(46.dp)
                    .testTag("learning_mark_learned_button")
            ) {
                Icon(
                    imageVector = if (isLearned) Icons.Filled.CheckCircle else Icons.Filled.Check,
                    contentDescription = "Mark Learned",
                    tint = if (isLearned) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Next / Finish Button
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("learning_next_button")
            ) {
                Text(if (currentIndex >= totalWords - 1) "Finish" else "Next")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Word",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
