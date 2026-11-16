package com.example.ui.revision

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VocabularyWord
import com.example.ui.components.CommonTopBar
import com.example.ui.components.WordCard
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.TealPrimary

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RevisionScreen(
    viewModel: RevisionViewModel
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "Revision Hub",
                subtitle = "Revision & Practice"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("revision_screen_content")
        ) {
            // Tab Row
            PrimaryTabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = TealPrimary
            ) {
                RevisionTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                text = tab.title,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        modifier = Modifier.testTag("revision_tab_${tab.name.lowercase()}")
                    )
                }
            }

            // Word List or Empty State
            if (state.words.isEmpty()) {
                RevisionEmptyState(tab = state.selectedTab)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${state.words.size} items in ${state.selectedTab.title}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    items(state.words) { word ->
                        WordCard(
                            word = word,
                            isBookmarked = state.bookmarkedIds.contains(word.id),
                            onBookmarkToggle = { viewModel.toggleBookmark(word.id) },
                            onSpeakClick = { viewModel.speakWord(word.word) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RevisionEmptyState(tab: RevisionTab) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (tab) {
                        RevisionTab.MISTAKES -> Icons.Filled.Inbox
                        RevisionTab.BOOKMARKS -> Icons.Filled.Bookmark
                        RevisionTab.LEARNED -> Icons.Filled.CheckCircle
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (tab) {
                    RevisionTab.MISTAKES -> "No quiz mistakes yet! 🎉"
                    RevisionTab.BOOKMARKS -> "No bookmarked words yet."
                    RevisionTab.LEARNED -> "No learned words yet."
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when (tab) {
                    RevisionTab.MISTAKES -> "When you answer incorrectly during tests, words are automatically collected here for targeted revision."
                    RevisionTab.BOOKMARKS -> "Tap the star icon on any word card to bookmark it for quick access."
                    RevisionTab.LEARNED -> "Complete level learning sessions and mastery quizzes to mark words as learned."
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}
