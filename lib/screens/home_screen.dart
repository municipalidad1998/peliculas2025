import 'package:flutter/material.dart';
import 'package:carousel_slider/carousel_slider.dart';
import '../services/dorama_service.dart';
import '../models/dorama.dart';
import '../widgets/dorama_card.dart';
import 'search_screen.dart'; // Importador de búsqueda

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  final DoramaService _service = DoramaService();
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadData();
  }

  Future<void> _loadData() async {
    await _service.init();
    if (mounted) {
      setState(() {
        _isLoading = false;
      });
    }
  }

  void _openSearch() {
    // Unimos series y peliculas en la lista gigante del buscador
    final allContent = [..._service.allSeries, ..._service.allMovies];
    showSearch(context: context, delegate: DoramaSearchDelegate(allContent));
  }

  void _openCastDialog() {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          backgroundColor: Colors.grey[900],
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          title: const Row(
            children: [
              Icon(Icons.cast_connected, color: Colors.blueAccent),
              SizedBox(width: 10),
              Text('Dispositivos Cercanos', style: TextStyle(color: Colors.white)),
            ],
          ),
          content: const Text(
            'Buscando televisores...\n\nComo el contenido encriptado de esta app utiliza un WebView nativo, las señales crudas (MP4) no pueden lanzarse vía Google Chromecast normal sin interrupciones.\n\nPara mandar esta app al TV en 4K, por favor abre el menú rápido de tu celular Android y toca en "Transmitir Pantalla" (Smart View) hacia tu TV.',
            style: TextStyle(color: Colors.white70),
          ),
          actions: [
            TextButton(
              child: const Text('Entendido', style: TextStyle(color: Colors.red)),
              onPressed: () => Navigator.of(context).pop(),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black.withOpacity(0.8),
        elevation: 0,
        title: Row(
          children: [
            const Text(
              "N",
              style: TextStyle(color: Colors.red, fontSize: 32, fontWeight: FontWeight.bold),
            ),
            const SizedBox(width: 8),
            const Text("Doramflix", style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
          ],
        ),
        actions: [
          IconButton(icon: const Icon(Icons.cast, color: Colors.white), onPressed: _openCastDialog),
          IconButton(icon: const Icon(Icons.search, color: Colors.white), onPressed: _openSearch),
          const SizedBox(width: 16),
        ],
      ),
      body: _isLoading 
          ? const Center(child: CircularProgressIndicator(color: Colors.red))
          : SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (_service.recentReleases.isNotEmpty)
                    _buildHeroCarousel(),
                  
                  const SizedBox(height: 20),
                  
                  if (_service.recentReleases.isNotEmpty)
                    _buildRow("Agregado Recientemente (En Vivo)", _service.recentReleases),
                  
                  _buildRow("Catálogo de Doramas", _service.allSeries.take(20).toList()),
                  _buildRow("Nuestras Películas Recomendadas", _service.allMovies.take(20).toList()),
                  _buildRow("Tendencias en Series", _service.allSeries.skip(20).take(20).toList()),
                  
                  const SizedBox(height: 40),
                ],
              ),
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
          builder: (BuildContext context) {
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
                      Column(
                        children: [
                          Icon(Icons.add, color: Colors.white),
                          Text("Mi Lista", style: TextStyle(color: Colors.white, fontSize: 10))
                        ],
                      ),
                      Container(
                        padding: EdgeInsets.symmetric(horizontal: 20, vertical: 8),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(4)
                        ),
                        child: Row(
                          children: [
                            Icon(Icons.play_arrow, color: Colors.black),
                            Text(" Reproducir", style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold))
                          ],
                        ),
                      ),
                      Column(
                        children: [
                          Icon(Icons.info_outline, color: Colors.white),
                          Text("Info", style: TextStyle(color: Colors.white, fontSize: 10))
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
          child: Text(title, style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
        ),
        SizedBox(
          height: 160,
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 16.0),
            itemCount: items.length,
            itemBuilder: (context, index) {
              return DoramaCard(dorama: items[index], width: 110, height: 160);
            },
          ),
        ),
        const SizedBox(height: 10),
      ],
    );
  }
}
