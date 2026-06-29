package recloudstream.novelas

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import recloudstream.BaseSiteProvider
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.StringUtils.encodeUri

class TlNovelas : BaseSiteProvider() {
    override var mainUrl = "https://ww2.tlnovelas.net"
    override var name = "TlNovelas"
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)
    override var lang = "es"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get(mainUrl).document
        val results = doc.select("article, div[class*=novela], .poster, div[class*=item]").mapNotNull { el ->
            val title = el.selectFirst("h2, h3, .title, img")?.let { it.attr("alt").ifBlank { it.text() } } ?: return@mapNotNull null
            val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = el.selectFirst("img")?.let { it.attr("src").ifBlank { it.attr("data-src") } }
            this.newMovieSearchResponse(title.trim(), resolveUrl(href), TvType.TvSeries) {
                this.posterUrl = img(poster)
            }
        }
        return newHomePageResponse(listOf(HomePageList("Novelas", results, true)))
    }

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        val doc = app.get("$mainUrl/?s=${query.encodeUri()}").document
        return doc.select("article, div[class*=novela], .poster").mapNotNull { el ->
            val title = el.selectFirst("h2, h3, .title")?.text() ?: return@mapNotNull null
            val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
            val poster = el.selectFirst("img")?.let { it.attr("src").ifBlank { it.attr("data-src") } }
            this.newMovieSearchResponse(title.trim(), resolveUrl(href), TvType.TvSeries) {
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

        val episodes = doc.select("div.episodios a, ul.episodes li a, .episode-list a").mapNotNull { el ->
            val epUrl = el.attr("href")
            if (epUrl.isBlank()) return@mapNotNull null
            val epText = el.text()
            val epNum = Regex("(?:Ep|Episode|Cap|Capitulo)\\s*(\\d+)").find(epText)?.groupValues?.get(1)?.toIntOrNull()
            newEpisode(resolveUrl(epUrl)) {
                name = epText.trim()
                season = 1
                episode = epNum
            }
        }.sortedBy { it.episode ?: 0 }

        return if (episodes.isEmpty()) {
            newMovieLoadResponse(title.trim(), url, TvType.TvSeries, url) {
                this.posterUrl = img(poster)
                this.plot = plot
                this.tags = genres
            }
        } else {
            newTvSeriesLoadResponse(title.trim(), url, TvType.TvSeries, episodes) {
                this.posterUrl = img(poster)
                this.plot = plot
                this.tags = genres
            }
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
