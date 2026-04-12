import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

class PlayerScreen extends StatefulWidget {
  final String title;
  final String url;

  const PlayerScreen({Key? key, required this.title, required this.url}) : super(key: key);

  @override
  State<PlayerScreen> createState() => _PlayerScreenState();
}

class _PlayerScreenState extends State<PlayerScreen> {
  late final WebViewController _controller;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(Colors.black)
      ..setNavigationDelegate(
        NavigationDelegate(
          onNavigationRequest: (NavigationRequest request) {
            // BLOQUEADOR DE PUBLICIDAD ABSOLUTA
            // Si la webview de DoramaExpress intenta mandarnos a una app externa
            // o salirse de su página principal a otra misteriosa (ej, ads), lo destruimos!
            if (!request.url.contains('doramaexpress.com') && 
                !request.url.contains('yandispoiler.net')) { // A veces yandi es su CDN
              
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('🚀 Anuncio emergente bloqueado y cerrado'),
                  backgroundColor: Colors.green,
                  duration: Duration(seconds: 1),
                ),
              );
              return NavigationDecision.prevent; // Anular salto
            }
            return NavigationDecision.navigate; // Fluir normal
          },
          onPageFinished: (String url) {
            if (mounted) {
              setState(() {
                _isLoading = false;
              });
            }
          },
        ),
      )
      ..loadRequest(Uri.parse(widget.url));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        title: Text(widget.title, style: const TextStyle(fontSize: 14)),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
      ),
      body: Stack(
        children: [
          WebViewWidget(controller: _controller),
          if (_isLoading)
            const Center(
              child: CircularProgressIndicator(color: Colors.red),
            ),
        ],
      ),
    );
  }
}
