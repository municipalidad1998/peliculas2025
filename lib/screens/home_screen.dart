import 'package:flutter/material.dart';
import 'package:carousel_slider/carousel_slider.dart';
import '../services/dorama_service.dart';
import '../models/dorama.dart';
import '../widgets/dorama_card.dart';
import 'search_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final DoramaService _service = DoramaService();
  bool _isLoading = true;
  int _selectedIndex = 0; // 0=Inicio, 1=Series, 2=Películas

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    await _service.init();
    if (mounted) setState(() => _isLoading = false);
  }

  void _openSearch() {
    final allContent = [..._service.allSeries, ..._service.allMovies];
    showSearch(context: context, delegate: DoramaSearchDelegate(allContent));
  }

  void _openCastDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: Colors.grey[900],
        title: const Row(
          children: [
            Icon(Icons.cast_connected, color: Colors.blueAccent),
            SizedBox(width: 10),
            Text('Dispositivos', style: TextStyle(color: Colors.white)),
          ],
        ),
        content: const Text(
          'Para lanzar desde los 6 sitios integrados hacia la TV en 4K, usa la función Android nativa "Transmitir Pantalla" o "Smart View".',
          style: TextStyle(color: Colors.white70),
        ),
      ),
    );
  }

  Widget _buildCurrentView() {
    if (_selectedIndex == 0) {
      // INICIO
      return SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (_service.recentReleases.isNotEmpty) _buildHeroCarousel(),
            const SizedBox(height: 20),
            if (_service.recentReleases.isNotEmpty)
              _buildRow("Estrenos Globales (En Vivo)", _service.recentReleases),
            _buildRow("Catálogo de Series Asiáticas", _service.allSeries.take(20).toList()),
            _buildRow("Películas Recomendadas", _service.allMovies.take(20).toList()),
            _buildRow("Tendencias Clásicas", _service.allSeries.skip(20).take(20).toList()),
            const SizedBox(height: 40),
          ],
        ),
      );
    } else if (_selectedIndex == 1) {
      // SERIES MODO REJILLA
      return _buildGrid("Series Disponibles", _service.allSeries);
    } else {
      // PELICULAS MODO REJILLA
      return _buildGrid("Películas Disponibles", _service.allMovies);
    }
  }

  Widget _buildGrid(String title, List<Dorama> items) {
    if (items.isEmpty) return const Center(child: Text("Sin contenido", style: TextStyle(color: Colors.white)));
    
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Text(title, style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold)),
        ),
        Expanded(
          child: GridView.builder(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
              maxCrossAxisExtent: 140,
              childAspectRatio: 0.65,
              crossAxisSpacing: 10,
              mainAxisSpacing: 10,
            ),
            itemCount: items.length,
            itemBuilder: (context, index) {
              return DoramaCard(dorama: items[index], width: double.infinity, height: double.infinity);
            },
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black.withOpacity(0.8),
        title: Row(
          children: const [
            Text("N", style: TextStyle(color: Colors.red, fontSize: 32, fontWeight: FontWeight.bold)),
            SizedBox(width: 8),
            Text("Doramflix", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          ],
        ),
        actions: [
          IconButton(icon: const Icon(Icons.cast, color: Colors.white), onPressed: _openCastDialog),
          IconButton(icon: const Icon(Icons.search, color: Colors.white), onPressed: _openSearch),
          const SizedBox(width: 8),
        ],
      ),
      body: _isLoading 
          ? const Center(child: CircularProgressIndicator(color: Colors.red))
          : _buildCurrentView(),
      bottomNavigationBar: BottomNavigationBar(
        backgroundColor: Colors.black,
        selectedItemColor: Colors.white,
        unselectedItemColor: Colors.grey[600],
        currentIndex: _selectedIndex,
        onTap: (index) => setState(() => _selectedIndex = index),
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.home), label: 'Inicio'),
          BottomNavigationBarItem(icon: Icon(Icons.tv), label: 'Series'),
          BottomNavigationBarItem(icon: Icon(Icons.local_movies), label: 'Películas'),
        ],
      ),
    );
  }

  Widget _buildHeroCarousel() {
    final list = _service.recentReleases.take(5).toList();
    return CarouselSlider(
      options: CarouselOptions(
        height: 400.0,
        viewportFraction: 0.8,
        enlargeCenterPage: true,
        autoPlay: true,
      ),
      items: list.map((dorama) {
        return Builder(
          builder: (context) {
            return Stack(
              fit: StackFit.expand,
              children: [
                DoramaCard(dorama: dorama, width: double.infinity, height: 400),
                Positioned(
                  bottom: 20,
                  left: 0,
                  right: 0,
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      Column(children: const [Icon(Icons.add, color: Colors.white), Text("Mi Lista", style: TextStyle(color: Colors.white, fontSize: 10))]),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(4)),
                        child: Row(children: const [Icon(Icons.play_arrow, color: Colors.black), Text(" Reproducir", style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold))]),
                      ),
                      Column(children: const [Icon(Icons.info_outline, color: Colors.white), Text("Info", style: TextStyle(color: Colors.white, fontSize: 10))]),
                    ],
                  ),
                )
              ],
            );
          },
        );
      }).toList(),
    );
  }

  Widget _buildRow(String title, List<Dorama> items) {
    if (items.isEmpty) return const SizedBox.shrink();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0), child: Text(title, style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold))),
        SizedBox(
          height: 160,
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            itemCount: items.length,
            itemBuilder: (context, index) => DoramaCard(dorama: items[index], width: 110, height: 160),
          ),
        ),
        const SizedBox(height: 10),
      ],
    );
  }
}
