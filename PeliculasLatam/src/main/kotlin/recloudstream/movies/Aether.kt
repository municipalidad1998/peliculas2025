package recloudstream.movies

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.StringUtils.encodeUri

class Aether : MainAPI() {
    override var mainUrl = "https://aether.bar"
    override var name = "Aether"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "es"
    override val hasMainPage = true

    private fun resolveUrl(href: String): String {
        if (href.startsWith("http")) return href
        if (href.startsWith("//")) return "https:$href"
        return "${mainUrl.trimEnd('/')}/${href.trimStart('/')}"
    }

    private fun img(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val t = url.trim()
        return when {
            t.startsWith("http") -> t
            t.startsWith("//") -> "https:$t"
            else -> "$mainUrl/$t"
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get(mainUrl).document
        val results = doc.select("article, div[class*=movie], .poster, div[class*=item]").mapNotNull { el ->
            val title = el.selectFirst("h2, h3, .title, img")?.let { it.attr("alt").ifBlank { it.text() } } ?: return@mapNotNull null
            val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = el.selectFirst("img")?.let { it.attr("src").ifBlank { it.attr("data-src") } }
            this.newMovieSearchResponse(title.trim(), resolveUrl(href), TvType.Movie) {
                this.posterUrl = img(poster)
            }
        }
        return newHomePageResponse(listOf(HomePageList("Peliculas", results, true)))
    }

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        val doc = app.get("$mainUrl/?s=${query.encodeUri()}").document
        return doc.select("article, div[class*=movie], .poster").mapNotNull { el ->
            val title = el.selectFirst("h2, h3, .title")?.text() ?: return@mapNotNull null
            val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = el.selectFirst("img")?.let { it.attr("src").ifBlank { it.attr("data-src") } }
            this.newMovieSearchResponse(title.trim(), resolveUrl(href), TvType.Movie) {
                this.posterUrl = img(poster)
            }
        }.toNewSearchResponseList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1, h2.title")?.text()
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content") ?: return null
        val poster = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: doc.selectFirst(".poster img")?.attr("src")
        val plot = doc.selectFirst("meta[property=og:description]")?.attr("content")
            ?: doc.selectFirst(".description, .sinopsis")?.text()
        val genres = doc.select(".genre a, .genres a").map { it.text().trim() }
        return newMovieLoadResponse(title.trim(), url, TvType.Movie, url) {
            this.posterUrl = img(poster)
            this.plot = plot
            this.tags = genres
        }
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
                loadExtractor(resolveUrl(src), null, subtitleCallback, callback)
                found = true
            }
        }

        Regex("\"(https?://[^\"]+\\.(m3u8|mp4)[^\"]*)\"").findAll(html).forEach { m ->
            loadExtractor(m.groupValues[1], null, subtitleCallback, callback)
            found = true
        }

        return found
    }
}
