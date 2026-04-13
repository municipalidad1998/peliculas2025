package com.streamflix.reborn.data.scraper

import com.streamflix.reborn.data.model.Movie
import org.jsoup.Jsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// TODO: Implementar selectores CSS para cada sitio

class DoramaExpressScraper : BaseScraper { override suspend fun getHomeMovies(): List<Movie> = emptyList() }
class DoramasMp4Scraper : BaseScraper { override suspend fun getHomeMovies(): List<Movie> = emptyList() }
class HiTvScraper : BaseScraper { override suspend fun getHomeMovies(): List<Movie> = emptyList() }
class DoramasiaScraper : BaseScraper { override suspend fun getHomeMovies(): List<Movie> = emptyList() }
class YoukuScraper : BaseScraper { override suspend fun getHomeMovies(): List<Movie> = emptyList() }
class PanDramaScraper : BaseScraper { override suspend fun getHomeMovies(): List<Movie> = emptyList() }
class GnulaScraper : BaseScraper { override suspend fun getHomeMovies(): List<Movie> = emptyList() }
class BetaSeriesScraper : BaseScraper { override suspend fun getHomeMovies(): List<Movie> = emptyList() }
class PelisFlixScraper : BaseScraper { override suspend fun getHomeMovies(): List<Movie> = emptyList() }
