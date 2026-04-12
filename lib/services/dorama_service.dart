import 'dart:convert';
import 'package:flutter/services.dart' show rootBundle;
import 'package:http/http.dart' as http;
import 'package:html/parser.dart' show parse;
import '../models/dorama.dart';

class DoramaService {
  List<Dorama> recentReleases = [];
  List<Dorama> allSeries = [];
  List<Dorama> allMovies = [];
  
  Function? onDataUpdate;

  // Singleton
  static final DoramaService _instance = DoramaService._internal();
  factory DoramaService() => _instance;
  DoramaService._internal();

  final List<String> providers = [
    "https://www.doramasyt.com/doramas",
    "https://doramasia.com/doramas/",
    "https://doramasmp4.io/doramas",
    "https://pelisflix200.org/series/",
    "https://pelisflix200.org/peliculas/",
    "https://papayaseries.com/series/",
    "https://doramaexpress.com"
  ];

  Future<void> init() async {
    // 1. Carga Caché (mega_catalogo.json) de forma inmediata
    try {
      final String response = await rootBundle.loadString('assets/mega_catalogo.json');
      final data = await json.decode(response);
      
      final seriesList = (data['series'] as List?) ?? [];
      allSeries = seriesList.map((m) {
        return Dorama(
          titulo: m['titulo'] ?? 'Desconocido', 
          url: m['url'] ?? '', 
          coverUrl: m['coverUrl'], 
          category: m['category'] ?? 'serie'
        );
      }).toList();

      final moviesList = (data['movies'] as List?) ?? [];
      allMovies = moviesList.map((m) {
        return Dorama(
          titulo: m['titulo'] ?? 'Desconocido', 
          url: m['url'] ?? '', 
          coverUrl: m['coverUrl'], 
          category: m['category'] ?? 'movies'
        );
      }).toList();
      
      recentReleases = allSeries.take(5).toList();
    } catch (e) {
      print("Aviso: No hay caché local.");
    }

    // 2. Extracción EN VIVO Streamflix-like mode
    _startLiveProviders();
  }

  void _startLiveProviders() {
    for (var url in providers) {
       _scrapeProviderNative(url);
    }
  }

  Future<void> _scrapeProviderNative(String sourceUrl) async {
    try {
      final defaultHeaders = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      };
      
      http.Response response;
      try {
         response = await http.get(Uri.parse(sourceUrl), headers: defaultHeaders).timeout(const Duration(seconds: 15));
      } catch (e) {
         return; 
      }
      
      if (response.statusCode == 403 || response.statusCode == 503) {
         // Auto Cloudflare bypass fallback (como se usaba allorigins)
         response = await http.get(Uri.parse('https://api.allorigins.win/get?url=\${Uri.encodeComponent(sourceUrl)}')).timeout(const Duration(seconds: 15));
         if (response.statusCode == 200) {
            final proxyData = json.decode(response.body);
            response = http.Response(proxyData['contents'], 200);
         }
      }

      if (response.statusCode == 200) {
        final document = parse(response.body);
        final linkElements = document.querySelectorAll('a');
        
        bool appended = false;
        
        for (var element in linkElements) {
          String href = element.attributes['href'] ?? '';
          if (href.isEmpty) continue;
          
          if (!href.startsWith('http')) {
             if (href.startsWith('/')) {
                final base = Uri.parse(sourceUrl);
                href = '\${base.scheme}://\${base.host}$href';
             } else {
                continue;
             }
          }

          final isValidBase = ['/serie/', '/pelicula/', '/drama/', '/dorama/', '/ver/', '/peliculas/'].any((k) => href.contains(k));
          final isNotSingleEp = !href.contains('-capitulo-') && !href.contains('-episodio-');
          final isNotSocial = !href.contains('facebook') && !href.contains('instagram');
          
          if (isValidBase && isNotSingleEp && isNotSocial) {
             String title = element.attributes['title'] ?? '';
             String coverUrl = '';
             var img = element.querySelector('img');
             if (img != null) {
                if (title.isEmpty) title = img.attributes['alt'] ?? '';
                coverUrl = img.attributes['src'] ?? '';
             }
             if (title.isEmpty) title = element.text.trim();
             
             if (title.length > 3 && !title.toLowerCase().contains("capitulo")) {
                 String category = (href.contains('pelicula') || href.contains('movies')) ? 'movies' : 'serie';
                 
                 final dorama = Dorama(titulo: title, url: href, coverUrl: coverUrl.isNotEmpty ? coverUrl : null, category: category);
                 
                 // Deduplicador en vivo
                 if (category == 'serie') {
                    if (!allSeries.any((d) => d.titulo.toLowerCase() == title.toLowerCase() || d.url == href)) {
                        allSeries.insert(0, dorama); // Recientes primero
                        if (allSeries.length > 500) allSeries.removeLast();
                        appended = true;
                    }
                 } else {
                    if (!allMovies.any((d) => d.titulo.toLowerCase() == title.toLowerCase() || d.url == href)) {
                        allMovies.insert(0, dorama);
                        if (allMovies.length > 500) allMovies.removeLast();
                        appended = true;
                    }
                 }
                 
                 if (!recentReleases.any((d) => d.titulo.toLowerCase() == title.toLowerCase())) {
                    if (appended && recentReleases.length < 15) {
                       recentReleases.insert(0, dorama);
                    }
                 }
             }
          }
        }
        
        if (appended && onDataUpdate != null) {
           onDataUpdate!(); // Refresh UI as stream arrives
        }
      }
    } catch (e) {
      print("Provider Error [\$sourceUrl]: $e");
    }
  }
}
