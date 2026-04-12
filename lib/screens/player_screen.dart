import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:webview_flutter/webview_flutter.dart';
// ignore: depend_on_referenced_packages
import 'package:webview_flutter_android/webview_flutter_android.dart';

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
  late WebViewController _controller;
  bool _isLoading = true;
  bool _isDirectPlayMode = false;
  
  List<VideoServer> _extractedServers = [];
  String _currentServerName = "Auto-Selección";

  @override
  void initState() {
    super.initState();
    
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(Colors.black)
      ..addJavaScriptChannel('Extractor', onMessageReceived: _handleExtractedData)
      ..setNavigationDelegate(
        NavigationDelegate(
          onPageFinished: (String url) async {
            if (mounted && !_isDirectPlayMode) {
              // Si estamos en la página inicial, intentamos robar los servidores!
              _startScrapingServers();
              setState(() => _isLoading = false);
            } else if (mounted && _isDirectPlayMode) {
              setState(() => _isLoading = false);
            }
          },
        ),
      )
      ..loadRequest(Uri.parse(widget.url));
      
    if (_controller.platform is AndroidWebViewController) {
      AndroidWebViewController.enableDebugging(true);
      (_controller.platform as AndroidWebViewController).setMediaPlaybackRequiresUserGesture(false);
    }
  }

  @override
  void dispose() {
    SystemChrome.setPreferredOrientations([DeviceOrientation.portraitUp]);
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.manual, overlays: SystemUiOverlay.values);
    super.dispose();
  }

  void _startScrapingServers() {
    // Inyectamos un JS muy agresivo que caza los reproductores integrados en la carga inicial
    final String jsCode = """
      (function() {
         let list = [];
         
         // Buscar iframes puros (Fembed, Mega, DoodStream, etc)
         let frames = document.querySelectorAll('iframe');
         for (let i = 0; i < frames.length; i++) {
            let src = frames[i].src || frames[i].dataset.src;
            if(src && src.includes('http') && !src.includes('facebook') && !src.includes('ads')) {
               let n = src.split('/')[2];
               list.push({name: 'Opcion (' + n + ')', url: src});
            }
         }
         
         // Buscar enlaces rotulados como servidores ('Latino', 'Castellano', 'Subbed')
         let options = document.querySelectorAll('li, a, button, div.server');
         options.forEach(opt => {
            let text = opt.innerText.trim().toLowerCase();
            let src = opt.getAttribute('data-video') || opt.getAttribute('data-src') || opt.href;
            if(src && src.includes('http') && (text.includes('lat') || text.includes('sub') || text.includes('mega') || text.includes('fembed') || text.includes('netu'))) {
               list.push({name: opt.innerText.trim(), url: src});
            }
         });
         
         if(list.length > 0) {
            Extractor.postMessage(JSON.stringify(list));
         }

         // Inyector removedor de basura comercial y barras para aparentar ser Nativo
         let css = `header, footer, nav, aside, .sidebar, iframe[src*="ads"], .widget, .comments { display: none !important; }`;
         let style = document.createElement('style'); style.innerHTML = css; document.head.appendChild(style);
      })();
    """;
    
    Future.delayed(const Duration(seconds: 2), () {
      if(mounted && !_isDirectPlayMode) _controller.runJavaScript(jsCode);
    });
    Future.delayed(const Duration(seconds: 4), () {
      if(mounted && !_isDirectPlayMode) _controller.runJavaScript(jsCode);
    });
  }

  void _handleExtractedData(JavaScriptMessage message) {
    if (_isDirectPlayMode) return;
    
    try {
      final List<dynamic> data = json.decode(message.message);
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
          // Ordenamos para que los latinos queden arriba de todo
          found.sort((a, b) {
            if(a.isLatino && !b.isLatino) return -1;
            if(!a.isLatino && b.isLatino) return 1;
            return 0;
          });
          _extractedServers = found;
        });
        
        // Mostrar automaticamente la selección
        _showServerSelection();
      }
    } catch(e) {
      print("Extractor err: \$e");
    }
  }

  void _playDirectly(VideoServer server) {
     Navigator.pop(context); // Cierra menu
     
     // Activa Pantalla Completa y Horizontal!
     SystemChrome.setPreferredOrientations([DeviceOrientation.landscapeRight, DeviceOrientation.landscapeLeft]);
     SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);

     setState(() {
       _isDirectPlayMode = true; // Entramos en modo fullscreen limpio
       _isLoading = true;
       _currentServerName = server.name;
     });
     
     _controller.loadRequest(Uri.parse(server.url));
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
                 const Text("☁️ Servidores Encontrados", style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
                 const SizedBox(height: 10),
                 const Text("Selecciona un servidor. Los servidores en LATINO (si la página los provee) están fijados arriba. Las calidades dependerán del servidor elegido.", style: TextStyle(color: Colors.grey, fontSize: 13), textAlign: TextAlign.center,),
                 const SizedBox(height: 15),
                 Expanded(
                   child: ListView.builder(
                     itemCount: _extractedServers.length,
                     itemBuilder: (context, index) {
                        final s = _extractedServers[index];
                        return Card(
                          color: s.isLatino ? Colors.green[900] : Colors.grey[800],
                          margin: const EdgeInsets.only(bottom: 8),
                          child: ListTile(
                            leading: Icon(s.isLatino ? Icons.g_translate : Icons.public, color: Colors.white),
                            title: Text(s.name.isEmpty ? 'Servidor \${index+1}' : s.name, style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold)),
                            trailing: const Icon(Icons.play_circle_fill, color: Colors.red),
                            onTap: () => _playDirectly(s),
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
           if(_extractedServers.isNotEmpty)
             TextButton.icon(
               style: TextButton.styleFrom(foregroundColor: Colors.white),
               icon: const Icon(Icons.switch_video, color: Colors.blueAccent),
               label: const Text("Cambiar Servidor"),
               onPressed: _showServerSelection,
             )
        ],
      ),
      body: Stack(
        children: [
          WebViewWidget(controller: _controller),
          
          if (_isLoading)
            Container(
              color: Colors.black,
              child: const Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    CircularProgressIndicator(color: Colors.red),
                    SizedBox(height: 20),
                    Text(
                      'Decodificando video...',
                      style: TextStyle(color: Colors.white, fontSize: 16),
                    ),
                  ],
                ),
              ),
            ),
        ],
      ),
      floatingActionButton: _extractedServers.isNotEmpty && !_isLoading
          ? FloatingActionButton(
              backgroundColor: Colors.red,
              child: const Icon(Icons.list),
              onPressed: _showServerSelection,
            )
          : null,
    );
  }
}
