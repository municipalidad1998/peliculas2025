package recloudstream.live

import com.lagradost.cloudstream3.*
import recloudstream.BaseSiteProvider
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.StringUtils.encodeUri

class StreamXHD : BaseSiteProvider() {
    override var mainUrl = "https://stream-xhd.com"
    override var name = "StreamXHD"
    override val supportedTypes = setOf(TvType.Others)
    override var lang = "es"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get(mainUrl).document
        val results = doc.select("article, div[class*=stream], .poster, div[class*=item], a[href*=live]").mapNotNull { el ->
            val title = el.selectFirst("h2, h3, .title, img")?.let { it.attr("alt").ifBlank { it.text() } } ?: return@mapNotNull null
            val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            this.newMovieSearchResponse(title.trim(), resolveUrl(href), TvType.Others) {}
        }
        return newHomePageResponse(listOf(HomePageList("Deportes en Vivo", results, true)))
    }

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        val doc = app.get("$mainUrl/?s=${query.encodeUri()}").document
        return doc.select("article, div[class*=stream], .poster").mapNotNull { el ->
            val title = el.selectFirst("h2, h3, .title")?.text() ?: return@mapNotNull null
            val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            this.newMovieSearchResponse(title.trim(), resolveUrl(href), TvType.Others) {}
        }.toNewSearchResponseList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1, h2.title")?.text()
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content") ?: return null
        return newMovieLoadResponse(title.trim(), url, TvType.Others, url) {}
    }

    override suspend fun loadLinks(
        data: String, isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        val html = doc.html()
        var found = false

        doc.select("iframe[src]").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.isNotBlank()) {
                loadExtractor(resolveUrl(src), data, subtitleCallback, callback)
                found = true
            }
        }

        Regex("\"(https?://[^\"]+\\.m3u8[^\"]*)\"").findAll(html).forEach { match ->
            loadExtractor(match.groupValues[1], data, subtitleCallback, callback)
            found = true
        }

        return found
    }
}
