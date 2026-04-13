package com.streamflix.reborn.data.repository

import com.streamflix.reborn.data.model.Movie
import com.streamflix.reborn.data.scraper.DoramasYtScraper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MovieRepository(private val scraper: DoramasYtScraper) {
    fun getMovies(): Flow<List<Movie>> = flow {
        emit(scraper.getHomeMovies())
    }
}
