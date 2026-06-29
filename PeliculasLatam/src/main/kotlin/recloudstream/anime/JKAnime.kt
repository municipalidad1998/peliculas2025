package recloudstream.anime

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.StringUtils.encodeUri
import recloudstream.BaseSiteProvider

class JKAnime : BaseSiteProvider() {
    override var mainUrl = "https://jkanime.net"
    override var name = "JKAnime"
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie)
    override var lang = "es"
    override val hasMainPage = true

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val doc = app.get(mainUrl).document
        // jkanime.net uses div.movie-card with .movie-card__img and .movie-card__title
        val results = doc.select("div.movie-card").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            if (href.isBlank()) return@mapNotNull null
            val title = el.selectFirst(".movie-card__title")?.text()
                ?: el.selectFirst("img")?.attr("alt")
                ?: return@mapNotNull null
            val poster = el.selectFirst(".movie-card__img img, img")?.let { 
                it.attr("src").ifBlank { it.attr("data-src") } 
            }
            this.newMovieSearchResponse(title.trim(), resolveUrl(href), TvType.Anime) {
                this.posterUrl = img(poster)
            }
        }
        return newHomePageResponse(listOf(HomePageList("Anime", results, true)))
    }

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        // jkanime.net search: /buscar/{query}/
        val doc = app.get("$mainUrl/buscar/${query.encodeUri()}/").document
        return doc.select("div.movie-card").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            if (href.isBlank()) return@mapNotNull null
            val title = el.selectFirst(".movie-card__title")?.text()
                ?: el.selectFirst("img")?.attr("alt")
                ?: a.text()
                ?: return@mapNotNull null
            val poster = el.selectFirst(".movie-card__img img, img")?.let {
                it.attr("src").ifBlank { it.attr("data-src") }
            }
            this.newMovieSearchResponse(title.trim(), resolveUrl(href), TvType.Anime) {
                this.posterUrl = img(poster)
            }
        }.toNewSearchResponseList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1, h2.title, .anime-title")?.text()
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?: return null
        val posterUrl = poster(doc)
        val plot = desc(doc)
        val genres = genres(doc)

        // jkanime.net episode list
        val episodes = doc.select(".list-episodes a, div.episodios a, ul.episodes li a, a[href*=episode], a[href*=capitulo]").mapNotNull { el ->
            val epUrl = el.attr("href")
            if (epUrl.isBlank()) return@mapNotNull null
            val epText = el.text()
            val epNum = Regex("(?:Ep|Episode|Cap)\\s*(\\d+)").find(epText)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("(\\d+)").find(epText)?.value?.toIntOrNull()
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
        data: String,
        isCasting: Boolean,
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
