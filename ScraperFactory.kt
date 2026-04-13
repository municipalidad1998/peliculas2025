package com.streamflix.reborn.data.scraper

object ScraperFactory {
    fun getScraper(url: String): BaseScraper {
        return when {
            url.contains("doramasyt") -> DoramasYtScraper()
            url.contains("doramaexpress") -> DoramaExpressScraper()
            url.contains("doramasmp4") -> DoramasMp4Scraper()
            url.contains("hitv") -> HiTvScraper()
            url.contains("doramasia") -> DoramasiaScraper()
            url.contains("youku") -> YoukuScraper()
            url.contains("pandrama") -> PanDramaScraper()
            url.contains("gnula") -> GnulaScraper()
            url.contains("betaseries") -> BetaSeriesScraper()
            url.contains("pelisflix") -> PelisFlixScraper()
            else -> throw IllegalArgumentException("No scraper found for $url")
        }
    }
}
