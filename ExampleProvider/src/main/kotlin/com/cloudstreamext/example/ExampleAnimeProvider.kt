package com.cloudstreamext.example

import com.cloudstreamext.base.AnimeProvider
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Example Anime Provider for CloudStream 3 Extensions.
 *
 * This provider demonstrates anime-specific features:
 * - Dub/sub detection
 * - Episode numbering (season 1 for dub, season 2 for sub)
 * - Anime metadata (type, status, studios)
 * - MAL-style episode organization
 *
 * To use this as a template:
 * 1. Copy this file and rename the class
 * 2. Update mainUrl and name
 * 3. Implement the actual scraping logic for your target anime site
 */
class ExampleAnimeProvider : AnimeProvider() {

    override var mainUrl = "https://example-anime.com"
    override var name = "Example Anime"
    override val lang = "en"
    override val hasMainPage = true
    override val usesHttps = true
    override val supportsDub = true

    // --- Main Page ---

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        return when (page) {
            1 -> newHomePageList("Popular Anime", fetchPopularAnime())
            2 -> newHomePageList("Latest Episodes", fetchLatestEpisodes())
            3 -> newHomePageList("New Anime", fetchNewAnime())
            4 -> newHomePageList("Movies & OVAs", fetchMoviesOvas())
            else -> null
        }
    }

    private suspend fun fetchPopularAnime(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/popular?page=$page")
                ?: throw Exception("Failed to load popular anime")

            doc.select(".anime-card").mapNotNull { element ->
                val title = element.selectFirst("h3.title")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = element.selectFirst("img")?.attr("src")
                val isDub = element.selectFirst(".badge-dub") != null

                animeSearchResponse(
                    title = cleanTitle(title),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(poster),
                    isDub = isDub
                )
            }
        }
    }

    private suspend fun fetchLatestEpisodes(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/latest-episodes")
                ?: throw Exception("Failed to load latest episodes")

            doc.select(".episode-card").mapNotNull { element ->
                val title = element.selectFirst("h3.title")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = element.selectFirst("img")?.attr("src")
                val isDub = element.selectFirst(".badge-dub") != null

                animeSearchResponse(
                    title = cleanTitle(title),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(poster),
                    isDub = isDub
                )
            }
        }
    }

    private suspend fun fetchNewAnime(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/new?page=$page")
                ?: throw Exception("Failed to load new anime")

            doc.select(".anime-card").mapNotNull { element ->
                val title = element.selectFirst("h3.title")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = element.selectFirst("img")?.attr("src")
                val isDub = element.selectFirst(".badge-dub") != null

                animeSearchResponse(
                    title = cleanTitle(title),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(poster),
                    isDub = isDub
                )
            }
        }
    }

    private suspend fun fetchMoviesOvas(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/movies-ovas")
                ?: throw Exception("Failed to load movies/OVAs")

            doc.select(".anime-card").mapNotNull { element ->
                val title = element.selectFirst("h3.title")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = element.selectFirst("img")?.attr("src")
                val isDub = element.selectFirst(".badge-dub") != null

                animeSearchResponse(
                    title = cleanTitle(title),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(poster),
                    isDub = isDub
                )
            }
        }
    }

    // --- Search ---

    override suspend fun search(query: String): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/search?q=${NetworkUtils.encodeUrl(query)}")
                ?: throw Exception("Search failed for query: $query")

            doc.select(".anime-card, .search-result").mapNotNull { element ->
                val title = element.selectFirst("h3, h2, .title")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = element.selectFirst("img")?.attr("src")
                val isDub = element.selectFirst(".badge-dub, .dub") != null

                animeSearchResponse(
                    title = cleanTitle(title),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(poster),
                    isDub = isDub
                )
            }
        }
    }

    // --- Load Anime Details ---

    override suspend fun load(url: String): LoadResponse? {
        return withRetry {
            val doc = safeDocument(url)
                ?: throw Exception("Failed to load anime details")

            val title = doc.metaContent("og:title")
                ?: doc.selectFirst("h1")?.text()
                ?: throw Exception("Could not extract title")

            val poster = extractPoster(doc)
            val banner = extractBanner(doc)
            val plot = doc.metaContent("og:description")
                ?: doc.selectFirst(".description, .synopsis, .storyline")?.text()
            val year = extractYear(doc.selectFirst(".aired, .year, .premiered")?.text() ?: "")
            val genres = extractGenres(doc)
            val actors = doc.selectFirst(".studio, .studios")?.text()?.let { listOf(it) } ?: emptyList()

            // Extract episodes with dub/sub detection
            val episodes = extractEpisodes(doc, url)

            // Determine if this is a movie or series
            val isMovie = episodes.isEmpty() ||
                    doc.selectFirst(".movie-badge, .type-movie") != null

            if (isMovie) {
                newMovieLoadResponse(
                    title = cleanTitle(title),
                    url = url,
                    type = TvType.AnimeMovie
                ) {
                    this.posterUrl = poster
                    this.backgroundPosterUrl = banner
                    this.plot = plot
                    this.year = year
                    this.tags = genres
                    this.actors = actors
                }
            } else {
                newTvSeriesLoadResponse(
                    title = cleanTitle(title),
                    url = url,
                    type = TvType.Anime,
                    episodes = episodes
                ) {
                    this.posterUrl = poster
                    this.backgroundPosterUrl = banner
                    this.plot = plot
                    this.year = year
                    this.tags = genres
                    this.actors = actors
                }
            }
        }
    }

    // --- Extract Episodes ---

    private fun extractEpisodes(doc: Document, baseUrl: String): List<Episode> {
        val episodes = mutableListOf<Episode>()

        // Try to find episode list
        val episodeElements = doc.select(".episode-item, .ep-item, .episode")

        if (episodeElements.isNotEmpty()) {
            episodeElements.forEach { element ->
                val epTitle = element.selectFirst(".ep-title, .title")?.text()
                val epUrl = element.selectFirst("a")?.attr("href")
                val epNumber = element.selectFirst(".ep-number, .episode-number")?.text()
                        ?.let { Regex("(\\d+)").find(it)?.value?.toIntOrNull() }
                val epPoster = element.selectFirst("img")?.attr("src")
                val epDuration = element.selectFirst(".duration, .length")?.text()?.let {
                    extractDuration(it)
                }

                if (epUrl != null) {
                    episodes.add(
                        Episode(
                            name = epTitle,
                            url = resolveUrl(baseUrl, epUrl),
                            season = 1, // Sub
                            episode = epNumber,
                            poster = normalizeImage(epPoster),
                            duration = epDuration
                        )
                    )
                }
            }
        } else {
            // Try to find video sources directly (single episode/movie)
            val videoSection = doc.selectFirst(".video-section, .player-container, #player")
            if (videoSection != null) {
                episodes.add(
                    Episode(
                        name = "Full",
                        url = baseUrl,
                        season = 1,
                        episode = 1
                    )
                )
            }
        }

        return episodes
    }

    // --- Load Links ---

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return try {
            val doc = safeDocument(data)
                ?: throw Exception("Failed to load video page")

            // Extract video sources
            val videoUrls = extractVideoSources(doc, data)

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

            // Extract subtitles
            val subtitles = extractSubtitles(doc, data)
            subtitles.forEach { subtitleCallback.invoke(it) }

            true
        } catch (e: Exception) {
            false
        }
    }

    // --- Helper Methods ---

    private fun resolveUrl(base: String, relative: String): String {
        if (relative.startsWith("http")) return relative
        if (relative.startsWith("//")) return "https:$relative"
        return "${base.trimEnd('/')}/${relative.trimStart('/')}"
    }

    private fun normalizeImage(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            else -> "${mainUrl.trimEnd('/')}/${trimmed.trimStart('/')}"
        }
    }

    private fun extractVideoSources(doc: Document, baseUrl: String): List<Triple<String, String, Boolean>> {
        val sources = mutableListOf<Triple<String, String, Boolean>>()
        val html = doc.html()

        // Pattern 1: Direct video URLs
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
                sources.add(Triple(resolveUrl(baseUrl, src), quality, isHls))
            }
        }

        // Pattern 2: JavaScript video data
        val jsDataPatterns = listOf(
            Regex("\"(https?://[^\"*\\.m3u8[^\"]*)\""),
            Regex("'(https?://[^']*\\.m3u8[^']*)'"),
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

        return sources
    }

    private fun extractSubtitles(doc: Document, baseUrl: String): List<SubtitleFile> {
        val subtitles = mutableListOf<SubtitleFile>()

        doc.select("track[kind=subtitles], track[kind=captions]").forEach { track ->
            val src = track.attr("src")
            if (src.isNotBlank()) {
                val lang = track.attr("srclang").ifBlank { "en" }
                val label = track.attr("label").ifBlank { lang }
                val fullUrl = resolveUrl(baseUrl, src)
                subtitles.add(SubtitleFile(label, fullUrl))
            }
        }

        return subtitles
    }

    /**
     * Extracts year from text.
     */
    private fun extractYear(text: String): Int? {
        return Regex("(19|20)\\d{2}").find(text)?.value?.toIntOrNull()
    }

    /**
     * Extracts duration in minutes from text.
     */
    private fun extractDuration(text: String): Int? {
        val match = Regex("(\\d+)\\s*(?:min|m(?:in)?)").find(text.lowercase())
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    /**
     * Extracts genres from the page.
     */
    private fun extractGenres(doc: Document): List<String> {
        return doc.select(".genre a, .genres a, .genre-tag").map { it.text().trim() }
    }
}
