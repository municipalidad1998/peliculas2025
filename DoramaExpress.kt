import com.laguna.lib.*
import com.laguna.lib.utils.*

class DoramaExpress : MainAPI() {
    override var name = "Dorama Express"
    override var mainUrl = "https://doramaexpress.com"
    override val lang = "es"
    override val hasMainPage = true
    override val hasSearch = true

    override suspend fun getMainPage(): HomePageResponse {
        val doc = app.get(mainUrl).document
        val home = ArrayList<HomePageList>()
        val featured = doc.select("article.item, div.post, div.entry")
        val featuredList = mutableListOf<TvShow>()

        featured.forEach { element ->
            val title = element.select("h3, h2, .title").text()
            val url = element.select("a").attr("href")
            val poster = element.select("img").attr("src")

            if (title.isNotBlank() && url.isNotBlank()) {
                featuredList.add(TvShow(title, url, posterUrl = poster))
            }
        }

        if (featuredList.isNotEmpty()) {
            home.add(HomePageList("Destacados", featuredList))
        }

        return HomePageResponse(home)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl?s="+query
        val doc = app.get(url).document
        val results = mutableListOf<SearchResponse>()

        doc.select("article.item, div.post, div.entry").forEach { element ->
            val title = element.select("h3, h2, .title").text()
            val url = element.select("a").attr("href")
            val poster = element.select("img").attr("src")

            if (title.isNotBlank() && url.isNotBlank()) {
                results.add(SearchResponse(title, url, posterUrl = poster))
            }
        }

        return results
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.select("h1, .title-serie").text()
        val poster = doc.select("img.cover, .poster img").attr("src")
        val description = doc.select(".description, .sinopsis").text()
        val episodes = mutableListOf<Episode>()

        doc.select(".episodios a, .capitulos a").forEachIndexed { index, link ->
            val epUrl = link.attr("href")
            val epName = link.text().ifEmpty { "Episodio ${index + 1}" }
            episodes.add(Episode(epName, epUrl))
        }

        return TvShowLoadResponse(title, url, posterUrl = poster, plot = description, episodes = episodes)
    }

    override suspend fun loadLinks(url: String): List<VideoLink> {
        val doc = app.get(url).document
        val links = mutableListOf<VideoLink>()
        val iframe = doc.select("iframe").attr("src")

        if (iframe.isNotBlank()) {
            links.add(VideoLink("Servidor 1", iframe, quality = "HD"))
        }

        doc.select("source, video source").forEach { source ->
            val src = source.attr("src")
            if (src.isNotBlank()) {
                links.add(VideoLink("Directo", src))
            }
        }

        return links
    }
}