package com.cloudstreamext.example

import com.cloudstreamext.base.AnimeProvider
import com.cloudstreamext.util.*
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink

/**
 * JKAnime Provider - jkanime.net
 * Real anime provider scraping from jkanime.net
 */
class JKAnimeProvider : AnimeProvider() {

    override var mainUrl = "https://jkanime.net"
    override var name = "JKAnime"
    override var lang = "es"
    override var hasMainPage = true
    override var usesHttps = true

    override var defaultHeaders: MutableMap<String, String> = mutableMapOf(
        "Accept-Language" to "es-ES,es;q=0.9",
        "Referer" to "https://jkanime.net/"
    )

    // --- Main Page ---

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        return try {
            val doc = safeDocument(mainUrl) ?: return null

            // Latest episodes section
            val latestEpisodes = doc.select("div.ep-card, article.ep-card, div[class*=ep-item]").mapNotNull { el ->
                val title = el.selectFirst("h2, h3, .ep-title, .title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src") ?: el.selectFirst("img")?.attr("data-src")
                val epText = el.selectFirst(".ep-num, .episode-num")?.text()
                val episode = Regex("(\\d+)").find(epText ?: "")?.value?.toIntOrNull()

                Episode(
                    name = title,
                    url = resolveUrl(mainUrl, href),
                    season = 1,
                    episode = episode,
                    poster = normalizeImageUrl(poster)
                )
            }

            // Featured/popular section
            val featured = doc.select("div.featured-item, div.hero__item, article.anime-card").mapNotNull { el ->
                val title = el.selectFirst("h2, h3, .anime-title, .title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src") ?: el.selectFirst("img")?.attr("data-src")
                val type = el.selectFirst(".type, .badge")?.text()

                animeSearchResponse(
                    formatTitle(title),
                    resolveUrl(mainUrl, href),
                    normalizeImageUrl(poster)
                )
            }

            val lists = mutableListOf<HomePageListResponse>()
            if (latestEpisodes.isNotEmpty()) {
                lists.add(HomePageListResponse("Últimos Episodios", featured))
            }
            if (featured.isNotEmpty()) {
                lists.add(HomePageListResponse("Destacados", featured))
            }
            HomePageList("JKAnime", lists)
        } catch (e: Exception) {
            null
        }
    }

    // --- Search ---

    override suspend fun search(query: String): List<SearchResponse> {
        return withRetry {
            val doc = safeDocument("$mainUrl/buscar/${query.urlEncode()}/")
                ?: throw Exception("Search failed")

            doc.select("div.anime-card, article.anime-card, div[class*=anime]").mapNotNull { el ->
                val title = el.selectFirst("h2, h3, .anime-title, .title")?.text() ?: return@mapNotNull null
                val href = el.selectFirst("a")?.attr("href") ?: return@mapNotNull null
                val poster = el.selectFirst("img")?.attr("src") ?: el.selectFirst("img")?.attr("data-src")
                val status = el.selectFirst(".status, .badge")?.text()?.lowercase()
                val isDub = status?.contains("doblado") == true

                animeSearchResponse(
                    formatTitle(title),
                    resolveUrl(mainUrl, href),
                    normalizeImageUrl(poster),
                    isDub = isDub
                )
            }
        }
    }

    // --- Load Details ---

    override suspend fun load(url: String): LoadResponse? {
        return withRetry {
            val doc = safeDocument(url) ?: throw Exception("Failed to load anime")

            val title = doc.ogTitle() ?: doc.selectFirst("h1, h2.title, .anime-title")?.text()
                ?: throw Exception("No title found")
            val poster = extractPoster(doc) ?: doc.selectFirst(".poster img, .cover img")?.attr("src")
            val banner = extractBanner(doc)
            val plot = doc.ogDescription() ?: doc.selectFirst(".description, .sinopsis, .synopsis")?.text()
            val year = parseYear(doc.selectFirst(".year, .fecha, .date")?.text() ?: "")
            val genres = doc.select(".genre a, .genres a, .generos a").map { it.text().trim() }
            val type = doc.selectFirst(".type, .badge-type")?.text()?.lowercase()
            val isMovie = type?.contains("pelicula") == true

            // Extract episodes
            val episodes = doc.select("div.episodios a, ul.episodes li a, .episode-list a, .cap-list a").mapNotNull { el ->
                val epUrl = el.attr("href")
                if (epUrl.isBlank()) return@mapNotNull null
                val epText = el.text()
                val epNum = Regex("Ep(?:isode)?\\s*(\\d+)").find(epText)?.groupValues?.get(1)?.toIntOrNull()
                    ?: Regex("(\\d+)").find(epText)?.value?.toIntOrNull()
                val epTitle = el.selectFirst(".ep-title, .cap-title")?.text() ?: epText

                Episode(
                    name = epTitle.trim(),
                    url = resolveUrl(mainUrl, epUrl),
                    season = 1,
                    episode = epNum
                )
            }.sortedBy { it.episode ?: 0 }

            if (isMovie || episodes.isEmpty()) {
                newMovieLoadResponse(formatTitle(title), url, TvType.AnimeMovie) {
                    this.posterUrl = poster
                    this.backgroundPosterUrl = banner
                    this.plot = plot
                    this.year = year
                    this.tags = genres
                }
            } else {
                newTvSeriesLoadResponse(formatTitle(title), url, TvType.Anime, episodes) {
                    this.posterUrl = poster
                    this.backgroundPosterUrl = banner
                    this.plot = plot
                    this.year = year
                    this.tags = genres
                }
            }
        }
    }

    // --- Load Links ---

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        return try {
            val doc = safeDocument(data) ?: throw Exception("Failed to load")

            // Extract iframe sources from the video player
            val iframes = doc.select("iframe[src]").map { it.attr("src") }
            for (iframeUrl in iframes) {
                if (iframeUrl.isNotBlank()) {
                    callback(
                        ExtractorLink(
                            source = name,
                            name = name,
                            url = resolveUrl(data, iframeUrl),
                            referer = data,
                            quality = "Unknown",
                            headers = mapOf("Referer" to data)
                        )
                    )
                }
            }

            // Also try direct video sources in page HTML
            val html = doc.html()
            val m3u8Pattern = Regex("\"(https?://[^\"]+\\.m3u8[^\"]*)\"")
            m3u8Pattern.findAll(html).forEach { match ->
                val url = match.groupValues[1]
                callback(
                    ExtractorLink(
                        source = name,
                        name = name,
                        url = url,
                        referer = data,
                        quality = "Unknown",
                        isM3u8 = true,
                        headers = mapOf("Referer" to data)
                    )
                )
            }

            // Extract subtitles
            doc.select("track[kind=subtitles], track[kind=captions]").forEach { track ->
                val src = track.attr("src")
                if (src.isNotBlank()) {
                    val lang = track.attr("srclang").ifBlank { "es" }
                    val label = track.attr("label").ifBlank { lang }
                    subtitleCallback(SubtitleFile(label, resolveUrl(data, src)))
                }
            }

            iframes.isNotEmpty() || m3u8Pattern.containsMatchIn(html)
        } catch (_: Exception) {
            false
        }
    }

    // --- Helpers ---

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

    private fun formatTitle(title: String): String {
        return title.replace(Regex("\\s+"), " ").trim()
    }

    private fun parseYear(text: String): Int? {
        return Regex("(19|20)\\d{2}").find(text)?.value?.toIntOrNull()
    }

    private fun String.urlEncode(): String = java.net.URLEncoder.encode(this, "UTF-8")
}
