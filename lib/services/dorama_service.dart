import 'dart:convert';
import 'package:flutter/services.dart' show rootBundle;
import 'package:http/http.dart' as http;
import 'package:html/parser.dart' show parse;
import '../models/dorama.dart';

class DoramaService {
  List<Dorama> recentReleases = [];
  List<Dorama> allSeries = [];
  List<Dorama> allMovies = [];

  // Singleton
  static final DoramaService _instance = DoramaService._internal();
  factory DoramaService() => _instance;
  DoramaService._internal();

  Future<void> init() async {
    // 1. Carga Masiva desde Base de Datos Local Híbrida (mega_catalogo.json)
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
    } catch (e) {
      print("Error leyendo mega catalogo local: $e");
    }

    // 2. Extracción EN VIVO y Ligera (limitada a doramaexpress para no saturar al usuario)
    try {
      fetchLiveUpdates();
    } catch (e) {
      print("Scraping live error: $e");
    }
  }

  Future<void> fetchLiveUpdates() async {
    final response = await http.get(Uri.parse('https://doramaexpress.com/'));
    if (response.statusCode == 200) {
      var document = parse(response.body);
      var linkElements = document.querySelectorAll('a');
      for (var element in linkElements) {
        String title = element.text.trim();
        if (title.isEmpty) {
          title = element.attributes['title'] ?? '';
          if (title.isEmpty) {
            title = element.attributes['alt'] ?? '';
          }
        }
        
        String href = element.attributes['href'] ?? '';
        bool isIgnored = href.contains('facebook') || href.contains('instagram');
        
        if (title.length > 2 && !isIgnored && href.contains('doramaexpress.com')) {
          if (!recentReleases.any((d) => d.url == href)) {
             String category = href.contains('/movies/') ? 'movies' : 'serie';
             String coverUrl = '';
             var img = element.querySelector('img');
             if (img != null) {
                coverUrl = img.attributes['src'] ?? '';
             }
             
             recentReleases.add(Dorama(titulo: title, url: href, coverUrl: coverUrl.isNotEmpty ? coverUrl : null, category: category));
          }
        }
      }
    }
  }
}
