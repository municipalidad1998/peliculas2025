package com.lagradost.cloudstream3

/**
 * Stub Episode class for CloudStream 3.
 */
data class Episode(
    val name: String? = null,
    val url: String,
    val season: Int? = null,
    val episode: Int? = null,
    val poster: String? = null,
    val duration: Int? = null,
    val description: String? = null,
    val date: Long? = null
)
