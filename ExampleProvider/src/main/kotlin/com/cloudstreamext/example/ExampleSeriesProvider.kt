package com.cloudstreamext.example

import com.cloudstreamext.base.SeriesProvider
import com.cloudstreamext.util.*
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Document

/**
 * Example TV Series Provider for CloudStream 3 Extensions.
 */
class ExampleSeriesProvider : SeriesProvider() {

    override var mainUrl = "https://example-series.com"
    override var name = "Example Series"
    override var lang = "en"
    override var hasMainPage = true
    override var usesHttps = true

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        return when (page) {
            1 -> newHomePageList("Popular Series", fetchSeries("popular"))
            2 -> newHomePageList("Latest Episodes", fetchSeries("latest"))
            3 -> newHomePageList("New Series", fetchSeries("new"))
            else -> null
        }
    }

    private suspend fun fetchSeries(section: String): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/$section?page=1")
                ?: throw Exception("Failed to load $section series")
            doc.select(".series-card, .search-result").mapNotNull { el ->
                val title = el.selectFirst("h3, .title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src")
                val year = el.selectFirst(".year")?.text()?.extractYear()
                tvSearchResponse(cleanTitle(title), resolveUrl(mainUrl, href), normalizeImage(poster), year)
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/search?q=${query.urlEncode()}")
                ?: throw Exception("Search failed")
            doc.select(".series-card, .search-result").mapNotNull { el ->
                val title = el.selectFirst("h3, .title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src")
                val year = el.selectFirst(".year")?.text()?.extractYear()
                tvSearchResponse(cleanTitle(title), resolveUrl(mainUrl, href), normalizeImage(poster), year)
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        return withRetry {
            val doc = safeDocument(url) ?: throw Exception("Failed to load series")
            val title = doc.ogTitle() ?: doc.selectFirst("h1")?.text() ?: throw Exception("No title")
            val poster = extractPoster(doc)
            val banner = extractBanner(doc)
            val plot = doc.ogDescription() ?: doc.selectFirst(".description, .synopsis")?.text()
            val year = extractYear(doc.selectFirst(".year, .premiered")?.text() ?: "")
            val genres = extractGenres(doc)
            val episodes = doc.select(".episode-item, .ep-item").mapNotNull { el ->
                val epUrl = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val season = Regex("S(\\d+)").find(el.selectFirst(".season")?.text() ?: "")?.value?.toIntOrNull()
                val epNum = Regex("E(\\d+)").find(el.selectFirst(".episode-number")?.text() ?: "")?.value?.toIntOrNull()
                Episode(el.selectFirst(".ep-title, .title")?.text(), resolveUrl(url, epUrl), season ?: 1, epNum)
            }
            tvSeriesLoadResponse(cleanTitle(title), url, episodes, poster, banner, plot, year, genres = genres)
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
