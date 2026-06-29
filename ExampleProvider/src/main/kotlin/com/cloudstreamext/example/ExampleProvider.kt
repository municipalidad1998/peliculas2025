package com.cloudstreamext.example

import com.cloudstreamext.base.MovieProvider
import com.cloudstreamext.util.*
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkNormalizer
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Example Movie Provider for CloudStream 3 Extensions.
 *
 * This provider demonstrates the full architecture for creating a movie provider:
 * - Main page with popular/latest movies
 * - Search functionality
 * - Movie details loading with metadata extraction
 * - Video stream extraction
 * - Subtitle detection
 * - Error handling with retries
 * - Rate limiting
 * - Smart caching
 *
 * To use this as a template:
 * 1. Copy this file and rename the class
 * 2. Update mainUrl and name
 * 3. Implement the actual scraping logic for your target website
 * 4. Update the selectors to match the website's HTML structure
 */
class ExampleProvider : MovieProvider() {

    // --- Provider Configuration ---

    override var mainUrl = "https://example.com"
    override var name = "Example Movies"
    override val lang = "en"
    override val hasMainPage = true
    override val usesHttps = true

    // Custom headers for this provider
    override val defaultHeaders = mutableMapOf(
        "Accept-Language" to "en-US,en;q=0.9",
        "Referer" to "$mainUrl/"
    )

    // Rate limit between requests (ms)
    override val rateLimitMs = 300L

    // --- Main Page ---

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        return when (page) {
            1 -> newHomePageList("Popular Movies", fetchPopularMovies())
            2 -> newHomePageList("Latest Movies", fetchLatestMovies())
            3 -> newHomePageList("Top Rated", fetchTopRatedMovies())
            else -> null
        }
    }

    /**
     * Fetches popular movies from the main page.
     */
    private suspend fun fetchPopularMovies(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/popular?page=1")
                ?: throw Exception("Failed to load popular movies")

            doc.select(".movie-card").mapNotNull { element ->
                val title = element.selectFirst("h3.title")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = element.selectFirst("img")?.attr("src")
                val year = element.selectFirst(".year")?.text()?.extractYear()

                movieSearchResponse(
                    title = cleanTitle(title),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(poster),
                    year = year
                )
            }
        }
    }

    /**
     * Fetches latest movies from the main page.
     */
    private suspend fun fetchLatestMovies(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/latest?page=1")
                ?: throw Exception("Failed to load latest movies")

            doc.select(".movie-card").mapNotNull { element ->
                val title = element.selectFirst("h3.title")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = element.selectFirst("img")?.attr("src")
                val year = element.selectFirst(".year")?.text()?.extractYear()

                movieSearchResponse(
                    title = cleanTitle(title),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(poster),
                    year = year
                )
            }
        }
    }

    /**
     * Fetches top rated movies from the main page.
     */
    private suspend fun fetchTopRatedMovies(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/top-rated?page=1")
                ?: throw Exception("Failed to load top rated movies")

            doc.select(".movie-card").mapNotNull { element ->
                val title = element.selectFirst("h3.title")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = element.selectFirst("img")?.attr("src")
                val year = element.selectFirst(".year")?.text()?.extractYear()

                movieSearchResponse(
                    title = cleanTitle(title),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(poster),
                    year = year
                )
            }
        }
    }

    // --- Search ---

    override suspend fun search(query: String): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/search?q=${NetworkUtils.encodeUrl(query)}")
                ?: throw Exception("Search failed for query: $query")

            doc.select(".movie-card, .search-result").mapNotNull { element ->
                val title = element.selectFirst("h3, h2, .title")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = element.selectFirst("img")?.attr("src")
                val year = element.selectFirst(".year")?.text()?.extractYear()

                movieSearchResponse(
                    title = cleanTitle(title),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(poster),
                    year = year
                )
            }
        }
    }

    // --- Load Movie Details ---

    override suspend fun load(url: String): LoadResponse? {
        return withRetry {
            val doc = safeDocument(url)
                ?: throw Exception("Failed to load movie details")

            val title = doc.metaContent("og:title")
                ?: doc.selectFirst("h1")?.text()
                ?: throw Exception("Could not extract title")

            val poster = extractPoster(doc)
            val banner = extractBanner(doc)
            val plot = doc.metaContent("og:description")
                ?: doc.selectFirst(".description, .plot, .synopsis")?.text()
            val year = extractYear(doc.selectFirst(".year, [itemprop=datePublished]")?.text() ?: "")
            val durationText = doc.selectFirst(".duration, .runtime, [itemprop=duration]")?.text()
            val duration = durationText?.let { extractDuration(it) }
            val ratingText = doc.selectFirst(".rating, .score, [itemprop=ratingValue]")?.text()
            val rating = ratingText?.let { extractRating(it) }?.times(10)?.toInt()
            val genres = extractGenres(doc)
            val actors = extractActors(doc)

            // Extract trailer URL if available
            val trailerUrl = doc.selectFirst("meta[property=og:video]")
                ?.attr("content")
                ?: doc.selectFirst("a[href*=youtube], a[href*=trailer]")
                ?.attr("href")

            // Extract recommendations
            val recommendations = doc.select(".recommendations .movie-card, .similar .movie-card").mapNotNull { element ->
                val recTitle = element.selectFirst("h3, .title")?.text() ?: return@mapNotNull null
                val recUrl = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val recPoster = element.selectFirst("img")?.attr("src")
                val recYear = element.selectFirst(".year")?.text()?.extractYear()

                movieSearchResponse(
                    title = cleanTitle(recTitle),
                    url = resolveUrl(url, recUrl),
                    poster = normalizeImage(recPoster),
                    year = recYear
                )
            }

            movieLoadResponse(
                title = cleanTitle(title),
                url = url,
                poster = poster,
                background = banner,
                plot = plot,
                year = year,
                duration = duration,
                rating = rating,
                genres = genres,
                actors = actors,
                recommendations = recommendations
            )
        }
    }

    // --- Load Links (Video Extraction) ---

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return try {
            val doc = safeDocument(data)
                ?: throw Exception("Failed to load video page")

            // Extract video sources from the page
            val videoUrls = extractVideoSources(doc)

            // Create extractor links
            videoUrls.forEach { (url, quality, isHls) ->
                callback.invoke(
                    ExtractorLink(
                        source = name,
                        name = name,
                        url = url,
                        referer = data,
                        quality = quality,
                        isM3u8 = isHls,
                        headers = mapOf("Referer" to data)
                    )
                )
            }

            // Extract and provide subtitles
            val subtitles = extractSubtitles(doc, data)
            subtitles.forEach { subtitleCallback.invoke(it) }

            true
        } catch (e: Exception) {
            false
        }
    }

    // --- Helper Methods ---

    /**
     * Resolves a URL relative to the base URL.
     */
    private fun resolveUrl(base: String, relative: String): String {
        if (relative.startsWith("http")) return relative
        if (relative.startsWith("//")) return "https:$relative"
        return "${base.trimEnd('/')}/${relative.trimStart('/')}"
    }

    /**
     * Normalizes an image URL.
     */
    private fun normalizeImage(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            else -> "${mainUrl.trimEnd('/')}/${trimmed.trimStart('/')}"
        }
    }

    /**
     * Extracts video sources from the page HTML.
     * Searches for common video player patterns.
     */
    private fun extractVideoSources(doc: Document): List<Triple<String, String, Boolean>> {
        val sources = mutableListOf<Triple<String, String, Boolean>>()
        val html = doc.html()

        // Pattern 1: Direct video URLs in data attributes
        doc.select("video source, video[src]").forEach { element ->
            val src = element.attr("src").ifBlank { element.attr("data-src") }
            if (src.isNotBlank()) {
                val isHls = src.contains(".m3u8")
                val quality = when {
                    src.contains("1080p") -> "1080p"
                    src.contains("720p") -> "720p"
                    src.contains("480p") -> "480p"
                    else -> "Unknown"
                }
                sources.add(Triple(resolveUrl(data, src), quality, isHls))
            }
        }

        // Pattern 2: Video player iframe
        val iframeSrc = doc.selectFirst("iframe[src*=embed], iframe[src*=player]")?.attr("src")
        if (iframeSrc != null && iframeSrc.isNotBlank()) {
            sources.add(Triple(resolveUrl(data, iframeSrc), "Unknown", false))
        }

        // Pattern 3: JavaScript video data
        val jsDataPatterns = listOf(
            Regex("\"(https?://[^\"*\\.m3u8[^\"]*)\""),
            Regex("'(https?://[^']*\\.m3u8[^']*)'"),
            Regex("source[\"':\\s]+[\"'](https?://[^\"']+\\.m3u8)[\"']"),
            Regex("file[\"':\\s]+[\"'](https?://[^\"']+\\.m3u8)[\"']"),
            Regex("src[\"':\\s]+[\"'](https?://[^\"']+\\.m3u8)[\"']")
        )

        for (pattern in jsDataPatterns) {
            pattern.findAll(html).forEach { match ->
                val url = match.groupValues[1]
                if (url.isNotBlank() && sources.none { it.first == url }) {
                    sources.add(Triple(url, "Unknown", true))
                }
            }
        }

        // Pattern 4: MP4 direct links
        val mp4Patterns = listOf(
            Regex("\"(https?://[^\"*\\.mp4[^\"]*)\""),
            Regex("'(https?://[^']*\\.mp4[^']*)'"),
            Regex("src[\"':\\s]+[\"'](https?://[^\"']+\\.mp4)[\"']")
        )

        for (pattern in mp4Patterns) {
            pattern.findAll(html).forEach { match ->
                val url = match.groupValues[1]
                if (url.isNotBlank() && sources.none { it.first == url }) {
                    val quality = when {
                        url.contains("1080p") -> "1080p"
                        url.contains("720p") -> "720p"
                        url.contains("480p") -> "480p"
                        else -> "Unknown"
                    }
                    sources.add(Triple(url, quality, false))
                }
            }
        }

        return sources
    }

    /**
     * Extracts subtitles from the page.
     */
    private fun extractSubtitles(doc: Document, baseUrl: String): List<SubtitleFile> {
        val subtitles = mutableListOf<SubtitleFile>()

        // Check track elements
        doc.select("track[kind=subtitles], track[kind=captions]").forEach { track ->
            val src = track.attr("src")
            if (src.isNotBlank()) {
                val lang = track.attr("srclang").ifBlank { "en" }
                val label = track.attr("label").ifBlank { lang }
                val fullUrl = resolveUrl(baseUrl, src)
                subtitles.add(SubtitleFile(label, fullUrl))
            }
        }

        // Check for subtitle links in the page
        doc.select("a[href*=.srt], a[href*=.vtt], a[href*=subtitle]").forEach { link ->
            val href = link.attr("href")
            if (href.isNotBlank()) {
                val lang = detectLanguage(link.text() + href)
                val fullUrl = resolveUrl(baseUrl, href)
                subtitles.add(SubtitleFile(lang, fullUrl))
            }
        }

        // Check JavaScript for subtitle data
        val jsSubtitlePattern = Regex("(?:subtitle|subs|captions)[\"':\\s]*\\[([\\s\\S]*?)\\]")
        val html = doc.html()
        jsSubtitlePattern.findAll(html).forEach { match ->
            val subtitleData = match.groupValues[1]
            val urlPattern = Regex("\"(https?://[^\"]+\\.(?:srt|vtt|ass)[^\"]*)\"")
            urlPattern.findAll(subtitleData).forEach { urlMatch ->
                val url = urlMatch.groupValues[1]
                if (url.isNotBlank() && subtitles.none { it.url == url }) {
                    val lang = detectLanguage(url)
                    subtitles.add(SubtitleFile(lang, url))
                }
            }
        }

        return subtitles
    }

    /**
     * Detects language from a string.
     */
    private fun detectLanguage(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("english") || lower.contains("eng") -> "English"
            lower.contains("spanish") || lower.contains("español") || lower.contains("spa") -> "Spanish"
            lower.contains("french") || lower.contains("français") || lower.contains("fre") -> "French"
            lower.contains("german") || lower.contains("deutsch") || lower.contains("ger") -> "German"
            lower.contains("italian") || lower.contains("italiano") || lower.contains("ita") -> "Italian"
            lower.contains("portuguese") || lower.contains("português") || lower.contains("por") -> "Portuguese"
            lower.contains("japanese") || lower.contains("日本語") || lower.contains("jpn") -> "Japanese"
            lower.contains("korean") || lower.contains("한국어") || lower.contains("kor") -> "Korean"
            lower.contains("chinese") || lower.contains("中文") || lower.contains("chi") -> "Chinese"
            lower.contains("arabic") || lower.contains("العربية") || lower.contains("ara") -> "Arabic"
            lower.contains("hindi") || lower.contains("हिन्दी") || lower.contains("hin") -> "Hindi"
            lower.contains("russian") || lower.contains("русский") || lower.contains("rus") -> "Russian"
            lower.contains("turkish") || lower.contains("türkçe") || lower.contains("tur") -> "Turkish"
            lower.contains("thai") || lower.contains("ไทย") || lower.contains("tha") -> "Thai"
            lower.contains("vietnamese") || lower.contains("tiếng việt") -> "Vietnamese"
            else -> "English"
        }
    }
}
