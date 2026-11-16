package com.example.data

import com.example.model.VocabularyCategory
import com.example.model.VocabularyWord
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object CsvParser {

    /**
     * Parses an input stream of CSV content into a list of [VocabularyWord] objects.
     * Safely handles:
     * - UTF-8 BOM characters (\uFEFF)
     * - Quoted fields containing commas and quotes
     * - Inconsistent line breaks
     * - Missing columns and trailing empty lines
     */
    fun parseVocabularyCsv(
        inputStream: InputStream,
        category: VocabularyCategory
    ): List<VocabularyWord> {
        val words = mutableListOf<VocabularyWord>()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val lines = mutableListOf<String>()

        reader.useLines { sequence ->
            sequence.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    lines.add(trimmed)
                }
            }
        }

        if (lines.isEmpty()) return emptyList()

        // Locate header line that contains "word_id"
        var headerIndex = 0
        while (headerIndex < lines.size) {
            val candidate = lines[headerIndex].removePrefix("\uFEFF").lowercase()
            if (candidate.contains("word_id") && candidate.contains("word")) {
                break
            }
            headerIndex++
        }
        if (headerIndex >= lines.size) return emptyList()

        val headerLine = lines[headerIndex].removePrefix("\uFEFF")
        val headers = parseCsvRow(headerLine).map { it.trim().lowercase() }
        val idIdx = headers.indexOf("word_id")
        val wordIdx = headers.indexOf("word")
        val engPronIdx = headers.indexOf("english_pronunciation")
        val hinPronIdx = headers.indexOf("hindi_pronunciation")
        val engMeanIdx = headers.indexOf("english_meaning")
        val hinMeanIdx = headers.indexOf("hindi_meaning")
        val synonymsIdx = headers.indexOf("synonyms")
        val exampleIdx = headers.indexOf("example")

        val wrongIndices = mutableListOf<Int>()
        for (i in 1..6) {
            val idx = headers.indexOf("wrong_option_$i")
            if (idx != -1) {
                wrongIndices.add(idx)
            }
        }

        for (i in (headerIndex + 1) until lines.size) {
            val rawLine = lines[i]
            val columns = parseCsvRow(rawLine)
            if (columns.isEmpty()) continue

            fun col(index: Int): String {
                return if (index in columns.indices) columns[index].trim() else ""
            }

            val id = col(idIdx)
            val word = col(wordIdx)
            if (id.isEmpty() || word.isEmpty()) continue

            val engPron = col(engPronIdx)
            val hinPron = col(hinPronIdx)
            val engMean = col(engMeanIdx)
            val hinMean = col(hinMeanIdx)
            val rawSynonyms = col(synonymsIdx)
            val synonyms = if (rawSynonyms.isNotEmpty()) {
                rawSynonyms.split(";").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                emptyList()
            }
            val example = col(exampleIdx)

            val wrongOptions = mutableListOf<String>()
            for (wIdx in wrongIndices) {
                val opt = col(wIdx)
                if (opt.isNotEmpty()) {
                    wrongOptions.add(opt)
                }
            }

            words.add(
                VocabularyWord(
                    id = id,
                    word = word,
                    englishPronunciation = engPron,
                    hindiPronunciation = hinPron,
                    englishMeaning = engMean,
                    hindiMeaning = hinMean,
                    synonyms = synonyms,
                    example = example,
                    wrongOptions = wrongOptions,
                    category = category
                )
            )
        }

        return words
    }

    /**
     * Splits a single line of CSV text respecting quotes and escaped quotes.
     */
    fun parseCsvRow(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = java.lang.StringBuilder()
        var insideQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (insideQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    // Escaped quote: "" -> "
                    current.append('"')
                    i++ // Skip second quote
                } else {
                    insideQuotes = !insideQuotes
                }
            } else if (c == ',' && !insideQuotes) {
                result.add(current.toString())
                current.setLength(0)
            } else {
                current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
