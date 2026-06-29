package com.cloudstreamext.base

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.SubtitleHelper

/**
 * Base provider for anime sources.
 * Handles dub/sub detection, anime-specific metadata, and episode organization.
 *
 * Subclasses must implement:
 * - [mainUrl]: Base URL of the source website
 * - [search]: Search functionality
 * - [load]: Load anime details from a URL
 * - [loadLinks]: Extract video streams for an episode
 */
abstract class AnimeProvider : BaseProvider() {

    init {
        this.supportedTypes = setOf(TvType.Anime)
    }

    /**
     * Whether this provider supports dubbed content.
     * Override to enable dub detection.
     */
    open val supportsDub: Boolean = true

    /**
     * Creates an anime search response.
     */
    protected fun animeSearchResponse(
        title: String,
        url: String,
        poster: String? = null,
        year: Int? = null,
        isDub: Boolean = false
    ): SearchResponse {
        return newAnimeSearchResponse(title, url, TvType.Anime) {
            this.posterUrl = poster
            this.year = year
            addDubStatus(isDub, !isDub)
        }
    }

    /**
     * Creates an anime load response with episodes.
     */
    protected fun animeLoadResponse(
        title: String,
        url: String,
        episodes: List<Episode>,
        poster: String? = null,
        background: String? = null,
        plot: String? = null,
        year: Int? = null,
        rating: Int? = null,
        genres: List<String> = emptyList(),
        actors: List<String> = emptyList(),
        recommendations: List<SearchResponse>? = null,
        isAdult: Boolean = false,
        comingSoon: Boolean = false,
        dubStatus: DubStatus? = null
    ): LoadResponse {
        return newAnimeLoadResponse(title, url, TvType.Anime, episodes) {
            this.posterUrl = poster
            this.backgroundPosterUrl = background
            this.plot = plot
            this.year = year
            this.rating = rating
            this.tags = genres
            this.actors = actors
            this.recommendations = recommendations
            this.isAdult = isAdult
            this.comingSoon = comingSoon
            this.dubStatus = dubStatus
        }
    }

    /**
     * Detects if a video/source is dubbed based on filename or URL.
     */
    protected fun isDubbed(filename: String): Boolean {
        val lower = filename.lowercase()
        return lower.contains("dub") ||
                lower.contains("dubbed") ||
                lower.contains("español") ||
                lower.contains("latino") ||
                lower.contains("castellano")
    }

    /**
     * Detects if a video/source is subbed.
     */
    protected fun isSubbed(filename: String): Boolean {
        val lower = filename.lowercase()
        return lower.contains("sub") ||
                lower.contains("subbed") ||
                lower.contains("subs")
    }

    /**
     * Builds episodes with automatic dub/sub detection from the filename.
     */
    protected fun buildAnimeEpisodes(
        entries: List<AnimeEpisodeEntry>
    ): List<Episode> {
        return entries.map { entry ->
            Episode(
                name = entry.name,
                url = entry.url,
                season = entry.season,
                episode = entry.episode,
                poster = entry.poster,
                duration = entry.duration,
                description = entry.description
            )
        }
    }

    /**
     * Data class for building anime episodes.
     */
    data class AnimeEpisodeEntry(
        val name: String? = null,
        val url: String,
        val season: Int? = null,
        val episode: Int? = null,
        val poster: String? = null,
        val duration: Int? = null,
        val description: String? = null,
        val isDub: Boolean = false
    )

    /**
     * Creates subtitle files from common anime subtitle sources.
     */
    protected fun buildSubtitle(
        name: String,
        url: String,
        language: String = "und"
    ): SubtitleFile {
        val lang = if (language == "und") detectLanguage(name + url) else language
        return SubtitleFile(
            name = name,
            url = url,
            mimeType = getSubtitleMimeType(url),
            language = lang
        )
    }

    /**
     * Gets the MIME type for a subtitle file based on its URL.
     */
    private fun getSubtitleMimeType(url: String): String {
        val format = detectSubtitleFormat(url) ?: "SRT"
        return when (format) {
            "SRT" -> "application/x-subrip"
            "VTT" -> "text/vtt"
            "ASS", "SSA" -> "text/x-ssa"
            "SUB" -> "application/x-subviewer"
            else -> "application/x-subrip"
        }
    }

    /**
     * Default main page for anime providers.
     */
    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        val title = when (page) {
            1 -> "Popular Anime"
            else -> "Latest Anime"
        }
        return try {
            val items = fetchMainPageContent(page, request)
            newHomePageList(title, items)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fetches main page content. Override to provide data from the actual website.
     */
    open suspend fun fetchMainPageContent(
        page: Int,
        request: HomePageRequest
    ): List<SearchResponse> {
        return emptyList()
    }
}
