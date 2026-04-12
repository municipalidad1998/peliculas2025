import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:html/parser.dart' show parse;
import '../screens/player_screen.dart' show VideoServer;

class NativeScraperService {
  static const Map<String, String> defaultHeaders = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
    'Accept-Language': 'es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3',
  };

  /// Devuelve la lista de servidores hallados en una página
  Future<List<VideoServer>> getServersFromUrl(String sourceUrl) async {
    try {
      final response = await http.get(Uri.parse(sourceUrl), headers: defaultHeaders);
      if (response.statusCode != 200) return [];

      final document = parse(response.body);
      final List<VideoServer> found = [];
      
      // Buscar iframes
      final iframes = document.querySelectorAll('iframe');
      for (var f in iframes) {
        final src = f.attributes['src'] ?? f.attributes['data-src'] ?? '';
        if (_isValidServerDomain(src)) {
          found.add(VideoServer(name: 'Server: \${_getDomainName(src)}', url: src));
        }
      }

      // Buscar botones de servidores comunes
      final links = document.querySelectorAll('li, a, button, .server');
      int counter = 1;
      for (var link in links) {
        final src = link.attributes['data-video'] ?? link.attributes['href'] ?? link.attributes['data-src'] ?? '';
        if (_isValidServerDomain(src)) {
          final text = link.text.trim().isNotEmpty ? link.text.trim() : 'Opción \$counter';
          found.add(VideoServer(name: text, url: src));
          counter++;
        }
      }

      // Eliminar duplicados simulado
      final Map<String, VideoServer> unique = {};
      for (var v in found) {
        unique[v.url] = v;
      }
      return unique.values.toList();
    } catch (e) {
      return [];
    }
  }

  /// Recupera el .mp4 o .m3u8 crudo desde la url del servidor seleccionado
  Future<String?> extractDirectVideoStream(String serverUrl) async {
    // Si ya es video, pasarlo limpio
    if (serverUrl.contains('.mp4') || serverUrl.contains('.m3u8')) {
      return _cleanUrl(serverUrl);
    }
    
    // De lo contrario intentamos scrape del embed
    try {
      final response = await http.get(Uri.parse(serverUrl), headers: defaultHeaders);
      if (response.statusCode == 200) {
        final document = parse(response.body);
        
        // Estrategia 1: etiqueta <video> o <source> directa
        final sources = document.querySelectorAll('source');
        for (var s in sources) {
          final src = s.attributes['src'] ?? '';
          if (src.contains('.mp4') || src.contains('.m3u8')) {
            return _cleanUrl(src);
          }
        }
        
        // Estrategia 2: RegExp en el HTML (buscando 'file': 'http...mp4')
        final RegExp urlRegex = RegExp("(http[s]?://[^\\\\s\'\"]+\\\\.(?:mp4|m3u8)[^\\\\s\'\"]*)");
        final match = urlRegex.firstMatch(response.body);
        if (match != null && match.group(1) != null) {
          return _cleanUrl(match.group(1)!);
        }
      }
    } catch (e) {}

    return null; // Nada encontrado
  }

  bool _isValidServerDomain(String url) {
    if (!url.startsWith('http')) return false;
    final lower = url.toLowerCase();
    
    // Lista negra estricta
    if (lower.contains('ads') || 
        lower.contains('pop') || 
        lower.contains('redirect') || 
        lower.contains('tracker') || 
        lower.contains('go.') ||
        lower.contains('facebook') ||
        lower.contains('googlesyndication.com')) {
      return false;
    }
    return true;
  }

  String _cleanUrl(String raw) {
    // Eliminar basuras frecuentes como '?ads=1', '&ref='
    var cleaned = raw.split('?ref=')[0].split('&ads=')[0];
    return cleaned;
  }

  String _getDomainName(String url) {
    try {
      return Uri.parse(url).host.replaceFirst('www.', '');
    } catch (e) {
      return 'Desconocido';
    }
  }
}
