package com.cloudstreamext.example

import com.cloudstreamext.base.AnimeProvider
import com.cloudstreamext.util.*
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Document

/**
 * Example Anime Provider for CloudStream 3 Extensions.
 */
class ExampleAnimeProvider : AnimeProvider() {

    override var mainUrl = "https://example-anime.com"
    override var name = "Example Anime"
    override var lang = "en"
    override var hasMainPage = true
    override var usesHttps = true

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        return when (page) {
            1 -> newHomePageList("Popular Anime", fetchAnime("popular"))
            2 -> newHomePageList("Latest Episodes", fetchAnime("latest"))
            3 -> newHomePageList("New Anime", fetchAnime("new"))
            else -> null
        }
    }

    private suspend fun fetchAnime(section: String): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/$section?page=1")
                ?: throw Exception("Failed to load $section anime")
            doc.select(".anime-card, .search-result").mapNotNull { el ->
                val title = el.selectFirst("h3, h2, .title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src")
                val isDub = el.selectFirst(".badge-dub, .dub") != null
                animeSearchResponse(cleanTitle(title), resolveUrl(mainUrl, href), normalizeImage(poster), isDub = isDub)
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/search?q=${query.urlEncode()}")
                ?: throw Exception("Search failed")
            doc.select(".anime-card, .search-result").mapNotNull { el ->
                val title = el.selectFirst("h3, .title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src")
                animeSearchResponse(cleanTitle(title), resolveUrl(mainUrl, href), normalizeImage(poster))
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        return withRetry {
            val doc = safeDocument(url) ?: throw Exception("Failed to load anime")
            val title = doc.ogTitle() ?: doc.selectFirst("h1")?.text() ?: throw Exception("No title")
            val poster = extractPoster(doc)
            val banner = extractBanner(doc)
            val plot = doc.ogDescription() ?: doc.selectFirst(".description, .synopsis")?.text()
            val year = extractYear(doc.selectFirst(".year, .aired")?.text() ?: "")
            val genres = extractGenres(doc)
            val episodes = doc.select(".episode-item, .ep-item").mapNotNull { el ->
                val epUrl = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val epNum = Regex("(\\d+)").find(el.selectFirst(".ep-number, .episode-number")?.text() ?: "")?.value?.toIntOrNull()
                Episode(el.selectFirst(".ep-title, .title")?.text(), resolveUrl(url, epUrl), season = 1, episode = epNum)
            }
            if (episodes.isEmpty()) {
                newMovieLoadResponse(cleanTitle(title), url, TvType.AnimeMovie) {
                    this.posterUrl = poster; this.backgroundPosterUrl = banner; this.plot = plot; this.year = year; this.tags = genres
                }
            } else {
                newTvSeriesLoadResponse(cleanTitle(title), url, TvType.Anime, episodes) {
                    this.posterUrl = poster; this.backgroundPosterUrl = banner; this.plot = plot; this.year = year; this.tags = genres
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
            val patterns = listOf(Regex("\"(https?://[^\"]+\\.m3u8[^\"]*)\""), Regex("'(https?://[^']+\\.m3u8[^']*)'"))
            for (p in patterns) {
                p.findAll(html).forEach { m ->
                    callback(ExtractorLink(name, name, m.groupValues[1], data, "Unknown", true, mapOf("Referer" to data)))
                }
            }
            doc.select("track[kind=subtitles]").forEach { track ->
                val src = track.attr("src")
                if (src.isNotBlank()) subtitleCallback(SubtitleFile(track.attr("label").ifBlank { "Sub" }, resolveUrl(data, src)))
            }
            true
        } catch (_: Exception) { false }
    }

    private fun resolveUrl(base: String, relative: String): String {
        if (relative.startsWith("http")) return relative
        if (relative.startsWith("//")) return "https:$relative"
        return "${base.trimEnd('/')}/${relative.trimStart('/')}"
    }

    private fun String.urlEncode(): String = java.net.URLEncoder.encode(this, "UTF-8")
}
