package com.cloudstreamext.util

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Utility class for extracting and normalizing image URLs from web pages.
 * Handles various image formats, lazy loading, and responsive images.
 *
 * Usage:
 * ```kotlin
 * val helper = ImageHelper()
 * val poster = helper.extractPoster(doc)
 * val images = helper.extractAllImages(doc)
 * ```
 */
class ImageHelper {

    /**
     * Image data class containing URL and metadata.
     */
    data class ImageInfo(
        val url: String,
        val alt: String = "",
        val width: Int = 0,
        val height: Int = 0,
        val type: ImageType = ImageType.UNKNOWN
    )

    /**
     * Image type classification.
     */
    enum class ImageType {
        POSTER,    // Portrait orientation (movie posters)
        BANNER,    // Landscape orientation (backdrops, banners)
        THUMBNAIL, // Small preview images
        THUMBNAIL_AUTO, // Auto-detected thumbnails
        UNKNOWN
    }

    companion object {
        /**
         * Normalizes an image URL to ensure it has a protocol.
         */
        fun normalizeUrl(url: String?, baseUrl: String = ""): String? {
            if (url.isNullOrBlank()) return null
            val trimmed = url.trim()
            return when {
                trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
                trimmed.startsWith("//") -> "https:$trimmed"
                baseUrl.isNotBlank() -> {
                    val base = baseUrl.trimEnd('/')
                    val path = trimmed.trimStart('/')
                    "$base/$path"
                }
                else -> trimmed
            }
        }

        /**
         * Gets the best quality image URL from a srcset attribute.
         */
        fun bestFromSrcset(srcset: String, preferredWidth: Int = 0): String? {
            if (srcset.isBlank()) return null
            val entries = srcset.split(",").mapNotNull { entry ->
                val parts = entry.trim().split(Regex("\\s+"))
                if (parts.size >= 2) {
                    val url = parts[0]
                    val descriptor = parts[1]
                    val width = when {
                        descriptor.endsWith("w") -> descriptor.dropLast(1).toIntOrNull() ?: 0
                        descriptor.endsWith("x") -> (descriptor.dropLast(1).toFloat() * 1000).toInt()
                        else -> 0
                    }
                    Pair(url, width)
                } else null
            }
            if (entries.isEmpty()) return null

            return if (preferredWidth > 0) {
                entries.minByOrNull { kotlin.math.abs(it.second - preferredWidth) }?.first
            } else {
                entries.maxByOrNull { it.second }?.first
            }
        }

        /**
         * Extracts the image URL from a picture element's sources.
         */
        fun bestFromPicture(pictureEl: org.jsoup.nodes.Element): String? {
            // Check source elements for best resolution
            val source = pictureEl.select("source")
                .sortedByDescending { it.attr("width").toIntOrNull() ?: 0 }
                .firstOrNull()
            if (source != null) {
                val srcset = source.attr("srcset")
                if (srcset.isNotBlank()) {
                    return bestFromSrcset(srcset, 1000)
                }
            }
            // Fallback to img src
            return pictureEl.selectFirst("img")?.attr("src")
        }
    }

    /**
     * Extracts the poster image from a document (portrait/vertical orientation preferred).
     */
    fun extractPoster(doc: Document, baseUrl: String = ""): String? {
        // Priority 1: Open Graph image
        val ogImage = doc.selectFirst("meta[property=og:image]")
            ?.attr("content")
        if (!ogImage.isNullOrBlank()) return normalizeUrl(ogImage, baseUrl)

        // Priority 2: Twitter image
        val twitterImage = doc.selectFirst("meta[name=twitter:image]")
            ?.attr("content")
        if (!twitterImage.isNullOrBlank()) return normalizeUrl(twitterImage, baseUrl)

        // Priority 3: Common poster class selectors
        val posterSelectors = listOf(
            ".poster img", ".movie-poster img", ".series-poster img",
            ".film-poster img", "[itemprop=image]", ".cover img",
            ".post img", ".card-poster img"
        )
        for (selector in posterSelectors) {
            val img = doc.selectFirst(selector)
            if (img != null) {
                val src = img.attr("src").ifBlank { img.attr("data-src") }
                if (src.isNotBlank()) return normalizeUrl(src, baseUrl)
            }
        }

        return null
    }

    /**
     * Extracts the banner/backdrop image from a document (landscape orientation preferred).
     */
    fun extractBanner(doc: Document, baseUrl: String = ""): String? {
        // Priority 1: Background image in meta tags
        val bgImage = doc.selectFirst("meta[property=og:image]")
            ?.closest("meta[property=og:image]")
            ?.attr("content")
        if (!bgImage.isNullOrBlank()) return normalizeUrl(bgImage, baseUrl)

        // Priority 2: Background/banner CSS
        val bannerSelectors = listOf(
            ".backdrop img", ".banner img", ".fanart img",
            ".background img", ".hero img", ".page-header img",
            ".cover-image img", ".movie-backdrop img"
        )
        for (selector in bannerSelectors) {
            val img = doc.selectFirst(selector)
            if (img != null) {
                val src = img.attr("src").ifBlank { img.attr("data-src") }
                if (src.isNotBlank()) return normalizeUrl(src, baseUrl)
            }
        }

        return null
    }

    /**
     * Extracts a thumbnail/preview image from a document.
     */
    fun extractThumbnail(doc: Document, baseUrl: String = ""): String? {
        // Priority 1: OG image alt
        val ogAlt = doc.selectFirst("meta[property=og:image:alt]")
            ?.attr("content")
        if (!ogAlt.isNullOrBlank() && ogAlt.startsWith("http")) return ogAlt

        // Priority 2: Thumbnail selectors
        val thumbSelectors = listOf(
            ".thumb img", ".thumbnail img", ".preview img",
            ".poster-mini img", ".cover-mini img", "img.thumb"
        )
        for (selector in thumbSelectors) {
            val img = doc.selectFirst(selector)
            if (img != null) {
                val src = img.attr("src").ifBlank { img.attr("data-src") }
                if (src.isNotBlank()) return normalizeUrl(src, baseUrl)
            }
        }

        return null
    }

    /**
     * Extracts all unique images from a document.
     */
    fun extractAllImages(doc: Document, baseUrl: String = ""): List<ImageInfo> {
        val images = mutableListOf<ImageInfo>()
        val seenUrls = mutableSetOf<String>()

        doc.select("img").forEach { img ->
            val src = img.attr("src").ifBlank {
                img.attr("data-src").ifBlank { img.attr("data-lazy-src") }
            }
            if (src.isNotBlank() && src !in seenUrls) {
                val normalized = normalizeUrl(src, baseUrl) ?: return@forEach
                seenUrls.add(normalized)

                val width = img.attr("width").toIntOrNull() ?: 0
                val height = img.attr("height").toIntOrNull() ?: 0
                val type = classifyImage(width, height)

                images.add(
                    ImageInfo(
                        url = normalized,
                        alt = img.attr("alt"),
                        width = width,
                        height = height,
                        type = type
                    )
                )
            }
        }

        // Also check background images in style attributes
        doc.select("[style*=background-image]").forEach { el ->
            val style = el.attr("style")
            val match = Regex("url\\(['\"]?([^'\")]+)['\"]?\\)").find(style)
            if (match != null) {
                val src = match.groupValues[1]
                val normalized = normalizeUrl(src, baseUrl) ?: return@forEach
                if (normalized !in seenUrls) {
                    seenUrls.add(normalized)
                    images.add(
                        ImageInfo(
                            url = normalized,
                            type = ImageType.UNKNOWN
                        )
                    )
                }
            }
        }

        return images
    }

    /**
     * Classifies an image type based on its dimensions.
     */
    private fun classifyImage(width: Int, height: Int): ImageType {
        if (width == 0 || height == 0) return ImageType.UNKNOWN
        val ratio = width.toFloat() / height.toFloat()
        return when {
            ratio < 0.8f -> ImageType.POSTER    // Portrait
            ratio > 1.5f -> ImageType.BANNER    // Landscape
            else -> ImageType.THUMBNAIL         // Square-ish
        }
    }

    /**
     * Adds image proxy parameters for resizing.
     * Useful for providers that support image resizing via URL parameters.
     */
    fun addResizeParams(url: String, width: Int, height: Int): String {
        if (!url.contains("?")) return "$url?w=$width&h=$height"
        return "$url&w=$width&h=$height"
    }

    /**
     * Removes common tracking parameters from image URLs.
     */
    fun cleanImageUrl(url: String): String {
        val paramsToRemove = listOf("utm_", "fbclid", "gclid", "ref", "spm")
        val cleanUrl = url
        var result = cleanUrl
        for (param in paramsToRemove) {
            result = result.replace(Regex("[?&]$param=[^&]*"), "")
            result = result.replace(Regex("&$param=[^&]*"), "")
        }
        return result
    }
}
