package com.example.ui.categories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.VocabularyCategory
import com.example.ui.components.CommonTopBar
import com.example.ui.home.CategoryCard
import com.example.ui.home.HomeViewModel

@Composable
fun CategoriesScreen(
    homeViewModel: HomeViewModel,
    onCategoryClick: (VocabularyCategory) -> Unit
) {
    val state by homeViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CommonTopBar(
                title = "Learning Categories",
                subtitle = "Vocabulary Categories"
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("categories_screen_content"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(state.categorySummaries) { summary ->
                CategoryCard(
                    summary = summary,
                    onClick = { onCategoryClick(summary.category) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
