import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import '../models/dorama.dart';
import '../screens/info_screen.dart';

final Map<String, String> tmdbCache = {};

class DoramaCard extends StatefulWidget {
  final Dorama dorama;
  final double width;
  final double height;

  const DoramaCard({
    Key? key,
    required this.dorama,
    this.width = 120,
    this.height = 180,
  }) : super(key: key);

  @override
  State<DoramaCard> createState() => _DoramaCardState();
}

class _DoramaCardState extends State<DoramaCard> {
  String? _finalCover;
  bool _isFetching = false;

  @override
  void initState() {
    super.initState();
    _resolveCover();
  }

  Future<void> _resolveCover() async {
    // 1. Caché TMDB directo
    if (tmdbCache.containsKey(widget.dorama.titulo)) {
      setState(() => _finalCover = tmdbCache[widget.dorama.titulo]);
      return;
    }

    if (_isFetching) return;
    _isFetching = true;

    // 2. Intentar TMDB API para Pósteres en 4K (Prioridad Máxima)
    bool tmdbSuccess = false;
    try {
      final cleanTitle = widget.dorama.titulo.replaceAll(RegExp(r'\[.*?\]'), '').trim();
      final query = Uri.encodeComponent(cleanTitle);
      final apiKey = '15d2ea6d0dc1d476efbca3eba2b9bbfb';
      
      final response = await http.get(Uri.parse(
          'https://api.themoviedb.org/3/search/multi?api_key=$apiKey&language=es-MX&query=$query'
      ));
      
      if (response.statusCode == 200) {
        final Map<String, dynamic> data = json.decode(response.body);
        final List<dynamic> results = data['results'] ?? [];
        
        if (results.isNotEmpty) {
          final firstResult = results.firstWhere((r) => r['poster_path'] != null, orElse: () => results[0]);
          if (firstResult['poster_path'] != null) {
            final imageUrl = 'https://image.tmdb.org/t/p/w500${firstResult['poster_path']}';
            tmdbCache[widget.dorama.titulo] = imageUrl;
            if (mounted) setState(() => _finalCover = imageUrl);
            tmdbSuccess = true;
          }
        }
      }
    } catch (e) { }

    // 3. Fallback A: URL pre-agarrada del scraper (mega_catalogo)
    if (!tmdbSuccess && widget.dorama.coverUrl != null && widget.dorama.coverUrl!.isNotEmpty) {
      String cover = widget.dorama.coverUrl!;
      if (cover.startsWith('//')) cover = 'https:$cover';
      else if (cover.startsWith('/')) cover = 'https://doramaexpress.com$cover';
      if (mounted) setState(() => _finalCover = cover);
      tmdbSuccess = true;
    }

    // 4. Extractor Definitivo En Vivo: Si la portada sigue vacía, raspar la página exacta del Dorama (og:image)
    if (!tmdbSuccess) {
       try {
          final response = await http.get(Uri.parse(widget.dorama.url));
          if (response.statusCode == 200) {
             final docStr = response.body;
             // Buscar un og:image a lo bruto
             final ogMatch = RegExp(r'property="og:image" content="(.*?)"').firstMatch(docStr);
             if (ogMatch != null && ogMatch.group(1) != null) {
                if (mounted) setState(() => _finalCover = ogMatch.group(1));
             } else {
                // Alternativa: Primera imagen grande de la página (común en WordPress)
                final imgMatch = RegExp(r'<img[^>]+class="[^"]*wp-post-image[^"]*"[^>]+src="([^"]+)"').firstMatch(docStr);
                if (imgMatch != null && imgMatch.group(1) != null) {
                   if (mounted) setState(() => _finalCover = imgMatch.group(1));
                }
             }
          }
       } catch (e) { }
    }

    _isFetching = false;
  }

  void _openInfo() {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => InfoScreen(
          dorama: widget.dorama,
          initialCover: _finalCover,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: _openInfo,
      child: Container(
        width: widget.width,
        height: widget.height,
        margin: const EdgeInsets.only(right: 8),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: Stack(
            fit: StackFit.expand,
            children: [
              if (_finalCover != null && _finalCover!.isNotEmpty)
                Image.network(
                  _finalCover!,
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) => _buildPlaceholder(),
                )
              else
                _buildPlaceholder(),
              
              Container(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [Colors.black.withOpacity(0.9), Colors.transparent],
                    begin: Alignment.bottomCenter,
                    end: Alignment.topCenter,
                  ),
                ),
              ),
              
              Positioned(
                bottom: 8,
                left: 8,
                right: 8,
                child: Text(
                  widget.dorama.titulo,
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                    fontSize: 12,
                  ),
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPlaceholder() {
    return Container(
      decoration: BoxDecoration(color: Colors.grey[900]),
      child: Center(
        child: Icon(Icons.movie_creation_outlined, color: Colors.grey[800], size: 50),
      ),
    );
  }
}
