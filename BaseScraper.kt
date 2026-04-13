package com.streamflix.reborn.data.scraper

import com.streamflix.reborn.data.model.Movie

interface BaseScraper {
    suspend fun getHomeMovies(): List<Movie>
    // Se pueden añadir más métodos como getMovieDetails, getEpisodeList
}
