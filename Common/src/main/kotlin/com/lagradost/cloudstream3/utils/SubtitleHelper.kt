package com.lagradost.cloudstream3.utils

/**
 * Stub SubtitleHelper utility.
 */
object SubtitleHelper {
    fun getLanguageCode(language: String): String {
        return language.take(2).lowercase()
    }
}
