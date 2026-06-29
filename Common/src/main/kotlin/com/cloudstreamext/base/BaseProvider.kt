package com.cloudstreamext.base

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.app
import okhttp3.OkHttpClient
import okhttp3.Headers
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

/**
 * Base provider for all CloudStream extensions.
 * Provides common networking, parsing, error handling, rate limiting, and caching.
 *
 * To create a new provider:
 * 1. Extend the appropriate type provider (MovieProvider, SeriesProvider, AnimeProvider, LiveTVProvider)
 * 2. Set mainUrl, name, supportedTypes
 * 3. Implement search(), load(), loadLinks()
 */
abstract class BaseProvider : MainAPI() {

    // --- Networking Configuration ---

    /** Custom user agent for requests */
    open var userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /** Custom headers for all requests */
    override var defaultHeaders: MutableMap<String, String> = mutableMapOf(
        "Accept-Language" to "en-US,en;q=0.9",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
    )

    /** Request timeout in seconds */
    open val requestTimeout: Long = 30L

    /** Connect timeout in seconds */
    open val connectTimeout: Long = 15L

    /** Read timeout in seconds */
    open val readTimeout: Long = 30L

    /** Maximum retry attempts */
    open val maxRetries: Int = 3

    /** Retry delay in milliseconds */
    open val retryDelayMs: Long = 1000L

    /** Rate limit delay between requests in milliseconds */
    open val rateLimitMs: Long = 500L

    // --- Rate Limiter ---

    @Volatile
    private var lastRequestTime: Long = 0L

    /**
     * Enforces rate limiting between requests.
     * Call before making external requests.
     */
    protected suspend fun rateLimitedRequest() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTime
        if (elapsed < rateLimitMs) {
            kotlinx.coroutines.delay(rateLimitMs - elapsed)
        }
        lastRequestTime = System.currentTimeMillis()
    }

    // --- HTTP Client ---

    /**
     * Builds a reusable OkHttpClient with configured timeouts and interceptors.
     */
    open fun buildClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .writeTimeout(requestTimeout, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", userAgent)
                    .apply {
                        defaultHeaders.forEach { (key, value) ->
                            header(key, value)
                        }
                    }
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    /**
     * HTTP client instance for this provider.
     */
    val client: OkHttpClient by lazy { buildClient() }

    // --- Cookie Management ---

    private val cookies: MutableMap<String, String> = mutableMapOf()

    /**
     * Sets a cookie for subsequent requests.
     */
    fun setCookie(name: String, value: String) {
        cookies[name] = value
    }

    /**
     * Gets the current cookies as a formatted string.
     */
    fun getCookieString(): String {
        return cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    /**
     * Clears all stored cookies.
     */
    fun clearCookies() {
        cookies.clear()
    }

    /**
     * Parses Set-Cookie headers and stores cookies for the domain.
     */
    fun parseCookies(responseHeaders: Headers) {
        responseHeaders.values("Set-Cookie").forEach { cookie ->
            val parts = cookie.split(";").first().split("=", limit = 2)
            if (parts.size == 2) {
                cookies[parts[0].trim()] = parts[1].trim()
            }
        }
    }

    // --- Smart Caching ---

    private val responseCache = LinkedHashMap<String, CachedResponse>(64, 0.75f, true)

    /**
     * Cached response wrapper.
     */
    data class CachedResponse(
        val body: String,
        val timestamp: Long,
        val cacheDurationMs: Long = 300_000L // 5 minutes default
    ) {
        val isExpired: Boolean get() = System.currentTimeMillis() - timestamp > cacheDurationMs
    }

    /**
     * Makes a cached GET request. Returns cached response if available and not expired.
     */
    open suspend fun getCachedDocument(
        url: String,
        cacheDurationMs: Long = 300_000L,
        customHeaders: Map<String, String> = emptyMap()
    ): Document {
        val cached = responseCache[url]
        if (cached != null && !cached.isExpired) {
            return Jsoup.parse(cached.body)
        }

        rateLimitedRequest()
        val response = app.get(url, headers = buildRequestHeaders(customHeaders))
        val body = response.text

        responseCache[url] = CachedResponse(body, System.currentTimeMillis(), cacheDurationMs)

        // Evict old entries if cache grows too large
        if (responseCache.size > 100) {
            val iterator = responseCache.iterator()
            var removed = 0
            while (iterator.hasNext() && removed < 20) {
                if (iterator.next().value.isExpired) {
                    iterator.remove()
                    removed++
                }
            }
        }

        return Jsoup.parse(body)
    }

    /**
     * Builds headers map for requests including cookies.
     */
    open fun buildRequestHeaders(customHeaders: Map<String, String> = emptyMap()): Map<String, String> {
        val allHeaders = mutableMapOf<String, String>()
        allHeaders.putAll(defaultHeaders)
        allHeaders["User-Agent"] = userAgent
        val cookieStr = getCookieString()
        if (cookieStr.isNotEmpty()) {
            allHeaders["Cookie"] = cookieStr
        }
        allHeaders.putAll(customHeaders)
        return allHeaders
    }

    // --- Error Handling ---

    /**
     * Executes a block with retry logic and error handling.
     */
    protected suspend fun <T> withRetry(
        retries: Int = maxRetries,
        delayMs: Long = retryDelayMs,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        repeat(retries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < retries - 1) {
                    kotlinx.coroutines.delay(delayMs * (attempt + 1))
                }
            }
        }
        throw lastException ?: Exception("Unknown error after $retries retries")
    }

    /**
     * Safely loads a document from a URL with error handling.
     */
    protected suspend fun safeDocument(
        url: String,
        customHeaders: Map<String, String> = emptyMap()
    ): Document? {
        return try {
            withRetry {
                rateLimitedRequest()
                val response = app.get(url, headers = buildRequestHeaders(customHeaders))
                val resp = response.okhttpResponse
                if (resp != null) parseCookies(resp.headers)
                response.document
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Safely loads text content from a URL.
     */
    protected suspend fun safeText(
        url: String,
        customHeaders: Map<String, String> = emptyMap()
    ): String? {
        return try {
            withRetry {
                rateLimitedRequest()
                val response = app.get(url, headers = buildRequestHeaders(customHeaders))
                val resp = response.okhttpResponse
                if (resp != null) parseCookies(resp.headers)
                response.text
            }
        } catch (e: Exception) {
            null
        }
    }

    // --- Quality Selector ---

    /**
     * Quality mappings for automatic selection.
     */
    enum class Quality(val label: String, val priority: Int) {
        QUALITY_4K("4K", 8),
        QUALITY_2160P("2160p", 7),
        QUALITY_1080P("1080p", 6),
        QUALITY_720P("720p", 5),
        QUALITY_480P("480p", 4),
        QUALITY_360P("360p", 3),
        QUALITY_240P("240p", 2),
        QUALITY_SD("SD", 1),
        QUALITY_UNKNOWN("Unknown", 0);

        companion object {
            /**
             * Detects quality from a string (URL, title, or quality label).
             */
            fun detectFrom(text: String): Quality {
                val lower = text.lowercase()
                return when {
                    lower.contains("2160p") || lower.contains("4k") || lower.contains("uhd") -> QUALITY_2160P
                    lower.contains("1080p") || lower.contains("fhd") -> QUALITY_1080P
                    lower.contains("720p") || lower.contains("hd") -> QUALITY_720P
                    lower.contains("480p") || lower.contains("sd") -> QUALITY_480P
                    lower.contains("360p") -> QUALITY_360P
                    lower.contains("240p") -> QUALITY_240P
                    else -> QUALITY_UNKNOWN
                }
            }
        }
    }

    /**
     * Sorts extractor links by quality (highest first).
     */
    protected fun sortByQuality(links: List<ExtractorLink>): List<ExtractorLink> {
        return links.sortedByDescending { Quality.detectFrom(it.quality).priority }
    }

    // --- Language Detection ---

    /**
     * Common language codes and names.
     */
    protected val languageMap = mapOf(
        "en" to "English",
        "es" to "Spanish",
        "español" to "Spanish",
        "fr" to "French",
        "français" to "French",
        "de" to "German",
        "deutsch" to "German",
        "it" to "Italian",
        "italiano" to "Italian",
        "pt" to "Portuguese",
        "português" to "Portuguese",
        "ja" to "Japanese",
        "日本語" to "Japanese",
        "ko" to "Korean",
        "한국어" to "Korean",
        "zh" to "Chinese",
        "中文" to "Chinese",
        "ar" to "Arabic",
        "العربية" to "Arabic",
        "hi" to "Hindi",
        "हिन्दी" to "Hindi",
        "ru" to "Russian",
        "русский" to "Russian",
        "tr" to "Turkish",
        "türkçe" to "Turkish",
        "th" to "Thai",
        "ไทย" to "Thai",
        "vi" to "Vietnamese",
        "tiếng việt" to "Vietnamese"
    )

    /**
     * Detects the language code from a string (filename, subtitle label, etc).
     */
    protected fun detectLanguage(text: String): String {
        val lower = text.lowercase()
        // Check for explicit language codes
        languageMap.keys.forEach { code ->
            if (lower.contains(code)) return code
        }
        // Check for full language names
        languageMap.entries.forEach { (code, name) ->
            if (lower.contains(name.lowercase())) return code
        }
        return "und" // undefined
    }

    // --- Image Helpers ---

    /**
     * Normalizes an image URL to ensure it has a protocol.
     */
    protected fun normalizeImage(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("//") -> "https:$trimmed"
            else -> "$mainUrl/${trimmed.trimStart('/')}"
        }
    }

    /**
     * Extracts the poster image from a document.
     */
    protected fun extractPoster(doc: Document): String? {
        return doc.selectFirst("meta[property=og:image]")
            ?.attr("content")
            ?.let { normalizeImage(it) }
            ?: doc.selectFirst(".poster img, .movie-poster img, .series-poster img, .cover img")
            ?.attr("src")
            ?.let { normalizeImage(it) }
    }

    /**
     * Extracts the banner/backdrop image from a document.
     */
    protected fun extractBanner(doc: Document): String? {
        return doc.selectFirst("meta[property=og:image:width]")
            ?.closest("meta[property=og:image]")
            ?.attr("content")
            ?.let { normalizeImage(it) }
            ?: doc.selectFirst(".backdrop img, .banner img, .fanart img, .background img")
            ?.attr("src")
            ?.let { normalizeImage(it) }
    }

    /**
     * Extracts the thumbnail image from a document.
     */
    protected fun extractThumbnail(doc: Document): String? {
        return doc.selectFirst("meta[property=og:image:alt]")
            ?.attr("content")
            ?.let { normalizeImage(it) }
            ?: doc.selectFirst("meta[name=twitter:image]")
            ?.attr("content")
            ?.let { normalizeImage(it) }
    }

    // --- Subtitle Detection ---

    /**
     * Subtitle format detection regex patterns.
     */
    private val subtitlePatterns = listOf(
        PatternInfo("\\.srt$", "SRT"),
        PatternInfo("\\.vtt$", "VTT"),
        PatternInfo("\\.ass$", "ASS"),
        PatternInfo("\\.ssa$", "SSA"),
        PatternInfo("\\.sub$", "SUB"),
        PatternInfo("\\.sbv$", "SBV"),
        PatternInfo("\\.dfxp$", "DFXP"),
        PatternInfo("\\.ttml$", "TTML"),
        PatternInfo("\\.webvtt$", "VTT")
    )

    data class PatternInfo(val pattern: String, val format: String)

    /**
     * Detects subtitle format from a URL or filename.
     */
    protected fun detectSubtitleFormat(url: String): String? {
        val lower = url.lowercase()
        return subtitlePatterns.firstOrNull { Regex(it.pattern).containsMatchIn(lower) }?.format
    }

    /**
     * Checks if a URL points to a subtitle file.
     */
    protected fun isSubtitleUrl(url: String): Boolean {
        return detectSubtitleFormat(url) != null
    }

    // --- Utility Methods ---

    /**
     * Cleans up a title string (removes extra whitespace, HTML entities, etc).
     */
    protected fun cleanTitle(title: String): String {
        return title
            .replace(Regex("\\s+"), " ")
            .trim()
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
    }

    /**
     * Parses a year from a string.
     */
    protected fun extractYear(text: String): Int? {
        return Regex("(19|20)\\d{2}").find(text)?.value?.toIntOrNull()
    }

    /**
     * Parses a duration from a string (e.g., "1h 30m", "90 min", "01:30:00").
     */
    protected fun extractDuration(text: String): Int? {
        // Try "Xh Ym" format
        val hmMatch = Regex("(\\d+)\\s*h\\s*(\\d+)?\\s*m?").find(text.lowercase())
        if (hmMatch != null) {
            val hours = hmMatch.groupValues[1].toIntOrNull() ?: 0
            val minutes = hmMatch.groupValues[2].toIntOrNull() ?: 0
            return hours * 60 + minutes
        }
        // Try "X min" or "Xm" format
        val minMatch = Regex("(\\d+)\\s*(?:min|m(?:in)?)(?:utes?)?").find(text.lowercase())
        if (minMatch != null) {
            return minMatch.groupValues[1].toIntOrNull()
        }
        // Try HH:MM:SS format
        val timeMatch = Regex("(\\d+):(\\d+):(\\d+)").find(text)
        if (timeMatch != null) {
            val hours = timeMatch.groupValues[1].toIntOrNull() ?: 0
            val minutes = timeMatch.groupValues[2].toIntOrNull() ?: 0
            return hours * 60 + minutes
        }
        // Try plain minutes
        val plainMinutes = text.trim().toIntOrNull()
        if (plainMinutes != null && plainMinutes > 0 && plainMinutes < 600) {
            return plainMinutes
        }
        return null
    }

    /**
     * Extracts rating from a string (e.g., "8.5/10", "85%", "8.5").
     */
    protected fun extractRating(text: String): Double? {
        val match = Regex("([\\d.]+)\\s*(?:/\\s*(?:10|100)|%)").find(text)
            ?: Regex("([\\d.]+)").find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()?.let { rating ->
            when {
                rating > 10 -> (rating / 10).coerceIn(0.0, 10.0)
                rating <= 1.0 && text.contains("%") -> (rating * 10).coerceIn(0.0, 10.0)
                else -> rating.coerceIn(0.0, 10.0)
            }
        }
    }

    /**
     * Extracts genres from a document.
     */
    protected fun extractGenres(doc: Document): List<String> {
        val genres = mutableSetOf<String>()
        doc.select(".genre a, .genres a, .genre-tag, .tag, [data-genre]").forEach {
            genres.add(cleanTitle(it.text()))
        }
        // Also check meta tags
        doc.select("meta[property=video:tag]").forEach {
            genres.add(it.attr("content"))
        }
        return genres.filter { it.isNotBlank() }
    }

    /**
     * Extracts actors/cast from a document.
     */
    protected fun extractActors(doc: Document): List<String> {
        val actors = mutableListOf<String>()
        doc.select(".actor a, .cast a, .person, [itemprop=actor] a, [data-actor]").forEach {
            val name = it.attr("title").ifBlank { it.text() }
            if (name.isNotBlank()) actors.add(cleanTitle(name))
        }
        return actors.distinct()
    }
}
