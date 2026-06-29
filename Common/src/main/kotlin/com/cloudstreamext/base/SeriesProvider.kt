package com.cloudstreamext.base

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink

/**
 * Base provider for TV series sources.
 * Handles seasons, episodes, and series-specific metadata.
 *
 * Subclasses must implement:
 * - [mainUrl]: Base URL of the source website
 * - [search]: Search functionality
 * - [load]: Load series details (seasons, episodes) from a URL
 * - [loadLinks]: Extract video streams for an episode
 */
abstract class SeriesProvider : BaseProvider() {

    init {
        this.supportedTypes = setOf(TvType.TvSeries)
    }

    /**
     * Creates a TV series search response.
     */
    protected fun tvSearchResponse(
        title: String,
        url: String,
        poster: String? = null,
        year: Int? = null
    ): SearchResponse {
        return newTvSeriesSearchResponse(title, url, TvType.TvSeries) {
            this.posterUrl = poster
            this.year = year
        }
    }

    /**
     * Creates a TV series load response with seasons and episodes.
     */
    protected fun tvSeriesLoadResponse(
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
        comingSoon: Boolean = false
    ): LoadResponse {
        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.backgroundPosterUrl = background
            this.plot = plot
            this.year = year
            this.rating = rating
            this.tags = genres
            this.actors = actors
            this.recommendations = recommendations
            this.comingSoon = comingSoon
        }
    }

    /**
     * Helper to build an Episode object.
     */
    protected fun buildEpisode(
        name: String? = null,
        url: String,
        season: Int? = null,
        episode: Int? = null,
        poster: String? = null,
        duration: Int? = null,
        description: String? = null
    ): Episode {
        return Episode(
            name = name,
            url = url,
            season = season,
            episode = episode,
            poster = poster,
            duration = duration,
            description = description
        )
    }

    /**
     * Groups a flat list of episodes into seasons.
     */
    protected fun groupEpisodesBySeason(episodes: List<Episode>): Map<Int, List<Episode>> {
        return episodes.groupBy { it.season ?: 1 }
    }

    /**
     * Counts total episodes per season.
     */
    protected fun seasonEpisodeCount(episodes: List<Episode>, season: Int): Int {
        return episodes.count { (it.season ?: 1) == season }
    }

    /**
     * Default implementation for the main page.
     */
    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        val title = when (page) {
            1 -> "Popular Series"
            else -> "Latest Series"
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
