package com.cloudstreamext.util

/**
 * Utility class for common regex patterns used in web scraping.
 * Centralizes regex patterns to avoid duplication across providers.
 *
 * Usage:
 * ```kotlin
 * val year = RegexHelper.extractYear(pageHtml)
 * val episodeData = RegexHelper.firstMatch(html, "data-episode=\"(.*?)\"")
 * val urls = RegexHelper.findAllUrls(html, "https?://example\\.com/[^\"'\\s]+")
 * ```
 */
object RegexHelper {

    // --- Common Patterns ---

    /** Matches a 4-digit year (1900-2099) */
    private val YEAR_PATTERN = Regex("(?:19|20)\\d{2}")

    /** Matches common video file extensions */
    private val VIDEO_EXTENSIONS = Regex(
        "\\.(mp4|mkv|avi|mov|wmv|flv|webm|m4v|ts|m3u8|mpd)(?:\\?[^\"'\\s]*)?",
        RegexOption.IGNORE_CASE
    )

    /** Matches URLs in HTML/JS (href, src, data attributes) */
    private val URL_PATTERN = Regex(
        "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+",
        RegexOption.IGNORE_CASE
    )

    /** Matches base64 encoded strings (common in obfuscated sources) */
    private val BASE64_PATTERN = Regex("[A-Za-z0-9+/]{40,}={0,2}")

    /** Matches JSON object/array patterns */
    private val JSON_PATTERN = Regex("\\{[^{}]*\\}|\\[[^\\[\\]]*\\]")

    /** Matches episode numbers in various formats: "E01", "Ep 1", "Episode 1", etc. */
    private val EPISODE_NUMBER_PATTERN = Regex(
        "(?:ep(?:isode)?\\.?\\s*)(\\d+)",
        RegexOption.IGNORE_CASE
    )

    /** Matches season numbers: "S01", "Season 1", etc. */
    private val SEASON_NUMBER_PATTERN = Regex(
        "(?:s(?:eason)?\\.?\\s*)(\\d+)",
        RegexOption.IGNORE_CASE
    )

    /** Matches quality labels: "1080p", "720p", "4K", etc. */
    private val QUALITY_PATTERN = Regex(
        "(?:4k|uhd|(?:2160|1080|720|480|360|240)p)",
        RegexOption.IGNORE_CASE
    )

    /** Matches duration strings: "1h 30m", "90 min", "01:30:00" */
    private val DURATION_HM_PATTERN = Regex("(\\d+)\\s*h\\s*(\\d+)?\\s*m?", RegexOption.IGNORE_CASE)
    private val DURATION_MIN_PATTERN = Regex("(\\d+)\\s*(?:min|m(?:in)?)(?:utes?)?", RegexOption.IGNORE_CASE)
    private val DURATION_TIME_PATTERN = Regex("(\\d+):(\\d+):(\\d+)")

    // --- Extraction Methods ---

    /**
     * Finds the first match of a regex pattern and returns the specified group.
     */
    fun firstMatch(text: String, pattern: String, group: Int = 1): String? {
        return Regex(pattern, RegexOption.IGNORE_CASE)
            .find(text)
            ?.groupValues
            ?.getOrNull(group)
    }

    /**
     * Finds all matches of a regex pattern and returns the specified group for each.
     */
    fun findAllMatches(text: String, pattern: String, group: Int = 1): List<String> {
        return Regex(pattern, RegexOption.IGNORE_CASE)
            .findAll(text)
            .mapNotNull { it.groupValues.getOrNull(group) }
            .toList()
    }

    /**
     * Checks if a pattern exists in the text.
     */
    fun contains(text: String, pattern: String): Boolean {
        return Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(text)
    }

    // --- Domain-Specific Extractions ---

    /**
     * Extracts all URLs from text.
     */
    fun extractUrls(text: String): List<String> {
        return URL_PATTERN.findAll(text).map { it.value }.distinct().toList()
    }

    /**
     * Extracts all video file URLs from text.
     */
    fun extractVideoUrls(text: String): List<String> {
        return URL_PATTERN.findAll(text)
            .map { it.value }
            .filter { VIDEO_EXTENSIONS.containsMatchIn(it) }
            .distinct()
            .toList()
    }

    /**
     * Extracts the first year found in text.
     */
    fun extractYear(text: String): Int? {
        return YEAR_PATTERN.find(text)?.value?.toIntOrNull()
    }

    /**
     * Extracts all years found in text.
     */
    fun extractYears(text: String): List<Int> {
        return YEAR_PATTERN.findAll(text).mapNotNull { it.value.toIntOrNull() }.distinct().toList()
    }

    /**
     * Extracts episode number from text.
     */
    fun extractEpisodeNumber(text: String): Int? {
        return EPISODE_NUMBER_PATTERN.find(text)?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * Extracts season number from text.
     */
    fun extractSeasonNumber(text: String): Int? {
        return SEASON_NUMBER_PATTERN.find(text)?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * Extracts quality label from text.
     */
    fun extractQuality(text: String): String? {
        return QUALITY_PATTERN.find(text)?.value?.uppercase()
    }

    /**
     * Extracts duration in minutes from text.
     */
    fun extractDurationMinutes(text: String): Int? {
        // Try "Xh Ym" format
        val hmMatch = DURATION_HM_PATTERN.find(text.lowercase())
        if (hmMatch != null) {
            val hours = hmMatch.groupValues[1].toIntOrNull() ?: 0
            val minutes = hmMatch.groupValues[2].toIntOrNull() ?: 0
            return hours * 60 + minutes
        }
        // Try "X min" format
        val minMatch = DURATION_MIN_PATTERN.find(text.lowercase())
        if (minMatch != null) {
            return minMatch.groupValues[1].toIntOrNull()
        }
        // Try HH:MM:SS format
        val timeMatch = DURATION_TIME_PATTERN.find(text)
        if (timeMatch != null) {
            val hours = timeMatch.groupValues[1].toIntOrNull() ?: 0
            val minutes = timeMatch.groupValues[2].toIntOrNull() ?: 0
            return hours * 60 + minutes
        }
        return null
    }

    /**
     * Extracts a base64 string from text.
     */
    fun extractBase64(text: String): String? {
        return BASE64_PATTERN.find(text)?.value
    }

    /**
     * Decodes a base64 string.
     */
    fun decodeBase64(encoded: String): String? {
        return try {
            android.util.Base64.decode(encoded, android.util.Base64.DEFAULT).toString(Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extracts a JSON object or array from text.
     */
    fun extractJson(text: String): String? {
        return JSON_PATTERN.find(text)?.value
    }

    /**
     * Extracts a variable value from JavaScript.
     * Example: extractJsVar(js, "var dataUrl = ", "'", ";") -> value between quotes
     */
    fun extractJsVar(
        js: String,
        varName: String,
        startDelim: String = "\"",
        endDelim: String = "\""
    ): String? {
        val pattern = Regex(
            Regex.escape(varName) + "\\s*=\\s*" + Regex.escape(startDelim) + "(.*?)" + Regex.escape(endDelim),
            RegexOption.DOT_MATCHES_ALL
        )
        return pattern.find(js)?.groupValues?.get(1)
    }

    /**
     * Extracts a function return value from JavaScript.
     * Example: extractJsFunction(js, "getLink", "return \"", "\"") -> return value
     */
    fun extractJsFunction(
        js: String,
        funcName: String,
        startDelim: String = "return \"",
        endDelim: String = "\""
    ): String? {
        val pattern = Regex(
            "function\\s+${Regex.escape(funcName)}\\s*\\([^)]*\\)\\s*\\{[^}]*${Regex.escape(startDelim)}(.*?${Regex.escape(endDelim)})",
            RegexOption.DOT_MATCHES_ALL
        )
        return pattern.find(js)?.groupValues?.get(1)
    }

    /**
     * Extracts all source URLs from a JavaScript file that look like video sources.
     */
    fun extractSourceUrls(js: String): List<String> {
        val patterns = listOf(
            Regex("\"(https?://[^\"]*\\.(?:mp4|mkv|m3u8|mpd|m3u8|ts)[^\"]*)\""),
            Regex("'(https?://[^']*\\.(?:mp4|mkv|m3u8|mpd|m3u8|ts)[^']*)'"),
            Regex("src[\"':\\s]+(https?://[^\"'\\s]*\\.(?:mp4|mkv|m3u8|mpd|ts))"),
            Regex("file[\"':\\s]+(https?://[^\"'\\s]*\\.(?:mp4|mkv|m3u8|mpd|ts))"),
            Regex("url[\"':\\s]+(https?://[^\"'\\s]*\\.(?:mp4|mkv|m3u8|mpd|ts))")
        )
        return patterns.flatMap { pattern ->
            pattern.findAll(js).map { it.groupValues[1] }
        }.distinct()
    }

    /**
     * Replaces all occurrences of a pattern in text.
     */
    fun replaceAll(text: String, pattern: String, replacement: String): String {
        return Regex(pattern, RegexOption.IGNORE_CASE).replace(text, replacement)
    }

    /**
     * Splits text by a regex pattern and trims each part.
     */
    fun splitAndTrim(text: String, pattern: String): List<String> {
        return Regex(pattern, RegexOption.IGNORE_CASE)
            .split(text)
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
