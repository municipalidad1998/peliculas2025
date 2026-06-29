package com.lagradost.cloudstream3

/**
 * Stub SearchResponse class for CloudStream 3.
 */
open class SearchResponse(
    val name: String,
    val url: String,
    val type: TvType? = null,
    var posterUrl: String? = null,
    var year: Int? = null
) {
    var posterHeaders: Map<String, String>? = null
    var posterBackground: String? = null
}

class MovieSearchResponse(
    name: String,
    url: String,
    type: TvType? = TvType.Movie
) : SearchResponse(name, url, type)

class TvSeriesSearchResponse(
    name: String,
    url: String,
    type: TvType? = TvType.TvSeries
) : SearchResponse(name, url, type)

class AnimeSearchResponse(
    name: String,
    url: String,
    type: TvType? = TvType.Anime
) : SearchResponse(name, url, type) {
    fun addDubStatus(dub: Boolean, sub: Boolean) { /* stub */ }
}

class LiveStreamSearchResponse(
    name: String,
    url: String,
    type: TvType? = TvType.Live
) : SearchResponse(name, url, type) {
    var isLive: Boolean = true
}
