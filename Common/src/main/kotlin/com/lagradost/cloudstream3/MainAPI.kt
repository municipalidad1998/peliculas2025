package com.lagradost.cloudstream3

import com.lagradost.cloudstream3.utils.ExtractorLink

/**
 * Stub MainAPI class for CloudStream 3 extension compilation.
 * In production, this is provided by the CloudStream app itself.
 */
open class MainAPI {
    open var name: String = ""
    open var mainUrl: String = ""
    open var lang: String = "en"
    open var hasMainPage: Boolean = false
    open var usesHttps: Boolean = true
    open var supportedTypes: Set<TvType> = emptySet()

    open var defaultHeaders: MutableMap<String, String> = mutableMapOf()

    open suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageList? = null
    open suspend fun search(query: String): List<SearchResponse>? = null
    open suspend fun load(url: String): LoadResponse? = null
    open suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean = false

    companion object {
        fun newMovieSearchResponse(
            name: String,
            url: String,
            type: TvType? = TvType.Movie,
            initializer: MovieSearchResponse.() -> Unit = {}
        ): MovieSearchResponse = MovieSearchResponse(name, url, type).apply(initializer)

        fun newTvSeriesSearchResponse(
            name: String,
            url: String,
            type: TvType? = TvType.TvSeries,
            initializer: TvSeriesSearchResponse.() -> Unit = {}
        ): TvSeriesSearchResponse = TvSeriesSearchResponse(name, url, type).apply(initializer)

        fun newAnimeSearchResponse(
            name: String,
            url: String,
            type: TvType? = TvType.Anime,
            initializer: AnimeSearchResponse.() -> Unit = {}
        ): AnimeSearchResponse = AnimeSearchResponse(name, url, type).apply(initializer)

        fun newLiveStreamSearchResponse(
            name: String,
            url: String,
            type: TvType? = TvType.Live,
            initializer: LiveStreamSearchResponse.() -> Unit = {}
        ): LiveStreamSearchResponse = LiveStreamSearchResponse(name, url, type).apply(initializer)

        fun newMovieLoadResponse(
            name: String,
            url: String,
            type: TvType? = TvType.Movie,
            initializer: MovieLoadResponse.() -> Unit = {}
        ): MovieLoadResponse = MovieLoadResponse(name, url, type).apply(initializer)

        fun newTvSeriesLoadResponse(
            name: String,
            url: String,
            type: TvType? = TvType.TvSeries,
            episodes: List<Episode> = emptyList(),
            initializer: TvSeriesLoadResponse.() -> Unit = {}
        ): TvSeriesLoadResponse = TvSeriesLoadResponse(name, url, type, episodes).apply(initializer)

        fun newAnimeLoadResponse(
            name: String,
            url: String,
            type: TvType? = TvType.Anime,
            episodes: List<Episode> = emptyList(),
            initializer: AnimeLoadResponse.() -> Unit = {}
        ): AnimeLoadResponse = AnimeLoadResponse(name, url, type, episodes).apply(initializer)

        fun newLiveStreamLoadResponse(
            name: String,
            url: String,
            type: TvType? = TvType.Live,
            initializer: LiveStreamLoadResponse.() -> Unit = {}
        ): LiveStreamLoadResponse = LiveStreamLoadResponse(name, url, type).apply(initializer)

        fun newHomePageList(
            name: String,
            list: List<SearchResponse>,
            isHorizontalImages: Boolean = false
        ): HomePageList = HomePageList(name, listOf(HomePageListResponse(name, list, isHorizontalImages)))
    }
}
