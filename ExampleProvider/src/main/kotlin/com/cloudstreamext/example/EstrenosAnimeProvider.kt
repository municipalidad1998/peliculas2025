package com.cloudstreamext.example

import com.cloudstreamext.util.*
import com.cloudstreamext.base.AnimeProvider
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink

/** EstrenosAnime - estrenosanime.net */
class EstrenosAnimeProvider : AnimeProvider() {
    override var mainUrl = "https://estrenosanime.net"
    override var name = "EstrenosAnime"
    override var lang = "es"
    override var hasMainPage = true
    override var defaultHeaders = mutableMapOf("Referer" to "$mainUrl/")

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        val doc = safeDocument(mainUrl) ?: return null
        val items = doc.select("article, .anime-card, .card, div[class*=post]").mapNotNull { el ->
            val t = el.selectFirst("h2, h3, .title, a")?.text() ?: return@mapNotNull null
            val h = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val img = el.selectFirst("img")?.attr("src") ?: el.selectFirst("img")?.attr("data-src")
            animeSearchResponse(t.trim(), resolveUrl(mainUrl, h), fixUrl(img))
        }
        return HomePageList("EstrenosAnime", listOf(HomePageListResponse("Anime Sub/Latino", items)))
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = safeDocument("$mainUrl/?s=${enc(query)}") ?: throw Exception("Search failed")
        return doc.select("article, .anime-card, .card").mapNotNull { el ->
            val t = el.selectFirst("h2, h3, .title")?.text() ?: return@mapNotNull null
            val h = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val img = el.selectFirst("img")?.attr("src") ?: el.selectFirst("img")?.attr("data-src")
            animeSearchResponse(t.trim(), resolveUrl(mainUrl, h), fixUrl(img))
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = safeDocument(url) ?: return null
        val title = doc.ogTitle() ?: doc.selectFirst("h1")?.text() ?: return null
        val poster = fixUrl(doc.ogImage() ?: doc.selectFirst("img")?.attr("src"))
        val plot = doc.ogDescription() ?: doc.selectFirst(".description, .sinopsis")?.text()
        val genres = doc.select(".genre a, .generos a").map { it.text().trim() }
        val eps = doc.select("ul.episodes li a, .episode-list a, .cap-list a").mapNotNull { a ->
            val href = a.attr("href"); if (href.isBlank()) return@mapNotNull null
            val num = Regex("Ep(?:isode)?\\s*(\\d+)").find(a.text())?.groupValues?.get(1)?.toIntOrNull()
            Episode(a.text().trim(), resolveUrl(url, href), 1, num)
        }.sortedBy { it.episode ?: 0 }
        return if (eps.isEmpty()) newMovieLoadResponse(title.trim(), url, TvType.AnimeMovie) { this.posterUrl = poster; this.plot = plot; this.tags = genres }
        else newTvSeriesLoadResponse(title.trim(), url, TvType.Anime, eps) { this.posterUrl = poster; this.plot = plot; this.tags = genres }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val doc = safeDocument(data) ?: return false
        var found = false
        doc.select("iframe[src]").forEach { i ->
            val src = i.attr("src"); if (src.isNotBlank()) { callback(ExtractorLink(name, name, resolveUrl(data, src), data, "Unknown", headers = mapOf("Referer" to data))); found = true }
        }
        Regex("\"(https?://[^\"]+\\.m3u8[^\"]*)\"").findAll(doc.html()).forEach { m ->
            callback(ExtractorLink(name, name, m.groupValues[1], data, "Unknown", isM3u8 = true, headers = mapOf("Referer" to data))); found = true
        }
        return found
    }

    private fun resolveUrl(b: String, r: String) = if (r.startsWith("http")) r else if (r.startsWith("//")) "https:$r" else "${b.trimEnd('/')}/${r.trimStart('/')}"
    private fun fixUrl(u: String?) = u?.trim()?.let { when { it.startsWith("http") -> it; it.startsWith("//") -> "https:$it"; else -> "$mainUrl/$it" } }
    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
}
