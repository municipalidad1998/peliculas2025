import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import '../models/dorama.dart';
import '../widgets/dorama_card.dart';

class DoramaSearchDelegate extends SearchDelegate {
  final List<Dorama> allDoramas;

  DoramaSearchDelegate(this.allDoramas);

  @override
  String get searchFieldLabel => 'Buscar serie o película...';

  @override
  ThemeData appBarTheme(BuildContext context) {
    return ThemeData.dark().copyWith(
      appBarTheme: const AppBarTheme(
        backgroundColor: Colors.black,
        elevation: 0,
      ),
      scaffoldBackgroundColor: Colors.black,
      inputDecorationTheme: const InputDecorationTheme(
        border: InputBorder.none,
        hintStyle: TextStyle(color: Colors.white54),
      ),
    );
  }

  @override
  List<Widget> buildActions(BuildContext context) {
    return [
      if (query.isNotEmpty)
        IconButton(
          icon: const Icon(Icons.clear, color: Colors.white),
          onPressed: () {
            query = '';
          },
        )
    ];
  }

  @override
  Widget buildLeading(BuildContext context) {
    return IconButton(
      icon: const Icon(Icons.arrow_back, color: Colors.white),
      onPressed: () => close(context, null),
    );
  }

  @override
  Widget buildResults(BuildContext context) => _buildAdvancedList();

  @override
  Widget buildSuggestions(BuildContext context) => _buildAdvancedList();

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

  Widget _buildAdvancedList() {
    if (query.isEmpty) {
      return const Center(
        child: Text('Escribe para buscar (en Inglés o Español)...', style: TextStyle(color: Colors.white54, fontSize: 18)),
      );
    }

    return FutureBuilder<List<String>>(
      future: _getTmdbAliases(query),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
           return const Center(child: CircularProgressIndicator(color: Colors.purpleAccent));
        }

        final aliases = snapshot.data ?? [];
        final queryLower = query.toLowerCase();

        final list = allDoramas.where((d) {
          final titleLower = d.titulo.toLowerCase();
          if (titleLower.contains(queryLower)) return true;
          // Si no hace match directo, vemos si hace match con algún alias de TMDB en inglés u otro idioma
          for (var alias in aliases) {
             if (alias.isNotEmpty && titleLower.contains(alias)) return true;
             if (alias.isNotEmpty && alias.contains(titleLower) && titleLower.length > 4) return true;
          }
          return false;
        }).toList();

        if (list.isEmpty) {
          return const Center(
             child: Text('No se encontró contenido para esa búsqueda.', style: TextStyle(color: Colors.white, fontSize: 18)),
          );
        }

        return LayoutBuilder(
          builder: (context, constraints) {
            int columns = constraints.maxWidth ~/ 120;
            if (columns < 2) columns = 2;
            
            return GridView.builder(
              padding: const EdgeInsets.all(16),
              gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
                crossAxisCount: columns,
                childAspectRatio: 0.65,
                crossAxisSpacing: 12,
                mainAxisSpacing: 12,
              ),
              itemCount: list.length,
              itemBuilder: (context, index) {
                return DoramaCard(dorama: list[index], width: double.infinity, height: double.infinity);
              },
            );
          }
        );
      }
    );
  }
}
