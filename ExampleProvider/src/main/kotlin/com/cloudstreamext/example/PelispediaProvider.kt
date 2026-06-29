package com.cloudstreamext.example

import com.cloudstreamext.base.MovieProvider
import com.cloudstreamext.util.*
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink

/**
 * Pelispedia Provider - pelispedia.is
 * Real movie/series provider scraping from pelispedia.is
 */
class PelispediaProvider : MovieProvider() {

    override var mainUrl = "https://pelispedia.is"
    override var name = "Pelispedia"
    override var lang = "es"
    override var hasMainPage = true
    override var usesHttps = true

    override var defaultHeaders: MutableMap<String, String> = mutableMapOf(
        "Accept-Language" to "es-ES,es;q=0.9",
        "Referer" to "https://pelispedia.is/"
    )

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        return try {
            val doc = safeDocument("$mainUrl/page/$page/") ?: safeDocument(mainUrl) ?: return null
            val lists = mutableListOf<HomePageListResponse>()

            // Movie cards
            val movies = doc.select("article, div[class*=movie], div[class*=item], .poster").mapNotNull { el ->
                val title = el.selectFirst("h2, h3, .title, .movie-title, .entry-title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src") ?: el.selectFirst("img")?.attr("data-src")
                val rating = el.selectFirst(".rating, .score, .calidad")?.text()
                val year = parseYear(el.selectFirst(".year, .date, time")?.text() ?: "")
                movieSearchResponse(formatTitle(title), resolveUrl(mainUrl, href), normalizeImageUrl(poster), year)
            }
            if (movies.isNotEmpty()) lists.add(HomePageListResponse("Peliculas", movies))

            HomePageList("Pelispedia", lists)
        } catch (_: Exception) { null }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/?s=${query.urlEncode()}") ?: throw Exception("Search failed")
            doc.select("article, div[class*=movie], div[class*=item], .poster").mapNotNull { el ->
                val title = el.selectFirst("h2, h3, .title, .entry-title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src") ?: el.selectFirst("img")?.attr("data-src")
                val year = parseYear(el.selectFirst(".year, .date, time")?.text() ?: "")
                movieSearchResponse(formatTitle(title), resolveUrl(mainUrl, href), normalizeImageUrl(poster), year)
            }
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        return withRetry {
            val doc = safeDocument(url) ?: throw Exception("Failed to load")
            val title = doc.ogTitle() ?: doc.selectFirst("h1, h2.title, .entry-title")?.text() ?: throw Exception("No title")
            val poster = extractPoster(doc)
            val banner = extractBanner(doc)
            val plot = doc.ogDescription() ?: doc.selectFirst(".description, .sinopsis, .excerpt, .content")?.text()
            val year = parseYear(doc.selectFirst(".year, .date, time, span.meta-date")?.text() ?: "")
            val ratingText = doc.selectFirst(".rating, .score, .calificacion")?.text()
            val rating = ratingText?.let { parseRating(it) }?.toInt()
            val genres = extractGenres(doc)
            val actors = extractActors(doc)

            // Check if it's a series or movie
            val episodes = doc.select("div.episodios a, ul.episode-list li a, .ep-link a, a[href*=capitulo]").mapNotNull { el ->
                val epUrl = el.attr("href")
                if (epUrl.isBlank()) return@mapNotNull null
                val epText = el.text()
                val epNum = Regex("(?:Cap|Ep|Capitulo)\\s*(\\d+)").find(epText)?.groupValues?.get(1)?.toIntOrNull()
                Episode(name = epText.trim(), url = resolveUrl(url, epUrl), season = 1, episode = epNum)
            }.sortedBy { it.episode ?: 0 }

            if (episodes.isNotEmpty()) {
                newTvSeriesLoadResponse(formatTitle(title), url, TvType.TvSeries, episodes) {
                    this.posterUrl = poster
                    this.backgroundPosterUrl = banner
                    this.plot = plot
                    this.year = year
                    this.rating = rating
                    this.tags = genres
                    this.actors = actors
                }
            } else {
                newMovieLoadResponse(formatTitle(title), url) {
                    this.posterUrl = poster
                    this.backgroundPosterUrl = banner
                    this.plot = plot
                    this.year = year
                    this.rating = rating
                    this.tags = genres
                    this.actors = actors
                }
            }
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
            var found = false

            // Extract iframe sources
            doc.select("iframe[src]").forEach { iframe ->
                val src = iframe.attr("src")
                if (src.isNotBlank() && !src.contains("google") && !src.contains("facebook")) {
                    callback(ExtractorLink(name, name, resolveUrl(data, src), data, "Unknown", headers = mapOf("Referer" to data)))
                    found = true
                }
            }

            // Extract m3u8/mp4 sources
            Regex("\"(https?://[^\"]+\\.(m3u8|mp4)[^\"]*)\"").findAll(html).forEach { match ->
                val url = match.groupValues[1]
                val isHls = match.groupValues[2] == "m3u8"
                val quality = when {
                    url.contains("1080p") -> "1080p"
                    url.contains("720p") -> "720p"
                    url.contains("480p") -> "480p"
                    else -> "Unknown"
                }
                callback(ExtractorLink(name, name, url, data, quality, isM3u8 = isHls, headers = mapOf("Referer" to data)))
                found = true
            }

            found
        } catch (_: Exception) { false }
    }

    private fun resolveUrl(base: String, relative: String): String {
        if (relative.startsWith("http")) return relative
        if (relative.startsWith("//")) return "https:$relative"
        return "${base.trimEnd('/')}/${relative.trimStart('/')}"
    }

    private fun normalizeImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            else -> "$mainUrl/$trimmed"
        }
    }

    private fun formatTitle(title: String): String = title.replace(Regex("\\s+"), " ").trim()
    private fun parseYear(text: String): Int? = Regex("(19|20)\\d{2}").find(text)?.value?.toIntOrNull()
    private fun parseRating(text: String): Double? = Regex("([\\d.]+)").find(text)?.value?.toDoubleOrNull()
    private fun String.urlEncode(): String = java.net.URLEncoder.encode(this, "UTF-8")
}
