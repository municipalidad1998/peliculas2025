package com.cloudstreamext.util

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkNormalizer

/**
 * Extension functions for CloudStream types to simplify provider development.
 *
 * Usage:
 * ```kotlin
 * import com.cloudstreamext.util.*
 *
 * val link = createExtractorLink("MySource", "https://example.com/video.mp4", "1080p")
 * val cleanTitle = "<b>Bold Text</b>".stripHtml()
 * ```
 */

// --- ExtractorLink Extensions ---

/**
 * Creates an ExtractorLink with simplified parameters.
 */
fun createExtractorLink(
    source: String,
    url: String,
    quality: String = "Unknown",
    referer: String = "",
    isM3u8: Boolean = false,
    headers: Map<String, String> = emptyMap(),
    data: String = ""
): ExtractorLink {
    return ExtractorLink(
        source = source,
        name = source,
        url = url,
        referer = referer,
        quality = quality,
        isM3u8 = isM3u8,
        headers = headers,
        extractorData = data
    )
}

/**
 * Creates an HLS ExtractorLink.
 */
fun createHlsLink(
    source: String,
    url: String,
    quality: String = "Unknown",
    referer: String = "",
    headers: Map<String, String> = emptyMap()
): ExtractorLink {
    return createExtractorLink(
        source = source,
        url = url,
        quality = quality,
        referer = referer,
        isM3u8 = true,
        headers = headers
    )
}

// --- String Extensions ---

/**
 * Strips HTML tags from a string and returns clean text.
 */
fun String.stripHtml(): String {
    return this.replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

/**
 * Cleans a title by removing extra whitespace and HTML entities.
 */
fun String.cleanTitle(): String {
    return this.stripHtml().trim()
}

/**
 * Extracts the first year found in a string.
 */
fun String.extractYear(): Int? {
    return Regex("(19|20)\\d{2}").find(this)?.value?.toIntOrNull()
}

/**
 * Extracts the first episode number found in a string.
 */
fun String.extractEpisodeNumber(): Int? {
    return Regex("(?:ep(?:isode)?\\.?\\s*)(\\d+)", RegexOption.IGNORE_CASE)
        .find(this)
        ?.groupValues
        ?.get(1)
        ?.toIntOrNull()
}

/**
 * Extracts the first season number found in a string.
 */
fun String.extractSeasonNumber(): Int? {
    return Regex("(?:s(?:eason)?\\.?\\s*)(\\d+)", RegexOption.IGNORE_CASE)
        .find(this)
        ?.groupValues
        ?.get(1)
        ?.toIntOrNull()
}

/**
 * Checks if a string looks like a video URL.
 */
fun String.isVideoUrl(): Boolean {
    val lower = this.lowercase()
    return lower.endsWith(".mp4") ||
            lower.endsWith(".mkv") ||
            lower.endsWith(".avi") ||
            lower.endsWith(".mov") ||
            lower.endsWith(".m3u8") ||
            lower.endsWith(".mpd") ||
            lower.endsWith(".ts") ||
            lower.contains(".mp4?") ||
            lower.contains(".m3u8?")
}

/**
 * Checks if a string looks like a subtitle URL.
 */
fun String.isSubtitleUrl(): Boolean {
    val lower = this.lowercase()
    return lower.endsWith(".srt") ||
            lower.endsWith(".vtt") ||
            lower.endsWith(".ass") ||
            lower.endsWith(".ssa") ||
            lower.contains(".srt?")
}

// --- Document Extensions ---

/**
 * Extracts meta property content from a document.
 */
fun org.jsoup.nodes.Document.metaContent(property: String): String? {
    return this.selectFirst("meta[property=$property]")?.attr("content")?.trim()?.ifBlank { null }
}

/**
 * Extracts meta name content from a document.
 */
fun org.jsoup.nodes.Document.metaName(name: String): String? {
    return this.selectFirst("meta[name=$name]")?.attr("content")?.trim()?.ifBlank { null }
}

/**
 * Extracts the Open Graph image URL.
 */
fun org.jsoup.nodes.Document.ogImage(): String? {
    return metaContent("og:image")
}

/**
 * Extracts the Open Graph title.
 */
fun org.jsoup.nodes.Document.ogTitle(): String? {
    return metaContent("og:title")
}

/**
 * Extracts the Open Graph description.
 */
fun org.jsoup.nodes.Document.ogDescription(): String? {
    return metaContent("og:description")
}

/**
 * Extracts the page title, with fallback to og:title.
 */
fun org.jsoup.nodes.Document.pageTitle(): String? {
    return metaContent("og:title")
        ?: this.selectFirst("title")?.text()?.trim()?.ifBlank { null }
}

// --- List Extensions ---

/**
 * Finds the first non-null result from a list of nullable transformations.
 */
fun <T, R> List<T>.firstMapNotNull(transform: (T) -> R?): R? {
    for (item in this) {
        val result = transform(item)
        if (result != null) return result
    }
    return null
}

/**
 * Groups a list of episodes by season number.
 */
fun List<Episode>.groupBySeason(): Map<Int, List<Episode>> {
    return this.groupBy { it.season ?: 1 }
}

/**
 * Sorts a list of ExtractorLinks by quality (highest first).
 */
fun List<ExtractorLink>.sortByQuality(): List<ExtractorLink> {
    return this.sortedByDescending { link ->
        when {
            link.quality.contains("2160p") || link.quality.contains("4k", true) -> 8
            link.quality.contains("1080p") -> 6
            link.quality.contains("720p") -> 5
            link.quality.contains("480p") -> 4
            link.quality.contains("360p") -> 3
            else -> 0
        }
    }
}
