package com.cloudstreamext.base

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink

/**
 * Base provider for movie-specific sources.
 * Handles the complete movie lifecycle: search, browse, load, extract.
 *
 * Subclasses must implement:
 * - [mainUrl]: Base URL of the source website
 * - [search]: Search functionality
 * - [load]: Load movie details from a URL
 * - [loadLinks]: Extract video streams from a movie page
 */
abstract class MovieProvider : BaseProvider() {

    init {
        // Default supported types for movie providers
        this.supportedTypes = setOf(TvType.Movie)
    }

    /**
     * Creates a movie search response.
     */
    protected fun movieSearchResponse(
        title: String,
        url: String,
        poster: String? = null,
        year: Int? = null
    ): SearchResponse {
        return newMovieSearchResponse(title, url, TvType.Movie) {
            this.posterUrl = poster
            this.year = year
        }
    }

    /**
     * Creates a movie load response with metadata.
     */
    protected fun movieLoadResponse(
        title: String,
        url: String,
        poster: String? = null,
        background: String? = null,
        plot: String? = null,
        year: Int? = null,
        duration: Int? = null,
        rating: Int? = null,
        genres: List<String> = emptyList(),
        actors: List<String> = emptyList(),
        recommendations: List<SearchResponse>? = null,
        comesFrom: String? = null
    ): LoadResponse {
        return newMovieLoadResponse(title, url, TvType.Movie) {
            this.posterUrl = poster
            this.backgroundPosterUrl = background
            this.plot = plot
            this.year = year
            this.duration = duration
            this.rating = rating
            this.tags = genres
            this.actors = actors
            this.recommendations = recommendations
        }
    }

    /**
     * Default implementation for the main page that loads popular/latest movies.
     * Subclasses should override [fetchMainPageContent] to provide actual data.
     */
    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        val title = when (page) {
            1 -> "Popular Movies"
            else -> "Latest Movies"
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
