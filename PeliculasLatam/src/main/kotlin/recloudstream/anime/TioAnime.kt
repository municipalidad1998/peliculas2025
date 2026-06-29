package recloudstream.anime

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.StringUtils.encodeUri
import recloudstream.BaseSiteProvider

class TioAnime : BaseSiteProvider() {
    override var mainUrl = "https://tioanime.com"
    override var name = "TioAnime"
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie)
    override var lang = "es"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get(mainUrl).document
        // tioanime.com uses article.anime for cards
        val results = doc.select("article.anime, .poster").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            if (href.isBlank()) return@mapNotNull null
            val title = el.selectFirst("h2, h3")?.text()
                ?: el.selectFirst("img")?.attr("alt")
                ?: return@mapNotNull null
            val poster = el.selectFirst("img")?.let { it.attr("src").ifBlank { it.attr("data-src") } }
            this.newMovieSearchResponse(title.trim(), resolveUrl(href), TvType.Anime) {
                this.posterUrl = img(poster)
            }
        }
        return newHomePageResponse(listOf(HomePageList("Anime", results, true)))
    }

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        val doc = app.get("$mainUrl/directorio?q=${query.encodeUri()}").document
        // Search results use same article.anime structure
        return doc.select("article.anime, .poster").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            if (href.isBlank()) return@mapNotNull null
            val title = el.selectFirst("h2, h3")?.text()
                ?: el.selectFirst("img")?.attr("alt")
                ?: return@mapNotNull null
            val poster = el.selectFirst("img")?.let { it.attr("src").ifBlank { it.attr("data-src") } }
            this.newMovieSearchResponse(title.trim(), resolveUrl(href), TvType.Anime) {
                this.posterUrl = img(poster)
            }
        }.toNewSearchResponseList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1, h2.title")?.text()
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content") ?: return null
        val posterUrl = poster(doc)
        val plot = desc(doc)
        val genres = genres(doc)

        // tioanime.com episodes are links with href starting with /ver/
        val episodes = doc.select("a[href*=/ver/], ul.episodes-list li a, .episode-list a").mapNotNull { el ->
            val epUrl = el.attr("href")
            if (epUrl.isBlank()) return@mapNotNull null
            val epText = el.text()
            val epNum = Regex("(?:Ep|Episode)\\s*(\\d+)").find(epText)?.groupValues?.get(1)?.toIntOrNull()
            newEpisode(resolveUrl(epUrl)) {
                name = epText.trim()
                season = 1
                episode = epNum
            }
        }.sortedBy { it.episode ?: 0 }

        return if (episodes.isEmpty()) {
            newMovieLoadResponse(title.trim(), url, TvType.AnimeMovie, url) {
                this.posterUrl = posterUrl
                this.plot = plot
                this.tags = genres
            }
        } else {
            newTvSeriesLoadResponse(title.trim(), url, TvType.Anime, episodes) {
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
