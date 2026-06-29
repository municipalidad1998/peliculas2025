package com.lagradost.cloudstream3.utils

/**
 * Stub ExtractorLink class for CloudStream 3.
 */
data class ExtractorLink(
    val source: String = "",
    val name: String = "",
    val url: String,
    val referer: String = "",
    val quality: String = "Unknown",
    val isM3u8: Boolean = false,
    val headers: Map<String, String> = emptyMap(),
    val extractorData: String = "",
    val type: ExtractorLinkType = ExtractorLinkType.NONE
)

/**
 * Stub ExtractorLinkType.
 */
enum class ExtractorLinkType {
    NONE,
    M3U8,
    DASH,
    MP4
}
