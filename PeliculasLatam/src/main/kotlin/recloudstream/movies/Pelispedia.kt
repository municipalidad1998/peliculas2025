package recloudstream.movies

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.StringUtils.encodeUri
import recloudstream.BaseSiteProvider

class Pelispedia : BaseSiteProvider() {
    override var mainUrl = "https://pelispedia.is"
    override var name = "Pelispedia"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "es"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get(mainUrl).document
        // pelispedia.is uses article.post.dfx.fcl.movies for cards
        val results = doc.select("article.post, article[class*=post], .poster, div[class*=movie]").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            if (href.isBlank()) return@mapNotNull null
            val title = el.selectFirst("h2, h3")?.text()
                ?: el.selectFirst("img")?.attr("alt")
                ?: return@mapNotNull null
            val poster = el.selectFirst("img")?.let { it.attr("src").ifBlank { it.attr("data-src") } }
            this.newMovieSearchResponse(title.trim(), resolveUrl(href), TvType.Movie) {
                this.posterUrl = img(poster)
            }
        }
        return newHomePageResponse(listOf(HomePageList("Peliculas", results, true)))
    }

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        val doc = app.get("$mainUrl/?s=${query.encodeUri()}").document
        // Search results use same article.post structure
        return doc.select("article.post, article[class*=post], .poster, div[class*=movie]").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            if (href.isBlank()) return@mapNotNull null
            val title = el.selectFirst("h2, h3")?.text()
                ?: el.selectFirst("img")?.attr("alt")
                ?: return@mapNotNull null
            val poster = el.selectFirst("img")?.let { it.attr("src").ifBlank { it.attr("data-src") } }
            this.newMovieSearchResponse(title.trim(), resolveUrl(href), TvType.Movie) {
                this.posterUrl = img(poster)
            }
        }.toNewSearchResponseList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1, h2.title, .entry-title")?.text()
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content") ?: return null
        val posterUrl = poster(doc)
        val plot = desc(doc)
        val genres = genres(doc)

        // Check for series episodes (capitulos)
        val episodes = doc.select("a[href*=capitulo], a[href*=episode], div.episodios a").mapNotNull { el ->
            val epUrl = el.attr("href")
            if (epUrl.isBlank()) return@mapNotNull null
            val epText = el.text()
            val epNum = Regex("(?:Cap|Ep|Episode)\\s*(\\d+)").find(epText)?.groupValues?.get(1)?.toIntOrNull()
            newEpisode(resolveUrl(epUrl)) {
                name = epText.trim()
                season = 1
                episode = epNum
            }
        }.sortedBy { it.episode ?: 0 }

        return if (episodes.isEmpty()) {
            newMovieLoadResponse(title.trim(), url, TvType.Movie, url) {
                this.posterUrl = posterUrl
                this.plot = plot
                this.tags = genres
            }
        } else {
            newTvSeriesLoadResponse(title.trim(), url, TvType.TvSeries, episodes) {
                this.posterUrl = posterUrl
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
            if (src.isNotBlank() && !src.contains("google") && !src.contains("facebook")) {
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
