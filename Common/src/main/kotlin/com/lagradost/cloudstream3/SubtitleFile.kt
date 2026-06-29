package com.lagradost.cloudstream3

/**
 * Stub SubtitleFile class for CloudStream 3.
 */
data class SubtitleFile(
    val name: String,
    val url: String,
    val mimeType: String = "application/x-subrip",
    val language: String = "en"
)
