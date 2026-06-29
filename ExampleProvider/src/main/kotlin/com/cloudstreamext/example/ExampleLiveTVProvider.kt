package com.cloudstreamext.example

import com.cloudstreamext.base.LiveTVProvider
import com.cloudstreamext.util.*
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Document

/**
 * Example Live TV Provider for CloudStream 3 Extensions.
 */
class ExampleLiveTVProvider : LiveTVProvider() {

    override var mainUrl = "https://example-live.com"
    override var name = "Example Live TV"
    override var lang = "en"
    override var hasMainPage = true
    override var usesHttps = true

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        return when (page) {
            1 -> newHomePageList("Sports Channels", fetchChannels("sports"))
            2 -> newHomePageList("Entertainment", fetchChannels("entertainment"))
            3 -> newHomePageList("News", fetchChannels("news"))
            else -> null
        }
    }

    private suspend fun fetchChannels(category: String): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/$category")
                ?: throw Exception("Failed to load $category channels")
            doc.select(".channel-card").mapNotNull { el ->
                val chName = el.selectFirst("h3.name, .channel-name")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val logo = el.selectFirst("img")?.attr("src")
                liveSearchResponse(cleanTitle(chName), resolveUrl(mainUrl, href), normalizeImage(logo))
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/search?q=${query.urlEncode()}")
                ?: throw Exception("Search failed")
            doc.select(".channel-card, .search-result").mapNotNull { el ->
                val chName = el.selectFirst("h3, .name")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val logo = el.selectFirst("img")?.attr("src")
                liveSearchResponse(cleanTitle(chName), resolveUrl(mainUrl, href), normalizeImage(logo))
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        return withRetry {
            val doc = safeDocument(url) ?: throw Exception("Failed to load channel")
            val title = doc.ogTitle() ?: doc.selectFirst("h1")?.text() ?: throw Exception("No title")
            val poster = extractPoster(doc)
            val banner = extractBanner(doc)
            val plot = doc.ogDescription() ?: doc.selectFirst(".description")?.text()
            liveLoadResponse(cleanTitle(title), url, poster, banner, plot)
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
                    callback(ExtractorLink(name, name, m.groupValues[1], data, "Live", true, mapOf("Referer" to data)))
                }
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
