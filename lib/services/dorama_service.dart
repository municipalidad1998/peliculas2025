import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'package:html/parser.dart' as html;
import '../models/dorama.dart';

class DoramaService {
  List<Dorama> allSeries = [];
  List<Dorama> allMovies = [];
  List<Dorama> recentReleases = []; // Novedades capturadas en vivo

  Future<void> init() async {
    // 1. Cargar el catálogo masivo y ultra-rápido offline
    await loadInitialData();
    // 2. Conectarse a la web para jalar lo último subido
    await fetchLiveUpdates();
  }

  Future<void> loadInitialData() async {
    try {
      final String response = await rootBundle.loadString('assets/doramaexpress_completo.json');
      final data = await json.decode(response);
      
      final seriesData = data['series'] as List;
      final moviesData = data['peliculas'] as List;
      
      allSeries = seriesData.map((e) => Dorama.fromJson(e, 'serie')).toList();
      allMovies = moviesData.map((e) => Dorama.fromJson(e, 'movies')).toList();
    } catch (e) {
      print("Error cargando JSON local: $e");
    }
  }

  Future<void> fetchLiveUpdates() async {
    try {
      final response = await http.get(Uri.parse('https://doramaexpress.com/'));
      if (response.statusCode == 200) {
        var document = html.parse(response.body);
        var links = document.querySelectorAll('a[href]');
        
        for (var link in links) {
          String href = link.attributes['href'] ?? '';
          
          if (href.contains('/serie/') || href.contains('/movies/') || href.contains('/episodio/')) {
             if (href.contains('/page/')) continue;
             
             String title = link.attributes['title'] ?? '';
             String coverUrl = '';
             
             var img = link.querySelector('img');
             if (img != null) {
               if (title.isEmpty) title = img.attributes['alt'] ?? '';
               coverUrl = img.attributes['src'] ?? '';
             }
             
             if (title.isEmpty) {
               title = link.text.trim();
             }
             
             // Extracción inteligente filtrando basura
             var ignorar = ['Ver ahora', 'View All', 'Whatsapp', 'Twitter', 'Ver más', 'Facebook'];
             bool containsBannedWord = ignorar.any((word) => title.contains(word));

             if (title.length > 2 && !containsBannedWord) {
                // Solo si no existe ya en nuestros recientes (para no repetir el carrusel superior)
                if (!recentReleases.any((d) => d.url == href)) {
                   String category = href.contains('/movies/') ? 'movies' : 'serie';
                   recentReleases.add(Dorama(
                     titulo: title, 
                     url: href, 
                     coverUrl: coverUrl.isNotEmpty ? coverUrl : null, 
                     category: category
                   ));
                }
             }
          }
        }
      }
    } catch (e) {
      print("Error conectando al scraper en vivo: $e");
    }
  }
}
