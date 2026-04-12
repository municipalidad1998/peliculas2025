import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import '../models/dorama.dart';
import 'player_screen.dart';

class InfoScreen extends StatefulWidget {
  final Dorama dorama;
  final String? initialCover;

  const InfoScreen({Key? key, required this.dorama, this.initialCover}) : super(key: key);

  @override
  State<InfoScreen> createState() => _InfoScreenState();
}

class _InfoScreenState extends State<InfoScreen> {
  bool _isLoading = true;
  String _synopsis = 'Conectando con la base de datos TMDB (The Movie Database)...';
  String _rating = '';
  String? _highResCover;
  String? _backdrop;

  @override
  void initState() {
    super.initState();
    _highResCover = widget.initialCover;
    _fetchTmdbData();
  }

  Future<void> _fetchTmdbData() async {
    try {
      final cleanTitle = widget.dorama.titulo.replaceAll(RegExp(r'\[.*?\]'), '').trim();
      final query = Uri.encodeComponent(cleanTitle);
      final apiKey = '15d2ea6d0dc1d476efbca3eba2b9bbfb'; // Clave pública segura TMDB
      
      final response = await http.get(Uri.parse(
          'https://api.themoviedb.org/3/search/multi?api_key=$apiKey&language=es-MX&query=$query'
      ));
      
      if (response.statusCode == 200) {
        final Map<String, dynamic> data = json.decode(response.body);
        final List<dynamic> results = data['results'] ?? [];
        
        if (results.isNotEmpty) {
          // Buscamos el primero que tenga sentido o el primero directo
          final show = results.firstWhere((r) => r['poster_path'] != null, orElse: () => results[0]);
          
          if (mounted) {
            setState(() {
              final overview = show['overview'] as String?;
              _synopsis = (overview != null && overview.isNotEmpty) 
                  ? overview 
                  : 'Sin sinopsis disponible en español en TMDB para esta serie dramática.';
                  
              if (show['vote_average'] != null && show['vote_average'] > 0) {
                _rating = (show['vote_average'] as num).toStringAsFixed(1) + ' ⭐ (TMDB)';
              }
              
              if (show['poster_path'] != null) {
                _highResCover = 'https://image.tmdb.org/t/p/w780${show['poster_path']}';
              }
              if (show['backdrop_path'] != null) {
                _backdrop = 'https://image.tmdb.org/t/p/w1280${show['backdrop_path']}';
              } else {
                _backdrop = _highResCover;
              }
              
              _isLoading = false;
            });
          }
          return;
        }
      }
    } catch (e) {
      // Ignorar
    }

    if (mounted) {
      setState(() {
        _synopsis = 'Lo sentimos, la información detallada no se encontró en la gran base de datos de TMDB (The Movie Database).';
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: CustomScrollView(
        slivers: [
          SliverAppBar(
            expandedHeight: 400.0,
            pinned: true,
            backgroundColor: Colors.black,
            flexibleSpace: FlexibleSpaceBar(
              background: Stack(
                fit: StackFit.expand,
                children: [
                  if (_backdrop != null || _highResCover != null)
                    Image.network(
                      _backdrop ?? _highResCover!,
                      fit: BoxFit.cover,
                      alignment: Alignment.topCenter,
                    )
                  else
                    Container(color: Colors.grey[900]),
                  
                  // Gradiente oscuro
                  Container(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        colors: [Colors.black, Colors.transparent],
                        begin: Alignment.bottomCenter,
                        end: Alignment.center,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    widget.dorama.titulo,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 28,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  
                  if (_rating.isNotEmpty) ...[
                    Row(
                      children: [
                        Text(
                          _rating,
                          style: const TextStyle(color: Colors.green, fontWeight: FontWeight.bold, fontSize: 16),
                        ),
                        const SizedBox(width: 16),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(
                            border: Border.all(color: Colors.grey[600]!),
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: Text('TV-MA', style: TextStyle(color: Colors.grey[400], fontSize: 12)),
                        ),
                        const SizedBox(width: 12),
                        Text('HD', style: TextStyle(color: Colors.grey[400], fontWeight: FontWeight.bold)),
                      ],
                    ),
                    const SizedBox(height: 16),
                  ],

                  SizedBox(
                    width: double.infinity,
                    height: 50,
                    child: ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.white,
                        foregroundColor: Colors.black,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(4),
                        )
                      ),
                      icon: const Icon(Icons.play_arrow, size: 30),
                      label: const Text('Reproducir en Doramflix', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                      onPressed: () {
                        Navigator.of(context).push(
                          MaterialPageRoute(
                            builder: (context) => PlayerScreen(
                              title: widget.dorama.titulo,
                              url: widget.dorama.url,
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                  const SizedBox(height: 20),

                  if (_isLoading)
                    const Center(child: CircularProgressIndicator(color: Colors.red))
                  else
                    Text(
                      _synopsis,
                      style: const TextStyle(color: Colors.white, fontSize: 16, height: 1.5),
                    ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
