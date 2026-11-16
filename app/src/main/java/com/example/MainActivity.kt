package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.UserProgressRepository
import com.example.data.VocabularyRepository
import com.example.tts.TextToSpeechHelper
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var vocabularyRepository: VocabularyRepository
    private lateinit var userProgressRepository: UserProgressRepository
    private lateinit var ttsHelper: TextToSpeechHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        vocabularyRepository = VocabularyRepository(this)
        userProgressRepository = UserProgressRepository(this)
        ttsHelper = TextToSpeechHelper(this)

        // Set TTS speech speed from saved user preference
        ttsHelper.setSpeechRate(userProgressRepository.speechSpeed.value)

        setContent {
            MyApplicationTheme {
                AppNavigation(
                    vocabularyRepository = vocabularyRepository,
                    userProgressRepository = userProgressRepository,
                    ttsHelper = ttsHelper
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsHelper.shutdown()
    }
}
