package com.cloudstreamext.example

import com.cloudstreamext.base.AnimeProvider
import com.cloudstreamext.util.*
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink

/**
 * TioAnime Provider - tioanime.com
 * Real anime provider scraping from tioanime.com
 */
class TioAnimeProvider : AnimeProvider() {

    override var mainUrl = "https://tioanime.com"
    override var name = "TioAnime"
    override var lang = "es"
    override var hasMainPage = true
    override var usesHttps = true

    override var defaultHeaders: MutableMap<String, String> = mutableMapOf(
        "Accept-Language" to "es-ES,es;q=0.9",
        "Referer" to "https://tioanime.com/"
    )

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        return try {
            val doc = safeDocument(mainUrl) ?: return null
            val lists = mutableListOf<HomePageListResponse>()

            // Featured anime
            val featured = doc.select("article.anime-card, div.anime-card, .poster").mapNotNull { el ->
                val title = el.selectFirst("h2, h3, .title, .anime-title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src") ?: el.selectFirst("img")?.attr("data-src")
                animeSearchResponse(formatTitle(title), resolveUrl(mainUrl, href), normalizeImageUrl(poster))
            }
            if (featured.isNotEmpty()) lists.add(HomePageListResponse("Destacados", featured))

            // Latest episodes
            val latest = doc.select("div.ep-item, article.ep-card, .episode-card").mapNotNull { el ->
                val title = el.selectFirst("h3, .title, .ep-title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src") ?: el.selectFirst("img")?.attr("data-src")
                val epNum = Regex("Ep(?:isode)?\\s*(\\d+)").find(el.text())?.groupValues?.get(1)?.toIntOrNull()
                Episode(name = title, url = resolveUrl(mainUrl, href), season = 1, episode = epNum, poster = normalizeImageUrl(poster))
            }
            if (latest.isNotEmpty()) lists.add(HomePageListResponse("Últimos Episodios", featured))

            HomePageList("TioAnime", lists)
        } catch (_: Exception) { null }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/directorio?q=${query.urlEncode()}") ?: throw Exception("Search failed")
            doc.select("article.anime-card, div.anime-card, .poster").mapNotNull { el ->
                val title = el.selectFirst("h2, h3, .title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src") ?: el.selectFirst("img")?.attr("data-src")
                animeSearchResponse(formatTitle(title), resolveUrl(mainUrl, href), normalizeImageUrl(poster))
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        return withRetry {
            val doc = safeDocument(url) ?: throw Exception("Failed to load")
            val title = doc.ogTitle() ?: doc.selectFirst("h1, h2.title")?.text() ?: throw Exception("No title")
            val poster = extractPoster(doc) ?: doc.selectFirst(".poster img")?.attr("src")
            val plot = doc.ogDescription() ?: doc.selectFirst(".description, .sinopsis")?.text()
            val year = parseYear(doc.selectFirst(".year, .date")?.text() ?: "")
            val genres = doc.select(".genre a, .genres a").map { it.text().trim() }

            val episodes = doc.select("ul.episodes-list li a, .episode-list a, div.episodes a").mapNotNull { el ->
                val epUrl = el.attr("href")
                if (epUrl.isBlank()) return@mapNotNull null
                val epText = el.text()
                val epNum = Regex("Ep(?:isode)?\\s*(\\d+)").find(epText)?.groupValues?.get(1)?.toIntOrNull()
                Episode(name = epText.trim(), url = resolveUrl(mainUrl, epUrl), season = 1, episode = epNum)
            }.sortedBy { it.episode ?: 0 }

            if (episodes.isEmpty()) {
                newMovieLoadResponse(formatTitle(title), url, TvType.AnimeMovie) {
                    this.posterUrl = poster; this.plot = plot; this.year = year; this.tags = genres
                }
            } else {
                newTvSeriesLoadResponse(formatTitle(title), url, TvType.Anime, episodes) {
                    this.posterUrl = poster; this.plot = plot; this.year = year; this.tags = genres
                }
            }
        }
    }

    override suspend fun loadLinks(
        data: String, isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return try {
            val doc = safeDocument(data) ?: throw Exception("Failed to load")
            val html = doc.html()
            var found = false

            // Extract iframe sources
            doc.select("iframe[src]").forEach { iframe ->
                val src = iframe.attr("src")
                if (src.isNotBlank()) {
                    callback(ExtractorLink(name, name, resolveUrl(data, src), data, "Unknown", headers = mapOf("Referer" to data)))
                    found = true
                }
            }

            // Extract m3u8 sources
            Regex("\"(https?://[^\"]+\\.m3u8[^\"]*)\"").findAll(html).forEach { match ->
                callback(ExtractorLink(name, name, match.groupValues[1], data, "Unknown", isM3u8 = true, headers = mapOf("Referer" to data)))
                found = true
            }

            found
        } catch (_: Exception) { false }
    }

    private fun resolveUrl(base: String, relative: String): String {
        if (relative.startsWith("http")) return relative
        if (relative.startsWith("//")) return "https:$relative"
        return "${base.trimEnd('/')}/${relative.trimStart('/')}"
    }

    private fun normalizeImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            else -> "$mainUrl/$trimmed"
        }
    }

    private fun formatTitle(title: String): String = title.replace(Regex("\\s+"), " ").trim()
    private fun parseYear(text: String): Int? = Regex("(19|20)\\d{2}").find(text)?.value?.toIntOrNull()
    private fun String.urlEncode(): String = java.net.URLEncoder.encode(this, "UTF-8")
}
