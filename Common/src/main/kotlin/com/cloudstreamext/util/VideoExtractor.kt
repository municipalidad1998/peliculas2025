package com.cloudstreamext.util

import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink
import okhttp3.OkHttpClient

/**
 * Utility class for extracting video streams from various hosting services.
 * Supports common extractors and can be extended with new ones.
 *
 * Usage:
 * ```kotlin
 * val extractor = VideoExtractor(client)
 * val links = extractor.extractFromUrl(videoUrl)
 * links.forEach { callback.invoke(it) }
 * ```
 */
class VideoExtractor(private val client: OkHttpClient) {

    /**
     * Video source data class containing stream information.
     */
    data class VideoSource(
        val url: String,
        val quality: String = "Unknown",
        val format: VideoFormat = VideoFormat.M3U8,
        val headers: Map<String, String> = emptyMap(),
        val subtitles: List<SubtitleFile> = emptyList(),
        val isHls: Boolean = false,
        val isDash: Boolean = false
    )

    /**
     * Supported video formats.
     */
    enum class VideoFormat(val mimeType: String) {
        MP4("video/mp4"),
        MKV("video/x-matroska"),
        M3U8("application/x-mpegURL"),
        MPD("application/dash+xml"),
        TS("video/mp2t"),
        FLV("video/x-flv"),
        WEBM("video/webm"),
        UNKNOWN("video/mp4")
    }

    companion object {
        /**
         * Detects video format from a URL.
         */
        fun detectFormat(url: String): VideoFormat {
            val lower = url.lowercase()
            return when {
                lower.contains(".m3u8") || lower.contains("hls") -> VideoFormat.M3U8
                lower.contains(".mpd") || lower.contains("dash") -> VideoFormat.MPD
                lower.contains(".mp4") -> VideoFormat.MP4
                lower.contains(".mkv") -> VideoFormat.MKV
                lower.contains(".ts") -> VideoFormat.TS
                lower.contains(".flv") -> VideoFormat.FLV
                lower.contains(".webm") -> VideoFormat.WEBM
                else -> VideoFormat.UNKNOWN
            }
        }

        /**
         * Detects quality from a URL or title string.
         */
        fun detectQuality(text: String): String {
            val lower = text.lowercase()
            return when {
                lower.contains("2160p") || lower.contains("4k") || lower.contains("uhd") -> "4K"
                lower.contains("1080p") || lower.contains("fhd") -> "1080p"
                lower.contains("720p") || lower.contains("hd") && !lower.contains("sd") -> "720p"
                lower.contains("480p") || lower.contains("sd") -> "480p"
                lower.contains("360p") -> "360p"
                lower.contains("240p") -> "240p"
                else -> "Unknown"
            }
        }
    }

    /**
     * Extracts video sources from a page URL using common patterns.
     * Searches for video URLs in the page HTML and JavaScript.
     */
    suspend fun extractFromPage(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): List<VideoSource> {
        val response = try {
            client.newCall(
                okhttp3.Request.Builder()
                    .url(url)
                    .apply {
                        headers.forEach { (k, v) -> addHeader(k, v) }
                    }
                    .build()
            ).execute()
        } catch (e: Exception) {
            return emptyList()
        }

        val body = response.body?.string() ?: return emptyList()
        return extractFromHtml(body)
    }

    /**
     * Extracts video sources from HTML/JavaScript content.
     */
    fun extractFromHtml(html: String): List<VideoSource> {
        val sources = mutableListOf<VideoSource>()

        // Pattern 1: Direct video URLs in HTML
        val directUrls = listOf(
            Regex("\"(https?://[^\"]*\\.mp4[^\"]*)\""),
            Regex("'(https?://[^']*\\.mp4[^']*)'"),
            Regex("src[\"':\\s]+(https?://[^\"'\\s]*\\.mp4)"),
            Regex("source[\"':\\s]+(https?://[^\"'\\s]*\\.mp4)")
        )

        for (pattern in directUrls) {
            pattern.findAll(html).forEach { match ->
                val url = match.groupValues[1]
                if (url.isNotBlank() && !url.contains("example.com")) {
                    sources.add(
                        VideoSource(
                            url = url,
                            quality = detectQuality(url),
                            format = VideoFormat.MP4
                        )
                    )
                }
            }
        }

        // Pattern 2: HLS streams
        val hlsPatterns = listOf(
            Regex("\"(https?://[^\"]*\\.m3u8[^\"]*)\""),
            Regex("'(https?://[^']*\\.m3u8[^']*)'"),
            Regex("file[\"':\\s]+(https?://[^\"'\\s]*\\.m3u8)")
        )

        for (pattern in hlsPatterns) {
            pattern.findAll(html).forEach { match ->
                val url = match.groupValues[1]
                if (url.isNotBlank()) {
                    sources.add(
                        VideoSource(
                            url = url,
                            quality = detectQuality(url),
                            format = VideoFormat.M3U8,
                            isHls = true
                        )
                    )
                }
            }
        }

        // Pattern 3: DASH/MPD streams
        val dashPatterns = listOf(
            Regex("\"(https?://[^\"]*\\.mpd[^\"]*)\""),
            Regex("'(https?://[^']*\\.mpd[^']*)'"),
            Regex("url[\"':\\s]+(https?://[^\"'\\s]*\\.mpd)")
        )

        for (pattern in dashPatterns) {
            pattern.findAll(html).forEach { match ->
                val url = match.groupValues[1]
                if (url.isNotBlank()) {
                    sources.add(
                        VideoSource(
                            url = url,
                            quality = detectQuality(url),
                            format = VideoFormat.MPD,
                            isDash = true
                        )
                    )
                }
            }
        }

        // Pattern 4: Embedded player iframes
        val iframePattern = Regex("<iframe[^>]*src=[\"']([^\"']*)[\"'][^>]*>")
        iframePattern.findAll(html).forEach { match ->
            val iframeUrl = match.groupValues[1]
            if (iframeUrl.isNotBlank() && !iframeUrl.contains("youtube.com") && !iframeUrl.contains("dailymotion.com")) {
                // Note: For actual implementations, you would recursively extract from iframes
                sources.add(
                    VideoSource(
                        url = iframeUrl,
                        quality = "Unknown",
                        format = VideoFormat.UNKNOWN
                    )
                )
            }
        }

        return sources.distinctBy { it.url }
    }

    /**
     * Converts a VideoSource to an ExtractorLink for CloudStream.
     */
    fun VideoSource.toExtractorLink(
        sourceName: String,
        referer: String,
        sourceData: String = ""
    ): ExtractorLink {
        return ExtractorLink(
            source = sourceName,
            name = sourceName,
            url = this.url,
            referer = referer,
            quality = this.quality,
            isM3u8 = this.isHls,
            headers = this.headers,
            extractorData = sourceData
        )
    }

    /**
     * Converts a list of VideoSources to ExtractorLinks.
     */
    fun List<VideoSource>.toExtractorLinks(
        sourceName: String,
        referer: String,
        sourceData: String = ""
    ): List<ExtractorLink> {
        return map { it.toExtractorLink(sourceName, referer, sourceData) }
    }
}
