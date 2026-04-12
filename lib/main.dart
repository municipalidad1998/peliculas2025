import 'package:flutter/material.dart';
import 'screens/home_screen.dart';

void main() {
  runApp(const DoramaNetflixApp());
}

class DoramaNetflixApp extends StatelessWidget {
  const DoramaNetflixApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Doramflix',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        primaryColor: const Color(0xFF40E0D0),
        scaffoldBackgroundColor: const Color(0xFF0F1715),
        appBarTheme: const AppBarTheme(
          backgroundColor: Color(0xFF0F1715),
        ),
      ),
      home: const HomeScreen(),
    );
  }
}
