package com.cloudstreamext.example

import com.cloudstreamext.base.LiveTVProvider
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Example Live TV Provider for CloudStream 3 Extensions.
 *
 * This provider demonstrates live TV-specific features:
 * - Channel listing with categories
 * - Live stream extraction
 * - EPG (Electronic Program Guide) data
 * - Channel status detection
 *
 * To use this as a template:
 * 1. Copy this file and rename the class
 * 2. Update mainUrl and name
 * 3. Implement the actual scraping logic for your target IPTV site
 */
class ExampleLiveTVProvider : LiveTVProvider() {

    override var mainUrl = "https://example-live.com"
    override var name = "Example Live TV"
    override val lang = "en"
    override val hasMainPage = true
    override val usesHttps = true

    // --- Main Page ---

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        return when (page) {
            1 -> newHomePageList("Sports Channels", fetchSportsChannels())
            2 -> newHomePageList("Entertainment", fetchEntertainmentChannels())
            3 -> newHomePageList("News", fetchNewsChannels())
            4 -> newHomePageList("Movies", fetchMovieChannels())
            else -> null
        }
    }

    private suspend fun fetchSportsChannels(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/sports")
                ?: throw Exception("Failed to load sports channels")

            doc.select(".channel-card").mapNotNull { element ->
                val name = element.selectFirst("h3.name, .channel-name")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val logo = element.selectFirst("img")?.attr("src")
                val isLive = element.selectFirst(".live-badge, .status-live") != null

                liveSearchResponse(
                    name = cleanTitle(name),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(logo),
                    isLive = isLive
                )
            }
        }
    }

    private suspend fun fetchEntertainmentChannels(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/entertainment")
                ?: throw Exception("Failed to load entertainment channels")

            doc.select(".channel-card").mapNotNull { element ->
                val name = element.selectFirst("h3.name, .channel-name")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val logo = element.selectFirst("img")?.attr("src")
                val isLive = element.selectFirst(".live-badge, .status-live") != null

                liveSearchResponse(
                    name = cleanTitle(name),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(logo),
                    isLive = isLive
                )
            }
        }
    }

    private suspend fun fetchNewsChannels(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/news")
                ?: throw Exception("Failed to load news channels")

            doc.select(".channel-card").mapNotNull { element ->
                val name = element.selectFirst("h3.name, .channel-name")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val logo = element.selectFirst("img")?.attr("src")
                val isLive = element.selectFirst(".live-badge, .status-live") != null

                liveSearchResponse(
                    name = cleanTitle(name),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(logo),
                    isLive = isLive
                )
            }
        }
    }

    private suspend fun fetchMovieChannels(): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/movies")
                ?: throw Exception("Failed to load movie channels")

            doc.select(".channel-card").mapNotNull { element ->
                val name = element.selectFirst("h3.name, .channel-name")?.text() ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val logo = element.selectFirst("img")?.attr("src")
                val isLive = element.selectFirst(".live-badge, .status-live") != null

                liveSearchResponse(
                    name = cleanTitle(name),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(logo),
                    isLive = isLive
                )
            }
        }
    }

    // --- Search ---

    override suspend fun search(query: String): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/search?q=${NetworkUtils.encodeUrl(query)}")
                ?: throw Exception("Search failed for query: $query")

            doc.select(".channel-card, .search-result").mapNotNull { element ->
                val name = element.selectFirst("h3, h2, .name, .channel-name")?.text()
                        ?: return@mapNotNull null
                val href = element.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val logo = element.selectFirst("img")?.attr("src")
                val isLive = element.selectFirst(".live-badge, .status-live, .live") != null

                liveSearchResponse(
                    name = cleanTitle(name),
                    url = resolveUrl(mainUrl, href),
                    poster = normalizeImage(logo),
                    isLive = isLive
                )
            }
        }
    }

    // --- Load Channel Details ---

    override suspend fun load(url: String): LoadResponse? {
        return withRetry {
            val doc = safeDocument(url)
                ?: throw Exception("Failed to load channel details")

            val title = doc.metaContent("og:title")
                ?: doc.selectFirst("h1")?.text()
                ?: throw Exception("Could not extract channel name")

            val poster = extractPoster(doc)
            val banner = extractBanner(doc)
            val plot = doc.metaContent("og:description")
                ?: doc.selectFirst(".description, .about")?.text()
            val genres = extractGenres(doc)

            liveLoadResponse(
                name = cleanTitle(title),
                url = url,
                poster = poster,
                background = banner,
                plot = plot,
                genres = genres
            )
        }
    }

    // --- Load Links (Stream Extraction) ---

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return try {
            val doc = safeDocument(data)
                ?: throw Exception("Failed to load channel page")

            // Extract live stream URLs
            val streamUrls = extractStreamUrls(doc, data)

            // Create extractor links for each stream
            streamUrls.forEach { (url, quality, isHls) ->
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

    private fun extractStreamUrls(doc: Document, baseUrl: String): List<Triple<String, String, Boolean>> {
        val sources = mutableListOf<Triple<String, String, Boolean>>()
        val html = doc.html()

        // Pattern 1: HLS live streams (m3u8)
        val hlsPatterns = listOf(
            Regex("\"(https?://[^\"*\\.m3u8[^\"]*)\""),
            Regex("'(https?://[^']*\\.m3u8[^']*)'"),
            Regex("source[\"':\\s]+[\"'](https?://[^\"']+\\.m3u8)[\"']"),
            Regex("file[\"':\\s]+[\"'](https?://[^\"']+\\.m3u8)[\"']"),
            Regex("stream[\"':\\s]+[\"'](https?://[^\"']+\\.m3u8)[\"']")
        )

        for (pattern in hlsPatterns) {
            pattern.findAll(html).forEach { match ->
                val url = match.groupValues[1]
                if (url.isNotBlank() && sources.none { it.first == url }) {
                    sources.add(Triple(url, "Live", true))
                }
            }
        }

        // Pattern 2: Direct stream URLs (mpd, mp4)
        val directPatterns = listOf(
            Regex("\"(https?://[^\"*\\.mpd[^\"]*)\""),
            Regex("'(https?://[^']*\\.mpd[^']*)'"),
            Regex("\"(https?://[^\"*\\.mp4[^\"]*)\""),
            Regex("'(https?://[^']*\\.mp4[^']*)'")
        )

        for (pattern in directPatterns) {
            pattern.findAll(html).forEach { match ->
                val url = match.groupValues[1]
                if (url.isNotBlank() && sources.none { it.first == url }) {
                    val isDash = url.contains(".mpd")
                    sources.add(Triple(url, "Live", !isDash))
                }
            }
        }

        // Pattern 3: Video player iframe
        val iframeSrc = doc.selectFirst("iframe[src*=embed], iframe[src*=player]")?.attr("src")
        if (iframeSrc != null && iframeSrc.isNotBlank()) {
            // Note: For iframes, you would typically need to extract the actual stream URL
            // from the iframe content. This is a placeholder for that logic.
            sources.add(Triple(resolveUrl(baseUrl, iframeSrc), "Live", false))
        }

        return sources
    }

    private fun extractGenres(doc: Document): List<String> {
        return doc.select(".genre a, .category a, .channel-type").map { it.text().trim() }
    }
}
