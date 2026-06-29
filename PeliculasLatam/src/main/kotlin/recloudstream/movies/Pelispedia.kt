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
        val results = doc.select("article.post, article, div[class*=movie], div[class*=item]").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            if (href.isBlank()) return@mapNotNull null
            val title = el.selectFirst("h2.entry-title, h3, .title, .entry-title")?.text()
                ?: el.selectFirst("img")?.attr("alt")
                ?: return@mapNotNull null
            val poster = el.selectFirst("img")?.let {
                it.attr("src").ifBlank { it.attr("data-src") }
            }
            this.newMovieSearchResponse(title.trim(), resolveUrl(href), TvType.Movie) {
                this.posterUrl = img(poster)
            }
        }
        return newHomePageResponse(listOf(HomePageList("Peliculas", results, true)))
    }

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        val doc = app.get("$mainUrl/?s=${query.encodeUri()}").document
        return doc.select("article.post, article, div[class*=movie]").mapNotNull { el ->
            val a = el.selectFirst("a") ?: return@mapNotNull null
            val href = a.attr("href")
            if (href.isBlank()) return@mapNotNull null
            val title = el.selectFirst("h2.entry-title, h3, .title")?.text()
                ?: el.selectFirst("img")?.attr("alt")
                ?: return@mapNotNull null
            val poster = el.selectFirst("img")?.let {
                it.attr("src").ifBlank { it.attr("data-src") }
            }
            this.newMovieSearchResponse(title.trim(), resolveUrl(href), TvType.Movie) {
                this.posterUrl = img(poster)
            }
        }.toNewSearchResponseList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1, h2.entry-title, .title")?.text()
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?: return null
        val posterUrl = poster(doc)
        val plot = desc(doc)
        val genres = genres(doc)

        val episodes = doc.select("div.episodios a, ul.episode-list li a, a[href*=capitulo]").mapNotNull { el ->
            val epUrl = el.attr("href")
            if (epUrl.isBlank()) return@mapNotNull null
            val epText = el.text()
            val epNum = Regex("""(?:Cap|Ep)\s*(\d+)""").find(epText)?.groupValues?.get(1)?.toIntOrNull()
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

    /**
     * Collect all server URLs from a page's iframes and links, then pass each to CloudStream extractors.
     * Returns true if any links were found.
     */
    private suspend fun collectAndLoadLinks(
        doc: org.jsoup.nodes.Document,
        pageUrl: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        var found = false

        // Extract all iframe src and data-src URLs
        val iframeUrls = mutableListOf<String>()
        doc.select("iframe[src]").forEach { el ->
            val src = el.attr("src")
            if (src.isNotBlank() && src.startsWith("http")) iframeUrls.add(src)
        }
        doc.select("iframe[data-src]").forEach { el ->
            val src = el.attr("data-src")
            if (src.isNotBlank() && src.startsWith("http")) iframeUrls.add(src)
        }

        for (url in iframeUrls) {
            if (url.contains("trembed")) {
                // Follow the trembed chain
                try {
                    val trembedDoc = app.get(url, referer = "$mainUrl/").document
                    // trembed pages contain an iframe to the actual server
                    val serverIframe = trembedDoc.selectFirst("iframe[src]")?.attr("src")
                        ?: trembedDoc.selectFirst("iframe[data-src]")?.attr("data-src")
                    if (!serverIframe.isNullOrBlank() && serverIframe.startsWith("http")) {
                        // Try CloudStream's built-in extractors for this server
                        loadExtractor(serverIframe, pageUrl, subtitleCallback, callback)
                        found = true
                    }
                } catch (_: Exception) {}
            } else {
                loadExtractor(url, pageUrl, subtitleCallback, callback)
                found = true
            }
        }

        return found
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

        // Strategy 1: Follow trembed chain (Pelispedia's primary embed system)
        val trembedUrls = mutableListOf<String>()

        doc.select("iframe[data-src]").forEach { iframe ->
            val src = iframe.attr("data-src")
            if (src.contains("trembed")) trembedUrls.add(resolveUrl(src))
        }
        doc.select("iframe[src]").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.contains("trembed")) trembedUrls.add(resolveUrl(src))
        }
        Regex("""['"]([^'"]*trembed[^'"]*)['"]""").findAll(html).forEach { m ->
            val url = m.groupValues[1]
            if (url.isNotBlank() && !trembedUrls.any { it.contains(url) }) {
                trembedUrls.add(resolveUrl(url))
            }
        }

        for (trembedUrl in trembedUrls) {
            try {
                val response = app.get(trembedUrl, referer = "$mainUrl/")
                val trembedDoc = response.document
                val serverIframe = trembedDoc.selectFirst("iframe[src]")?.attr("src")
                    ?: trembedDoc.selectFirst("iframe[data-src]")?.attr("data-src")

                if (!serverIframe.isNullOrBlank() && serverIframe.startsWith("http")) {
                    // Pass the server URL to CloudStream's built-in extractors
                    loadExtractor(serverIframe, data, subtitleCallback, callback)
                    found = true

                    // For pastea.me: follow the page and extract links from <a> tags
                    if (serverIframe.contains("pastea.me")) {
                        val pasteDoc = app.get(serverIframe, referer = trembedUrl).document
                        collectAndLoadLinks(pasteDoc, serverIframe, subtitleCallback, callback)
                    }
                }
            } catch (_: Exception) {}
        }

        // Strategy 2: Regular iframes directly on the episode page
        doc.select("iframe[src], iframe[data-src]").forEach { iframe ->
            val src = iframe.attr("src").ifBlank { iframe.attr("data-src") }
            if (src.isNotBlank() && !src.contains("trembed") && src.startsWith("http")) {
                loadExtractor(resolveUrl(src), data, subtitleCallback, callback)
                found = true
            }
        }

        return found
    }
}
