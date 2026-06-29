package com.cloudstreamext.util

import com.lagradost.cloudstream3.SubtitleFile
import org.jsoup.Jsoup

/**
 * Utility class for detecting, parsing, and extracting subtitles from web pages.
 * Supports common subtitle formats and subtitle provider APIs.
 *
 * Usage:
 * ```kotlin
 * val extractor = SubtitleExtractor()
 * val subtitles = extractor.extractFromPage(pageHtml, baseUrl)
 * subtitles.forEach { subtitleCallback.invoke(it) }
 * ```
 */
class SubtitleExtractor {

    /**
     * Supported subtitle formats.
     */
    enum class SubtitleFormat(val mimeType: String, val extension: String) {
        SRT("application/x-subrip", "srt"),
        VTT("text/vtt", "vtt"),
        ASS("text/x-ssa", "ass"),
        SSA("text/x-ssa", "ssa"),
        SUB("application/x-subviewer", "sub"),
        SBV("text/x-subviewer", "sbv"),
        DFXP("application/ttml+xml", "dfxp"),
        TTML("application/ttml+xml", "ttml"),
        UNKNOWN("application/x-subrip", "srt")
    }

    /**
     * Subtitle info extracted from a page.
     */
    data class SubtitleInfo(
        val url: String,
        val language: String = "English",
        val languageCode: String = "en",
        val format: SubtitleFormat = SubtitleFormat.SRT,
        val label: String = ""
    ) {
        fun toSubtitleFile(): SubtitleFile {
            return SubtitleFile(
                name = label.ifBlank { language },
                url = url,
                mimeType = format.mimeType
            )
        }
    }

    companion object {
        /**
         * Detects subtitle format from a URL.
         */
        fun detectFormat(url: String): SubtitleFormat {
            val lower = url.lowercase()
            return when {
                lower.endsWith(".srt") || lower.contains(".srt?") -> SubtitleFormat.SRT
                lower.endsWith(".vtt") || lower.contains(".vtt?") -> SubtitleFormat.VTT
                lower.endsWith(".ass") || lower.contains(".ass?") -> SubtitleFormat.ASS
                lower.endsWith(".ssa") || lower.contains(".ssa?") -> SubtitleFormat.SSA
                lower.endsWith(".sub") || lower.contains(".sub?") -> SubtitleFormat.SUB
                lower.endsWith(".sbv") || lower.contains(".sbv?") -> SubtitleFormat.SBV
                lower.endsWith(".dfxp") -> SubtitleFormat.DFXP
                lower.endsWith(".ttml") -> SubtitleFormat.TTML
                else -> SubtitleFormat.SRT
            }
        }

        /**
         * Common language code to name mappings.
         */
        private val languageNames = mapOf(
            "en" to "English", "es" to "Spanish", "fr" to "French",
            "de" to "German", "it" to "Italian", "pt" to "Portuguese",
            "ja" to "Japanese", "ko" to "Korean", "zh" to "Chinese",
            "ar" to "Arabic", "hi" to "Hindi", "ru" to "Russian",
            "tr" to "Turkish", "th" to "Thai", "vi" to "Vietnamese",
            "nl" to "Dutch", "pl" to "Polish", "sv" to "Swedish",
            "da" to "Danish", "no" to "Norwegian", "fi" to "Finnish",
            "id" to "Indonesian", "ms" to "Malay", "uk" to "Ukrainian",
            "cs" to "Czech", "el" to "Greek", "he" to "Hebrew",
            "ro" to "Romanian", "hu" to "Hungarian", "bg" to "Bulgarian"
        )

        /**
         * Gets the full language name from a code.
         */
        fun getLanguageName(code: String): String {
            return languageNames[code.lowercase()] ?: code.uppercase()
        }

        /**
         * Detects the language code from a filename or label.
         */
        fun detectLanguage(text: String): String {
            val lower = text.lowercase()
            // Check for explicit language codes in brackets: [en], (es), _fr_
            val bracketMatch = Regex("[\\[\\(]([a-z]{2})[\\]\\)]").find(lower)
            if (bracketMatch != null) return bracketMatch.groupValues[1]

            // Check for language names
            for ((code, name) in languageNames) {
                if (lower.contains(name.lowercase())) return code
            }

            // Check for common abbreviations
            val abbreviations = mapOf(
                "eng" to "en", "spa" to "es", "fre" to "fr", "fra" to "fr",
                "ger" to "de", "deu" to "de", "ita" to "it", "por" to "pt",
                "jpn" to "ja", "kor" to "ko", "chi" to "zh", "zho" to "zh",
                "ara" to "ar", "hin" to "hi", "rus" to "ru", "tur" to "tr",
                "lat" to "es", "cast" to "es" // lat = latino, cast = castellano
            )
            for ((abbrev, code) in abbreviations) {
                if (lower.contains(abbrev)) return code
            }

            return "en" // default
        }
    }

    /**
     * Checks if a URL is a subtitle file.
     */
    fun isSubtitleUrl(url: String): Boolean {
        val format = detectFormat(url)
        return format != SubtitleFormat.UNKNOWN || url.contains("subtitle", ignoreCase = true)
    }

    /**
     * Extracts subtitle links from HTML page content.
     * Searches for common subtitle elements and patterns.
     */
    fun extractFromPage(html: String, baseUrl: String): List<SubtitleInfo> {
        val subtitles = mutableListOf<SubtitleInfo>()
        val doc = Jsoup.parse(html)

        // Pattern 1: <track> elements (video players)
        doc.select("track[kind=subtitles], track[kind=captions]").forEach { track ->
            val src = track.attr("src")
            if (src.isNotBlank()) {
                val lang = track.attr("srclang").ifBlank { "en" }
                val label = track.attr("label").ifBlank { getLanguageName(lang) }
                subtitles.add(
                    SubtitleInfo(
                        url = resolveUrl(baseUrl, src),
                        language = label,
                        languageCode = lang,
                        format = detectFormat(src),
                        label = label
                    )
                )
            }
        }

        // Pattern 2: Subtitle download links
        val subtitlePatterns = listOf(
            "a[href*=subtitle]", "a[href*=subtitulo]", "a[href*=sous-titre]",
            "a[href*=untertitel]", "a[href*=.srt]", "a[href*=.vtt]",
            "a[href*=.ass]", "a[data-subtitle]", ".subtitle a", ".subs a"
        )

        for (selector in subtitlePatterns) {
            doc.select(selector).forEach { link ->
                val href = link.attr("href")
                if (href.isNotBlank()) {
                    val lang = detectLanguage(link.text() + href)
                    val format = detectFormat(href)
                    subtitles.add(
                        SubtitleInfo(
                            url = resolveUrl(baseUrl, href),
                            language = getLanguageName(lang),
                            languageCode = lang,
                            format = format,
                            label = link.text().trim().ifBlank { getLanguageName(lang) }
                        )
                    )
                }
            }
        }

        // Pattern 3: JavaScript subtitle data
        val jsSubtitlePatterns = listOf(
            Regex("\"(https?://[^\"]*\\.srt[^\"]*)\""),
            Regex("'(https?://[^']*\\.srt[^']*)'"),
            Regex("\"(https?://[^\"]*\\.vtt[^\"]*)\""),
            Regex("'(https?://[^']*\\.vtt[^']*)'"),
            Regex("subtitle[\"':\\s]*(?:url|src|file)[\"':\\s]+[\"']([^\"']+)[\"']")
        )

        for (pattern in jsSubtitlePatterns) {
            pattern.findAll(html).forEach { match ->
                val url = match.groupValues[1]
                if (url.isNotBlank()) {
                    val lang = detectLanguage(url)
                    val format = detectFormat(url)
                    if (subtitles.none { it.url == url }) {
                        subtitles.add(
                            SubtitleInfo(
                                url = url,
                                language = getLanguageName(lang),
                                languageCode = lang,
                                format = format,
                                label = getLanguageName(lang)
                            )
                        )
                    }
                }
            }
        }

        return subtitles.distinctBy { it.url }
    }

    /**
     * Extracts subtitle data from a JSON array of subtitle objects.
     * Common format: [{"label":"English","file":"url.srt","country":"en"}]
     */
    fun extractFromJson(subtitlesJson: String): List<SubtitleInfo> {
        val subtitles = mutableListOf<SubtitleInfo>()
        val doc = Jsoup.parseBodyFragment(subtitlesJson)
        // Simple JSON parsing with regex for subtitle objects
        val labelPattern = Regex("\"label\"\\s*:\\s*\"([^\"]+)\"")
        val filePattern = Regex("\"(?:file|url)\"\\s*:\\s*\"([^\"]+)\"")
        val countryPattern = Regex("\"(?:country|lang|language)\"\\s*:\\s*\"([^\"]+)\"")

        val labels = labelPattern.findAll(subtitlesJson).map { it.groupValues[1] }.toList()
        val files = filePattern.findAll(subtitlesJson).map { it.groupValues[1] }.toList()
        val countries = countryPattern.findAll(subtitlesJson).map { it.groupValues[1] }.toList()

        for (i in files.indices) {
            val url = files[i]
            val label = labels.getOrElse(i) { "" }
            val country = countries.getOrElse(i) { detectLanguage(url + label) }
            subtitles.add(
                SubtitleInfo(
                    url = url,
                    language = if (label.isNotBlank()) label else getLanguageName(country),
                    languageCode = country,
                    format = detectFormat(url),
                    label = label.ifBlank { getLanguageName(country) }
                )
            )
        }

        return subtitles.distinctBy { it.url }
    }

    /**
     * Resolves a URL against a base URL.
     */
    private fun resolveUrl(baseUrl: String, url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        if (url.startsWith("//")) return "https:$url"
        val base = baseUrl.trimEnd('/')
        val path = url.trimStart('/')
        return "$base/$path"
    }
}
