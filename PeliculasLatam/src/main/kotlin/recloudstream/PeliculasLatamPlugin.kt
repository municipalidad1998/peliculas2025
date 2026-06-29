package recloudstream

import com.lagradost.cloudstream3.plugins.BasePlugin
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin

@CloudstreamPlugin
class PeliculasLatamPlugin : BasePlugin() {
    override fun load() {
        // Anime providers
        registerMainAPI(recloudstream.anime.JKAnime())
        registerMainAPI(recloudstream.anime.TioAnime())
        registerMainAPI(recloudstream.anime.Latanime())
        registerMainAPI(recloudstream.anime.AnimeJara())
        registerMainAPI(recloudstream.anime.JKAnimeFlv())
        registerMainAPI(recloudstream.anime.VerAnimes())
        registerMainAPI(recloudstream.anime.AnimeAV1())
        registerMainAPI(recloudstream.anime.EstrenosAnime())
        registerMainAPI(recloudstream.anime.MundoDonghua())

        // Movie/Series providers
        registerMainAPI(recloudstream.movies.Pelispedia())
        registerMainAPI(recloudstream.movies.PelispediaMov())
        registerMainAPI(recloudstream.movies.LaMovie())
        registerMainAPI(recloudstream.movies.DeTodoPeliculas())
        registerMainAPI(recloudstream.movies.SoloLatino())
        registerMainAPI(recloudstream.movies.Aether())

        // Doramas providers
        registerMainAPI(recloudstream.doramas.DoramasFlix())
        registerMainAPI(recloudstream.doramas.DoraMasyT())

        // Novelas providers
        registerMainAPI(recloudstream.novelas.TlNovelas())

        // Live TV providers
        registerMainAPI(recloudstream.live.StreamXHD())
        registerMainAPI(recloudstream.live.FutbolLibre())
    }
}
