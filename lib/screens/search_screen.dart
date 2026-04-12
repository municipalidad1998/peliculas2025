import 'package:flutter/material.dart';
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
  Widget buildResults(BuildContext context) => _buildList();

  @override
  Widget buildSuggestions(BuildContext context) => _buildList();

  Widget _buildList() {
    if (query.isEmpty) {
      return const Center(
        child: Text(
          'Escribe para buscar...',
          style: TextStyle(color: Colors.white54, fontSize: 18),
        ),
      );
    }

    final list = allDoramas
        .where((d) => d.titulo.toLowerCase().contains(query.toLowerCase()))
        .toList();

    if (list.isEmpty) {
      return const Center(
         child: Text(
           'No se encontraron resultados',
           style: TextStyle(color: Colors.white, fontSize: 18),
         ),
      );
    }

    // Usamos LayoutBuilder para adaptar la cuadrícula si es teléfono pequeño o ventana web ancha
    return LayoutBuilder(
      builder: (context, constraints) {
        int columns = constraints.maxWidth ~/ 120; // Aproximadamente 120px de ancho por tarjeta
        if (columns < 2) columns = 2; // Mínimo 2 columnas
        
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
            return DoramaCard(
              dorama: list[index],
              width: double.infinity,
              height: double.infinity,
            );
          },
        );
      }
    );
  }
}
