package com.cloudstreamext.example

import com.cloudstreamext.base.MovieProvider
import com.cloudstreamext.util.*
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Document

/**
 * Example Movie Provider for CloudStream 3 Extensions.
 *
 * Demonstrates the full architecture for creating a movie provider:
 * - Main page with popular/latest movies
 * - Search functionality
 * - Movie details loading with metadata extraction
 * - Video stream extraction
 * - Subtitle detection
 * - Error handling with retries, rate limiting, and smart caching
 */
class ExampleProvider : MovieProvider() {

    override var mainUrl = "https://example.com"
    override var name = "Example Movies"
    override var lang = "en"
    override var hasMainPage = true
    override var usesHttps = true

    // --- Main Page ---

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        return when (page) {
            1 -> newHomePageList("Popular Movies", fetchPopularMovies())
            2 -> newHomePageList("Latest Movies", fetchLatestMovies())
            3 -> newHomePageList("Top Rated", fetchTopRatedMovies())
            else -> null
        }
    }

    private suspend fun fetchPopularMovies(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/popular?page=1")
                ?: throw Exception("Failed to load popular movies")
            doc.select(".movie-card").mapNotNull { el ->
                val title = el.selectFirst("h3.title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src")
                val year = el.selectFirst(".year")?.text()?.extractYear()
                movieSearchResponse(cleanTitle(title), resolveUrl(mainUrl, href), normalizeImage(poster), year)
            }
        }
    }

    private suspend fun fetchLatestMovies(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/latest?page=1")
                ?: throw Exception("Failed to load latest movies")
            doc.select(".movie-card").mapNotNull { el ->
                val title = el.selectFirst("h3.title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src")
                val year = el.selectFirst(".year")?.text()?.extractYear()
                movieSearchResponse(cleanTitle(title), resolveUrl(mainUrl, href), normalizeImage(poster), year)
            }
        }
    }

    private suspend fun fetchTopRatedMovies(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/top-rated?page=1")
                ?: throw Exception("Failed to load top rated movies")
            doc.select(".movie-card").mapNotNull { el ->
                val title = el.selectFirst("h3.title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src")
                val year = el.selectFirst(".year")?.text()?.extractYear()
                movieSearchResponse(cleanTitle(title), resolveUrl(mainUrl, href), normalizeImage(poster), year)
            }
        }
    }

    // --- Search ---

    override suspend fun search(query: String): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/search?q=${query.urlEncode()}")
                ?: throw Exception("Search failed")
            doc.select(".movie-card, .search-result").mapNotNull { el ->
                val title = el.selectFirst("h3, h2, .title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src")
                val year = el.selectFirst(".year")?.text()?.extractYear()
                movieSearchResponse(cleanTitle(title), resolveUrl(mainUrl, href), normalizeImage(poster), year)
            }
        }
    }

    // --- Load Movie Details ---

    override suspend fun load(url: String): LoadResponse? {
        return withRetry {
            val doc = safeDocument(url) ?: throw Exception("Failed to load movie details")
            val title = doc.ogTitle() ?: doc.selectFirst("h1")?.text() ?: throw Exception("No title")
            val poster = extractPoster(doc)
            val banner = extractBanner(doc)
            val plot = doc.ogDescription() ?: doc.selectFirst(".description, .plot")?.text()
            val year = extractYear(doc.selectFirst(".year")?.text() ?: "")
            val duration = extractDuration(doc.selectFirst(".duration, .runtime")?.text() ?: "")
            val genres = extractGenres(doc)
            val actors = extractActors(doc)
            val recommendations = doc.select(".recommendations .movie-card").mapNotNull { el ->
                val recTitle = el.selectFirst("h3")?.text() ?: return@mapNotNull null
                val recUrl = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                movieSearchResponse(cleanTitle(recTitle), resolveUrl(url, recUrl))
            }
            movieLoadResponse(cleanTitle(title), url, poster, banner, plot, year, duration, genres = genres, actors = actors, recommendations = recommendations)
        }
    }

    // --- Load Links ---

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return try {
            val doc = safeDocument(data) ?: throw Exception("Failed to load video page")
            val html = doc.html()
            extractVideoUrls(html, data).forEach { (url, quality, isHls) ->
                callback(ExtractorLink(name, name, url, data, quality, isHls, mapOf("Referer" to data)))
            }
            extractSubtitles(doc, data).forEach { subtitleCallback(it) }
            true
        } catch (_: Exception) { false }
    }

    // --- Helper Methods ---

    private fun resolveUrl(base: String, relative: String): String {
        if (relative.startsWith("http")) return relative
        if (relative.startsWith("//")) return "https:$relative"
        return "${base.trimEnd('/')}/${relative.trimStart('/')}"
    }

    private fun String.urlEncode(): String = java.net.URLEncoder.encode(this, "UTF-8")

    private fun extractVideoUrls(html: String, baseUrl: String): List<Triple<String, String, Boolean>> {
        val sources = mutableListOf<Triple<String, String, Boolean>>()
        val patterns = listOf(
            Regex("\"(https?://[^\"]+\\.m3u8[^\"]*)\"") to true,
            Regex("'(https?://[^']+\\.m3u8[^']*)'") to true,
            Regex("\"(https?://[^\"]+\\.mp4[^\"]*)\"") to false,
            Regex("'(https?://[^']+\\.mp4[^']*)'") to false
        )
        for ((pattern, isHls) in patterns) {
            pattern.findAll(html).forEach { match ->
                val url = match.groupValues[1]
                if (url.isNotBlank() && sources.none { it.first == url }) {
                    val quality = when {
                        url.contains("1080p") -> "1080p"
                        url.contains("720p") -> "720p"
                        url.contains("480p") -> "480p"
                        else -> "Unknown"
                    }
                    sources.add(Triple(url, quality, isHls))
                }
            }
        }
        return sources
    }

    private fun extractSubtitles(doc: Document, baseUrl: String): List<SubtitleFile> {
        val subs = mutableListOf<SubtitleFile>()
        doc.select("track[kind=subtitles], track[kind=captions]").forEach { track ->
            val src = track.attr("src")
            if (src.isNotBlank()) {
                val lang = track.attr("srclang").ifBlank { "en" }
                subs.add(SubtitleFile(track.attr("label").ifBlank { lang }, resolveUrl(baseUrl, src)))
            }
        }
        return subs
    }
}
