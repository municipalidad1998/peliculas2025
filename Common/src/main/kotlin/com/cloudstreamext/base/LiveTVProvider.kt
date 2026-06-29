package com.cloudstreamext.base

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink

/**
 * Base provider for live TV / IPTV sources.
 * Handles channel listing, categories, and live stream extraction.
 *
 * Subclasses must implement:
 * - [mainUrl]: Base URL of the source website
 * - [search]: Search functionality for channels
 * - [load]: Load channel details from a URL
 * - [loadLinks]: Extract live stream URLs for a channel
 */
abstract class LiveTVProvider : BaseProvider() {

    init {
        this.supportedTypes = setOf(TvType.Live)
    }

    /**
     * Channel category for organizing live TV channels.
     */
    data class ChannelCategory(
        val name: String,
        val channels: List<SearchResponse>
    )

    /**
     * Creates a live TV search response.
     */
    protected fun liveSearchResponse(
        name: String,
        url: String,
        poster: String? = null,
        isLive: Boolean = true
    ): SearchResponse {
        return newLiveStreamSearchResponse(name, url, TvType.Live) {
            this.posterUrl = poster
            this.isLive = isLive
        }
    }

    /**
     * Creates a live TV load response.
     */
    protected fun liveLoadResponse(
        name: String,
        url: String,
        poster: String? = null,
        background: String? = null,
        plot: String? = null,
        genres: List<String> = emptyList(),
        comingSoon: Boolean = false
    ): LoadResponse {
        return newLiveStreamLoadResponse(name, url, TvType.Live) {
            this.posterUrl = poster
            this.backgroundPosterUrl = background
            this.plot = plot
            this.tags = genres
            this.comingSoon = comingSoon
        }
    }

    /**
     * Builds a list of live channel search responses from a category page.
     */
    protected fun buildChannels(
        entries: List<ChannelEntry>
    ): List<SearchResponse> {
        return entries.map { entry ->
            liveSearchResponse(
                name = entry.name,
                url = entry.url,
                poster = entry.poster,
                isLive = entry.isLive
            )
        }
    }

    /**
     * Data class for building channel entries.
     */
    data class ChannelEntry(
        val name: String,
        val url: String,
        val poster: String? = null,
        val isLive: Boolean = true,
        val category: String? = null
    )

    /**
     * Creates a channel-based homepage with categories.
     * Override [fetchChannelCategories] to provide actual data.
     */
    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? {
        return try {
            val categories = fetchChannelCategories()
            val lists = categories.map { category ->
                newHomePageList(category.name, category.channels)
            }
            HomePageList("Live TV", lists)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fetches channel categories. Override to provide actual data.
     */
    open suspend fun fetchChannelCategories(): List<ChannelCategory> {
        return emptyList()
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
