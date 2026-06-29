package com.cloudstreamext.example

import com.cloudstreamext.util.*
import com.cloudstreamext.base.LiveTVProvider
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink

/** FutbolLibre - futbollibre.gg - Live Sports */
class FutbolLibreProvider : LiveTVProvider() {
    override var mainUrl = "https://futbollibre.gg"
    override var name = "FutbolLibre"
    override var lang = "es"
    override var hasMainPage = true
    override var defaultHeaders = mutableMapOf("Referer" to "$mainUrl/")

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        val doc = safeDocument("$mainUrl/es") ?: safeDocument(mainUrl) ?: return null
        val items = doc.select("article, .event-card, .card, div[class*=event], div[class*=item], div[class*=match]").mapNotNull { el ->
            val t = el.selectFirst("h2, h3, .title, .event-title, .match-title")?.text() ?: return@mapNotNull null
            val h = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val img = el.selectFirst("img")?.attr("src") ?: el.selectFirst("img")?.attr("data-src")
            liveSearchResponse(t.trim(), resolveUrl(mainUrl, h), fixUrl(img))
        }
        return HomePageList("FutbolLibre", listOf(HomePageListResponse("Fútbol en Vivo", items)))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = safeDocument("$mainUrl/es/?s=${enc(query)}") ?: throw Exception("Search failed")
        return doc.select("article, .event-card, .card, div[class*=event]").mapNotNull { el ->
            val t = el.selectFirst("h2, h3, .title")?.text() ?: return@mapNotNull null
            val h = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val img = el.selectFirst("img")?.attr("src") ?: el.selectFirst("img")?.attr("data-src")
            liveSearchResponse(t.trim(), resolveUrl(mainUrl, h), fixUrl(img))
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = safeDocument(url) ?: return null
        val title = doc.ogTitle() ?: doc.selectFirst("h1")?.text() ?: return null
        val poster = fixUrl(doc.ogImage() ?: doc.selectFirst("img")?.attr("src"))
        val plot = doc.ogDescription() ?: doc.selectFirst(".description")?.text()
        return liveLoadResponse(title.trim(), url, poster, plot = plot)
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val doc = safeDocument(data) ?: return false
        var found = false
        doc.select("iframe[src]").forEach { i ->
            val src = i.attr("src"); if (src.isNotBlank()) { callback(ExtractorLink(name, name, resolveUrl(data, src), data, "Live", headers = mapOf("Referer" to data))); found = true }
        }
        Regex("\"(https?://[^\"]+\\.m3u8[^\"]*)\"").findAll(doc.html()).forEach { m ->
            callback(ExtractorLink(name, name, m.groupValues[1], data, "Live", isM3u8 = true, headers = mapOf("Referer" to data))); found = true
        }
        return found
    }

    private fun resolveUrl(b: String, r: String) = if (r.startsWith("http")) r else if (r.startsWith("//")) "https:$r" else "${b.trimEnd('/')}/${r.trimStart('/')}"
    private fun fixUrl(u: String?) = u?.trim()?.let { when { it.startsWith("http") -> it; it.startsWith("//") -> "https:$it"; else -> "$mainUrl/$it" } }
    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
}
