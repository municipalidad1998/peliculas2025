class Dorama {
  final String titulo;
  final String url;
  String? coverUrl;
  final String category;

  Dorama({
    required this.titulo,
    required this.url,
    this.coverUrl,
    required this.category,
  });

  factory Dorama.fromJson(Map<String, dynamic> json, String category) {
    return Dorama(
      titulo: json['titulo'] ?? '',
      url: json['url'] ?? '',
      coverUrl: json['coverUrl'], // Puede ser null si viene del JSON antiguo
      category: category,
    );
  }
}
