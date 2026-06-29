package com.cloudstreamext.example

import com.cloudstreamext.base.SeriesProvider
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Example TV Series Provider for CloudStream 3 Extensions.
 *
 * This provider demonstrates series-specific features:
 * - Season/episode organization
 * - Episode numbering across seasons
 * - Series metadata (status, network, episode count)
 * - Episode thumbnails and descriptions
 *
 * To use this as a template:
 * 1. Copy this file and rename the class
 * 2. Update mainUrl and name
 * 3. Implement the actual scraping logic for your target website
 */
class ExampleSeriesProvider : SeriesProvider() {

    override var mainUrl = "https://example-series.com"
    override var name = "Example Series"
    override val lang = "en"
    override val hasMainPage = true
    override val usesHttps = true

    // --- Main Page ---

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        return when (page) {
            1 -> newHomePageList("Popular Series", fetchPopularSeries())
            2 -> newHomePageList("Latest Episodes", fetchLatestEpisodes())
            3 -> newHomePageList("New Series", fetchNewSeries())
            4 -> newHomePageList("Networks", fetchNetworkSeries())
            else -> null
        }
    }

    private suspend fun fetchPopularSeries(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/popular?page=$page")
                ?: throw Exception("Failed to load popular series")

            doc.select(".series-card").mapNotNull { element ->
                val title = element.selectFirst("h3.title")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = element.selectFirst("img")?.attr("src")
                val year = element.selectFirst(".year")?.text()?.extractYear()

                tvSearchResponse(
                    title = cleanTitle(title),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(poster),
                    year = year
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
                val year = element.selectFirst(".year")?.text()?.extractYear()

                tvSearchResponse(
                    title = cleanTitle(title),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(poster),
                    year = year
                )
            }
        }
    }

    private suspend fun fetchNewSeries(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/new?page=$page")
                ?: throw Exception("Failed to load new series")

            doc.select(".series-card").mapNotNull { element ->
                val title = element.selectFirst("h3.title")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = element.selectFirst("img")?.attr("src")
                val year = element.selectFirst(".year")?.text()?.extractYear()

                tvSearchResponse(
                    title = cleanTitle(title),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(poster),
                    year = year
                )
            }
        }
    }

    private suspend fun fetchNetworkSeries(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/networks?page=$page")
                ?: throw Exception("Failed to load network series")

            doc.select(".series-card").mapNotNull { element ->
                val title = element.selectFirst("h3.title")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = element.selectFirst("img")?.attr("src")
                val year = element.selectFirst(".year")?.text()?.extractYear()

                tvSearchResponse(
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

            doc.select(".series-card, .search-result").mapNotNull { element ->
                val title = element.selectFirst("h3, h2, .title")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = element.selectFirst("img")?.attr("src")
                val year = element.selectFirst(".year")?.text()?.extractYear()

                tvSearchResponse(
                    title = cleanTitle(title),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(poster),
                    year = year
                )
            }
        }
    }

    // --- Load Series Details ---

    override suspend fun load(url: String): LoadResponse? {
        return withRetry {
            val doc = safeDocument(url)
                ?: throw Exception("Failed to load series details")

            val title = doc.metaContent("og:title")
                ?: doc.selectFirst("h1")?.text()
                ?: throw Exception("Could not extract title")

            val poster = extractPoster(doc)
            val banner = extractBanner(doc)
            val plot = doc.metaContent("og:description")
                ?: doc.selectFirst(".description, .synopsis, .storyline")?.text()
            val year = extractYear(doc.selectFirst(".year, .premiered")?.text() ?: "")
            val ratingText = doc.selectFirst(".rating, .score")?.text()
            val rating = ratingText?.let { extractRating(it) }?.times(10)?.toInt()
            val genres = extractGenres(doc)
            val actors = extractActors(doc)
            val status = doc.selectFirst(".status")?.text()?.lowercase()

            // Extract episodes with season/episode organization
            val episodes = extractEpisodes(doc, url)

            val comingSoon = status == "returning series" || status == "in production"

            tvSeriesLoadResponse(
                title = cleanTitle(title),
                url = url,
                episodes = episodes,
                poster = poster,
                background = banner,
                plot = plot,
                year = year,
                rating = rating,
                genres = genres,
                actors = actors,
                comingSoon = comingSoon
            )
        }
    }

    // --- Extract Episodes ---

    private fun extractEpisodes(doc: Document, baseUrl: String): List<Episode> {
        val episodes = mutableListOf<Episode>()

        // Try to find season/episode structure
        doc.select(".season, .season-list").forEach { seasonEl ->
            val seasonNumber = seasonEl.selectFirst(".season-number, .season-title")?.text()
                    ?.let { Regex("(\\d+)").find(it)?.value?.toIntOrNull() }

            seasonEl.select(".episode-item, .ep-item").forEach { epEl ->
                val epTitle = epEl.selectFirst(".ep-title, .title")?.text()
                val epUrl = epEl.selectFirst("a")?.attr("href")
                val epNumber = epEl.selectFirst(".ep-number, .episode-number")?.text()
                        ?.let { Regex("(\\d+)").find(it)?.value?.toIntOrNull() }
                val epPoster = epEl.selectFirst("img")?.attr("src")
                val epDuration = epEl.selectFirst(".duration, .length")?.text()?.let {
                    extractDuration(it)
                }
                val epDescription = epEl.selectFirst(".description, .overview")?.text()

                if (epUrl != null) {
                    episodes.add(
                        Episode(
                            name = epTitle,
                            url = resolveUrl(baseUrl, epUrl),
                            season = seasonNumber ?: 1,
                            episode = epNumber,
                            poster = normalizeImage(epPoster),
                            duration = epDuration,
                            description = epDescription
                        )
                    )
                }
            }
        }

        // If no season structure found, try flat episode list
        if (episodes.isEmpty()) {
            doc.select(".episode-item, .ep-item, .episode").forEach { epEl ->
                val epTitle = epEl.selectFirst(".ep-title, .title, .name")?.text()
                val epUrl = epEl.selectFirst("a")?.attr("href")
                val epNumber = epEl.selectFirst(".ep-number, .episode-number, .number")?.text()
                        ?.let { Regex("(\\d+)").find(it)?.value?.toIntOrNull() }
                val season = epEl.selectFirst(".season")?.text()
                        ?.let { Regex("(\\d+)").find(it)?.value?.toIntOrNull() }
                val epPoster = epEl.selectFirst("img")?.attr("src")
                val epDuration = epEl.selectFirst(".duration, .length")?.text()?.let {
                    extractDuration(it)
                }
                val epDescription = epEl.selectFirst(".description, .overview")?.text()

                if (epUrl != null) {
                    episodes.add(
                        Episode(
                            name = epTitle,
                            url = resolveUrl(baseUrl, epUrl),
                            season = season ?: 1,
                            episode = epNumber,
                            poster = normalizeImage(epPoster),
                            duration = epDuration,
                            description = epDescription
                        )
                    )
                }
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

    private fun extractRating(text: String): Double? {
        val match = Regex("([\\d.]+)\\s*(?:/\\s*(?:10|100)|%)").find(text)
            ?: Regex("([\\d.]+)").find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractGenres(doc: Document): List<String> {
        return doc.select(".genre a, .genres a, .genre-tag").map { it.text().trim() }
    }

    private fun extractActors(doc: Document): List<String> {
        return doc.select(".actor a, .cast a, .person").map {
            it.attr("title").ifBlank { it.text() }.trim()
        }.filter { it.isNotBlank() }
    }

    private fun extractDuration(text: String): Int? {
        val match = Regex("(\\d+)\\s*(?:min|m(?:in)?)").find(text.lowercase())
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractYear(text: String): Int? {
        return Regex("(19|20)\\d{2}").find(text)?.value?.toIntOrNull()
    }
}
