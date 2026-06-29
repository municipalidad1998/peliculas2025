package recloudstream

import com.lagradost.cloudstream3.MainAPI

/**
 * Shared base class for all site providers.
 * Contains common helper methods to avoid code duplication across 20+ providers.
 */
abstract class BaseSiteProvider : MainAPI() {

    /**
     * Resolves a relative URL against the main URL.
     * Handles http://, //, and relative paths.
     */
    fun resolveUrl(href: String): String {
        if (href.startsWith("http")) return href
        if (href.startsWith("//")) return "https:$href"
        return "${mainUrl.trimEnd('/')}/${href.trimStart('/')}"
    }

    /**
     * Normalizes an image URL to ensure it has a protocol.
     * Handles http://, //, and relative paths.
     */
    fun img(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val t = url.trim()
        return when {
            t.startsWith("http") -> t
            t.startsWith("//") -> "https:$t"
            else -> "$mainUrl/$t"
        }
    }

    /**
     * Extracts poster from common patterns: og:image meta, .poster img, .cover img
     */
    fun poster(doc: org.jsoup.nodes.Document): String? {
        return doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: doc.selectFirst(".poster img, .cover img")?.attr("src")
            ?.let { img(it) }
    }

    /**
     * Extracts description from common patterns: og:description meta, .description, .sinopsis
     */
    fun desc(doc: org.jsoup.nodes.Document): String? {
        return doc.selectFirst("meta[property=og:description]")?.attr("content")
            ?: doc.selectFirst(".description, .sinopsis, .synopsis, .excerpt")?.text()
    }

    /**
     * Extracts genres from common patterns: .genre a, .genres a, .generos a
     */
    fun genres(doc: org.jsoup.nodes.Document): List<String> {
        return doc.select(".genre a, .genres a, .generos a, .tag").map { it.text().trim() }
    }
}
