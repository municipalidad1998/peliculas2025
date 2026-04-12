import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:video_player/video_player.dart';
import '../widgets/hbo_player_controls.dart';

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

class _PlayerScreenState extends State<PlayerScreen> {
  // Sniffer State
  InAppWebViewController? _webViewController;
  bool _isWebVisible = true;
  bool _isSniffing = false;
  String _snifferStatus = "Analizando...";
  
  // Native Player State
  bool _isNativeMode = false;
  VideoPlayerController? _videoPlayerController;

  // Extractor State
  List<VideoServer> _extractedServers = [];
  String _currentServerName = "Buscando...";

  @override
  void initState() {
    super.initState();
    // Forzamos rotación normal al iniciar
    SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
  }

  @override
  void dispose() {
    _videoPlayerController?.dispose();
    SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.manual, overlays: SystemUiOverlay.values);
    super.dispose();
  }

  Future<void> _startNativePlayer(String rawVideoUrl) async {
    // Si ya hay uno, matarlo
    _videoPlayerController?.dispose();

    setState(() {
      _isSniffing = false;
      _isNativeMode = true;
      _isWebVisible = false;
    });

    _videoPlayerController = VideoPlayerController.networkUrl(Uri.parse(rawVideoUrl));
    
    try {
      await _videoPlayerController!.initialize();
      _videoPlayerController!.play();
      setState(() {}); 
    } catch (e) {
      if (mounted) {
        setState(() => _snifferStatus = "Error decodificando flujo nativo");
      }
    }
  }

  void _onServerSelected(VideoServer server) {
    Navigator.pop(context); // Cerrar menu
    
    // Apagar player nativo si existe
    _videoPlayerController?.pause();
    
    setState(() {
      _isNativeMode = false;
      _isSniffing = true;
      _isWebVisible = false; // Ocultamos web, mostramos letrero de sniffing
      _snifferStatus = "Extrayendo flujo bruto de \${server.name}...";
      _currentServerName = server.name;
    });

    _webViewController?.loadUrl(urlRequest: URLRequest(url: WebUri(server.url)));
  }
  void _showSettingsPanel() {
     showModalBottomSheet(
       context: context,
       backgroundColor: Colors.grey[900],
       shape: const RoundedRectangleBorder(borderRadius: BorderRadius.horizontal(left: Radius.circular(20))),
       isScrollControlled: true,
       builder: (context) {
          return FractionallySizedBox(
            widthFactor: 0.4,
            heightFactor: 1.0,
            child: Container(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text("⚙️ Ajustes (HBO Style)", style: TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
                  const Divider(color: Colors.grey),
                  SwitchListTile(
                    title: const Text("Decodificado por software", style: TextStyle(color: Colors.white)),
                    value: false,
                    onChanged: (v) {},
                    activeColor: Colors.purpleAccent,
                  ),
                  ListTile(
                    title: const Text("Servidor Actual", style: TextStyle(color: Colors.white)),
                    subtitle: Text(_currentServerName, style: const TextStyle(color: Colors.purpleAccent)),
                    trailing: const Icon(Icons.arrow_forward_ios, color: Colors.grey, size: 16),
                    onTap: () {
                       Navigator.pop(context);
                       _showServerSelection();
                    },
                  ),
                  SwitchListTile(
                    title: const Text("Servidor con búfer extra", style: TextStyle(color: Colors.white)),
                    value: true,
                    onChanged: (v) {},
                    activeColor: Colors.purpleAccent,
                  ),
                  const Spacer(),
                ],
              ),
            ),
          );
       }
     );
  }

  void _showServerSelection() {
     showModalBottomSheet(
       context: context,
       backgroundColor: Colors.grey[900],
       shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
       builder: (context) {
          return Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
               children: [
                 const Text("☁️ Servidores Nativos Encontrados", style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
                 const SizedBox(height: 10),
                 const Text("Selecciona un servidor. La app interceptará la red secreta de la web para robar la película cruda y transmitirla.", style: TextStyle(color: Colors.grey, fontSize: 13), textAlign: TextAlign.center,),
                 const SizedBox(height: 15),
                 Expanded(
                   child: ListView.builder(
                     itemCount: _extractedServers.length,
                     itemBuilder: (context, index) {
                        final s = _extractedServers[index];
                        return Card(
                          color: s.isLatino ? Colors.purple[900] : Colors.grey[800],
                          margin: const EdgeInsets.only(bottom: 8),
                          child: ListTile(
                            leading: Icon(s.isLatino ? Icons.g_translate : Icons.public, color: Colors.white),
                            title: Text(s.name.isEmpty ? 'Servidor ${index+1}' : s.name, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                            trailing: const Icon(Icons.flash_on, color: Colors.purpleAccent),
                            onTap: () => _onServerSelected(s),
                          ),
                        );
                     },
                   ),
                 )
               ],
            ),
          );
       }
      );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        title: Text(widget.title),
        backgroundColor: Colors.black,
        actions: [
           if (_extractedServers.isNotEmpty)
             TextButton.icon(
               style: TextButton.styleFrom(foregroundColor: Colors.white),
               icon: const Icon(Icons.router, color: Colors.blueAccent),
               label: const Text("Servidores"),
               onPressed: _showServerSelection,
             )
        ],
      ),
      body: Stack(
        children: [
          // Capa 1: El Sniffer Híbrido (Invisible si estamos en Nativo, visible si no se han extraido iframes para Series)
          if (!_isNativeMode)
            InAppWebView(
              initialUrlRequest: URLRequest(url: WebUri(widget.url)),
              initialSettings: InAppWebViewSettings(
                javaScriptEnabled: true,
                mediaPlaybackRequiresUserGesture: false,
                useShouldInterceptRequest: true,
                transparentBackground: true,
              ),
              onWebViewCreated: (controller) {
                _webViewController = controller;
                
                // Canal Mágico
                controller.addJavaScriptHandler(
                  handlerName: 'Extractor',
                  callback: (args) {
                    if (args.isEmpty) return;
                    try {
                      final List<dynamic> data = json.decode(args[0]);
                      List<VideoServer> found = [];
                      for(var d in data) {
                         if(d['url'] != null && d['url'].toString().startsWith('http')) {
                             if(!found.any((f) => f.url == d['url'])) {
                                 found.add(VideoServer(name: d['name'], url: d['url']));
                             }
                         }
                      }
                      
                      if (found.isNotEmpty) {
                        setState(() {
                          found.sort((a, b) {
                            if(a.isLatino && !b.isLatino) return -1;
                            if(!a.isLatino && b.isLatino) return 1;
                            return 0;
                          });
                          _extractedServers = found;
                          // Si es la página inicial y hay servidores, auto-robamos el primero!
                          if (_isWebVisible) {
                            _onServerSelected(found.first);
                          }
                        });
                      }
                    } catch(e) {}
                  }
                );
              },
              shouldInterceptRequest: (controller, request) async {
                if (!_isSniffing) return null;
                
                final url = request.url.toString();
                // SNIFFER: Atrapando tráfico de Video M3U8 o .MP4
                if (url.contains('.m3u8') || (url.contains('.mp4') && !url.contains('ads'))) {
                  print(">>> ATTRAPADO RAW: \$url");
                  Future.microtask(() => _startNativePlayer(url));
                  return WebResourceResponse(contentType: "text/plain", data: Uint8List.fromList([]), statusCode: 200, reasonPhrase: "Intercepted");
                }
                return null;
              },
              onLoadStop: (controller, url) async {
                 // Inyector removedor de basura web
                 await controller.evaluateJavascript(source: """
                    let css = `header, footer, nav, aside, .sidebar, iframe[src*="ads"], .widget, .comments { display: none !important; }`;
                    let style = document.createElement('style'); style.innerHTML = css; document.head.appendChild(style);
                    document.body.style.backgroundColor = 'black';
                 """);
                 
                 // Buscador agresivo de botones y servidores
                 await controller.evaluateJavascript(source: """
                    (function() {
                       let list = [];
                       let frames = document.querySelectorAll('iframe');
                       for (let i = 0; i < frames.length; i++) {
                          let s = frames[i].src || frames[i].dataset.src;
                          if(s && s.includes('http') && !s.includes('facebook') && !s.includes('ads')) {
                             list.push({name: 'Iframe (' + s.split('/')[2] + ')', url: s});
                          }
                       }
                       
                       let opts = document.querySelectorAll('li, a, button, div.server, .opt');
                       opts.forEach(opt => {
                          let text = opt.innerText.trim().toLowerCase();
                          let s = opt.getAttribute('data-video') || opt.getAttribute('data-src') || opt.href;
                          if(s && s.includes('http') && (text.includes('lat') || text.includes('sub') || text.includes('mega') || text.includes('fembed'))) {
                             list.push({name: opt.innerText.trim(), url: s});
                          }
                       });
                       
                       if(list.length > 0) window.flutter_inappwebview.callHandler('Extractor', JSON.stringify(list));
                    })();
                 """);
              },
            ),
            
          // Capa 2: Pantalla de "Sniffing" Ocultadora
          if (!_isNativeMode && (!_isWebVisible || _isSniffing))
            Container(
              color: Colors.black,
              width: double.infinity,
              height: double.infinity,
              child: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const CircularProgressIndicator(color: Colors.red),
                    const SizedBox(height: 20),
                    Text(
                      _snifferStatus,
                      style: const TextStyle(color: Colors.white, fontSize: 16),
                      textAlign: TextAlign.center,
                    ),
                  ],
                ),
              ),
            ),
            
          // Capa 3: El Reproductor de Cine Puramente Nativo
          if (_isNativeMode && _videoPlayerController != null)
             Container(
               color: Colors.black,
               width: double.infinity,
               height: double.infinity,
               child: _videoPlayerController!.value.isInitialized
                  ? Stack(
                      fit: StackFit.expand,
                      children: [
                        Center(
                          child: AspectRatio(
                            aspectRatio: _videoPlayerController!.value.aspectRatio,
                            child: VideoPlayer(_videoPlayerController!),
                          ),
                        ),
                        HboPlayerControls(
                          controller: _videoPlayerController!,
                          title: widget.title,
                          onSettingsTap: _showSettingsPanel,
                        ),
                      ],
                    )
                  : const Center(child: CircularProgressIndicator(color: Colors.purpleAccent)),
             ),
        ],
      ),
      floatingActionButton: _extractedServers.isNotEmpty && !_isNativeMode
          ? FloatingActionButton(
              backgroundColor: Colors.red,
              child: const Icon(Icons.list),
              onPressed: _showServerSelection,
            )
          : null,
    );
  }
}
