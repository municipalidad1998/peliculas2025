import 'package:flutter/material.dart';
import 'package:carousel_slider/carousel_slider.dart';
import '../services/dorama_service.dart';
import '../models/dorama.dart';
import '../widgets/dorama_card.dart';
import 'info_screen.dart';
import 'player_screen.dart';
import 'search_screen.dart';
import 'settings_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final DoramaService _service = DoramaService();
  bool _isLoading = true;
  int _selectedIndex = 1;

  @override
  void initState() {
    super.initState();
    _service.onDataUpdate = () {
       if (mounted) setState(() {});
    };
    _loadData();
  }

  @override
  void dispose() {
    _service.onDataUpdate = null;
    super.dispose();
  }

  Future<void> _loadData() async {
    await _service.init();
    if (mounted) setState(() => _isLoading = false);
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
            Text('Transmitir a TV', style: TextStyle(color: Colors.white)),
          ],
        ),
        content: const Text(
          'Debido a que extrajimos de 6 fuentes protegidas distintas, la señal de video nativa para Chromecast regular está encriptada.\n\nMétodo 1 (Directo): \nUsa el botón "Smart View" / "Transmitir Pantalla" desde el menú desplegable superior de tu celular Android hacia tu Smart TV.\n\nMétodo 2 (App Externa): \nDescarga la app gratuita "Web Video Caster" en la PlayStore y usa su navegador web incorporado para abrir las películas si deseas una conexión directa a Chromecast.',
          style: TextStyle(color: Colors.white70),
        ),
        actions: [
          TextButton(
            child: const Text('Entendido', style: TextStyle(color: Colors.red)),
            onPressed: () => Navigator.of(context).pop(),
          ),
        ],
      ),
    );
  }

  Widget _buildHomeView() {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (_service.recentReleases.isNotEmpty) _buildHeroCarousel(),
          const SizedBox(height: 20),
          if (_service.recentReleases.isNotEmpty)
            _buildRow("Estrenos Globales", _service.recentReleases),
          _buildRow("Te recomendamos", (_service.allSeries.toList()..shuffle()).take(40).toList()),
          _buildRow("Visto recientemente", (_service.allMovies.toList()..shuffle()).take(40).toList()),
          const SizedBox(height: 40),
        ],
      ),
    );
  }

  Widget _buildSeriesView() => _buildGrid("Series", _service.allSeries);
  Widget _buildPeliculasView() => _buildGrid("Películas", _service.allMovies);

  Widget _buildCurrentView() {
    switch (_selectedIndex) {
      case 0:
        return CloudSearchScreen(allDoramas: [..._service.allSeries, ..._service.allMovies]);
      case 1:
        return _buildHomeView();
      case 2:
        return _buildPeliculasView();
      case 3:
        return _buildSeriesView();
      case 4:
        return const SettingsScreen();
      default:
        return _buildHomeView();
    }
  }

  Widget _buildGrid(String title, List<Dorama> items) {
    if (items.isEmpty) return const Center(child: Text("Sin contenido", style: TextStyle(color: Colors.white)));
    
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Text(title, style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.w800, letterSpacing: 1.2)),
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
      backgroundColor: const Color(0xFF0F1715),
      body: _isLoading 
          ? const Center(child: CircularProgressIndicator(color: Color(0xFF40E0D0)))
          : _buildCurrentView(),
      bottomNavigationBar: Container(
        decoration: const BoxDecoration(
          border: Border(top: BorderSide(color: Colors.white12, width: 1)),
        ),
        child: BottomNavigationBar(
          backgroundColor: const Color(0xFF0B1310),
          selectedItemColor: const Color(0xFF40E0D0),
          unselectedItemColor: Colors.white54,
          currentIndex: _selectedIndex,
          type: BottomNavigationBarType.fixed,
          showSelectedLabels: true,
          showUnselectedLabels: true,
          selectedLabelStyle: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold),
          unselectedLabelStyle: const TextStyle(fontSize: 12),
          onTap: (index) => setState(() => _selectedIndex = index),
          items: const [
            BottomNavigationBarItem(icon: Icon(Icons.search, size: 26), label: 'Buscar'),
            BottomNavigationBarItem(icon: Icon(Icons.home_outlined, size: 26), label: 'Inicio'),
            BottomNavigationBarItem(icon: Icon(Icons.movie_outlined, size: 26), label: 'Películas'),
            BottomNavigationBarItem(icon: Icon(Icons.tv, size: 26), label: 'Series'),
            BottomNavigationBarItem(icon: Icon(Icons.settings_outlined, size: 26), label: 'Ajustes'),
          ],
        ),
      ),
    );
  }

  Widget _buildHeroCarousel() {
    final list = _service.recentReleases.take(5).toList();
    return CarouselSlider(
      options: CarouselOptions(
        height: 500.0,
        viewportFraction: 1.0, // Pantalla completa horizontal HBO Style
        enlargeCenterPage: false,
        autoPlay: true,
      ),
      items: list.map((dorama) {
        return Builder(
          builder: (context) {
            return Stack(
              fit: StackFit.expand,
              children: [
                DoramaCard(dorama: dorama, width: double.infinity, height: 500),
                // Gradiente encima del carrusel para blending con el fondo
                Container(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [const Color(0xFF1B1B22), Colors.transparent, Colors.black.withOpacity(0.8)],
                      begin: Alignment.bottomCenter,
                      end: Alignment.topCenter,
                      stops: const [0.0, 0.4, 1.0],
                    ),
                  ),
                ),
                Positioned(
                  bottom: 40,
                  left: 20,
                  right: 20,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      Text(
                        dorama.titulo.toUpperCase(), 
                        style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.w900, letterSpacing: 2.0),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 10),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(border: Border.all(color: Colors.white38), borderRadius: BorderRadius.circular(4)),
                            child: const Text('TV-MA', style: TextStyle(color: Colors.white70, fontSize: 10)),
                          ),
                          const SizedBox(width: 8),
                          const Text('• 2025 • Drama', style: TextStyle(color: Colors.white70, fontWeight: FontWeight.bold, fontSize: 12)),
                        ],
                      ),
                      const SizedBox(height: 15),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Expanded(
                            child: ElevatedButton.icon(
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.white,
                                foregroundColor: Colors.black,
                                padding: const EdgeInsets.symmetric(vertical: 12),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
                              ),
                              icon: const Icon(Icons.play_arrow, size: 24),
                              label: const Text("REPRODUCIR", style: TextStyle(fontWeight: FontWeight.bold, letterSpacing: 1.2)),
                              onPressed: () {
                                Navigator.push(context, MaterialPageRoute(builder: (c) => InfoScreen(dorama: dorama)));
                              },
                            ),
                          ),
                          const SizedBox(width: 15),
                          Expanded(
                            child: OutlinedButton.icon(
                              style: OutlinedButton.styleFrom(
                                foregroundColor: Colors.white,
                                side: const BorderSide(color: Colors.white54, width: 2),
                                padding: const EdgeInsets.symmetric(vertical: 12),
                                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(30)),
                              ),
                              icon: const Icon(Icons.add),
                              label: const Text("MI LISTA", style: TextStyle(fontWeight: FontWeight.bold, letterSpacing: 1.2)),
                              onPressed: () {},
                            ),
                          ),
                        ],
                      ),
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
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0), 
          child: Text(title, style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.w800, letterSpacing: 0.5))
        ),
        SizedBox(
          height: 180,
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 12.0),
            itemCount: items.length,
            itemBuilder: (context, index) => Padding(
              padding: const EdgeInsets.symmetric(horizontal: 4.0),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: DoramaCard(dorama: items[index], width: 120, height: 180),
              ),
            ),
          ),
        ),
        const SizedBox(height: 15),
      ],
    );
  }
}
