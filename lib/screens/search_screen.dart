import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import '../models/dorama.dart';
import '../widgets/dorama_card.dart';

class CloudSearchScreen extends StatefulWidget {
  final List<Dorama> allDoramas;

  const CloudSearchScreen({Key? key, required this.allDoramas}) : super(key: key);

  @override
  State<CloudSearchScreen> createState() => _CloudSearchScreenState();
}

class _CloudSearchScreenState extends State<CloudSearchScreen> {
  final TextEditingController _searchController = TextEditingController();
  bool _isGlobalSearch = true;
  String _query = '';

  final List<Map<String, dynamic>> _categories = [
    {"name": "Acción", "color": const Color(0xFFFF3B30)},
    {"name": "Action & Adventure", "color": const Color(0xFF8A2BE2)},
    {"name": "Animación", "color": const Color(0xFF1E90FF)},
    {"name": "Aventura", "color": const Color(0xFFFF1493)},
    {"name": "Bélica", "color": const Color(0xFFFF8C00)},
    {"name": "Ciencia ficción", "color": const Color(0xFF32CD32)},
    {"name": "Comedia", "color": const Color(0xFF000080)},
    {"name": "Crimen", "color": const Color(0xFFFF00FF)},
    {"name": "Documental", "color": const Color(0xFFDDA0DD)},
    {"name": "Drama", "color": const Color(0xFFFF4500)},
    {"name": "Familia", "color": const Color(0xFFDC143C)},
    {"name": "Fantasía", "color": const Color(0xFF808080)},
    {"name": "Historia", "color": const Color(0xFFFF69B4)},
    {"name": "Kids", "color": const Color(0xFF4682B4)},
    {"name": "Misterio", "color": const Color(0xFF008080)},
    {"name": "Música", "color": const Color(0xFF4682B4)},
    {"name": "News", "color": const Color(0xFFDAA520)},
    {"name": "Película de TV", "color": const Color(0xFFFF0000)},
  ];

  Future<List<String>> _getTmdbAliases(String q) async {
    if (q.trim().length < 3) return [];
    try {
      final apiKey = '15d2ea6d0dc1d476efbca3eba2b9bbfb';
      final response = await http.get(Uri.parse('https://api.themoviedb.org/3/search/multi?api_key=$apiKey&query=\${Uri.encodeComponent(q)}'));
      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final results = data['results'] as List<dynamic>;
        List<String> aliases = [];
        for (var r in results) {
          if (r['title'] != null) aliases.add(r['title'].toString().toLowerCase());
          if (r['original_title'] != null) aliases.add(r['original_title'].toString().toLowerCase());
          if (r['name'] != null) aliases.add(r['name'].toString().toLowerCase());
          if (r['original_name'] != null) aliases.add(r['original_name'].toString().toLowerCase());
        }
        return aliases;
      }
    } catch (_) {}
    return [];
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: const Color(0xFF0F1715),
      child: SafeArea(
        child: Column(
          children: [
            // Search Bar Area
            Container(
              margin: const EdgeInsets.all(16.0),
              decoration: BoxDecoration(
                color: const Color(0xFF1B2523),
                borderRadius: BorderRadius.circular(8),
              ),
              child: TextField(
                controller: _searchController,
                autofocus: false,
                style: const TextStyle(color: Colors.white, fontSize: 16),
                onChanged: (val) {
                  setState(() => _query = val);
                },
                decoration: InputDecoration(
                  hintText: 'Buscar película, serie',
                  hintStyle: const TextStyle(color: Colors.white54),
                  prefixIcon: const Icon(Icons.search, color: Colors.white54),
                  suffixIcon: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (_query.isNotEmpty)
                        IconButton(
                          icon: const Icon(Icons.close, color: Colors.white54),
                          onPressed: () {
                            _searchController.clear();
                            setState(() => _query = '');
                          },
                        ),
                      const IconButton(
                        icon: Icon(Icons.mic, color: Colors.white54),
                        onPressed: null,
                      ),
                    ],
                  ),
                  border: InputBorder.none,
                  contentPadding: const EdgeInsets.symmetric(vertical: 14),
                ),
              ),
            ),
            
            // Global search toggle
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    _query.isEmpty ? "Búsqueda global" : "Resultados para '\$_query'",
                    style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                  Switch(
                    value: _isGlobalSearch,
                    onChanged: (val) => setState(() => _isGlobalSearch = val),
                    activeColor: const Color(0xFF40E0D0),
                    activeTrackColor: const Color(0xFF1B3B36),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 10),

            // Main Content Area
            Expanded(
              child: _query.isEmpty ? _buildCategoryGrid() : _buildSearchResults(),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCategoryGrid() {
    return GridView.builder(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3,
        childAspectRatio: 2.2,
        crossAxisSpacing: 10,
        mainAxisSpacing: 10,
      ),
      itemCount: _categories.length,
      itemBuilder: (context, index) {
        final cat = _categories[index];
        return GestureDetector(
          onTap: () {
            _searchController.text = cat['name'];
            setState(() => _query = cat['name']);
          },
          child: Container(
            decoration: BoxDecoration(
              color: cat['color'],
              borderRadius: BorderRadius.circular(6),
            ),
            alignment: Alignment.center,
            child: Text(
              cat['name'],
              textAlign: TextAlign.center,
              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
            ),
          ),
        );
      },
    );
  }

  Widget _buildSearchResults() {
    return FutureBuilder<List<String>>(
      future: _getTmdbAliases(_query),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
           return const Center(child: CircularProgressIndicator(color: Color(0xFF40E0D0)));
        }

        final aliases = snapshot.data ?? [];
        final queryLower = _query.toLowerCase();

        final list = widget.allDoramas.where((d) {
          final titleLower = d.titulo.toLowerCase();
          if (titleLower.contains(queryLower)) return true;
          for (var alias in aliases) {
             if (alias.isNotEmpty && titleLower.contains(alias)) return true;
             if (alias.isNotEmpty && alias.contains(titleLower) && titleLower.length > 4) return true;
          }
          return false;
        }).toList();

        if (list.isEmpty) {
          return const Center(
             child: Text('No hay resultados.', style: TextStyle(color: Colors.white54, fontSize: 16)),
          );
        }

        return LayoutBuilder(
          builder: (context, constraints) {
            int columns = constraints.maxWidth ~/ 120;
            if (columns < 3) columns = 3;
            
            return GridView.builder(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: columns,
                childAspectRatio: 0.68,
                crossAxisSpacing: 12,
                mainAxisSpacing: 12,
              ),
              itemCount: list.length,
              itemBuilder: (context, index) {
                // To simulate CloudStream UI we wrap DoramaCard with a Tag
                return Stack(
                  fit: StackFit.expand,
                  children: [
                    DoramaCard(dorama: list[index], width: double.infinity, height: double.infinity),
                    Positioned(
                      top: 4,
                      right: 4,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: const Color(0xFF0F1715).withOpacity(0.8),
                          borderRadius: BorderRadius.circular(4),
                          border: Border.all(color: Colors.white38, width: 0.5),
                        ),
                        child: Text(
                          list[index].category == 'movies' ? 'Película' : 'Serie',
                          style: const TextStyle(color: Colors.white, fontSize: 10, fontWeight: FontWeight.bold),
                        ),
                      ),
                    )
                  ],
                );
              },
            );
          }
        );
      }
    );
  }
}
