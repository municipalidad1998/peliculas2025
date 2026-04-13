package com.streamflix.reborn.data.scraper

import com.streamflix.reborn.data.model.Movie
import org.jsoup.Jsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DoramasYtScraper : BaseScraper {
    override suspend fun getHomeMovies(): List<Movie> = withContext(Dispatchers.IO) {
        val doc = Jsoup.connect("https://www.doramasyt.com/").get()
        val elements = doc.select("ul.row > li")
        
        elements.map { element ->
            val link = element.select("a").attr("href")
            val title = element.select("h3").text()
            val imageUrl = element.select("img.lazy").attr("data-src")
            
            Movie(title, link, imageUrl)
        }
    }
}
