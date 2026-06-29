package com.lagradost.cloudstream3

/**
 * Stub LoadResponse class for CloudStream 3.
 */
open class LoadResponse(
    val name: String,
    val url: String,
    val type: TvType? = null
) {
    var posterUrl: String? = null
    var backgroundPosterUrl: String? = null
    var plot: String? = null
    var year: Int? = null
    var duration: Int? = null
    var rating: Int? = null
    var tags: List<String> = emptyList()
    var actors: List<String> = emptyList()
    var recommendations: List<SearchResponse>? = null
    var comingSoon: Boolean = false
    var isAdult: Boolean = false
    var dubStatus: DubStatus? = null
}

class MovieLoadResponse(
    name: String,
    url: String,
    type: TvType? = TvType.Movie
) : LoadResponse(name, url, type)

class TvSeriesLoadResponse(
    name: String,
    url: String,
    type: TvType? = TvType.TvSeries,
    val episodes: List<Episode> = emptyList()
) : LoadResponse(name, url, type)

class AnimeLoadResponse(
    name: String,
    url: String,
    type: TvType? = TvType.Anime,
    val episodes: List<Episode> = emptyList()
) : LoadResponse(name, url, type)

class LiveStreamLoadResponse(
    name: String,
    url: String,
    type: TvType? = TvType.Live
) : LoadResponse(name, url, type)
