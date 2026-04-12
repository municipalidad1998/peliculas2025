import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';
import '../widgets/hbo_player_controls.dart';
import '../services/native_scraper.dart';

class VideoServer {
  final String name;
  final String url;
  
  VideoServer({required this.name, required this.url});
  
  bool get isLatino => name.toLowerCase().contains('lat') || name.toLowerCase().contains('mx');
}

class PlayerScreen extends StatefulWidget {
  final String title;
  final String url;

  const PlayerScreen({Key? key, required this.title, required this.url}) : super(key: key);

  @override
  State<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends State<PlayerScreen> with SingleTickerProviderStateMixin {
  final NativeScraperService _scraper = NativeScraperService();
  late AnimationController _menuAnimController;

  // Video Player State
  VideoPlayerController? _videoPlayerController;
  bool _isPlayingVideo = false;
  String _statusMessage = "Analizando enlaces crudos...";

  // Overlay Menu State
  bool _isMenuOpen = true; // Empieza abierto
  List<VideoServer> _servers = [];
  VideoServer? _selectedServer;
  
  // Toggles de ajustes locales
  bool _hwDecoded = false;
  bool _extraBuffer = true;
  bool _gesturesEnabled = true;
  bool _keepAwake = false;

  @override
  void initState() {
    super.initState();
    SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp, DeviceOrientation.landscapeLeft, DeviceOrientation.landscapeRight]);
    
    _menuAnimController = AnimationController(vsync: this, duration: const Duration(milliseconds: 300));
    _menuAnimController.forward(); // Animación slide-in
    
    _loadServers();
  }

  Future<void> _loadServers() async {
    final list = await _scraper.getServersFromUrl(widget.url);
    if (mounted) {
      setState(() {
        _servers = list;
        if (_servers.isNotEmpty) {
          _selectedServer = _servers.first;
          _statusMessage = "Listo. Selecciona un servidor.";
        } else {
          _statusMessage = "No se encontraron servidores nativos.";
        }
      });
    }
  }

  @override
  void dispose() {
    _videoPlayerController?.dispose();
    _menuAnimController.dispose();
    SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.manual, overlays: SystemUiOverlay.values);
    super.dispose();
  }

  Future<void> _attemptCustomPlay() async {
    if (_selectedServer == null) return;
    
    setState(() => _statusMessage = "Obteniendo archivo M3U8/MP4...");
    final directUrl = await _scraper.extractDirectVideoStream(_selectedServer!.url);

    if (directUrl != null && (directUrl.contains('.mp4') || directUrl.contains('.m3u8'))) {
      _startNativePlayer(directUrl);
    } else {
      if (mounted) {
        setState(() => _statusMessage = "Servidor inalcanzable. Prueba otro.");
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
          content: Text("Servidor no disponible. Prueba otro.", style: TextStyle(color: Colors.white)),
          backgroundColor: Colors.red,
        ));
      }
    }
  }

  Future<void> _startNativePlayer(String cleanUrl) async {
    _videoPlayerController?.dispose();

    setState(() {
      _isPlayingVideo = true;
      _isMenuOpen = false; // Cerramos the overlay de configuración
      _menuAnimController.reverse(); // Slide Out
    });

    _videoPlayerController = VideoPlayerController.networkUrl(Uri.parse(cleanUrl));
    
    try {
      await _videoPlayerController!.initialize();
      _videoPlayerController!.play();
      if (mounted) setState(() {}); 
    } catch (e) {
      if (mounted) {
        setState(() {
           _isPlayingVideo = false;
           _isMenuOpen = true;
           _menuAnimController.forward();
           _statusMessage = "La fuente de video devolvió un error (403/500). Falla de Cloudflare.";
        });
      }
    }
  }

  void _openSettingsMenu() {
    setState(() {
       _isMenuOpen = true;
       _menuAnimController.forward();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        fit: StackFit.expand,
        children: [
          // Capa Base: Reproductor Nativo de Video Exclusivamente
          if (_isPlayingVideo && _videoPlayerController != null)
             _videoPlayerController!.value.isInitialized
                ? Stack(
                    fit: StackFit.expand,
                    children: [
                      Center(
                        child: AspectRatio(
                          aspectRatio: _videoPlayerController!.value.aspectRatio,
                          child: VideoPlayer(_videoPlayerController!),
                        ),
                      ),
                      if (!_isMenuOpen)
                        HboPlayerControls(
                          controller: _videoPlayerController!,
                          title: widget.title,
                          onSettingsTap: _openSettingsMenu,
                        ),
                    ],
                  )
                : const Center(child: CircularProgressIndicator(color: Colors.purpleAccent))
          else if (!_isMenuOpen)
             const Center(child: CircularProgressIndicator(color: Colors.white)),

          // Capa Superior: HBO Overlay Menu (Fondo Oscuro al 85% + SlideIn desde Derecha)
          if (_isMenuOpen)
             GestureDetector(
               onTap: _isPlayingVideo ? () {
                  setState(() { _isMenuOpen = false; _menuAnimController.reverse(); });
               } : null,
               child: Container(
                 color: Colors.black87,
                 width: double.infinity,
                 height: double.infinity,
                 child: Stack(
                   children: [
                     Positioned(
                       top: 40,
                       left: 20,
                       child: IconButton(
                         icon: const Icon(Icons.arrow_back_ios, color: Colors.white),
                         onPressed: () => Navigator.of(context).pop(),
                       ),
                     ),
                     SlideTransition(
                       position: Tween<Offset>(begin: const Offset(1, 0), end: Offset.zero).animate(
                         CurvedAnimation(parent: _menuAnimController, curve: Curves.easeOutQuad)
                       ),
                       child: Align(
                         alignment: Alignment.centerRight,
                         child: Container(
                           width: MediaQuery.of(context).size.width > 600 ? 400 : MediaQuery.of(context).size.width * 0.85,
                           height: double.infinity,
                           color: const Color(0xFF1B1B1E), // HBO dark tray
                           padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 40),
                           child: Column(
                             crossAxisAlignment: CrossAxisAlignment.start,
                             children: [
                               Text("Ver", style: TextStyle(color: Colors.grey[400], fontSize: 14, fontWeight: FontWeight.bold)),
                               Text(widget.title, style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold)),
                               const SizedBox(height: 20),
                               
                               if (_servers.isEmpty) ...[
                                  Text(_statusMessage, style: const TextStyle(color: Colors.white70)),
                                  const SizedBox(height: 10),
                                  const CircularProgressIndicator(color: Colors.white54),
                               ] else ...[
                                  const Text("📺 Servidores en Múltiples Redes", style: TextStyle(color: Colors.white70, fontSize: 13, fontWeight: FontWeight.bold)),
                                  const SizedBox(height: 10),
                                  Container(
                                    decoration: BoxDecoration(border: Border.all(color: Colors.white12), borderRadius: BorderRadius.circular(8)),
                                    constraints: const BoxConstraints(maxHeight: 180),
                                    child: ListView.builder(
                                      padding: EdgeInsets.zero,
                                      shrinkWrap: true,
                                      itemCount: _servers.length,
                                      itemBuilder: (context, i) {
                                        final s = _servers[i];
                                        final isSel = _selectedServer == s;
                                        return RadioListTile<VideoServer>(
                                          value: s,
                                          groupValue: _selectedServer,
                                          title: Text(s.name, style: TextStyle(color: isSel ? Colors.white : Colors.grey[400], fontSize: 14)),
                                          activeColor: Colors.white,
                                          onChanged: (val) {
                                            if (val != null) setState(() => _selectedServer = val);
                                          },
                                        );
                                      },
                                    ),
                                  ),
                               ],

                               const SizedBox(height: 20),
                               const Divider(color: Colors.white12),

                               Expanded(
                                 child: ListView(
                                   padding: EdgeInsets.zero,
                                   children: [
                                     SwitchListTile(
                                       contentPadding: EdgeInsets.zero,
                                       title: const Text("🔄 Servidor con búfer extra", style: TextStyle(color: Colors.white70, fontSize: 14)),
                                       value: _extraBuffer,
                                       activeColor: Colors.white,
                                       onChanged: (val) => setState(() => _extraBuffer = val),
                                     ),
                                     SwitchListTile(
                                       contentPadding: EdgeInsets.zero,
                                       title: const Text("⚙️ Decodificado por software", style: TextStyle(color: Colors.white70, fontSize: 14)),
                                       value: _hwDecoded,
                                       activeColor: Colors.white,
                                       onChanged: (val) => setState(() => _hwDecoded = val),
                                     ),
                                     SwitchListTile(
                                       contentPadding: EdgeInsets.zero,
                                       title: const Text("🖐️ Gestos Rápidos", style: TextStyle(color: Colors.white70, fontSize: 14)),
                                       value: _gesturesEnabled,
                                       activeColor: Colors.white,
                                       onChanged: (val) => setState(() => _gesturesEnabled = val),
                                     ),
                                     SwitchListTile(
                                       contentPadding: EdgeInsets.zero,
                                       title: const Text("💡 Mantener pantalla encendida", style: TextStyle(color: Colors.white70, fontSize: 14)),
                                       value: _keepAwake,
                                       activeColor: Colors.white,
                                       onChanged: (val) => setState(() => _keepAwake = val),
                                     ),
                                   ],
                                 ),
                               ),

                               // Play action
                               SizedBox(
                                 width: double.infinity,
                                 child: ElevatedButton.icon(
                                   style: ElevatedButton.styleFrom(
                                     backgroundColor: Colors.white,
                                     foregroundColor: Colors.black,
                                     padding: const EdgeInsets.symmetric(vertical: 16),
                                     shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                                     disabledBackgroundColor: Colors.grey[800],
                                     disabledForegroundColor: Colors.grey[600],
                                   ),
                                   icon: const Icon(Icons.play_arrow),
                                   label: const Text("REPRODUCIR", style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, letterSpacing: 1.5)),
                                   onPressed: _servers.isNotEmpty ? _attemptCustomPlay : null,
                                 ),
                               ),
                             ],
                           ),
                         ),
                       ),
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
