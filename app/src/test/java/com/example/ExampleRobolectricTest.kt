package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CsvParser
import com.example.data.UserProgressRepository
import com.example.data.VocabularyRepository
import com.example.model.VocabularyCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("English Learning", appName)
  }

  @Test
  fun `test csv parsing from assets`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = VocabularyRepository(context)
    val everydayWords = repository.getWordsForCategory(VocabularyCategory.EVERYDAY_WORDS)
    assertTrue("Everyday words should not be empty", everydayWords.isNotEmpty())
    assertEquals("EW0001", everydayWords[0].id)
    assertEquals("name", everydayWords[0].word)
    assertEquals("नेम", everydayWords[0].hindiPronunciation)
    assertEquals("नाम", everydayWords[0].hindiMeaning)
  }

  @Test
  fun `test dynamic level generation`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val vocabRepo = VocabularyRepository(context)
    val progressRepo = UserProgressRepository(context)

    val levels = vocabRepo.getLevelsForCategory(
      category = VocabularyCategory.EVERYDAY_WORDS,
      unlockedLevelKeys = progressRepo.unlockedLevels.value,
      completedLevelKeys = progressRepo.completedLevels.value,
      learnedWordIds = progressRepo.learnedWords.value
    )

    assertTrue("Should generate dynamic levels", levels.isNotEmpty())
    assertEquals(1, levels[0].levelNumber)
    assertEquals(10, levels[0].totalWords)
    assertTrue("Level 1 should be unlocked", levels[0].isUnlocked)
  }

  @Test
  fun `verify all five categories load full word lists`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = VocabularyRepository(context)

    val ew = repository.getWordsForCategory(VocabularyCategory.EVERYDAY_WORDS)
    val ue = repository.getWordsForCategory(VocabularyCategory.USEFUL_ENGLISH)
    val cc = repository.getWordsForCategory(VocabularyCategory.COMMON_CONVERSATIONS)
    val pv = repository.getWordsForCategory(VocabularyCategory.PROFICIENT_VOCABULARY)
    val mv = repository.getWordsForCategory(VocabularyCategory.MASTER_VOCABULARY)

    assertEquals(320, ew.size)
    assertEquals(300, ue.size)
    assertEquals(180, cc.size)
    assertEquals(300, pv.size)
    assertEquals(200, mv.size)

    assertEquals("PV0001", pv[0].id)
    assertEquals("PV0300", pv[299].id)
    assertEquals("MV0001", mv[0].id)
    assertEquals("MV0200", mv[199].id)
  }
}
