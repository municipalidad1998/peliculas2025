package recloudstream.movies

import com.lagradost.cloudstream3.*
import recloudstream.BaseSiteProvider
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.StringUtils.encodeUri

class SoloLatino : BaseSiteProvider() {
    override var mainUrl = "https://sololatino.net"
    override var name = "SoloLatino"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "es"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get(mainUrl).document
        // sololatino.net uses .card with .card__poster and .card__title (verified via browser-use)
        val results = doc.select(".card").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            if (href.isBlank()) return@mapNotNull null
            val title = el.selectFirst(".card__title")?.text()
                ?: el.selectFirst("img")?.attr("alt")
                ?: return@mapNotNull null
            val poster = el.selectFirst(".card__poster img, img")?.let {
                it.attr("src").ifBlank { it.attr("data-src") }
            }
            this.newMovieSearchResponse(title.trim(), resolveUrl(href), TvType.Movie) {
                this.posterUrl = img(poster)
            }
        }
        return newHomePageResponse(listOf(HomePageList("Películas Latino", results, true)))
    }

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        val doc = app.get("$mainUrl/?s=${query.encodeUri()}").document
        return doc.select(".card").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            if (href.isBlank()) return@mapNotNull null
            val title = el.selectFirst(".card__title")?.text()
                ?: el.selectFirst("img")?.attr("alt")
                ?: a.text()
                ?: return@mapNotNull null
            val poster = el.selectFirst(".card__poster img, img")?.let {
                it.attr("src").ifBlank { it.attr("data-src") }
            }
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
            ?: doc.selectFirst(".card__poster img")?.attr("src")
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
                loadExtractor(resolveUrl(src), data, subtitleCallback, callback)
                found = true
            }
        }

        Regex("\"(https?://[^\"]+\\.(m3u8|mp4)[^\"]*)\"").findAll(html).forEach { m ->
            loadExtractor(m.groupValues[1], data, subtitleCallback, callback)
            found = true
        }

        return found
    }
}