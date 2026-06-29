package com.cloudstreamext.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements

/**
 * Utility class for parsing HTML documents using Jsoup.
 * Provides convenience methods for common scraping patterns.
 *
 * Usage:
 * ```kotlin
 * val doc = HtmlParser.parse(htmlString)
 * val title = HtmlParser.text(doc, "h1.title")
 * val links = HtmlParser.selectUrls(doc, "a.movie-link", baseUrl)
 * ```
 */
object HtmlParser {

    /**
     * Parses an HTML string into a Jsoup Document.
     */
    fun parse(html: String): Document = Jsoup.parse(html)

    /**
     * Parses a fragment of HTML (not wrapped in full document structure).
     */
    fun parseFragment(html: String): Document = Jsoup.parseBodyFragment(html)

    /**
     * Extracts text content from a CSS selector, returning null if not found.
     */
    fun text(doc: Document, selector: String, clean: Boolean = true): String? {
        val element = doc.selectFirst(selector) ?: return null
        return if (clean) element.text().trim() else element.html()
    }

    /**
     * Extracts the attribute value from an element matching a CSS selector.
     */
    fun attr(doc: Document, selector: String, attribute: String): String? {
        return doc.selectFirst(selector)?.attr(attribute)?.trim()?.ifBlank { null }
    }

    /**
     * Extracts all text content from elements matching a CSS selector.
     */
    fun texts(doc: Document, selector: String): List<String> {
        return doc.select(selector).map { it.text().trim() }.filter { it.isNotBlank() }
    }

    /**
     * Extracts URLs from all elements matching a CSS selector.
     * Resolves relative URLs against a base URL.
     */
    fun selectUrls(doc: Document, selector: String, baseUrl: String): List<String> {
        return doc.select(selector)
            .mapNotNull { it.attr("href").trim().ifBlank { null } }
            .map { resolveUrl(baseUrl, it) }
    }

    /**
     * Extracts an attribute from multiple elements matching a CSS selector.
     */
    fun selectAttrs(doc: Document, selector: String, attribute: String): List<String> {
        return doc.select(selector)
            .mapNotNull { it.attr(attribute).trim().ifBlank { null } }
    }

    /**
     * Extracts the inner HTML from an element matching a CSS selector.
     */
    fun html(doc: Document, selector: String): String? {
        return doc.selectFirst(selector)?.html()
    }

    /**
     * Extracts data from an element, trying multiple selectors in order.
     */
    fun textFirstOf(doc: Document, vararg selectors: String): String? {
        for (selector in selectors) {
            val text = text(doc, selector)
            if (text != null) return text
        }
        return null
    }

    /**
     * Extracts an attribute, trying multiple selectors in order.
     */
    fun attrFirstOf(doc: Document, attribute: String, vararg selectors: String): String? {
        for (selector in selectors) {
            val value = attr(doc, selector, attribute)
            if (value != null) return value
        }
        return null
    }

    /**
     * Finds an element by selector and returns its parent.
     */
    fun parent(doc: Document, selector: String): Element? {
        return doc.selectFirst(selector)?.parent()
    }

    /**
     * Finds all elements matching a selector and extracts data into a list of maps.
     *
     * Example:
     * ```kotlin
     * val items = HtmlParser.extractList(doc, ".movie-card") { element ->
     *     mapOf(
     *         "title" to (element.selectFirst("h3")?.text() ?: ""),
     *         "url" to (element.selectFirst("a")?.attr("href") ?: ""),
     *         "poster" to (element.selectFirst("img")?.attr("src") ?: "")
     *     )
     * }
     * ```
     */
    fun <T> extractList(doc: Document, selector: String, mapper: (Element) -> T): List<T> {
        return doc.select(selector).map(mapper)
    }

    /**
     * Extracts a table as a list of maps (column name -> value).
     */
    fun tableToMaps(doc: Document, selector: String): List<Map<String, String>> {
        val table = doc.selectFirst(selector) ?: return emptyList()
        val headers = table.select("thead th, thead td").map { it.text().trim() }
        val rows = table.select("tbody tr")
        return rows.map { row ->
            val cells = row.select("td").map { it.text().trim() }
            headers.zip(cells).toMap()
        }
    }

    /**
     * Resolves a potentially relative URL against a base URL.
     */
    fun resolveUrl(baseUrl: String, url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        if (url.startsWith("//")) return "https:$url"
        val base = baseUrl.trimEnd('/')
        val path = url.trimStart('/')
        return "$base/$path"
    }

    /**
     * Normalizes an image URL, handling protocol-relative and relative URLs.
     */
    fun normalizeImageUrl(url: String?, baseUrl: String = ""): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            baseUrl.isNotBlank() -> resolveUrl(baseUrl, trimmed)
            else -> trimmed
        }
    }
}
